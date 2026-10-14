package com.proximitylabs.proxitip_system_bars

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager

/**
 * Applies the dark-navigation-bar request to ONE window: the host Activity's
 * own window. Must be called on the main thread.
 *
 * Expected effect per API (AOSP framework source, android-36.1):
 *  - setNavigationBarColor: on API <= 34 (and on Android 15 windows that are
 *    not edge-to-edge) it is the opaque bar colour. On edge-to-edge windows
 *    (API 35+ enforced) the colour is not drawn as-is; it marks the colour as
 *    "specified" (stops the system deriving a light bar from the window
 *    background) and, while contrast is enforced, the 3-button scrim becomes
 *    this colour at 80% alpha. Deprecated in API 35 but still functional.
 *  - setNavigationBarContrastEnforced(true): keep the system scrim for
 *    3-button navigation (readability). Ignored for gesture navigation, where
 *    the bar stays transparent.
 *  - clearing LIGHT_NAVIGATION_BARS: light buttons / light gesture handle.
 *
 * Some vendor builds may re-impose the light flag after this runs; the
 * diagnostics/log output exists to check that on a device.
 */
internal object NavBarApplier {

    fun apply(activity: Activity, config: NavBarConfig, reason: String) {
        val window = activity.window ?: return
        // Forces decor installation so getInsetsController() is non-null.
        val decor = window.decorView
        val before = if (config.debugLogging) snapshot(activity) else null
        val plan = NavBarPlan.forSdk(Build.VERSION.SDK_INT)

        // The bar colour is only drawn when the window draws system-bar
        // backgrounds and the navigation bar is not translucent.
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        if (plan.setColor) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = config.color
        }
        if (plan.setDividerColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        if (plan.setContrast && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = config.contrastEnforced
        }
        if (plan.clearLegacyLightFlag) {
            @Suppress("DEPRECATION")
            val flags = decor.systemUiVisibility
            val cleared = clearLightNavigationFlag(flags)
            if (cleared != flags) {
                @Suppress("DEPRECATION")
                decor.systemUiVisibility = cleared
            }
        }
        if (plan.clearAppearanceLightFlag && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            )
        }

        if (before != null) {
            Log.i(TAG, "apply reason=$reason before=$before after=${snapshot(activity)}")
        }
    }

    /**
     * Read-only state of the host window. No user data.
     * navigationBarColorReadback is TRANSPARENT (0) on edge-to-edge-enforced
     * windows (PhoneWindow.getNavigationBarColor), so it doubles as an
     * edge-to-edge indicator on API 35+.
     */
    fun snapshot(activity: Activity): Map<String, Any?> {
        val window: Window? = activity.window
        val decor: View? = window?.decorView
        val result = linkedMapOf<String, Any?>("sdkInt" to Build.VERSION.SDK_INT)
        if (window == null || decor == null) {
            result["window"] = false
            return result
        }
        @Suppress("DEPRECATION")
        val sysUi = decor.systemUiVisibility
        result["legacyLightNavFlag"] = (sysUi and LIGHT_NAVIGATION_BAR_FLAG) != 0
        @Suppress("DEPRECATION")
        result["navigationBarColorReadback"] = "#" + Integer.toHexString(window.navigationBarColor).uppercase().padStart(8, '0')
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            result["contrastEnforced"] = window.isNavigationBarContrastEnforced
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appearance = window.insetsController?.systemBarsAppearance
            result["appearance"] = appearance?.let { "0x" + Integer.toHexString(it).uppercase() }
            result["appearanceLightNav"] = appearance?.let {
                (it and WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS) != 0
            }
        }
        val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        result["osNightMode"] = night == Configuration.UI_MODE_NIGHT_YES
        return result
    }
}

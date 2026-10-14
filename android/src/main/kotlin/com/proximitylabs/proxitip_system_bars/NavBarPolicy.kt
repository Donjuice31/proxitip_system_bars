package com.proximitylabs.proxitip_system_bars

/**
 * Pure (Android-framework-free) configuration and policy, so it can be unit
 * tested on the JVM. Everything that touches a Window lives in NavBarApplier.
 */
internal data class NavBarConfig(
    /** Opaque ARGB colour for the navigation-bar area. */
    val color: Int,
    /** Window.setNavigationBarContrastEnforced (API 29+). */
    val contrastEnforced: Boolean,
    /** Re-apply on resume / window focus / configuration change. */
    val reapplyOnLifecycle: Boolean,
    /** Log one line per apply under [TAG]. Contains no user data. */
    val debugLogging: Boolean,
) {
    companion object {
        const val DEFAULT_COLOR: Int = 0xFF0D0D0F.toInt()

        /**
         * Parses the method-channel arguments. Returns null when the
         * arguments are malformed or the colour is not fully opaque (a
         * translucent colour would change how Android derives the scrim, which
         * this plugin does not want to depend on).
         */
        fun fromArguments(args: Any?): NavBarConfig? {
            val map = args as? Map<*, *> ?: return null
            val color = when (val raw = map["color"]) {
                null -> DEFAULT_COLOR
                is Number -> raw.toLong().toInt() // Dart ints > 2^31 arrive as Long
                else -> return null
            }
            if (!isOpaque(color)) return null
            return NavBarConfig(
                color = color,
                contrastEnforced = map["contrastEnforced"] as? Boolean ?: true,
                reapplyOnLifecycle = map["reapplyOnLifecycle"] as? Boolean ?: true,
                debugLogging = map["debugLogging"] as? Boolean ?: false,
            )
        }

        fun isOpaque(color: Int): Boolean = (color ushr 24) == 0xFF
    }
}

/** Which platform calls are made on a given API level. */
internal data class NavBarPlan(
    /** Window.setNavigationBarColor — all supported levels. */
    val setColor: Boolean,
    /** Window.setNavigationBarDividerColor — API 28+. */
    val setDividerColor: Boolean,
    /** Window.setNavigationBarContrastEnforced — API 29+. */
    val setContrast: Boolean,
    /** Clear View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR — API 26+. */
    val clearLegacyLightFlag: Boolean,
    /** WindowInsetsController APPEARANCE_LIGHT_NAVIGATION_BARS — API 30+. */
    val clearAppearanceLightFlag: Boolean,
) {
    companion object {
        fun forSdk(sdk: Int): NavBarPlan = NavBarPlan(
            setColor = true,
            setDividerColor = sdk >= 28,
            setContrast = sdk >= 29,
            clearLegacyLightFlag = sdk >= 26,
            clearAppearanceLightFlag = sdk >= 30,
        )
    }
}

/** View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR == WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS == 0x10. */
internal const val LIGHT_NAVIGATION_BAR_FLAG: Int = 0x10

internal fun clearLightNavigationFlag(flags: Int): Int = flags and LIGHT_NAVIGATION_BAR_FLAG.inv()

internal const val TAG = "ProxiTipSystemBars"

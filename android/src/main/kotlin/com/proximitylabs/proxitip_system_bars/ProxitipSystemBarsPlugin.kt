package com.proximitylabs.proxitip_system_bars

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Dark navigation bar with light buttons.
 *
 * Does nothing until Dart calls `applyDarkNavigationBar`. From then on (the
 * plugin is "armed") it re-applies to the host Activity's window:
 *  - immediately, and whenever an Activity is (re)attached to the engine;
 *  - after the host Activity resumes (posted, so it runs after Flutter's own
 *    onPostResume system-chrome re-apply);
 *  - when the host window regains focus (dialogs, notification shade,
 *    permission prompts — these do not pause the Activity);
 *  - on configuration change (OS Light/Dark switch: the host manifest lists
 *    uiMode in configChanges, so the Activity is not recreated).
 * Only the host Activity's window is ever touched; other activities (Stripe,
 * camera, browser) keep their own windows, and nothing runs while they are in
 * front because none of the triggers above fire for the host then.
 * No polling, no per-frame work.
 */
class ProxitipSystemBarsPlugin : FlutterPlugin, ActivityAware, MethodChannel.MethodCallHandler {

    private var channel: MethodChannel? = null
    private var activity: Activity? = null

    /** Non-null once Dart has requested the dark bar. */
    private var config: NavBarConfig? = null

    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var componentCallbacks: ComponentCallbacks? = null
    private var focusListener: ViewTreeObserver.OnWindowFocusChangeListener? = null
    private var focusObserver: ViewTreeObserver? = null

    // FlutterPlugin

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, CHANNEL).also {
            it.setMethodCallHandler(this)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
    }

    // ActivityAware

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        config?.let { applyNow(it, "attach") }
        registerHooksIfNeeded()
    }

    override fun onDetachedFromActivityForConfigChanges() = detachActivity()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) =
        onAttachedToActivity(binding)

    override fun onDetachedFromActivity() = detachActivity()

    private fun detachActivity() {
        unregisterHooks()
        activity = null
    }

    // MethodCallHandler (main thread)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "applyDarkNavigationBar" -> {
                val parsed = NavBarConfig.fromArguments(call.arguments)
                if (parsed == null) {
                    result.error("INVALID_ARGUMENTS", "Expected an opaque ARGB color and boolean flags.", null)
                    return
                }
                unregisterHooks()
                config = parsed
                val applied = activity?.let { applyNow(parsed, "dart") } ?: false
                registerHooksIfNeeded()
                // false = no Activity yet; it will be applied on attach.
                result.success(applied)
            }
            "stopReapplying" -> {
                unregisterHooks()
                config = null
                result.success(null)
            }
            "diagnostics" -> {
                val host = activity
                result.success(if (host == null) mapOf("activity" to false) else NavBarApplier.snapshot(host))
            }
            else -> result.notImplemented()
        }
    }

    // Internals

    private fun applyNow(cfg: NavBarConfig, reason: String): Boolean {
        val host = activity ?: return false
        return try {
            NavBarApplier.apply(host, cfg, reason)
            true
        } catch (e: RuntimeException) {
            // Cosmetic feature: never crash the host app.
            Log.w(TAG, "apply failed (reason=$reason)", e)
            false
        }
    }

    private fun registerHooksIfNeeded() {
        val host = activity ?: return
        val cfg = config ?: return
        if (!cfg.reapplyOnLifecycle || lifecycleCallbacks != null) return

        val lifecycle = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                if (a !== activity) return
                // Post so we run after FlutterActivity/Fragment onPostResume.
                a.window?.decorView?.post { config?.let { applyNow(it, "resume") } }
            }
            override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        }
        host.application.registerActivityLifecycleCallbacks(lifecycle)
        lifecycleCallbacks = lifecycle

        val component = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                activity?.window?.decorView?.post { config?.let { applyNow(it, "configuration") } }
            }
            @Deprecated("Deprecated in Android API 34")
            override fun onLowMemory() {}
        }
        host.registerComponentCallbacks(component)
        componentCallbacks = component

        val observer = host.window?.decorView?.viewTreeObserver
        if (observer != null && observer.isAlive) {
            val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                if (hasFocus) config?.let { applyNow(it, "focus") }
            }
            observer.addOnWindowFocusChangeListener(listener)
            focusListener = listener
            focusObserver = observer
        }
    }

    private fun unregisterHooks() {
        val host = activity
        lifecycleCallbacks?.let { host?.application?.unregisterActivityLifecycleCallbacks(it) }
        lifecycleCallbacks = null
        componentCallbacks?.let { host?.unregisterComponentCallbacks(it) }
        componentCallbacks = null
        val listener = focusListener
        if (listener != null) {
            // A pre-attach observer is merged into the live one and then dies,
            // so remove from the decor view's current observer as well.
            listOfNotNull(focusObserver, host?.window?.decorView?.viewTreeObserver)
                .filter { it.isAlive }
                .forEach { it.removeOnWindowFocusChangeListener(listener) }
        }
        focusObserver = null
        focusListener = null
    }

    private companion object {
        const val CHANNEL = "proxitip_system_bars"
    }
}

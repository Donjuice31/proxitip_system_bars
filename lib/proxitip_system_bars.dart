/// Dark Android navigation bar with light buttons.
///
/// Android-only. On every other platform (including web) each call is a
/// no-op, and the library does not import `dart:io`, so it compiles for web.
library;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Requests a dark navigation-bar appearance on the host Activity.
abstract final class ProxiTipSystemBars {
  /// Default navigation-bar colour (#0D0D0F).
  static const int defaultColor = 0xFF0D0D0F;

  @visibleForTesting
  static const MethodChannel channel = MethodChannel('proxitip_system_bars');

  static bool get _isAndroid =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  /// Asks Android for a dark navigation-bar area with light buttons.
  ///
  /// - [color] must be fully opaque ARGB.
  /// - [contrastEnforced] keeps the system 3-button scrim (API 29+).
  /// - [reapplyOnLifecycle] re-applies on resume, focus and configuration
  ///   change.
  /// - [debugLogging] writes one line per apply under the `ProxiTipSystemBars`
  ///   logcat tag.
  ///
  /// Returns `true` when it was applied to an attached Activity. It returns
  /// `false` off Android, for an invalid colour, when the plugin is missing,
  /// or when no Activity is attached yet; in that last case the request is
  /// applied as soon as one attaches.
  ///
  /// Never throws: this is a cosmetic call that runs during app startup.
  static Future<bool> applyDarkNavigationBar({
    int color = defaultColor,
    bool contrastEnforced = true,
    bool reapplyOnLifecycle = true,
    bool debugLogging = false,
  }) async {
    if (!_isAndroid) return false;
    if ((color >> 24) & 0xFF != 0xFF) {
      debugPrint('ProxiTipSystemBars: color must be opaque ARGB');
      return false;
    }
    try {
      final applied = await channel.invokeMethod<bool>('applyDarkNavigationBar', {
        'color': color,
        'contrastEnforced': contrastEnforced,
        'reapplyOnLifecycle': reapplyOnLifecycle,
        'debugLogging': debugLogging,
      });
      return applied ?? false;
    } on MissingPluginException {
      return false;
    } on PlatformException catch (e) {
      debugPrint('ProxiTipSystemBars: ${e.code} ${e.message}');
      return false;
    }
  }

  /// Stops lifecycle re-application. It does not restore the previous look.
  static Future<void> stopReapplying() async {
    if (!_isAndroid) return;
    try {
      await channel.invokeMethod<void>('stopReapplying');
    } on MissingPluginException {
      // Plugin not registered: nothing to stop.
    } on PlatformException catch (e) {
      debugPrint('ProxiTipSystemBars: ${e.code} ${e.message}');
    }
  }

  /// Read-only snapshot of the host window's navigation-bar state, for device
  /// diagnosis. It contains no user data. Returns an empty map off Android or
  /// on failure.
  static Future<Map<String, Object?>> diagnostics() async {
    if (!_isAndroid) return const {};
    try {
      final result = await channel.invokeMapMethod<String, Object?>('diagnostics');
      return result ?? const {};
    } on MissingPluginException {
      return const {};
    } on PlatformException {
      return const {};
    }
  }
}

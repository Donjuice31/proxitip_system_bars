# proxitip_system_bars

A small Flutter plugin that asks Android for a **dark navigation-bar area with light buttons** on the host app's own Activity window.

It was written for the ProxiTip app, which is dark-only. It contains only generic navigation-bar code and no app code.

> **Status:** version 0.1.0.
> - Verified with the `example/` harness on one Android 16 device, with 3-button and gesture navigation and with Light and Dark system themes.
> - Not yet verified inside a production app.
> - Not published to pub.dev. Depend on it via git, pinned to a commit.

## Why native code

- On Android 15+ (API 35+), Flutter's `SystemChrome.setSystemUIOverlayStyle` no longer calls `Window.setNavigationBarColor`: the engine gates that call at `SDK_INT < 35`.
- Some vendor builds give a window that never specifies a navigation-bar colour light buttons and a light 3-button scrim while the system theme is Light, even when the app itself is dark.
- Specifying the colour is only possible from Java/Kotlin, or from the app's Android theme.

## Installation

```yaml
dependencies:
  proxitip_system_bars:
    git:
      url: https://github.com/Donjuice31/proxitip_system_bars.git
      ref: <commit-sha>
```

## Dart API

```dart
import 'package:proxitip_system_bars/proxitip_system_bars.dart';

await ProxiTipSystemBars.applyDarkNavigationBar(); // #0D0D0F, contrast enforced
await ProxiTipSystemBars.diagnostics();            // read-only window snapshot
await ProxiTipSystemBars.stopReapplying();         // disarm lifecycle hooks
```

- Android only. Everywhere else, including web, every call is a no-op. The library does not import `dart:io`.
- `applyDarkNavigationBar` never throws. It returns `false` in these cases:
  - the plugin is missing;
  - the colour is not opaque;
  - the platform reports an error;
  - no Activity is attached yet. The request is then applied when one attaches.

## What it does (host Activity window only, main thread)

| Call | API | Expected effect |
|---|---|---|
| `addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)`, `clearFlags(FLAG_TRANSLUCENT_NAVIGATION)` | all | Required for any bar colour to be drawn. |
| `setNavigationBarColor(color)` | all (deprecated in 35, still functional) | ≤ 34 and non-edge-to-edge windows: an opaque bar. Edge-to-edge windows on 35+: marks the colour as *specified*; with contrast enforced, the 3-button scrim uses this colour. |
| `setNavigationBarDividerColor(TRANSPARENT)` | 28+ | No light divider line. |
| `setNavigationBarContrastEnforced(true)` | 29+ | Keeps the system 3-button scrim, for readability. Gesture navigation gets no scrim. |
| clear `SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR` | 26+ | Legacy light-button flag off. |
| `WindowInsetsController.setSystemBarsAppearance(0, APPEARANCE_LIGHT_NAVIGATION_BARS)` | 30+ | Light buttons, or a light gesture handle. |

- The status bar is untouched.
- No permissions, no network access, and no dependencies beyond the Flutter embedding.

## When it applies

1. **When Dart calls `applyDarkNavigationBar`.** For example, in `main()` before `runApp`.
2. **On lifecycle events afterwards.** Once called, the plugin is "armed" and re-applies:
   - when an Activity is attached or re-attached to the engine;
   - after the host Activity **resumes**. This is posted, so it runs after Flutter's own re-apply. It covers returning from other activities, such as payment sheets, the camera or a browser.
   - when the host window **regains focus**. Dialogs, the notification shade and permission prompts don't pause the Activity.
   - on a **configuration change**, such as a Light/Dark switch, when the manifest lists `uiMode` in `configChanges`.
3. **Never otherwise.** No polling and no per-frame work. Other activities' windows are never touched.

## Diagnostics

Pass `debugLogging: true` to log one line per apply, with no user data:

```
adb logcat -s ProxiTipSystemBars
```

Each line shows the reason for the apply and the state before and after it:
- `appearance` and `appearanceLightNav`
- `legacyLightNavFlag`
- `contrastEnforced`
- `navigationBarColorReadback`: `#00000000` means the window is edge-to-edge-enforced.
- `osNightMode`

## Tests

```
flutter test                                # Dart API
cd example/android
./gradlew :proxitip_system_bars:testDebugUnitTest :proxitip_system_bars:lintDebug
```

The `example/` harness uses Gradle 8.14, AGP 8.11.1, Kotlin 2.2.20, compileSdk/targetSdk 36, minSdk 24, and `FlutterFragmentActivity`.

## Build note

The plugin uses the classic AGP 8 layout: it applies `kotlin-android`. Newer Flutter templates use AGP 9 "built-in Kotlin". Migrate when the host apps do.

## License

MIT. See [LICENSE](LICENSE).

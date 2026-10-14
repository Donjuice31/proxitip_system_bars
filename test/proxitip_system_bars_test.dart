import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:proxitip_system_bars/proxitip_system_bars.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  final calls = <MethodCall>[];

  void handle(Future<Object?>? Function(MethodCall call) handler) {
    messenger.setMockMethodCallHandler(ProxiTipSystemBars.channel, (call) {
      calls.add(call);
      return handler(call);
    });
  }

  setUp(() {
    calls.clear();
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
  });

  tearDown(() {
    messenger.setMockMethodCallHandler(ProxiTipSystemBars.channel, null);
    debugDefaultTargetPlatformOverride = null;
  });

  test('sends the default dark request with safe defaults', () async {
    handle((_) async => true);
    expect(await ProxiTipSystemBars.applyDarkNavigationBar(), isTrue);
    expect(calls.single.method, 'applyDarkNavigationBar');
    expect(calls.single.arguments, {
      'color': 0xFF0D0D0F,
      'contrastEnforced': true,
      'reapplyOnLifecycle': true,
      'debugLogging': false,
    });
  });

  test('passes explicit options through', () async {
    handle((_) async => true);
    await ProxiTipSystemBars.applyDarkNavigationBar(
      color: 0xFF000000,
      contrastEnforced: false,
      reapplyOnLifecycle: false,
      debugLogging: true,
    );
    expect(calls.single.arguments, {
      'color': 0xFF000000,
      'contrastEnforced': false,
      'reapplyOnLifecycle': false,
      'debugLogging': true,
    });
  });

  test('reports not-yet-applied when no Activity is attached', () async {
    handle((_) async => false);
    expect(await ProxiTipSystemBars.applyDarkNavigationBar(), isFalse);
  });

  test('rejects a non-opaque colour without calling the platform', () async {
    handle((_) async => true);
    expect(
      await ProxiTipSystemBars.applyDarkNavigationBar(color: 0x800D0D0F),
      isFalse,
    );
    expect(calls, isEmpty);
  });

  test('never throws: missing plugin and platform errors return false', () async {
    // No handler registered → MissingPluginException.
    expect(await ProxiTipSystemBars.applyDarkNavigationBar(), isFalse);
    handle((_) async => throw PlatformException(code: 'INVALID_ARGUMENTS'));
    expect(await ProxiTipSystemBars.applyDarkNavigationBar(), isFalse);
    await ProxiTipSystemBars.stopReapplying();
    expect(await ProxiTipSystemBars.diagnostics(), isEmpty);
  });

  for (final platform in [TargetPlatform.iOS, TargetPlatform.macOS]) {
    test('is a no-op on ${platform.name}', () async {
      debugDefaultTargetPlatformOverride = platform;
      handle((_) async => true);
      expect(await ProxiTipSystemBars.applyDarkNavigationBar(), isFalse);
      await ProxiTipSystemBars.stopReapplying();
      expect(await ProxiTipSystemBars.diagnostics(), isEmpty);
      expect(calls, isEmpty);
    });
  }

  test('diagnostics returns the platform snapshot', () async {
    handle((call) async => {'sdkInt': 36, 'appearanceLightNav': false});
    expect(await ProxiTipSystemBars.diagnostics(),
        {'sdkInt': 36, 'appearanceLightNav': false});
    expect(calls.single.method, 'diagnostics');
  });

  test('stopReapplying calls the platform', () async {
    handle((_) async => null);
    await ProxiTipSystemBars.stopReapplying();
    expect(calls.single.method, 'stopReapplying');
  });
}

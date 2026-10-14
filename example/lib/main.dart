import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:proxitip_system_bars/proxitip_system_bars.dart';

// Isolated test harness: a dark screen that applies the plugin at startup
// (before runApp) and shows the native diagnostics. Not ProxiTip.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await ProxiTipSystemBars.applyDarkNavigationBar(debugLogging: true);
  runApp(const HarnessApp());
}

class HarnessApp extends StatelessWidget {
  const HarnessApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      themeMode: ThemeMode.dark,
      darkTheme: ThemeData(brightness: Brightness.dark, useMaterial3: false),
      home: const HarnessPage(),
    );
  }
}

class HarnessPage extends StatefulWidget {
  const HarnessPage({super.key});

  @override
  State<HarnessPage> createState() => _HarnessPageState();
}

class _HarnessPageState extends State<HarnessPage> {
  String _diagnostics = '';

  Future<void> _refresh() async {
    final snapshot = await ProxiTipSystemBars.diagnostics();
    setState(() => _diagnostics =
        const JsonEncoder.withIndent('  ').convert(snapshot));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0F),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const Text('ProxiTip system bars — harness'),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: () async {
                await ProxiTipSystemBars.applyDarkNavigationBar(debugLogging: true);
                await _refresh();
              },
              child: const Text('Apply (contrast enforced)'),
            ),
            ElevatedButton(
              onPressed: () async {
                await ProxiTipSystemBars.applyDarkNavigationBar(
                    contrastEnforced: false, debugLogging: true);
                await _refresh();
              },
              child: const Text('Apply (contrast off)'),
            ),
            ElevatedButton(onPressed: _refresh, child: const Text('Diagnostics')),
            const SizedBox(height: 12),
            SelectableText(_diagnostics),
          ],
        ),
      ),
    );
  }
}

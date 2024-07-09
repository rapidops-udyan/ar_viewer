import 'package:ar_viewer/ar_viewer.dart';
import 'package:flutter/material.dart';

class Demo extends StatefulWidget {
  const Demo({super.key});

  @override
  State<Demo> createState() => _DemoState();
}

class _DemoState extends State<Demo> {
  final _arViewerPlugin = ArViewer();

  @override
  Widget build(BuildContext context) {
    return const Scaffold();
  }
}

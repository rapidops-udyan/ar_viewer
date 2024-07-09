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
  void initState() {
    _arViewerPlugin.loadModel(
        "https://firebasestorage.googleapis.com/v0/b/flutter-ar-427312.appspot.com/o/sample_curtain.glb?alt=media&token=5f985ba1-2dca-477b-8c49-563abbf0830e");
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold();
  }
}

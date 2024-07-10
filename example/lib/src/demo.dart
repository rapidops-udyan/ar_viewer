import 'package:ar_viewer/ar_viewer.dart';
import 'package:flutter/material.dart';

class Demo extends StatefulWidget {
  const Demo({super.key});

  @override
  State<Demo> createState() => _DemoState();
}

class _DemoState extends State<Demo> {
  final _arViewerPlugin = ArViewer();
  final modelUrl =
      "https://firebasestorage.googleapis.com/v0/b/flutter-ar-427312.appspot.com/o/sample_curtain.glb?alt=media&token=5f985ba1-2dca-477b-8c49-563abbf0830e";
  final colors = [
    "#${Colors.red.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.blue.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.green.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.yellow.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.deepOrange.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.purple.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.teal.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.pink.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.indigo.value.toRadixString(16).padLeft(8, '0')}",
    "#${Colors.brown.value.toRadixString(16).padLeft(8, '0')}",
  ];

  @override
  void initState() {
    super.initState();
    _arViewerPlugin.loadModel(
      modelUrl: modelUrl,
      colors: colors,
    );
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold();
  }
}

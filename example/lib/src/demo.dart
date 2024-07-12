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
      "https://firebasestorage.googleapis.com/v0/b/fir-practice-7ec8c.appspot.com/o/Scene%20(3).glb?alt=media&token=81c068ea-f002-4d08-8157-f90a5e5845b4";
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
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        child: const Icon(Icons.chevron_right_rounded),
        onPressed: () => _arViewerPlugin.loadModel(
          modelUrl: modelUrl,
          colors: colors,
        ),
      ),
    );
  }
}

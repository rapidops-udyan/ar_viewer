import 'ar_viewer_platform_interface.dart';

class ArViewer {
  Future<String?> loadModel({
    required String modelUrl,
    required List<String> colors,
  }) {
    return ArViewerPlatform.instance.loadModel(
      modelUrl: modelUrl,
      colors: colors,
    );
  }
}

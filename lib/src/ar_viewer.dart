import 'ar_viewer_platform_interface.dart';

class ArViewer {
  Future<String?> loadModel(String modelUrl) {
    return ArViewerPlatform.instance.loadModel(modelUrl);
  }
}

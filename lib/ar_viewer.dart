
import 'ar_viewer_platform_interface.dart';

class ArViewer {
  Future<String?> getPlatformVersion() {
    return ArViewerPlatform.instance.getPlatformVersion();
  }
}

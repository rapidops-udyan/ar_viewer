import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'ar_viewer_platform_interface.dart';

/// An implementation of [ArViewerPlatform] that uses method channels.
class MethodChannelArViewer extends ArViewerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('ar_viewer');

  @override
  Future<String?> loadModel({
    required String modelUrl,
    required List<String> colors,
  }) async {
    final version = await methodChannel.invokeMethod<String>('loadModel', {
      "modelUrl": modelUrl,
      "colors": colors,
    });
    return version;
  }
}

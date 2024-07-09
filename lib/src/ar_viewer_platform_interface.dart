import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'ar_viewer_method_channel.dart';

abstract class ArViewerPlatform extends PlatformInterface {
  /// Constructs a ArViewerPlatform.
  ArViewerPlatform() : super(token: _token);

  static final Object _token = Object();

  static ArViewerPlatform _instance = MethodChannelArViewer();

  /// The default instance of [ArViewerPlatform] to use.
  ///
  /// Defaults to [MethodChannelArViewer].
  static ArViewerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [ArViewerPlatform] when
  /// they register themselves.
  static set instance(ArViewerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> loadModel(String modelUrl) {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}

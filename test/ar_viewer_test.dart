import 'package:flutter_test/flutter_test.dart';
import 'package:ar_viewer/ar_viewer.dart';
import 'package:ar_viewer/ar_viewer_platform_interface.dart';
import 'package:ar_viewer/ar_viewer_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockArViewerPlatform
    with MockPlatformInterfaceMixin
    implements ArViewerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final ArViewerPlatform initialPlatform = ArViewerPlatform.instance;

  test('$MethodChannelArViewer is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelArViewer>());
  });

  test('getPlatformVersion', () async {
    ArViewer arViewerPlugin = ArViewer();
    MockArViewerPlatform fakePlatform = MockArViewerPlatform();
    ArViewerPlatform.instance = fakePlatform;

    expect(await arViewerPlugin.getPlatformVersion(), '42');
  });
}

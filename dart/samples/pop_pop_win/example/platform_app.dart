library ppw_platform_web;

import 'dart:async';

import 'package:poppopwin/platform_target.dart';
import 'package:chrome_gen/gen/storage.dart';

class PlatformApp extends PlatformTarget {
  final StreamController _aboutController = new StreamController(sync: true);
  bool _about = false;

  PlatformApp() : super.base();

  @override
  Future clearValues() => storage.local.clear();

  @override
  Future setValue(String key, String value) =>
      storage.local.set({key : value});

  @override
  Future<String> getValue(String key) => storage.local.get(key)
        .then((Map<String, String> values) => values[key]);

  bool get renderBig => false;

  bool get showAbout => _about;

  Stream get aboutChanged => _aboutController.stream;

  void toggleAbout([bool value]) {
    assert(_about != null);
    if(value == null) {
      value = !_about;
    }
    _about = value;
    _aboutController.add(null);
  }
}

library ppw_platform_web;

import 'dart:async';

import 'package:poppopwin/platform_target.dart';
import 'package:chrome_gen/gen/storage.dart';

class PlatformApp extends PlatformTarget {

  PlatformApp() : super.base();

  @override
  Future clearValues() => storage.local.clear();

  @override
  Future setValue(String key, String value) =>
      storage.local.set({key : value});

  @override
  Future<String> getValue(String key) => storage.local.get(key)
        .then((Map<String, String> values) => values[key]);

}

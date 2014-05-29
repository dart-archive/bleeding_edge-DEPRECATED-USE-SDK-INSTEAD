// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library pop_pop_win.platform_chrome_app;

import 'dart:async';

import 'package:pop_pop_win/platform_target.dart';
import 'package:chrome/gen/storage.dart';

class PlatformChromeApp extends PlatformTarget {
  final StreamController _aboutController = new StreamController(sync: true);
  bool _about = false;

  int get size => 7;

  PlatformChromeApp(): super.base();

  Future clearValues() => storage.local.clear();

  Future setValue(String key, String value) => storage.local.set({key : value});

  Future<String> getValue(String key) => storage.local.get(key)
        .then((Map<String, String> values) => values[key]);

  bool get showAbout => _about;

  Stream get aboutChanged => _aboutController.stream;

  void toggleAbout([bool value]) {
    assert(_about != null);
    if (value == null) {
      value = !_about;
    }
    _about = value;
    _aboutController.add(null);
  }
}

// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library polymer_intl_test;

import 'dart:async';
import 'dart:html';
import 'package:polymer/polymer.dart';
import 'package:unittest/unittest.dart';
import 'package:unittest/html_config.dart';
import '../web/localized.dart';

/**
 * A trivial test to get the analyzer plugged into this sample. See
 * searchable_list_test.dart.
 */
main() {
  initPolymer();
  useHtmlConfiguration();

  setUp(() => Polymer.onReady);

  test('loading the element', () {
    return new Future(() {
      var localized = querySelector('localized-example');
      expect(localized is LocalizedExampleElement, isTrue);
    });
  });
}

// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library unittestTest;
import 'dart:isolate';
import 'dart:async';
import 'package:unittest/unittest.dart';

part 'unittest_test_utils.dart';

var testName = 'async exception test';

var testFunction = (_) {
  test(testName, () {
    expectAsync(() {});
    _defer(() { throw "error!"; });
  });
};

var expected = buildStatusString(0, 1, 0, testName, message: 'Caught error!');

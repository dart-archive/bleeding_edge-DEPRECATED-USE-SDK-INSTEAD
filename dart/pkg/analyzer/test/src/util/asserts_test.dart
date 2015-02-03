// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.src.util.asserts;

import 'package:analyzer/src/util/asserts.dart';
import 'package:unittest/unittest.dart';

import '../../reflective_tests.dart';


main() {
  groupSep = ' | ';
  runReflectiveTests(AnalysisTaskTest);
}


@reflectiveTest
class AnalysisTaskTest {
  void test_notNull_notNull() {
    notNull(this);
  }

  void test_notNull_null_hasDescription() {
    expect(() => notNull(null, 'desc'), throwsArgumentError);
  }

  void test_notNull_null_noDescription() {
    expect(() => notNull(null), throwsArgumentError);
  }
}

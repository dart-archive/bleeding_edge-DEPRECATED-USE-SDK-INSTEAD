// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.services.dependencies;

import 'package:unittest/unittest.dart';

import 'library_dependencies_test.dart' as library_dependencies_test;

/// Utility for manually running all tests.
main() {
  groupSep = ' | ';
  group('dependencies', () {
    library_dependencies_test.main();
  });
}

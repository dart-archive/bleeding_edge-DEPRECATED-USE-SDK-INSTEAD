// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.services.completion;

import 'package:unittest/unittest.dart';

import 'arglist_computer_test.dart' as arglist_test;
import 'combinator_computer_test.dart' as combinator_test;
import 'common_usage_computer_test.dart' as common_usage_computer_test;
import 'completion_computer_test.dart' as completion_computer_test;
import 'completion_manager_test.dart' as completion_manager_test;
import 'completion_target_test.dart' as completion_target_test;
import 'imported_computer_test.dart' as imported_test;
import 'invocation_computer_test.dart' as invocation_test;
import 'keyword_computer_test.dart' as keyword_test;
import 'local_computer_test.dart' as local_test;
import 'optype_test.dart' as optype_test;

/// Utility for manually running all tests.
main() {
  groupSep = ' | ';
  group('completion', () {
    arglist_test.main();
    combinator_test.main();
    common_usage_computer_test.main();
    completion_computer_test.main();
    completion_manager_test.main();
    completion_target_test.main();
    imported_test.main();
    keyword_test.main();
    invocation_test.main();
    local_test.main();
    optype_test.main();
  });
}

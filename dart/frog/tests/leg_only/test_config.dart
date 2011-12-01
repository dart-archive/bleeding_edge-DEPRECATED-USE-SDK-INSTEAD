// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("leg_only_test_config");

#import("../../../tools/testing/dart/test_suite.dart");

class LegOnlyTestSuite extends StandardTestSuite {
  LegOnlyTestSuite(Map configuration)
      : super(configuration,
              "frog/tests/leg_only/src",
              ["frog/tests/leg_only/leg_only.status"]);

  List<String> additionalOptions() => ['--leg_only'];
}

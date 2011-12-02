// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("leg_test_config");

#import("../../../tools/testing/dart/test_suite.dart");

class LegTestSuite extends StandardTestSuite {
  LegTestSuite(Map configuration)
      : super(configuration,
              "leg",
              "frog/tests/leg/src",
              ["frog/tests/leg/leg.status"]);
}

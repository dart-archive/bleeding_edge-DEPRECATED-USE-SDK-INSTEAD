// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("await_test_config");

#import("../../../tools/testing/dart/test_suite.dart");

class AwaitTestSuite extends StandardTestSuite {
  AwaitTestSuite(Map configuration)
      : super(configuration,
              "await",
              "frog/tests/await/src",
              ["frog/tests/await/await.status"]);

  List<String> additionalOptions() {
    // Support running the tests from the frog as well as the
    // top-level directory.
    var awaitFile = new File("await/awaitc.dart");
    if (!awaitFile.existsSync()) {
      awaitFile = new File("frog/await/awaitc.dart");
    }
    return [awaitFile.fullPathSync()];
  }
}

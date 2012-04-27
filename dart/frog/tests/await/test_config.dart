// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("await_test_config");

#import("../../../tools/testing/dart/test_suite.dart");

class AwaitTestSuite extends StandardTestSuite {
  AwaitTestSuite(Map configuration)
      : super(configuration,
              "await",
              "frog/tests/await",
              ["frog/tests/await/await.status"]);

  List<String> additionalOptions(String filename) {
    return ['$dartDir/frog/await/awaitc.dart'];
  }

  bool isTestFile(String filename) => filename.endsWith("_test.dart");
}

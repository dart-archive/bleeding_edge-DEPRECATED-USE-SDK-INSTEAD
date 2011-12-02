// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("samples_test_config");

#import("../../../tools/testing/dart/test_suite.dart");

class SamplesTestSuite extends StandardTestSuite {
  SamplesTestSuite(Map configuration)
      : super(configuration,
              "samples",
              "samples/tests/samples/src",
              ["samples/tests/samples/samples.status"]);

  void listRecursively() => true;
}

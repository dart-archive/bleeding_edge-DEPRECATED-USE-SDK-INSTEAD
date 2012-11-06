// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("test_sample_extension");

#import("sample_synchronous_extension.dart");

// TODO(3008): Run this test automatically on buildbot (dart:3008).
void main() {
  systemSrand(17);
  var x1 = systemRand();
  var x2 = systemRand();
  var x3 = systemRand();
  Expect.notEquals(x1, x2);
  Expect.notEquals(x1, x3);
  Expect.notEquals(x2, x3);
  systemSrand(17);
  Expect.equals(x1, systemRand());
  Expect.equals(x2, systemRand());
  Expect.equals(x3, systemRand());
  systemSrand(18);
  Expect.notEquals(x1, systemRand());
  Expect.notEquals(x2, systemRand());
  Expect.notEquals(x3, systemRand());
}

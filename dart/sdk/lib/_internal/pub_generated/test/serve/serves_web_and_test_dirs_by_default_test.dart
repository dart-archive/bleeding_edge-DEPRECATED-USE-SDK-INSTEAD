// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS d.file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub_tests;

import '../descriptor.dart' as d;
import '../test_pub.dart';
import 'utils.dart';

main() {
  initConfig();
  integration("serves web/ and test/ dirs by default", () {
    d.dir(
        appPath,
        [
            d.appPubspec(),
            d.dir("web", [d.file("foo", "contents")]),
            d.dir("test", [d.file("bar", "contents")]),
            d.dir("example", [d.file("baz", "contents")])]).create();

    pubServe();
    requestShouldSucceed("foo", "contents", root: "web");
    requestShouldSucceed("bar", "contents", root: "test");
    requestShould404("baz", root: "web");
    endPubServe();
  });
}

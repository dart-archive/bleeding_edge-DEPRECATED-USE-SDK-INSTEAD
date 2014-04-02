// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library benchmarksmoketest;

// Tests that benchmark classes used in perf testing are not broken.
import 'benchmark_lib.dart';
import 'dart:async';
import 'dart:html';
import '../../pkg/expect/lib/expect.dart';
import '../../pkg/unittest/lib/unittest.dart';
import '../../pkg/unittest/lib/html_config.dart';

void main() {
  useHtmlConfiguration();

  test('performanceTesting', () {
    Timer.run(BENCHMARK_SUITE.runBenchmarks);
    Timer.run(expectAsync(testForCompletion));
  });
}

testForCompletion() {
  Element element = document.query('#testResultScore');
  RegExp re = new RegExp('Score: [0-9]+');
  print(element.text);
  Expect.isTrue(re.hasMatch(element.text));
}

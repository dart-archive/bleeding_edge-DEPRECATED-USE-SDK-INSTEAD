// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// If caching is working properly, the coverage data will go into the same
// Script object from which we requested coverage data, instead of a new
// Script object.

library caching_test;

import 'package:observatory/service_io.dart';
import 'package:unittest/unittest.dart';
import 'test_helper.dart';
import 'dart:async';

script() {
  print("This executed");
}

hasSomeCoverageData(Script script) {
  for (var line in script.lines) {
    if (line.hits != null) return true;
  }
  return false;
}

var tests = [
(Isolate isolate) async {
  Library lib = await isolate.rootLib.load();
  Script script = await lib.scripts.single.load();
  expect(hasSomeCoverageData(script), isFalse);
  Script script2 = await script.refreshCoverage();
  expect(identical(script, script2), isTrue);
  expect(hasSomeCoverageData(script), isTrue);
},

];

main(args) => runIsolateTests(args, tests, testeeBefore: script);

// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// VMOptions=--compile-all --error_on_bad_type --error_on_bad_override

import 'dart:async';

import 'package:observatory/service_io.dart';
import 'package:unittest/unittest.dart';

import 'test_helper.dart';

var tests = [
  (Isolate isolate) async {
    var params = {
    };
    var result = await isolate.invokeRpcNoUpgrade('getHeapMap', params);
    expect(result['type'], equals('HeapMap'));
    expect(result['freeClassId'], isPositive);
    expect(result['unitSizeBytes'], isPositive);
    expect(result['pageSizeBytes'], isPositive);
    expect(result['classList'], isNotNull);
    expect(result['pages'].length, isPositive);
    expect(result['pages'][0]['objectStart'], new isInstanceOf<String>());
    expect(result['pages'][0]['objects'].length, isPositive);
    expect(result['pages'][0]['objects'][0], isPositive);
  },
];

main(args) async => runIsolateTests(args, tests);

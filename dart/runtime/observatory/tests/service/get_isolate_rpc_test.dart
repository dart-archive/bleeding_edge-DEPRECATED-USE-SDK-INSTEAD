// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// VMOptions=--compile-all --error_on_bad_type --error_on_bad_override

import 'dart:async';

import 'package:observatory/service_io.dart';
import 'package:unittest/unittest.dart';

import 'test_helper.dart';

var tests = [
  (VM vm) async {
    var params = {
      'isolateId': vm.isolates.first.id,
    };
    var result = await vm.invokeRpcNoUpgrade('getIsolate', params);
    expect(result['type'], equals('Isolate'));
    expect(result['id'], startsWith('isolates/'));
    expect(result['number'], new isInstanceOf<String>());
    expect(result['startTime'], isPositive);
    expect(result['livePorts'], isPositive);
    expect(result['pauseOnExit'], isFalse);
    expect(result['pauseEvent']['type'], equals('ServiceEvent'));
    expect(result['error'], isNull);
    expect(result['rootLib']['type'], equals('@Library'));
    expect(result['libraries'].length, isPositive);
    expect(result['libraries'][0]['type'], equals('@Library'));
    expect(result['breakpoints'].length, isZero);
    expect(result['features'].length, isPositive);
    expect(result['features'][0], new isInstanceOf<String>());
    expect(result['heaps']['new']['type'], equals('HeapSpace'));
    expect(result['heaps']['old']['type'], equals('HeapSpace'));
  },
];

main(args) async => runVMTests(args, tests);

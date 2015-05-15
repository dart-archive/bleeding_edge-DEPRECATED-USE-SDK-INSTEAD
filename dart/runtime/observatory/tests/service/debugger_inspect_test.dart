// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// VMOptions=--compile-all --error_on_bad_type --error_on_bad_override

import 'package:observatory/service_io.dart';
import 'package:unittest/unittest.dart';
import 'test_helper.dart';
import 'dart:async';
import 'dart:developer';

class Point {
  int x, y;
  Point(this.x, this.y);
}

void testeeDo() {
  inspect(new Point(3, 4));
}

var tests = [

(Isolate isolate) async {
  Completer completer = new Completer();
  var subscription;
  subscription = isolate.vm.events.stream.listen((ServiceEvent event) {
    print(event);
    if (event.eventType == ServiceEvent.kInspect) {
      expect((event.inspectee as Instance).clazz.name, equals('Point'));

      subscription.cancel();
      completer.complete();
    }
  });

  // Start listening for events first.
  await isolate.rootLibrary.evaluate('testeeDo();');
  return completer.future;
},

];

main(args) => runIsolateTests(args, tests);

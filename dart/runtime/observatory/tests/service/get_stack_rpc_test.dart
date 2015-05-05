// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// VMOptions=--compile-all --error_on_bad_type --error_on_bad_override

import 'package:observatory/service_io.dart';
import 'package:unittest/unittest.dart';
import 'test_helper.dart';
import 'dart:async';
import 'dart:isolate' as isolate;
import 'dart:debugger' as debugger;

int counter = 0;
const stoppedAtLine = 23;
var port = new isolate.RawReceivePort(msgHandler);

// This name is used in a test below.
void msgHandler(_) { }

void periodicTask(_) {
  port.sendPort.send(34);
  debugger.Debugger.breakHere(); // We will be at a breakpoint at the next line.
  counter++;
  if (counter % 300 == 0) {
    print('counter = $counter');
  }
}

void startTimer() {
  new Timer.periodic(const Duration(milliseconds:10), periodicTask);
}

var tests = [

// Initial data fetch and verify we've hit the breakpoint.
(Isolate isolate) async {
  await isolate.rootLib.load();
  var script = isolate.rootLib.scripts[0];
  await script.load();
  await hasStoppedAtBreakpoint(isolate);
  // Sanity check.
  expect(isolate.pauseEvent.eventType, equals(ServiceEvent.kPauseBreakpoint));
},

// Get stack
(Isolate isolate) async {
  var stack = await isolate.getStack();
  expect(stack.type, equals('Stack'));

  // Sanity check.
  expect(stack['frames'].length, greaterThanOrEqualTo(1));
  Script script = stack['frames'][0]['script'];
  expect(script.tokenToLine(stack['frames'][0]['tokenPos']),
         equals(stoppedAtLine));

  // Iterate over frames.
  var frameDepth = 0;
  for (var frame in stack['frames']) {
    print('checking frame $frameDepth');
    expect(frame.type, equals('Frame'));
    expect(frame['depth'], equals(frameDepth++));
    expect(frame['code'].type, equals('Code'));
    expect(frame['function'].type, equals('Function'));
    expect(frame['script'].type, equals('Script'));
    expect(frame['tokenPos'], isNotNull);
  }

  // Sanity check.
  expect(stack['messages'].length, greaterThanOrEqualTo(1));

  // Iterate over messages.
  var messageDepth = 0;
  // objectId of message to be handled by msgHandler.
  var msgHandlerObjectId;
  for (var message in stack['messages']) {
    print('checking message $messageDepth');
    expect(message.type, equals('Message'));
    expect(message['_destinationPort'], isNotNull);
    expect(message['depth'], equals(messageDepth++));
    expect(message['name'], isNotNull);
    expect(message['size'], greaterThanOrEqualTo(1));
    expect(message['priority'], isNotNull);
    expect(message['handlerFunction'].type, equals('Function'));
    if (message['handlerFunction'].name.contains('msgHandler')) {
      msgHandlerObjectId = message['messageObjectId'];
    }
  }
  expect(msgHandlerObjectId, isNotNull);

  // Get object.
  var object = await isolate.getObject(msgHandlerObjectId);
  expect(object.valueAsString, equals('34'));
}

];

main(args) => runIsolateTests(args, tests, testeeBefore: startTimer);

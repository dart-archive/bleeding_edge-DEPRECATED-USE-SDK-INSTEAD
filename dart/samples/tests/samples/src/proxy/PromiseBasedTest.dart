// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("PromiseBasedTest");
#import("dart:isolate");
#import("../../../../proxy/promise.dart");
#import("../../../../../tests/isolate/src/TestFramework.dart");

class TestIsolate extends Isolate {

  TestIsolate() : super();

  void main() {
    int seed = 0;
    this.port.receive((var message, SendPort replyTo) {
      //print("Got ${message[0]}");
      if (seed == 0) {
        seed = message[0];
      } else {
        Promise<int> response = new Promise<int>();
        var proxy = new Proxy.forPort(replyTo);
        //print("send to proxy");
        proxy.send([response]);
        //print("sent");
        response.complete(seed + message[0]);
        this.port.close();
      }
    });
  }

}

Future promiseToFuture(Promise p) {
  Completer c = new Completer();
  p.then((v) { c.complete(v); });
  return c.future;
}

void test(TestExpectation expect) {
  Proxy proxy = new Proxy.forIsolate(new TestIsolate());
  proxy.send([42]);  // Seed the isolate.
  Promise<int> result = new PromiseProxy<int>(proxy.call([87]));
  Completer completer = new Completer();
  expect.completes(promiseToFuture(result)).then((int value) {
    //print("expect 1: $value");
    Expect.equals(42 + 87, value);
    completer.complete(99);
  });
  expect.completes(completer.future).then((int value) {
    //print("expect 2: $value");
    Expect.equals(99, value);
    expect.succeeded();
  });
}

void expandedTest(TestExpectation expect) {
  Proxy proxy = new Proxy.forIsolate(new TestIsolate());
  proxy.send([42]);  // Seed the isolate.
  Promise<SendPort> sendCompleter = proxy.call([87]);
  Promise<int> result = new Promise<int>();
  ReceivePort receivePort = new ReceivePort();
  receivePort.receive((var msg, SendPort _) {
    receivePort.close();
    //print("test completer");
    result.complete(msg[0]);
  });
  sendCompleter.addCompleteHandler((SendPort port) {
    //print("test send");
    port.send([receivePort.toSendPort()], null);
  });
  Completer completer = new Completer();
  expect.completes(promiseToFuture(result)).then((int value) {
    //print("expect 1: $value");
    Expect.equals(42 + 87, value);
    completer.complete(99);
  });
  expect.completes(completer.future).then((int value) {
    //print("expect 2: $value");
    Expect.equals(99, value);
    expect.succeeded();
  });
}

void main() {
  runTests([test, expandedTest]);
}

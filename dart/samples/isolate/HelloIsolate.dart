// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
//
// A minimal example of Dart isolates, standalone without use of browser.

// An isolate that responds with a greeting to each message it receives.
class HelloIsolate extends Isolate {
  HelloIsolate() {}

  void main() {
    // Whenever we receive a message, expect that it contains a name,
    // and send back a greeting personalized to that name.
    port.receive((String recipientName, SendPort replyTo) {
      replyTo.send("hello ${recipientName}");
    });
  }
}

void main() {
  final receivePort = new ReceivePort();
  receivePort.receive((String message, SendPort notUsedHere) {
    print("Received message: $message");
    receivePort.close();
  });

  new HelloIsolate().spawn().then((SendPort sendPort) {
    sendPort.send("sailor", receivePort.toSendPort());
  });

  // It's OK for us to return now, while messages are still in flight.
  // The event loop will keep running until they all land.
  //
  // When there are no more messages pending, the current isolates
  // implementation will expect all of our ports to be closed, which
  // is why we close our receivePort.
  print("Returning.");
}

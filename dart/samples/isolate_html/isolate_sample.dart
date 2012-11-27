// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library isolate_sample;

import 'dart:html';
import 'dart:isolate';
import 'dart:math';

/*
 * This is a simple sample application showing how to create two isolates
 * and send messages to them, and receive replies back.
 */

/**
 * These are the messages we are going to send to the isolates that we create.
 */
class MessageId {
  static const INIT = 'init';
  static const GREETING = 'greeting';
}

/**
 * Create a new isolate with a given name.  (In this sample app
 * we only have two isolates, and they are named 'A' and 'B').
 * Note, isolates aren't normally named, but it's useful to give
 * them names in this app so we can show which isolate is doing
 * what.
 */
SendPort createIsolate(String name) {
  var sendPort = spawnDomFunction(isolateMain);
  var message = {
    'id' : MessageId.INIT,
    'args' : [name, port.toSendPort()]
  };
  sendPort.send(message, null);
  return sendPort;
}

// TODO(mattsh) get this off the System object once it's available
// see http://dartbug.com/3357
bool isVm() => 1234567890123456789 % 2 > 0;

/**
 * This function will run in a separate isolate, which shares almost
 * no state with the main isolate. They will both run in the main
 * UI thread, though, so that they can share DOM state.
 */
void isolateMain() {
  Element div;
  String isolateName;
  SendPort chirpPort;

  void init(String isolateName_, SendPort chirpPort_) {
    isolateName = isolateName_;
    chirpPort = chirpPort_;
    div = new DivElement()
      ..classes = ['isolate', 'isolate${isolateName}']
      ..innerHtml = query('#isolateTemplate').innerHtml
      ..query('.isolateName').text = isolateName
      ..query('.chirpButton').on.click.add((event) {
          chirpPort.send(
            'this is a chirp message from isolate $isolateName', null);
        });
    query('#isolateParent').nodes.add(div);
  }

  /**
   * Display the message we received, and send back a simple reply (unless
   * the user has unchecked the reply checkbox).
   */
  void greeting(String message, SendPort replyTo) {
    div.query('.messageBox').innerHtml =
      'received message: <span class="messageText">"${message}"</span>';
    if (div.query('input.replyCheckbox').checked) {
      InputElement element = div.query('.delayTextbox');
      int millis = parseInt(element.value);
      // TODO(justinfagnani): use Timer when it works in isolates in dart2js
      // see: http://dartbug.com/4997
      window.setTimeout(() {
        replyTo.send('this is a reply from isolate "${isolateName}"', null);
      }, millis);
    }
  }

  port.receive((message, SendPort replyTo) {
    switch(message['id']) {
      case MessageId.INIT:
        init(message['args'][0], message['args'][1]);
      break;
      case MessageId.GREETING:
        greeting(message['args'][0], replyTo);
      break;
    }
  });
}

main() {
  //Map from isolate name to the port used to send messages to that isolate.
  final Map<String, SendPort> ports = new Map();

  // Do initialization and set up event handlers.
  query('#appTitle').text = 'Hello, isolates.';
  query('#vmStatus').text = '${isVm()}';

  var replyElement = query('.isolateMain .replyText');

  ports['A'] = createIsolate('A');
  ports['B'] = createIsolate('B');

  for (var element in queryAll('.sendButton')) {
    element.on.click.add((Event e) {
      replyElement.text = 'waiting for reply...';

      var isolateName =
          (e.currentTarget as Element).attributes['data-isolate-name'];
      var greeting = query('input#greetingText').value;
      var message = {'id': MessageId.GREETING, 'args': [greeting]};
      ports[isolateName].call(message).then((var msg) {
        replyElement.text = msg;
      });
    });
  }

  port.receive((var message, SendPort replyTo) {
    replyElement.text = message;
  });
}

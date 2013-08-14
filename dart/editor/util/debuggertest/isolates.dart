
library isolates;

import 'dart:async';
import 'dart:isolate';

String strVar = 'foo';
int intVar = 123;

void main() {
  print('main isolate started');

  SendPort sendPort = spawnFunction(isolateEntry);

  ReceivePort receivePort = new ReceivePort();

  sendPort.send('hey', receivePort.toSendPort());

  //String foo = 1.0;

  receivePort.receive((message, _) {
    print('main isolate received: $message');
    receivePort.close();
    print('main isolate exiting');
  });
}

void isolateEntry() {
  print('child isolate started');

  port.receive((message, SendPort port) {
    strVar = message;
    print('child isolate received: $message');
    //String foo = 1.0;
    port.send('there');
    print('child isolate exiting');
  });
}

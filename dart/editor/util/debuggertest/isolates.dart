
library isolates;

import 'dart:isolate';

String strVar = 'foo';
int intVar = 123;

void main() {
  print('main isolate started');

  ReceivePort receivePort = new ReceivePort();

  Isolate.spawn(isolateEntry, receivePort.sendPort).then((Isolate isolate) {
    SendPort port = receivePort.sendPort;

    port.send('hey');

    receivePort.listen((message) {
      print('main isolate received: $message');
      receivePort.close();
      print('main isolate exiting');
    });
  });
}

void isolateEntry(SendPort port) {
  print('child isolate started');
  port.send('hey there');
  print('child isolate exiting');
}

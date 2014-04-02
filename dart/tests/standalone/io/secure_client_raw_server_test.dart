// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
//
// VMOptions=
// VMOptions=--short_socket_read
// VMOptions=--short_socket_write
// VMOptions=--short_socket_read --short_socket_write

import "dart:async";
import "dart:io";

import "package:async_helper/async_helper.dart";
import "package:expect/expect.dart";

InternetAddress HOST;
const CERTIFICATE = "localhost_cert";
Future<RawSecureServerSocket> startEchoServer() {
  return RawSecureServerSocket.bind(HOST,
                                    0,
                                    CERTIFICATE).then((server) {
    server.listen((RawSecureSocket client) {
      List<List<int>> readChunks = <List<int>>[];
      List<int> dataToWrite = null;
      int bytesWritten = 0;
      client.writeEventsEnabled = false;
      client.listen((event) {
        switch (event) {
          case RawSocketEvent.READ:
            Expect.isTrue(bytesWritten == 0);
            Expect.isTrue(client.available() > 0);
            readChunks.add(client.read());
            break;
          case RawSocketEvent.WRITE:
            Expect.isFalse(client.writeEventsEnabled);
            Expect.isNotNull(dataToWrite);
            bytesWritten += client.write(
                dataToWrite, bytesWritten, dataToWrite.length - bytesWritten);
            if (bytesWritten < dataToWrite.length) {
              client.writeEventsEnabled = true;
            }
            if (bytesWritten == dataToWrite.length) {
              client.shutdown(SocketDirection.SEND);
            }
            break;
          case RawSocketEvent.READ_CLOSED:
            dataToWrite = readChunks.fold(<int>[], (list, x) {
              list.addAll(x);
              return list;
            });
            client.writeEventsEnabled = true;
            break;
        }
      });
    });
    return server;
  });
}

Future testClient(server) {
  Completer success = new Completer();
  List<String> chunks = <String>[];
  SecureSocket.connect(HOST, server.port).then((socket) {
    socket.write("Hello server.");
    socket.close();
    socket.listen(
      (List<int> data) {
        var received = new String.fromCharCodes(data);
        chunks.add(received);
      },
      onDone: () {
        String reply = chunks.join();
        Expect.equals("Hello server.", reply);
        success.complete(server);
      });
  });
  return success.future;
}

void main() {
  asyncStart();
  String certificateDatabase = Platform.script.resolve('pkcert').toFilePath();
  SecureSocket.initialize(database: certificateDatabase,
                          password: 'dartdart');
  InternetAddress.lookup("localhost").then((hosts) => HOST = hosts.first)
      .then((_) => startEchoServer())
      .then(testClient)
      .then((server) => server.close())
      .then((_) => asyncEnd());
}

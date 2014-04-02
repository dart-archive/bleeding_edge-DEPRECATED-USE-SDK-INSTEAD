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

void testSimpleBind() {
  asyncStart();
  SecureServerSocket.bind(HOST, 0, CERTIFICATE).then((s) {
    Expect.isTrue(s.port > 0);
    s.close();
    asyncEnd();
  });
}

void testInvalidBind() {
  int count = 0;

  // Bind to a unknown DNS name.
  asyncStart();
  SecureServerSocket.bind("ko.faar.__hest__", 0, CERTIFICATE).then((_) {
    Expect.fail("Failure expected");
  }).catchError((error) {
    Expect.isTrue(error is SocketException);
    asyncEnd();
  });

  // Bind to an unavaliable IP-address.
  asyncStart();
  SecureServerSocket.bind("8.8.8.8", 0, CERTIFICATE).then((_) {
    Expect.fail("Failure expected");
  }).catchError((error) {
    Expect.isTrue(error is SocketException);
    asyncEnd();
  });

  // Bind to a port already in use.
  asyncStart();
  SecureServerSocket.bind(HOST, 0, CERTIFICATE).then((s) {
    SecureServerSocket.bind(HOST,
                            s.port,
                            CERTIFICATE).then((t) {
      Expect.fail("Multiple listens on same port");
    }).catchError((error) {
      Expect.isTrue(error is SocketException);
      s.close();
      asyncEnd();
    });
  });
}

void testSimpleConnect(String certificate) {
  asyncStart();
  SecureServerSocket.bind(HOST, 0, certificate).then((server) {
    var clientEndFuture = SecureSocket.connect(HOST, server.port);
    server.listen((serverEnd) {
      clientEndFuture.then((clientEnd) {
        clientEnd.close();
        serverEnd.close();
        server.close();
        asyncEnd();
      });
    });
  });
}

void testSimpleConnectFail(String certificate, bool cancelOnError) {
  asyncStart();
  SecureServerSocket.bind(HOST, 0, certificate).then((server) {
    var clientEndFuture = SecureSocket.connect(HOST, server.port)
      .then((clientEnd) {
        Expect.fail("No client connection expected.");
      })
      .catchError((error) {
        Expect.isTrue(error is HandshakeException ||
                      error is SocketException);
      });
    server.listen((serverEnd) {
      Expect.fail("No server connection expected.");
    },
    onError: (error) {
      Expect.isTrue(error is CertificateException);
      clientEndFuture.then((_) {
        if (!cancelOnError) server.close();
        asyncEnd();
      });
    },
    cancelOnError: cancelOnError);
  });
}

void testServerListenAfterConnect() {
  asyncStart();
  SecureServerSocket.bind(HOST, 0, CERTIFICATE).then((server) {
    Expect.isTrue(server.port > 0);
    var clientEndFuture = SecureSocket.connect(HOST, server.port);
    new Timer(const Duration(milliseconds: 500), () {
      server.listen((serverEnd) {
        clientEndFuture.then((clientEnd) {
          clientEnd.close();
          serverEnd.close();
          server.close();
          asyncEnd();
        });
      });
    });
  });
}

void testSimpleReadWrite() {
  // This test creates a server and a client connects. The client then
  // writes and the server echos. When the server has finished its
  // echo it half-closes. When the client gets the close event is
  // closes fully.
  asyncStart();

  const messageSize = 1000;

  List<int> createTestData() {
    List<int> data = new List<int>(messageSize);
    for (int i = 0; i < messageSize; i++) {
      data[i] = i & 0xff;
    }
    return data;
  }

  void verifyTestData(List<int> data) {
    Expect.equals(messageSize, data.length);
    List<int> expected = createTestData();
    for (int i = 0; i < messageSize; i++) {
      Expect.equals(expected[i], data[i]);
    }
  }

  SecureServerSocket.bind(HOST, 0, CERTIFICATE).then((server) {
    server.listen((client) {
      int bytesRead = 0;
      int bytesWritten = 0;
      List<int> data = new List<int>(messageSize);

      client.listen(
        (buffer) {
          Expect.isTrue(bytesWritten == 0);
          data.setRange(bytesRead, bytesRead + buffer.length, buffer);
          bytesRead += buffer.length;
          if (bytesRead == data.length) {
            verifyTestData(data);
            client.add(data);
            client.close();
          }
        },
        onDone: () {
          server.close();
        });
    });

    SecureSocket.connect(HOST, server.port).then((socket) {
      int bytesRead = 0;
      int bytesWritten = 0;
      List<int> dataSent = createTestData();
      List<int> dataReceived = new List<int>(dataSent.length);
      socket.add(dataSent);
      socket.close();  // Can also be delayed.
      socket.listen(
        (List<int> buffer) {
          dataReceived.setRange(bytesRead, bytesRead + buffer.length, buffer);
          bytesRead += buffer.length;
        },
        onDone: () {
          verifyTestData(dataReceived);
          socket.close();
          asyncEnd();
        });
    });
  });
}

main() {
  asyncStart();
  String certificateDatabase = Platform.script.resolve('pkcert').toFilePath();
  SecureSocket.initialize(database: certificateDatabase,
                          password: 'dartdart',
                          useBuiltinRoots: false);
  InternetAddress.lookup("localhost").then((hosts) {
    HOST = hosts.first;
    runTests();
    asyncEnd();
  });
}

runTests() {
  testSimpleBind();
  testInvalidBind();
  testSimpleConnect(CERTIFICATE);
  testSimpleConnect("CN=localhost");
  testSimpleConnectFail("not_a_nickname", false);
  testSimpleConnectFail("CN=notARealDistinguishedName", false);
  testSimpleConnectFail("not_a_nickname", true);
  testSimpleConnectFail("CN=notARealDistinguishedName", true);
  testServerListenAfterConnect();
  testSimpleReadWrite();
}

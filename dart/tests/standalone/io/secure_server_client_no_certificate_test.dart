// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import "dart:async";
import "dart:io";

import "package:async_helper/async_helper.dart";
import "package:expect/expect.dart";

InternetAddress HOST;
const CERTIFICATE = "localhost_cert";

Future testNoClientCertificate() {
  var completer = new Completer();
  SecureServerSocket.bind(HOST,
                          0,
                          CERTIFICATE,
                          requestClientCertificate: true).then((server) {
    var clientEndFuture = SecureSocket.connect(HOST,
                                               server.port);
    server.listen((serverEnd) {
      X509Certificate certificate = serverEnd.peerCertificate;
      Expect.isNull(certificate);
      clientEndFuture.then((clientEnd) {
        clientEnd.close();
        serverEnd.close();
        server.close();
        completer.complete();
      });
    });
  });
  return completer.future;
}

Future testNoRequiredClientCertificate() {
  var completer = new Completer();
  bool clientError = false;
  SecureServerSocket.bind(HOST,
                          0,
                          CERTIFICATE,
                          requireClientCertificate: true).then((server) {
    Future clientDone = SecureSocket.connect(HOST, server.port)
      .catchError((e) { clientError = true; });
    server.listen((serverEnd) {
      Expect.fail("Got a unverifiable connection");
    },
    onError: (e) {
      clientDone.then((_) {
        Expect.isTrue(clientError);
        server.close();
        completer.complete();
      });
    });
  });
  return completer.future;
}

void main() {
  String certificateDatabase = Platform.script.resolve('pkcert').toFilePath();
  SecureSocket.initialize(database: certificateDatabase,
                          password: 'dartdart',
                          useBuiltinRoots: false);

  asyncStart();
  InternetAddress.lookup("localhost").then((hosts) => HOST = hosts.first)
    .then((_) => testNoRequiredClientCertificate())
    .then((_) => testNoClientCertificate())
    .then((_) => asyncEnd());
}

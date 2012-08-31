// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("chat_server.dart");
#import("dart:io");
#import("dart:isolate");
#import("chat_server_lib.dart");


const DEFAULT_PORT = 8123;
const DEFAULT_HOST = "127.0.0.1";

void main() {
  // For profiling stopping after some time is convenient. Set
  // stopAfter for that.
  int stopAfter;

  var serverPort = spawnFunction(startChatServer);
  ServerMain serverMain =
      new ServerMain.start(serverPort, DEFAULT_HOST, DEFAULT_PORT);

  // Start a shutdown timer if requested.
  if (stopAfter != null) {
    new Timer(stopAfter * 1000, (timer) => serverMain.shutdown());
  }
}

// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("chat_server_lib.dart");


void main() {
  // For profiling stopping after some time is convenient. Set
  // stopAfter for that.
  int stopAfter;

  ServerMain serverMain = new ServerMain.start();

  // Start a shutdown timer if requested.
  if (stopAfter != null) {
    new Timer((timer) => serverMain.shutdown(), stopAfter * 1000, false);
  }
}

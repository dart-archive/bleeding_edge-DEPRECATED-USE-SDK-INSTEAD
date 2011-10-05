// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Logger {
  static void log(Object message) {
    print(message.toString());
  }

  static void error(Object message) {
    print("ERROR: ${message}");
  }
}

// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('tty');
#import('node.dart');

// module tty

class tty native "require('tty')" {
  static bool isatty(int fd) native;
  static void setRawMode(bool mode) native;
}

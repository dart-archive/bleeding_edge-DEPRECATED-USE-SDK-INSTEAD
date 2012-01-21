// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('path');

class path native "require('path')" {
  static bool existsSync(String filename) native;
  static String dirname(String path) native;
  static String basename(String path) native;
  static String extname(String path) native;
  static String normalize(String path) native;
  // TODO(jimhug): Get the right signatures for normalizeArray and join
}


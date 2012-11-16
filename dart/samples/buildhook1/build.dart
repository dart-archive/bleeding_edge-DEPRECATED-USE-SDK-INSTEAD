// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import "dart:io";

/**
 * This minimal build script copies the contents of .foo files to .foobar files.
 * In order to be invoked automatically by the Editor, this script must be named
 * 'build.dart' and placed in the root of a project.
 */
void main() {
  for (String arg in new Options().arguments) {
    if (arg.startsWith("--changed=")) {
      String file = arg.substring("--changed=".length);

      if (file.endsWith(".foo")) {
        _processFile(file);
      }
    }
  }
}

void _processFile(String file) {
  String contents = new File(file).readAsStringSync();

  if (contents != null) {
    OutputStream out = new File("${file}bar").openOutputStream();
    out.writeString("// processed from ${file}:\n${contents}");
    out.close();
  }
}

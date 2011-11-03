// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('file_system_node');

#import('file_system.dart');
#import('lib/node/node.dart');

/** File system implementation using nodejs api's (for self-hosted compiler). */
class NodeFileSystem implements FileSystem {
  void writeString(String outfile, String text) {
    fs.writeFileSync(outfile, text);
  }

  String readAll(String filename) {
    return fs.readFileSync(filename, 'utf8');
  }

  bool fileExists(String filename) {
    return path.existsSync(filename);
  }
}

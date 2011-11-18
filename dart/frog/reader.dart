// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An code reader that abstracts away the distinction between internal and user
 * libraries.
 */
class LibraryReader {
  Map _specialLibs;
  LibraryReader() {
    _specialLibs = {
      'dart:core': joinPaths(options.libDir, 'corelib.dart'),
      'dart:coreimpl': joinPaths(options.libDir, 'corelib_impl.dart'),
      'dart:html': joinPaths(options.libDir,
          '../../client/html/release/html.dart'),
      'dart:dom': joinPaths(options.libDir,
          '../../client/dom/frog/frog_dom.dart'),
      'dart:json': joinPaths(options.libDir, 'json.dart'),
    };
  }

  SourceFile readFile(String fullname) {
    var filename = _specialLibs[fullname];
    if (filename == null) {
      filename = fullname;
    }

    if (world.files.fileExists(filename)) {
      // TODO(jimhug): Should we cache these based on time stamps here?
      return new SourceFile(filename, world.files.readAll(filename));
    } else {
      world.error('File not found: $filename', null);
      return new SourceFile(filename, '');
    }
  }
}

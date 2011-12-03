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
    if (options.config == 'dev') {
      _specialLibs = {
        'dart:core': joinPaths(options.libDir, 'corelib.dart'),
        'dart:coreimpl': joinPaths(options.libDir, 'corelib_impl.dart'),
        'dart:html': joinPaths(options.libDir,
            '../../client/html/release/html.dart'),
        'dart:htmlimpl': joinPaths(options.libDir,
            '../../client/html/release/htmlimpl.dart'),
        'dart:dom': joinPaths(options.libDir,
            '../../client/dom/frog/frog_dom.dart'),
        'dart:json': joinPaths(options.libDir, 'json.dart'),
      };
    } else if (options.config == 'sdk') {
      _specialLibs = {
        'dart:core': joinPaths(options.libDir, 'core/core_frog.dart'),
        'dart:coreimpl': joinPaths(options.libDir, 
          'coreimpl/coreimpl_frog.dart'),
        'dart:html': joinPaths(options.libDir, 'html/html.dart'),
        'dart:htmlimpl': joinPaths(options.libDir, 'htmlimpl/htmlimpl.dart'),
        'dart:dom': joinPaths(options.libDir, 'dom/frog/frog_dom.dart'),
        'dart:json': joinPaths(options.libDir, 'json/json_frog.dart'),
      };
    } else {
      world.error('Invalid configuration ${options.config}');
    }
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

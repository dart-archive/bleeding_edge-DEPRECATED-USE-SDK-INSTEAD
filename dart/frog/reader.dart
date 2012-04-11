// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
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
            '../../lib/html/frog/html_frog.dart'),
        'dart:dom': joinPaths(options.libDir,
            '../../lib/dom/frog/dom_frog.dart'),
        'dart:json': joinPaths(options.libDir, '../../lib/json/json_frog.dart'),
         'dart:io': joinPaths(options.libDir,
            '../../lib/compiler/implementation/lib/io.dart'),
        'dart:isolate': joinPaths(options.libDir,
            '../../lib/isolate/isolate_frog.dart'),
        'dart:uri': joinPaths(options.libDir, '../../lib/uri/uri.dart'),
        'dart:utf': joinPaths(options.libDir, '../../lib/utf/utf.dart'),
      };
    } else if (options.config == 'sdk') {
      _specialLibs = {
        'dart:core': joinPaths(options.libDir, 'core/core_frog.dart'),
        'dart:coreimpl': joinPaths(options.libDir,
          'coreimpl/coreimpl_frog.dart'),
        'dart:html': joinPaths(options.libDir, 'html/html_frog.dart'),
        'dart:dom': joinPaths(options.libDir, 'dom/dom_frog.dart'),
        // TODO(rnystrom): How should we handle dart:io here?
        'dart:isolate': joinPaths(options.libDir, 'isolate/isolate_frog.dart'),
        'dart:json': joinPaths(options.libDir, 'json/json_frog.dart'),
        'dart:uri': joinPaths(options.libDir, 'uri/uri.dart'),
        'dart:utf': joinPaths(options.libDir, 'utf/utf.dart'),
      };
    } else {
      world.error('Invalid configuration ${options.config}');
    }
  }

  SourceFile readFile(String fullName) {
    String filename;
    if (fullName.startsWith('package:')) {
      filename = joinPaths(dirname(options.dartScript),
          joinPaths('packages', fullName.substring('package:'.length)));
    } else {
      filename = _specialLibs[fullName];
    }

    if (filename == null) {
      filename = fullName;
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

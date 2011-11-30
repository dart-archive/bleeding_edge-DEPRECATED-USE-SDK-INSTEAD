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

  void createDirectory(String path, [bool recursive = false]) {
    if (!recursive) {
      fs.mkdirSync(path);
      return;
    }

    // See how much of the path already exists and how much we need to create.
    final parts = path.split('/');
    var existing = '.';
    var part;
    for (part = 0; part < parts.length; part++) {
      final subpath = joinPaths(existing, parts[part]);

      try {
        final stat = fs.statSync(subpath);

        if (stat.isDirectory()) {
          existing = subpath;
        } else {
          throw 'Cannot create directory $path because $existing exists and ' +
              'is not a directory.';
        }
      } catch (e) {
        // Ugly hack. We only want to catch ENOENT exceptions from fs.statSync
        // which means the path we're trying doesn't exist. Since this is coming
        // from node, we can't check the exception's type.
        if (e.toString().indexOf('ENOENT') != -1) break;

        // Re-throw any other exceptions.
        throw e;
      }
    }

    // Create the remaining directories.
    for (; part < parts.length; part++) {
      existing = joinPaths(existing, parts[part]);
      fs.mkdirSync(existing);
    }
  }

  void removeDirectory(String path, [bool recursive = false]) {
    if (recursive) {
      // Remove the contents first.
      for (final file in fs.readdirSync(path)) {
        final subpath = joinPaths(path, file);
        final stat = fs.statSync(subpath);

        if (stat.isDirectory()) {
          // Recurse into subdirectories.
          removeDirectory(subpath, recursive: true);
        } else if (stat.isFile()) {
          // Try to remove the file.
          fs.unlinkSync(subpath);
        }
      }
    }

    fs.rmdirSync(path);
  }
}

// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('file_system_http');
#import('dart:dom');
#import('../../frog/file_system.dart');

/** File system implementation using HTML5's Web Storage. */
class HTTPFileSystem implements FileSystem {
  final Storage storage;

  HTTPFileSystem() : storage = window.sessionStorage;

  void writeString(String outfile, String text) {
    storage.setItem(outfile, text);
  }

  String readAll(String filename) {
    String response = storage.getItem(filename);

    if (response == null) {
      XMLHttpRequest xr = new XMLHttpRequest();
      xr.open("GET", filename, false);
      xr.send();
      response = xr.responseText;
      storage.setItem(filename, response);
    }
    return response;
  }

  bool fileExists(String filename) {
    return storage.getItem(filename) !== null;
  }

  void createDirectory(String path, [bool recursive = false]) {
    throw new UnsupportedOperationException('createDirectory');
  }

  void removeDirectory(String path, [bool recursive = false]) {
    throw new UnsupportedOperationException('removeDirectory');
  }
}

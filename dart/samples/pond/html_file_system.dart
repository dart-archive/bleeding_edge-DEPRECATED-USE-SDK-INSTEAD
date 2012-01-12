// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('html_file_system');
#import('../../frog/file_system.dart');
#import('ui.dart', prefix: 'ui'); // TODO(sigmund): remove dependency

/**
 * Implement an ad-hoc file-system for frog, that reads files from an iframe
 * document content.
 */
// TODO(sigmund,mattsh): replace once we move frog to a background worker.
class HtmlFileSystem implements FileSystem {
  final Object frameDocument;

  HtmlFileSystem() : frameDocument = getFrameDocument() {}

  // TODO - remove native
  static Object getFrameDocument() native
    'return document.getElementById("dartlibFrame").contentDocument;';

  // TODO - remove native
  static String getElementText(Object frame, String id) native
    'return frame.getElementById(id).text;';

  String readAll(String filename) {
    if (filename == 'user.dart') {
      // TODO(sigmund): remove this dependency
      return ui.getEditorText('dartEditor');
    }
    int slash1 = filename.lastIndexOf('/', filename.length);
    if (slash1 < 0) {
      throw new Exception("can't find slash1");
    }
    int slash2 = filename.lastIndexOf('/', slash1 - 1);
    String name = filename.substring(slash2 + 1);
    String id = name.replaceAll('.', '_').replaceAll('/', '_');
    return getElementText(frameDocument, id);
  }

  void writeString(String outfile, String text) {
    throw new UnsupportedOperationException();
  }

  bool fileExists(String filename) {
    return true;
  }

  void createDirectory(String path, [bool recursive]) {
    throw new UnsupportedOperationException();
  }
  void removeDirectory(String path, [bool recursive]) {
    throw new UnsupportedOperationException();
  }
}


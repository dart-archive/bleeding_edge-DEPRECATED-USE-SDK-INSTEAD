// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('html_file_system');
#import('../../frog/file_system.dart');
#import('ui.dart', prefix: 'ui'); // TODO(sigmund): remove dependency
#import('dart:dom');

/**
 * Implement an ad-hoc file-system for frog, that reads files from an iframe
 * document content.
 */
// TODO(sigmund,mattsh): replace once we move frog to a background worker.
class HtmlFileSystem implements FileSystem {
  final Document frame;
  final Map<String, String> files;

  HtmlFileSystem() : frame = _getFrameDocument(), files = {};

  String readAll(String filename) {
    if (files.containsKey(filename)) {
      return files[filename];
    }
    int slash1 = filename.lastIndexOf('/', filename.length);
    if (slash1 < 0) {
      return null;
    }
    int slash2 = filename.lastIndexOf('/', slash1 - 1);
    String name = filename.substring(slash2 + 1);
    String id = name.replaceAll('.', '_').replaceAll('/', '_');
    String res = _getElementText(frame, id);
    files[filename] = res;
    return res;
  }

  void writeString(String outfile, String text) {
    files[outfile] = text;
  }

  bool fileExists(String filename) {
    return readAll(filename) != null;
  }

  void createDirectory(String path, [bool recursive]) {
    throw new UnsupportedOperationException();
  }
  void removeDirectory(String path, [bool recursive]) {
    throw new UnsupportedOperationException();
  }

  // TODO(sigmund): this code should go away. Accessing another's IFrame's
  // content is not going to be supported by the DOM library in the future.
  static Document _getFrameDocument() {
    HTMLIFrameElement frameElem = document.getElementById("dartlibFrame");
    return frameElem.contentDocument;
  }

  static String _getElementText(Document frame, String id) {
    HTMLScriptElement script = frame.getElementById(id);
    return script == null ? null : script.text;
  }
}

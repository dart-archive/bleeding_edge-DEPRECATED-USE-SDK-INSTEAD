// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Support for generating code to be extended with source location mapping.
 */
class CodeWriter {
  static final INDENTATION = '  ';
  static final NEWLINE = '\n';

  StringBuffer _buf;
  int _indentation = 0;
  bool _pendingIndent = false;
  bool writeComments = true;

  CodeWriter(): _buf = new StringBuffer();

  String get text() { return _buf.toString(); }

  _indent() {
    _pendingIndent = false;
    for (int i=0; i < _indentation; i++) {
      _buf.add(INDENTATION);
    }
  }

  comment(String text) {
    if (writeComments) {
      writeln(text);
    }
  }

  write(String text) {
    if (text.length == 0) return;

    if (_pendingIndent) _indent();
    // TODO(jimhug): Check perf consequences of this split.
    if (text.indexOf('\n') != -1) {
      var lines = text.split('\n');
      for (int i = 0; i < lines.length - 1; i++) {
        writeln(lines[i]);
      }
      write(lines[lines.length-1]);
    } else {
      _buf.add(text);
    }
  }

  writeln([String text=null]) {
    if (text != null) {
      write(text);
    }
    if (!text.endsWith('\n')) _buf.add(NEWLINE);
    _pendingIndent = true;
  }

  enterBlock(String text) {
    writeln(text);
    _indentation++;
  }

  exitBlock(String text) {
    _indentation--;
    writeln(text);
  }

  /** Switch to an adjacent block in one line, e.g. "} else if (...) {" */
  nextBlock(String text) {
    _indentation--;
    writeln(text);
    _indentation++;
  }
}

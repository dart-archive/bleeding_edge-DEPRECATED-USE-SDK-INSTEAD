// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(sigmund,mattsh): convert this to be an isolate
// TODO(sigmund,mattsh): rewrite in JS, we want to remove all uses of 'native'
#library('editor_isolate');

// Common interfaces and classes used to define the external API for the editors
// isolate.
#import('editors.dart');

#native('codemirror/lib/codemirror.js');
#native('codemirror/mode/dart/dart.js');
#native('codemirror/mode/htmlmixed/htmlmixed.js');
#native('codemirror/mode/xml/xml.js');
#native('codemirror/mode/javascript/javascript.js');
#native('codemirror/mode/css/css.js');
#native('codemirror/mode/diff/diff.js');

/** An editor factory that creates editors using codemirror. */
class JsEditorFactory implements EditorFactory {
  JsEditorFactory();
  Editor newEditor(String id, String type) native '''
    return CodeMirror(document.getElementById(id), {
      mode: type,
      tabSize: 2,
      lineNumbers: true,
      gutter: true
    });
  ''';
}

/** Dart-JS adapter to a code mirror editor instance. */
class JsEditor implements Editor native '*Object' {
  String getText() native '''
     return this.getValue();
  ''';

  setText(String value) native 'this.setValue(value);';

  Marker mark(Position start, Position end, int kind) {
    return _mark(start.line, start.column, end.line, end.column,
        (kind == Marks.ERROR) ? 'compile_error'
        : ((kind == Marks.WARNING) ? 'compile_warning' : ''));
  }

  Marker _mark(int startLine, int startCol, int endLine, int endCol,
      String className) native '''
    return this.markText({line: startLine, ch: startCol},
      {line:endLine, ch:endCol}, className);
  ''';

}

/** Dart-JS adapter for CodeMirror's TextMarker. */
class JsMarker implements Marker native '*TextMarker' {
  void clear() native;
}

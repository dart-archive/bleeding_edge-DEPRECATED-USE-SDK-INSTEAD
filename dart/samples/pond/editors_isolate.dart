// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// An isolate that presents itself as an editor service, it internally uses
// code-mirror to create editor instances.
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
class JsEditorFactory {
  JsEditorFactory();
  JsEditor newEditor(String id, String type) native '''
    return CodeMirror(document.getElementById(id), {
      mode: type,
      tabSize: 2,
      lineNumbers: true,
      gutter: true
    });
  ''';
}

/** Dart-JS adapter to a code mirror editor instance. */
class JsEditor native '*Object' {
  String get text() native '''
     return this.getValue();
  ''';

  set text(String value) native 'this.setValue(value);';

  JsMarker mark(int startLine, int startCol, int endLine, int endCol,
      String className) native '''
    return this.markText({line: startLine, ch: startCol},
      {line:endLine, ch:endCol}, className);
  ''';

}

/** Dart-JS adapter for CodeMirror's TextMarker. */
class JsMarker native '*TextMarker' {
  void clear() native;
}

// ------------------------------------------------------------
// All of the following code should be generated automatically.
// ------------------------------------------------------------

/** A [RpcReceiver] that delegates to an editor factory. */
class EditorFactoryReceiver extends RpcReceiver<JsEditorFactory> {
  EditorFactoryReceiver(ReceivePort receivePort)
      : super(new JsEditorFactory(), receivePort) {}
  Object receiveCommand(String command, List args) {
    switch(command) {
      case "newEditor":
        String id = args[0];
        String type = args[1];
        JsEditor editor = target.newEditor(id, type);
        return new EditorReceiver(editor, new ReceivePort());
      case "close":
        RpcReceiver.closeAll();
        return "close command processed";
      default:
          throw "EditorFactory unrecognized command";
      }
  }
}

/** A [RpcReceiver] that delegates to a codemirror editor. */
class EditorReceiver extends RpcReceiver<JsEditor> {

  EditorReceiver(JsEditor editor, ReceivePort receivePort)
    : super(editor, receivePort) {}

  Object receiveCommand(String command, List args) {
    switch(command) {
      case "getText":
        return target.text;
      case "setText":
        String value = args[0];
        target.text = value;
        return null;
      case "mark":
        return new MarkerReceiver(
            target.mark(args[0], args[1], args[2], args[3],
                (args[4] == Marks.ERROR) ? 'compile_error'
                : ((args[4] == Marks.WARNING) ? 'compile_warning' : '')),
            new ReceivePort());
      default:
        throw "EditorReceiver unrecognized command";
    }
  }
}

/** A [RpcReceiver] that delegates to a text marker. */
class MarkerReceiver extends RpcReceiver<JsMarker> {

  MarkerReceiver(JsMarker marker, ReceivePort receivePort)
    : super(marker, receivePort) {}

  Object receiveCommand(String command, List args) {
    switch(command) {
      case "clear":
        return target.clear();
      default:
        throw "MarkerReceiver unrecognized command";
    }
  }
}

/** Entry-point to this isolate. */
// TODO(sigmund): rewrite. This should be replaced so that this code is compiled
// separately from pond, and spawned with a new isolate spawn API.
class EditorsIsolate extends Isolate {
  EditorsIsolate() : super.light() {}

  void main() {
    // Associate the default port with the editor-factory receiver
    new EditorFactoryReceiver(port);
  }
}

// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Stubs implementing the editors API.
// --------------------------------------------------------------
// All of the code in this file should be generated automatically.
// --------------------------------------------------------------

#library("editor_stub");

#import("editors.dart");

/**
 * An [EditorFactory] proxy that sends messages to another isolate that actually
 * implements the editor factory functionality.
 */
class EditorFactoryProxy extends RpcProxy implements EditorFactory {
  EditorFactoryProxy(Future<SendPort> futureSendPort)
      : super(futureSendPort) { }
  Future<EditorProxy> newEditor(String id, String type) {
    return sendCommand("newEditor", [id, type], (SendPort sendPort) {
      // TODO(sigmund): this way of using the RpcProxy api is not fun and
      // should be fixed.
      final completer = new Completer<SendPort>();
      completer.complete(sendPort);
      return new EditorProxy(completer.future);
    });
  }
}

/**
 * An [Editor] proxy that sends messages to another isolate that actually
 * implements the editor functionality.
 */
class EditorProxy extends RpcProxy implements Editor {
  EditorProxy(Future<SendPort> futureSendPort) : super(futureSendPort) { }

  Future<String> getText() {
    return sendCommand("getText", null, null);
  }

  Future setText(String value) {
    return sendCommand("setText", [value], null);
  }

  Future<Marker> mark(Position start, Position end, int kind) {
    return sendCommand("mark",
        [start.line, start.column, end.line, end.column, kind],
        (SendPort sendPort) {
          final completer = new Completer<SendPort>();
          completer.complete(sendPort);
          return new MarkerProxy(completer.future);
        });
  }
}

/**
 * An [Marker] proxy that sends messages to another isolate that actually
 * implements the text marker functionality.
 */
class MarkerProxy extends RpcProxy implements Marker {
  MarkerProxy(Future<SendPort> futureSendPort) : super(futureSendPort) { }
  Future clear() {
    return sendCommand("clear", null, null);
  }
}

// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Stubs implementing the editors API.

#library("editor_stub");

#import("editors.dart");
#import("dart:html");

Map<String, Function> _listeners;

/**
 * An [EditorFactory] stub that sends messages via window.postMessage to
 * implement the editor factory functionality.
 */
class EditorFactoryStub implements EditorFactory {

  Future<Editor> newEditor(String id, String type, [Function changeListener]) {
    if (changeListener != null) {
      if (_listeners == null) {
        _listeners = {};
      }
      _listeners[id] = changeListener;
    }

    return _callJs(["newEditor", [id, type, changeListener != null]]).
        transform((ignoreValue) => new EditorStub(id));
  }
}

/**
 * An [Editor] stub that sends messages via window.postMessage to implement the
 * editor factory functionality.
 */
class EditorStub implements Editor {
  /** Identifier for this editor. */
  String _editorId;

  EditorStub(this._editorId);

  Future<String> getText() {
    return _callJs(["getText", [_editorId]]);
  }

  Future setText(String value) {
    return _callJs(["setText", [_editorId, value]]);
  }

  Future<Marker> mark(Position start, Position end, int kind) {
    return _callJs(["mark",
        [_editorId, start.line, start.column, end.line, end.column, kind]]).
        transform((int markerId) => new MarkerStub(markerId));
  }

  Future refresh() {
    return _callJs(["refresh", [_editorId]]);
  }
}

/**
 * An [Marker] stub that sends messages via window.postMessage to implement the
 * editor factory functionality.
 */
class MarkerStub implements Marker {
  /** Identifier for this editor. */
  String _markerId;

  MarkerStub(this._markerId);

  Future clear() => _callJs(["clearMark", [_markerId]]);
}

/** Sparse map of pending messages. */
Map<int, Completer> _pending = null;

/** Next available id for a pending completer/message. */
int _nextId = 0;

/** Whether we are already listening on window messages. */
bool _dispatcherAdded = false;

Future _callJs(var message) {
  if (_pending == null) {
    _pending = new Map<int, Completer>();
  }
  int id = _nextId++;
  _pending[id] = new Completer();

  // TODO(sigmund): use a map here, instead of an array. Unfortunately, dart
  // maps can't be used yet to send them via postMessage (bug 1883)
  window.postMessage(['dart-to-js', id, message].dynamic, '*');
  if (!_dispatcherAdded) {
    _dispatcherAdded = true;
    window.on.message.add((e) => postMessageDispatcher(e.data));
  }
  return _pending[id].future;
}

void postMessageDispatcher(envelope) {
  if (envelope[0] == 'js-to-dart') {
    if (envelope[1] == 'update') {
      final f = _listeners[envelope[2]];
      if (f != null) f();
    } else {
      print("warning: unrecognized js-to-dart message: $envelope");
    }
  } else if (envelope[0] == 'js-to-dart-reply') {
    int id = envelope[1];
    _pending[id].complete(envelope[2]);
    _pending.remove(id);
  }
}

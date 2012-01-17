// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Dart-API to communicate with an editor service (currently running in the same
// isolate).
#library("editors");

/** An editor factory that creates new editor instances. */
interface EditorFactory {

  /**
   * Create an editor of a specific [type] (dart, js, html) and display it under
   * a DOM element with the given [id].
   */
  Editor newEditor(String id, String type);
}

/** A remote-editor interface. */
interface Editor {

  /** Retrieve the contents of the editor. */
  String getText();

  /** Update the contents of the editor. */
  void setText(String value);

  /** Create an error or warning marker between [start] and [end]. */
  Marker mark(Position start, Position end, int kind);
}

/** Interface for a text-marker in an editor. */
interface Marker {
  void clear();
}

/** Enumeration of kinds of markers. */
class Marks {
  static final NONE = 0;
  static final ERROR = 1;
  static final WARNING = 2;
  static final INFO = 3;
}

/** Represents a position in the editor. */
class Position {
  int line;
  int column;
  Position(this.line, this.column);
}

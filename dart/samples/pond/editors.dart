// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A thin Dart-API to communicate with an editor service that is running in a
// separate isolate.
#library("editors");

/** An editor factory that creates new editor instances. */
interface EditorFactory {

  /**
   * Create an editor of a specific [type] (dart, js, html) and display it under
   * a DOM element with the given [id].
   */
  Future<Editor> newEditor(String id, String type, [Function changeListener]);
}

/** A remote-editor interface. */
interface Editor {

  /** Asynchronously retrieve the contents of the editor. */
  Future<String> getText();

  /**
   * Asynchronously update the contents of the editor. The returned future will
   * be completed when the text has been updated.
   */
  Future setText(String value);

  /** Create an error or warning marker between [start] and [end]. */
  Future<Marker> mark(Position start, Position end, int kind);

  /**
   * Ensure that an editor is visible and up to date. Sometimes editors are not
   * up to date if the UI is hidden when it was rendered.
   */
  Future refresh();
}

/** Interface for a text-marker in an editor. */
interface Marker {
  Future clear();
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

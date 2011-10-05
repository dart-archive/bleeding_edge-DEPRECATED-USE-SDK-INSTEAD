// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A stack of [Command]s with a current position that supports [execute], [undo], and [redo]
 * methods.  Calling [execute] destroys any previously "undone" commands beyond the current
 * position.  Attempting to undo beyond the first command or redo beyond the last command are
 * treated as no-ops.
 */
class UndoStack {

  int _undoPosition;
  List<Command> _undoStack;

  UndoStack() {
    this._undoStack = new List<Command>();
    this._undoPosition = -1;
  }

  /**
   * Return [:true:] if there is a command on the stack that can be "redone."
   */
  bool canRedo() {
    return _undoPosition < _undoStack.length - 1;
  }

  /**
   * Return [:true:] if there is a command on the stack that can be "undone."
   */
  bool canUndo() {
    return _undoPosition >= 0;
  }

  /**
   * Execute a command and make it the next "undoable" command.
   * Any commands that have previously been "undone" will be removed from the stack.
   */
  void execute(Command command) {
    int pos = ++_undoPosition;
    if (pos == _undoStack.length) {
      _undoStack.add(command);
    } else {
      _undoStack[pos] = command;
      // Remove susequent commands on the stack
      _undoStack.length = pos + 1;
    }
    command.execute();
  }

  /**
   * Redo the most recently "undone" command, or do nothing if there is none.
   */
  void redo() {
    if (_undoPosition < _undoStack.length - 1) {
      ++_undoPosition;
      Command command = _undoStack[_undoPosition];
      print("Redo ${command.description}");
      command.execute();
    }
  }

  /**
   * Undo the most recent command, or do nothing if there is none.
   */
  void undo() {
    if (_undoPosition >= 0) {
      Command command = _undoStack[_undoPosition];
      _undoPosition--;
      print("Undo ${command.description}");
      command.unexecute();
    }
  }
}

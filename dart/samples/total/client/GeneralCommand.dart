// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A function type for the action of a [GeneralCommand]
 */
typedef void ActionFunction();

/**
 * A [Command] that calls a supplied action function to modify the spreadsheet.
 *
 * This class holds the entire sequence of cell operations - change contents and change style -
 * used to exact the action. The command will take care of marking changed cells as dirty.
 *
 * Note that only cell modifications are tracked. For example, changes to the row/column
 * sizes that occur in the action function are not tracked by the spreadsheet.
 */
class GeneralCommand extends Command {

  ActionFunction _action;
  List<UndoableAction> _deltas;

  // This is here to handle derived classes that have their own action
  // function that cannot be passed during construction.
  void set action(ActionFunction action) {
    _action = action;
  }

  /**
   * Create a new command with the given action and a description.
   */
  GeneralCommand(Spreadsheet spreadsheet, this._action, String description)
      : super(spreadsheet, description) {
  }

  void execute() {
    _deltas = new List<UndoableAction>();
    _spreadsheet.trackDeltas(_deltas);

    // Perform the action on the spreadsheet
    _action();
    _spreadsheet.trackDeltas(null);

    print("${_deltas.length} deltas");
  }

  void unexecute() {
    // Revert content changes
    int count = _deltas.length;
    for (int i = count - 1; i >= 0; --i) {
      _deltas[i].undoAction(_spreadsheet);
    };
    _deltas = null;
  }
}

/**
 * A command to remove a block of cells and shift cells up to fill the gap.
 */
class RemoveBlockAndShiftUpCommand extends GeneralCommand {
  RowCol _maxCorner;
  RowCol _minCorner;

  /**
   * Create a new command.
   *
   * [minCorner] is the top left corner of the block to remove, while [maxCorner] is the
   * bottom right corner. The removed block includes both corners.
   */
  RemoveBlockAndShiftUpCommand(Spreadsheet spreadsheet, this._minCorner, this._maxCorner)
      : super(spreadsheet, null, "Remove/Shift Up") {
    // Set the action here because we cannot reference a field (_spreadheet) in the super call.
    action = () {
      _spreadsheet.removeBlockAndShiftUp(_minCorner, _maxCorner);
    };
  }
}

/**
 * A command to remove a block of cells and shift cells left to fill the gap.
 */
class RemoveBlockAndShiftLeftCommand extends GeneralCommand {
  RowCol _maxCorner;
  RowCol _minCorner;

  /**
   * Create a new command.
   *
   * [minCorner] is the top left corner of the block to remove, while [maxCorner] is the
   * bottom right corner. The removed block includes both corners.
   */
  RemoveBlockAndShiftLeftCommand(Spreadsheet spreadsheet, this._minCorner, this._maxCorner)
      : super(spreadsheet, null, "Remove/Shift Left") {
    // Set the action here because we cannot reference a field (_spreadheet) in the super call.
    action = () {
      _spreadsheet.removeBlockAndShiftLeft(_minCorner, _maxCorner);
    };
  }
}

/**
 * The command for removing columns from the spreadsheet.
 */
class RemoveColumnsCommand extends GeneralCommand {
  int _maxColumn;
  int _minColumn;

  /**
   * Create a new command.
   *
   * [minColumn] is the leftmost column to remove, while [maxColumn] is the rightmost
   * column to be removed.
   */
  RemoveColumnsCommand(Spreadsheet spreadsheet, this._minColumn, this._maxColumn)
      : super(spreadsheet, null, "Remove Columns") {
    // Set the action here because we cannot reference a field (_spreadheet) in the super call.
    action = () {
      _spreadsheet.removeColumns(_minColumn, _maxColumn);
    };
  }
}

/**
 * The command for removing rows from the spreadsheet.
 */
class RemoveRowsCommand extends GeneralCommand {
  Map<CellLocation, Formula> _changedFormulas;
  int _maxRow;
  int _minRow;

  /**
   * Create a new command.
   *
   * [minRow] is the topmost row to remove, while [maxRow] is the bottommost
   * row to be removed.
   */
  RemoveRowsCommand(Spreadsheet spreadsheet, this._minRow, this._maxRow)
      : super(spreadsheet, null, "Remove Rows") {
    // Set the action here because we cannot reference a field (_spreadheet) in the super call.
    action = () {
      _spreadsheet.removeRows(_minRow, _maxRow);
    };
  }
}

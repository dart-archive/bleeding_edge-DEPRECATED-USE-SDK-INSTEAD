// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * The base class for commands that may be placed on the undo/redo stack.
 */
class Command {

  String _description;
  Spreadsheet _spreadsheet;

  String get description() {
    return _description;
  }

  /**
   * Constructs a new command on a given spreadsheet.
   *
   * The description is currently used for logging during undo/redo events.
   */
  Command(this._spreadsheet, this._description) { }

  /**
   * Perform the action associated with this command.
   */
  void execute() {
    throw new NotImplementedException();
  }

  String toString() {
    return "Command[${_description}]";
  }

  /**
   * Undo the action.
   *
   * This method should reverse any operation done during [execute()].
   */
  void unexecute() {
    throw new NotImplementedException();
  }
}

/**
 * The command for inserting a block of cells and shifting down.
 */
class InsertBlockAndShiftDownCommand extends Command {

  RowCol _maxCorner;
  RowCol _minCorner;

  /**
   * Create a new command.
   *
   * [minCorner] is the top left corner of the block to insert, while [maxCorner] is the
   * bottom right corner. The shifted block includes both corners.
   */
  InsertBlockAndShiftDownCommand(Spreadsheet spreadsheet, this._minCorner, this._maxCorner)
      : super(spreadsheet, "Insert/Shift Down") { }

  void execute() {
    _spreadsheet.insertBlockAndShiftDown(_minCorner, _maxCorner);
  }

  void unexecute() {
    _spreadsheet.removeBlockAndShiftUp(_minCorner, _maxCorner);
  }
}

/**
 * The command for inserting a block of cells and shifting right.
 */
class InsertBlockAndShiftRightCommand extends Command {

  RowCol _maxCorner;
  RowCol _minCorner;

  /**
   * Create a new command.
   *
   * [minCorner] is the top left corner of the block to insert, while [maxCorner] is the
   * bottom right corner. The shifted block includes both corners.
   */
  InsertBlockAndShiftRightCommand(Spreadsheet spreadsheet, this._minCorner, this._maxCorner)
      : super(spreadsheet, "Insert/Shift Right") { }

  void execute() {
    _spreadsheet.insertBlockAndShiftRight(_minCorner, _maxCorner);
  }

  void unexecute() {
    _spreadsheet.removeBlockAndShiftLeft(_minCorner, _maxCorner);
  }
}

/**
 * The command for inserting columns into the spreadsheet.
 */
class InsertColumnsCommand extends Command {

  int _maxColumn;
  int _minColumn;

  /**
   * Create a new command.
   *
   * [minColumn] is the leftmost column of the block to insert, while [maxColumn] is the rightmost
   * column to be inserted.
   */
  InsertColumnsCommand(Spreadsheet spreadsheet, this._minColumn, this._maxColumn)
      : super(spreadsheet, "Insert Columns") { }

  void execute() {
    _spreadsheet.insertColumns(_minColumn, _maxColumn);
  }

  void unexecute() {
    _spreadsheet.removeColumns(_minColumn, _maxColumn);
  }
}

/**
 * The command for inserting rows into the spreadsheet.
 */
class InsertRowsCommand extends Command {

  int _maxRow;
  int _minRow;

  /**
   * Create a new command.
   *
   * [minRow] is the topmost row of the block to insert, while [maxRow] is the bottommost
   * row to be inserted.
   */
  InsertRowsCommand(Spreadsheet spreadsheet, this._minRow, this._maxRow)
      : super(spreadsheet, "Insert Rows") { }

  void execute() {
    _spreadsheet.insertRows(_minRow, _maxRow);
  }

  void unexecute() {
    _spreadsheet.removeRows(_minRow, _maxRow);
  }
}

/**
 * The command to reset the row and column sizes.
 */
class ResetRowColumnSizesCommand extends Command {

  List<List<int>> _rowColumnSizes;

  ResetRowColumnSizesCommand(Spreadsheet spreadsheet)
      : super(spreadsheet, "Reset Row/Column Sizes") {
    _rowColumnSizes = _spreadsheet.layout.getRowColumnSizes();
  }

  void execute() {
    _spreadsheet.layout.resetSizes();
  }

  void unexecute() {
    _spreadsheet.layout.setRowColumnSizes(_rowColumnSizes);
  }
}

/**
 * The command to resize a row or column.
 */
class ResizeRowColumnCommand extends Command {

  int _index;
  int _oldSize;
  int _rowOrCol;
  int _size;

  /**
   * Create a new command.
   *
   * rowOrCol indicates which of a row or column we are resizing. Index indicates which
   * row or column. Size is the new size and oldSize is the former size.
   */
  ResizeRowColumnCommand(Spreadsheet spreadsheet, this._rowOrCol, this._index, this._size,
      this._oldSize) : super(spreadsheet, "Resize Row/Column") { }

  void execute() {
    if (_rowOrCol == Spreadsheet.COL) {
      _spreadsheet.setColumnWidth(_index, _size);
    } else {
      _spreadsheet.setRowHeight(_index, _size);
    }
  }

  void unexecute() {
    if (_rowOrCol == Spreadsheet.COL) {
      _spreadsheet.setColumnWidth(_index, _oldSize);
    } else {
      _spreadsheet.setRowHeight(_index, _oldSize);
    }
  }
}

/**
 * A command to set the content of a single cell, in string form.
 */
class SetCellContentsCommand extends Command {

  String _content;
  String _oldContent;
  RowCol _rowCol;
  Style _style;

  /**
   * Create a new command, given the new content for the cell.
   */
  SetCellContentsCommand(CellLocation location, this._content)
      : super(location.spreadsheet, "Set Cell Content") {
    _rowCol = location.rowCol;
    Cell oldCell = location.getCell();
    if (oldCell == null) {
      _oldContent = null;
    } else {
      _oldContent = oldCell.getContentString();
    }
  }

  void execute() {
    _spreadsheet.setCellFromContentString(_rowCol, _content);
    _spreadsheet.markDirty(_rowCol);
  }

  void unexecute() {
    String rowColString = _rowCol.toString();
    if (_oldContent == null) {
      _spreadsheet.setCellContent(_rowCol, null);
    } else {
      _spreadsheet.setCellFromContentString(_rowCol, _oldContent);
    }
    _spreadsheet.markDirty(_rowCol);
  }
}

/**
 * A type for the action method used to set style properties.
 */
typedef Style SetStyleFunction(Style s, int index);

/**
 * A command to set the style of a range of cells.
 */
class SetStyleCommand extends Command {

  SetStyleFunction _action;
  CellRange _cellRange;
  int _index;
  List<UndoableAction> _oldCellStyles;

  /**
   * Create a new command.
   *
   * [cellRange] is the range of cells to apply the style change to. [index] is passed to the
   * action function, allowing a single action to be used for multiple properties. [action] is
   * the function to apply to the range.
   */
  SetStyleCommand(CellRange cellRange, this._index, this._action) : super(cellRange.spreadsheet,
      "Set Style") {
    _cellRange = cellRange;
  }

  void execute() {
    _spreadsheet.clearDirtyCells();
    _oldCellStyles = new List<UndoableAction>();
    _spreadsheet.trackDeltas(_oldCellStyles);

    bool isSheet = _cellRange.isSheetSelection();
    bool isCol = _cellRange.isColumnSelection();
    bool isRow = _cellRange.isRowSelection();
    RowColStyle s;
    if (isSheet) {
      s = _spreadsheet.getSheetStyle();
      _spreadsheet.setSheetStyle(_updateStyle(s));
    } else if (isRow) {
      for (int row = _cellRange.minCorner.row; row <= _cellRange.maxCorner.row; row++) {
        s = _spreadsheet.getRowStyle(row);
        _spreadsheet.setRowStyle(row, _updateStyle(s));
      }
    } else if (isCol) {
      for (int col = _cellRange.minCorner.col; col <= _cellRange.maxCorner.col; col++) {
        s = _spreadsheet.getColumnStyle(col);
        _spreadsheet.setColumnStyle(col, _updateStyle(s));
      }
    }

    // Perform the action the intersection between the range and the active area
    CellRange range = _cellRange.makeBounded();
    _cellRange.forEach((CellLocation location) {
      Cell c = location.getCell();
      // Don't force cell creation for whole row/col selection
      if (c == null && !isCol && !isRow) {
        Style newStyle = _action(new Style(), _index);
        location.spreadsheet.setCellStyle(location.rowCol, newStyle);
      }
      if (c != null) {
        Style newStyle = _action(c.style, _index);
        location.spreadsheet.setCellStyle(location.rowCol, newStyle);
      }
    });
  }

  void unexecute() {
    _spreadsheet.clearDirtyCells();
    int count = _oldCellStyles.length;
    for (int i = count - 1; i >= 0; --i) {
      _oldCellStyles[i].undoAction(_spreadsheet);
    };
    _oldCellStyles = null;
  }

  // Return a RowColStyle that is the result of performing the style setting action on
  // an existing RowColStyle.
  RowColStyle _updateStyle(RowColStyle s) {
    Style oldStyle = s == null ? new Style() : s.style;
    // Perform the style setting action
    Style newStyle = _action(oldStyle, _index);
    RowColStyle newRCStyle = new RowColStyle(newStyle);
    return newRCStyle;
  }
}

// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * The [UndoableAction] interface must be implemented by classes that store undo/redo data for
 * the [GeneralCommand] class and its subclasses.
 */
interface UndoableAction {
  /**
   * Undo the effect of the action on the given spreadsheet.
   */
  void undoAction(Spreadsheet spreadsheet);
}

/**
 * The [CellAction class] represents an action on a cell at a given location.
 */
class CellAction implements UndoableAction {
  // The location of the cell that has changed
  RowCol _rowCol;
  
  CellAction(this._rowCol) {}
  
  // Apply does nothing
  void undoAction(Spreadsheet spreadsheet) {}
}

/**
 * A class to hold a change to the content of a [Cell].
 */
class CellContentAction extends CellAction {
  // The old content
  CellContent _originalContent;
  
  CellContentAction(RowCol rowCol, this._originalContent) : super(rowCol) {}

  void undoAction(Spreadsheet spreadsheet) {
    spreadsheet.setCellContent(_rowCol, _originalContent);
  }
}

/**
 * A class to hold a change to the style of a [Cell].
 */
class CellStyleAction extends CellAction {
  // The old style
  Style _originalStyle;
  
  CellStyleAction(RowCol rowCol, this._originalStyle) : super(rowCol) {}

  void undoAction(Spreadsheet spreadsheet) {
    if (_originalStyle == null) {
      Cell c = spreadsheet.getCell(_rowCol);
      if (c != null) {
        spreadsheet.setCellStyle(_rowCol, null);
      }
    } else {
      spreadsheet.setCellStyle(_rowCol, _originalStyle);
    }
  }
}

/**
 * A class to hold a change to a row, column or entire sheet.
 */
class RowColAction implements UndoableAction {
  // The index of the row or column that has changed
  int _index;
  
  // One of Spreadsheet.ROW, Spreadsheet.COL, or Spreadsheet.SHEET.
  int _rowOrCol;
  
  RowColAction(this._index, this._rowOrCol) {}
  
  // Apply does nothing
  void undoAction(Spreadsheet spreadsheet) {}
}

/**
 * A class to hold a change to a row, column or spreadsheet style
 */
class RowColStyleAction extends RowColAction {
  // The original style before the delta is applied
  RowColStyle _originalStyle;
  
  RowColStyleAction(int index, int rowOrCol, this._originalStyle) : super(index, rowOrCol) {}
  
  void undoAction(Spreadsheet spreadsheet) {
    if (_rowOrCol == Spreadsheet.ROW) {
      spreadsheet.setRowStyle(_index, _originalStyle);
    } else if (_rowOrCol == Spreadsheet.COL) {
      spreadsheet.setColumnStyle(_index, _originalStyle);
    } else {
      spreadsheet.setSheetStyle(_originalStyle);
    }
  }
}

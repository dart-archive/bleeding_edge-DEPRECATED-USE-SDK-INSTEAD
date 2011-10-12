// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A class to represent a range of cells in a particular spreadsheet.
class CellRange {
  // A row or column value of 0 indicates [0, infinity)
  RowCol _maxCorner;
  RowCol _minCorner;
  Spreadsheet _spreadsheet;

  int get columns() => _maxCorner.col - _minCorner.col + 1;

  RowCol get maxCorner() => _maxCorner;

  RowCol get minCorner() => _minCorner;

  int get rows() => _maxCorner.row - _minCorner.row + 1;

  Spreadsheet get spreadsheet() => _spreadsheet;

  CellRange(this._spreadsheet, this._minCorner, this._maxCorner) {
    int minRow = _minCorner.row;
    int minCol = _minCorner.col;
    int maxRow = _maxCorner.row;
    int maxCol = _maxCorner.col;
    if ((maxRow != 0 && minRow > maxRow) ||
        (maxCol != 0 && minCol > maxCol)) {
      throw new RuntimeException("Invalid range");
    }
  }

  CellRange.columns(this._spreadsheet, int minColumn, int maxColumn) {
    _minCorner = new RowCol(0, minColumn);
    _maxCorner = new RowCol(0, maxColumn);
  }

  CellRange.rows(this._spreadsheet, int minRow, int maxRow) {
    _minCorner = new RowCol(minRow, 0);
    _maxCorner = new RowCol(maxRow, 0);
  }

  CellRange.spreadsheet(this._spreadsheet) {
    _minCorner = _maxCorner = new RowCol(0, 0);
  }

  bool operator==(final CellRange other) {
    if (other === this) {
      return true;
    }
    if (other == null || !(other is CellRange)) {
      return false;
    }
    return _spreadsheet === other._spreadsheet
        && _minCorner == other._minCorner
        && _maxCorner == other._maxCorner;
  }

  // Iterate over all cells in the intersection of the range and the active spreadsheet area
  void forEach(apply(CellLocation location)) {
    // TODO: Better handle ranges of entire rows and columns.
    // We probably want to call apply with the header row so that the
    // calling method can take appropriate action for its own situation.
    int minRow = this._minCorner.row;
    int minCol = this._minCorner.col;
    int maxRow = this._maxCorner.row;
    int maxCol = this._maxCorner.col;
    if (minRow == 0 && maxRow == 0) {
      minRow = 1;
      maxRow = _spreadsheet.rowCount();
    }
    if (minCol == 0 && maxCol == 0) {
      minCol = 1;
      maxCol = _spreadsheet.columnCount();
    }
    for (int row = minRow; row <= maxRow; row++) {
      for (int col = minCol; col <= maxCol; col++) {
        apply(new CellLocation(_spreadsheet, new RowCol(row, col)));
      }
    }
  }

  CellRange intersect(CellRange other) {
    // Separating lines test for null intersection
    if (_spreadsheet !== other._spreadsheet
        || _minCorner.row > other._maxCorner.row
        || _minCorner.col > other.maxCorner.col
        || _maxCorner.row < other._minCorner.row
        || _maxCorner.col < other._minCorner.col) {
      return null;
    }

    // Must intersect
    int minRow = Math.max(_minCorner.row, other._minCorner.row);
    int minCol = Math.max(_minCorner.col, other._minCorner.col);
    int maxRow = Math.min(_maxCorner.row, other._maxCorner.row);
    int maxCol = Math.min(_maxCorner.col, other._maxCorner.col);
    return new CellRange(_spreadsheet, new RowCol(minRow, minCol), new RowCol(maxRow, maxCol));
  }

  // Return true if this selection represents a contiguous set of columns
  bool isColumnSelection() => _minCorner.row == 0 && _minCorner.col != 0;

  bool isInRange(CellLocation location) => location.spreadsheet === _spreadsheet &&
      location.row >= _minCorner.row && location.col >= _minCorner.col &&
      location.row <= _maxCorner.row && location.col <= _maxCorner.col;

  // Return true if this selection represents a contiguous set of rows
  bool isRowSelection() => _minCorner.col == 0 && _minCorner.row != 0;

  // Return true if this selection represents the entire spreadsheet
  bool isSheetSelection() => _minCorner.col == 0 && _minCorner.row == 0;

  // For unbounded ranges (entire spreadsheet, or whole rows or columns), return a bounded range
  // that is the intersection of the range with the active area of the spreadsheet.
  CellRange makeBounded() {
    int minRow = this._minCorner.row;
    int minCol = this._minCorner.col;
    int maxRow = this._maxCorner.row;
    int maxCol = this._maxCorner.col;
    if (minRow == 0 && maxRow == 0) {
      minRow = 1;
      maxRow = _spreadsheet.rowCount();
    }
    if (minCol == 0 && maxCol == 0) {
      minCol = 1;
      maxCol = _spreadsheet.columnCount();
    }
    return new CellRange(_spreadsheet, new RowCol(minRow, minCol), new RowCol(maxRow, maxCol));
  }
}


// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Encapsulates the column widths and row heights of a view of a spreadsheet
class SpreadsheetLayout {
  // Constants to access row/col size and position lists
  // This assumes that Spreadsheet has already had an instance created.
  static final int COL = Spreadsheet.COL;
  static final int ROW = Spreadsheet.ROW;

  // The current number of columns for which we have size information.
  int _columns;
  // _rowColumnEnds[COL][c] contains the pixel position of the end of column c
  // _rowColumnEnds[ROW][r] contains the pixel position of the end of row r
  List<List<int>> _rowColumnEnds;
  // _rowColumnSizes[COL][c] contains the width of column c
  // _rowColumnSizes[ROW][r] contains the height of row r
  List<List<int>> _rowColumnSizes;
  // The current number of rows for which we have size information.
  int _rows;

  SpreadsheetLayout() : _rows = 0, _columns = 0 {
    _rowColumnSizes = new List<List<int>>(2);
    _rowColumnEnds = new List<List<int>>(2);
    resetSizes();
  }

  // Return the pixel position of the end of a given column
  int getColumnEnd(int index) => _getEnd(COL, index);

  // Return the number of pixels to shift in order to place the column 'origin' in
  // the leftmost position
  int getColumnShift(int origin) => _getShift(COL, origin);

  // Return the width of a column
  int getColumnWidth(int index) => _getSize(COL, index);

  // Return the default column width
  int getDefaultColumnWidth(int index) => index == 0 ? CssStyles.ROW_HEADER_WIDTH : CssStyles.DEFAULT_COLUMN_WIDTH;

  // Return the default row height
  int getDefaultRowHeight(int index) => index == 0 ? CssStyles.COLUMN_HEADER_HEIGHT : CssStyles.DEFAULT_ROW_HEIGHT;

  // Return a copy of the sizes array
  List<List<int>> getRowColumnSizes() {
    List<List<int>> retVal = new List<List<int>>(2);
    for (int rowOrCol = COL; rowOrCol <= ROW; rowOrCol++) {
      int len = _rowColumnSizes[rowOrCol].length;
      retVal[rowOrCol] = new List<int>();
      for (int i = 0; i < len; i++) {
        retVal[rowOrCol].add(_rowColumnSizes[rowOrCol][i]);
      }
    }
    return retVal;
  }

  // Return the pixel position of the end of a given row
  int getRowEnd(int index) => _getEnd(ROW, index);

  // Return the height of a row
  int getRowHeight(int index) => _getSize(ROW, index);

  // Return the number of pixels to shift in order to place row 'origin' in
  // the topmost position
  int getRowShift(int origin) => _getShift(ROW, origin);

  // Insert a new column with the given size
  void insertColumn(int index, int size) {
    _insert(COL, index, size);
  }

  // Insert a new row with the given size
  void insertRow(int index, int size) {
    _insert(ROW, index, size);
  }

  // Reset the sizes of all rows and columns to their default values
  void resetSizes() {
    _rowColumnSizes[ROW] = new List<int>();
    _rowColumnEnds[ROW] = new List<int>();
    _rowColumnSizes[ROW].add(CssStyles.COLUMN_HEADER_HEIGHT);
    _rowColumnEnds[ROW].add(CssStyles.COLUMN_HEADER_HEIGHT);
    for (int i = 1; i <= _rows; i++) {
      _rowColumnSizes[ROW].add(CssStyles.DEFAULT_ROW_HEIGHT);
      _rowColumnEnds[ROW].add(_rowColumnEnds[ROW][i - 1] + CssStyles.DEFAULT_ROW_HEIGHT);
    }

    _rowColumnSizes[COL] = new List<int>();
    _rowColumnEnds[COL] = new List<int>();
    _rowColumnSizes[COL].add(CssStyles.ROW_HEADER_WIDTH);
    _rowColumnEnds[COL].add(CssStyles.ROW_HEADER_WIDTH);
    for (int i = 1; i <= _columns; i++) {
      _rowColumnSizes[COL].add(CssStyles.DEFAULT_COLUMN_WIDTH);
      _rowColumnEnds[COL].add(_rowColumnEnds[COL][i - 1] + CssStyles.DEFAULT_COLUMN_WIDTH);
    }
  }

  // Change the width of a column
  void setColumnWidth(int index, int width) {
    _setSize(COL, index, width);
  }

  // Reset the sizes and ends arrays and the number of rows and columns from a supplied sizes array
  void setRowColumnSizes(List<List<int>> sizes) {
    _rows = sizes[ROW].length -1;
    _columns = sizes[COL].length -1;
    for (int rowOrCol = COL; rowOrCol <= ROW; rowOrCol++) {
      int len = sizes[rowOrCol].length;
      _rowColumnSizes[rowOrCol] = new List<int>();
      _rowColumnEnds[rowOrCol] = new List<int>();
      for (int i = 0; i < len; i++) {
        _rowColumnSizes[rowOrCol].add(sizes[rowOrCol][i]);
        if (i == 0) {
          _rowColumnEnds[rowOrCol].add(_rowColumnSizes[rowOrCol][i]);
        } else {
          _rowColumnEnds[rowOrCol].add(_rowColumnEnds[rowOrCol][i - 1] +
              _rowColumnSizes[rowOrCol][i]);
        }
      }
    }
  }

  // Change the height of a row
  void setRowHeight(int index, int height) {
    _setSize(ROW, index, height);
  }

  // Return the end position of the given row or column
  int _getEnd(int rowOrCol, int index) {
    _grow(rowOrCol, index);
    return _rowColumnEnds[rowOrCol][index];
  }

  // Return the number of pixels to shift in order to place row or column 'origin' in
  // the topmost/leftmost position
  int _getShift(int rowOrCol, int origin) => _getEnd(rowOrCol, origin) - _getEnd(rowOrCol, 0);

  // Return the size of the given row or column
  int _getSize(int rowOrCol, int index) {
    _grow(rowOrCol, index);
    return _rowColumnSizes[rowOrCol][index];
  }

  void _grow(int rowOrCol, int index) {
    int len = _rowColumnSizes[rowOrCol].length;
    while (len <= index) {
      int size = rowOrCol == COL ? getDefaultColumnWidth(index) : getDefaultRowHeight(index);
      _rowColumnSizes[rowOrCol].add(size);
      _rowColumnEnds[rowOrCol].add(_rowColumnEnds[rowOrCol][len - 1] + size);
      if (rowOrCol == COL) {
        _columns++;
      } else {
        _rows++;
      }
      ++len;
    }
  }

  // Insert a new row or column with the given size
  void _insert(int rowOrCol, int index, int size) {
    if (rowOrCol == COL) {
      _columns++;
    } else {
      _rows++;
    }
    _rowColumnSizes[rowOrCol].add(0);
    _rowColumnEnds[rowOrCol].add(0);
    // If we didn't insert at the end, we need to bubble the default size down
    // to the insert position.
    for (int i = _rowColumnSizes[rowOrCol].length - 1; i > index; --i) {
      _rowColumnSizes[rowOrCol][i] = _rowColumnSizes[rowOrCol][i - 1];
      _rowColumnEnds[rowOrCol][i] = _rowColumnEnds[rowOrCol][i - 1] + size;
    }
    _rowColumnSizes[rowOrCol][index] = size;
    if (index == 0) {
      _rowColumnEnds[rowOrCol][0] = size;
    } else {
      _rowColumnEnds[rowOrCol][index] = _rowColumnEnds[rowOrCol][index - 1] + size;
    }
  }

  // Change the size of a row/column
  int _setSize(int rowOrCol, int index, int size) {
    int oldSize = _rowColumnSizes[rowOrCol][index];
    _rowColumnSizes[rowOrCol][index] = size;
    for (int i = index; i < _rowColumnSizes[rowOrCol].length; i++) {
      if (i == 0) {
        _rowColumnEnds[rowOrCol][i] = _rowColumnSizes[rowOrCol][i];
      } else {
        _rowColumnEnds[rowOrCol][i] = _rowColumnEnds[rowOrCol][i - 1] +
            _rowColumnSizes[rowOrCol][i];
      }
    }
    return size - oldSize;
  }
}

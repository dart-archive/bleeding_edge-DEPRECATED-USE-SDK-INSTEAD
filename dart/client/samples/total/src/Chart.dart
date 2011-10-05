// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Superclass for client and server charts
 */
class Chart {

  int _firstCol;
  int _firstRow;
  int _height;
  int _lastCol;
  int _lastRow;
  double _maxValue;
  double _minValue;
  CellRange _range;
  List<String> _seriesLabels;
  String _title;
  int _width;

  int get height() {
    return _height;
  }

  void set height(int height) {
    _height = height;
  }

  String get title() {
    return _title;
  }

  void set title(String title) {
    _title = title;
  }

  int get width() {
    return _width;
  }

  void set width(int width) {
    _width = width;
  }

  Chart() {
    _title = null;
    _width = 600;
    _height = 400;
  }

  // Extract numeric values from the given column
  List<double> getSeries(int col) {
    List<double> values = new List<double>();
    for (int row = _firstRow; row <= _lastRow; row++) {
      Cell cell = _range.spreadsheet.getCell(new RowCol(row, col));
      if (cell != null && cell.isNumeric()) {
        values.add(cell.getDoubleValue());
      }
    }
    return values;
  }

  void setData(CellRange dataRange) {
    _range = dataRange.makeBounded();
    _minValue = 1.0e100;
    _maxValue = -1.0e100;
    _range.forEach((CellLocation location) {
      double value = 0.0;
      Cell c = location.getCell();
      if (c != null && c.isNumeric()) {
        value = c.getDoubleValue();
        if (value > _maxValue) {
          _maxValue = value;
        }
        if (value < _minValue) {
          _minValue = value;
        }
      }
    });
    _firstCol = _range.minCorner.col;
    _lastCol = _range.maxCorner.col;
    _firstRow = _range.minCorner.row;
    _lastRow = _range.maxCorner.row;

    // Extract series labels
    _seriesLabels = new List<String>();
    Spreadsheet spreadsheet = _range.spreadsheet;
    String label;
    for (int col = _firstCol; col <= _lastCol; col++) {
      Cell topCell = spreadsheet.getCell(new RowCol(_firstRow, col));
      if (topCell != null && topCell.isString()) {
        label = topCell.getContentString();
      } else {
        label = StringUtils.columnString(col);
      }
      _seriesLabels.add(label);
    }
  }

  void setSeriesLabel(int col, String label) {
    if (_seriesLabels == null) {
      throw new RuntimeException("Must call setData prior to setSeriesLabel");
    }
    _seriesLabels[col - _firstCol] = label;
  }
}

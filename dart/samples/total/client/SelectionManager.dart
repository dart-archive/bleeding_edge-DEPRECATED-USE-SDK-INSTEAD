// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The callback method type invoked when the selection changes.
typedef void SelectionCallback();

// The method type for the SelectionManager.applyToSelectedCells method.
typedef void SelectionApply(CellLocation location);

class BoundingBox {
  int height;
  int left;
  int top;
  int width;

  BoundingBox(this.left, this.top, this.width, this.height) { }
}

interface SelectionListener {
  void onSelectionChanged();
}

class SelectionManager {
  static ZoomTracker _zoomTracker;

  SelectionListener _listener;
  int _originColumn;
  int _originRow;
  CellLocation _selectedCell;
  CellLocation _selectionCorner;
  DivElement _selectionDiv;
  Spreadsheet _spreadsheet;
  HtmlTable _table;

  CellLocation get selectedCell() => _selectedCell;

  void set selectedCell(CellLocation value) {
    _selectedCell = value;
  }

  CellLocation get selectionCorner() => _selectionCorner;

  void set selectionCorner(CellLocation value) {
    _selectionCorner = value;
  }

  Spreadsheet get spreadsheet() => _spreadsheet;

  SelectionManager(this._spreadsheet, SpreadsheetPresenter presenter, Window window, this._table)
      : _selectedCell = null, _selectionCorner = null, _originRow = 0, _originColumn = 0 {
    Document doc = document;

    Element spreadsheetElement = presenter.spreadsheetElement;
    _selectionDiv = new Element.tag("div");
    _selectionDiv.id = "selection-${_spreadsheet.name}";
    _selectionDiv.attributes["class"] = "selection";
    _selectionDiv.style.setProperty("display", "none");
    spreadsheetElement.nodes.add(_selectionDiv);

    Element thumb = new Element.tag("div");
    thumb.id = "selection-thumb-${_spreadsheet.name}";
    thumb.attributes["class"] = "selection-thumb";
    _selectionDiv.nodes.add(thumb);

    // Update selection after changing zoom factor, to reduce problems with position precision
    // Only create a single tracker per page
    if (_zoomTracker == null) {
      _zoomTracker = new ZoomTracker(window);
      _zoomTracker.addListener((double oldZoom, double newZoom) {
        updateSelection();
      });
    }
  }

  // Move the selection by a given (row, col) amount
  void advanceSelectedCell(int rows, int cols) {
    if (_selectedCell != null) {
      int row = Math.max(1, _selectedCell.row + rows);
      int col = Math.max(1, _selectedCell.col + cols);
      _selectedCell = new CellLocation(_spreadsheet, new RowCol(row, col));
      _selectionCorner = _selectedCell;
      selectionChanged();
    }
  }

  void applyToSelectedCells(SelectionApply apply) {
    CellRange range = _getSelectionRange(_selectedCell, _selectionCorner);
    if (range == null) {
      return;
    }

    range.forEach(apply);
  }

  // Hide the selection marquee and empty the selection range
  void clearSelection() {
    _selectionDiv.style.setProperty("display", "none");
    _selectedCell = _selectionCorner = null;
    selectionChanged();
  }

  // Return a BoundingBox for the given CellRange, clipped to the visible region of the table
  // TODO:  deal with full row and/or column selection
  Future<BoundingBox> getBoundingBoxForRange(CellRange r) {
    // Modify the overlay for entire row/column selection
    int minRow = r.minCorner.row;
    int maxRow = r.maxCorner.row;
    int minCol = r.minCorner.col;
    int maxCol = r.maxCorner.col;

    // FIXME - provide the table size to this class in a better way
    int maxVisibleRow = _table.numRows() - 1;
    TableRowElement tableRow = _table.getRowElement(0);
    int maxVisibleCol = tableRow.cells.length - 1;

    if (r.minCorner.row == 0 && r.maxCorner.row == 0) {
      minRow = 1;
      maxRow = maxVisibleRow + _originRow;
    }
    if (r.minCorner.col == 0 && r.maxCorner.col == 0) {
      minCol = 1;
      maxCol = maxVisibleCol + _originColumn;
    }

    // Check whether the range is entirely outside the visible area
    if (maxCol <= _originColumn) {
      return null;
    }
    if (maxRow <= _originRow) {
      return null;
    }
    if (minCol > _originColumn + maxVisibleCol) {
      return null;
    }
    if (minRow > _originRow + maxVisibleRow) {
      return null;
    }

    final completer = new Completer<BoundingBox>(); 

    // Clip the range to the visible region
    int minPinned = _pin(minRow - _originRow, 1, maxVisibleRow);
    TableRowElement minRowElmt = _table.getRowElement(minPinned);
    TableCellElement minCellElmt =
        minRowElmt.cells[_pin(minCol - _originColumn, 1, maxVisibleCol)];
    int maxPinned = _pin(maxRow - _originRow, 1, maxVisibleRow);
    TableRowElement maxRowElmt = _table.getRowElement(maxPinned);
    TableCellElement maxCellElmt =
        maxRowElmt.cells[_pin(maxCol - _originColumn, 1, maxVisibleCol)];

    // We need bounding box relative to the container which will be offset by
    // css.
    final tableRect = _table.rect;
    final minCellElmtRect = minCellElmt.rect;
    final maxCellElmtRect = maxCellElmt.rect;

    window.requestLayoutFrame(() {
      ClientRect orgP = tableRect.value.bounding;
      ClientRect minP = minCellElmtRect.value.bounding;
      ClientRect maxP = maxCellElmtRect.value.bounding;
      completer.complete(new BoundingBox(
          (minP.left - orgP.left).toInt(),
          (minP.top - orgP.top).toInt(),
          (maxP.left - minP.left + maxCellElmtRect.value.client.width).toInt(),
          (maxP.top - minP.top + maxCellElmtRect.value.client.height).toInt()));
    });
    return completer.future;
  }

  CellRange getSelectionRange() => _getSelectionRange(_selectedCell, _selectionCorner);

  bool isColumnSelected(int column) {
    CellRange r = _getSelectionRange(_selectedCell, _selectionCorner);
    if (r == null) {
      return false;
    }
    return r.minCorner.col <= column && r.maxCorner.col >= column;
  }

  bool isRowSelected(int row) {
    CellRange r = _getSelectionRange(_selectedCell, _selectionCorner);
    if (r == null) {
      return false;
    }
    return r.minCorner.row <= row && r.maxCorner.row >= row;
  }

  bool isSelectionEmpty() => _selectedCell == null;

  bool isSingleCellSelection() => _selectedCell == _selectionCorner;

  void selectionChanged() {
    if (_listener != null) {
      _listener.onSelectionChanged();
    }
  }

  void setListener(SelectionListener listener) {
    _listener = listener;
  }

  // Called when the origin of the SpreadsheetPresenter changes
  // The selection marquee is updated to the new physical position
  // and the dependencies are redisplayed
  void setOrigin(int originRow, int originColumn) {
    if (originRow == _originRow && originColumn == _originColumn) {
      return;
    }
    _originRow = originRow;
    _originColumn = originColumn;
    updateSelection();
    selectionChanged();
  }

  void setSelectedCell(int row, int col) {
    _selectedCell = new CellLocation(_spreadsheet, new RowCol(row, col));
    _selectionCorner = _selectedCell;
    selectionChanged();
  }

  // Draw or hide the selection marquee using current cell metrics
  void updateSelection() {
    CellRange r = _getSelectionRange(_selectedCell, _selectionCorner);
    if (r == null) {
      clearSelection();
      return;
    }

    getBoundingBoxForRange(r).then((BoundingBox box) {
      if (box != null) {
        _selectionDiv.style.setProperty("left", HtmlUtils.toPx(box.left));
        _selectionDiv.style.setProperty("top", HtmlUtils.toPx(box.top));
        _selectionDiv.style.setProperty("width", HtmlUtils.toPx(box.width));
        _selectionDiv.style.setProperty("height", HtmlUtils.toPx(box.height));
        _selectionDiv.style.removeProperty("display");
      } else {
        _selectionDiv.style.setProperty("display", "none");
      }
    });
  }

  // Return the selection range for the given corners.
  // If the first corner is on a row header, it is assumed that the entire
  // row is selected, and the returned range has min.col == 0 and max.col == 0.
  // If the first corner is on a column header, it is assumed that the entire
  // column is selected, and the returned range has min.row == 0 and max.row == 0.
  CellRange _getSelectionRange(CellLocation corner1, CellLocation corner2) {
    if (corner1 == null || corner2 == null) {
      return null;
    }
    if (corner1.spreadsheet != corner2.spreadsheet) {
      throw new RuntimeException("Invalid range: Corner spreadsheets do not match.");
    }

    int minRow = Math.min(corner1.row, corner2.row);
    int maxRow = Math.max(corner1.row, corner2.row);
    int minCol = Math.min(corner1.col, corner2.col);
    int maxCol = Math.max(corner1.col, corner2.col);

    // If the initial mousedown was in the upper-left corner header cell, use the entire
    // spreadsheet as the range. If the initial mousedown was on a header, use the entire
    // set of rows/columns as the range.
    if (corner1.col == 0 && corner1.row == 0) {
      return new CellRange.spreadsheet(corner1.spreadsheet);
    } else if (corner1.col == 0) {
      return new CellRange.rows(corner1.spreadsheet, minRow, maxRow);
    } else if (corner1.row == 0) {
      return new CellRange.columns(corner1.spreadsheet, minCol, maxCol);
    }

    return new CellRange(corner1.spreadsheet, new RowCol(minRow, minCol),
        new RowCol(maxRow, maxCol));
  }

  // Return the value closest to x in the range [min, max]
  int _pin(int x, int min, int max) => Math.max(Math.min(x, max), min);
}

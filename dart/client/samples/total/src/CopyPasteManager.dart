// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class CopyPasteManager {

  // Anchor cell of the current selection
  CellLocation _selectionAnchor;
  // Current selection in paste string format
  Map<CellLocation, String> _selectionContents;
  // Opposite corner cell of the current selection
  CellLocation _selectionCorner;
  // Height of the selection
  int _selectionHeight;
  // The selection manager
  SelectionManager _selectionManager;
  // Current selection styles (as copies)
  Map<CellLocation, Style> _selectionStyles;
  // Width of the selection
  int _selectionWidth;
  // Spreadsheet containing the selection
  Spreadsheet _spreadsheet;

  CopyPasteManager(this._selectionManager, this._spreadsheet) { }

  /**
   * Clear the cell contents within the selection, leaving styles intact.
   */
  void clearSelection() {
    _selectionManager.applyToSelectedCells(
      (CellLocation location) {
        // TODO:  should styles be cleared?
        _spreadsheet.clearCell(location.rowCol);
      });
  }

  /**
   * Copy the current selection contents and styles into a local copy buffer.
   */
  void copySelection() {
    if (_selectionManager.isSelectionEmpty()) {
      return;
    }

    _selectionWidth = _selectionManager.selectionCorner.col - _selectionManager.selectedCell.col;
    _selectionHeight = _selectionManager.selectionCorner.row - _selectionManager.selectedCell.row;
    _selectionContents = new Map<CellLocation, String>();
    _selectionStyles = new Map<CellLocation, Style>();
    _selectionManager.applyToSelectedCells(
      (CellLocation location) {
        Cell cell = location.getCell();
        if (cell != null) {
          _selectionContents[location] = cell.getPasteContent();
          _selectionStyles[location] = cell.style;
        } else {
          _selectionContents[location] = null;
          _selectionStyles[location] = null;
        }
      });
    if (_selectionContents.isEmpty()) {
      // Empty -> null
      _selectionContents = null;
      _selectionStyles = null;
      _selectionAnchor = null;
      _selectionCorner = null;
    } else {
      _selectionAnchor = _selectionManager.selectedCell;
      _selectionCorner = _selectionManager.selectionCorner;
    }
  }

  /**
   * Return [:true:] if the local copy buffer is non-empty.
   */
  bool hasCopiedData() => _selectionContents != null;

  /**
   * Paste data from the local copy buffer into the spreadsheet, starting at the
   * [SelectionManager]'s selected cell.
   */
  void pasteSelection() {
    if (_selectionContents == null) {
      return;
    }
    // TODO:  copy in chunks (when source is not a single row/col)
    CellLocation pasteAnchor = _selectionManager.selectedCell;
    CellLocation pasteCorner = _selectionManager.selectionCorner;
    int dx1 = pasteAnchor.col - _selectionAnchor.col;
    int dy1 = pasteAnchor.row - _selectionAnchor.row;
    int dx2 = pasteCorner.col - _selectionAnchor.col;
    int dy2 = pasteCorner.row - _selectionAnchor.row;
    int ystep = _sign(dy2 - dy1);
    int xstep = _sign(dx2 - dx1);
    if (_selectionCorner.row != _selectionAnchor.row) {
      dy2 = dy1;
    }
    if (_selectionCorner.col != _selectionAnchor.col) {
      dx2 = dx1;
    }
    _selectionContents.forEach((CellLocation location, String contents) {
      int dy = dy1;
      while (true) {
        int dx = dx1;
        while (true) {
          CellLocation newLocation = new CellLocation(pasteAnchor.spreadsheet,
            new RowCol(location.row + dy, location.col + dx));
          if (contents != null &&
              newLocation.row >= 1 &&
              newLocation.col >= 1) {
            newLocation.setCellFromContentString(contents);
            newLocation.spreadsheet.setCellStyle(newLocation.rowCol, _selectionStyles[location]);
          }
          if (dx == dx2) {
            break;
          }
          dx += xstep;
        }
        if (dy == dy2) {
          break;
        }
        dy += ystep;
      }
    });
  }

  int _sign(int x) {
    if (x < 0) {
      return -1;
    } else if (x > 0) {
      return 1;
    }
    return 0;
  }
}

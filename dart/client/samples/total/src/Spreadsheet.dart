// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

typedef void RowColActionFunction(RowCol rowCol);
typedef void RowColCellActionFunction(RowCol rowCol, Cell value);

/**
 * Spreadsheet data model.
 *
 * To recalculate when a single cell or a group of cells have changed:
 *
 * Spreadsheet spreadsheet;
 *
 * spreadsheet.clearDirtyCells();
 * spreadsheet.beginRecalc();
 * // Mark cells as dirty when they are changed or deleted
 * RowCol rowCol = ...;
 * String contents = ...;
 * Style style = ...;
 * setCellContents(rowCol, contents, style);
 * spreadsheet.markDirty(rowCol);
 * // etc.
 * spreadsheet.forEachDirtyCell((RowCol rowCol, Cell cell) {
 *   String html = cell.toHtml();
 *   // display the cell
 * });
 * int calculated = spreadsheet.endRecalc();
 *
 * To recalculate everything:
 *
 * spreadsheet.markAllDirty(); // or markRegionDirty
 * spreadsheet.beginRecalc();
 * spreadsheet.forEachDirtyCell((RowCol rowCol, Cell cell) {
 *   String html = cell.toHtml();
 *   // display the cell
 * });
 * int calculated = spreadsheet.endRecalc();
 */
class Spreadsheet {
  // Flags used to indicate which of a row or column we are dealing with. Used in various other
  // classes.
  static final int COL = 0;
  static final int ROW = 1;
  static final int SHEET = 2;

  static IdGenerator _idGenerator;

  Set<RowCol> _alwaysRecalculateCells;
  int _calculated; // # of cells calculated
  Map<RowCol, Cell> _cells;
  List<UndoableAction> _changeDeltas; // Cell change tracking for undo/redo

  // A map from column indices to the default style for new cells in that column.
  // The absence of a value for a given column index means that the column uses
  // the default style.
  Map<int, RowColStyle> _columnStyles;

  Set<RowCol> _dirtyCells; // Empty cells may be dirty for display, so don't store cells
  int _id;
  SpreadsheetLayout _layout;
  SpreadsheetListener _listener;
  int _maxCol;
  int _maxRow;
  String _name;
  bool _rowColumnCountsDirty;

  // A map from row indices to the default style for new cells in that row.
  // The absence of a value for a given row index means that the row uses
  // the default style.
  Map<int, RowColStyle> _rowStyles;

  // The default style for the entire sheet
  RowColStyle _sheetStyle;

  UndoStack _undoStack;

  /**
   * Return the number of cells calculated since the most recent call to [beginRecalc].
   */
  int get calculated() => _calculated;

  /**
   * Return the [SpreadsheetLayout] for this spreadsheet.
   */
  SpreadsheetLayout get layout() => _layout;

  String get name() => _name;

  UndoStack get undoStack() => _undoStack;

  Spreadsheet() : _maxRow = 0, _maxCol = 0, _calculated = 0, _rowColumnCountsDirty = false {
    if (_idGenerator == null) {
      _idGenerator = new IdGenerator();
    }
    _id = _idGenerator.next();
    // Provide a default name for the sheet
    _name = "#${_id}";
    _layout = new SpreadsheetLayout();
    _undoStack = new UndoStack();
    _alwaysRecalculateCells = new Set<RowCol>();
    _cells = new Map<RowCol, Cell>();
    _dirtyCells = new Set<RowCol>();
    _columnStyles = new Map<int, RowColStyle>();
    _rowStyles = new Map<int, RowColStyle>();
  }

  factory Spreadsheet.name(String name) {
    Spreadsheet s = new Spreadsheet();
    s._name = name;
    return s;
  }

  // Public API

  /**
   * Mark the beginning of a recalculation.  Sets the value of [calculated] to 0.
   */
  void beginRecalc() {
    _calculated = 0;
  }

  /**
   * Clear everything in a cell and remove it.
   *
   * If you would only like to clear some aspect of a cell (content, style) use the
   * setCellXXX methods with null arguments.
   */
  void clearCell(RowCol rowCol) {
    Cell cell = getCell(rowCol);
    if (cell == null) {
      return;
    }

    // Use these methods because they will correctly account for undo/redo.
    setCellContent(rowCol, null);
    setCellStyle(rowCol, null);
  }

  /**
   * Initialize the current set of dirty cells (cells in need of recalculation) to the
   * set of cells that always require recalculation (e.g., due to use of dynamic functions such
   * as [:RAND():] or [:NOW():]) and their forward dependencies.
   */
  void clearDirtyCells() {
    _dirtyCells.clear();
    _alwaysRecalculateCells.forEach((RowCol rowCol) {
      markDirty(rowCol);
    });
  }

  /**
   * Return the max of the number of columns in the spreadsheet.
   */
  int columnCount() {
    if (_rowColumnCountsDirty) {
      _refreshRowColumnCounts();
    }
    return _maxCol;
  }

  /**
   * Mark the end of a recalculation and get the number of cells that have been
   * recalculated since the last call to beginRecalc.
   */
  int endRecalc() => _calculated;

  /**
   * Execute a command and place it on the Undo stack.
   */
  void execute(Command command) {
    _undoStack.execute(command);
  }

  /**
   * Perform a given action for each dirty cell of the spreadsheet.  The action should not
   * attempt to modify the spreadsheet itself.  The action function will be passed a
   * [RowCol] reference for each cell.
   */
  void forEachDirtyCell(RowColActionFunction action) {
    _dirtyCells.forEach((RowCol rowCol) {
      action(rowCol);
    });
  }

  /**
   * Retrieve and return the given cell from the spreadsheet.  If the cell index
   * is outside the valid spreadsheet area, a [RuntimeException] will be thrown.
   *
   */
  Cell getCell(RowCol rowCol) {
    if (!rowCol.isValidCell()) {
      throw new RuntimeException("Cell index out of bounds: ${rowCol.row}, ${rowCol.col}");
    }
    return _cells[rowCol];
  }

  /**
   * Return the pixel position of the end of a given column.
   */
  int getColumnEnd(int index) => _layout.getColumnEnd(index);

  /**
   * Return the number of pixels to shift in order to place the column [origin] in
   * the leftmost position.
   */
  int getColumnShift(int origin) => _layout.getColumnShift(origin);

  /**
   * Return the style associated with a column, or [:null:] if none.
   *
   * When a previously empty cell in the column is given new contents, the column's style (if any)
   * will be merged with sheet and row styles and applied to the cell.
   */
  RowColStyle getColumnStyle(int column) => _columnStyles[column];

  /**
   * Return the width of a givencolumn.
   */
  int getColumnWidth(int index) => _layout.getColumnWidth(index);

  /**
   * Return the default column width.  If [index] is 0, return the width of the row header column
   * (i.e., the column containing the row names "1", "2", "3", etc.); otherwise, return the width
   * of an ordinary data column.
   */
  int getDefaultColumnWidth(int index) => _layout.getDefaultColumnWidth(index);

  /**
   * Return the default row height.  If [index] is 0, return the height of the column header row
   * (i.e, the row containing the column names "A", "B", "C", etc.); otherwise, return the height
   * of an ordinary data row.
   */
  int getDefaultRowHeight(int index) => _layout.getDefaultRowHeight(index);

  /**
   * Return the value at a given [RowCol] as a double.  A cell containing a String will return
   * A value of 0.0.  An exception will be thrown if the given cell is empty.
   */
  double getDoubleValue(RowCol rowCol) {
    Value value = getValue(rowCol);
    double d = value.asDouble(null);
    return d;
  }

  /**
   * Return the pixel position of the end of a given row.
   */
  int getRowEnd(int index) => _layout.getRowEnd(index);

  /**
   * Return the height of a row.
   */
  int getRowHeight(int index) => _layout.getRowHeight(index);

  /**
   * Return the number of pixels to shift in order to place row [origin] in
   * the topmost position.
   */
  int getRowShift(int origin) => _layout.getRowShift(origin);

  /**
   * Return the style associated with a row, or [:null:] if none.
   *
   * When a previously empty cell in the row is given new contents, the row style (if any)
   * will be merged with sheet and column styles and applied to the cell.
   */
  RowColStyle getRowStyle(int row) => _rowStyles[row];

  /**
   * Return the style associated with the entire spreadsheet, or [:null:] if none.
   *
   * When a previously empty cell is given new contents, the sheet style (if any)
   * will be merged with row and column styles and applied to the cell.
   */
  RowColStyle getSheetStyle() => _sheetStyle;

  /**
   * Evaluate the given cell as a double value.  Throws a [ValueException] if the cell
   * is not present.  Tracks the number of dirty cells calculated.
   */
  Value getValue(RowCol rowCol) {
    // getCell will call rowCol.isValidCell
    Cell cell = getCell(rowCol);
    if (cell == null) {
      throw new ValueException();
    }
    return cell.getValue();
  }

  int hashCode() => _id * 2654435769;

  /**
   * Increase the value of the counter returned by [calculated].
   */
  void incrementCalculated() {
    _calculated++;
  }

  /**
   * Insert a contiguous block of cells into the sheet, moving displaced cells down.
   * Shifted cells are those in the range [:minCorner.col <= cell.col <= maxCorner.col:] with
   * [:cell.row >= minCorner.row:]. Shifted cells are moved [:(maxCorner.row - minCorner.row + 1):]
   * rows downward.
   */
  void insertBlockAndShiftDown(RowCol minCorner, RowCol maxCorner) {
    RowCol shiftedMaxCorner = new RowCol(rowCount(), maxCorner.col);
    RowCol shiftOffset = new RowCol(maxCorner.row - minCorner.row + 1, 0);
    _shiftBlock(minCorner, shiftedMaxCorner, shiftOffset);
  }

  /**
   * Insert a contiguous block of cells into the sheet, moving displaced cells right.
   * Shifted cells are those in the range [:minCorner.row <= cell.row <= maxCorner.row:] with
   * [:cell.col >= minCorner.col:]. Shifted cells are moved [:(maxCorner.col - minCorner.col + 1):]
   * columns rightward.
   */
  void insertBlockAndShiftRight(RowCol minCorner, RowCol maxCorner) {
    RowCol shiftedMaxCorner = new RowCol(maxCorner.row, columnCount());
    RowCol shiftOffset = new RowCol(0, maxCorner.col - minCorner.col + 1);
    _shiftBlock(minCorner, shiftedMaxCorner, shiftOffset);
  }

  /**
   * Insert a new column with the given width. The listener's
   * [SpreadsheetListener.onColumnInserted] method is called with the column index.
   */
  void insertColumn(int index, int width) {
    _layout.insertColumn(index, width);
    _listener.onColumnInserted(index);
  }

  // Insert an entire column at the given index, moving displaced cells to the right.
  void insertColumns(int minColumn, int maxColumn) {
    insertBlockAndShiftRight(new RowCol(0, minColumn), new RowCol(rowCount(), maxColumn));

    _columnStyles = _insertRowColStyles(minColumn, maxColumn - minColumn + 1, _columnStyles);
  }

  /**
   * Insert a new row with the given height. The listener's
   * [SpreadsheetListener.onRowInserted] method is called with the column index.
   */
  void insertRow(int index, int height) {
    _layout.insertRow(index, height);
    _listener.onRowInserted(index);
  }

  /**
   * Insert a block of rows at the given index, moving displaced cells downward.
   */
  void insertRows(int minRow, int maxRow) {
    insertBlockAndShiftDown(new RowCol(minRow, 0), new RowCol(maxRow, columnCount()));

    _rowStyles = _insertRowColStyles(minRow, maxRow - minRow + 1, _rowStyles);
  }

  /**
   * Mark all spreadsheet cells as dirty.
   *
   * This forces recomputation of dependencies of all cells within the viewport.
   *
   * Since this method requires visiting all spreadsheet cells, it should be avoided where possible
   * in favor of more targeted calls to [markDirty] on cells known to be in need of recalculation.
   */
  void markAllDirty() {
    _dirtyCells.clear();
    _cells.forEach((RowCol rowCol, Cell cell) {
      cell.setContentDirty();
      _dirtyCells.add(rowCol);
    });
  }

  /**
   * Mark the cell at the given (row, col), and all the cells that depend (transitively)
   * on it as dirty.
   */
  void markDirty(RowCol rowCol) {
    _markDirty(rowCol, true);
  }

  /**
   * Mark a rectangular region of spreadsheet cells as dirty.
   *
   * (minRow, minCol) is inclusive and (maxRow, maxCol) is exclusive.
   * This does not invalidate cells, it only adds the cells to
   * the set which will be iterated over by forEachDirtyCell.
   */
  void markRegionDirty(int minRow, int minCol, int maxRow, int maxCol) {
    _dirtyCells.clear();
    for (int r = minRow; r < maxRow; r++) {
      for (int c = minCol; c < maxCol; c++) {
        RowCol rowCol = new RowCol(r, c);
        Cell cell = getCell(rowCol);
        if (cell != null) {
          _dirtyCells.add(rowCol);
        }
      }
    }
  }

  /**
   * Call the listener's [SpreadsheetListener.onRefresh] method.
   */
  void refresh() {
    if (_listener != null) {
      _listener.onRefresh();
    }
  }

  /**
   * Remove a contiguous block of cells from the sheet, moving cells left to fill the gap.
   * Shifted cells are those in the range [:minCorner.row <= cell.row <= maxCorner.row:] with
   * [:cell.col > maxCorner.col:]. Shifted cells are moved [:(maxCorner.col - minCorner.col + 1):]
   * columns leftward.
   */
  void removeBlockAndShiftLeft(RowCol minCorner, RowCol maxCorner) {
    RowCol shiftOffset = new RowCol(0, minCorner.col - maxCorner.col - 1);

    _removeBlock(minCorner, maxCorner, shiftOffset);

    if (maxCorner.col + 1 <= columnCount()) {
      RowCol shiftedMinCorner = new RowCol(minCorner.row, maxCorner.col + 1);
      RowCol shiftedMaxCorner = new RowCol(maxCorner.row, columnCount());
      _shiftBlock(shiftedMinCorner, shiftedMaxCorner, shiftOffset);
    }

    _refreshRowColumnCounts();
  }

  /**
   * Remove a contiguous block of cells into the sheet, moving cells up to fill the gap.
   * Shifted cells are those in the range [:minCorner.col <= cell.col <= maxCorner.col:] with
   * [:cell.row > maxCorner.row:]. Shifted cells are moved [:(maxCorner.row - minCorner.row + 1):]
   * rows upward.
   */
  void removeBlockAndShiftUp(RowCol minCorner, RowCol maxCorner) {
    RowCol shiftOffset = new RowCol(minCorner.row - maxCorner.row - 1, 0);

    _removeBlock(minCorner, maxCorner, shiftOffset);

    if (maxCorner.row + 1 <= rowCount()) {
      RowCol shiftedMinCorner = new RowCol(maxCorner.row + 1, minCorner.col);
      RowCol shiftedMaxCorner = new RowCol(rowCount(), maxCorner.col);
      _shiftBlock(shiftedMinCorner, shiftedMaxCorner, shiftOffset);
    }

    _refreshRowColumnCounts();
  }

  /**
   * Remove entire columns in the given range (inclusive), moving cells to the left to fill the gap.
   */
  void removeColumns(int minColumn, int maxColumn) {
    // FIXME: When style is applied at the column header level, or there are other per-column
    // properties, we need to shift the column header information too.
    removeBlockAndShiftLeft(new RowCol(1, minColumn), new RowCol(rowCount(), maxColumn));

    _columnStyles = _removeRowColStyles(minColumn, maxColumn, COL, _columnStyles);
  }

  /**
   * Remove entire rows in the given range (inclusive), moving cells up to fill the gap.
   */
  void removeRows(int minRow, int maxRow) {
    removeBlockAndShiftUp(new RowCol(minRow, 1), new RowCol(maxRow, columnCount()));

    _rowStyles = _removeRowColStyles(minRow, maxRow, ROW, _rowStyles);
  }

  /**
   * Return the number of rows in the spreadsheet.
   */
  int rowCount() {
    if (_rowColumnCountsDirty) {
      _refreshRowColumnCounts();
    }

    return _maxRow;
  }

  /**
   * Set the content at a given location.
   *
   * The new content may be null, in which case the cell will be removed if it also has default
   * style and no dependents.
   *
   * This method records information for undo/redo.
   */
  void setCellContent(RowCol rowCol, CellContent newContent) {
    // Get the cell, creating it if it does not already exist.
    Cell cell = getCell(rowCol);
    if (cell == null) {
      if (newContent == null) {
        return;
      }
      cell = _createEmptyCell(rowCol);
      if (cell == null) {
        return;
      }
    }

    // Undo/redo information
    if (_changeDeltas != null) {
      _changeDeltas.add(new CellContentAction(rowCol, cell.content));
    }

    // Check if we previously had content but don't now, or vice versa. If so, we may need to
    // recompute the row column count for the sheet.
    if ((newContent == null) != (cell.content == null)) {
      if (rowCol.row == _maxRow || rowCol.col == _maxCol) {
        // Force recalculation of _maxRow and _maxCol the next time they are queried
        _rowColumnCountsDirty = true;
      }
    }

    // Clear data related to the previous content
    CellLocation cellLocation = new CellLocation(this, rowCol);
    _removeFromDependencies(cellLocation, cell);
    _alwaysRecalculateCells.remove(rowCol);

    // See if we need a new cell at all
    if (newContent == null && cell.isStyleDefault() && cell.dependents == null) {
      _removeCell(rowCol);
      return;
    }

    // Set data for the new content
    cell.setContent(newContent);
    _addToDependencies(cellLocation, cell);
    if (cell.alwaysRecalculate()) {
      // Add cell to list of cells to always recalculate
      _alwaysRecalculateCells.add(rowCol);
    }
    setRowCount(rowCol.row);
    setColumnCount(rowCol.col);
    markDirty(rowCol);
  }

  /**
   * Set a cell of the spreadsheet, given a literal or formula and the style.
   *
   * The most appropriate cell type for the contents is created. The style, if
   * non null, is applied. If null, style from an existing cell or row/column
   * styles will be used. In the absence of other style information, the
   * default style is used.
   *
   * The new cell is returned.
   *
   * An exception is thrown if the requested cell RowCol is invalid.
   */
  Cell setCellFromContentString(RowCol rowCol, String contents) {
    // getCell will call rowCol.isValidCell
    Cell cell = getCell(rowCol);
    if (cell == null) {
      cell = _createEmptyCell(rowCol);
      if (cell == null) {
        return null;
      }
    }

    CellContent newContent = null;
    double date;
    // Modify the selected cell with the contents of the input
    if (contents.length > 0 && contents[0] == "="[0]) {
      // '=' starts a formula
      StringFormula formula = new StringFormula(contents, new CellLocation(this, rowCol));
      newContent = new FormulaContent(new CellLocation(this, rowCol), formula);
    } else if (StringUtils.isNumeric(contents)) {
      // It's a number in double format
      newContent = new ValueContent(new DoubleValue(Math.parseDouble(contents)), contents);
    } else if ((date = DateUtils.parseDate(contents)) != -1.0) {
      // It's a date (i.e., the number of days since the epoch, formatted as a date)
      newContent = new ValueContent(new DateValue(date), contents);
    } else if (contents == "TRUE" || contents == "FALSE") {
      newContent = new ValueContent(new BooleanValue(contents == "TRUE"), contents);
    } else {
      // It's a String
      newContent = new ValueContent(new StringValue(contents), contents);
    }

    setCellContent(rowCol, newContent);

    return cell;
  }

  /**
   * Set the style at a given location.
   *
   * The new style may be null, in which case the cell will be removed if it also has no content
   * and no dependents.
   *
   * This method records information for undo/redo.
   */
  void setCellStyle(RowCol rowCol, Style newStyle) {
    // Get the cell, creating it if it doesn't exist
    Cell cell = getCell(rowCol);
    if (cell == null) {
      if (newStyle == null || newStyle.isDefault()) {
        return;
      }
      cell = _createEmptyCell(rowCol);
      if (cell == null) {
        return;
      }
    }

    // Save for undo/redo
    if (_changeDeltas != null) {
      _changeDeltas.add(new CellStyleAction(rowCol, cell.style));
    }

    // Remove the cell if we no longer need it
    if ((newStyle == null || newStyle.isDefault()) && cell.content == null &&
        cell.dependents == null) {
      _removeCell(rowCol);
      return;
    }
    cell.setStyle(newStyle);
    markDirty(rowCol);
  }

  /**
   * Set the column count. The new column count will be the greater of the previous column count
   * and the given count.
   */
  // TODO: Support shrinking the table
  void setColumnCount(int numColumns) {
    if (_rowColumnCountsDirty) {
      _refreshRowColumnCounts();
    }
    _maxCol = Math.max(_maxCol, numColumns);
    _tableSizeChanged();
  }

  /**
   * Set the [Style] associated with the given [column], replacing any existing setting.
   * A [:null:] value for [style] removes any previous setting.
   */
  void setColumnStyle(int column, RowColStyle style) {
    // Do nothing if the style isn't changing.
    if (style == _columnStyles[column]) {
      return;
    }

    // Update the column style
    if (_changeDeltas != null) {
      _changeDeltas.add(new RowColStyleAction(column, COL, _columnStyles[column]));
    }
    _columnStyles[column] = style;
  }

  /**
   * Change the width of a column.
   */
  void setColumnWidth(int index, int width) {
    _layout.setColumnWidth(index, width);
    _listener.onColumnWidthChanged(index, width);
  }

  /**
   * Set a [SpreadsheetListener] that will receive notifications of changes to the spreadsheet.
   */
  void setListener(SpreadsheetListener listener) {
    _listener = listener;
  }

  /**
   * Set the row count. The new row count will be the greater of the previous row count and the
   * given count.
   */
  // TODO: Support shrinking the table
  void setRowCount(int numRows) {
    if (_rowColumnCountsDirty) {
      _refreshRowColumnCounts();
    }
    _maxRow = Math.max(_maxRow, numRows);
    _tableSizeChanged();
  }

  /**
   * Change the height of a row.
   */
  void setRowHeight(int index, int height) {
    _layout.setRowHeight(index, height);
    _listener.onRowHeightChanged(index, height);
  }

  /**
   * Set the [Style] associated with the given [column], replacing any existing setting.
   * A [:null:] value for [style] removes any previous setting.
   */
  void setRowStyle(int row, RowColStyle style) {
    // Do nothing if the style isn't changing.
    if (style == _rowStyles[row]) {
      return;
    }

    // Update the row style
    if (_changeDeltas != null) {
      _changeDeltas.add(new RowColStyleAction(row, ROW, _rowStyles[row]));
    }
    _rowStyles[row] = style;
  }

  /**
   * Set the [Style] associated with the entire spreadsheet, replacing any existing setting.
   * A [:null:] value for [style] removes any previous setting.
   *
   * Existing row and column styles are modified to take the new sheet style into account
   * by removing any values that are set in the sheet style.  Row and column styles that have
   * become empty are removed.
   */
  void setSheetStyle(RowColStyle style) {
    // Do nothing if the style isn't changing.
    if (style == _sheetStyle) {
      return;
    }

    // Update the sheet style
    if (_changeDeltas != null) {
      _changeDeltas.add(new RowColStyleAction(0, SHEET, _sheetStyle));
    }
    _sheetStyle = style;
    if (style == null) {
      return;
    }

    // Clear row/column style settings that are superseded by the sheet style
    Map<int, RowColStyle> rowStylesToReplace = new Map<int, RowColStyle>();
    Map<int, RowColStyle> columnStylesToReplace = new Map<int, RowColStyle>();

    _rowStyles.forEach((int row, RowColStyle s) {
      Style newStyle = s.style.clearSettings(style.style);
      if (newStyle !== s.style) {
        if (_changeDeltas != null) {
          _changeDeltas.add(new RowColStyleAction(row, ROW, s));
        }
        if (newStyle.isEmpty()) {
          rowStylesToReplace[row] = null;
        } else {
          rowStylesToReplace[row] = new RowColStyle(newStyle);
        }
      }
    });

    _columnStyles.forEach((int col, RowColStyle s) {
      Style newStyle = s.style.clearSettings(style.style);
      if (newStyle !== s) {
        if (_changeDeltas != null) {
          _changeDeltas.add(new RowColStyleAction(col, COL, s));
        }
        if (newStyle.isEmpty()) {
          columnStylesToReplace[col] = null;
        } else {
          columnStylesToReplace[col] = new RowColStyle(newStyle);
        }
      }
    });

    // Delete row/column styles that have become empty
    rowStylesToReplace.forEach((int index, RowColStyle s) {
      if (s == null) {
        _rowStyles.remove(index);
      } else {
        _rowStyles[index] = s;
      }
    });
    columnStylesToReplace.forEach((int index, RowColStyle s) {
      if (s == null) {
        _columnStyles.remove(index);
      } else {
        _columnStyles[index] = s;
      }
    });
  }

  /**
   * Initiate tracking of changed cells.
   *
   * The caller can supply a list to hold cell change deltas (of type [UndoableAction]) which
   * will be filled in during calls to [setCellContent()] or [setCellStyle()]. Providing a null
   * list disables tracking.
   */
  void trackDeltas(List<UndoableAction> changeDeltas) {
    _changeDeltas = changeDeltas;
  }

  ////////////////////////////////////////////////////
  //
  // Private methods - not part of the Spreadsheet API
  //
  ////////////////////////////////////////////////////

  // For each cell on which the given cell depends, add this (row, col) as a dependent.
  // If a dependency is null, create an empty cell to hold the forward dep.
  void _addToDependencies(CellLocation thisLocation, Cell cell) {
    Set<CellLocation> deps = cell.getDependencies();
    if (deps != null) {
      deps.forEach((CellLocation location) {
        // Ignore dependencies outside the spreadsheet area
        if (location.isValidCell()) {
          Cell c = location.getCell();
          if (c == null) {
            c = location.spreadsheet._createEmptyCell(location.rowCol);
          }
          c.addDependent(thisLocation);
        }
      });
    }
  }

  /**
   * Create an empty cell as a place holder.
   *
   * Returns the new cell, or null if the requested rowCol is invalid.
   */
  Cell _createEmptyCell(RowCol rowCol) {
    assert(getCell(rowCol) == null);
    if (rowCol.isValidCell()) {
      Cell newCell = new Cell();

      _cells[rowCol] = newCell;

      // Infer style information from the sheet and/or row and/or column
      RowColStyle sheetStyle = getSheetStyle();
      RowColStyle rowStyle = getRowStyle(rowCol.row);
      RowColStyle columnStyle = getColumnStyle(rowCol.col);
      Style style = RowColStyle.merge3(sheetStyle, rowStyle, columnStyle);
      if (style != null) {
        // Do not use setCellStyle, because we do not want to store the original style for
        // this new cell, and we do not want the side effect of deleting an empty cell with
        // the default style.
        newCell.setStyle(style);
        markDirty(rowCol);
      }

      return newCell;
    } else {
      return null;
    }
  }

  // Insert a set of row or column headers. Any style in a column higher than the min index will
  // be shifted by the given offset. Return a new set of styles.
  Map<int, RowColStyle> _insertRowColStyles(int index, int offset, Map<int, RowColStyle> existing) {
    // FIXME: This is nowhere near as efficient as it could be.
    // We really need the row/col style info to be some kind of data structure that
    // supports fast insert/delete/lookup and the ability to shift a range of keys.
    Map<int, RowColStyle> newStyles = new Map<int, RowColStyle>();
    existing.forEach(void shiftStyles(int i, RowColStyle style) {
      if (i < index) {
        newStyles[i] = existing[i];
      } else {
        newStyles[i + offset] = existing[i];
      }
    });
    return newStyles;
  }

  // Private version to handle recursion.  When a dirty cell is encountered, stop the recursion
  // unless the 'force' flag is true.
  void _markDirty(RowCol rowCol, bool force) {
    Cell cell = getCell(rowCol);
    _dirtyCells.add(rowCol);
    if (cell != null && (force || !cell.isDirty())) {
      cell.setContentDirty();
      Set<CellLocation> dependents = cell.dependents;
      if (dependents != null) {
        dependents.forEach((CellLocation location) {
          location.spreadsheet._markDirty(location.rowCol, false);
        });
      }
    }
  }

  // Scan the spreadsheet to determine the maxinum populated row and column
  void _refreshRowColumnCounts() {
    _maxRow = -1;
    _maxCol = -1;
    _cells.forEach((RowCol key, Cell value) {
      _maxRow = Math.max(_maxRow, key.row);
      _maxCol = Math.max(_maxCol, key.col);
    });
    _rowColumnCountsDirty = false;
    _tableSizeChanged();
  }

  // Remove a block from the sheet, and update dependencies appropriately.
  void _removeBlock(RowCol minCorner, RowCol maxCorner, RowCol shiftOffset) {
    CellRange range = new CellRange(this, minCorner, maxCorner);

    // Gather all of the cells that depend on the region to remove,
    // and at the same time remove the cells in the region.
    Set<CellLocation> allDependents = new Set<CellLocation>();
    range.forEach((CellLocation targetLocation) {
      Cell cell = getCell(targetLocation.rowCol);
      if (cell != null) {
        if (cell.dependents != null) {
          allDependents.addAll(cell.dependents);
        }
        clearCell(targetLocation.rowCol);
      }
    });

    // Process the dependent cells. We gather and then process in this way to
    // avoid modifying cell range references more than once.
    allDependents.forEach((CellLocation dependentLocation) {
      // The cell may have been deleted as part of the range, so could be null
      Cell dependentCell = dependentLocation.getCell();
      if (dependentCell != null) {
        _removeFromDependencies(dependentLocation, dependentCell);
        CellContent formerContent =
          dependentCell.invalidateReferences(dependentLocation, range, shiftOffset);
        if (formerContent != null && _changeDeltas != null) {
          _changeDeltas.add(new CellContentAction(dependentLocation.rowCol, formerContent));
        }
        _addToDependencies(dependentLocation, dependentCell);
      }
    });
  }

  // Remove a cell entirely from the spreadsheet
  void _removeCell(RowCol rowCol) {
    _cells.remove(rowCol);
  }

  // For each cell on which the given cell depends, remove this (row, col) as a dependent.
  void _removeFromDependencies(CellLocation thisLocation, Cell cell) {
    Set<CellLocation> odeps = cell.getDependencies();
    if (odeps != null) {
      odeps.forEach((CellLocation location) {
        // Ignore dependencies outside the spreadsheet area
        if (location.isValidCell()) {
          Cell c = location.getCell();
          if (c != null) {
            c.removeDependent(thisLocation);
            // Remove the cell if we no longer need it
            if (c.content == null && c.dependents == null && c.isStyleDefault()) {
              _removeCell(location.rowCol);
            }
          }
        }
      });
    }
  }

  // Remove a set of row or column headers, between min and max inclusive.
  // Return a new set of styles.
  Map<int, RowColStyle> _removeRowColStyles(int minIndex, int maxIndex, int rowOrCol,
      Map<int, RowColStyle> existing) {
    // FIXME: See comment in insertRowColStyles
    Map<int, RowColStyle> newStyles = new Map<int, RowColStyle>();
    int offset = maxIndex - minIndex + 1;
    existing.forEach(void shiftStyles(int index, RowColStyle style) {
      if (index < minIndex) {
        newStyles[index] = existing[index];
      } else if (index > maxIndex) {
        if (_changeDeltas != null) {
          _changeDeltas.add(
              new RowColStyleAction(index - offset, rowOrCol, existing[index - offset]));
        }
        newStyles[index - offset] = existing[index];
      }
    });
    return newStyles;
  }

  // Shift a block of cells by the given offset.
  void _shiftBlock(RowCol minCorner, RowCol maxCorner, RowCol offset) {
    CellRange range = new CellRange(this, minCorner, maxCorner);
    Map<RowCol, Cell> cellsToShift = new Map<RowCol, Cell>();
    _cells.forEach((RowCol key, Cell cell) {
      CellLocation keyLocation = new CellLocation(this, key);
      bool inRange = range.isInRange(keyLocation);
      if (inRange) {
        // Remove from dependencies now because the set of dependencies
        // will change later. We re-add to dependencies when we eventually
        // call setCellContent.
        cellsToShift[key] = cell;
        _removeFromDependencies(keyLocation, cell);
        markDirty(key);
      }
      // TODO: The following code will fail with multiple sheets.
      // It should look at all forward dependencies of cells that are
      // in range, and modify those. As it stands, it will not modify
      // dependencies in other spreadsheets.
      CellContent formerContent = cell.modifyDependenciesForShift(range, offset);
      if (formerContent != null && _changeDeltas != null) {
        _changeDeltas.add(new CellContentAction(key, formerContent));
      }
    });
    cellsToShift.forEach((RowCol key, Cell cell) {
      if (cell != null && _changeDeltas != null) {
        _changeDeltas.add(new CellContentAction(key, cell.content));
        _changeDeltas.add(new CellStyleAction(key, cell.style));
      }
      _removeCell(key);
    });
    cellsToShift.forEach((RowCol key, Cell cell) {
      RowCol newKey = key + offset;
      setCellContent(newKey, cell.content);
      setCellStyle(newKey, cell.style);
      Cell newCell = getCell(newKey);
      newCell.addDependents(cell.dependents);
    });
  }

  // Refresh presenter table sizes (affects scrollbars)
  void _tableSizeChanged() {
    if (_listener != null) {
      _listener.onTableSizeChanged();
    }
  }
}

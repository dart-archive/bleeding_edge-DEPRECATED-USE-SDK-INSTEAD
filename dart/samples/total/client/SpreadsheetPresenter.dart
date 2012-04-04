// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

typedef void EditCellRangeFunction(CellRange range);

/**
 * Spreadsheet widget.
 */
class SpreadsheetPresenter implements SpreadsheetListener, SelectionListener {

  // Constants controlling the grid display type
  static final int CELL_DISPLAY_VALUES = 0; // default spreadsheet view
  static final int CELL_DISPLAY_CONTENTS = 1; // show cells as types
  static final int CELL_DISPLAY_PASTE_CONTENTS = 2; // cell contents canonicalized for pasting
  static final int CELL_DISPLAY_STYLES = 3; // cell styles
  static final int CELL_DISPLAY_DATATYPE = 4; // cell datatypes
  static final int CELL_DISPLAY_DEPS = 5; // cell dependencies
  static final int CELL_DISPLAY_FORWARD_DEPS = 6; // cell forward dependencies

  // Constants to access row/col size and position lists
  // This assumes that Spreadsheet has already had an instance created.
  static final int COL = Spreadsheet.COL;
  static final int ROW = Spreadsheet.ROW;

  // The number of pixels around a row/column boundary for which a resize dragger will be shown
  static final int _DRAGGER_TOLERANCE = 4;

  static int _zIndex = 1;

  // The currently active inner menu
  InnerMenuView _activeInnerMenu;

  // The current display type
  int _cellDisplay;

  Element _cellDisplayElement;
  int _columnShift;
  int _columns;
  ContextMenu _contextMenu;
  CopyPasteManager _copyPasteManager;
  EventListener _drag;

  // Indexed as _dragIndicators[COL], _dragIndicators[ROW]
  List<DivElement> _dragIndicators;

  bool _dragging;

  // When the user is editing formula and clicks on some cell, we start cell selection
  bool _formulaCellSelecting;

  RowCol _formulaCellSelectingClickLocation;
  RowCol _formulaCellSelectingDragLocation;
  int _formulaCellSelectingTextEnd;
  int _formulaCellSelectingTextStart;

  int _formulaCellWidth;
  Element _formulaDiv;

  // When the user activates the formula input box, allow the left and right arrow keys
  // to move within the formula rather than moving the selected cell
  bool _formulaEditing;

  InputElement _formulaInput;
  Element _formulaInputMeasure;

  // The row the active inner menu is attached to
  int _innerMenuRowIndex;

  // We increment this ID for each request to show inner menu, to detect situation
  // when user requested showing, but quickly moved to a different cell.
  int _innerMenuShowRequestId;

  // True if the inner menu is being shown.  This will remain true even while
  // the menu is being hidden and re-shown due to movement of the selection.
  bool _innerMenuShown;

  EventListener _move;
  Element _moveDragger;
  PopupHandler _popupHandler;
  Element _resizeDragger;
  int _rowShift;
  int _rows;

  // True if the table is being scrolled in response to a keyboard event.
  // In this case, we don't want the table's on.scroll listener to perform
  // repositioning.
  bool _scrolledByKeyboard;

  // Cells on which the currently selected cell depends
  Set<CellLocation> _selectedCellDependencies;
  // Cells which depend on the currently selected cell
  Set<CellLocation> _selectedCellDependents;

  SelectionManager _selectionManager;
  Spreadsheet _spreadsheet;
  Element _spreadsheetElement;
  HtmlTable _table;
  Element _tableScrollContainer;
  Element _tableScrollDiv;
  int _tableWidth;
  EventListener _undrag;
  Window _window;

  // Public getters

  SelectionManager get selectionManager() => _selectionManager;

  Spreadsheet get spreadsheet() => _spreadsheet;

  Element get spreadsheetElement() => _spreadsheetElement;

  Window get window() => _window;

  factory SpreadsheetPresenter.blank(Window window) {
    Spreadsheet spreadsheet = new Spreadsheet();
    SpreadsheetPresenter presenter = new SpreadsheetPresenter(spreadsheet, window, 0,0, 25, 10);
    spreadsheet.setListener(presenter);
    return presenter;
  }

  SpreadsheetPresenter(this._spreadsheet, this._window, this._rowShift, this._columnShift,
      this._rows, this._columns) {
    Document doc = _window.document;

    // Must do this first
    _popupHandler = new PopupHandler(doc);

    // Create UI elements under a common parent
    Element parent = doc.query("#spreadsheets");

    _spreadsheetElement = new Element.tag("div");
    _spreadsheetElement.id = "spreadsheet-${_spreadsheet.name}";
    _spreadsheetElement.attributes["class"] = "spreadsheetContainer";
    _spreadsheetElement.style.setProperty("z-index", "${_zIndex++}");
    parent.nodes.add(_spreadsheetElement);

    _tableScrollContainer = new Element.tag("div");
    _tableScrollContainer.id = "tableScrollContainer-${_spreadsheet.name}";
    _tableScrollContainer.attributes["class"] = "tableScrollContainer";
    _spreadsheetElement.nodes.add(_tableScrollContainer);

    _createTable(doc);
    _createMoveDragger(doc);
    _createResizeDragger(doc);

    _createSpreadsheetLayout(doc);
    _createFormulaInput(doc);
    _createCellDisplaySelector(doc);
    _setupTableMouseListeners();

    _selectionManager = new SelectionManager(_spreadsheet, this, _window, _table);
    _selectionManager.setListener(this);

    _contextMenu = new ContextMenu(this);
    new ContextMenuBuilder(this, spreadsheet, selectionManager, _contextMenu).build();

    _copyPasteManager = new CopyPasteManager(_selectionManager, _spreadsheet);

    // Disable default right-click behavior
    _window.on.contextMenu.add((Event event) { return false; });

    // Capture mouse move and mouse up events at the document-level, displatching to
    // 'drag' and 'undrag' functions supplied by the most recently invoked 'mouseDown'
    // handler. The 'drag' function will be called for every mouseMove and mouseUp position
    // and the 'undrag' function will be called at mouseUp. When no 'drag' function is specified,
    // a 'move' function will be called if specified. On mouseUp, the 'drag' and 'undrag' functions
    // are set to null.

    _window.document.on.mouseMove.add((MouseEvent e) {
      if (_drag != null) {
        _drag(e);
      } else if (_move != null) {
        _move(e);
      }
    });

    _window.document.on.mouseUp.add((MouseEvent e) {
      if (_drag != null) {
        _dragging = false;
        _drag(e);
        _drag = null;
      }
      if (_undrag != null) {
        _undrag(e);
        _undrag = null;
      }
    });

    // Initialize variables
    _cellDisplay = CELL_DISPLAY_VALUES;
    _dragging = false;
    _formulaCellSelecting = false;
    _formulaCellSelectingTextEnd = -1;
    _formulaCellSelectingTextStart = -1;
    _formulaCellWidth = 0;
    _formulaEditing = false;
    _innerMenuRowIndex = -1;
    _innerMenuShown = false;
    _innerMenuShowRequestId = 0;
    _scrolledByKeyboard = false;
  }

  // Clear the cells of the current selection, preserving styles (?).
  void clearSelection() {
    _copyPasteManager.clearSelection();
  }

  // Copy the current selection contents and styles.
  void copySelection() {
    _copyPasteManager.copySelection();
  }

  bool hasCopiedData() => _copyPasteManager.hasCopiedData();

  void onColumnInserted(int col) {
    _columns++;
    _addTableColumnHtml(_columns);
  }

  void onColumnWidthChanged(int col, int size) {
    _rowColSizeChanged();
  }

  void onRefresh() {
    _table.resetTableContents(_rows, _columns);
    _selectionManager.setOrigin(_rowShift, _columnShift);
    recalculateViewport();
  }

  void onRowHeightChanged(int row, int size) {
    _rowColSizeChanged();
  }

  void onRowInserted(int row) {
    _rows++;
    _addTableRowHtml(row);
  }

  void onSelectionChanged() {
    // Remove any user-typed input
    _resetFormulaInput();
    // Remove "dependency" class from the previous selection's dependencies
    if (_selectedCellDependencies != null) {
      _table.modifyClasses(_selectedCellDependencies, "dependency", false,
          _rows, _columns, _rowShift, _columnShift);
      _selectedCellDependencies = null;
    }
    // Remove "forwardDependency" class from the previous selection's forward dependencies
    if (_selectedCellDependents != null) {
      _table.modifyClasses(_selectedCellDependents, "forwardDependency", false,
          _rows, _columns, _rowShift, _columnShift);
      _selectedCellDependents = null;
    }

    bool repositioned = false;
    Style style = new Style();
    CellLocation selectedCell = _selectionManager.selectedCell;
    if (!_dragging && _selectionManager.isSingleCellSelection() && selectedCell != null) {
      if (selectedCell.row == 0 && selectedCell.col == 0) {
        RowColStyle sheetStyle = _spreadsheet.getSheetStyle();
        if (sheetStyle != null) {
          style = sheetStyle.style;
        }
      } else if (selectedCell.row == 0) {
        RowColStyle columnStyle = _spreadsheet.getColumnStyle(selectedCell.col);
        if (columnStyle != null) {
          style = columnStyle.style;
        }
      } else if (selectedCell.col == 0) {
        RowColStyle rowStyle = _spreadsheet.getRowStyle(selectedCell.row);
        if (rowStyle != null) {
          style = rowStyle.style;
        }
      } else {
        Cell c = selectedCell.getCell();
        if (c != null) {
          // Copy the cell's contents to the formula bar
          _formulaInput.value = c.getContentString();
          // Update the format <select> dropdowns
          style = c.style;
          // Add "dependency" class to the new selection's dependencies
          _selectedCellDependencies = c.getDependencies();
          if (_selectedCellDependencies != null) {
            _table.modifyClasses(_selectedCellDependencies, "dependency", true,
                _rows, _columns, _rowShift, _columnShift);
          }
          _selectedCellDependents = c.dependents;
          if (_selectedCellDependents != null) {
            _table.modifyClasses(_selectedCellDependents, "forwardDependency", true,
                _rows, _columns, _rowShift, _columnShift);
          }
        } else {
          _formulaInput.value = "";
        }
        _repositionFormulaInput(_selectionManager.selectedCell, false);
        repositioned = true;
      }
    }

    if (!repositioned) {
      _repositionFormulaInput(null, false);
    }

    // Update the state of the inner menu.
    if (!_dragging) {
      if (_selectionManager.isSelectionEmpty()) {
        _hideInnerMenu(true);
      } else {
        _innerMenuShown = true;
        _tableSizeChanged();
        _scheduleShowInnerMenu(style);
      }
    }

    if (_selectionManager.isSelectionEmpty()) {
      _hideFormula();
      return;
    }
  }

  void onTableSizeChanged() {
    _tableSizeChanged();
  }

  // Paste the current selection into the spreadsheet, overwriting the current cell contents
  // and styles.
  void pasteSelection() {
    _copyPasteManager.pasteSelection();

    CellLocation pasteAnchor = _selectionManager.selectedCell;
    Cell anchor = pasteAnchor.getCell();
    if (anchor != null) {
      if (_activeInnerMenu != null) {
        _activeInnerMenu.updateStyleUI(anchor.style);
      }
    }
    // Fit the selection around the pasted area
    int selectionWidth = _selectionManager.selectionCorner.col
        - _selectionManager.selectedCell.col;
    int selectionHeight = _selectionManager.selectionCorner.row
        - _selectionManager.selectedCell.row;
    _selectionManager.selectionCorner = new CellLocation(_spreadsheet,
        new RowCol(pasteAnchor.row + selectionHeight, pasteAnchor.col + selectionWidth));
    _selectionManager.updateSelection();
  }

  void performEditOnSelection(EditCellRangeFunction editOperation) {
    // Get the selected column
    CellRange selection = _selectionManager.getSelectionRange();
    if (selection == null) {
      return;
    }
    editOperation(selection);
    recalculate();
  }

  void popdownMenu(Element menuElement, int value,
      void callback(Element elmt, int value)) {
    _popupHandler.deactivatePopup();
    callback(menuElement, value);
  }

  void popupMenu(Element menuElement, int x, int y) {
    _hideFormula();
    _popupHandler.activatePopup(menuElement, x, y);
  }

  // Recalculate the spreadsheet.  The caller is responsible for marking as dirty any cells
  // that require actual recalculation. Code that causes the table to be resized is responsible
  // for updating the DOM table size before this method is called.
  int recalculate() {
    int start = _currentTimeMllis();
    _spreadsheet.beginRecalc();

    _setTableWidth(_getVisibleTableWidth());
    _tableSizeChanged();

    int renderedCells = _table.redraw(_selectionManager, _rows, _columns,
        _rowShift, _columnShift, _cellDisplay);

    // Cell sizes may change so update the selection marquee
    _selectionManager.updateSelection();

    int calculated = _spreadsheet.endRecalc();
    int end = _currentTimeMllis();

    int t = end - start;
    double s = _spreadsheet.calculated * 1000.0 / t;
    print("Recalculated ${calculated}, rendered ${renderedCells} cells in ${t} msec (${s} cells/sec)");

    return calculated;
  }

  // Recalculate the entire spreadsheet
  void recalculateAll() {
    _spreadsheet.markAllDirty();
    recalculate();
  }

  void recalculateViewport() {
    _spreadsheet.markRegionDirty(_rowShift + 1, _columnShift + 1,
        _rowShift + _rows + 1, _columnShift + _columns + 1);
    recalculate();
  }

  // Sorts the selected area by its leftmost column
  void sortSelection(bool ascending) {
    if (_selectionManager.isSelectionEmpty()) {
      return;
    }
    CellRange range = _selectionManager.getSelectionRange();
    range = range.makeBounded();
    RowCol minCorner = range.minCorner;
    RowCol maxCorner = range.maxCorner;

    List<int> order = new List<int>(maxCorner.row - minCorner.row + 1);
    _getSortOrder(minCorner.col, minCorner.row, maxCorner.row, ascending, order);

    // TODO: This code will not update references from elsewhere into the sorted cells.
    // I can see use cases for leaving references going to the pre-sort location, and also cases of
    // references going to the new sorted location.
    Map<CellLocation, CellContent> cells = new Map<CellLocation, CellContent>();
    range.forEach((CellLocation location) {
      Cell cell = location.getCell();
      // Make a copy of the cell - dependencies will be recalculated on insertion
      cells[location] = cell.content;
      _spreadsheet.setCellContent(location.rowCol, null);
    });

    range.forEach((CellLocation location) {
      int col = location.col;
      int row = location.row;
      int newRow = order[row - minCorner.row] + minCorner.row;
      CellContent cell = cells[new CellLocation(_spreadsheet, new RowCol(newRow, col))];
      _spreadsheet.setCellContent(location.rowCol, cell);
    });
  }

  /////////////////////////////////////////////////////////////
  //
  // Private methods - not part of the SpreadsheetPresenter API
  //
  /////////////////////////////////////////////////////////////

  // Insert HTML elements corresponding to a new content column
  void _addTableColumnHtml(int col) {
    int width = _spreadsheet.getColumnWidth(col);
    _table.addColumn(col, width);
    _setTableWidth(_tableWidth + width);
  }

  // Insert HTML elements corresponding to a new content row
  void _addTableRowHtml(int row) {
    _table.addRow(row, _columns);
    _updateInnerMenu();
  }

  /**
   * Moves the cell selection and ensures that new selected cell is visible.
   */
  void _advanceSelectedCell(int deltaRow, int deltaCol) {
    _selectionManager.advanceSelectedCell(deltaRow, deltaCol);
    bool scrolled = false;

    // move viewport down
    int downShift = _rowShift + 1 - _selectionManager.selectedCell.row;
    if (downShift > 0) {
      scrolled = true;
      _scroll(_rowShift - downShift, _columnShift);
      // remove bottom rows
      for (int i = _rows - 1; i > _rows - downShift - 1; i--) {
        _removeTableRowHtml(1 + i);
      }
      // insert top rows, mark them dirty
      for (int i = 0; i < downShift; i++) {
        _addTableRowHtml(1);
      }
      _spreadsheet.markRegionDirty(
          _rowShift + 1,
          _columnShift + 1,
          _rowShift + downShift + 1,
          _columnShift + _columns + 1);
    }

    // move viewport up
    int upShift = _selectionManager.selectedCell.row - (_rowShift + _rows);
    if (upShift > 0) {
      scrolled = true;
      _scroll(_rowShift + upShift, _columnShift);
      // remove top rows
      for (int i = 0; i < upShift; i++) {
        _removeTableRowHtml(1);
      }
      // insert bottom rows, mark them dirty
      for (int i = _rows - upShift; i < _rows; i++) {
        _addTableRowHtml(1 + i);
      }
      _spreadsheet.markRegionDirty(
          _rowShift + _rows - upShift + 1,
          _columnShift + 1,
          _rowShift + _rows + 1,
          _columnShift + _columns + 1);
    }

    // move viewport right
    int rightShift = _columnShift + 1 - _selectionManager.selectedCell.col;
    if (rightShift > 0) {
      scrolled = true;
      _scroll(_rowShift, _columnShift - rightShift);
      // remove right columns
      for (int i = _columns - 1; i > _columns - rightShift - 1; i--) {
        _removeTableColumnHtml(1 + i);
      }
      // insert left column, mark them dirty
      for (int i = 0; i < rightShift; i++) {
        _addTableColumnHtml(1);
      }
      _spreadsheet.markRegionDirty(
          _rowShift + 1,
          _columnShift + 1,
          _rowShift + _rows + 1,
          _columnShift + rightShift + 1);
    }

    // move viewport left
    int leftShift = _selectionManager.selectedCell.col - (_columnShift + _columns);
    if (leftShift > 0) {
      scrolled = true;
      _scroll(_rowShift, _columnShift + leftShift);
      // remove left columns
      for (int i = 0; i < leftShift; i++) {
        _removeTableColumnHtml(1);
      }
      // insert right columns, mark them dirty
      for (int i = _columns - leftShift; i < _columns; i++) {
        _addTableColumnHtml(1 + i);
      }
      _spreadsheet.markRegionDirty(
          _rowShift + _rows + 1,
          _columnShift - leftShift + 1,
          _rowShift + _rows + 1,
          _columnShift + _columns + 1);
    }

    // update dirty cells
    if (scrolled) {
      recalculate();
      _selectionManager.setOrigin(_rowShift, _columnShift);
    } else {
      _selectionManager.updateSelection();
      _redrawHeaders();
    }
  }

  // Create the cell display dropdown.
  SelectElement _createCellDisplay(Document doc, String parentId, List<String> values) {
    Element parent = doc.query('#$parentId');
    SelectElement element = new Element.tag("select");
    StringBuffer sb = new StringBuffer();
    values.forEach((String value) {
      sb.add("<option>");
      sb.add(value);
      sb.add("</option>");
    });
    element.innerHTML = sb.toString();
    element.on.change.add(
      (Event e) {
        _cellDisplay = element.selectedIndex;
        _table.resetTableContents(_rows, _columns);
        recalculateViewport();
      });
    parent.nodes.add(element);
    return element;
  }

  void _createCellDisplaySelector(Document doc) {
    _cellDisplayElement = _createCellDisplay(doc, "debugbar",
        ["Show Values","Show Contents","Show Paste Contents","Show Styles","Show Datatypes",
         "Show Dependencies","Show Forward Dependencies"]);
  }

  void _createFormulaInput(Document doc) {
    _formulaDiv = new Element.tag("div");
    _formulaDiv.id = "formulaDiv-${_spreadsheet.name}";
    _formulaDiv.attributes["class"] = "formulaDiv fadeOut";
    _spreadsheetElement.nodes.add(_formulaDiv);

    _formulaInput = new Element.tag("input");
    _formulaInput.id = "formulaInput-${_spreadsheet.name}";
    _formulaInput.attributes["class"] = "formulaInput";
    _formulaDiv.nodes.add(_formulaInput);

    _formulaInputMeasure = new Element.tag("div");
    _formulaInputMeasure.id = "formulaInputMeasure-${_spreadsheet.name}";
    _formulaInputMeasure.attributes["class"] = "formulaInputMeasure";
    _spreadsheetElement.nodes.add(_formulaInputMeasure);

    _formulaInput.on.click.add((MouseEvent e) {
      _formulaEditing = true;
      _hideFormulaCellSelecting();
    });

    _formulaInput.on.keyDown.add((KeyboardEvent e) {
      // Escape ends edit mode.
      // TODO: KeyName.ESC does not match KeyboardEvent::keyIdentifier when the Escape key is
      // pressed.
      if (e.keyCode == 27 /* esc */) {
        _formulaEditing = false;
        _hideFormula();
      }
      if (_formulaCellSelecting) {
        if (e.keyIdentifier == KeyName.UP ||
            e.keyIdentifier == KeyName.DOWN ||
            e.keyIdentifier == KeyName.LEFT ||
            e.keyIdentifier == KeyName.RIGHT) {
          return;
        }
        if (e.keyCode >= 0x20) {
          _hideFormulaCellSelectingTextSelection();
        }
      }
    });

    _formulaInput.on.keyUp.add((KeyboardEvent e) {
      _growFormulaInput();

      // wait for Enter to stop cell selecting
      if (_formulaCellSelecting) {
        if (e.keyIdentifier == KeyName.ENTER) {
          _hideFormulaCellSelecting();
        }
        return;
      }

      // wait for Enter to stop formula editing
      if (e.keyIdentifier != KeyName.ENTER) {
        return;
      }

      _formulaEditing = false;
      CellLocation location = _selectionManager.selectedCell;
      if (location != null && location.isValidCell()) {
        String value = _formulaInput.value;
        try {
          _spreadsheet.clearDirtyCells();
          _spreadsheet.execute(new SetCellContentsCommand(location, value));
          print("Successfully parsed formula '${value}'");

          // Move focus one cell down
          _selectionManager.advanceSelectedCell(1, 0);
          recalculate();
        } catch (var exception) {
          // Clear the cell contents
          location.markDirty();
          _selectionManager.selectionChanged();
          print("Error parsing formula '${value}': ${exception}");
        }
      }
    });
  }

  void _createMoveDragger(Document doc) {
    _moveDragger = new Element.tag("div");
    _moveDragger.id = "moveDragger-${_spreadsheet.name}";
    _moveDragger.attributes["class"] = "moveDragger";
    _moveDragger.style.setProperty("left", HtmlUtils.toPx(3));
    _moveDragger.style.setProperty("top", HtmlUtils.toPx(3));
    _spreadsheetElement.nodes.add(_moveDragger);

    _moveDragger.on.mouseDown.add((MouseEvent e) {
      _moveToTop();
      _hideInnerMenu(true);

      int mouseStartX = e.x;
      int mouseStartY = e.y;

      _spreadsheetElement.rect.then((ElementRect elementRect) {
        ClientRect rect = elementRect.bounding;
        int startX = rect.left;
        int startY = rect.top;
        _window.document.body.style.setProperty("cursor", "move");

        _setDragFunction((MouseEvent e_) {
          int x = startX + e_.x - mouseStartX;
          int y = startY + e_.y - mouseStartY;

          x = Math.max(x, CssStyles.OBJECTBAR_WIDTH);
          y = Math.max(y, CssStyles.SANDBAR_HEIGHT);
          // Move the spreadsheet container
          _spreadsheetElement.style.setProperty("left", HtmlUtils.toPx(x));
          _spreadsheetElement.style.setProperty("top", HtmlUtils.toPx(y));
        });
        });

      _setUndragFunction((MouseEvent e_) {
        _window.document.body.style.setProperty("cursor", "auto");
      });
    });
  }

  void _createResizeDragger(Document doc) {
    _resizeDragger = new Element.tag("div");
    _resizeDragger.id = "resizeDragger-${_spreadsheet.name}";
    _resizeDragger.attributes["class"] = "resizeDragger";
    _spreadsheetElement.nodes.add(_resizeDragger);

    _resizeDragger.on.mouseDown.add((MouseEvent e) {
      _moveToTop();

      // Hide popups
      _hideFormula();
      _popupHandler.deactivatePopup();

      int mouseStartX = e.x;
      int mouseStartY = e.y;
      int startX = HtmlUtils.fromPx(_resizeDragger.style.getPropertyValue("left"));
      int startY = HtmlUtils.fromPx(_resizeDragger.style.getPropertyValue("top"));
      _window.document.body.style.setProperty("cursor", "move");

      _setDragFunction((MouseEvent e_) {
        int x = startX + e_.x - mouseStartX;
        int y = startY + e_.y - mouseStartY;

        // Move the drag handle
        _resizeDragger.style.setProperty("left", HtmlUtils.toPx(x));
        _resizeDragger.style.setProperty("top", HtmlUtils.toPx(y));

        int column = _getMaxRowOrColumn(x, COL);
        int row = _getMaxRowOrColumn(y, ROW) + 1;
        if (column == -1 || row == -1) {
          return;
        }
        while (column < _columns + _columnShift) {
          if (_columns == 1) {
            break;
          }
          _removeTableColumnHtml(_columns);
          _columns--;
        }
        while (column > _columns + _columnShift) {
          _spreadsheet.insertColumn(_columns, _spreadsheet.getDefaultColumnWidth(_columns));
        }
        while (row < _rows + _rowShift) {
          if (_rows == 1) {
            break;
          }
          _removeTableRowHtml(_rows);
          _rows--;
        }
        while (row > _rows + _rowShift) {
          _spreadsheet.insertRow(_rows, _spreadsheet.getDefaultRowHeight(_rows));
        }
        _spreadsheet.refresh();
      });

      _setUndragFunction((MouseEvent e_) {
        _window.document.body.style.setProperty("cursor", "auto");
      });
    });

    _refreshResizeDragger();
  }

  // Initialize the HTML elements used to indicate dragging
  void _createSpreadsheetLayout(Document doc) {
    DivElement columnDraggerElement = new Element.tag("div");
    columnDraggerElement.id = "columnDragger-${_spreadsheet.name}";
    columnDraggerElement.attributes["class"] = "columnDragger rowColDragger";

    DivElement rowDraggerElement = new Element.tag("div");
    rowDraggerElement.id = "rowDragger-${_spreadsheet.name}";
    rowDraggerElement.attributes["class"] = "rowDragger rowColDragger";

    _spreadsheetElement.nodes.add(columnDraggerElement);
    _spreadsheetElement.nodes.add(rowDraggerElement);

    _dragIndicators = new List<DivElement>(2);
    _dragIndicators[COL] = columnDraggerElement;
    _dragIndicators[ROW] = rowDraggerElement;
  }

  void _createTable(Document doc) {
    // Create the table
    _table = new HtmlTable(_spreadsheet, _spreadsheetElement);

    _setTableWidth(_spreadsheet.getColumnEnd(_columns));
    for (int r = 0; r <= _rows; r++) {
      _addTableRowHtml(r);
    }

    // Create two divs -- the outer one has "overflow:scroll" and has the same width and height
    // as the visible scpreadsheet area.  The inner one has the same width and height as the
    // entire active spreadsheet area.  When the outer div is scrolled, the viewport is shifted
    // to match the scroll coordinates.

    // Inner div
    _tableScrollDiv = new Element.tag("div");
    _tableScrollDiv.id = "tableScrollDiv-${_spreadsheet.name}";
    _tableScrollDiv.attributes["class"] = "tableScrollDiv";
    _tableScrollContainer.style.setProperty("left", "0px");
    _tableScrollContainer.style.setProperty("top", "0px");
    _tableScrollContainer.nodes.add(_tableScrollDiv);
    _tableSizeChanged();

    _tableScrollContainer.on.scroll.add((e) {
      if (_scrolledByKeyboard) {
        _scrolledByKeyboard = false;
        return;
      }
      Future<ElementRect> future = _tableScrollContainer.rect;
      future.then((ElementRect rect) {
        int scrollTop = rect.scroll.top;
        int row = _getAbsRowOrColumn(scrollTop, ROW) - 1;
        int col = _getAbsRowOrColumn(rect.scroll.left, COL) - 1;
        int newRowShift = Math.max(0, row);
        int newColumnShift = Math.max(0, col);
        if (newRowShift != _rowShift || newColumnShift != _columnShift) {
          _rowShift = newRowShift;
          _columnShift = newColumnShift;
          _spreadsheet.refresh();
        }
      });
    });
  }

  // Return the number of milliseconds since the epoch
  int _currentTimeMllis() {
    Date now = new Date.now();
    Date then = new Date.fromEpoch(0, now.timeZone);
    return now.difference(then).inMilliseconds;
  }

  void _formulaCellSelectingInsertReference(RowCol clickLocation, RowCol dragLocation) {
    // set "click" location and reset "drag"
    if (clickLocation != null) {
      if (!clickLocation.isValidCell()) {
        return;
      }
      _formulaCellSelectingClickLocation = clickLocation;
      _formulaCellSelectingDragLocation = clickLocation;
    }
    // update "drag" location
    if (dragLocation != null) {
      if (!dragLocation.isValidCell()) {
        return;
      }
      _formulaCellSelectingDragLocation = dragLocation;
    }

    // prepare selection corners
    RowCol minCorner, maxCorner;
    {
      RowCol click = _formulaCellSelectingClickLocation;
      RowCol drag = _formulaCellSelectingDragLocation;
      int minRow = Math.min(click.row, drag.row);
      int maxRow = Math.max(click.row, drag.row);
      int minCol = Math.min(click.col, drag.col);
      int maxCol = Math.max(click.col, drag.col);
      minCorner = new RowCol(minRow, minCol);
      maxCorner = new RowCol(maxRow, maxCol);
    }

    // update formula text
    {
      // prepare reference text (single or range)
      String ref;
      if (minCorner == maxCorner) {
        ref = minCorner.toA1String();
      } else {
        ref = "${minCorner.toA1String()}:${maxCorner.toA1String()}";
      }
      // replace selection with new cell reference
      String formula = _formulaInput.value;
      formula =
          formula.substring(0, _formulaCellSelectingTextStart) +
          ref +
          formula.substring(_formulaCellSelectingTextEnd, formula.length);
      _formulaInput.value = formula;
      // update selection range
      _formulaCellSelectingTextEnd = _formulaCellSelectingTextStart + ref.length;
    }

    // apply text selection now
    _formulaCellSelectingSelectText();

    // show cell selecting div
    {
      DivElement div = _table.formulaCellSelectingDiv;
      CSSStyleDeclaration divStyle = div.style;
      int borderWidth = 2 + 2;
      CellRange cellRange = new CellRange(_spreadsheet, minCorner, maxCorner);
      _selectionManager.getBoundingBoxForRange(cellRange).then(
          (BoundingBox box) {
        if (box != null) {
          divStyle.setProperty("left", HtmlUtils.toPx(box.left));
          divStyle.setProperty("top", HtmlUtils.toPx(box.top));
          divStyle.setProperty("width", HtmlUtils.toPx(box.width - borderWidth));
          divStyle.setProperty("height", HtmlUtils.toPx(box.height - borderWidth));
          divStyle.removeProperty("display");
        } else {
          divStyle.setProperty("display", "none");
        }
      });
    }
  }

  void _formulaCellSelectingRememberSelectionRange() {
    _formulaCellSelectingTextStart = _formulaInput.selectionStart;
    _formulaCellSelectingTextEnd = _formulaInput.selectionEnd;
  }

  void _formulaCellSelectingSelectText() {
    _formulaInput.focus();
    _formulaInput.setSelectionRange(_formulaCellSelectingTextStart, _formulaCellSelectingTextEnd);
  }



  // Returns the index of the row/column that ends near pixel row/column pos (in absolute
  // coordinates).
  int _getAbsRowOrColumn(int pos, int rowOrCol) {
    // TODO:  use something faster than linear search
    pos += _getRowOrColEnd(rowOrCol, 0);
    int i = 1;
    while (true) {
      int rowOrColPos = _getRowOrColEnd(rowOrCol, i - 1);
      if (rowOrCol == ROW) {
        rowOrColPos += _getInnerMenuOffset(i);
      }
      if (rowOrColPos >= pos) {
        return i;
      }
      i++;
    }
  }

  // Return the location of a given Element within a spreadsheet, or null
  // if the target is not in a spreadsheet.
  CellLocation _getCellLocation(Element target) {
    if (target === _table) {
      return null;
    }
    // Work around a problem where target is a Document but we can't detect it properly
    try {
      while (target.tagName != "TD") {
        Element parent = target.parent;
        // Avoid an infinite loop
        if (target == parent) {
          return null;
        }
        target = parent;
      }
    } catch (var e) {
      return null;
    }

    // Locate the (row, col) index of the selected cell
    TableCellElement cell = target;
    TableRowElement rowElement = cell.parent;
    // If the cell was in row 0 or column 0, it's a header cell.  Return 0 to indicate
    // that a header cell was selected.  Otherwise, add the current row shift or column shift
    // to obtain the real row or column index.
    int row = rowElement.rowIndex == 0 ? 0 : rowElement.rowIndex + _rowShift;
    int col = cell.cellIndex == 0 ? 0 : cell.cellIndex + _columnShift;
    return new CellLocation(_spreadsheet, new RowCol(row, col));
  }

  // return the offset in pixels that the inner menu (if present) adds for
  // rowIndices before rowIndex.
  int _getInnerMenuOffset(int rowIndex) {
    if (_activeInnerMenu != null && rowIndex > _activeInnerMenu.row.rowIndex) {
      return _activeInnerMenu.currentRowHeight;
    } else {
      return 0;
    }
  }

  // Return the largest row or column index whose end is above or to the left of pos
  int _getMaxRowOrColumn(int pos, int rowOrCol) {
    int origin = rowOrCol == COL ? _columnShift : _rowShift;
    int rowsOrCols = rowOrCol == COL ? _columns : _rows;
    pos += rowOrCol == COL ? _spreadsheet.getColumnShift(origin) :
      _spreadsheet.getRowShift(origin);
    // TODO:  use something faster than linear search
    int i = Math.max(origin, 1);
    while (true) {
      int leftRowOrColPos = _getRowOrColEnd(rowOrCol, i - 1);
      int rightRowOrColPos = _getRowOrColEnd(rowOrCol, i);
      if (rowOrCol == ROW) {
        leftRowOrColPos += _getInnerMenuOffset(i - 1);
        rightRowOrColPos += _getInnerMenuOffset(i);
      }
      int midpoint = (leftRowOrColPos + rightRowOrColPos) ~/ 2;
      if (midpoint > pos) {
        break;
      }
      i++;
    }
    return i - 1;
  }

  // Return the pixel position of the end of a given row or column
  int _getRowOrColEnd(int rowOrCol, int index) {
    if (rowOrCol == COL) {
      return _spreadsheet.getColumnEnd(index);
    } else {
      return _spreadsheet.getRowEnd(index);
    }
  }

  // Returns the index of the row/column that ends near pixel row/column pos, or -1
  // The origin is taken into account.  The position is given in mouse coordinates
  int _getRowOrColumn(int pos, int rowOrCol) {
    int origin = rowOrCol == COL ? _columnShift : _rowShift;
    int rowsOrCols = rowOrCol == COL ? _columns : _rows;
    pos += rowOrCol == COL ? _spreadsheet.getColumnShift(origin) :
        _spreadsheet.getRowShift(origin);
    // TODO:  use something faster than linear search
    for (int i = Math.max(origin, 1); i <= origin + rowsOrCols; i++) {
      // Don't allow the row attached to the inner menu to be selected
      if (rowOrCol == ROW && i == _innerMenuRowIndex) {
        continue;
      }

      int rowOrColPos = _getRowOrColEnd(rowOrCol, i);
      if (rowOrCol == ROW) {
        rowOrColPos += _getInnerMenuOffset(i);
      }
      if ((rowOrColPos - pos).abs() <= _DRAGGER_TOLERANCE) {
        return i;
      }
    }
    return -1;
  }

  // Return the end position of the given column, taking a shift into account
  int _getShiftedColumnEnd(int index, int shift) => _spreadsheet.getColumnEnd(index) -
      _spreadsheet.getColumnEnd(shift) + _spreadsheet.getColumnEnd(0);

  // Return the end position of the given row, taking a shift into account
  int _getShiftedRowEnd(int index, int shift) => (_spreadsheet.getRowEnd(index) -
      _spreadsheet.getRowEnd(shift) + _spreadsheet.getRowEnd(0));

  // Calculates the permutation required in order to sort the given portion of a column,
  // returning the results in the 'order' list. 'order' must have size maxRow - minRow + 1
  void _getSortOrder(int col, int minRow, int maxRow, bool ascending, List<int> order) {
    List<IndexedValue> data = new List<IndexedValue>(maxRow - minRow + 1);
    for (int row = minRow; row <= maxRow; row++) {
      Cell cell = _spreadsheet.getCell(new RowCol(row, col));
      if (cell == null) {
        data[row - minRow] = new IndexedValue.blank(row - minRow);
      } else {
        if (cell.getDatatype() == Value.TYPE_STRING) {
          data[row - minRow] = new IndexedValue.string(row - minRow, cell.getStringValue());
        } else {
          data[row - minRow] = new IndexedValue.number(row - minRow, cell.getDoubleValue());
        }
      }
    }
    data.sort((IndexedValue a, IndexedValue b) {
      int cmp = a.compareTo(b);
      return ascending ? cmp : -cmp;
    });

    for (int i = 0; i < data.length; i++) {
      order[i] = data[i].index;
    }
  }

  // Return the number of _rows currently being displayed (not counting the column header row)
  int _getVisibleTableHeight() => _getShiftedRowEnd(_rowShift + _rows, _rowShift);

  // Return the number of _columns currently being displayed (not counting the row header column)
  int _getVisibleTableWidth() => _getShiftedColumnEnd(_columnShift + _columns, _columnShift);

  // Resize the formula input field to fit the contained text
  void _growFormulaInput() {
    _formulaInputMeasure.text = _formulaInput.value;
    _formulaInputMeasure.rect.then((ElementRect rect) {
      int textWidth = rect.client.width;
      int width = Math.max(textWidth + 25, _formulaCellWidth);
      _formulaDiv.style.setProperty("width", HtmlUtils.toPx(width));
      _formulaInput.style.setProperty("width", HtmlUtils.toPx(width));
    });
  }

  // Fade out the formula input field
  void _hideFormula() {
    _hideFormulaCellSelecting();
    _formulaEditing = false;
    _formulaDiv.classes.remove("fadeIn");
    _formulaDiv.classes.add("fadeOut");
  }

  // Stops selecting cell for formula
  void _hideFormulaCellSelecting() {
    if (_formulaCellSelecting) {
      _formulaCellSelecting = false;
      _hideFormulaCellSelectingTextSelection();
      if (_table.formulaCellSelectingDiv != null) {
        _table.formulaCellSelectingDiv.style.setProperty("display", "none");
      }
    }
  }

  // When we stop selecting cell, we want to put cursor directly after cell reference
  void _hideFormulaCellSelectingTextSelection() {
    if (_formulaCellSelectingTextEnd != -1) {
      _formulaInput.setSelectionRange(_formulaCellSelectingTextEnd, _formulaCellSelectingTextEnd);
      _formulaCellSelectingTextStart = _formulaCellSelectingTextEnd = -1;
    }
  }

  // Hide the inner menu.  If 'remove' is false, the menu is appearing at
  // another location at the same time.
  void _hideInnerMenu(bool remove) {
    if (_activeInnerMenu != null) {
      _activeInnerMenu.hide(() {
        if (remove) {
          _innerMenuShown = false;
          _tableSizeChanged();
        }
      });
      _activeInnerMenu = null;
      _innerMenuRowIndex = -1;
    }
  }

  // Return true if the given (row, col) is in the displayed area
  bool _isCellVisible(RowCol rowCol) {
    int row = rowCol.row;
    int col = rowCol.col;
    return row > _rowShift && row <= _rowShift + _rows
        && col > _columnShift && col <= _columnShift + _columns;
  }

  void _moveToTop() {
    // Move the table to the top
    _spreadsheetElement.style.setProperty("z-index", "${_zIndex++}");
  }

  // update table row and column headers
  void _redrawHeaders() {
    _setTableWidth(_getVisibleTableWidth());
    _tableSizeChanged();

    _table.redrawHeaders(_selectionManager, _rows, _columns, _rowShift, _columnShift,
        _cellDisplay);
  }

  void _refreshResizeDragger() {
    _table.rect.then((ElementRect elementRect) {
      // We may be called before the dragger is ready
      if (_resizeDragger == null) {
        return;
      }
      ClientRect rect = elementRect.bounding;

      _resizeDragger.style.setProperty("left", HtmlUtils.toPx(rect.width));
      _resizeDragger.style.setProperty("top", HtmlUtils.toPx(rect.height));
    });
  }

  // Remove the HTML elements corresponding to the given column
  void _removeTableColumnHtml(int col) {
    _table.removeColumn(col);
    int width = _spreadsheet.getColumnWidth(col);
    _setTableWidth(_tableWidth - width);
  }

  // Remove the HTML elements corresponding to the given row
  void _removeTableRowHtml(int row) {
    _table.deleteRow(row);
    _updateInnerMenu();
  }

  // Move the formula input field over the currently selected cell, or hide it if
  // there is no cell selected or the selected cell is not in view
  void _repositionFormulaInput(CellLocation location, bool showIt) {
    // Ensure the formula input is hidden when scrolled out of view.  We hide
    // it immediately, without fading out.
    if (location != null && _isCellVisible(location.rowCol)) {
      _formulaDiv.style.removeProperty("display");
    } else {
      _formulaDiv.style.setProperty("display", "none");
    }

    // Hide the formula bar when dragging
    if (location == null || _dragging) {
      _hideFormula();
    } else {
      int col = location.rowCol.col;
      int left;
      if (col == 0) {
        left = 0;
      } else {
        left = _getShiftedColumnEnd(col - 1, _columnShift);
      }
      int row = location.rowCol.row;
      int top;
      if (row == 0) {
        top = 0;
      } else {
        top = _getShiftedRowEnd(row - 1, _rowShift);
      }
      int width = _spreadsheet.getColumnWidth(location.rowCol.col);
      int height = _spreadsheet.getRowHeight(location.rowCol.row);

      _formulaDiv.style.setProperty("left", HtmlUtils.toPx(left));
      _formulaDiv.style.setProperty("top", HtmlUtils.toPx(top));
      _formulaDiv.style.setProperty("width", HtmlUtils.toPx(width));
      _formulaDiv.style.setProperty("height", HtmlUtils.toPx(height));
      _formulaInput.style.setProperty("left", HtmlUtils.toPx(left));
      _formulaInput.style.setProperty("top", HtmlUtils.toPx(top));
      _formulaInput.style.setProperty("width", HtmlUtils.toPx(width - 1));
      _formulaInput.style.setProperty("height", HtmlUtils.toPx(height - 1));
      _formulaCellWidth = width - 1;
      if (showIt) {
        _showFormula();
      }
    }
  }

  // Reset the formula bar to an empty string
  void _resetFormulaInput() {
    _formulaInput.value = "";
  }

  void _rowColSizeChanged() {
    _redrawHeaders();
    // Move the table handles
    _refreshResizeDragger();
    // Update the selection marquee
    _selectionManager.updateSelection();
    // update the inner menu
    _updateInnerMenu();
  }

  void _scheduleShowInnerMenu(Style style) {
    int requestId = ++_innerMenuShowRequestId;
    _window.setTimeout(() {
      if (requestId == _innerMenuShowRequestId) {
        _showInnerMenu(style);
      }
    }, 300);
  }

  // Update the scrollbars to match the scroll position
  void _scroll(int row, int column) {
    _rowShift = row;
    _scrolledByKeyboard = true;
    int top = _spreadsheet.getRowEnd(row) - _spreadsheet.getRowEnd(0);
    _columnShift = column;
    _scrolledByKeyboard = true;
    int left = _spreadsheet.getColumnEnd(column) - _spreadsheet.getColumnEnd(0);
    _tableScrollContainer.$dom_scrollTop = top;
    _tableScrollContainer.$dom_scrollLeft = left;
  }

  // Set a function to be called for each mousemove and mouseup event on the document.
  // The function will be cleared following the mouseup event.  This should be called from
  // a mousedown handler that wishes to initiate dragging behavior.
  void _setDragFunction(EventListener drag) {
    _drag = drag;
    _dragging = drag == null ? false : true;
  }

  // Set a function to be called for each mousemove when dragging is not taking place.
  void _setMove(EventListener move) {
    _move = move;
  }

  void _setTableWidth(int width) {
    _tableWidth = width;
    _table.setWidth(width);
    _refreshResizeDragger();
  }

  // Set a function to be called on the next mouseup event on the document. The function will be
  // cleared following the mouseup event.  This should be called from a mousedown handler that
  // wishes to initiate dragging behavior.
  void _setUndragFunction(EventListener undrag) {
    _undrag = undrag;
  }

  void _setupTableMouseListeners() {
    bool draggingSelection = false;
    List<int> hoverRowColumn = new List<int>(2);
    hoverRowColumn[COL] = -1;
    hoverRowColumn[ROW] = -1;

    List<int> draggingRowColumn = new List<int>(2);
    draggingRowColumn[COL] = -1;
    draggingRowColumn[ROW] = -1;

    int oldSize;
    int start;
    int x;
    int y;

    // Sets the drag indicator to the given position, given as an absolute offset from
    // the upper-left corner of the spreadsheet
    void setDragPosition(int rowOrCol, int pos) {
      if (rowOrCol == COL) {
        pos -= _spreadsheet.getColumnShift(_columnShift);
        _dragIndicators[COL].style.setProperty("left", HtmlUtils.toPx(pos));
      } else {
        pos -= _spreadsheet.getRowShift(_rowShift);
        // need to shift for the inner menu offset
        pos += _getInnerMenuOffset(hoverRowColumn[ROW]);
        _dragIndicators[ROW].style.setProperty("top", HtmlUtils.toPx(pos));
      }
    }

    int getXOrY(int rowOrCol) => rowOrCol == COL ? x : y;

    String getWidthOrHeight(int rowOrCol) => rowOrCol == COL ? "width" : "height";

    // Trim the drag indicator and display it at the hover row/column
    void setDrag(int rowOrCol, bool visible) {
      if (visible) {
        setDragPosition(rowOrCol, _getRowOrColEnd(rowOrCol, hoverRowColumn[rowOrCol]));
        int other = 1 - rowOrCol;
        int maxVisible = rowOrCol == COL ? _rows : _columns;
	int size = _getRowOrColEnd(other, maxVisible);
        if (rowOrCol == COL && _activeInnerMenu != null) {
          size += _activeInnerMenu.currentRowHeight;
        }
        _dragIndicators[rowOrCol].style.setProperty(getWidthOrHeight(other),
            HtmlUtils.toPx(size));
      }
      _dragIndicators[rowOrCol].style.setProperty("display", visible ? "block" : "none");
    }

    // Return true if we are in a row/column dragging state
    bool dragRowColumn(bool mouseUp) {
      bool inDragState = false;
      if (draggingRowColumn[COL] != -1) {
        int opos = _spreadsheet.getColumnShift(_columnShift);
        int size = Math.max(getXOrY(COL) - start + opos, CssStyles.MIN_COLUMN_WIDTH);
        setDragPosition(COL, start + size);
        _updateRowColumnSize(COL, draggingRowColumn[COL], size, oldSize, mouseUp);
        inDragState = true;
      } else if (draggingRowColumn[ROW] != -1) {
        int opos = _spreadsheet.getRowShift(_rowShift);
        // account for inner menu height
        opos -= _getInnerMenuOffset(draggingRowColumn[ROW]);
        int size = Math.max(getXOrY(ROW) - start + opos, CssStyles.MIN_ROW_HEIGHT);
        setDragPosition(ROW, start + size);
        _updateRowColumnSize(ROW, draggingRowColumn[ROW], size, oldSize, mouseUp);
        inDragState = true;
      }
      return inDragState;
    }

    _window.document.on.keyDown.add((KeyboardEvent e) {
      if (_selectionManager.isSelectionEmpty()) {
        return;
      }

      // Prevent arrow keys from controlling scrolling.
      if (e.keyIdentifier == KeyName.UP ||
          e.keyIdentifier == KeyName.DOWN ||
          e.keyIdentifier == KeyName.LEFT ||
          e.keyIdentifier == KeyName.RIGHT) {
        e.preventDefault();
      }

      // Navigation inside of formula editor, or cell selection
      if (_formulaEditing) {
        if (_formulaCellSelecting) {
          int deltaRow = 0;
          int deltaCol = 0;
          if (e.keyIdentifier == KeyName.UP) {
            deltaRow = -1;
          }
          if (e.keyIdentifier == KeyName.DOWN) {
            deltaRow = 1;
          }
          if (e.keyIdentifier == KeyName.LEFT) {
            deltaCol = -1;
          }
          if (e.keyIdentifier == KeyName.RIGHT) {
            deltaCol = 1;
          }
          if (deltaRow != 0 || deltaCol != 0) {
            _formulaCellSelectingRememberSelectionRange();
            RowCol newLocation = _formulaCellSelectingClickLocation.translate(deltaRow, deltaCol);
            _formulaCellSelectingInsertReference(newLocation, null);
            return;
          }
        }
        return;
      }

      // TODO: text modifying keys should trigger edit mode if the selection is a single cell.

      // TODO: The selection rectangle can scroll out of view with
      // use of the arrow keys. We need intelligent use of scrollIntoView
      // so that doesn't happen.

      if (e.keyIdentifier == KeyName.UP) {
        _advanceSelectedCell(-1, 0);
        return;
      }
      if (e.keyIdentifier == KeyName.DOWN) {
        _advanceSelectedCell(1, 0);
        return;
      }
      if (e.keyIdentifier == KeyName.LEFT) {
        _advanceSelectedCell(0, -1);
        return;
      }
      if (e.keyIdentifier == KeyName.RIGHT) {
        _advanceSelectedCell(0, 1);
        return;
      }
      if (e.keyIdentifier == KeyName.END) {
        // Prevent arrow keys from controlling scrolling.
        e.preventDefault();
        _selectionManager.setSelectedCell(_spreadsheet.rowCount(), _spreadsheet.columnCount());
        _scroll(Math.max(0, _spreadsheet.rowCount() - _rows),
            Math.max(0, _spreadsheet.columnCount() - _columns));
        _spreadsheet.refresh();
        return;
      }
      if (e.keyIdentifier == KeyName.HOME) {
        // Prevent arrow keys from controlling scrolling.
        e.preventDefault();
        _selectionManager.setSelectedCell(1, 1);
        _scroll(0, 0);
        _spreadsheet.refresh();
        return;
      }
    }, false);

    // TODO: We need a way to clear the selection when the user clicks outside of the table.
    _window.document.on.click.add((MouseEvent e) {
      // Ignore right-click and ctrl-click events
      if (e.button == 2 || (e.button == 0 && e.ctrlKey)) {
        return;
      }
      Element target = e.target;
      // We should probably pass the spreadsheet around a bit more, there are a
      // number of places where we have to walk up to _table's parent.

      // TODO: 'contains' is not doing what it should
      bool c1 = _spreadsheetElement.contains(target);
      bool c2 = target.contains(_spreadsheetElement);
      bool contains = c1 != c2;
      if (!contains && !_selectionManager.isSelectionEmpty()) {
        _selectionManager.clearSelection();
        // need to update the selected header styles
        _redrawHeaders();
      }
    }, true);

    _table.on.mouseOut.add((MouseEvent e) {
      if (!_dragging) {
        setDrag(ROW, false);
        setDrag(COL, false);
      }
    });

    // The inline cell editing UI is displayed when a user makes the same single
    // cell selection over again. We keep track of the current single cell that
    // is selected here to know when to enter edit mode.
    CellLocation currentSelectedSingleCell = null;

    void mouseMove(MouseEvent e) {
      _table.rect.then((ElementRect rect) {
        // Set x and y to the mouse coordinates, relative to the top left of
        // the spreadsheet table.
        ClientRect boundingRect = rect.bounding;
        int scrollOffsetX = -boundingRect.left.toInt();
        int scrollOffsetY = -boundingRect.top.toInt();
        x = e.x + scrollOffsetX;
        y = e.y + scrollOffsetY;

        // Update the dragger position and optionally the actual row/column size
        if (dragRowColumn(false)) {
          return;
        }

        // Show a row/column dragging indicator when hovering over row/column 0
        if (x < _spreadsheet.getColumnWidth(0) && x > 0) {
          hoverRowColumn[ROW] = _getRowOrColumn(y, ROW);
        } else {
          hoverRowColumn[ROW] = -1;
        }
        if (y < _spreadsheet.getRowHeight(0) && y > 0) {
          hoverRowColumn[COL] = _getRowOrColumn(x, COL);
        } else {
          hoverRowColumn[COL] = -1;
        }
        if (hoverRowColumn[COL] != -1) {
          hoverRowColumn[ROW] = -1;
          setDrag(COL, true);
          setDrag(ROW, false);
        } else {
          setDrag(COL, false);
          if (hoverRowColumn[ROW] != -1) {
            setDrag(ROW, true);
          } else {
            setDrag(ROW, false);
          }
        }
        if (hoverRowColumn[COL] != -1) {
          _table.setColResizeCursor();
        } else if (hoverRowColumn[ROW] != -1) {
          _table.setRowResizeCursor();
        } else {
          _table.setCellCursor();
        }

        // If dragging a selections, update the selection's second corner
        if (!draggingSelection) {
          return;
        }
        Element target = e.target;
        CellLocation location = _getCellLocation(target);
        if (location == null) {
          return;
        }
        _selectionManager.selectionCorner = location;
        // If the drag has selected more than one cell then we no longer
        // have a current single cell selection.
        if (!_selectionManager.isSingleCellSelection()) {
          currentSelectedSingleCell = null;
        }
        _selectionManager.updateSelection();
        _redrawHeaders();
      });
    }

    _setMove(mouseMove);

    _table.on.mouseDown.add((MouseEvent e) {
      _moveToTop();

      // Right click toggles and positions the context menu
      if (e.button == 2 || (e.button == 0 && e.ctrlKey)) {
        _table.rect.then((ElementRect rect) {
          ClientRect boundingRect = rect.bounding;
          int scrollOffsetX = -boundingRect.left;
          int scrollOffsetY = -boundingRect.top;
          _contextMenu.show(e.x + scrollOffsetX, e.y + scrollOffsetY);
        });
        return;
      }

      _setDragFunction(mouseMove);

      _setUndragFunction((MouseEvent e_) {
        dragRowColumn(true);
        _table.setDefaultCursor();
        draggingSelection = false;
        draggingRowColumn[COL] = -1;
        setDrag(COL, false);
        draggingRowColumn[ROW] = -1;
        setDrag(ROW, false);
        // Finalize the selection
        _selectionManager.selectionChanged();
        // Check to see if the selection we just finalized is a request
        // for edit mode.
        CellLocation selectedCell = _selectionManager.selectedCell;
        if (_selectionManager.isSingleCellSelection()
            && currentSelectedSingleCell == selectedCell) {
          currentSelectedSingleCell = null;
          _repositionFormulaInput(selectedCell, true);
          _formulaInput.focus();
        }
      });

      // If a hover row/column is highlighted, convert it to a drag row/column
      for (int rowOrCol = COL; rowOrCol <= ROW; rowOrCol++) {
        draggingRowColumn[rowOrCol] = hoverRowColumn[rowOrCol];
        if (draggingRowColumn[rowOrCol] != -1) {
          start = draggingRowColumn[rowOrCol] == 0 ?
              0 : _getRowOrColEnd(rowOrCol, draggingRowColumn[rowOrCol] - 1);
          int size = (rowOrCol == COL ?
              _spreadsheet.getColumnWidth(draggingRowColumn[rowOrCol]) :
              _spreadsheet.getRowHeight(draggingRowColumn[rowOrCol]));
          oldSize = size;
          setDragPosition(rowOrCol, start + size);
          return;
        }
      }

      // Determine if we moused in a cell
      Element target = e.target;
      CellLocation location = _getCellLocation(target);
      if (location == null) {
        _selectionManager.clearSelection();
        return;
      }

      // Click on cell during formula editing
      if (_formulaEditing) {
        _formulaCellSelecting = true;
        _formulaCellSelectingRememberSelectionRange();
        // disable text selection, because browser inverts selection once we drag over input
        _formulaInput.style.setProperty("-webkit-user-select", "none");
        // listen for drag operation
        _setDragFunction((MouseEvent e_) {
          Element target_ = e_.target;
          CellLocation location_ = _getCellLocation(target_);
          if (location_ == null) {
            return;
          }
          _formulaCellSelectingInsertReference(null, location_.rowCol);
        });
        _setUndragFunction((MouseEvent e_) {
          _formulaInput.style.removeProperty("-webkit-user-select");
          _formulaCellSelectingSelectText();
        });
        // set "click" location
        _formulaCellSelectingInsertReference(location.rowCol, null);
        return;
      }

      // Begin selection
      draggingSelection = true;
      if (e.shiftKey) {
        _selectionManager.selectionCorner = location;
      } else {
        if (_selectionManager.isSingleCellSelection()
            && location == _selectionManager.selectedCell) {
          currentSelectedSingleCell = _selectionManager.selectedCell;
        }
        _selectionManager.clearSelection();
        _selectionManager.selectedCell = location;
        _selectionManager.selectionCorner = location;
      }
      _selectionManager.updateSelection();
      // Update the highlighted cells for the initial selection
      _selectionManager.selectionChanged();
    });
  }

  // Resize the formula input field to fit the contained text and fade it in
  void _showFormula() {
    _formulaEditing = true;
    _formulaCellSelecting = false;
    _growFormulaInput();
    _formulaDiv.classes.remove("fadeOut");
    _formulaDiv.classes.add("fadeIn");
  }

  void _showInnerMenu(Style style) {
    if (_selectionManager.selectedCell === null || _selectionManager.selectionCorner === null) {
      // Debugging - log when this happens
      print("_showInnerMenu called with cell=${_selectionManager.selectedCell} "
          + "and corner=${_selectionManager.selectionCorner}");
      return;
    }

    // Place the menu below the selection rectangle
    int rowIndex = Math.max(_selectionManager.selectedCell.row,
        _selectionManager.selectionCorner.row);
    if (rowIndex > 0) {
      rowIndex -= _rowShift;
    }

    // Hide the menu if the selection has scrolled out of view
    if (rowIndex < 0 || rowIndex > _rows) {
      _hideInnerMenu(true);
      return;
    }

    TableRowElement row = _table.getRowElement(rowIndex);
    if (_activeInnerMenu != null) {
      _activeInnerMenu.updateStyleUI(style);
      if (_activeInnerMenu.isAttachedTo(row)) {
        return;
      }
      // Hiding is temporary, so don't change _innerMenuShown
      _hideInnerMenu(false);
    }
    _activeInnerMenu = new InnerMenuView(_window, row, _selectionManager, style,
        _spreadsheet.layout.getRowHeight(rowIndex),
        (){ _tableSizeChanged(); },
        (){ _hideInnerMenu(true); _repositionFormulaInput(null, false); });
    _innerMenuRowIndex = rowIndex;
  }

  // Update the scroll mechanism due to a change in the visible table area
  void _tableSizeChanged() {
    _table.rect.then((ElementRect elementRect) {
      ClientRect rect = elementRect.bounding;

      _tableScrollContainer.style.width = HtmlUtils.toPx(rect.width + 10);
      _spreadsheetElement.style.width = HtmlUtils.toPx(rect.width);

      _tableScrollContainer.style.height = HtmlUtils.toPx(rect.height + 10);
      _spreadsheetElement.style.height = HtmlUtils.toPx(rect.height);

      _tableScrollDiv.style.width = HtmlUtils.toPx(
          _spreadsheet.getColumnEnd(_spreadsheet.columnCount()));
      int extra = _activeInnerMenu == null ?
          0 : _activeInnerMenu.currentRowHeight;
      _tableScrollDiv.style.height = HtmlUtils.toPx(_spreadsheet.getRowEnd(
          _spreadsheet.rowCount()) + extra);

      // Reposition the scroll bars
      _scroll(_rowShift, _columnShift);
      // Move the resize dragger to the bottom-right corner
      _refreshResizeDragger();
    });
  }

  void _updateInnerMenu() {
    if (_activeInnerMenu != null) {
      _activeInnerMenu.updateSize();
    }
  }

  // Change the width/height of a column or row.
  void _updateRowColumnSize(int rowOrCol, int index, int size, int oldSize, bool mouseUp) {
    if (mouseUp) {
      Command command = new ResizeRowColumnCommand(_spreadsheet, rowOrCol, index, size, oldSize);
      _spreadsheet.execute(command);
    } else {
      if (rowOrCol == COL) {
        _spreadsheet.setColumnWidth(index, size);
      } else {
        _spreadsheet.setRowHeight(index, size);
      }
    }
  }
}

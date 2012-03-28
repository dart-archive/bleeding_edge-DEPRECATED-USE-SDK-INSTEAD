// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A class encapsulating the managemnet of the Html [:<table>:] representing the spreadsheet grid.
 */
class HtmlTable {

  DivElement _formulaCellSelectingDiv;
  Spreadsheet _spreadsheet;
  TableElement _table;

  // Must be called after the creation of the <tbody> element
  DivElement get formulaCellSelectingDiv() {
    if (_formulaCellSelectingDiv == null) {
      _formulaCellSelectingDiv = new Element.tag("div");
      _formulaCellSelectingDiv.id = "formulaSelectingCell-${_spreadsheet.name}";
      _formulaCellSelectingDiv.attributes["class"] = "formulaSelectingCell";
    }
    _table.nodes.add(_formulaCellSelectingDiv);
    return _formulaCellSelectingDiv;
  }

  // FIXME
  ElementEvents get on() => _table.on;

  HtmlTable(this._spreadsheet, Element parent) {
    Document doc = parent.document;

    _table = new Element.tag("table");
    _table.attributes["class"] = "spreadsheet";
    _table.style.setProperty("position", "absolute");
    _table.style.setProperty("z-index", "2");
    _table.style.setProperty("left", "0");
    _table.style.setProperty("top", "0");

    parent.nodes.add(_table);
  }

  void addColumn(int col, int columnWidth) {
    Document doc = _table.document;
    bool first = true;
    for (TableRowElement row in _table.rows) {
      TableCellElement cell;
      if (first) {
        first = false;
        cell = new Element.tag("td");
        cell.width = HtmlUtils.toPx(columnWidth);
        row.insertBefore(cell, row.cells[col]);
      } else {
        cell = row.insertCell(col);
      }
    }
  }

  void addRow(int row, int columns) {
    TableRowElement rowElement = _table.insertRow(row);
    rowElement.style.setProperty("height", HtmlUtils.toPx(CssStyles.DEFAULT_ROW_HEIGHT));
    for (int col = 0; col <= columns; col++) {
      TableCellElement cell = rowElement.insertCell(col);
      // If the row being added is the first row, set the column widths
      if (row == 0) {
        cell.width = HtmlUtils.toPx(_spreadsheet.getColumnWidth(col));
      }
    }
  }

  // FIXME
  void appendChild(Node child) {
    _table.nodes.add(child);
  }

  void deleteRow(int index) {
    _table.deleteRow(index);
  }

  Future<ElementRect> get rect() => _table.rect;

  // FIXME?
  TableRowElement getRowElement(int row) => _table.rows[row];

  // Add or remove a class name from a table cell.
  void modifyClasses(Set<CellLocation> locations, String className, bool add,
      int rows, int columns, int rowShift, int columnShift) {
    List<Node> tableRows = _table.rows;
    locations.forEach((CellLocation loc) {
      if (!_inView(loc.rowCol, rows, columns, rowShift, columnShift)) {
        return;
      }
      TableRowElement rowElement = tableRows[loc.row - rowShift];
      TableCellElement cellElement = rowElement.cells[loc.col - columnShift];
      if (add) {
        cellElement.classes.add(className);
      } else {
        cellElement.classes.remove(className);
      }
    });
  }

  int numRows() => _table.rows.length;

  int redraw(SelectionManager selectionManager,
      int rows, int columns, int rowShift, int columnShift, int cellDisplay) {
    List<Node> tableRows = _table.rows;
    TableRowElement tableHeaderRow = tableRows[0];
    List<Node> tableHeaderCells = tableHeaderRow.cells;
    TableRowElement rowElement;
    TableCellElement cellElement;
    DOMTokenList cellClasses;

    redrawHeaders(selectionManager, rows, columns, rowShift, columnShift, cellDisplay);

    int rendered = 0;

    // Render dirty cells
    _spreadsheet.forEachDirtyCell((RowCol rowCol) {
      if (!_inView(rowCol, rows, columns, rowShift, columnShift)) {
        return;
      }
      rowElement = tableRows[rowCol.row - rowShift];
      cellElement = rowElement.cells[rowCol.col - columnShift];
      Cell cell = _spreadsheet.getCell(rowCol);
      if (cell != null && cellDisplay == SpreadsheetPresenter.CELL_DISPLAY_VALUES) {
        Style style = cell.style;
        CSSStyleDeclaration cssStyle = cellElement.style;
        setCellStyle(style, cssStyle);
      } else {
        cellElement.attributes.remove("style");
      }

      if (cell != null) {
        // Force recalculation
        String html = cell.toHtml();
        switch (cellDisplay) {
        case SpreadsheetPresenter.CELL_DISPLAY_VALUES:
          break;
        case SpreadsheetPresenter.CELL_DISPLAY_CONTENTS:
          if (cell.isEmpty()) {
            html = "<i>empty</i>";
          } else if (cell.isFormula()) {
            html = "${HtmlUtils.quoteHtml(cell.getContentString())}";
          } else {
            html = "&quot;${HtmlUtils.quoteHtml(cell.getContentString())}&quot;";
          }
          break;
        case SpreadsheetPresenter.CELL_DISPLAY_PASTE_CONTENTS:
          if (cell.isEmpty()) {
            html = "<i>empty</i>";
          } else {
            html = HtmlUtils.quoteHtml(cell.getPasteContent());
          }
          break;
        case SpreadsheetPresenter.CELL_DISPLAY_STYLES:
          html = cell.getStyleAsHtml();
          break;
        case SpreadsheetPresenter.CELL_DISPLAY_DATATYPE:
          html = HtmlUtils.quoteHtml(cell.getDatatypeAsString());
          break;
        case SpreadsheetPresenter.CELL_DISPLAY_DEPS:
          StringBuffer sb = new StringBuffer();
          Set<CellLocation> deps = cell.getDependencies();
          if (deps == null) {
            html = "--";
          } else {
            bool first = true;
            deps.forEach((CellLocation cellLocation) {
              if (!first) {
                sb.add(",");
              }
              first = false;
              sb.add(cellLocation.rowCol.toA1String());
            });
            html = HtmlUtils.quoteHtml(sb.toString());
          }
          break;
        case SpreadsheetPresenter.CELL_DISPLAY_FORWARD_DEPS:
          StringBuffer sb = new StringBuffer();
          Set<CellLocation> deps = cell.dependents;
          if (deps == null) {
            html = "--";
          } else {
            bool first = true;
            deps.forEach((CellLocation cellLocation) {
              if (!first) {
                sb.add(",");
              }
              first = false;
              sb.add(cellLocation.rowCol.toA1String());
            });
            html = HtmlUtils.quoteHtml(sb.toString());
          }
          break;
        }

        cellElement.innerHTML = html;
        ++rendered;

        cell.clearStyleDirty();
      } else {
        cellElement.innerHTML = "";
      }
    });

    // Set row/column styles for empty cells
    RowColStyle sheetStyle = _spreadsheet.getSheetStyle();
    for (int r = 1; r <= rows; r++) {
      int row = r + rowShift;
      rowElement = tableRows[r];
      RowColStyle rowStyle = _spreadsheet.getRowStyle(row);
      for (int c = 1; c <= columns; c++) {
        int col = c + columnShift;
        Cell cell = _spreadsheet.getCell(new RowCol(row, col));
        // Non-null cells have already been rendered above
        if (cell != null) {
          continue;
        }
        RowColStyle columnStyle = _spreadsheet.getColumnStyle(col);
        Style style = RowColStyle.merge3(sheetStyle, rowStyle, columnStyle);
        cellElement = rowElement.cells[c];
        CSSStyleDeclaration cssStyle = cellElement.style;
        if (cellDisplay == SpreadsheetPresenter.CELL_DISPLAY_VALUES) {
          if (style != null) {
            setCellStyle(style, cssStyle);
          } else {
            cssStyle.removeProperty("background-color");
          }
        } else if (cellDisplay == SpreadsheetPresenter.CELL_DISPLAY_STYLES) {
          if (style != null) {
            cellElement.innerHTML = style.toHtml();
          } else {
            cellElement.innerHTML = "";
          }
        }
      }
    }

    return rendered;
  }

  void redrawHeaders(SelectionManager selectionManager,
      int rows, int columns, int rowShift, int columnShift, int cellDisplay) {
    List<Node> tableRows = _table.rows;
    TableRowElement tableHeaderRow = tableRows[0];
    List<Node> tableHeaderCells = tableHeaderRow.cells;
    TableCellElement cellElement;
    Set<String> cellClasses;

    // Redraw first column header
    TableCellElement firstColumnCell = tableHeaderCells[0];
    firstColumnCell.classes.add("cornerHeader");
    firstColumnCell.width = HtmlUtils.toPx(_spreadsheet.getColumnWidth(0));

    RowColStyle sheetStyle = _spreadsheet.getSheetStyle();

    bool displayStyles = cellDisplay == SpreadsheetPresenter.CELL_DISPLAY_STYLES;

    // Redraw column headers
    for (int c = 0; c <= columns; c++) {
      // Column headers - physical row 0 contains the column numbers
      cellElement = tableHeaderCells[c];
      cellClasses = cellElement.classes;
      cellElement.width = HtmlUtils.toPx(_spreadsheet.getColumnWidth(c == 0 ? 0 : c + columnShift));
      if (displayStyles) {
        cellClasses.remove("columnHeader");
        cellClasses.remove("columnHeader-selected");
      } else {
        if (selectionManager.isColumnSelected(c + columnShift)) {
          cellClasses.remove("columnHeader");
          cellClasses.add("columnHeader-selected");
        } else {
          cellClasses.remove("columnHeader-selected");
          cellClasses.add("columnHeader");
        }
      }

      int col = c + columnShift;
      String html = c == 0 ? HtmlUtils.quoteHtml(_spreadsheet.name) : StringUtils.columnString(col);
      if (displayStyles) {
        RowColStyle s;
        if (c == 0) {
          s = _spreadsheet.getSheetStyle();
          if (s != null) {
            html += s.style.toHtml();
          }
        } else {
          s = _spreadsheet.getColumnStyle(col);
          if (s != null) {
            html = "${html}: ${s.style.toHtml()}";
          }
        }
      }
      cellElement.innerHTML = html;
    }

    // Redraw first row header
    TableRowElement firstTableRow = tableRows[0];
    firstTableRow.style.setProperty("height", HtmlUtils.toPx(_spreadsheet.getRowHeight(0)));

    // Redraw row headers
    for (int r = 1; r <= rows; r++) {
      TableRowElement tableRow = tableRows[r];
      tableRow.style.setProperty("height",
          HtmlUtils.toPx(_spreadsheet.getRowHeight(r + rowShift)));
      List<Node> rowCells = tableRow.cells;
      // Row headers - physical column 0 contains the row numbers
      cellElement = rowCells[0];
      cellClasses = cellElement.classes;
      if (displayStyles) {
        cellClasses.remove("rowHeader");
        cellClasses.remove("rowHeader-selected");
      } else {
        if (selectionManager.isRowSelected(r + rowShift)) {
          cellClasses.remove("rowHeader");
          cellClasses.add("rowHeader-selected");
        } else {
          cellClasses.remove("rowHeader-selected");
          cellClasses.add("rowHeader");
        }
      }

      int row = r + rowShift;
      String html = StringUtils.rowString(row);
      if (displayStyles) {
        RowColStyle rowStyle = _spreadsheet.getRowStyle(row);
        if (rowStyle != null) {
          html = "${html}: ${rowStyle.style.toHtml()}";
        }
      }
      cellElement.innerHTML = html;
    }
  }

  void removeColumn(int col) {
    for (TableRowElement row in _table.rows) {
      row.deleteCell(col);
    }
  }

  // Remove innerHTML and class names from the entire visible portion of the table
  void resetTableContents(int rows, int columns) {
    List<Node> tableRows = _table.rows;
    for (int r = 0; r <= rows; r++) {
      TableRowElement tableRow = tableRows[r];
      List<Node> tableRowCells = tableRow.cells;
      int oldLen = tableRowCells.length;
      for (int c = 0; c <= columns; c++) {
        TableCellElement cellElement = tableRowCells[c];
        cellElement.attributes.remove("class");
        // TODO:  can the style attribute be removed entirely?
        cellElement.style.cssText = "";
        cellElement.innerHTML = "";
      }
      // Delete extra cells
      for (int c = oldLen - 1; c > columns; c--) {
        tableRow.deleteCell(c);
      }
    }
  }

  void setCellContents(int row, int col, String value) {
    TableRowElement rowElement = _table.rows[row];
    TableCellElement cellElement = rowElement.cells[col];
    cellElement.innerHTML = HtmlUtils.quoteHtml(value);
  }

  void setCellCursor() {
    _table.style.setProperty("cursor", "cell");
  }

  // Renders aspects of a cell's style that require styling the containing <TD> element,
  // namely text alignment, text color, and background color.
  void setCellStyle(Style style, CSSStyleDeclaration cssStyle) {
    String textAlignment = style.getTextAlignmentString();
    cssStyle.setProperty("text-align", textAlignment);

    int textColor = style.textColor;
    if (textColor != Style.BLACK && textColor != Style.UNSET) {
      String textColorString = Formats.getHtmlColor(textColor);
      cssStyle.setProperty("color", textColorString);
    } else {
      cssStyle.removeProperty("color");
    }

    int backgroundColor = style.backgroundColor;
    if (backgroundColor != Style.UNSET && backgroundColor != Style.WHITE) {
      String backgroundColorString = Formats.getHtmlColor(backgroundColor);
      cssStyle.setProperty("background-color", backgroundColorString);
    } else {
      cssStyle.removeProperty("background-color");
    }
  }

  void setColResizeCursor() {
    _table.style.setProperty("cursor", "col-resize");
  }

  void setDefaultCursor() {
    _table.style.removeProperty("cursor");
  }

  void setRowResizeCursor() {
    _table.style.setProperty("cursor", "row-resize");
  }

  void setWidth(int width) {
    _table.style.setProperty("width", HtmlUtils.toPx(width));
  }

  // Return true if the given (row, col) is within the viewport of this presenter.
  bool _inView(RowCol rowCol, int rows, int columns, int rowShift, int columnShift) {
    int row = rowCol.row - rowShift;
    if (row <= 0 || row > rows) {
      return false;
    }
    int col = rowCol.col - columnShift;
    if (col <= 0 || col > columns) {
      return false;
    }
    return true;
  }
}

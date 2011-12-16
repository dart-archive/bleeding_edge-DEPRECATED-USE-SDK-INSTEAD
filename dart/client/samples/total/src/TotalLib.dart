// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("TotalLib");
#import("dart:html");

#source("Cell.dart");
#source("CellContents.dart");
#source("CellLocation.dart");
#source("CellRange.dart");
#source("Chart.dart");
#source("ClientChart.dart");
#source("ColorPicker.dart");
#source("Command.dart");
#source("ContextMenu.dart");
#source("ContextMenuBuilder.dart");
#source("CopyPasteManager.dart");
#source("CssStyles.dart");
#source("CSVReader.dart");
#source("DateTimeUtils.dart");
#source("UndoableAction.dart");
#source("DomUtils.dart");
#source("Exceptions.dart");
#source("Formats.dart");
#source("Formula.dart");
#source("Functions.dart");
#source("GeneralCommand.dart");
#source("HtmlTable.dart");
#source("HtmlUtils.dart");
#source("IdGenerator.dart");
#source("IndexedValue.dart");
#source("InnerMenuView.dart");
#source("Parser.dart");
#source("Picker.dart");
#source("PopupHandler.dart");
#source("Reader.dart");
#source("RowCol.dart");
#source("RowColStyle.dart");
#source("Scanner.dart");
#source("SelectionManager.dart");
#source("ServerChart.dart");
#source("Spreadsheet.dart");
#source("SpreadsheetLayout.dart");
#source("SpreadsheetListener.dart");
#source("SpreadsheetPresenter.dart");
#source("StringUtils.dart");
#source("Style.dart");
#source("SYLKReader.dart");
#source("UndoStack.dart");
#source("Value.dart");
#source("ValuePicker.dart");
#source("ZoomTracker.dart");

class Total {
  static final int DEFAULT_VISIBLE_COLUMNS = 10;
  static final int DEFAULT_VISIBLE_ROWS = 25;

  Spreadsheet _spreadsheet;
  SpreadsheetPresenter _presenter;

  Total() {
    Element recalcButton = document.query("#recalcButton");
    recalcButton.innerHTML = "Recalculate";
    recalcButton.on.click.add((Event e) {
      _presenter.recalculateAll();
    });
  }

  void run() {
    _spreadsheet = new Spreadsheet();
    SYLKReader reader = new SYLKReader();
    reader.request("mortgage", (String data) {
        reader.loadFromString(_spreadsheet, data);
        _presenter = new SpreadsheetPresenter(_spreadsheet, window,
            0, 0, DEFAULT_VISIBLE_ROWS, DEFAULT_VISIBLE_COLUMNS);
        _spreadsheet.setListener(_presenter);
        _presenter.recalculateViewport();
      });
  }
}
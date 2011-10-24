// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("Total");
#import("dart:html");
#import("TotalLib.dart");

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
    Reader reader = new SYLKReader();
    List<String> data = reader.makeExample("mortgage");
    reader.loadSpreadsheet(_spreadsheet, data);
    _presenter = new SpreadsheetPresenter(_spreadsheet, window,
      0, 0, DEFAULT_VISIBLE_ROWS, DEFAULT_VISIBLE_COLUMNS);
    _spreadsheet.setListener(_presenter);
    _presenter.recalculateViewport();
  }
}

void main() {
  // Instantiate the app
  new Total().run();

  document.on.keyDown.add((KeyboardEvent event) {
    // TODO: Browsers need to fix the keyCode/keyIdentifier situation.
    if (event.ctrlKey && event.keyCode == 68 /* d */) {
      Element db = document.query('#debugbar');
      if (db.classes.contains('on')) {
        db.classes.remove('on');
      } else {
        db.classes.add('on');
      }
    }
  }, false);
}

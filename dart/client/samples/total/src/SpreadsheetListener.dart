// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Listener for Spreadsheet events.  All [Spreadsheet] -> [SpreadsheetPresenter] calls should pass
 * through this interface.
 */
interface SpreadsheetListener {

  void onColumnInserted(int col);

  void onColumnWidthChanged(int col, int size);

  // Model requests a full refresh
  // TODO: this is temporary (legacy) and needs to be broken into methods such
  // as onCellChanged(), onCellsChanged(), etc.
  void onRefresh();

  void onRowHeightChanged(int row, int size);

  void onRowInserted(int row);

  // Table size (ie, number of columns or rows) changed
  void onTableSizeChanged();
}

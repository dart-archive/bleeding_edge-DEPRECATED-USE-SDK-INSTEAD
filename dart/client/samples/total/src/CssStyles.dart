// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Because we can't read CSS values in code, we keep an auxillary class for style settings
// that need to be accessed programmatically
class CssStyles {

  // Height of the column headers above the spreadsheet
  static final int COLUMN_HEADER_HEIGHT = 25; // px

  // Initial column width
  static final int DEFAULT_COLUMN_WIDTH = 130; // px

  // Initial row height
  static final int DEFAULT_ROW_HEIGHT = 25; // px

  // Inner menu height
  static final int INNER_MENU_HEIGHT = 38; // px

  // Smallest allowable width for a column
  static final int MIN_COLUMN_WIDTH = 25; // px

  // Smallest allowable height for a row
  static final int MIN_ROW_HEIGHT = 25; // px

  // Width of the object bar on the left side of the page
  static final int OBJECTBAR_WIDTH = 32; // px

  // Width of the row headers to the left of the spreadsheet
  static final int ROW_HEADER_WIDTH = 50; // px

  // Height of the sandbar on the top of the page
  static final int SANDBAR_HEIGHT = 33; // px

  // Scrollbar width
  static final int SCROLLBAR_WIDTH = 10; // px
}

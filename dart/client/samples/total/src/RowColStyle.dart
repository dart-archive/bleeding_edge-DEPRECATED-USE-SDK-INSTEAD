// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A class holding information about row, column or spreadsheet style.
 *
 * This method adds a unique change order id to the Style class, enabling
 * the application to prioritize styles when merging.
 *
 * This class is immutable; if the style changes, create a new one.
 */
class RowColStyle {
  static IdGenerator _idGenerator;

  static Style merge3(RowColStyle style1, RowColStyle style2, RowColStyle style3) {
    Style s1 = null;
    int id1 = 0;
    if (style1 != null) {
      s1 = style1.style;
      id1 = style1.id;
    }
    Style s2 = null;
    int id2 = 0;
    if (style2 != null) {
      s2 = style2.style;
      id2 = style2.id;
    }
    Style s3 = null;
    int id3 = 0;
    if (style3 != null) {
      s3 = style3.style;
      id3 = style3.id;
    }
    return Style.merge3(s1, id1, s2, id2, s3, id3);
  }

  int _id;
  Style _style;

  int get id() => _id;

  Style get style() => _style;

  RowColStyle(this._style) {
    if (_style == null) {
      throw new RuntimeException("style == null");
    }
    if (_idGenerator == null) {
      _idGenerator = new IdGenerator();
    }
    _id = _idGenerator.next();
  }

  // These objects are immutable.
  bool operator==(Object o) => o === this;
}

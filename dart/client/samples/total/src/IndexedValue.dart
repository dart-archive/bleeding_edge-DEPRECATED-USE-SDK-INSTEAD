// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Utility class used for sorting spreadsheet columns
class IndexedValue {
  // Sort numbers, followed by non-blank strings, followed by blanks
  static final int NUMBER = 0;
  static final int STRING = 1;
  static final int BLANK = 2;

  int _class;
  double _dValue;
  int _index;
  String _sValue;

  int get index() {
    return _index;
  }

  IndexedValue.blank(this._index) {
    _class = BLANK;
  }

  IndexedValue.number(this._index, this._dValue) {
    _class = NUMBER;
  }

  IndexedValue.string(this._index, this._sValue) {
    _class = (_sValue == null || _sValue.length == 0) ? BLANK : STRING;
  }

  int compareTo(IndexedValue other) {
    if (_class < other._class) {
      return -1;
    } else if (_class > other._class) {
      return 1;
    }

    if (_class == NUMBER) {
      return _dValue < other._dValue ? -1 : (_dValue > other._dValue ? 1 : 0);
    } else if (_class == STRING) {
      return StringUtils.compare(_sValue, other._sValue);
    }
    // All blanks are equal
    return 0;
  }
}

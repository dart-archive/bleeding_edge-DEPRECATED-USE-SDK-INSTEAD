// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An object containing a (row, column) reference.
 *
 * Objects of this class are immutable: they do not change after creation.
 */
class RowCol {

  static int hashOneValue(int val) {
    final int fnvPrime = 0x01000193;
    int output = fnvPrime;
    int octet1 = val & 0xff;
    output ^= octet1;
    output *= fnvPrime;
    int octet2 = (val >> 8) & 0xff;
    output ^= octet2;
    output *= fnvPrime;
    int octet3 = (val >> 16) & 0xff;
    output ^= octet3;

    return output;
  }

  final int _col;
  final int _row;

  int get col() => _col;

  int get row() => _row;

  RowCol(this._row, this._col) { }

  bool operator ==(RowCol other) {
    if (!(other is RowCol)) {
      return false;
    }
    return other._row == _row && other._col == _col;
  }

  RowCol operator +(RowCol other) => new RowCol(_row + other._row, _col + other._col);

  int hashCode() => (51 + hashOneValue(_row)) * 51 + hashOneValue(_col);

  bool isValidCell() => _row > 0 && _col > 0;

  String toA1String() => "${StringUtils.columnString(_col)}${_row}";

  String toString() => "R${_row}C${_col}";

  RowCol translate(int deltaRow, int deltaCol) => new RowCol(_row + deltaRow, _col + deltaCol);
}


// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A generic token.
// This class is immutable, and all derived classes should also be immutable.
class Token {

  // FIXME: replace getters below with static const fields.
  static int get A_LOWER()   { return _c("a"); }  // 97
  static int get A_UPPER()   { return _c("A"); }  // 65
  static int get BACKSLASH() { return _c("\\"); } // 92
  static int get COLON()     { return _c(":"); }  // 58
  static int get COMMA()     { return _c(","); }  // 44
  static int get CR()        { return _c("\n"); } // 13
  static int get C_UPPER()   { return _c("C"); }  // 67
  static int get DOLLAR()    { return _c("\$"); } // 36
  static int get EQUAL()     { return _c("="); }  // 61
  static int get E_LOWER()   { return _c("e"); }  // 101
  static int get E_UPPER()   { return _c("E"); }  // 69
  static int get F_UPPER()   { return _c("F"); }  // 70
  static int get GREATER()   { return _c(">"); }  // 62
  static int get LBRACKET()  { return _c("["); }  // 91
  static int get LESS()      { return _c("<"); }  // 60
  static int get LPAREN()    { return _c("("); }  // 40
  static int get MINUS()     { return _c("-"); }  // 45
  static int get NINE()      { return _c("9"); }  // 57
  static int get PERIOD()    { return _c("."); }  // 46
  static int get PLUS()      { return _c("+"); }  // 43
  static int get QUOTE()     { return _c("\""); } // 34
  static int get RBRACKET()  { return _c("]"); }  // 93
  static int get RPAREN()    { return _c(")"); }  // 41
  static int get R_UPPER()   { return _c("R"); }  // 82
  static int get SLASH()     { return _c("/"); }  // 47
  static int get SPACE()     { return _c(" "); }  // 32
  static int get STAR()      { return _c("*"); }  // 42
  static int get TAB()       { return _c("\t"); } // 9
  static int get T_UPPER()   { return _c("T"); }  // 84
  static int get ZERO()      { return _c("0"); }  // 48
  static int get Z_LOWER()   { return _c("z"); }  // 122
  static int get Z_UPPER()   { return _c("Z"); }  // 90

  static int _c(String s) => s.charCodeAt(0);

  Token() {
  }

  bool isAddOp() => false;

  bool isColon() => false;

  bool isComma() => false;

  bool isComparisonOp() => false;

  bool isLParen() => false;

  bool isMinus() => false;

  bool isMulOp() => false;

  bool isRParen() => false;

  // Offset the references in this token to account for the given range moving
  // by the given offset.
  //
  // tokenInRange is true when this token is in the range.
  //
  // Return a new Token if this one must change, otherwise return null.
  Token offsetReferences(CellRange range, RowCol offset, CellLocation tokenLocation,
      bool tokenInRange) => null;

  String toA1String(CellLocation location) => toString();

  String toDebugString() => "TOKEN";

  String toString() => "TOKEN";

  String toUserString() => toString();
}

// A token holding a reference to another cell.
// This class is immutable.
class CellRefToken extends Token {
  int _col;
  bool _colRelative;
  String _original;
  int _row;
  bool _rowRelative;
  Spreadsheet _spreadsheet;

  int get col() => _col;

  bool get colRelative() => _colRelative;

  int get row() => _row;

  bool get rowRelative() => _rowRelative;

  Spreadsheet get spreadsheet() => _spreadsheet;

  CellRefToken(CellLocation location, this._rowRelative, this._colRelative, this._original)
      : super() {
    _spreadsheet = location.spreadsheet;
    _row = location.row;
    _col = location.col;
  }

  factory CellRefToken.offset(CellRefToken base, int deltaX, int deltaY) {
    CellRefToken token = new CellRefToken._private();
    token._spreadsheet = base._spreadsheet;
    token._row = base._row + deltaY;
    token._rowRelative = base._rowRelative;
    token._col = base._col + deltaX;
    token._colRelative = base._colRelative;
    token._original = token.toString();
    return token;
  }

  CellRefToken._private() : super() { }

  CellLocation getCellLocation(CellLocation location) => new CellLocation(_spreadsheet,
      new RowCol(getRow(location), getCol(location)));

  int getCol(CellLocation location) {
    if (_colRelative) {
      if (location.spreadsheet != _spreadsheet) {
        throw new RuntimeException("Relative reference must be to the same sheet");
      }
      return _col + location.col;
    } else {
      return _col;
    }
  }

  int getRow(CellLocation location) {
    if (_rowRelative) {
      if (location.spreadsheet != _spreadsheet) {
        throw new RuntimeException("Relative reference must be to the same sheet");
      }
      return _row + location.row;
    } else {
      return _row;
    }
  }

  Token offsetReferences(CellRange range, RowCol offset, CellLocation tokenLocation,
      bool tokenInRange) {
    bool targetInRange = range.isInRange(getCellLocation(tokenLocation));

    int newRow = _row;
    int newCol = _col;
    if (tokenInRange) {
      if (targetInRange) {
        // Absolute references from the shifted portion to the shifted portion are modified to
        // refer to the same cell as before the insertion.
        // Relative references from the shifted portion to the shifted portion are left unchanged.
        if (!_rowRelative) {
          newRow += offset.row;
        }
        if (!_colRelative) {
          newCol += offset.col;
        }
      } else {
        // Relative references from the shifted section into the unshifted section are
        // adjusted to refer to the same cell as before the insertion.
        if (_rowRelative) {
          newRow -= offset.row;
        }
        if (_colRelative) {
          newCol -= offset.col;
        }
      }
    } else if (targetInRange) {
      // Absolute and relative formula references from the unshifted portions of the sheet into
      // the shifted segment are adjusted to refer to the same cell as before the insertion.
      newRow += offset.row;
      newCol += offset.col;
    }
    // Absolute and relative formula references from the unshifted portions of the sheet into
    // the unshifted segment are unchanged.

    if (newRow != _row || newCol != _col) {
      // Something changed. Create a new token from this one and return it.
      return new CellRefToken(new CellLocation(_spreadsheet, new RowCol(newRow, newCol)),
          _rowRelative, _colRelative, null);
    }

    return null;
  }

  String toA1String(CellLocation location) {
    // TODO: Output sheet if not the default sheet. We'll need to pass in the
    // sheet we are printing for.
    int displayRow = getRow(location);
    int displayCol = getCol(location);
    // Use RC format if the row or col falls outside the table
    if (displayRow < 1 || displayCol < 1) {
      return toString();
    }
    String cr = _colRelative ? '' : '\$';
    String rr = _rowRelative ? '' : '\$';
    return "${cr}${StringUtils.columnString(displayCol)}${rr}${StringUtils.rowString(displayRow)}";
  }

  // TODO: Output sheet if not the default sheet. We'll need to pass in the
  // sheet we are printing for.
  String toDebugString() => "RCRefToken[${this}]";

  // TODO: Output sheet if not the default sheet. We'll need to pass in the
  // sheet we are printing for.
  String toString() => "R${_ref(_row, _rowRelative)}C${_ref(_col, _colRelative)}";

  String toUserString() => _original == null ? toString() : _original;

  String _ref(int index, bool relative) {
    if (relative && index == 0) {
      // omit [0]
      return "";
    }
    if (relative) {
      return "[${index}]";
    } else {
      return index.toString();
    }
  }
}

class InvalidCellRefToken extends Token {
  InvalidCellRefToken() : super() { }

  String toDebugString() => "InvalidCellRefToken";

  String toString() => "!UNDEFINED";
}

// A token to represent a reference to a range of cells.
// Synthesized by the Parser.
// This class is immutable.
class RangeToken extends Token {
  CellRefToken _endRef;
  CellRefToken _startRef;

  CellRefToken get endRef() => _endRef;

  Spreadsheet get spreadsheet() => _startRef.spreadsheet;

  CellRefToken get startRef() => _startRef;

  RangeToken(this._startRef, this._endRef) : super() {
    // TODO: Add validation
  }

  CellRange getCellRange(CellLocation location) {
    CellLocation startLocation = _startRef.getCellLocation(location);
    CellLocation endLocation = _endRef.getCellLocation(location);
    return new CellRange(startLocation.spreadsheet, startLocation.rowCol, endLocation.rowCol);
  }

  // Modify the range to remove invalid cells, as given by the intersection
  // argument.
  // thisRange is the absolute range of this range token
  // intersection is the interseciton of the removed block with this range. It
  // is sure to be a subset of this range
  // shiftingUp is true if the deleted block will be filled by shifting cells
  // up, as opposed to left.
  // Return a new range token if we successfully modify, null if this entire range must be
  // invalidated.
  Token invalidateRange(CellRange thisRange, CellRange intersection, bool shiftingUp) {
    // We cannot modify if the intersection is the entire range. Instead this
    // range will be invalidated in its entirety.
    if (intersection == thisRange) {
      return null;
    }

    CellRefToken newStart = _startRef;
    CellRefToken newEnd = _endRef;
    if (shiftingUp) {
      // We can modify the range if the intersection spans the entire width of
      // the range.
      if (intersection.minCorner.col != thisRange.minCorner.col
          || intersection.maxCorner.col != thisRange.maxCorner.col) {
        return null;
      }

      // We know we can modify. We may need to move the top of the range down,
      // or the bottom up, but not both.
      if (intersection.minCorner.row == thisRange.minCorner.row) {
        // The intersection cuts off the top of the range.
        int newStartRow =
            _startRef.row + (intersection.maxCorner.row - intersection.minCorner.row + 1);
        newStart = new CellRefToken(
            new CellLocation(_startRef.spreadsheet, new RowCol(newStartRow, _startRef.col)),
            _startRef.rowRelative, _startRef.colRelative, null);
      }
      if (intersection.maxCorner.row == thisRange.maxCorner.row) {
        int newEndRow =
            _endRef.row - (intersection.maxCorner.row - intersection.minCorner.row + 1);
        newEnd = new CellRefToken(
            new CellLocation(_endRef.spreadsheet, new RowCol(newEndRow, _endRef.col)),
            _endRef.rowRelative, _endRef.colRelative, null);
      }
    } else {
      // We can modify the range if the intersection spans the entire height of
      // the range.
      if (intersection.minCorner.row != thisRange.minCorner.row
          || intersection.maxCorner.row != thisRange.maxCorner.row) {
        return null;
      }

      // We know we can modify. We may need to move the left of the range right,
      // or the right toward the left, but not both.
      if (intersection.minCorner.col == thisRange.minCorner.col) {
        // The intersection cuts off the left of the range.
        int newStartCol =
            _startRef.col + (intersection.maxCorner.col - intersection.minCorner.col + 1);
        newStart = new CellRefToken(
            new CellLocation(_startRef.spreadsheet, new RowCol(_startRef.row, newStartCol)),
            _startRef.rowRelative, _startRef.colRelative, null);
      }
      if (intersection.maxCorner.col == thisRange.maxCorner.col) {
        int newEndCol =
            _endRef.col - (intersection.maxCorner.col - intersection.minCorner.col + 1);
        newEnd = new CellRefToken(
            new CellLocation(_endRef.spreadsheet, new RowCol(_endRef.row, newEndCol)),
            _endRef.rowRelative, _endRef.colRelative, null);
      }
    }

    // Return null if we did not modify anything
    if (newStart === _startRef && newEnd === _endRef) {
      return null;
    }

    return new RangeToken(newStart, newEnd);
  }

  Token offsetReferences(CellRange range, RowCol offset, CellLocation tokenLocation,
      bool tokenInRange) {
    // TODO: This code does not account for cells shifted from within
    // a range, such that the output range would need to be broken into
    // disjoint blocks to maintain references. It should, when we allow
    // disjoint ranges for aggregating functions.
    Token newStart = _startRef.offsetReferences(range, offset, tokenLocation, tokenInRange);
    Token newEnd = _endRef.offsetReferences(range, offset, tokenLocation, tokenInRange);
    if (newStart != null || newEnd != null) {
      if (newStart == null) {
        newStart = _startRef;
      }
      if (newEnd == null) {
        newEnd = _endRef;
      }
      return new RangeToken(newStart, newEnd);
    }

    return null;
  }

  String toDebugString() => "RangeToken[${startRef},${endRef}]";
}

// A token that holds a function name.
// This class is immutable.
class FunctionNameToken extends Token {
  String _name;

  String get name() => _name;

  FunctionNameToken(this._name) : super() { }

  String toDebugString() => "FunctionNameToken[${this}]";

  String toString() => _name;
}

// A token that holds a double.
// This class is immutable.
class NumberToken extends Token {
  double _value;

  double get value() => _value;

  NumberToken(this._value) : super() { }

  String toDebugString() => "NumberToken[${this}]";

  // Canonicalize numbers by removing trailing zeroes
  // 1234.000 ==> 1200
  // 1234.500 ==> 1234.5
  String toString() {
    String s = _value.toString();
    int dot = s.indexOf(".", 0);
    if (dot != -1) {
      int len = s.length;
      // Remove trailing zeroes
      while (s[len - 1] == "0"[0]) {
        len--;
      }
      // Remove the decimal point if it has become the last character
      if (len - 1 == dot) {
        len--;
      }
      if (len < s.length) {
        s = s.substring(0, len);
      }
    }
    return s;
  }
}

class StringToken extends Token {
  String _value;

  String get value() => _value;

  StringToken(this._value) : super() { }

  String toDebugString() => "StringToken[${this}]";

  String toString() => '"${StringUtils.escapeStringLiteral(_value)}"';
}

class BooleanToken extends NumberToken {
  bool _bValue;

  bool get bValue() => _bValue;

  BooleanToken(bool bValue) : super(bValue ? 1.0 : 0.0) {
    this._bValue = bValue;
  }

  String toDebugString() => "BooleanToken[${this}]";

  String toString() => _bValue ? "TRUE" : "FALSE";
}

// A token that holds an operator.
// This class is immutable.
class OperatorToken extends Token {
  // FIXME: replace getters below with static const fields.
  static int get OP_COLON() { return Token.COLON; }
  static int get OP_COMMA() { return Token.COMMA; }
  static int get OP_DIVIDE() { return Token.SLASH; }
  static int get OP_EQUAL() { return Token.EQUAL; }
  static int get OP_GREATER() { return Token.GREATER; }
  static int get OP_GREATER_THAN_EQUAL() { return 3; } // '<='
  static int get OP_LESS() { return Token.LESS; }
  static int get OP_LESS_THAN_EQUAL() { return 2; } // '<='
  static int get OP_LPAREN() { return Token.LPAREN; }
  static int get OP_MINUS() { return Token.MINUS; }
  static int get OP_NOT_EQUAL() { return 1; } // '<>'
  static int get OP_PLUS() { return Token.PLUS; }
  static int get OP_RPAREN() { return Token.RPAREN; }
  static int get OP_TIMES() { return Token.STAR; }

  int _type;

  int get type() => _type;

  OperatorToken(int op) : super() {
    switch (op) {
    case OP_PLUS:
    case OP_MINUS:
    case OP_TIMES:
    case OP_DIVIDE:
    case OP_LPAREN:
    case OP_RPAREN:
    case OP_COMMA:
    case OP_COLON:
    case OP_LESS:
    case OP_EQUAL:
    case OP_GREATER:
    case OP_NOT_EQUAL:
    case OP_LESS_THAN_EQUAL:
    case OP_GREATER_THAN_EQUAL:
      _type = op;
      break;
    default:
      throw new RuntimeException("Unknown operator");
    }
  }

  bool isAddOp() => type == OP_PLUS || type == OP_MINUS;

  bool isColon() => type == OP_COLON;

  bool isComma() => type == OP_COMMA;

  bool isComparisonOp() => type == OP_LESS || type == OP_GREATER || type == OP_EQUAL
      || type == OP_NOT_EQUAL || type == OP_LESS_THAN_EQUAL || type == OP_GREATER_THAN_EQUAL;

  bool isLParen() => type == OP_LPAREN;

  bool isMinus() => type == OP_MINUS;

  bool isMulOp() => type == OP_TIMES || type == OP_DIVIDE;

  bool isRParen() => type == OP_RPAREN;

  Value operate(Value left, Value right) {
    double l = left.asDouble(null);
    double r = right.asDouble(null);

    switch (_type) {
    case OP_PLUS:
      return new DoubleValue(l + r);
    case OP_MINUS:
      return new DoubleValue(l - r);
    case OP_TIMES:
      return new DoubleValue(l * r);
    case OP_DIVIDE:
      if (r == 0.0) {
        throw new DivByZeroException();
      }
      return new DoubleValue(l / r);
    case OP_GREATER:
      return new BooleanValue(l > r);
    case OP_EQUAL:
      return new BooleanValue(l == r);
    case OP_LESS:
      return new BooleanValue(l < r);
    case OP_NOT_EQUAL:
      return new BooleanValue(l != r);
    case OP_LESS_THAN_EQUAL:
      return new BooleanValue(l <= r);
    case OP_GREATER_THAN_EQUAL:
      return new BooleanValue(l >= r);
    default:
      throw new NotImplementedException();
    }
  }

  String toDebugString() => "OperatorToken[${this}]";

  String toString() {
    switch (_type) {
    case OP_PLUS:
      return "+";
    case OP_MINUS:
      return "-";
    case OP_TIMES:
      return "*";
    case OP_DIVIDE:
      return "/";
    case OP_LPAREN:
      return "(";
    case OP_RPAREN:
      return ")";
    case OP_COMMA:
      return ",";
    case OP_COLON:
      return ":";
    case OP_LESS:
      return "<";
    case OP_EQUAL:
      return "=";
    case OP_GREATER:
      return ">";
    case OP_NOT_EQUAL:
      return "<>";
    case OP_LESS_THAN_EQUAL:
      return "<=";
    case OP_GREATER_THAN_EQUAL:
      return ">=";
    }
  }
}

// Token that hlds some amount of whitespace, represented as a string.
// This class is immutable.
class WhitespaceToken extends Token {
  String _whitespace;

  WhitespaceToken(this._whitespace) : super() { }

  String toDebugString() => "WhitespaceToken[${this}]";

  String toString() => _whitespace;
}

// TODO: Add a way to refer to another spreadsheet when making references to cells.
// Infrastructure exists to store the information, but no way to specify it.
class Scanner {
  static final int EOI = -1;

  // A=1, ..., Z=26, AA=27, ..., AZ=52, ...
  static int columnNameToInt(String columnName) {
    int value = 0;
    value = letterToInt(columnName.charCodeAt(0));
    if (value == -1) {
      return -1;
    }
    for (int i = 1; i < columnName.length; i++) {
      value *= 26;
      int digit = letterToInt(columnName.charCodeAt(i));
      if (digit == -1) {
        return -1;
      }
      value += digit;
    }
    return value;
  }

  static bool isDigit(int c) => c >= Token.ZERO && c <= Token.NINE;

  static bool isLetter(int c) => (c >= Token.A_UPPER && c <= Token.Z_UPPER)
      || (c >= Token.A_LOWER && c <= Token.Z_LOWER);

  static bool isNewline(int c) => c == Token.CR;

  static bool isOperator(int c) => c == Token.PLUS || c == Token.MINUS || c == Token.STAR
      || c == Token.SLASH || c == Token.LPAREN || c == Token.RPAREN || c == Token.COMMA
      || c == Token.COLON || c == Token.LESS || c == Token.EQUAL || c == Token.GREATER;

  static bool isWhitespace(int c) => c == Token.SPACE || c == Token.TAB;

  static bool isWhitespaceOrNewline(int c) => c == Token.SPACE || c == Token.TAB || c == Token.CR;

  // A=1, B=2, ..., Z=26
  static int letterToInt(int letter) {
    if (letter < Token.A_UPPER || letter > Token.Z_UPPER) {
      return -1;
    }
    return letter - Token.A_UPPER + 1;
  }

  String _input;
  CellLocation _location;
  int _pos;
  bool _preserveWhitespace;

  Scanner(this._input, this._location) : _pos = 0, _preserveWhitespace = false { }

  Scanner.preserveWhitespace(this._input, this._location)
      : _pos = 0, _preserveWhitespace = true { }

  CellRefToken getA1Ref() {
    bool colRelative = true;
    bool rowRelative = true;
    int rowRef;
    int colRef;

    int start = _pos;
    int c = nextChar();
    if (c == Token.DOLLAR) {
      colRelative = false;
      c = nextChar();
    }
    if (!isLetter(c)) {
      _pos = start;
      return null;
    }
    int colStart = _pos - 1;
    while (isLetter(c)) {
      c = nextChar();
    }
    retract(1);
    String colName = _input.substring(colStart, _pos);
    colRef = columnNameToInt(colName);
    if (colRef == -1) {
      _pos = start;
      return null;
    }

    c = nextChar();
    if (c == Token.DOLLAR) {
      rowRelative = false;
      c = nextChar();
    }
    if (!isDigit(c)) {
      _pos = start;
      return null;
    }
    retract(1);
    rowRef = scanInt();
    if (colRelative) {
      colRef -= _location.col;
    }
    if (rowRelative) {
      rowRef -= _location.row;
    }

    String original = _input.substring(start, _pos);
    return new CellRefToken(new CellLocation(_location.spreadsheet, new RowCol(rowRef, colRef)),
        rowRelative, colRelative, original);
  }

  BooleanToken getBoolean() {
    if (_input.length >= _pos + 4 && _input.substring(_pos, _pos + 4) == "TRUE") {
      _pos += 4;
      return new BooleanToken(true);
    } else if (_input.length >= _pos + 5 && _input.substring(_pos, _pos + 5) == "FALSE") {
      _pos += 5;
      return new BooleanToken(false);
    } else {
      return null;
    }
  }

  FunctionNameToken getFunctionName() {
    int start = _pos;
    int c = nextChar();
    if (!isLetter(c)) {
      _pos = start;
      return null;
    }
    StringBuffer sb = new StringBuffer();
    c = nextChar();
    while (isLetter(c) || isDigit(c)) {
      c = nextChar();
    }
    retract(1);
    // Following character must be a '('
    if (c != Token.LPAREN) {
      _pos = start;
      return null;
    }
    return new FunctionNameToken(_input.substring(start, _pos));
  }

  // [-][0-9]*(\.[0-9]+)?([eE][+-]?[0-9]+)?
  NumberToken getNumber() {
    int start = _pos;
    int c = nextChar();
    if (!isDigit(c) && c != Token.PERIOD) {
      _pos = start;
      return null;
    }

    while (isDigit(c)) {
      c = nextChar();
    }
    if (c == Token.PERIOD) {
      c = nextChar();
      if (!isDigit(c)) {
        _pos = start;
        return null;
      }
      while (isDigit(c)) {
        c = nextChar();
      }
    }
    if (c == Token.E_LOWER || c == Token.E_UPPER) {
      int ePos = _pos;
      c = nextChar();
      if (c == Token.MINUS || c == Token.PLUS) {
        c = nextChar();
      }

      // 5.7e-Hello -> parse as '5.7', leave _pos at 'e'
      if (!isDigit(c)) {
        _pos = ePos;
      }
      while (isDigit(c)) {
        c = nextChar();
      }
    }
    retract(1);
    double value = Math.parseDouble(_input.substring(start, _pos));
    return new NumberToken(value);
  }

  OperatorToken getOperator() {
    int start = _pos;
    int c = nextChar();
    if (isOperator(c)) {
      int p = peekChar();
      if (c == Token.LESS) {
        if (p == Token.GREATER) {
          nextChar();
          return new OperatorToken(OperatorToken.OP_NOT_EQUAL);
        } else if (p == Token.EQUAL) {
          nextChar();
          return new OperatorToken(OperatorToken.OP_LESS_THAN_EQUAL);
        }
      } else if (c == Token.GREATER && p == Token.EQUAL) {
        nextChar();
        return new OperatorToken(OperatorToken.OP_GREATER_THAN_EQUAL);
      }
      return new OperatorToken(c);
    }

    _pos = start;
    return null;
  }

  CellRefToken getRCRef() {
    bool rowRelative = false;
    bool colRelative = false;
    bool rowNeedsRightBracket = false;
    bool colNeedsRightBracket = false;
    int row = 0;
    int col = 0;

    // Record starting position
    int start = _pos;

    int c = nextChar();
    if (c != Token.R_UPPER) {
      _pos = start;
      return null;
    }
    c = nextChar();
    if (c == Token.LBRACKET) {
      rowRelative = true;
      rowNeedsRightBracket = true;
      row = scanInt();
    } else if (isDigit(c) || (c == Token.MINUS && rowRelative)) {
      retract(1);
      row = scanInt();
    } else if (c == Token.C_UPPER) {
      rowRelative = true;
      row = 0;
      retract(1);
    }
    if (rowNeedsRightBracket) {
      c = nextChar();
      if (c != Token.RBRACKET) {
        _pos = start;
        return null;
      }
    }

    c = nextChar();
    if (c != Token.C_UPPER) {
      _pos = start;
      return null;
    }
    c = nextChar();
    if (c == Token.LBRACKET) {
      colRelative = true;
      colNeedsRightBracket = true;
      col = scanInt();
    } else if (isDigit(c) || (c == Token.MINUS && colRelative)) {
      retract(1);
      col = scanInt();
    } else {
      colRelative = true;
      col = 0;
      retract(1);
    }
    if (colNeedsRightBracket) {
      c = nextChar();
      if (c != Token.RBRACKET) {
        _pos = start;
        return null;
      }
    }

    String original = _input.substring(start, _pos);
    return new CellRefToken(new CellLocation(_location.spreadsheet, new RowCol(row, col)),
        rowRelative, colRelative, original);
  }

  StringToken getString() {
    int start = _pos;
    int c = nextChar();
    if (c != Token.QUOTE) {
      _pos = start;
      return null;
    }

    List<int> charCodes = new List<int>();
    c = nextChar();
    while (c != Token.QUOTE) {
      if (c == Token.BACKSLASH) {
        c = nextChar();
        if (c == Token.BACKSLASH) {
          charCodes.add(Token.BACKSLASH);
        } else if (c == Token.QUOTE) {
          charCodes.add(Token.QUOTE);
        } else {
          _pos = start;
          return null;
        }
      } else {
        charCodes.add(c);
      }
      c = nextChar();
    }

    String s = new String.fromCharCodes(charCodes);
    return new StringToken(s);
  }

  int nextChar() {
    int length = _input.length;
    if (_pos == length) {
      _pos++;
      return EOI;
    } else if (_pos > length) {
      throw new RuntimeException("Passed EOI!");
    }
    return _input.charCodeAt(_pos++);
  }

  Token nextToken() {
    // Skip whitespace
    int start = _pos;
    while (isWhitespaceOrNewline(peekChar())) {
      nextChar();
    }
    if (_preserveWhitespace && (_pos > start)) {
      WhitespaceToken whitespaceToken = new WhitespaceToken(_input.substring(start, _pos));
      return whitespaceToken;
    }

    int c = peekChar();
    if (c == EOI) {
      return null;
    }

    // Function name
    if (isLetter(c)) {
      FunctionNameToken functionName = getFunctionName();
      if (functionName != null) {
        return functionName;
      }
    }

    // RC-style cell reference
    if (c == Token.R_UPPER) {
      CellRefToken cellRef = getRCRef();
      if (cellRef != null) {
        return cellRef;
      }
    }

    // A1-style cell reference
    if (c == Token.DOLLAR || isLetter(c)) {
      CellRefToken cellRef = getA1Ref();
      if (cellRef != null) {
        return cellRef;
      }
    }

    // Number
    if (isDigit(c) || c == Token.PERIOD) {
      NumberToken number = getNumber();
      if (number != null) {
        return number;
      }
    }

    // Boolean
    if (c == Token.F_UPPER || c == Token.T_UPPER) {
      BooleanToken boolean = getBoolean();
      if (boolean != null) {
        return boolean;
      }
    }

    // Operator
    if (isOperator(c)) {
      OperatorToken operator = getOperator();
      if (operator != null) {
        return operator;
      }
    }

    if (c == Token.QUOTE) {
      StringToken str = getString();
      if (str != null) {
        return str;
      }
    }

    throw new RuntimeException("Can't recognize '${_input.substring(_pos, _input.length)}'");
  }

  int peekChar() {
    int length = _input.length;
    if (_pos == length) {
      return EOI;
    } else if (_pos > length) {
      throw new RuntimeException("Passed EOI!");
    }
    return _input.charCodeAt(_pos);
  }

  void retract(int count) {
    _pos -= count;
  }

  List<Token> scan() {
    List<Token> tokens = new List<Token>();
    Token t;
    while ((t = nextToken()) != null) {
      tokens.add(t);
    }
    return tokens;
  }

  int scanInt() {
    int start = _pos;
    int c = nextChar();
    if (c == Token.MINUS) {
      c = nextChar();
    }
    while (isDigit(c)) {
      c = nextChar();
    }
    retract(1);
    return Math.parseInt(_input.substring(start, _pos));
  }
}

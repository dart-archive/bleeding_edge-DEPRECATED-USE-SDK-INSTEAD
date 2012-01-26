// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A formula, which may potentially be implemented directly in Dart.
 *
 * A formula must be able to calculate itself and return its dependencies, relative
 * to a particular cell.
 *
 * Formulas must be immutable.
 */
// TODO: allow location-independent formulas
interface Formula {
  /**
   * If true, this formula must be evaluated even when the cell is not marked
   * dirty.
   */
  bool alwaysRecalculate();

  /**
   * Calculate the value for this cell and return the result.
   */
  Value calculate();

  /**
   * Return those cell locations that this formula uses to compute its result.
   */
  Set<CellLocation> getDependencies();

  /**
   * Invalidate the references from this formula into the given range, when the
   * range will be filled by shifting cells as given in the shiftOffset.
   *
   * This method should return a new formula if the references are invalidated
   * or otherwise modified. Return null when the formula is unchanged.
   */
  Formula invalidateReferences(CellLocation thisLocation, CellRange range, RowCol shiftOffset);

  /**
   * Update this formula for a block shift that moves the given range by the
   * given offset.
   *
   * Return a new formula if the shift will change the existing formula, or null
   * if the shift does not affect this formula.
   */
  Formula modifyDependenciesForShift(CellRange range, RowCol offset);
}

/**
 * A formula given by a String (with or without a leading '=' character)
 */
class StringFormula implements Formula {
  static Functions functions;

  bool _alwaysRecalculate;
  String _formula;
  CellLocation _location;
  List<Token> _rawTokens;
  List<Token> _tokens;

  StringFormula(this._formula, this._location) : _alwaysRecalculate = false {
    if (functions == null) {
      functions = new Functions();
    }
    // Strip off leading '='
    if (_formula[0] == "="[0]) {
      _formula = _formula.substring(1, _formula.length);
    }

    // Attempt to parse the formula
    Scanner scanner = new Scanner(_formula, _location);
    Parser parser = new Parser(scanner);
    try {
      _tokens = parser.parse();
      _tokens.forEach((Token t) {
        if (t is FunctionNameToken) {
          FunctionNameToken fnt = t;
          if (functions.alwaysRecalculate(fnt.name)) {
            _alwaysRecalculate = true;
          }
        }
      });

      scanner = new Scanner.preserveWhitespace(_formula, _location);
      _rawTokens = scanner.scan();
    } catch (var e) {
      _tokens = null;
      _rawTokens = null;
    }
  }

  StringFormula.fromTokens(this._location, List<Token> tokens, List<Token> rawTokens) {
    _formula = null;
    _tokens = new List<Token>(tokens.length);
    for (int i = 0; i < tokens.length; ++i) {
      _tokens[i] = tokens[i];
    }
    _rawTokens = new List<Token>(rawTokens.length);
    for (int i = 0; i < rawTokens.length; ++i) {
      _rawTokens[i] = rawTokens[i];
    }
  }

  bool alwaysRecalculate() => _alwaysRecalculate;

  // Evaluate an expression given as a list of tokens in postfix order, using a stack
  Value calculate() {
    if (_tokens == null) {
      throw new BadFormulaException();
    }
    return functions.evaluate(_location, _tokens);
  }

  // Return a list of (row, col) references to the cells on which this cell depends
  Set<CellLocation> getDependencies() {
    Set<CellLocation> output = new Set<CellLocation>();
    if (_tokens == null) {
      return output;
    }
    _tokens.forEach((Token t) {
      if (t is CellRefToken) {
        CellRefToken crt = t;
        output.add(crt.getCellLocation(_location));
      } else if (t is RangeToken) {
        RangeToken rt = t;
        CellRefToken start = rt.startRef;
        CellRefToken end = rt.endRef;
        int minRow = Math.min(start.getRow(_location), end.getRow(_location));
        int maxRow = Math.max(start.getRow(_location), end.getRow(_location));
        int minCol = Math.min(start.getCol(_location), end.getCol(_location));
        int maxCol = Math.max(start.getCol(_location), end.getCol(_location));
        for (int i = minRow; i <= maxRow; i++) {
          for (int j = minCol; j <= maxCol; j++) {
            output.add(new CellLocation(rt.spreadsheet, new RowCol(i, j)));
          }
        }
      }
    });
    return output;
  }

  String getFormula(bool useRCRefs) {
    if (_rawTokens == null) {
      return "=${_formula}";
    }
    StringBuffer sb = new StringBuffer();
    sb.add("=");
    _rawTokens.forEach((Token t) {
      if (useRCRefs) {
        sb.add(t.toString());
      } else {
        sb.add(t.toA1String(_location));
      }
    });
    return sb.toString();
  }

  Formula invalidateReferences(CellLocation thisLocation, CellRange range, RowCol shiftOffset) {
    // Figure out which tokens reference the cell and either replace them with InvalidCellRefTokens,
    // or modify them to exclude the deleted cells. If we do need to replace any
    // tokens, then we return a new formula.
    // TODO: Try to sensibly modify cell ranges to exclude deleted cells, but there are numerous
    // cases depending on how the deleted region overlaps the cell range, and not all of them
    // can be supported with simple rectangular regions.
    StringFormula result = null;

    // First look for a changed token and stop when we find one.
    for (int i = 0; i < _tokens.length; ++i) {
      Token newToken = invalidateToken(_tokens[i], thisLocation, range, shiftOffset);
      if (newToken != null) {
        if (result == null ) {
          result = new StringFormula.fromTokens(_location, _tokens, _rawTokens);
        }
        result._tokens[i] = newToken;
      }
    }
    for (int i = 0; i < _rawTokens.length; ++i) {
      Token newToken = invalidateToken(_rawTokens[i], thisLocation, range, shiftOffset);
      if (newToken != null) {
        if (result == null ) {
          result = new StringFormula.fromTokens(_location, _tokens, _rawTokens);
        }
        result._rawTokens[i] = newToken;
      }
    }

    return result;
  }

  Token invalidateToken(Token toCheck, CellLocation thisLocation, CellRange targetRange,
      RowCol shiftOffset) {
    if (toCheck is CellRefToken) {
      CellRefToken crt = toCheck;
      CellLocation refLoc = crt.getCellLocation(thisLocation);
      if (targetRange.isInRange(refLoc)) {
        return new InvalidCellRefToken();
      }
    } else if (toCheck is RangeToken) {
      RangeToken rt = toCheck;

      // First check if this token must be modified at all
      CellRange thisRange = rt.getCellRange(thisLocation);
      CellRange intersection = thisRange.intersect(targetRange);
      if (intersection == null) {
        return null;
      }

      // Now check to see if it's a modification we can make, rather than
      // invalidating the range entirely
      Token newRange = rt.invalidateRange(thisRange, intersection, shiftOffset.col == 0);
      if (newRange != null) {
        return newRange;
      } else {
        return new InvalidCellRefToken();
      }
    }

    return null;
  }

  Formula modifyDependenciesForShift(CellRange range, RowCol offset) {
    if (_tokens == null) {
      return null;
    }
    bool inRange = range.isInRange(_location);
    StringFormula result = null;

    // Do the case where we know for sure we need to modify something about the
    // formula.
    if (inRange) {
      CellLocation newLocation = _location + offset;
      result = new StringFormula.fromTokens(newLocation, _tokens, _rawTokens);
    }

    // Look for changed tokens.
    for (int i = 0; i < _tokens.length; ++i) {
      Token newToken = _tokens[i].offsetReferences(range, offset, _location, inRange);
      if (newToken != null) {
        if (result == null) {
          result = new StringFormula.fromTokens(_location, _tokens, _rawTokens);
        }
        result._tokens[i] = newToken;
      }
    }
    for (int i = 0; i < _rawTokens.length; ++i) {
      Token newToken = _rawTokens[i].offsetReferences(range, offset, _location, inRange);
      if (newToken != null) {
        if (result == null) {
          result = new StringFormula.fromTokens(_location, _tokens, _rawTokens);
        }
        result._rawTokens[i] = newToken;
      }
    }

    return result;
  }
}

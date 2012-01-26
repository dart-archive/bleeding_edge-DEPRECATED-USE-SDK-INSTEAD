// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Classes that hold the content of cells.
 */

/**
 * Base class for cell content.
 *
 * Cells content is immutable.
 */
class CellContent {
  /**
   * Create new empty cell contents.
   */
  CellContent() {
  }

  /**
   * Return true if the content's value should be refreshed on every spreadsheet recalculation.
   *
   * The implementation in this superclass returns false.
   */
  bool alwaysRecalculate() => false;

  /**
   * Create a clone (new instance that is an exact copy).
   *
   * Subclasses must override this method to clone their specific content.
   */
  CellContent clone() => new CellContent();

  /**
   * Return what the user typed to produce the content.
   */
  String getContent() => "";

  /**
   * Return the datatype of this content, one of [Functions.NUMBER], [Functions.DATE],
   * [Functions.TIME], [Functions.DATE_TIME], [Functions.STRING] or -1 if unknown.
   */
  int getDatatype() {
    try {
      Value value = getValue();
      return value.datatype;
    } catch (TotalException e) {
      // If getValue() fails, the datatype is unknown
      return Value.TYPE_UNKNOWN;
    }
  }

  /**
   * Return the Set of (row, col) references to the cells on which this content depends directly.
   *
   * 'null' indicates the empty set.
   */
  Set<CellLocation> getDependencies() => null;

  /**
   * Return the cell contents in 'RC' form, suitable for pasting into another cell.
   */
  String getPasteContent() => getContent();

  /**
   * Return the computed value of this content.
   *
   * Since general content cannot be used as a source value, the implementation in this
   * class throws a ValueException.
   */
  Value getValue() {
    throw new ValueException();
  }

  /**
   * Invalidate this content's reference to a range of cells that will be removed.
   *
   * This method only does work in content that references other cells (and hence
   * not in this base class).
   *
   * Return new content if this content was modified; otherwise null
   */
  CellContent invalidateReferences(CellLocation thisLocation, CellRange range,
      RowCol shiftOffset) => null;

  /**
   * Return true if this cell content requires recomputation.
   *
   * The implementation in this superclass returns false.
   */
  bool isDirty() => false;

  /**
   * Return true if this cell content contains a formula.
   *
   * The implementation in this superclass returns false.
   */
  bool isFormula() => false;

  /**
   * Return true if this content may be represented by a numeric value.  The datatype is
   * retrieved by calling [getDatatype].  All datatypes except [Functions.STRING] and
   * [Value.TYPE_UNKNOWN] are considered numeric.
   */
  bool isNumeric() {
    int datatype = getDatatype();
    return datatype != Value.TYPE_STRING && datatype != Value.TYPE_UNKNOWN;
  }

  /**
   * Return true if this content is a string value.The datatype is
   * retrieved by calling [getDatatype] and if it is equal to [Functions.STRING]
   * then [:true:] is returned, otherwise [:false:] is returned.
   */
  bool isString() => getDatatype() == Value.TYPE_STRING;

  /**
   * Update formula dependencies for an insertion.
   *
   * Content for cells that have moved are in the range.min <= cell <= range.max range, and
   * they have been moved by offset amount.
   *
   * This method only does work for content that references other cells (and hence
   * not in this base class).
   *
   * Return new content if the content was modified; otherwise null.
   */
  CellContent modifyDependenciesForShift(CellRange range, RowCol offset) => null;

  /**
   * Set the content to be dirty, so that the next call to isDirty will return true if
   * the content supports recalculation.
   *
   * Content that never changes can ignore the dirty setting.
   */
  void setDirty() {}

  /**
   * Return a formatted HTML version of this contents.  The value is retrieved using
   * [getValue] and the [Style] and the value's datatype are used to select the formatting
   * method.
   */
  String toHtml(Style style) {
    String result;

    try {
      Value val = getValue();
      int datatype = val.datatype;
      if (datatype == Value.TYPE_STRING) {
        String value = val.asString(null);
        String qh = HtmlUtils.quoteHtml(value);
        result = style.formatText(qh);
      } else {
        // All other datatypes are represented numerically
        // The default format is responsible for displaying values as
        // numbers, dates, times, booleans, etc.
        double value = val.asDouble(null);
        String nf = style.formatNumber(datatype, value);
        result = style.formatText(nf);
      }
    } catch (TotalException e) {
      return HtmlUtils.quoteHtml(e.toString());
    }

    return result;
  }
}

class ValueContent extends CellContent {
  String _content;
  Value _value;

  ValueContent(this._value, this._content) : super() { }

  CellContent clone() => new ValueContent(_content, _value);

  String getContent() => _content;

  Value getValue() => _value;
}

/**
 * Cell content that evaluates to a formula.
 *
 * Formula objects (i.e., pre-compiled formulas) may be shared by multiple cells.
 * Using RC notation internally allows for greater sharing.
 */
// TODO:  cache compiled formulas and canonicalize formula strings
class FormulaContent extends CellContent {
  static final int DIRTY = 0;
  static final int ERROR = 1;
  static final int IN_PROGRESS = 2;
  static final int UP_TO_DATE = 3;

  FormulaException _error;
  final StringFormula _formula;
  CellLocation _location;
  int _state;
  Value _val;

  FormulaContent(this._location, this._formula) : super() {
    _state = DIRTY;
  }

  bool alwaysRecalculate() => _formula.alwaysRecalculate();

  CellContent clone() => new FormulaContent(_location, _formula);

  String getContent() => _formula.getFormula(false);

  Set<CellLocation> getDependencies() => _formula.getDependencies();

  String getPasteContent() => _formula.getFormula(true);

  Value getValue() {
    // Evaluate each cell only once per recalculate pass
    if (_state == DIRTY) {
      _state = IN_PROGRESS;
      try {
        _val = _formula.calculate();
        _location.spreadsheet.incrementCalculated();
      } catch (FormulaException fe) {
        _state = ERROR;
        _error = fe;
        throw _error;
      }
      _state = UP_TO_DATE;
    } else if (_state == IN_PROGRESS) {
      throw new CycleException();
    } else if (_state == ERROR) {
      throw _error;
    }
    return _val;
  }

  // Invalidate this cell's reference to another cell.
  CellContent invalidateReferences(CellLocation thisLocation, CellRange range, RowCol offset) {
    Formula newFormula = _formula.invalidateReferences(thisLocation, range, offset);
    if (newFormula != null) {
      return new FormulaContent(_location, newFormula);
    }

    return null;
  }

  // Returns true if this cell is in a dirty or in progress state.
  bool isDirty() => _state != UP_TO_DATE && _state != ERROR;

  bool isFormula() => true;

  CellContent modifyDependenciesForShift(CellRange range, RowCol offset) {
    Formula newFormula = _formula.modifyDependenciesForShift(range, offset);
    CellLocation newLocation = null;
    if (range.isInRange(_location)) {
      newLocation = _location + offset;
    }

    if (newLocation != null || newFormula != null) {
      if (newLocation == null) {
        newLocation = _location;
      }
      if (newFormula == null) {
        newFormula = _formula;
      }
      FormulaContent result = new FormulaContent(newLocation, newFormula);
      return result;
    }

    return null;
  }

  // Set the state of the cell to dirty and clear any previous error.
  void setDirty() {
    _state = DIRTY;
    _error = null;
  }
}

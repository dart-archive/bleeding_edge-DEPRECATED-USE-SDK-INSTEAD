// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A Cell holds all the information for a single spreadsheet cell location.
 *
 * A cell has contents and style, and tracks cells that depend on this cell (dependents).
 */
class Cell {
  CellContent _content;
  Set<CellLocation> _dependents;
  bool _isStyleDirty;
  Style _style;

  CellContent get content() => _content;

  /**
   * Return the Set of (row, col) references to the cells that depend on this cell directly.
   *
   * 'null' indicates the empty set.
   */
  Set<CellLocation> get dependents() => _dependents;

  Style get style() => _style;

  Cell() {
    _style = new Style();
  }

  Cell.content(CellContent this._content) {
    // Default style
    _style = new Style();
  }

  Cell.contentAndStyle(CellContent this._content, Style this._style) {
    if (_style == null) {
      throw new RuntimeException("style == null");
    }
  }

  Cell.style(Style this._style) {
    if (_style == null) {
      throw new RuntimeException("style == null");
    }
    _isStyleDirty = true;
    // Leave content null
  }

  bool operator==(Cell o) {
    if (o is !Cell) {
      return false;
    }
    Cell other = o;
    return _content == other._content && _style == other._style;
  }

  /**
   * Add a single dependent.
   */
  void addDependent(CellLocation location) {
    if (_dependents == null) {
      _dependents = new Set<CellLocation>();
    }
    _dependents.add(location);
  }

  /**
   * Add a set of dependents (cells that rely on this cell for values).
   */
  void addDependents(Set<CellLocation> locations) {
    if (locations == null || locations.isEmpty()) {
      return;
    }
    if (_dependents == null) {
      _dependents = new Set<CellLocation>();
    }
    _dependents.addAll(locations);
  }

  /**
   * Return true if the cell's value should be refreshed on every spreadsheet recalculation.
   */
  bool alwaysRecalculate() => _content == null ? false : _content.alwaysRecalculate();

  /**
   * Clear the set of dependents.
   */
  void clearDependents() {
    _dependents = null;
  }

  /**
   * Clear the dirty style flag.
   *
   * Call this when you have used the style information to update the display and will not need
   * to update it again until the style for the cell changes.
   */
  void clearStyleDirty() {
    _isStyleDirty = false;
  }

  // Return the content as a string
  String getContentString() => _content == null ? "" : _content.getContent();

  /**
   * Return the datatype of this cell, one of [Value.TYPE_BOOLEAN], [Value.TYPE_DOUBLE],
   * [Value.TYPE_DATE_TIME], [Value.TYPE_DOUBLE], [Value.TYPE_STRING], or [Value.TYPE_TIME],
   * or [Value.TYPE_UNKNOWN] if unknown.
   */
  int getDatatype() => _content == null ? Value.TYPE_UNKNOWN : _content.getDatatype();

  /**
   * Return a displayable version of the datatype associated with this cell.
   */
  String getDatatypeAsString() => Value.getDatatypeAsString(getDatatype());

  /**
   * Return the Set of (row, col) references to the cells on which this cell depends directly.
   *
   * 'null' indicates the empty set.
   */
  Set<CellLocation> getDependencies() => _content == null ? null : _content.getDependencies();

  double getDoubleValue() => getValue().asDouble(null);

  // Return the cell contents in 'RC' form, suitable for pasting into another cell.
  String getPasteContent() => _content == null ? "" : _content.getPasteContent();

  String getStringValue() => getValue().asString(null);

  /**
   * Return a displayable HTML version of the Style associated with this cell.
   */
  String getStyleAsHtml() => _style.toHtml();

  /**
   * Return what the user typed to produce the cell.
   */
  String getUserContent() => _content == null ? "" : _content.getContent();

  /**
   * Return the computed value of this cell, or throw a ValueException if there is
   * no way to compute a value.
   */
  Value getValue() {
    if (_content == null) {
      throw new ValueException();
    }
    CellContent cc = _content;
    return cc.getValue();
  }

  /**
   * Invalidate this cell's reference to a range of cells that will be removed.
   *
   * See [CellContents].
   *
   * Return the old content if the content has changed; otherwise null.
   */
  CellContent invalidateReferences(CellLocation thisLocation, CellRange range, RowCol shiftOffset) {
    if (_content == null) {
      return null;
    }

    CellContent originalContent = _content;
    CellContent newContent = _content.invalidateReferences(thisLocation, range, shiftOffset);
    if (newContent != null) {
      _content = newContent;
      return originalContent;
    }

    return null;
  }

  /**
   * Return true if this cell requires recomputation.
   */
  bool isDirty() => _isStyleDirty || (_content == null ? false : _content.isDirty());

  /**
   * A cell with [:null:] content is considered empty for the purposes of the "COUNTA" function.
   */
  bool isEmpty() => _content == null;

  /**
   * Return true if this cell contains a formula.
   */
  bool isFormula() => _content == null ? false : _content.isFormula();

  /**
   * Return true if this cell contains a numeric value.
   */
  bool isNumeric() => _content == null ? false : _content.isNumeric();

  /**
   * Return true if this cell contains a String value.
   */
  bool isString() => _content == null ? false : _content.isString();

  /**
   * Return true if the Style associated with this cell is the default style.
   */
  bool isStyleDefault() => _style.isDefault();

  /**
   * Update formula dependencies for an insertion.
   *
   * See [CellContents].
   *
   * Return the old content if the content has changed; otherwise null.
   */
  CellContent modifyDependenciesForShift(CellRange range, RowCol offset) {
    if (_content == null) {
      return null;
    }

    CellContent originalContent = _content;
    CellContent newContent = _content.modifyDependenciesForShift(range, offset);
    if (newContent != null) {
      _content = newContent;
      return originalContent;
    }

    return null;
  }

  /**
   * Remove a single dependent.
   */
  void removeDependent(CellLocation location) {
    if (_dependents == null) {
      return;
    }
    _dependents.remove(location);
    if (_dependents.isEmpty()) {
      _dependents = null;
    }
  }

  /**
   * Set the content associated with this cell.
   *
   * Returns the former content associated with the cell.
   */
  CellContent setContent(CellContent newContent) {
    CellContent originalContent = _content;
    _content = newContent;
    return originalContent;
  }

  /**
   * Mark this cell as dirty for the purposes of spreadsheet recalculation.
   *
   * For cells that don't perform computation, this has no effect.
   */
  void setContentDirty() {
    if (_content != null) {
      _content.setDirty();
    }
  }

  /**
   * Set the style associated with this cell.
   *
   * Returns the former style associated with the cell.
   */
  Style setStyle(Style newStyle) {
    Style originalStyle = _style;
    if (newStyle == null) {
      _style = new Style();
    } else {
      _style = newStyle;
    }
    _isStyleDirty = true;
    return originalStyle;
  }

  /**
   * Return a formatted HTML version of the content of this cell.
   */
  String toHtml() => _content == null ? "" : _content.toHtml(_style);
}

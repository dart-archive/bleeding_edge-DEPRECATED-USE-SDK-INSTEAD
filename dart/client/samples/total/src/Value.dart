// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A dynamically-typed value that may appear as the result or intermediate value of a spreadsheet
 * computation.
 */
class Value {
  static final int TYPE_UNKNOWN = -1;
  static final int TYPE_BOOLEAN = 1;
  static final int TYPE_CELLREF = 2;
  static final int TYPE_DATE = 3;
  static final int TYPE_DATE_TIME = 4;
  static final int TYPE_DOUBLE = 5;
  static final int TYPE_RANGE = 6;
  static final int TYPE_STRING = 7;
  static final int TYPE_TIME = 8;

  static String getDatatypeAsString(int datatype) {
    switch (datatype) {
    case Value.TYPE_DOUBLE:
      return "double";
    case Value.TYPE_DATE:
      return "date";
    case Value.TYPE_TIME:
      return "time";
    case Value.TYPE_DATE_TIME:
      return "date_time";
    case Value.TYPE_STRING:
      return "string";
    case Value.TYPE_BOOLEAN:
      return "boolean";
    default:
      return "unknown";
    }
  }

  int _datatype;

  /**
   * Returns the datatype of this [Value], one of [TYPE_BOOLEAN], [TYPE_CELLREF], [TYPE_DOUBLE],
   * [TYPE_RANGE], [TYPE_STRING], [TYPE_DATE], [TYPE_TIME], or [TYPE_DATE_TIME].  If the datatype
   * is unknown, [TYPE_UNKNOWN] is returned.
   */
  int get datatype() {
    return _datatype;
  }

  Value(this._datatype) { }

  /**
   * Return the value as a boolean.  For numeric values, 0 equates to [:false:] and all other
   * values equate to [:true:].
   */
  bool asBoolean(CellLocation location) {
    return asDouble(location) != 0.0;
  }

  /**
   * Returns a [CellRefToken] from this [Value].  If this [Value] is not a [RefValue],
   * an exception is thrown.
   */
  CellRefToken asCellRefToken() {
    throw new RuntimeException("Expected CellRefToken");
  }

  /**
   * Returns a double from this [Value].  If this [Value] is a [RefValue], get the computed
   * value from the spreadsheet, interpreting the reference relative to the given
   * (sheet, row, col) [location].  Otherwise, [location] is ignored ([:null:] is an
   * acceptable value).
   */
  abstract double asDouble(CellLocation location);

  RangeToken asRangeToken() {
    throw new RuntimeException("Expected RangeToken");
  }

  /**
   * Returns a [String] from this [Value].  If this [Value] is a [RefValue], get the computed
   * value from the spreadsheet, interpreting the reference relative to the given
   * (sheet, row, col) [location].  Otherwise, [location] is ignored ([:null:] is an
   * acceptable value).
   *
   * For numeric values, this method will throw an exception.
   */
  String asString(CellLocation location) {
    throw new RuntimeException("Expected String");
  }

  /**
   * Returns [:true:] if this [Value] is a string.  If this [Value] is a [RefValue], the reference
   * is evaluated relative to the given (sheet, row, col) [location].
   *
   * The implementation in this superclass returns [:false:].
   */
  bool isString(CellLocation location) {
    return false;
  }

  /**
   * For reference values, the reference is evaluated relative to the given (sheet, row, col)
   * [location]. For non-reference [Value]s, [lookup] returns the value itself.
   */
  Value lookup(CellLocation location) {
    return this;
  }

  /**
   * Returns a String representation of this [Value] for debugging.  The return value is not
   * intended to be part of the application's user-facing UI.
   */
  abstract String toString();
}

/**
 * Superclass for all values other than strings and references.
 */
class NumericValue extends Value {
  double _value;

  double get value() {
    return _value;
  }

  NumericValue(this._value, int datatype) : super(datatype) { }

  double asDouble(CellLocation location) {
    return _value;
  }

  String toString() {
    return "NumericValue[${value}]";
  }
}

class BooleanValue extends NumericValue {

  BooleanValue(bool b) : super(b ? 1.0 : 0.0, TYPE_BOOLEAN) { }

  bool asBoolean(CellLocation location) {
    return value != 0.0;
  }

  String toString() {
    return "BooleanValue[${value == 0.0 ? "FALSE" : "TRUE"}]";
  }
}

class DateValue extends NumericValue {

  DateValue(double date) : super(date, TYPE_DATE) { }

  String toString() {
    return "DateValue[${value}]";
  }
}

class DateTimeValue extends NumericValue {

  DateTimeValue(double dateTime) : super(dateTime, TYPE_DATE_TIME) { }

  String toString() {
    return "DateTimeValue[${value}]";
  }
}

class DoubleValue extends NumericValue {

  DoubleValue(double value) : super(value, TYPE_DOUBLE) { }

  String toString() {
    return "DoubleValue[${value}]";
  }
}

class TimeValue extends NumericValue {

  TimeValue(double time) : super(time, TYPE_TIME) { }

  String toString() {
    return "TimeValue[${value}]";
  }
}

class StringValue extends Value {
  String _s;

  StringValue(this._s) : super(TYPE_STRING) { }

  // Strings are interpreted as 0.0 when used as inputs to a numeric computation
  double asDouble(CellLocation location) {
    return 0.0;
  }

  String asString(CellLocation location) {
    return _s;
  }

  bool isString(CellLocation location) {
    return true;
  }

  String toString() {
    return "StringValue[${_s}]";
  }
}

class RefValue extends Value {

  RefValue(int datatype) : super(datatype) { }

  double asDouble(CellLocation location) {
    return lookup(location).asDouble(null);
  }

  String asString(CellLocation location) {
    return lookup(location).asString(null);
  }

  bool isString(CellLocation location) {
    return lookup(location).isString(null);
  }

  /**
   * Returns the value stored at the location specified by this value's underlying reference
   * (i.e., the reference obtained by calling [asCellRefToken]), relative to a given [CellLocation].
   */
  Value lookup(CellLocation location) {
    if (location == null) {
      throw new RuntimeException("Attempt to retrieve primitive value from ref");
    }
    CellLocation refLocation = asCellRefToken().getCellLocation(location);
    if (!refLocation.isValidCell()) {
      throw new RefException();
    }
    return refLocation.spreadsheet.getValue(refLocation.rowCol);
  }
}

class CellRefTokenValue extends RefValue {
  CellRefToken _crt;

  CellRefTokenValue(this._crt) : super(TYPE_CELLREF) { }

  CellRefToken asCellRefToken() {
    return _crt;
  }

  RangeToken asRangeToken() {
    // Create a range consisting of a single cell
    return new RangeToken(_crt, _crt);
  }

  String toString() {
    return "CellRefTokenValue[${_crt.toDebugString()}]";
  }
}

class RangeTokenValue extends RefValue {
  RangeToken _rt;

  RangeTokenValue(this._rt) : super(TYPE_RANGE) {}

  CellRefToken asCellRefToken() {
    return _rt.startRef;
  }

  RangeToken asRangeToken() {
    return _rt;
  }

  String toString() {
    return "RangeTokenValue[${_rt.toDebugString()}]";
  }
}

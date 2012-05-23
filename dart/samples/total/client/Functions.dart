// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Functions implemented:
//
// ABS ACOS ACOSH AND ASIN ASINH ATAN ATAN2 ATANH COLUMNS COMBIN CONCATENATE COS COSH COUNTA DATE
// DAY DEGREES EVEN EXP FACT FACTDOUBLE FALSE GCD HLOOKUP HOUR IF INT LARGE LCM LEN LN LOG
// LOG10 MATCH MAX MID MIN MINUTE MOD MONTH MULTINOMIAL NOT NOW ODD OFFSET OR PI POWER
// PRODUCT QUOTIENT RADIANS RAND RANDBETWEEN RIGHT ROUND SECOND SERIESSUM SIGN
// SIN SINH SMALL SQRT SQRTPI SUM SUMSQ TAN TANH TIME TODAY TRUE TRUNC VALUE VLOOKUP YEAR

// Some functions to be implemented:
// Math: CEILING, FLOOR, MROUND, ROUNDDOWN, ROUNDUP, SUBTOTAL, SUMIF
// Date: DATEVALUE, DAYS360, EDATE, EMONTH, EOMONTH, NETWORKDAYS, TIMEVALUE,
//       WEEKDAY, WEEKNUM, WORKDAY, YEARFRAC
// Lists: SUMPRODUCT, SUMX2MY2, SUMX2PY2, SUMXMY2
// Matrix: MDETERM, MINVERSE, MMULT
// String: ROMAN

typedef void NumericFunction(CellLocation location, List<Value> stack, int top, int nargs);
typedef double AggregateFunction(double accum, double value);

// A built-in function, encapsulating the function name, number of arguments, output datatype,
class SpreadsheetFunction {
  NumericFunction _func;
  String _name;
  int _nargs;

  // void func(List<Value> stack, int top, int nargs)
  // args are in stack[top], stack[top + 1], ..., stack[top + nargs - 1]
  // result should be placed in stack[top]
  NumericFunction get func() => _func;

  String get name() => _name;

  int get nargs() => _nargs;

  SpreadsheetFunction(this._name, this._nargs, this._func) { }
}

class Functions {
  static final double DEGREES_TO_RADIANS = Math.PI / 180.0;
  static final double RADIANS_TO_DEGREES = 180.0 / Math.PI;

  // FIXME: replace getter below with static const field.
  static double get LOG_10_FACTOR() { return 1.0 / Math.log(10.0); }

  static double _choose(double n, double k) {
    double r = 1.0;
    for (int d = 1; d <= k; d++) {
      // Avoid intermediate overflow
      double g = _gcd(r, d.toDouble());
      var div;
      if (g > 1.0) {
        // FIXME: replace with /= when supported by VM.
        r = r / g;
        div = d / g;
      } else {
        div = d;
      }
      r *= n--;
      // FIXME: replace with /= when supported by VM.
      r = r / div;
    }
    return r;
  }

  static int _countCellsInRange(CellRange range, int countCell(Cell cell)) {
    int count = 0;
    range.forEach(_(CellLocation cl) {
      RowCol rowCol = cl.rowCol;
      // Values outside the spreadsheet are ignored, empty cells are treated as 0.0
      if (rowCol.row >= 1 && rowCol.col >= 1) {
        Cell cell = range.spreadsheet.getCell(rowCol);
        if (cell != null) {
          count += countCell(cell);
        }
      }
    });

    return count;
  }

  static double _exp(double x) => Math.pow(Math.E, x);

  // Assume arguments are positive
  // FIXME: GCD should not work on doubles.
  static double _gcd(double a, double b) {
    if (b == 0) {
      return a;
    }
    return _gcd(b, a % b);
  }

  static List<double> _getValues(CellRange range) {
    int count = range.rows * range.columns;
    List<double> values = new List<double>();
    range.forEach(_(CellLocation cl) {
      RowCol rowCol = cl.rowCol;
      // Values outside the spreadsheet are ignored, empty cells are treated as 0.0
      if (rowCol.row >= 1 && rowCol.col >= 1) {
        Cell cell = range.spreadsheet.getCell(rowCol);
        if (cell != null) {
          double value = cell.getDoubleValue();
          values.add(value);
        } else {
          values.add(0.0);
        }
      }
    });

    return values;
  }

  static double _lcm(double a, double b) => (a * b) / _gcd(a, b);

  // Function name ==> double func(double accum, double newValue)
  Map<String, AggregateFunction> _aggregateFunctions;
  // Initial value for aggregation (e.g., 0 for sums, 1 for products)
  Map<String, double> _aggregateInitialValues;
  // Functions that should be evaluated for every recalculation
  Set<String> _alwaysRecalculateFunctions;
  // Functions by name
  Map<String, SpreadsheetFunction> _functions;

  Functions() {
    _functions = new Map<String, SpreadsheetFunction>();
    _alwaysRecalculateFunctions = new Set<String>();

    // Nullary (zero-arg) functions
    _newFunc("FALSE", 0, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        stack[top] = new BooleanValue(false);
      });
    _newFunc("NOW", 0, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        stack[top] = new DateTimeValue(DateUtils.now());
      });
    _alwaysRecalculateFunctions.add("NOW");
    _newFunc("PI", 0, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        stack[top] = new DoubleValue(Math.PI);
      });
    _newFunc("RAND", 0, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        stack[top] = new DoubleValue(Math.random());
      });
    _alwaysRecalculateFunctions.add("RAND");
    _newFunc("TODAY", 0, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        stack[top] = new DateValue(DateUtils.today());
      });
    _alwaysRecalculateFunctions.add("TODAY");
    _newFunc("TRUE", 0, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        stack[top] = new BooleanValue(true);
      });

    // Unary functions
    _newFunc("ABS", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        double val;
        if (x < 0.0) {
          val = -x;
        } else {
          val = x;
        }
        stack[top] = new DoubleValue(val);
      });
    _newFunc("ACOS", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.acos(x));
      });
    _newFunc("ACOSH", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(2.0 * Math.log(Math.sqrt((x + 1.0) / 2.0)
            + Math.sqrt((x - 1.0) / 2.0)));
      });
    _newFunc("ASIN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.asin(x));
      });
    _newFunc("ASINH", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.log(x + Math.sqrt(1.0 + x * x)));
      });
    _newFunc("ATAN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.atan(x));
      });
    _newFunc("ATANH", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue((Math.log(1.0 + x) - Math.log(1.0 - x)) / 2.0);
      });
    _newFunc("COLUMNS", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {

        RangeToken range = stack[top].asRangeToken();
        int cols = range.endRef.getCol(location) - range.startRef.getCol(location) + 1;
        stack[top] = new DoubleValue(cols.toDouble());
      });
    _newFunc("COS", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.cos(x));
      });
    _newFunc("COSH", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue((_exp(x) + _exp(-x)) * 0.5);
      });
    _newFunc("DAY", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        int days = x.floor().toInt();
        Duration t = new Duration(days: days);
        Date dateTime = DateUtils.EPOCH.add(t);
        stack[top] = new DoubleValue(dateTime.day.toDouble());
      });
    _newFunc("DEGREES", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(x * RADIANS_TO_DEGREES);
      });
    _newFunc("EVEN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        bool neg = false;
        if (x < 0) {
          x = -x;
          neg = true;
        }
        double val = x.ceil();
        if (val % 2 == 1) {
          val++;
        }
        stack[top] = new DoubleValue(neg ? -val : val);
      });
    _newFunc("EXP", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.pow(Math.E, x));
      });
    _newFunc("FACT", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        if (x < 0.0) {
          throw new NumberException();
        }
        double ix = x.floor();
        double fact = 1.0;
        for (int i = 2; i <= ix; i++) {
          fact *= i;
        }
        stack[top] = new DoubleValue(fact);
      });
    _newFunc("FACTDOUBLE", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        if (x < 0.0) {
          throw new NumberException();
        }
        int ix = x.floor().toInt();
        int start = (ix % 2) + 2;
        double fact = 1.0;
        for (int i = start; i <= ix; i += 2) {
          fact *= i;
        }
        stack[top] = new DoubleValue(fact);
      });
    _newFunc("HOUR", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double time = stack[top].asDouble(location);
        time -= time.floor();
        stack[top] = new DoubleValue((time * 24.0).floor());
      });
    _newFunc("INT", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(x.floor());
      });
    _newFunc("LEN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
      String s = stack[top].asString(location);
      stack[top] = new DoubleValue(s.length.toDouble());
    });
    _newFunc("LN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        if (x <= 0.0) {
          throw new NumberException();
        }
        stack[top] = new DoubleValue(Math.log(x));
      });
    _newFunc("LOG10", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        if (x <= 0.0) {
          throw new NumberException();
        }
        stack[top] = new DoubleValue(Math.log(x) * LOG_10_FACTOR);
      });
    _newFunc("MINUTE", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double time = stack[top].asDouble(location);
        time *= 24.0;
        time -= time.floor();
        stack[top] = new DoubleValue((time * 60.0).floor());
      });
    _newFunc("MONTH", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        int days = x.floor().toInt();
        Duration t = new Duration(days: days);
        Date dateTime = DateUtils.EPOCH.add(t);
        stack[top] = new DoubleValue(dateTime.month.toDouble());
      });
    _newFunc("NOT", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new BooleanValue(x == 0.0);
      });
    _newFunc("ODD", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        bool neg = false;
        if (x < 0) {
          x = -x;
          neg = true;
        }
        double val = x.ceil();
        if (val % 2 == 0) {
          val++;
        }
        stack[top] = new DoubleValue(neg ? -val : val);
      });
    _newFunc("RADIANS", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(x * DEGREES_TO_RADIANS);
      });
    _newFunc("ROWS", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        RangeToken range = stack[top].asRangeToken();
        int rows = range.endRef.getRow(location) - range.startRef.getRow(location) + 1;
        stack[top] = new DoubleValue(rows.toDouble());
      });
    _newFunc("SECOND", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double time = stack[top].asDouble(location);
        time *= 24.0 * 60.0;
        time -= time.floor();
        stack[top] = new DoubleValue((time * 60.0).floor());
      });
    _newFunc("SIGN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        double val;
        if (x < 0.0) {
          val = -1.0;
        } else if (x > 0.0) {
          val = 1.0;
        } else {
          val = 0.0;
        }
        stack[top] = new DoubleValue(val);
      });
    _newFunc("SIN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.sin(x));
      });
    _newFunc("SINH", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue((_exp(x) - _exp(-x)) * 0.5);
      });
    _newFunc("SQRT", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        if (x < 0.0) {
          throw new NumberException();
        }
        stack[top] = new DoubleValue(Math.sqrt(x));
      });
    _newFunc("SQRTPI", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        if (x < 0.0) {
          throw new NumberException();
        }
        stack[top] = new DoubleValue(Math.sqrt(x * Math.PI));
      });
    _newFunc("TAN", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        stack[top] = new DoubleValue(Math.tan(x));
      });
    _newFunc("TANH", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        x = _exp(2.0 * x);
        stack[top] = new DoubleValue((x - 1.0) / (x + 1.0));
      });
    _newFunc("VALUE", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
      Value v = stack[top];
      if (v.isString(location)) {
        String s = stack[top].asString(location);
        try {
          double x = Math.parseDouble(s);
          stack[top] = new DoubleValue(x);
        } catch (BadNumberFormatException e) {
          throw new ValueException();
        }
      } else {
        stack[top] = new DoubleValue(v.asDouble(location));
      }
    });
    _newFunc("YEAR", 1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        int days = x.floor().toInt();
        Duration t = new Duration(days: days);
        Date dateTime = DateUtils.EPOCH.add(t);
        stack[top] = new DoubleValue(dateTime.year.toDouble());
      });

    // Binary functions
    _newFunc("ATAN2", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        double y = stack[top + 1].asDouble(location);
        stack[top] = new DoubleValue(Math.atan2(x, y));
      });
    _newFunc("COMBIN", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double n = stack[top].asDouble(location).floor();
        double k = stack[top + 1].asDouble(location).floor();
        if (n < 0 || k < 0 || n < k) {
          throw new NumberException();
        }
        stack[top] = new DoubleValue(_choose(n, k));
      });
    _newFunc("LARGE", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        RangeToken rt = stack[top].asRangeToken();
        CellRange range = rt.getCellRange(location);
        int count = range.rows * range.columns;

        List<double> values = _getValues(range);
        int k = stack[top + 1].asDouble(location).floor().toInt();
        if (k < 1 || k > values.length) {
          throw new NumberException();
        }

        stack[top] = new DoubleValue(_find(values, values.length - k));
      });
    _newFunc("LOG", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double value = stack[top].asDouble(location);
        double base = stack[top + 1].asDouble(location);
        if (value <= 0.0 || base <= 0.0) {
          throw new NumberException();
        }
        stack[top] = new DoubleValue(Math.log(value) / Math.log(base));
      });
    _newFunc("MOD", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double number = stack[top].asDouble(location);
        double divisor = stack[top + 1].asDouble(location);
        if (divisor == 0.0) {
          throw new DivByZeroException();
        }
        stack[top] = new DoubleValue(number - divisor * (number / divisor).floor());
      });
    _newFunc("POWER", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double base = stack[top].asDouble(location);
        double exponent = stack[top + 1].asDouble(location);
        stack[top] = new DoubleValue(Math.pow(base, exponent));
      });
    _newFunc("QUOTIENT", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double numerator = stack[top].asDouble(location);
        double denominator = stack[top + 1].asDouble(location);
        if (denominator < 0.0) {
          numerator = -numerator;
          denominator = -denominator;
        }
        double val;
        if (numerator < 0.0) {
          val = -(((-numerator) / denominator).floor());
        } else {
          val = (numerator / denominator).floor();
        }
        stack[top] = new DoubleValue(val);
      });
    _newFunc("RANDBETWEEN", 2,
      void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double b = stack[top].asDouble(location).floor();
        double t = stack[top + 1].asDouble(location).floor();
        if (b > t) {
          throw new NumberException();
        }
        stack[top] = new DoubleValue(b + (Math.random() * (t - b + 1)).floor());
      });
    _alwaysRecalculateFunctions.add("RANDBETWEEN");
    _newFunc("REPT", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        String text = stack[top].asString(location);
        double number = stack[top + 1].asDouble(location);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < number; i++) {
          sb.add(text);
        }
        stack[top] = new StringValue(sb.toString());
      });
    _newFunc("ROUND", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double value = stack[top].asDouble(location);
        double precision = stack[top + 1].asDouble(location);
        if (precision < 0.0) {
          throw new NumberException();
        }
        precision = precision.floor();
        double prec = Math.pow(10.0, precision);
        stack[top] = new DoubleValue((value * prec).round() / prec);
      });
    _newFunc("SMALL", 2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        RangeToken rt = stack[top].asRangeToken();
        CellRange range = rt.getCellRange(location);
        int count = range.rows * range.columns;

        List<double> values = _getValues(range);
        int k = stack[top + 1].asDouble(location).floor().toInt();
        if (k <= 0 || k > values.length) {
          throw new NumberException();
        }

        stack[top] = new DoubleValue(_find(values, k - 1));
      });
    _newFunc("LEFT", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        String s = stack[top].asString(location);
        int chars = 1;
        if (nargs == 2) {
          chars = stack[top + 1].asDouble(location).floor().toInt();
        }
        stack[top] = new StringValue(s.substring(0, Math.min(chars, s.length)));
      });
    _newFunc("RIGHT", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        String s = stack[top].asString(location);
        int chars = 1;
        if (nargs == 2) {
          chars = stack[top + 1].asDouble(location).floor().toInt();
        }
        stack[top] = new StringValue(s.substring(Math.max(s.length - chars, 0), s.length));
      });
    _newFunc("TRUNC", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double number = stack[top].asDouble(location);
        bool neg = false;
        if (number < 0.0) {
          neg = true;
          number = -number;
        }
        double val;
        if (nargs == 1) {
          val = number.floor();
        } else if (nargs == 2) {
          double prec = Math.pow(10.0, stack[top + 1].asDouble(location));
          val = (number * prec).floor() / prec;
        } else {
          throw new NumArgsException();
        }
        stack[top] = new DoubleValue(neg ? -val : val);
      });
    _newFunc("DATE", 3, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        int year = stack[top].asDouble(location).floor().toInt();
        int month = stack[top + 1].asDouble(location).floor().toInt();
        int day = stack[top + 2].asDouble(location).floor().toInt();
        stack[top] = new DateValue(DateUtils.getDate(year, month, day));
      });
    _newFunc("MID", 3, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        String s = stack[top].asString(location);
        int start = stack[top + 1].asDouble(location).floor().toInt();
        int chars = stack[top + 2].asDouble(location).floor().toInt();

        if (start >= s.length) {
          stack[top] = new StringValue("");
          return;
        }
        if (start < 1) {
          throw new NumberException();
        }
        if (chars < 0) {
          throw new NumberException();
        }
        stack[top] = new StringValue(s.substring(start - 1, Math.min(start + chars - 1, s.length)));
      });
    _newFunc("TIME", 3, void _(CellLocation location, List<Value> stack, int top, int nargs) {
      int hour = stack[top].asDouble(location).floor().toInt();
      int minute = stack[top + 1].asDouble(location).floor().toInt();
      int second = stack[top + 2].asDouble(location).floor().toInt();
      stack[top] = new TimeValue(DateUtils.getTime(hour, minute, second));
    });
    _newFunc("OFFSET", 5, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        RangeToken rt = stack[top].asRangeToken();
        int rows = stack[top + 1].asDouble(location).floor().toInt();
        int columns = stack[top + 2].asDouble(location).floor().toInt();
        int height = stack[top + 3].asDouble(location).floor().toInt();
        int width = stack[top + 4].asDouble(location).floor().toInt();

        CellRefToken startRef = new CellRefToken.offset(rt.startRef, columns, rows);
        CellRefToken endRef = new CellRefToken.offset(rt.startRef,
            columns + width - 1, rows + height - 1);

        RangeTokenValue result = new RangeTokenValue(new RangeToken(startRef, endRef));
        stack[top] = result;
      });
    // Always recalculate the dependents of an OFFSET since the OFFSET may produce an
    // otherwise unknown dependency
    _alwaysRecalculateFunctions.add("OFFSET");

    // N-ary functions
    _newFunc("AND", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        for (int i = 0; i < nargs; i++) {
          if (stack[top + i].asDouble(location) == 0.0) {
            stack[top] = new BooleanValue(false);
            return;
          }
        }
        stack[top] = new BooleanValue(true);
      });
    _newFunc("CONCATENATE", -2,
      void _(CellLocation location, List<Value> stack, int top, int nargs) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nargs; i++) {
          sb.add(stack[top + i].asString(location));
        }
        stack[top] = new StringValue(sb.toString());
      });
    _newFunc("COUNT", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        int count = 0;
        for (int i = 0; i < nargs; i++) {
          Value value = stack[top + i];
          if (value.datatype == Value.TYPE_CELLREF || value.datatype == Value.TYPE_RANGE) {
            count += _countCellsInRange(value.asRangeToken().getCellRange(location),
              int __(Cell cell) { return cell.isNumeric() ? 1 : 0; });
          } else if (value.datatype == Value.TYPE_DOUBLE) {
            count++;
          }
        }
        stack[top] = new DoubleValue(count.toDouble());
      });
    _newFunc("COUNTA", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        int count = 0;
        for (int i = 0; i < nargs; i++) {
          Value value = stack[top + i];
          if (value.datatype == Value.TYPE_CELLREF || value.datatype == Value.TYPE_RANGE) {
            count += _countCellsInRange(value.asRangeToken().getCellRange(location),
              int __(Cell cell) { return cell.isEmpty() ? 0 : 1; });
          } else {
            count++;
          }
        }
        stack[top] = new DoubleValue(count.toDouble());
      });
    _newFunc("GCD", -2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        for (int i = 0; i < nargs; i++) {
          if (stack[top + i].asDouble(location) < 0.0) {
            throw new NumberException();
          }
        }
        double gcd = _gcd(stack[top].asDouble(location), stack[top + 1].asDouble(location));
        for (int i = 2; i < nargs; i++) {
          gcd = _gcd(gcd, stack[top + i].asDouble(location));
        }
        stack[top] = new DoubleValue(gcd);
      });
    // Inherit datatype from stack[top + 1]
    _newFunc("IF", -2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double expr = stack[top].asDouble(location);
        if (expr == 1.0) {
          stack[top] = stack[top + 1];
        } else {
          if (nargs > 2) {
            stack[top] = stack[top + 2];
          } else {
            stack[top] = new BooleanValue(false); // IF(1>0, onearg) returns FALSE
          }
        }
      });
    _newFunc("LCM", -2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        for (int i = 0; i < nargs; i++) {
          if (stack[top + i].asDouble(location) < 0.0) {
            throw new NumberException();
          }
        }
        double lcm = _lcm(stack[top].asDouble(location), stack[top + 1].asDouble(location));
        for (int i = 2; i < nargs; i++) {
          lcm = _lcm(lcm, stack[top + i].asDouble(location));
        }
        stack[top] = new DoubleValue(lcm);
      });
    _newFunc("MAX", -1,
        void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double max = stack[top].asDouble(location);
        for (int i = 1; i < nargs; i++) {
          double x = stack[top + 1].asDouble(location);
          if (x > max) {
            max = x;
          }
        }
        stack[top] = new DoubleValue(max);
       });
    _newFunc("MIN", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
       double min = stack[top].asDouble(location);
       for (int i = 1; i < nargs; i++) {
         double x = stack[top + 1].asDouble(location);
         if (x < min) {
           min = x;
         }
       }
       stack[top] = new DoubleValue(min);
      });
    _newFunc("MULTINOMIAL", -1,
      void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double sumN = stack[top].asDouble(location) + stack[top + 1].asDouble(location);
        double sumK = stack[top].asDouble(location);
        double product = 1.0;
        for (int i = 1; i < nargs; i++) {
          product *= _choose(sumN, sumK);
          sumN += stack[top + i + 1].asDouble(location);
          sumK += stack[top + i].asDouble(location);
        }
        stack[top] = new DoubleValue(product);
      });
    _newFunc("OR", -1, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        for (int i = 0; i < nargs; i++) {
          if (stack[top + i].asDouble(location) == 1.0) {
            stack[top] = new BooleanValue(true);
            return;
          }
        }
        stack[top] = new BooleanValue(false);
      });
    _newFunc("SERIESSUM", -3, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double x = stack[top].asDouble(location);
        double n = stack[top + 1].asDouble(location);
        double m = stack[top + 2].asDouble(location);
        double xpow = Math.pow(x, n);
        double xstep = Math.pow(x, m);
        double sum = 0.0;
        for (int i = 3; i < nargs; i++) {
          sum += xpow * stack[top + i].asDouble(location);
          xpow *= xstep;
        }
        stack[top] = new DoubleValue(sum);
      });

    _newFunc("MATCH", -2, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        double value = stack[top].asDouble(location);
        RangeToken rt = stack[top + 1].asRangeToken();
        CellRange range = rt.getCellRange(location);

        int matchType = 1;
        if (nargs > 2) {
          matchType = stack[top + 2].asDouble(location).floor().toInt();
        }

        // The range must be 1xN or Nx1
        if (range.rows > 1 && range.columns > 1) {
          throw new NumberException();
        }

        int index = 1;
        double bestValue = matchType == 1 ? -1.0e100 : 1.0e100;
        int bestIndex = -1;
        range.forEach(__(CellLocation loc) {
          Cell cell = loc.getCell();
          if (cell != null) {
            double d = cell.getDoubleValue();
            if (d == value) {
              bestIndex = index;
              return;
            } else if (matchType == 1 && d < value && d > bestValue) {
              bestValue = d;
              bestIndex = index;
            } else if (matchType == -1 && d > value && d < bestValue) {
              bestValue = d;
              bestIndex = index;
            }
          }
          index++;
        });
        if (bestIndex == -1) {
          throw new NumberException();
        }

        stack[top] = new DoubleValue(bestIndex.toDouble());
      });

    _newFunc("HLOOKUP", -3, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        _lookup(location, stack, top, nargs, true);
      });
    _newFunc("VLOOKUP", -3, void _(CellLocation location, List<Value> stack, int top, int nargs) {
        _lookup(location, stack, top, nargs, false);
      });

    // Functions that operate over ranges
    _aggregateFunctions = new Map<String, AggregateFunction>();
    _aggregateInitialValues = new Map<String, double>();
    _aggregateFunctions["SUM"] = double _(double accum, double value) => accum + value;
    _aggregateInitialValues["SUM"] = 0.0;
    _aggregateFunctions["SUMSQ"] = double _(double accum, double value) => accum + value * value;
    _aggregateInitialValues["SUMSQ"] = 0.0;
    _aggregateFunctions["PRODUCT"] = double _(double accum, double value) => accum * value;
    _aggregateInitialValues["PRODUCT"] = 1.0;
  }

  bool alwaysRecalculate(String functionName) => _alwaysRecalculateFunctions.contains(functionName);

  // TODO:  implement a visitor pattern in order to reduce redundant code in datatype() and
  // evaluate()

  // Evaluate a function at a given position within a spreadsheet,
  Value evaluate(CellLocation location, List<Token> tokens) {
    List<Value> stack = new List<Value>();
    int top = 0;

    tokens.forEach((Token t) {
      if (t is NumberToken) {
        NumberToken nt = t;
        _growValues(stack, top);
        stack[top++] = new DoubleValue(nt.value);
      } else if (t is StringToken) {
        StringToken st = t;
        _growValues(stack, top);
        stack[top++] = new StringValue(st.value);
      } else if (t is CellRefToken) {
        CellRefToken crt = t;
        CellLocation refLocation = crt.getCellLocation(location);
        if (!refLocation.isValidCell()) {
          throw new RefException();
        }
        _growValues(stack, top);
        stack[top++] = new CellRefTokenValue(crt);
      } else if (t is RangeToken) {
        RangeToken rt = t;
        _growValues(stack, top);
        stack[top++] = new RangeTokenValue(rt);
      } else if (t is OperatorToken) {
        OperatorToken ot = t;
        Value right = stack[--top];
        Value left = stack[--top];
        left = left.lookup(location);
        right = right.lookup(location);
        Value result = ot.operate(left, right);
        stack[top++] = result;
      } else if (t is FunctionNameToken) {
        FunctionNameToken fnt = t;
        String name = fnt.name;
        double nargs = stack[--top].asDouble(location);

        AggregateFunction aggregate = _aggregateFunctions[name];
        if (aggregate != null) {
          if (nargs != 1) {
            throw new NumArgsException();
          }
          double initialValue = _aggregateInitialValues[name];
          RangeToken range = stack[--top].asRangeToken();
          double value = _aggregate(range, location, initialValue, aggregate);
          stack[top++] = new DoubleValue(value);
        } else {
          SpreadsheetFunction f = _functions[name];
          if (f == null) {
            throw new FunctionException();
          }
          int argsNeeded = f.nargs; // -x means at least x varargs
          if (argsNeeded >= 0 && argsNeeded != nargs) {
            throw new NumArgsException();
          } else if (argsNeeded < 0 && nargs < -argsNeeded) {
            throw new NumArgsException();
          }
          top -= nargs.toInt();
          Function func = f.func;
            func(location, stack, top++, nargs.toInt());
        }
      } else if (t is InvalidCellRefToken) {
        throw new RefException();
      }
    });

    Value result = stack[0].lookup(location);
    return result;
  }

  // accum = initialValue; for each cell C in range: accum = f(accum, value of C)
  double _aggregate(RangeToken range, CellLocation location, double initialValue,
      AggregateFunction f) {
    CellRefToken startRef = range.startRef;
    CellRefToken endRef = range.endRef;

    int startRow = startRef.getRow(location);
    int endRow = endRef.getRow(location);
    int startCol = startRef.getCol(location);
    int endCol = endRef.getCol(location);

    int minRow = startRow < endRow ? startRow : endRow;
    int maxRow = startRow > endRow ? startRow : endRow;
    int minCol = startCol < endCol ? startCol : endCol;
    int maxCol = startCol > endCol ? startCol : endCol;
    if (minRow < 1 || minCol < 1) {
      throw new RefException();
    }

    double accum = initialValue;
    for (int row = minRow; row <= maxRow; row++) {
      for (int col = minCol; col <= maxCol; col++) {
        accum = f(accum, range.spreadsheet.getValue(new RowCol(row, col)).asDouble(null));
      }
    }

    return accum;
  }

  // Returns the kth smallest value in the input list
  // Based on C. A. R. Hoare, CACM 14 (1971) pp. 39-45
  double _find(List<double> values, int k) {
    int m = 0;
    int n = values.length - 1;
    while (m < n) {
      double r = values[k];
      int i = m;
      int j = n;
      while (i <= j) {
        while (values[i] < r) {
          i++;
        }
        while (r < values[j]) {
          j--;
        }
        if (i <= j) {
          double w = values[i];
          values[i] = values[j];
          values[j] = w;
          i++;
          j--;
        }
      }
      if (k <= j) {
        n = j;
      } else if (i <= k) {
        m = i;
      } else {
        break;
      }
    }

    return values[k];
  }

  void _growInt(List<int> array, int top) {
    while (array.length < top + 1) {
      array.add(0);
    }
  }

  void _growValues(List<Value> list, int top) {
    while (list.length < top + 1) {
      list.add(null);
    }
  }

  bool _isDate(int datatype) => datatype == Value.TYPE_DATE || datatype == Value.TYPE_TIME ||
        datatype == Value.TYPE_DATE_TIME;

  // TODO: bounds checking
  void _lookup(CellLocation location, List<Value> stack, int top, int nargs, bool horizontal) {
    double value = stack[top].asDouble(location);
    RangeToken rt = stack[top + 1].asRangeToken();
    CellRange range = rt.getCellRange(location);

    int index = stack[top + 2].asDouble(location).floor().toInt();
    if (index < 1 || index > (horizontal ? range.rows : range.columns)) {
      throw new NumberException();
    }
    bool exact = false;
    if (nargs > 3) {
      exact = stack[top + 3].asDouble(location) == 0.0;
    }

    int min = horizontal ? rt.startRef.getCol(location) : rt.startRef.getRow(location);
    int max = horizontal ? rt.endRef.getCol(location) : rt.endRef.getRow(location);
    int rowCol = horizontal ? rt.startRef.getRow(location) : rt.startRef.getCol(location);
    int outRowCol = rowCol + index - 1;

    double bestVal = -1.0e100;
    int bestIndex;
    for (int x = min; x <= max; x++) {
      Cell cell = range.spreadsheet.getCell(horizontal ?
          new RowCol(rowCol, x) : new RowCol(x, rowCol));
      double val = cell == null ? 0.0 : cell.getDoubleValue();
      if (val == value) {
        Cell resultCell = range.spreadsheet.getCell(horizontal ?
            new RowCol(outRowCol, x) : new RowCol(x, outRowCol));
        stack[top] = new DoubleValue(resultCell == null ? 0.0 : resultCell.getDoubleValue());
        return;
      }
      if (!exact && val < value && value > bestVal) {
        bestVal = val;
        bestIndex = x;
      }
    }
    if (exact) {
      throw new NumberException(); // TODO: should display "#N/A"
    } else {
      Cell resultCell = range.spreadsheet.getCell(horizontal ?
          new RowCol(outRowCol, bestIndex) : new RowCol(bestIndex, outRowCol));
      stack[top] = new DoubleValue(resultCell == null ? 0.0 : resultCell.getDoubleValue());
    }
  }

  void _newFunc(String name, int nargs, NumericFunction f) {
    _functions[name] = new SpreadsheetFunction(name, nargs, f);
  }
}

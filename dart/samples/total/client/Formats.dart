// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SplitDecimal {
  String fracPart;
  String intPart;
  bool isNegative;

  String get lparen() => isNegative ? "(" : "";

  String get rparen() => isNegative ? ")" : "";

  String get sign() => isNegative ? "-" : "";

  SplitDecimal(this.intPart, this.fracPart, this.isNegative) { }
}

class Formats {

  static final int FORMAT_DEFAULT = 0;
  static final int FORMAT_DOUBLE = 1;
  static final int FORMAT_ROUNDED = 2;
  static final int FORMAT_ROUNDED_2 = 3;
  static final int FORMAT_CURRENCY_ROUNDED = 4;
  static final int FORMAT_CURRENCY_ROUNDED_2 = 5;
  static final int FORMAT_PARENS_ROUNDED = 6;
  static final int FORMAT_PARENS_ROUNDED_2 = 7;
  static final int FORMAT_PERCENT_ROUNDED = 8;
  static final int FORMAT_PERCENT_ROUNDED_2 = 9;
  static final int FORMAT_DATE = 10;
  static final int FORMAT_DATE_WORDS = 11;
  static final int FORMAT_TIME = 12;
  static final int FORMAT_DATE_TIME = 13;
  static final int FORMAT_HOURS = 14;
  static final int FORMAT_BOOLEAN = 15;

  static NumericFormat get DEFAULT_NUMERIC_FORMAT() => defaultNumericFormat;
  
  static String defaultNumericFormat(double x) {
    if (x == x.floor()) {
      return x.toInt().toString();
    } else {
      return x.toString();
    }
  }

  static TextFormat get DEFAULT_TEXT_FORMAT() => String _(String x) => x;

  /**
   * Color names corresponding to [htmlColors].
   */
  static final List<String> _colorDescriptions = const <String>[
    "--",
    "White", "Black", "Red", "Green", "Blue",
    "15% Grey", "30% Grey", "45% Gray", "60% Gray", "75% Gray", "85% Gray",
    "Orange", "Lime Green", "Forest Green", "Aqua", "Sky Blue", "Purple",
    "Magenta", "Amber", "Bright Yellow", "Bright Green", "Cyan", "Light Blue",
    "Dark Rose", "Light Rose", "Pale Orange", "Light Yellow", "Pale Cyan", "Bluish Gray",
    "Pinkish Gray", "Flamingo Pink", "Peach", "Amber Yellow","Cyanish Gray", "Bluish Gray",
    "Light Azure", "Light Violet", "Vivid Orange", "Olive", "Strong Green", "Strong Cyan",
    "Moderate Blue", "Strong Red", "Strong Orange Red", "Deep Olive", "Deep Green", "Deep Azure"
  ];

  /**
   * Text and background colors in HTML form.  White is in position 1 (= Style.WHITE).
   */
  static final List<String> _htmlColors = const <String>[
    "#FFFFFF", "#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF",
    "#242424", "#484848", "#737373", "#919191", "#B6B6B6", "#DADADA",
    "#FF9900", "#99CC00", "#339966", "#33CCCC", "#3366FF", "#800080",
    "#FF00FF", "#FFCC00", "#FFFF00", "#00FF00", "#00FFFF", "#00CCFF",
    "#993366", "#FF99CC", "#FADCB3", "#FFFF99", "#CCFFFF", "#C2D1F0",
    "#E1C7E1", "#E69999", "#FFCC99", "#EBD780", "#B3D580", "#BDE6E1",
    "#99CCFF", "#CC99FF", "#FF6600", "#808000", "#008000", "#008080",
    "#6666CC", "#800000", "#993300", "#333300", "#003300", "#003366",
    "#000080", "#333399"
  ];

  static Formats _instance; // singleton

  /**
   * Names of months, with January == 0.
   */
  static final List<String> _monthNames = const <String>[
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];

  static final List<String> _textAlignmentDescriptions =
      const <String>["--", "Left", "Center", "Right"];

  // TODO: don't expose internal array
  static List<String> get htmlColors() => _htmlColors;

  static int get numColorDescriptions() => _colorDescriptions.length;

  static int get numTextAlignmentDescriptions() => _textAlignmentDescriptions.length;

  static String addCommas(String sx, int groupLength) {
    String output = "";
    while (sx.length > groupLength) {
      String group = sx.substring(sx.length - groupLength, sx.length);
      output = ",${group}${output}";
      sx = sx.substring(0, sx.length - groupLength);
    }
    return "${sx}${output}";
  }

  static String getColorDescription(int index) => _colorDescriptions[index];

  static String getHtmlColor(int colorIndex) {
    assert(colorIndex >= 0 && colorIndex < _htmlColors.length);
    return _htmlColors[colorIndex];
  }

  static String getTextAlignmentDescription(int index) => _textAlignmentDescriptions[index];

  static SplitDecimal split(double x, int decimalPlaces) {
    bool isNegative = false;
    if (x < 0.0) {
      x = -x;
      isNegative = true;
    }
    double power = Math.pow(10.0, decimalPlaces);
    x = ((x + (0.5 / power)) * power).floor();
    String xs = x.toInt().toString();
    while (xs.length <= decimalPlaces) {
      xs = "0${xs}";
    }
    String intPart = xs.substring(0, xs.length - decimalPlaces);
    String fracPart = xs.substring(xs.length - decimalPlaces, xs.length);
    return new SplitDecimal(intPart, fracPart, isNegative);
  }

  List<String> _numericFormatDescriptions;
  List<NumericFormat> _numericFormats;
  List<String> _textFormatDescriptions;

  int get numNumericFormats() => _numericFormats.length;

  int get numTextFormats() => _textFormatDescriptions.length;

  // TODO: don't expose internal array
  List<String> get numericFormatDescriptions() => _numericFormatDescriptions;

  factory Formats() {
    if (_instance == null) {
      _instance = new Formats._internal();
    }
    return _instance;
  }

  Formats._internal() {
    _numericFormats = new List<NumericFormat>();
    _numericFormatDescriptions = new List<String>();

    _numericFormats.add(DEFAULT_NUMERIC_FORMAT);
    _numericFormatDescriptions.add("Default"); // 0

    _numericFormats.add((double x) => x.toString());
    _numericFormatDescriptions.add("Decimal"); // 1

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x, 0);
      return "${sd.sign}${addCommas(sd.intPart, 3)}";
    });
    _numericFormatDescriptions.add("1,000 (rounded)"); // 2

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x, 2);
      return "${sd.sign}${addCommas(sd.intPart, 3)}.${sd.fracPart}";
    });
    _numericFormatDescriptions.add("1,000.12 (2 decimals)"); // 3

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x, 0);
      return "${sd.sign}\$${addCommas(sd.intPart, 3)}";
    });
    _numericFormatDescriptions.add("\$1,000"); // 4

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x, 2);
      return "${sd.sign}\$${addCommas(sd.intPart, 3)}.${sd.fracPart}";
    });
    _numericFormatDescriptions.add("\$1,000.12"); // 5

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x, 0);
      return "${sd.lparen}${addCommas(sd.intPart, 3)}${sd.rparen}";
    });
    _numericFormatDescriptions.add("(1,000)"); // 6

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x, 2);
      return "${sd.lparen}${addCommas(sd.intPart, 3)}.${sd.fracPart}${sd.rparen}";
    });
    _numericFormatDescriptions.add("(1,000.12)"); // 7

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x * 100.0, 0);
      return "${sd.sign}${sd.intPart}%";
    });
    _numericFormatDescriptions.add("10%"); // 8

    _numericFormats.add(String _(double x) {
      SplitDecimal sd = split(x * 100.0, 2);
      return "${sd.sign}${sd.intPart}.${sd.fracPart}%";
    });
    _numericFormatDescriptions.add("10.12%"); // 9

    _numericFormats.add(String _(double x) {
      Date dt = DateUtils.getDateTime(x);
      return "${dt.month}/${dt.day}/${dt.year}";
    });
    _numericFormatDescriptions.add("MM/DD/YYYY"); // 10

    _numericFormats.add(String _(double x) {
      Date dt = DateUtils.getDateTime(x);
      return "${_monthNames[dt.month - 1]} ${dt.day}, ${dt.year}";
    });
    _numericFormatDescriptions.add("Month Day, Year"); // 11

    _numericFormats.add(String _(double x) {
      x -= x.floor(); // ignore days
      Date dt = DateUtils.getDateTime(x);
      String hh = StringUtils.twoDigits(dt.hours);
      String mm = StringUtils.twoDigits(dt.minutes);
      String ss = StringUtils.twoDigits(dt.seconds);
      return "${hh}:${mm}:${ss}";
    });
    _numericFormatDescriptions.add("HH:MM:SS (Time)"); // 12

    _numericFormats.add(String _(double x) {
      Date dt = DateUtils.getDateTime(x);
      String hh = StringUtils.twoDigits(dt.hours);
      String mm = StringUtils.twoDigits(dt.minutes);
      String ss = StringUtils.twoDigits(dt.seconds);
      return "${dt.month}/${dt.day}/${dt.year} ${hh}:${mm}:${ss}";
    });
    _numericFormatDescriptions.add("MM/DD/YYYY HH:MM:SS"); // 13

    _numericFormats.add(String _(double x) {
      double days = x.floor();
      x -= days;
      Date dt = DateUtils.getDateTime(x);
      String mm = StringUtils.twoDigits(dt.minutes);
      String ss = StringUtils.twoDigits(dt.seconds);
      return "${24 * days + dt.hours}:${mm}:${ss}";
    });
    _numericFormatDescriptions.add("HH:MM:SS (Hours)"); // 14

    _numericFormats.add((double x) => x == 0.0 ? "FALSE" : "TRUE");
    _numericFormatDescriptions.add("TRUE/FALSE"); // 15

    _textFormatDescriptions = new List<String>();
    _textFormatDescriptions.add("--");
    _textFormatDescriptions.add("B"); // 1
    _textFormatDescriptions.add("I"); // 2
    _textFormatDescriptions.add("B+I"); // 3
    _textFormatDescriptions.add("U"); // 4
    _textFormatDescriptions.add("B+U"); // 5
    _textFormatDescriptions.add("I+U"); // 6
    _textFormatDescriptions.add("B+I+U"); // 7
    _textFormatDescriptions.add("S"); // 8
    _textFormatDescriptions.add("B+S"); // 9
    _textFormatDescriptions.add("I+S"); // 10
    _textFormatDescriptions.add("B+I+S"); // 11
    _textFormatDescriptions.add("U+S"); // 12
    _textFormatDescriptions.add("B+U+S"); // 13
    _textFormatDescriptions.add("I+U+S"); // 14
    _textFormatDescriptions.add("B+I+U+S"); // 15
  }

  // Return the index of a format suitable for displaying data of a given type.
  int getFormatForDatatype(int datatype) {
    switch (datatype) {
    case Value.TYPE_DOUBLE: case Value.TYPE_STRING:
      return FORMAT_DEFAULT;
    case Value.TYPE_DATE:
      return FORMAT_DATE;
    case Value.TYPE_TIME:
      return FORMAT_TIME;
    case Value.TYPE_DATE_TIME:
      return FORMAT_DATE_TIME;
    case Value.TYPE_BOOLEAN:
      return FORMAT_BOOLEAN;
    default:
      throw new RuntimeException("Unknown datatype: ${datatype}");
    }
  }

  // Return the numeric formatting function with the given index
  NumericFormat getNumericFormat(int index) => _numericFormats[index];

  String getNumericFormatDescription(int index) => _numericFormatDescriptions[index];

  String getTextFormatDescription(int index) => _textFormatDescriptions[index];
}

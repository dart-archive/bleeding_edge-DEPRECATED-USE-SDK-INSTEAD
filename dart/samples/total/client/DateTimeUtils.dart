// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class DateUtils {
  static final int DAYS_FROM_1900_TO_1970 = 25569;
  static final int MILLISECONDS_PER_DAY = 86400000;
  static final int MILLISECONDS_PER_HOUR = 3600000;
  static Date _EPOCH;

  // Return the 1899-12-30 spreadsheet epoch
  static Date get EPOCH() {
    if (_EPOCH == null) {
      _EPOCH = new Date(1899, 12, 30, 0, 0, 0, 0);
    }
    return _EPOCH;
  }

  static double getDate(int year, int month, int day) {
    Date dateTime = new Date(year, month, day, 12, 0, 0, 0);
    int milliseconds = dateTime.difference(EPOCH).inMilliseconds;
    double days = (milliseconds / MILLISECONDS_PER_DAY).floor();
    return days;
  }

  // Return a Date object corresponding to the given number of days after 1899-12-30.
  static Date getDateTime(double daysSince18991230) {
    double daysSinceEpoch = daysSince18991230 - DAYS_FROM_1900_TO_1970;
    int millisSinceEpoch = (daysSinceEpoch * MILLISECONDS_PER_DAY).toInt();
    return new Date.fromEpoch(millisSinceEpoch, isUtc: true);
  }

  static double getTime(int hour, int minute, int second) {
    Date dateTime = new Date(1899, 12, 30, hour, minute, second, 0);
    int milliseconds = dateTime.difference(EPOCH).inMilliseconds;
    double days = milliseconds / MILLISECONDS_PER_DAY;
    return days;
  }

  // Return true if the given string will be treated as a date when entered into a spreadsheet
  // cell.  Currently, the forms MM-DD (with the current year implied), MM-DD-YY, and MM-DD-YYYY
  // are recognized.  For two digit years, values between 00 and 50 are prefixed with "20" and
  // values between 51 and 99 are prefixed with "19".  Either '-' or '/' may be used as the
  // separator character.
  static bool isDate(String value) {
    // Perform trivial rejection if the initial character is not a digit
    if (!StringUtils.isDigit(value.charCodeAt(0))) {
      return false;
    }
    // TODO: use @"" syntax
    // MM-DD
    RegExp mmdd = const RegExp("^(\\d\\d?)[-/](\\d\\d?)\$");
    if (mmdd.hasMatch(value)) {
      return true;
    }
    // MM-DD-YY or MM-DD-YYYY
    RegExp mmddyyyy = const RegExp("^(\\d\\d?)[-/](\\d\\d?)[-/](\\d\\d\\d?\\d?)\$");
    if (mmddyyyy.hasMatch(value)) {
      return true;
    }
    return false;
  }

  // Return the number of days between 1899-12-30 and the current time.
  static double now() {
    Date nowDate = new Date.now();
    int milliseconds = nowDate.difference(EPOCH).inMilliseconds;
    // We round the result to get rid of daylight saving differences.
    double days = (milliseconds / MILLISECONDS_PER_DAY).round();
    return days;
  }

  // Parse a date and return the number of days between 1899-12-30 and the given date.
  // The acceptable formats are as described in the comments for isDate(String).
  static double parseDate(String value) {
    RegExp mmdd = const RegExp("^(\\d\\d?)[-/](\\d\\d?)\$");
    Match m = mmdd.firstMatch(value);
    if (m != null) {
      int month = _parseInt(m[1]);
      int day = _parseInt(m[2]);
      Date nowDate = new Date.now();
      int thisYear = nowDate.year;
      Date dateTime = new Date(thisYear, month, day, 0, 0, 0, 0);
      int milliseconds = dateTime.difference(EPOCH).inMilliseconds;
      // We round the result to get rid of daylight saving differences.
      double days = (milliseconds / MILLISECONDS_PER_DAY).round();
      return days;
    }

    RegExp mmddyyyy = const RegExp("^(\\d\\d?)[-/](\\d\\d?)[-/](\\d\\d\\d?\\d?)\$");
    m = mmddyyyy.firstMatch(value);
    if (m != null) {
      int month = _parseInt(m[1]);
      int day = _parseInt(m[2]);
      int year = _parseInt(m[3]);
      // 0-50 ==> 2000-2050, 51-99 ==> 1951-1999
      if (year < 50) {
        year += 2000;
      } else if (year < 100) {
        year += 1900;
      }
      Date dateTime = new Date(year, month, day, 0, 0, 0, 0);
      int milliseconds = dateTime.difference(EPOCH).inMilliseconds;
      // We round the result to get rid of daylight saving differences.
      double days = (milliseconds / MILLISECONDS_PER_DAY).round();
      return days;
    }

    return -1.0;
  }

  // Return the number of days between 1899-12-30 and the current time, truncated to
  // an integer.
  static double today() => now().floor();

  // Parse an integer, stripping leading zeros to avoid an octal parsing bug.
  static int _parseInt(String s) {
    while (s.length > 1 && s.charCodeAt(0) == StringUtils.ZERO) {
      s = s.substring(1, s.length);
    }
    return Math.parseInt(s);
  }
}

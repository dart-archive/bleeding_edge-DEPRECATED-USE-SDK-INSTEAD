// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class StringUtils {

  static final String _ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  static final String _BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

  // FIXME: replace getters below with static const fields.
  static int get COMMA() { return _c(","); }
  static int get E_LOWER() { return _c("e"); }
  static int get E_UPPER() { return _c("E"); }
  static int get MINUS() { return _c("-"); }
  static int get NEWLINE() { return _c("\n"); }
  static int get NINE() { return _c("9"); }
  static int get PERIOD() { return _c("."); }
  static int get PLUS() { return _c("+"); }
  static int get QUOTE() { return _c("\""); }
  static int get SEMICOLON() { return _c(";"); }
  static int get ZERO() { return _c("0"); }

  static String base64Encode(String data) {
    StringBuffer sb = new StringBuffer();
    int length = data.length;
    for (int i = 0; i < length; i += 3) {
      int c0 = data.charCodeAt(i) & 0xff;
      int c1 = i + 1 < length ? data.charCodeAt(i + 1) & 0xff : 0;
      int c2 = i + 2 < length ? data.charCodeAt(i + 2) & 0xff : 0;

      int x0 = c0 >> 2;
      int x1 = ((c0 << 4) | (c1 >> 4)) & 0x3f;
      int x2 = ((c1 << 2) | (c2 >> 6)) & 0x3f;
      int x3 = c2 & 0x3f;

      sb.add(_BASE64[x0]);
      sb.add(_BASE64[x1]);
      sb.add(_BASE64[x2]);
      sb.add(_BASE64[x3]);
    }
    return sb.toString();
  }

  // Return 'A', ..., 'Z', 'AA, 'AB', ..., 'ZZ', 'AAA', ...
  // 'A' = 0, 'Z' = 25, 'AA' = 26, etc.
  static String columnString(int c) {
    if (c <= 0) {
      throw new RuntimeException("c <= 0");
    }

    List<int> x = new List<int>(4);
    int i = 0;
    while (c > 0) {
      c -= 1;
      x[i++] = c % 26;
      c ~/= 26;
    }
    StringBuffer sb = new StringBuffer();
    while (i-- > 0) {
      sb.add(_ALPHABET[x[i]]);
    }
    return sb.toString();
  }

  // Return a negative integer if s1 is lexically before s2, a positive integer if
  // s1 is lexically after s2, or 0 if s1 and s2 are equal.
  static int compare(String s1, String s2) {
    int pos1 = 0;
    int pos2 = 0;
    int length = Math.min(s1.length, s2.length);
    while (pos1 < length) {
      int result;
      if ((result = s1.charCodeAt(pos1++) - s2.charCodeAt(pos2++)) != 0) {
        return result;
      }
    }
    return s1.length - s2.length;
  }

  static String escapeStringLiteral(String s) {
    StringBuffer sb = new StringBuffer();
    int last = 0;
    for (int i = 0; i < s.length; i++) {
      switch (s[i]) {
      case "\\":
        sb.add(s.substring(last, i));
        sb.add("\\\\");
        last = i + 1;
        break;
      case "\"":
        sb.add(s.substring(last, i));
        sb.add("\\\"");
        last = i + 1;
        break;
      }
    }
    sb.add(s.substring(last, s.length));
    return sb.toString();
  }

  static bool isDigit(int s) {
    return s >= ZERO && s <= NINE;
  }

  // Returns true if the given string matches ^[+-]?[0-9]*\.?[0-9]*([eE][+-]?[0-9]+)?$
  static bool isNumeric(String value) {
    int len = value.length;
    if (len == 0) {
      return false;
    }
    bool gotDecimalPoint = false;
    bool gotDigit = false;
    int i = 0;
    // Allow optional + or -
    if (value.charCodeAt(i) == PLUS || value.charCodeAt(i) == MINUS) {
      i++;
    }
    // Consume digits and "."'s
    while (i < len && (isDigit(value.charCodeAt(i)) || value.charCodeAt(i) == PERIOD)) {
      if (value.charCodeAt(i) == PERIOD) {
        if (gotDecimalPoint) {
          return false;
        }
        gotDecimalPoint = true;
      } else {
        gotDigit = true;
      }
      i++;
    }
    // There must be at least one digit
    if (!gotDigit) {
      return false;
    }
    if (i < len && (value.charCodeAt(i) == E_UPPER
        || value.charCodeAt(i) == E_LOWER)) {
      i++;
      // Allow optional + or -
      if (i < len && (value.charCodeAt(i) == PLUS || value.charCodeAt(i) == MINUS)) {
        i++;
      }
      bool gotExponent = false;
      while (i < len && isDigit(value.charCodeAt(i))) {
        gotExponent = true;
        i++;
      }
      // There must be at least one digit in the exponent
      if (!gotExponent) {
        return false;
      }
    }
    // The entire input must be consumed
    if (i < len) {
      return false;
    }
    return true;
  }

  // Return 1, 2, 3, ...
  static String rowString(int r) {
    if (r <= 0) {
      throw new RuntimeException("r <= 0");
    }
    return r.toString();
  }

  /**
   * Split a delimited string of the form "Stuff","Stuff,With,Commas",UnquotedStuff
   * Delimiters inside quotes are ignored.
   */
  static List<String> split(String s, int splitChar) {
    List<String> out = new List<String>();
    bool inQuote = false;
    int start = 0;
    for (int i = 0; i < s.length; i++) {
      if (s.charCodeAt(i) == QUOTE) {
        inQuote = !inQuote;
      } else if ((s.charCodeAt(i) == splitChar) && !inQuote) {
        out.add(s.substring(start, i));
        start = i + 1;
      }
    }
    out.add(s.substring(start, s.length));
    return out;
  }

  /**
   * Remove a pair of quotes from the ends of a string, if present.
   */
  static String stripQuotes(String s) {
    int len = s.length;
    if (len > 1 && s.charCodeAt(0) == QUOTE && s.charCodeAt(len - 1) == QUOTE) {
      return s.substring(1, len - 1);
    }
    return s;
  }

  // Return "X", "X.Y", or "X.YZ", stripping trailing zeroes.
  static String twoDecimals(double value) {
    String v = value.toStringAsFixed(2);
    if (v.endsWith(".00")) {
      v = v.substring(0, v.length - 3);
     } else if (v.endsWith("0")) {
      v = v.substring(0, v.length - 1);
    }
    return v;
  }

  static String twoDigits(int x) {
    String s = x.toString();
    if (x < 10) {
      return "0${s}";
    } else if (x < 100) {
      return s;
    } else {
      throw new RuntimeException("x >= 100");
    }
  }

  static int _c(String s) {
    return s.charCodeAt(0);
  }
}

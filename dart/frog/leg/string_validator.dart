// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Check the validity of string literals.

#library("stringvalidator");

#import("leg.dart");
#import("scanner/scannerlib.dart");
#import("tree/tree.dart");
#import("elements/elements.dart");
#import("util/characters.dart");

class StringValidator  {
  final DiagnosticListener listener;

  StringValidator(this.listener);

  QuotedString validateQuotedString(Token token) {
    SourceString source = token.value;
    StringQuoting quoting = quotingFromString(source);
    int leftQuote = quoting.leftQuoteLength;
    int rightQuote = quoting.rightQuoteLength;
    SourceString content = source.copyWithoutQuotes(leftQuote, rightQuote);
    return validateString(token,
                          token.charOffset + leftQuote,
                          content,
                          quoting);
  }

  QuotedString validateInterpolationPart(Token token, StringQuoting quoting,
                                         [bool isFirst = false,
                                          bool isLast = false]) {
    SourceString source = token.value;
    int leftQuote = 0;
    int rightQuote = 0;
    if (isFirst) leftQuote = quoting.leftQuoteLength;
    if (isLast) rightQuote = quoting.rightQuoteLength;
    SourceString content = source.copyWithoutQuotes(leftQuote, rightQuote);
    return validateString(token,
                          token.charOffset + leftQuote,
                          content,
                          quoting);
  }

  static StringQuoting quotingFromString(SourceString sourceString) {
    Iterator<int> source = sourceString.iterator();
    bool raw = false;
    int quoteChar = source.next();
    if (quoteChar == $AT) {
      raw = true;
      quoteChar = source.next();
    }
    assert(quoteChar === $SQ || quoteChar === $DQ);
    // String has at least one quote. Check it if has three.
    // If it only have two, the string must be an empty string literal,
    // and end after the second quote.
    bool multiline = false;
    if (source.hasNext() && source.next() == quoteChar && source.hasNext()) {
      assert(source.next() == quoteChar);
      multiline = true;
    }
    return StringQuoting.get(quoteChar, raw, multiline);
  }

  void stringParseError(String message, Token token, int offset) {
    listener.cancel("$message @ $offset", token : token);
  }

  /**
   * Validates the escape sequences and special characters of a string literal.
   * Returns a QuotedString if valid, and null if not.
   */
  QuotedString validateString(Token token,
                              int startOffset,
                              SourceString string,
                              StringQuoting quoting) {
    // We only need to check for invalid x and u escapes, for line
    // terminators in non-multiline strings, and for invalid Unicode
    // scalar values (either directly or as u-escape values).
    int length = 0;
    int index = startOffset;
    for(Iterator<int> iter = string.iterator(); iter.hasNext(); length++) {
      index++;
      int code = iter.next();
      if (code === $BACKSLASH) {
        if (quoting.raw) continue;
        if (!iter.hasNext()) {
          stringParseError("Incomplete escape sequence",token, index);
          return null;
        }
        index++;
        code = iter.next();
        if (code === $x) {
          for (int i = 0; i < 2; i++) {
            if (!iter.hasNext()) {
              stringParseError("Incomplete escape sequence", token, index);
              return null;
            }
            index++;
            code = iter.next();
            if (!isHexDigit(code)) {
              stringParseError("Invalid character in escape sequence",
                               token, index);
              return null;
            }
          }
          // A two-byte hex escape can't generate an invalid value.
          continue;
        } else if (code === $u) {
          int escapeStart = index - 1;
          index++;
          code = iter.next();
          int value = 0;
          if (code == $OPEN_CURLY_BRACKET) {
            // expect 1-6 hex digits.
            int count = 0;
            index++;
            code = iter.next();
            do {
              if (!isHexDigit(code)) {
                stringParseError("Invalid character in escape sequence",
                                 token, index);
                return null;
              }
              count++;
              value = value * 16 + hexDigitValue(code);
              index++;
              code = iter.next();
            } while (code != $CLOSE_CURLY_BRACKET);
            if (count > 6) {
              stringParseError("Invalid character in escape sequence",
                               token, index - (count - 6));
              return null;
            }
          } else {
            // Expect four hex digits, including the one just read.
            for (int i = 0; i < 4; i++) {
              if (i > 0) {
                index++;
                code = iter.next();
              }
              if (!isHexDigit(code)) {
                stringParseError("Invalid character in escape sequence",
                                 token, index);
                return null;
              }
              value = value * 16 + hexDigitValue(code);
            }
          }
          code = value;
        }
      }
      // This handles both unescaped characters and the value of unicode
      // escapes.
      if (!isUnicodeScalarValue(code)) {
        stringParseError(
            "Invalid Unicode scalar value U+${code.toRadixString(16)}",
            token, index);
        return null;
      }
    }
    // String literal successfully validated.
    return new QuotedString(string, quoting, length);
  }
}

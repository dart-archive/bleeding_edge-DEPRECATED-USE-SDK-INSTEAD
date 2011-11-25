// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface Scanner {
  Token tokenize();
}

/**
 * Common base class for a Dart scanner.
 */
class AbstractScanner<T> implements Scanner {
  // TODO(ahe): following makes frog happy.
  abstract int advance();
  abstract int nextByte();
  abstract int peek();
  abstract int select(int choice, String yes, String no);
  abstract void appendStringToken(int kind, String value);
  abstract void appendByteStringToken(int kind, T value);
  abstract void appendKeywordToken(Keyword keyword);
  abstract void appendWhiteSpace(int next);
  abstract void appendEofToken();
  abstract T asciiString(int start);
  abstract T utf8String(int start, int offset);
  abstract Token firstToken();
  abstract void beginToken();
  abstract void addToCharOffset(int offset);
  abstract int get charOffset();
  abstract int get byteOffset();
  abstract void appendBeginGroup(int kind, String value);
  abstract void appendEndGroup(int kind, String value, int openKind);
  abstract void appendGtGt(int kind, String value);
  abstract void appendGtGtGt(int kind, String value);

  // TODO(ahe): Move this class to implementation.

  Token tokenize() {
    int next = advance();
    while (next != $EOF) {
      next = bigSwitch(next);
    }
    appendEofToken();
    return firstToken();
  }

  int bigSwitch(int next) {
    beginToken();
    if (next === $TAB || next === $LF || next === $CR || next === $SPACE) {
      appendWhiteSpace(next);
      return advance();
    }

    if ($a <= next && next <= $z) {
      return tokenizeKeywordOrIdentifier(next);
    }

    if (($A <= next && next <= $Z) || next === $_ || next === $DOLLAR) {
      return tokenizeIdentifier(next, byteOffset);
    }

    if (next === $LT) {
      return tokenizeLessThan(next);
    }

    if (next === $GT) {
      return tokenizeGreaterThan(next);
    }

    if (next === $EQ) {
      return tokenizeEquals(next);
    }

    if (next === $BANG) {
      return tokenizeExclamation(next);
    }

    if (next === $PLUS) {
      return tokenizePlus(next);
    }

    if (next === $MINUS) {
      return tokenizeMinus(next);
    }

    if (next === $STAR) {
      return tokenizeMultiply(next);
    }

    if (next === $PERCENT) {
      return tokenizePercent(next);
    }

    if (next === $AMPERSAND) {
      return tokenizeAmpersand(next);
    }

    if (next === $BAR) {
      return tokenizeBar(next);
    }

    if (next === $CARET) {
      return tokenizeCaret(next);
    }

    if (next === $LBRACKET) {
      return tokenizeOpenBracket(next);
    }

    if (next === $TILDE) {
      return tokenizeTilde(next);
    }

    if (next === $BACKSLASH) {
      appendStringToken(BACKSLASH_TOKEN, "\\");
      return advance();
    }

    if (next === $HASH) {
      return tokenizeTag(next);
    }

    if (next === $LPAREN) {
      appendBeginGroup(LPAREN_TOKEN, "(");
      return advance();
    }

    if (next === $RPAREN) {
      appendEndGroup(RPAREN_TOKEN, ")", LPAREN_TOKEN);
      return advance();
    }

    if (next === $COMMA) {
      appendStringToken(COMMA_TOKEN, ",");
      return advance();
    }

    if (next === $COLON) {
      appendStringToken(COLON_TOKEN, ":");
      return advance();
    }

    if (next === $SEMICOLON) {
      appendStringToken(SEMICOLON_TOKEN, ";");
      return advance();
    }

    if (next === $QUESTION) {
      appendStringToken(QUESTION_TOKEN, "?");
      return advance();
    }

    if (next === $RBRACKET) {
      appendEndGroup(RBRACKET_TOKEN, "]", LBRACKET_TOKEN);
      return advance();
    }

    if (next === $BACKPING) {
      appendStringToken(BACKPING_TOKEN, "`");
      return advance();
    }

    if (next === $LBRACE) {
      appendBeginGroup(LBRACE_TOKEN, "{");
      return advance();
    }

    if (next === $RBRACE) {
      appendEndGroup(RBRACE_TOKEN, "}", LBRACE_TOKEN);
      return advance();
    }

    if (next === $SLASH) {
      return tokenizeSlashOrComment(next);
    }

    if (next === $AT) {
      return tokenizeRawString(next);
    }

    if (next === $DQ || next === $SQ) {
      return tokenizeString(next, byteOffset, false);
    }

    if (next === $PERIOD) {
      return tokenizeDotOrNumber(next);
    }

    if (next === $0) {
      return tokenizeHexOrNumber(next);
    }

    // TODO(ahe): Would a range check be faster?
    if (next === $1 || next === $2 || next === $3 || next === $4 ||  next === $5
        || next === $6 || next === $7 || next === $8 || next === $9) {
      return tokenizeNumber(next);
    }

    if (next === $EOF) {
      return $EOF;
    }
    if (next < 0x1f) {
      throw new MalformedInputException(charOffset);
    }
    // Non-ascii identifier.
    return tokenizeIdentifier(next, byteOffset);
  }

  int tokenizeTag(int next) {
    // # or #!.*[\n\r]
    if (byteOffset === 0) {
      if (peek() === $BANG) {
        do {
          next = advance();
        } while (next != $LF && next != $CR);
        return next;
      }
    }
    appendStringToken(HASH_TOKEN, "#");
    return advance();
  }

  int tokenizeTilde(int next) {
    // ~ ~/ ~/=
    next = advance();
    if (next === $SLASH) {
      return select($EQ, "~/=", "~/");
    } else {
      appendStringToken(TILDE_TOKEN, "~");
      return next;
    }
  }

  int tokenizeOpenBracket(int next) {
    // [ [] []=
    next = advance();
    if (next === $RBRACKET) {
      return select($EQ, "[]=", "[]");
    } else {
      appendBeginGroup(LBRACKET_TOKEN, "[");
      return next;
    }
  }

  int tokenizeCaret(int next) {
    // ^ ^=
    return select($EQ, "^=", "^");
  }

  int tokenizeBar(int next) {
    // | || |=
    next = advance();
    if (next === $BAR) {
      appendStringToken(BAR_TOKEN, "||");
      return advance();
    } else if (next === $EQ) {
      appendStringToken(BAR_TOKEN, "|=");
      return advance();
    } else {
      appendStringToken(BAR_TOKEN, "|");
      return next;
    }
  }

  int tokenizeAmpersand(int next) {
    // && &= &
    next = advance();
    if (next === $AMPERSAND) {
      appendStringToken(AMPERSAND_TOKEN, "&&");
      return advance();
    } else if (next === $EQ) {
      appendStringToken(AMPERSAND_TOKEN, "&=");
      return advance();
    } else {
      appendStringToken(AMPERSAND_TOKEN, "&");
      return next;
    }
  }

  int tokenizePercent(int next) {
    // % %=
    return select($EQ, "%=", "%");
  }

  int tokenizeMultiply(int next) {
    // * *=
    return select($EQ, "*=", "*");
  }

  int tokenizeMinus(int next) {
    // - -- -=
    next = advance();
    if (next === $MINUS) {
      appendStringToken(MINUS_TOKEN, "--");
      return advance();
    } else if (next === $EQ) {
      appendStringToken(MINUS_TOKEN, "-=");
      return advance();
    } else {
      appendStringToken(MINUS_TOKEN, "-");
      return next;
    }
  }


  int tokenizePlus(int next) {
    // + ++ +=
    next = advance();
    if ($PLUS === next) {
      appendStringToken(PLUS_TOKEN, "++");
      return advance();
    } else if ($EQ === next) {
      appendStringToken(PLUS_TOKEN, "+=");
      return advance();
    } else {
      appendStringToken(PLUS_TOKEN, "+");
      return next;
    }
  }

  int tokenizeExclamation(int next) {
    // ! != !==
    next = advance();
    if (next === $EQ) {
      return select($EQ, "!==", "!=");
    }
    appendStringToken(BANG_TOKEN, "!");
    return next;
  }

  int tokenizeEquals(int next) {
    // = == ===
    next = advance();
    if (next === $EQ) {
      return select($EQ, "===", "==");
    } else if (next === $GT) {
      appendStringToken(FUNCTION_TOKEN, "=>");
      return advance();
    }
    appendStringToken(EQ_TOKEN, "=");
    return next;
  }

  int tokenizeGreaterThan(int next) {
    // > >= >> >>= >>> >>>=
    next = advance();
    if ($EQ === next) {
      appendStringToken(GT_TOKEN, ">=");
      return advance();
    } else if ($GT === next) {
      next = advance();
      if ($EQ === next) {
        appendStringToken(GT_TOKEN, ">>=");
        return advance();
      } else if ($GT === next) {
        next = advance();
        if (next === $EQ) {
          appendStringToken(GT_TOKEN, ">>>=");
          return advance();
        } else {
          appendGtGtGt(GT_TOKEN, ">>>");
          return next;
        }
      } else {
        appendGtGt(GT_TOKEN, ">>");
        return next;
      }
    } else {
      appendEndGroup(GT_TOKEN, ">", LT_TOKEN);
      return next;
    }
  }

  int tokenizeLessThan(int next) {
    // < <= << <<=
    next = advance();
    if ($EQ === next) {
      appendStringToken(LT_EQ_TOKEN, "<=");
      return advance();
    } else if ($LT === next) {
      return select($EQ, "<<=", "<<");
    } else {
      appendBeginGroup(LT_TOKEN, "<");
      return next;
    }
  }

  int tokenizeNumber(int next) {
    int start = byteOffset;
    while (true) {
      next = advance();
      if ($0 <= next && next <= $9) {
        continue;
      } else if (next === $PERIOD) {
        return tokenizeFractionPart(advance(), start);
      } else if (next === $e || next === $E || next === $d || next === $D) {
        return tokenizeFractionPart(next, start);
      } else {
        appendByteStringToken(INT_TOKEN, asciiString(start));
        return next;
      }
    }
  }

  int tokenizeHexOrNumber(int next) {
    int x = peek();
    if (x === $x || x === $X) {
      advance();
      return tokenizeHex(x);
    }
    return tokenizeNumber(next);
  }

  int tokenizeHex(int next) {
    int start = byteOffset;
    bool hasDigits = false;
    while (true) {
      next = advance();
      if (($0 <= next && next <= $9)
          || ($A <= next && next <= $F)
          || ($a <= next && next <= $f)) {
        hasDigits = true;
      } else {
        if (!hasDigits) {
          throw new MalformedInputException(charOffset);
        }
        appendByteStringToken(HEXADECIMAL_TOKEN, asciiString(start));
        return next;
      }
    }
  }

  int tokenizeDotOrNumber(int next) {
    int start = byteOffset;
    next = advance();
    if (($0 <= next && next <= $9)) {
      return tokenizeFractionPart(next, start);
    } else if ($PERIOD === next) {
      return select($PERIOD, "...", "..");
    } else {
      appendStringToken(PERIOD_TOKEN, ".");
      return next;
    }
  }

  int tokenizeFractionPart(int next, int start) {
    bool done = false;
    LOOP: while (!done) {
      if ($0 <= next && next <= $9) {
      } else if ($e === next || $E === next) {
        next = tokenizeExponent(advance());
        done = true;
        continue LOOP;
      } else {
        done = true;
        continue LOOP;
      }
      next = advance();
    }
    if (next === $d || next === $D) {
      next = advance();
    }
    appendByteStringToken(DOUBLE_TOKEN, asciiString(start));
    return next;
  }

  int tokenizeExponent(int next) {
    if (next === $PLUS || next === $MINUS) {
      next = advance();
    }
    bool hasDigits = false;
    while (true) {
      if ($0 <= next && next <= $9) {
        hasDigits = true;
      } else {
        if (!hasDigits) {
          throw new MalformedInputException(charOffset);
        }
        return next;
      }
      next = advance();
    }
  }

  int tokenizeSlashOrComment(int next) {
    next = advance();
    if ($STAR === next) {
      return tokenizeMultiLineComment(next);
    } else if ($SLASH === next) {
      return tokenizeSingleLineComment(next);
    } else if ($EQ === next) {
      appendStringToken(SLASH_TOKEN, "/=");
      return advance();
    } else {
      appendStringToken(SLASH_TOKEN, "/");
      return next;
    }
  }

  int tokenizeSingleLineComment(int next) {
    while (true) {
      next = advance();
      if ($LF === next || $CR === next || $EOF === next) {
        return next;
      }
    }
  }

  int tokenizeMultiLineComment(int next) {
    next = advance();
    while (true) {
      if ($EOF === next) {
        return next;
      } else if ($STAR === next) {
        next = advance();
        if (next === $SLASH) {
          return advance();
        } else if (next === $EOF) {
          return next;
        }
      } else {
        next = advance();
      }
    }
  }


  int tokenizeKeywordOrIdentifier(int next) {
    KeywordState state = KeywordState.KEYWORD_STATE;
    int start = byteOffset;
    while (state !== null && $a <= next && next <= $z) {
      state = state.next(next);
      next = advance();
    }
    if (state === null || !state.isLeaf()) {
      return tokenizeIdentifier(next, start);
    }
    if (($A <= next && next <= $Z) ||
        ($0 <= next && next <= $9) ||
        next === $_ ||
        next === $DOLLAR) {
      return tokenizeIdentifier(next, start);
    } else if (next < 128) {
      appendKeywordToken(state.keyword);
      return next;
    } else {
      return tokenizeIdentifier(next, start);
    }
  }

  int tokenizeIdentifier(int next, int start) {
    bool isAscii = true;
    while (true) {
      if (($a <= next && next <= $z) ||
          ($A <= next && next <= $Z) ||
          ($0 <= next && next <= $9) ||
          next === $_ ||
          next === $DOLLAR) {
        next = advance();
      } else if (next < 128) {
        if (isAscii) {
          appendByteStringToken(IDENTIFIER_TOKEN, asciiString(start));
        } else {
          appendByteStringToken(IDENTIFIER_TOKEN, utf8String(start, -1));
        }
        return next;
      } else {
        int nonAsciiStart = byteOffset;
        do {
          next = nextByte();
        } while (next > 127);
        String string = utf8String(nonAsciiStart, -1).toString();
        isAscii = false;
        int byteLength = nonAsciiStart - byteOffset;
        addToCharOffset(string.length - byteLength);
      }
    }
  }

  int tokenizeRawString(int next) {
    int start = byteOffset;
    next = advance();
    if (next === $DQ || next === $SQ) {
      return tokenizeString(next, start, true);
    } else {
      throw new MalformedInputException(charOffset);
    }
  }

  int tokenizeString(int next, int start, bool raw) {
    int q = next;
    next = advance();
    if (q === next) {
      next = advance();
      if (q === next) {
        // Multiline string.
        return tokenizeMultiLineString(q, start, raw);
      } else {
        // Empty string.
        appendByteStringToken(STRING_TOKEN, utf8String(start, -1));
        return next;
      }
    }
    if (raw) {
      return tokenizeSingleLineRawString(next, q, start);
    } else {
      return tokenizeSingleLineString(next, q, start);
    }
  }

  int tokenizeSingleLineString(int next, int q1, int start) {
    while (next != $EOF) {
      if (next === q1) {
        appendByteStringToken(STRING_TOKEN, utf8String(start, 0));
        return advance();
      } else if (next === $BACKSLASH) {
        next = advance();
        if (next === $EOF) {
          throw new MalformedInputException(charOffset);
        }
      } else if (next === $LF || next === $CR) {
        throw new MalformedInputException(charOffset);
      }
      next = advance();
    }
    throw new MalformedInputException(charOffset);
  }

  int tokenizeSingleLineRawString(int next, int q1, int start) {
    next = advance();
    while (next != $EOF) {
      if (next === q1) {
        appendByteStringToken(STRING_TOKEN, utf8String(start, 0));
        return advance();
      } else if (next === $LF || next === $CR) {
        throw new MalformedInputException(charOffset);
      }
      next = advance();
    }
    throw new MalformedInputException(charOffset);
  }

  int tokenizeMultiLineString(int q, int start, bool raw) {
    // TODO(ahe): Handle escapes.
    int next = advance();
    while (next != $EOF) {
      if (next === q) {
        next = advance();
        if (next === q) {
          next = advance();
          if (next === q) {
            appendByteStringToken(STRING_TOKEN, utf8String(start, 0));
            return advance();
          }
        }
      }
      next = advance();
    }
    return next;
  }
}

class MalformedInputException {
  final message;
  MalformedInputException(this.message);
  toString() => message.toString();
}

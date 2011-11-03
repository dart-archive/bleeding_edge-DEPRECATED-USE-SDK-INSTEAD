// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface Scanner<T> {
  int advance();
  int nextByte();
  int peek();
  int select(int choice, String yes, String no);
  void appendStringToken(int kind, String value);
  void appendByteStringToken(int kind, T value);
  void appendKeywordToken(Keyword keyword);
  void appendWhiteSpace(int next);
  T asciiString(int start);
  T utf8String(int start, int offset);
  Token firstToken();
  void beginToken();
  void addToCharOffset(int offset);
  final int charOffset;
  final int byteOffset;
  Token tokenize();
}

/**
 * Common base class for a Dart scanner.
 */
class AbstractScanner<T> implements Scanner<T> {
  // TODO(ahe): following makes frog happy.
  abstract int advance();
  abstract int nextByte();
  abstract int peek();
  abstract int select(int choice, String yes, String no);
  abstract void appendStringToken(int kind, String value);
  abstract void appendByteStringToken(int kind, T value);
  abstract void appendKeywordToken(Keyword keyword);
  abstract void appendWhiteSpace(int next);
  abstract T asciiString(int start);
  abstract T utf8String(int start, int offset);
  abstract Token firstToken();
  abstract void beginToken();
  abstract void addToCharOffset(int offset);
  abstract int get charOffset();
  abstract int get byteOffset();

  // TODO(ahe): Move this class to implementation.

  Token tokenize() {
    int next = advance();
    while (next != -1) {
      next = bigSwitch(next);
    }
    return firstToken();
  }

  int bigSwitch(int next) {
    beginToken();
    switch (next) {
      case $TAB:
      case $LF:
      case $CR:
      case $SPACE:
        appendWhiteSpace(next);
        return advance();

      case $LT:
        return tokenizeLessThan(next);

      case $GT:
        return tokenizeGreaterThan(next);

      case $EQ:
        return tokenizeEquals(next);

      case $BANG:
        return tokenizeExclamation(next);

      case $PLUS:
        return tokenizePlus(next);

      case $MINUS:
        return tokenizeMinus(next);

      case $STAR:
        return tokenizeMultiply(next);

      case $PERCENT:
        return tokenizePercent(next);

      case $AMPERSAND:
        return tokenizeAmpersand(next);

      case $BAR:
        return tokenizeBar(next);

      case $CARET:
        return tokenizeCaret(next);

      case $LBRACKET:
        return tokenizeOpenBracket(next);

      case $TILDE:
        return tokenizeTilde(next);

      case $BACKSLASH:
        appendStringToken(BACKSLASH_TOKEN, "\\");
        return advance();

      case $HASH:
        return tokenizeTag(next);

      case $LPAREN:
        appendStringToken(LPAREN_TOKEN, "(");
        return advance();

      case $RPAREN:
        appendStringToken(RPAREN_TOKEN, ")");
        return advance();

      case $COMMA:
        appendStringToken(COMMA_TOKEN, ",");
        return advance();

      case $COLON:
        appendStringToken(COLON_TOKEN, ":");
        return advance();

      case $SEMICOLON:
        appendStringToken(SEMICOLON_TOKEN, ";");
        return advance();

      case $QUESTION:
        appendStringToken(QUESTION_TOKEN, "?");
        return advance();

      case $RBRACKET:
        appendStringToken(RBRACKET_TOKEN, "]");
        return advance();

      case $BACKPING:
        appendStringToken(BACKPING_TOKEN, "`");
        return advance();

      case $LBRACE:
        appendStringToken(LBRACE_TOKEN, "{");
        return advance();

      case $RBRACE:
        appendStringToken(RBRACE_TOKEN, "}");
        return advance();

      case $SLASH:
        return tokenizeSlashOrComment(next);

      case $AT:
        return tokenizeRawString(next);

      case $DQ:
      case $SQ:
        return tokenizeString(next, byteOffset, false);

      case $PERIOD:
        return tokenizeDotOrNumber(next);

      case $0:
        return tokenizeHexOrNumber(next);

      case $1:
      case $2:
      case $3:
      case $4:
      case $5:
      case $6:
      case $7:
      case $8:
      case $9:
        return tokenizeNumber(next);

      case $DOLLAR:
      case $A:
      case $B:
      case $C:
      case $D:
      case $E:
      case $F:
      case $G:
      case $H:
      case $I:
      case $J:
      case $K:
      case $L:
      case $M:
      case $N:
      case $O:
      case $P:
      case $Q:
      case $R:
      case $S:
      case $T:
      case $U:
      case $V:
      case $W:
      case $X:
      case $Y:
      case $Z:
      case $_:
      case $a:
      case $b:
      case $c:
      case $d:
      case $e:
      case $f:
      case $g:
      case $h:
      case $i:
      case $j:
      case $k:
      case $l:
      case $m:
      case $n:
      case $o:
      case $p:
      case $q:
      case $r:
      case $s:
      case $t:
      case $u:
      case $v:
      case $w:
      case $x:
      case $y:
      case $z:
        return tokenizeIdentifier(next);

      default:
        if (next == -1) {
          return -1;
        }
        if (next < 0x1f) {
          throw new MalformedInputException(charOffset);
        }
        return tokenizeIdentifier(next);
    }
  }

  int tokenizeTag(int next) {
    // # or #!.*[\n\r]
    if (byteOffset == 0) {
      if (peek() == $BANG) {
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
    if (next == $SLASH) {
      return select($EQ, "~/=", "~/");
    } else {
      appendStringToken(TILDE_TOKEN, "~");
      return next;
    }
  }

  int tokenizeOpenBracket(int next) {
    // [ [] []=
    next = advance();
    if (next == $RBRACKET) {
      return select($EQ, "[]=", "[]");
    } else {
      appendStringToken(RBRACKET_TOKEN, "[");
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
    switch (next) {
      case $BAR:
        appendStringToken(BAR_TOKEN, "||");
        return advance();
      case $EQ:
        appendStringToken(BAR_TOKEN, "|=");
        return advance();
      default:
        appendStringToken(BAR_TOKEN, "|");
        return next;
    }
  }

  int tokenizeAmpersand(int next) {
    // && &= &
    next = advance();
    switch (next) {
      case $AMPERSAND:
        appendStringToken(AMPERSAND_TOKEN, "&&");
        return advance();
      case $EQ:
        appendStringToken(AMPERSAND_TOKEN, "&=");
        return advance();
      default:
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
    switch (next) {
      case $MINUS:
        appendStringToken(MINUS_TOKEN, "--");
        return advance();
      case $EQ:
        appendStringToken(MINUS_TOKEN, "-=");
        return advance();
      default:
        appendStringToken(MINUS_TOKEN, "-");
        return next;
    }
  }

  int tokenizePlus(int next) {
    // + ++ +=
    next = advance();
    switch (next) {
      case $PLUS:
        appendStringToken(PLUS_TOKEN, "++");
        return advance();
      case $EQ:
        appendStringToken(PLUS_TOKEN, "+=");
        return advance();
      default:
        appendStringToken(PLUS_TOKEN, "+");
        return next;
    }
  }

  int tokenizeExclamation(int next) {
    // ! != !==
    next = advance();
    if (next == $EQ) {
      return select($EQ, "!==", "!=");
    }
    appendStringToken(BANG_TOKEN, "!");
    return next;
  }

  int tokenizeEquals(int next) {
    // = == ===
    next = advance();
    if (next == $EQ) {
      return select($EQ, "===", "==");
    }
    appendStringToken(EQ_TOKEN, "=");
    return next;
  }

  int tokenizeGreaterThan(int next) {
    // > >= >> >>= >>> >>>=
    next = advance();
    switch (next) {
      case $EQ:
        appendStringToken(GT_TOKEN, ">=");
        return advance();
      case $GT:
        next = advance();
        switch (next) {
          case $EQ:
            appendStringToken(GT_TOKEN, ">>=");
            return advance();
          case $GT:
            return select($EQ, ">>>=", ">>>");
          default:
            appendStringToken(GT_TOKEN, ">>");
            return next;
        }
      default:
        appendStringToken(GT_TOKEN, ">");
        return next;
    }
  }

  int tokenizeLessThan(int next) {
    // < <= << <<=
    next = advance();
    switch (next) {
      case $EQ:
        appendStringToken(LT_TOKEN, "<=");
        return advance();
      case $LT:
        return select($EQ, "<<=", "<<");
      default:
        appendStringToken(LT_TOKEN, "<");
        return next;
    }
  }

  int tokenizeNumber(int next) {
    int start = byteOffset;
    while (true) {
      next = advance();
      switch (next) {
        case $0:
        case $1:
        case $2:
        case $3:
        case $4:
        case $5:
        case $6:
        case $7:
        case $8:
        case $9:
          break;

        case $PERIOD:
          return tokenizeFractionPart(advance(), start);

        case $e:
        case $E:
        case $d:
        case $D:
          return tokenizeFractionPart(next, start);

        default:
          appendByteStringToken(INT_TOKEN, asciiString(start));
          return next;
      }
    }
  }

  int tokenizeHexOrNumber(int next) {
    int x = peek();
    if (x == $x || x == $X) {
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
      switch (next) {
        case $0:
        case $1:
        case $2:
        case $3:
        case $4:
        case $5:
        case $6:
        case $7:
        case $8:
        case $9:
        case $A:
        case $B:
        case $C:
        case $D:
        case $E:
        case $F:
        case $a:
        case $b:
        case $c:
        case $d:
        case $e:
        case $f:
          hasDigits = true;
          break;

        default:
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
    switch (next) {
      case $0:
      case $1:
      case $2:
      case $3:
      case $4:
      case $5:
      case $6:
      case $7:
      case $8:
      case $9: {
        return tokenizeFractionPart(next, start);
      }

      case $PERIOD:
        return select($PERIOD, "...", "..");

      default:
        appendStringToken(PERIOD_TOKEN, ".");
        return next;
    }
  }

  int tokenizeFractionPart(int next, int start) {
    bool done = false;
    LOOP: while (!done) {
      switch (next) {
        case $0:
        case $1:
        case $2:
        case $3:
        case $4:
        case $5:
        case $6:
        case $7:
        case $8:
        case $9:
          break;

        case $e:
        case $E:
          next = tokenizeExponent(advance());
          done = true;
          continue LOOP;

        default:
          done = true;
          continue LOOP;
      }
      next = advance();
    }
    if (next == $d || next == $D) {
      next = advance();
    }
    appendByteStringToken(DOUBLE_TOKEN, asciiString(start));
    return next;
  }

  int tokenizeExponent(int next) {
    if (next == $PLUS || next == $MINUS) {
      next = advance();
    }
    bool hasDigits = false;
    while (true) {
      switch (next) {
        case $0:
        case $1:
        case $2:
        case $3:
        case $4:
        case $5:
        case $6:
        case $7:
        case $8:
        case $9:
          hasDigits = true;
          break;

        default:
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
    switch (next) {
      case $STAR:
        return tokenizeMultiLineComment(next);

      case $SLASH:
        return tokenizeSingleLineComment(next);

      case $EQ:
        appendStringToken(SLASH_TOKEN, "/=");
        return advance();

      default:
        appendStringToken(SLASH_TOKEN, "/");
        return next;
    }
  }

  int tokenizeSingleLineComment(int next) {
    while (true) {
      next = advance();
      switch (next) {
        case -1:
        case $LF:
        case $CR:
          return next;
      }
    }
  }

  int tokenizeMultiLineComment(int next) {
    next = advance();
    while (true) {
      switch (next) {
        case -1:
          return next;
        case $STAR:
          next = advance();
          if (next == $SLASH) {
            return advance();
          } else if (next == -1) {
            return next;
          }
          break;
        default:
          next = advance();
          break;
      }
    }
  }

  int tokenizeIdentifier(int next) {
    int start = byteOffset;
    KeywordState state = null;
    if ($a <= next && next <= $z) {
      state = KeywordState.KEYWORD_STATE.next(next);
      next = advance();
    }

    bool isAscii = true;
    while (true) {
      if ($a <= next && next <= $z) {
        if (state != null) {
          state = state.next(next);
        }
      } else if (($0 <= next && next <= $9) || ($A <= next && next <= $Z)
                 || next == $_
                 || next == $DOLLAR) {
        state = null;
      } else if (next < 128) {
        if (state != null && state.isLeaf()) {
          appendKeywordToken(state.keyword);
        } else if (isAscii) {
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
        addToCharOffset(string.length);
        return next;
      }
      next = advance();
    }
  }

  int tokenizeRawString(int next) {
    int start = byteOffset;
    next = advance();
    if (next == $DQ || next == $SQ) {
      return tokenizeString(next, start, true);
    } else {
      throw new MalformedInputException(charOffset);
    }
  }

  int tokenizeString(int next, int start, bool raw) {
    int q = next;
    next = advance();
    if (q == next) {
      next = advance();
      if (q == next) {
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
    while (next != -1) {
      if (next == q1) {
        appendByteStringToken(STRING_TOKEN, utf8String(start, 0));
        return advance();
      } else if (next == $BACKSLASH) {
        next = advance();
        if (next == -1) {
          throw new MalformedInputException(charOffset);
        }
      } else if (next == $LF || next == $CR) {
        throw new MalformedInputException(charOffset);
      }
      next = advance();
    }
    throw new MalformedInputException(charOffset);
  }

  int tokenizeSingleLineRawString(int next, int q1, int start) {
    next = advance();
    while (next != -1) {
      if (next == q1) {
        appendByteStringToken(STRING_TOKEN, utf8String(start, 0));
        return advance();
      } else if (next == $LF || next == $CR) {
        throw new MalformedInputException(charOffset);
      }
      next = advance();
    }
    throw new MalformedInputException(charOffset);
  }

  int tokenizeMultiLineString(int q, int start, bool raw) {
    // TODO(ahe): Handle escapes.
    int next = advance();
    while (next != -1) {
      if (next == q) {
        next = advance();
        if (next == q) {
          next = advance();
          if (next == q) {
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
  MalformedInputException(ignored);
}

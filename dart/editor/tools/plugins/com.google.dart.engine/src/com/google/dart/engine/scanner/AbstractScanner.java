/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.scanner;

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.source.Source;

import java.io.IOException;

/**
 * The abstract class <code>AbstractScanner</code> implements a scanner for Dart code. Subclasses
 * are required to implement the interface used to access the characters being scanned.
 */
public abstract class AbstractScanner {
  // Based on leg scanner revision 6073.

  /**
   * The source being scanned.
   */
  private final Source source;

  /**
   * The error listener that will be informed of any errors that are found during the scan.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The token pointing to the head of the linked list of tokens.
   */
  private final Token tokens;

  /**
   * The last token that was scanned.
   */
  private Token tail;

  /**
   * The index of the first character of the current token.
   */
  private int tokenStart;

  /**
   * A non-breaking space, which is allowed by this scanner as a white-space character.
   */
  private static final int $NBSP = 160;

  /**
   * Initialize a newly created scanner.
   * 
   * @param source the source being scanned
   * @param errorListener the error listener that will be informed of any errors that are found
   */
  public AbstractScanner(Source source, AnalysisErrorListener errorListener) {
    this.source = source;
    this.errorListener = errorListener;
    tokens = new Token(TokenType.EOF, -1);
    tail = tokens;
    tokenStart = -1;
  }

  public abstract int getOffset();

  public Token tokenize() throws IOException {
    int next = advance();
    while (next != -1) {
      next = bigSwitch(next);
    }
    appendEofToken();
    return firstToken();
  }

  protected abstract int advance();

  protected abstract String getString(int start, int offset);

  protected abstract int peek();

  private void appendEofToken() {
    tail = tail.setNext(new Token(TokenType.EOF, getOffset()));
    // EOF points to itself so there's always infinite look-ahead.
    tail.setNext(tail);
  }

  private void appendKeywordToken(Keyword keyword) {
    tail = tail.setNext(new KeywordToken(keyword, tokenStart));
  }

  private void appendStringToken(TokenType type, String value) {
    tail = tail.setNext(new StringToken(type, value, tokenStart));
  }

  private void appendStringToken(TokenType type, String value, int offset) {
    tail = tail.setNext(new StringToken(type, value, tokenStart + offset));
  }

  private void appendToken(TokenType type) {
    tail = tail.setNext(new Token(type, tokenStart));
  }

  private void appendToken(TokenType type, int offset) {
    tail = tail.setNext(new Token(type, offset));
  }

  private void beginToken() {
    tokenStart = getOffset();
  }

  private int bigSwitch(int next) throws IOException {
    beginToken();
    if (next == '\t' || next == '\n' || next == '\r' || next == ' ') {
      //appendWhiteSpace(next);
      return advance();
    }

    if ('a' <= next && next <= 'z') {
      return tokenizeKeywordOrIdentifier(next, true);
    }

    if (('A' <= next && next <= 'Z') || next == '_' || next == '$') {
      return tokenizeIdentifier(next, getOffset(), true);
    }

    if (next == '<') {
      return tokenizeLessThan(next);
    }

    if (next == '>') {
      return tokenizeGreaterThan(next);
    }

    if (next == '=') {
      return tokenizeEquals(next);
    }

    if (next == '!') {
      return tokenizeExclamation(next);
    }

    if (next == '+') {
      return tokenizePlus(next);
    }

    if (next == '-') {
      return tokenizeMinus(next);
    }

    if (next == '*') {
      return tokenizeMultiply(next);
    }

    if (next == '%') {
      return tokenizePercent(next);
    }

    if (next == '&') {
      return tokenizeAmpersand(next);
    }

    if (next == '|') {
      return tokenizeBar(next);
    }

    if (next == '^') {
      return tokenizeCaret(next);
    }

    if (next == '[') {
      return tokenizeOpenSquareBracket(next);
    }

    if (next == '~') {
      return tokenizeTilde(next);
    }

    if (next == '\\') {
      appendToken(TokenType.BACKSLASH);
      return advance();
    }

    if (next == '#') {
      return tokenizeTag(next);
    }

    if (next == '(') {
      appendToken(TokenType.OPEN_PAREN);
      return advance();
    }

    if (next == ')') {
      appendToken(TokenType.CLOSE_PAREN);
      return advance();
    }

    if (next == ',') {
      appendToken(TokenType.COMMA);
      return advance();
    }

    if (next == ':') {
      appendToken(TokenType.COLON);
      return advance();
    }

    if (next == ';') {
      appendToken(TokenType.SEMICOLON);
      return advance();
    }

    if (next == '?') {
      appendToken(TokenType.QUESTION);
      return advance();
    }

    if (next == ']') {
      appendToken(TokenType.CLOSE_SQUARE_BRACKET);
      return advance();
    }

    if (next == '`') {
      appendToken(TokenType.BACKPING);
      return advance();
    }

    if (next == '{') {
      appendToken(TokenType.OPEN_CURLY_BRACKET);
      return advance();
    }

    if (next == '}') {
      appendToken(TokenType.CLOSE_CURLY_BRACKET);
      return advance();
    }

    if (next == '/') {
      return tokenizeSlashOrComment(next);
    }

    if (next == '@') {
      return tokenizeRawString(next);
    }

    if (next == '"' || next == '\'') {
      return tokenizeString(next, getOffset(), false);
    }

    if (next == '.') {
      return tokenizeDotOrNumber(next);
    }

    if (next == '0') {
      return tokenizeHexOrNumber(next);
    }

    if ('1' <= next && next <= '9') {
      return tokenizeNumber(next);
    }

    if (next == -1) {
      return -1;
    }

    if (Character.isLetter(next)) {
      return tokenizeIdentifier(next, getOffset(), true);
    }

    // The following are non-ASCII characters.

    if (next == $NBSP) {
      //appendWhiteSpace(next);
      return advance();
    }

    errorListener.onError(new AnalysisError(
        getSource(),
        ScannerErrorCode.ILLEGAL_CHARACTER,
        next,
        getOffset()));
    return advance();
  }

  private Token firstToken() {
    return tokens.getNext();
  }

  /**
   * Return the source being scanned.
   * 
   * @return the source being scanned
   */
  private Source getSource() {
    return source;
  }

  private int select(char choice, TokenType yesType, TokenType noType) throws IOException {
    int next = advance();
    if (next == choice) {
      appendToken(yesType);
      return advance();
    } else {
      appendToken(noType);
      return next;
    }
  }

  private int tokenizeAmpersand(int next) throws IOException {
    // && &= &
    next = advance();
    if (next == '&') {
      appendToken(TokenType.AMPERSAND_AMPERSAND);
      return advance();
    } else if (next == '=') {
      appendToken(TokenType.AMPERSAND_EQ);
      return advance();
    } else {
      appendToken(TokenType.AMPERSAND);
      return next;
    }
  }

  private int tokenizeBar(int next) throws IOException {
    // | || |=
    next = advance();
    if (next == '|') {
      appendToken(TokenType.BAR_BAR);
      return advance();
    } else if (next == '=') {
      appendToken(TokenType.BAR_EQ);
      return advance();
    } else {
      appendToken(TokenType.BAR);
      return next;
    }
  }

  private int tokenizeCaret(int next) throws IOException {
    // ^ ^=
    return select('=', TokenType.CARET_EQ, TokenType.CARET);
  }

  private int tokenizeDotOrNumber(int next) throws IOException {
    int start = getOffset();
    next = advance();
    if (('0' <= next && next <= '9')) {
      return tokenizeFractionPart(next, start);
    } else if ('.' == next) {
      return select('.', TokenType.PERIOD_PERIOD_PERIOD, TokenType.PERIOD_PERIOD);
    } else {
      appendToken(TokenType.PERIOD);
      return next;
    }
  }

  private int tokenizeEquals(int next) throws IOException {
    // = == === =>
    next = advance();
    if (next == '=') {
      return select('=', TokenType.EQ_EQ_EQ, TokenType.EQ_EQ);
    } else if (next == '>') {
      appendToken(TokenType.FUNCTION);
      return advance();
    }
    appendToken(TokenType.EQ);
    return next;
  }

  private int tokenizeExclamation(int next) throws IOException {
    // ! != !===
    next = advance();
    if (next == '=') {
      return select('=', TokenType.BANG_EQ_EQ, TokenType.BANG_EQ);
    }
    appendToken(TokenType.BANG);
    return next;
  }

  private int tokenizeExponent(int next) throws IOException {
    if (next == '+' || next == '-') {
      next = advance();
    }
    boolean hasDigits = false;
    while (true) {
      if ('0' <= next && next <= '9') {
        hasDigits = true;
      } else {
        if (!hasDigits) {
          errorListener.onError(new AnalysisError(
              getSource(),
              ScannerErrorCode.MISSING_DIGIT,
              getOffset()));
        }
        return next;
      }
      next = advance();
    }
  }

  private int tokenizeFractionPart(int next, int start) throws IOException {
    boolean done = false;
    boolean hasDigit = false;
    LOOP : while (!done) {
      if ('0' <= next && next <= '9') {
        hasDigit = true;
      } else if ('e' == next || 'E' == next) {
        hasDigit = true;
        next = tokenizeExponent(advance());
        done = true;
        continue LOOP;
      } else {
        done = true;
        continue LOOP;
      }
      next = advance();
    }
    if (!hasDigit) {
      appendStringToken(TokenType.INT, getString(start, -2));
      appendToken(TokenType.PERIOD, getOffset() - 1);
      return bigSwitch(next);
    }
    if (next == 'd' || next == 'D') {
      next = advance();
    }
    appendStringToken(TokenType.DOUBLE, getString(start, next < 0 ? 0 : -1));
    return next;
  }

  private int tokenizeGreaterThan(int next) throws IOException {
    // > >= >> >>= >>> >>>=
    next = advance();
    if ('=' == next) {
      appendToken(TokenType.GT_EQ);
      return advance();
    } else if ('>' == next) {
      next = advance();
      if ('=' == next) {
        appendToken(TokenType.GT_GT_EQ);
        return advance();
      } else if ('>' == next) {
        next = advance();
        if (next == '=') {
          appendToken(TokenType.GT_GT_GT_EQ);
          return advance();
        } else {
          appendToken(TokenType.GT_GT_GT);
          return next;
        }
      } else {
        appendToken(TokenType.GT_GT);
        return next;
      }
    } else {
      appendToken(TokenType.GT);
      return next;
    }
  }

  private int tokenizeHex(int next) throws IOException {
    int start = getOffset() - 1;
    boolean hasDigits = false;
    while (true) {
      next = advance();
      if (('0' <= next && next <= '9') || ('A' <= next && next <= 'F')
          || ('a' <= next && next <= 'f')) {
        hasDigits = true;
      } else {
        if (!hasDigits) {
          errorListener.onError(new AnalysisError(
              getSource(),
              ScannerErrorCode.MISSING_HEX_DIGIT,
              getOffset()));
        }
        appendStringToken(TokenType.HEXADECIMAL, getString(start, next < 0 ? 0 : -1));
        return next;
      }
    }
  }

  private int tokenizeHexOrNumber(int next) throws IOException {
    int x = peek();
    if (x == 'x' || x == 'X') {
      advance();
      return tokenizeHex(x);
    }
    return tokenizeNumber(next);
  }

  private int tokenizeIdentifier(int next, int start, boolean allowDollar) {
    while (('a' <= next && next <= 'z') || ('A' <= next && next <= 'Z')
        || ('0' <= next && next <= '9') || next == '_' || (next == '$' && allowDollar)
        || Character.isLetterOrDigit(next)) {
      next = advance();
    }
    appendStringToken(TokenType.IDENTIFIER, getString(start, next < 0 ? 0 : -1));
    return next;
  }

  private int tokenizeInterpolatedExpression(int next, int start) throws IOException {
    appendStringToken(TokenType.STRING_INTERPOLATION, "${", 0);
    next = advance();
    while (next != -1) {
      if (next == '}') {
        beginToken();
        appendToken(TokenType.CLOSE_CURLY_BRACKET);
        next = advance();
        beginToken();
        return next;
      } else {
        next = bigSwitch(next);
      }
    }
    if (next == -1) {
      return next;
    }
    next = advance();
    beginToken();
    return next;
  }

  private int tokenizeInterpolatedIdentifier(int next, int start) {
    appendStringToken(TokenType.STRING_INTERPOLATION, "$", 0);
    beginToken();
    next = tokenizeKeywordOrIdentifier(next, false);
    beginToken();
    return next;
  }

  private int tokenizeKeywordOrIdentifier(int next, boolean allowDollar) {
    KeywordState state = KeywordState.KEYWORD_STATE;
    int start = getOffset();
    while (state != null && 'a' <= next && next <= 'z') {
      state = state.next((char) next);
      next = advance();
    }
    if (state == null || state.keyword() == null) {
      return tokenizeIdentifier(next, start, allowDollar);
    }
    if (('A' <= next && next <= 'Z') || ('0' <= next && next <= '9') || next == '_' || next == '$') {
      return tokenizeIdentifier(next, start, allowDollar);
    } else if (next < 128) {
      appendKeywordToken(state.keyword());
      return next;
    } else {
      return tokenizeIdentifier(next, start, allowDollar);
    }
  }

  private int tokenizeLessThan(int next) throws IOException {
    // < <= << <<=
    next = advance();
    if ('=' == next) {
      appendToken(TokenType.LT_EQ);
      return advance();
    } else if ('<' == next) {
      return select('=', TokenType.LT_LT_EQ, TokenType.LT_LT);
    } else {
      appendToken(TokenType.LT);
      return next;
    }
  }

  private int tokenizeMinus(int next) throws IOException {
    // - -- -=
    next = advance();
    if (next == '-') {
      appendToken(TokenType.MINUS_MINUS);
      return advance();
    } else if (next == '=') {
      appendToken(TokenType.MINUS_EQ);
      return advance();
    } else {
      appendToken(TokenType.MINUS);
      return next;
    }
  }

  private int tokenizeMultiLineComment(int next) throws IOException {
    int nesting = 1;
    next = advance();
    while (true) {
      if (-1 == next) {
        errorListener.onError(new AnalysisError(
            getSource(),
            ScannerErrorCode.UNTERMINATED_MULTI_LINE_COMMENT,
            getOffset()));
        appendStringToken(TokenType.MULTI_LINE_COMMENT, getString(tokenStart, 0));
        return next;
      } else if ('*' == next) {
        next = advance();
        if ('/' == next) {
          --nesting;
          if (0 == nesting) {
            appendStringToken(TokenType.MULTI_LINE_COMMENT, getString(tokenStart, 0));
            return advance();
          } else {
            next = advance();
          }
        }
      } else if ('/' == next) {
        next = advance();
        if ('*' == next) {
          next = advance();
          ++nesting;
        }
      } else {
        next = advance();
      }
    }
  }

  private int tokenizeMultiLineRawString(int quoteChar, int start) {
    int next = advance();
    outer : while (next != -1) {
      while (next != quoteChar) {
        next = advance();
        if (next == -1) {
          break outer;
        }
      }
      next = advance();
      if (next == quoteChar) {
        next = advance();
        if (next == quoteChar) {
          appendStringToken(TokenType.STRING, getString(start, 0));
          return advance();
        }
      }
    }
    errorListener.onError(new AnalysisError(
        getSource(),
        ScannerErrorCode.UNTERMINATED_STRING_LITERAL,
        getOffset()));
    appendStringToken(TokenType.STRING, getString(start, 0));
    return advance();
  }

  private int tokenizeMultiLineString(int quoteChar, int start, boolean raw) throws IOException {
    if (raw) {
      return tokenizeMultiLineRawString(quoteChar, start);
    }
    int next = advance();
    while (next != -1) {
      if (next == '$') {
        appendStringToken(TokenType.STRING, getString(start, -1));
        beginToken();
        next = tokenizeStringInterpolation(start);
        start = getOffset();
        continue;
      }
      if (next == quoteChar) {
        next = advance();
        if (next == quoteChar) {
          next = advance();
          if (next == quoteChar) {
            appendStringToken(TokenType.STRING, getString(start, 0));
            return advance();
          }
        }
        continue;
      }
      if (next == '\\') {
        next = advance();
        if (next == -1) {
          break;
        }
      }
      next = advance();
    }
    errorListener.onError(new AnalysisError(
        getSource(),
        ScannerErrorCode.UNTERMINATED_STRING_LITERAL,
        getOffset()));
    appendStringToken(TokenType.STRING, getString(start, 0));
    return advance();
  }

  private int tokenizeMultiply(int next) throws IOException {
    // * *=
    return select('=', TokenType.STAR_EQ, TokenType.STAR);
  }

  private int tokenizeNumber(int next) throws IOException {
    int start = getOffset();
    while (true) {
      next = advance();
      if ('0' <= next && next <= '9') {
        continue;
      } else if (next == '.') {
        return tokenizeFractionPart(advance(), start);
      } else if (next == 'd' || next == 'D') {
        appendStringToken(TokenType.DOUBLE, getString(start, 0));
        return advance();
      } else if (next == 'e' || next == 'E') {
        return tokenizeFractionPart(next, start);
      } else {
        appendStringToken(TokenType.INT, getString(start, next < 0 ? 0 : -1));
        return next;
      }
    }
  }

  private int tokenizeOpenSquareBracket(int next) throws IOException {
    // [ []  []=
    next = advance();
    if (next == ']') {
      return select('=', TokenType.INDEX_EQ, TokenType.INDEX);
    } else {
      appendToken(TokenType.OPEN_SQUARE_BRACKET);
      return next;
    }
  }

  private int tokenizePercent(int next) throws IOException {
    // % %=
    return select('=', TokenType.PERCENT_EQ, TokenType.PERCENT);
  }

  private int tokenizePlus(int next) throws IOException {
    // + ++ +=
    next = advance();
    if ('+' == next) {
      appendToken(TokenType.PLUS_PLUS);
      return advance();
    } else if ('=' == next) {
      appendToken(TokenType.PLUS_EQ);
      return advance();
    } else {
      appendToken(TokenType.PLUS);
      return next;
    }
  }

  private int tokenizeRawString(int next) throws IOException {
    int start = getOffset();
    next = advance();
    if (next != '"' && next != '\'') {
      errorListener.onError(new AnalysisError(
          getSource(),
          ScannerErrorCode.UNTERMINATED_STRING_LITERAL,
          getOffset()));
    }
    return tokenizeString(next, start, true);
  }

  private int tokenizeSingleLineComment(int next) throws IOException {
    while (true) {
      next = advance();
      if ('\n' == next || '\r' == next || -1 == next) {
        appendStringToken(TokenType.SINGLE_LINE_COMMENT, getString(tokenStart, 0));
        return next;
      }
    }
  }

  private int tokenizeSingleLineRawString(int next, int quoteChar, int start) throws IOException {
    next = advance();
    while (next != -1) {
      if (next == quoteChar) {
        appendStringToken(TokenType.STRING, getString(start, 0));
        return advance();
      } else if (next == '\r' || next == '\n') {
        errorListener.onError(new AnalysisError(
            getSource(),
            ScannerErrorCode.UNTERMINATED_STRING_LITERAL,
            getOffset()));
        appendStringToken(TokenType.STRING, getString(start, 0));
        return advance();
      }
      next = advance();
    }
    errorListener.onError(new AnalysisError(
        getSource(),
        ScannerErrorCode.UNTERMINATED_STRING_LITERAL,
        getOffset()));
    appendStringToken(TokenType.STRING, getString(start, 0));
    return advance();
  }

  private int tokenizeSingleLineString(int next, int quoteChar, int start) throws IOException {
    while (next != quoteChar) {
      if (next == '\\') {
        next = advance();
      } else if (next == '$') {
        appendStringToken(TokenType.STRING, getString(start, -1));
        beginToken();
        next = tokenizeStringInterpolation(start);
        start = getOffset();
        continue;
      }
      if (next <= '\r' && (next == '\n' || next == '\r' || next == -1)) {
        errorListener.onError(new AnalysisError(
            getSource(),
            ScannerErrorCode.UNTERMINATED_STRING_LITERAL,
            getOffset()));
        appendStringToken(TokenType.STRING, getString(start, 0));
        return advance();
      }
      next = advance();
    }
    appendStringToken(TokenType.STRING, getString(start, 0));
    return advance();
  }

  private int tokenizeSlashOrComment(int next) throws IOException {
    next = advance();
    if ('*' == next) {
      return tokenizeMultiLineComment(next);
    } else if ('/' == next) {
      return tokenizeSingleLineComment(next);
    } else if ('=' == next) {
      appendToken(TokenType.SLASH_EQ);
      return advance();
    } else {
      appendToken(TokenType.SLASH);
      return next;
    }
  }

  private int tokenizeString(int next, int start, boolean raw) throws IOException {
    int quoteChar = next;
    next = advance();
    if (quoteChar == next) {
      next = advance();
      if (quoteChar == next) {
        // Multiline string.
        return tokenizeMultiLineString(quoteChar, start, raw);
      } else {
        // Empty string.
        appendStringToken(TokenType.STRING, getString(start, -1));
        return next;
      }
    }
    if (raw) {
      return tokenizeSingleLineRawString(next, quoteChar, start);
    } else {
      return tokenizeSingleLineString(next, quoteChar, start);
    }
  }

  private int tokenizeStringInterpolation(int start) throws IOException {
    beginToken();
    int next = advance();
    if (next == '{') {
      return tokenizeInterpolatedExpression(next, start);
    } else {
      return tokenizeInterpolatedIdentifier(next, start);
    }
  }

  private int tokenizeTag(int next) throws IOException {
    // # or #!.*[\n\r]
    if (getOffset() == 0) {
      if (peek() == '!') {
        do {
          next = advance();
        } while (next != '\n' && next != '\r');
        return next;
      }
    }
    appendToken(TokenType.HASH);
    return advance();
  }

  private int tokenizeTilde(int next) throws IOException {
    // ~ ~/ ~/=
    next = advance();
    if (next == '/') {
      return select('=', TokenType.TILDE_SLASH_EQ, TokenType.TILDE_SLASH);
    } else {
      appendToken(TokenType.TILDE);
      return next;
    }
  }
}

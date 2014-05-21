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
import com.google.dart.engine.utilities.collection.IntList;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * The class {@code Scanner} implements a scanner for Dart code.
 * <p>
 * The lexical structure of Dart is ambiguous without knowledge of the context in which a token is
 * being scanned. For example, without context we cannot determine whether source of the form "<<"
 * should be scanned as a single left-shift operator or as two left angle brackets. This scanner
 * does not have any context, so it always resolves such conflicts by scanning the longest possible
 * token.
 * 
 * @coverage dart.engine.parser
 */
public class Scanner {
  /**
   * The source being scanned.
   */
  private final Source source;

  /**
   * The reader used to access the characters in the source.
   */
  private CharacterReader reader;

  /**
   * The error listener that will be informed of any errors that are found during the scan.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The flag specifying if documentation comments should be parsed.
   */
  private boolean preserveComments = true;

  /**
   * The token pointing to the head of the linked list of tokens.
   */
  private final Token tokens;

  /**
   * The last token that was scanned.
   */
  private Token tail;

  /**
   * The first token in the list of comment tokens found since the last non-comment token.
   */
  private Token firstComment;

  /**
   * The last token in the list of comment tokens found since the last non-comment token.
   */
  private Token lastComment;

  /**
   * The index of the first character of the current token.
   */
  private int tokenStart;

  /**
   * A list containing the offsets of the first character of each line in the source code.
   */
  private IntList lineStarts = new IntList(1024);

  /**
   * A list, treated something like a stack, of tokens representing the beginning of a matched pair.
   * It is used to pair the end tokens with the begin tokens.
   */
  private List<BeginToken> groupingStack = new ArrayList<BeginToken>(128);

  /**
   * The index of the last item in the {@link #groupingStack}, or {@code -1} if the stack is empty.
   */
  private int stackEnd = -1;

  /**
   * A flag indicating whether any unmatched groups were found during the parse.
   */
  private boolean hasUnmatchedGroups = false;

  /**
   * Initialize a newly created scanner.
   * 
   * @param source the source being scanned
   * @param reader the character reader used to read the characters in the source
   * @param errorListener the error listener that will be informed of any errors that are found
   */
  public Scanner(Source source, CharacterReader reader, AnalysisErrorListener errorListener) {
    this.source = source;
    this.reader = reader;
    this.errorListener = errorListener;
    tokens = new Token(TokenType.EOF, -1);
    tokens.setNext(tokens);
    tail = tokens;
    tokenStart = -1;
    lineStarts.add(0);
  }

  /**
   * Return an array containing the offsets of the first character of each line in the source code.
   * 
   * @return an array containing the offsets of the first character of each line in the source code
   */
  public int[] getLineStarts() {
    return lineStarts.toArray();
  }

  /**
   * Return {@code true} if any unmatched groups were found during the parse.
   * 
   * @return {@code true} if any unmatched groups were found during the parse
   */
  public boolean hasUnmatchedGroups() {
    return hasUnmatchedGroups;
  }

  /**
   * Set whether documentation tokens should be scanned.
   * 
   * @param preserveComments {@code true} if documentation tokens should be scanned
   */
  public void setPreserveComments(boolean preserveComments) {
    this.preserveComments = preserveComments;
  }

  /**
   * Record that the source begins on the given line and column at the current offset as given by
   * the reader. The line starts for lines before the given line will not be correct.
   * <p>
   * This method must be invoked at most one time and must be invoked before scanning begins. The
   * values provided must be sensible. The results are undefined if these conditions are violated.
   * 
   * @param line the one-based index of the line containing the first character of the source
   * @param column the one-based index of the column in which the first character of the source
   *          occurs
   */
  public void setSourceStart(int line, int column) {
    int offset = reader.getOffset();
    if (line < 1 || column < 1 || offset < 0 || (line + column - 2) >= offset) {
      return;
    }
    for (int i = 2; i < line; i++) {
      lineStarts.add(1);
    }
    lineStarts.add(offset - column + 1);
  }

  /**
   * Scan the source code to produce a list of tokens representing the source.
   * 
   * @return the first token in the list of tokens that were produced
   */
  public Token tokenize() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("dart.engine.AbstractScanner.tokenize");
    int tokenCounter = 0;
    try {
      int next = reader.advance();
      while (next != -1) {
        tokenCounter++;
        next = bigSwitch(next);
      }
      appendEofToken();
      instrumentation.metric("tokensCount", tokenCounter);
      return getFirstToken();
    } finally {
      instrumentation.log(2); //Log if over 1ms
    }
  }

  /**
   * Append the given token to the end of the token stream being scanned. This method is intended to
   * be used by subclasses that copy existing tokens and should not normally be used because it will
   * fail to correctly associate any comments with the token being passed in.
   * 
   * @param token the token to be appended
   */
  protected void appendToken(Token token) {
    tail = tail.setNext(token);
  }

  protected int bigSwitch(int next) {
    beginToken();

    if (next == '\r') {
      next = reader.advance();
      if (next == '\n') {
        next = reader.advance();
      }
      recordStartOfLine();
      return next;
    } else if (next == '\n') {
      next = reader.advance();
      recordStartOfLine();
      return next;
    } else if (next == '\t' || next == ' ') {
      return reader.advance();
    }

    if (next == 'r') {
      int peek = reader.peek();
      if (peek == '"' || peek == '\'') {
        int start = reader.getOffset();
        return tokenizeString(reader.advance(), start, true);
      }
    }

    if ('a' <= next && next <= 'z') {
      return tokenizeKeywordOrIdentifier(next, true);
    }

    if (('A' <= next && next <= 'Z') || next == '_' || next == '$') {
      return tokenizeIdentifier(next, reader.getOffset(), true);
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
      appendTokenOfType(TokenType.BACKSLASH);
      return reader.advance();
    }

    if (next == '#') {
      return tokenizeTag(next);
    }

    if (next == '(') {
      appendBeginToken(TokenType.OPEN_PAREN);
      return reader.advance();
    }

    if (next == ')') {
      appendEndToken(TokenType.CLOSE_PAREN, TokenType.OPEN_PAREN);
      return reader.advance();
    }

    if (next == ',') {
      appendTokenOfType(TokenType.COMMA);
      return reader.advance();
    }

    if (next == ':') {
      appendTokenOfType(TokenType.COLON);
      return reader.advance();
    }

    if (next == ';') {
      appendTokenOfType(TokenType.SEMICOLON);
      return reader.advance();
    }

    if (next == '?') {
      appendTokenOfType(TokenType.QUESTION);
      return reader.advance();
    }

    if (next == ']') {
      appendEndToken(TokenType.CLOSE_SQUARE_BRACKET, TokenType.OPEN_SQUARE_BRACKET);
      return reader.advance();
    }

    if (next == '`') {
      appendTokenOfType(TokenType.BACKPING);
      return reader.advance();
    }

    if (next == '{') {
      appendBeginToken(TokenType.OPEN_CURLY_BRACKET);
      return reader.advance();
    }

    if (next == '}') {
      appendEndToken(TokenType.CLOSE_CURLY_BRACKET, TokenType.OPEN_CURLY_BRACKET);
      return reader.advance();
    }

    if (next == '/') {
      return tokenizeSlashOrComment(next);
    }

    if (next == '@') {
      appendTokenOfType(TokenType.AT);
      return reader.advance();
    }

    if (next == '"' || next == '\'') {
      return tokenizeString(next, reader.getOffset(), false);
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

    reportError(ScannerErrorCode.ILLEGAL_CHARACTER, Integer.valueOf(next));
    return reader.advance();
  }

  /**
   * Return the first token in the token stream that was scanned.
   * 
   * @return the first token in the token stream that was scanned
   */
  protected Token getFirstToken() {
    return tokens.getNext();
  }

  /**
   * Return the last token that was scanned.
   * 
   * @return the last token that was scanned
   */
  protected Token getTail() {
    return tail;
  }

  /**
   * Record the fact that we are at the beginning of a new line in the source.
   */
  protected void recordStartOfLine() {
    lineStarts.add(reader.getOffset());
  }

  private void appendBeginToken(TokenType type) {
    BeginToken token;
    if (firstComment == null) {
      token = new BeginToken(type, tokenStart);
    } else {
      token = new BeginTokenWithComment(type, tokenStart, firstComment);
      firstComment = null;
      lastComment = null;
    }
    tail = tail.setNext(token);
    groupingStack.add(token);
    stackEnd++;
  }

  private void appendCommentToken(TokenType type, String value) {
    // Ignore comment tokens if client specified that it doesn't need them.
    if (!preserveComments) {
      return;
    }
    // OK, remember comment tokens.
    if (firstComment == null) {
      firstComment = new StringToken(type, value, tokenStart);
      lastComment = firstComment;
    } else {
      lastComment = lastComment.setNext(new StringToken(type, value, tokenStart));
    }
  }

  private void appendEndToken(TokenType type, TokenType beginType) {
    Token token;
    if (firstComment == null) {
      token = new Token(type, tokenStart);
    } else {
      token = new TokenWithComment(type, tokenStart, firstComment);
      firstComment = null;
      lastComment = null;
    }
    tail = tail.setNext(token);
    if (stackEnd >= 0) {
      BeginToken begin = groupingStack.get(stackEnd);
      if (begin.getType() == beginType) {
        begin.setEndToken(token);
        groupingStack.remove(stackEnd--);
      }
    }
  }

  private void appendEofToken() {
    Token eofToken;
    if (firstComment == null) {
      eofToken = new Token(TokenType.EOF, reader.getOffset() + 1);
    } else {
      eofToken = new TokenWithComment(TokenType.EOF, reader.getOffset() + 1, firstComment);
      firstComment = null;
      lastComment = null;
    }
    // The EOF token points to itself so that there is always infinite look-ahead.
    eofToken.setNext(eofToken);
    tail = tail.setNext(eofToken);
    if (stackEnd >= 0) {
      hasUnmatchedGroups = true;
      // TODO(brianwilkerson) Fix the ungrouped tokens?
    }
  }

  private void appendKeywordToken(Keyword keyword) {
    if (firstComment == null) {
      tail = tail.setNext(new KeywordToken(keyword, tokenStart));
    } else {
      tail = tail.setNext(new KeywordTokenWithComment(keyword, tokenStart, firstComment));
      firstComment = null;
      lastComment = null;
    }
  }

  private void appendStringToken(TokenType type, String value) {
    if (firstComment == null) {
      tail = tail.setNext(new StringToken(type, value, tokenStart));
    } else {
      tail = tail.setNext(new StringTokenWithComment(type, value, tokenStart, firstComment));
      firstComment = null;
      lastComment = null;
    }
  }

  private void appendStringTokenWithOffset(TokenType type, String value, int offset) {
    if (firstComment == null) {
      tail = tail.setNext(new StringToken(type, value, tokenStart + offset));
    } else {
      tail = tail.setNext(new StringTokenWithComment(type, value, tokenStart + offset, firstComment));
      firstComment = null;
      lastComment = null;
    }
  }

  private void appendTokenOfType(TokenType type) {
    if (firstComment == null) {
      tail = tail.setNext(new Token(type, tokenStart));
    } else {
      tail = tail.setNext(new TokenWithComment(type, tokenStart, firstComment));
      firstComment = null;
      lastComment = null;
    }
  }

  private void appendTokenOfTypeWithOffset(TokenType type, int offset) {
    if (firstComment == null) {
      tail = tail.setNext(new Token(type, offset));
    } else {
      tail = tail.setNext(new TokenWithComment(type, offset, firstComment));
      firstComment = null;
      lastComment = null;
    }
  }

  private void beginToken() {
    tokenStart = reader.getOffset();
  }

  /**
   * Return the beginning token corresponding to a closing brace that was found while scanning
   * inside a string interpolation expression. Tokens that cannot be matched with the closing brace
   * will be dropped from the stack.
   * 
   * @return the token to be paired with the closing brace
   */
  private BeginToken findTokenMatchingClosingBraceInInterpolationExpression() {
    while (stackEnd >= 0) {
      BeginToken begin = groupingStack.get(stackEnd);
      if (begin.getType() == TokenType.OPEN_CURLY_BRACKET
          || begin.getType() == TokenType.STRING_INTERPOLATION_EXPRESSION) {
        return begin;
      }
      hasUnmatchedGroups = true;
      groupingStack.remove(stackEnd--);
    }
    //
    // We should never get to this point because we wouldn't be inside a string interpolation
    // expression unless we had previously found the start of the expression.
    //
    return null;
  }

  /**
   * Return the source being scanned.
   * 
   * @return the source being scanned
   */
  private Source getSource() {
    return source;
  }

  /**
   * Report an error at the current offset.
   * 
   * @param errorCode the error code indicating the nature of the error
   * @param arguments any arguments needed to complete the error message
   */
  private void reportError(ScannerErrorCode errorCode, Object... arguments) {
    errorListener.onError(new AnalysisError(
        getSource(),
        reader.getOffset(),
        1,
        errorCode,
        arguments));
  }

  private int select(char choice, TokenType yesType, TokenType noType) {
    int next = reader.advance();
    if (next == choice) {
      appendTokenOfType(yesType);
      return reader.advance();
    } else {
      appendTokenOfType(noType);
      return next;
    }
  }

  private int selectWithOffset(char choice, TokenType yesType, TokenType noType, int offset) {
    int next = reader.advance();
    if (next == choice) {
      appendTokenOfTypeWithOffset(yesType, offset);
      return reader.advance();
    } else {
      appendTokenOfTypeWithOffset(noType, offset);
      return next;
    }
  }

  private int tokenizeAmpersand(int next) {
    // && &= &
    next = reader.advance();
    if (next == '&') {
      appendTokenOfType(TokenType.AMPERSAND_AMPERSAND);
      return reader.advance();
    } else if (next == '=') {
      appendTokenOfType(TokenType.AMPERSAND_EQ);
      return reader.advance();
    } else {
      appendTokenOfType(TokenType.AMPERSAND);
      return next;
    }
  }

  private int tokenizeBar(int next) {
    // | || |=
    next = reader.advance();
    if (next == '|') {
      appendTokenOfType(TokenType.BAR_BAR);
      return reader.advance();
    } else if (next == '=') {
      appendTokenOfType(TokenType.BAR_EQ);
      return reader.advance();
    } else {
      appendTokenOfType(TokenType.BAR);
      return next;
    }
  }

  private int tokenizeCaret(int next) {
    // ^ ^=
    return select('=', TokenType.CARET_EQ, TokenType.CARET);
  }

  private int tokenizeDotOrNumber(int next) {
    int start = reader.getOffset();
    next = reader.advance();
    if (('0' <= next && next <= '9')) {
      return tokenizeFractionPart(next, start);
    } else if ('.' == next) {
      return select('.', TokenType.PERIOD_PERIOD_PERIOD, TokenType.PERIOD_PERIOD);
    } else {
      appendTokenOfType(TokenType.PERIOD);
      return next;
    }
  }

  private int tokenizeEquals(int next) {
    // = == =>
    next = reader.advance();
    if (next == '=') {
      appendTokenOfType(TokenType.EQ_EQ);
      return reader.advance();
    } else if (next == '>') {
      appendTokenOfType(TokenType.FUNCTION);
      return reader.advance();
    }
    appendTokenOfType(TokenType.EQ);
    return next;
  }

  private int tokenizeExclamation(int next) {
    // ! !=
    next = reader.advance();
    if (next == '=') {
      appendTokenOfType(TokenType.BANG_EQ);
      return reader.advance();
    }
    appendTokenOfType(TokenType.BANG);
    return next;
  }

  private int tokenizeExponent(int next) {
    if (next == '+' || next == '-') {
      next = reader.advance();
    }
    boolean hasDigits = false;
    while (true) {
      if ('0' <= next && next <= '9') {
        hasDigits = true;
      } else {
        if (!hasDigits) {
          reportError(ScannerErrorCode.MISSING_DIGIT);
        }
        return next;
      }
      next = reader.advance();
    }
  }

  private int tokenizeFractionPart(int next, int start) {
    boolean done = false;
    boolean hasDigit = false;
    LOOP : while (!done) {
      if ('0' <= next && next <= '9') {
        hasDigit = true;
      } else if ('e' == next || 'E' == next) {
        hasDigit = true;
        next = tokenizeExponent(reader.advance());
        done = true;
        continue LOOP;
      } else {
        done = true;
        continue LOOP;
      }
      next = reader.advance();
    }
    if (!hasDigit) {
      appendStringToken(TokenType.INT, reader.getString(start, -2));
      if ('.' == next) {
        return selectWithOffset(
            '.',
            TokenType.PERIOD_PERIOD_PERIOD,
            TokenType.PERIOD_PERIOD,
            reader.getOffset() - 1);
      }
      appendTokenOfTypeWithOffset(TokenType.PERIOD, reader.getOffset() - 1);
      return bigSwitch(next);
    }
    appendStringToken(TokenType.DOUBLE, reader.getString(start, next < 0 ? 0 : -1));
    return next;
  }

  private int tokenizeGreaterThan(int next) {
    // > >= >> >>=
    next = reader.advance();
    if ('=' == next) {
      appendTokenOfType(TokenType.GT_EQ);
      return reader.advance();
    } else if ('>' == next) {
      next = reader.advance();
      if ('=' == next) {
        appendTokenOfType(TokenType.GT_GT_EQ);
        return reader.advance();
      } else {
        appendTokenOfType(TokenType.GT_GT);
        return next;
      }
    } else {
      appendTokenOfType(TokenType.GT);
      return next;
    }
  }

  private int tokenizeHex(int next) {
    int start = reader.getOffset() - 1;
    boolean hasDigits = false;
    while (true) {
      next = reader.advance();
      if (('0' <= next && next <= '9') || ('A' <= next && next <= 'F')
          || ('a' <= next && next <= 'f')) {
        hasDigits = true;
      } else {
        if (!hasDigits) {
          reportError(ScannerErrorCode.MISSING_HEX_DIGIT);
        }
        appendStringToken(TokenType.HEXADECIMAL, reader.getString(start, next < 0 ? 0 : -1));
        return next;
      }
    }
  }

  private int tokenizeHexOrNumber(int next) {
    int x = reader.peek();
    if (x == 'x' || x == 'X') {
      reader.advance();
      return tokenizeHex(x);
    }
    return tokenizeNumber(next);
  }

  private int tokenizeIdentifier(int next, int start, boolean allowDollar) {
    while (('a' <= next && next <= 'z') || ('A' <= next && next <= 'Z')
        || ('0' <= next && next <= '9') || next == '_' || (next == '$' && allowDollar)) {
      next = reader.advance();
    }
    appendStringToken(TokenType.IDENTIFIER, reader.getString(start, next < 0 ? 0 : -1));
    return next;
  }

  private int tokenizeInterpolatedExpression(int next, int start) {
    appendBeginToken(TokenType.STRING_INTERPOLATION_EXPRESSION);
    next = reader.advance();
    while (next != -1) {
      if (next == '}') {
        BeginToken begin = findTokenMatchingClosingBraceInInterpolationExpression();
        if (begin == null) {
          beginToken();
          appendTokenOfType(TokenType.CLOSE_CURLY_BRACKET);
          next = reader.advance();
          beginToken();
          return next;
        } else if (begin.getType() == TokenType.OPEN_CURLY_BRACKET) {
          beginToken();
          appendEndToken(TokenType.CLOSE_CURLY_BRACKET, TokenType.OPEN_CURLY_BRACKET);
          next = reader.advance();
          beginToken();
        } else if (begin.getType() == TokenType.STRING_INTERPOLATION_EXPRESSION) {
          beginToken();
          appendEndToken(TokenType.CLOSE_CURLY_BRACKET, TokenType.STRING_INTERPOLATION_EXPRESSION);
          next = reader.advance();
          beginToken();
          return next;
        }
      } else {
        next = bigSwitch(next);
      }
    }
    return next;
  }

  private int tokenizeInterpolatedIdentifier(int next, int start) {
    appendStringTokenWithOffset(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 0);
    if ((('A' <= next && next <= 'Z') || ('a' <= next && next <= 'z') || next == '_')) {
      beginToken();
      next = tokenizeKeywordOrIdentifier(next, false);
    }
    beginToken();
    return next;
  }

  private int tokenizeKeywordOrIdentifier(int next, boolean allowDollar) {
    KeywordState state = KeywordState.KEYWORD_STATE;
    int start = reader.getOffset();
    while (state != null && 'a' <= next && next <= 'z') {
      state = state.next((char) next);
      next = reader.advance();
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

  private int tokenizeLessThan(int next) {
    // < <= << <<=
    next = reader.advance();
    if ('=' == next) {
      appendTokenOfType(TokenType.LT_EQ);
      return reader.advance();
    } else if ('<' == next) {
      return select('=', TokenType.LT_LT_EQ, TokenType.LT_LT);
    } else {
      appendTokenOfType(TokenType.LT);
      return next;
    }
  }

  private int tokenizeMinus(int next) {
    // - -- -=
    next = reader.advance();
    if (next == '-') {
      appendTokenOfType(TokenType.MINUS_MINUS);
      return reader.advance();
    } else if (next == '=') {
      appendTokenOfType(TokenType.MINUS_EQ);
      return reader.advance();
    } else {
      appendTokenOfType(TokenType.MINUS);
      return next;
    }
  }

  private int tokenizeMultiLineComment(int next) {
    int nesting = 1;
    next = reader.advance();
    while (true) {
      if (-1 == next) {
        reportError(ScannerErrorCode.UNTERMINATED_MULTI_LINE_COMMENT);
        appendCommentToken(TokenType.MULTI_LINE_COMMENT, reader.getString(tokenStart, 0));
        return next;
      } else if ('*' == next) {
        next = reader.advance();
        if ('/' == next) {
          --nesting;
          if (0 == nesting) {
            appendCommentToken(TokenType.MULTI_LINE_COMMENT, reader.getString(tokenStart, 0));
            return reader.advance();
          } else {
            next = reader.advance();
          }
        }
      } else if ('/' == next) {
        next = reader.advance();
        if ('*' == next) {
          next = reader.advance();
          ++nesting;
        }
      } else if (next == '\r') {
        next = reader.advance();
        if (next == '\n') {
          next = reader.advance();
        }
        recordStartOfLine();
      } else if (next == '\n') {
        recordStartOfLine();
        next = reader.advance();
      } else {
        next = reader.advance();
      }
    }
  }

  private int tokenizeMultiLineRawString(int quoteChar, int start) {
    int next = reader.advance();
    outer : while (next != -1) {
      while (next != quoteChar) {
        next = reader.advance();
        if (next == -1) {
          break outer;
        } else if (next == '\r') {
          next = reader.advance();
          if (next == '\n') {
            next = reader.advance();
          }
          recordStartOfLine();
        } else if (next == '\n') {
          recordStartOfLine();
          next = reader.advance();
        }
      }
      next = reader.advance();
      if (next == quoteChar) {
        next = reader.advance();
        if (next == quoteChar) {
          appendStringToken(TokenType.STRING, reader.getString(start, 0));
          return reader.advance();
        }
      }
    }
    reportError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL);
    appendStringToken(TokenType.STRING, reader.getString(start, 0));
    return reader.advance();
  }

  private int tokenizeMultiLineString(int quoteChar, int start, boolean raw) {
    if (raw) {
      return tokenizeMultiLineRawString(quoteChar, start);
    }
    int next = reader.advance();
    while (next != -1) {
      if (next == '$') {
        appendStringToken(TokenType.STRING, reader.getString(start, -1));
        beginToken();
        next = tokenizeStringInterpolation(start);
        start = reader.getOffset();
        continue;
      }
      if (next == quoteChar) {
        next = reader.advance();
        if (next == quoteChar) {
          next = reader.advance();
          if (next == quoteChar) {
            appendStringToken(TokenType.STRING, reader.getString(start, 0));
            return reader.advance();
          }
        }
        continue;
      }
      if (next == '\\') {
        next = reader.advance();
        if (next == -1) {
          break;
        }
        if (next == '\r') {
          next = reader.advance();
          if (next == '\n') {
            next = reader.advance();
          }
          recordStartOfLine();
        } else if (next == '\n') {
          recordStartOfLine();
          next = reader.advance();
        } else {
          next = reader.advance();
        }
      } else if (next == '\r') {
        next = reader.advance();
        if (next == '\n') {
          next = reader.advance();
        }
        recordStartOfLine();
      } else if (next == '\n') {
        recordStartOfLine();
        next = reader.advance();
      } else {
        next = reader.advance();
      }
    }
    reportError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL);
    appendStringToken(TokenType.STRING, reader.getString(start, 0));
    return reader.advance();
  }

  private int tokenizeMultiply(int next) {
    // * *=
    return select('=', TokenType.STAR_EQ, TokenType.STAR);
  }

  private int tokenizeNumber(int next) {
    int start = reader.getOffset();
    while (true) {
      next = reader.advance();
      if ('0' <= next && next <= '9') {
        continue;
      } else if (next == '.') {
        return tokenizeFractionPart(reader.advance(), start);
      } else if (next == 'e' || next == 'E') {
        return tokenizeFractionPart(next, start);
      } else {
        appendStringToken(TokenType.INT, reader.getString(start, next < 0 ? 0 : -1));
        return next;
      }
    }
  }

  private int tokenizeOpenSquareBracket(int next) {
    // [ []  []=
    next = reader.advance();
    if (next == ']') {
      return select('=', TokenType.INDEX_EQ, TokenType.INDEX);
    } else {
      appendBeginToken(TokenType.OPEN_SQUARE_BRACKET);
      return next;
    }
  }

  private int tokenizePercent(int next) {
    // % %=
    return select('=', TokenType.PERCENT_EQ, TokenType.PERCENT);
  }

  private int tokenizePlus(int next) {
    // + ++ +=
    next = reader.advance();
    if ('+' == next) {
      appendTokenOfType(TokenType.PLUS_PLUS);
      return reader.advance();
    } else if ('=' == next) {
      appendTokenOfType(TokenType.PLUS_EQ);
      return reader.advance();
    } else {
      appendTokenOfType(TokenType.PLUS);
      return next;
    }
  }

  private int tokenizeSingleLineComment(int next) {
    while (true) {
      next = reader.advance();
      if (-1 == next) {
        appendCommentToken(TokenType.SINGLE_LINE_COMMENT, reader.getString(tokenStart, 0));
        return next;
      } else if ('\n' == next || '\r' == next) {
        appendCommentToken(TokenType.SINGLE_LINE_COMMENT, reader.getString(tokenStart, -1));
        return next;
      }
    }
  }

  private int tokenizeSingleLineRawString(int next, int quoteChar, int start) {
    next = reader.advance();
    while (next != -1) {
      if (next == quoteChar) {
        appendStringToken(TokenType.STRING, reader.getString(start, 0));
        return reader.advance();
      } else if (next == '\r' || next == '\n') {
        reportError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL);
        appendStringToken(TokenType.STRING, reader.getString(start, 0));
        return reader.advance();
      }
      next = reader.advance();
    }
    reportError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL);
    appendStringToken(TokenType.STRING, reader.getString(start, 0));
    return reader.advance();
  }

  private int tokenizeSingleLineString(int next, int quoteChar, int start) {
    while (next != quoteChar) {
      if (next == '\\') {
        next = reader.advance();
      } else if (next == '$') {
        appendStringToken(TokenType.STRING, reader.getString(start, -1));
        beginToken();
        next = tokenizeStringInterpolation(start);
        start = reader.getOffset();
        continue;
      }
      if (next <= '\r' && (next == '\n' || next == '\r' || next == -1)) {
        reportError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL);
        appendStringToken(TokenType.STRING, reader.getString(start, 0));
        return reader.advance();
      }
      next = reader.advance();
    }
    appendStringToken(TokenType.STRING, reader.getString(start, 0));
    return reader.advance();
  }

  private int tokenizeSlashOrComment(int next) {
    next = reader.advance();
    if ('*' == next) {
      return tokenizeMultiLineComment(next);
    } else if ('/' == next) {
      return tokenizeSingleLineComment(next);
    } else if ('=' == next) {
      appendTokenOfType(TokenType.SLASH_EQ);
      return reader.advance();
    } else {
      appendTokenOfType(TokenType.SLASH);
      return next;
    }
  }

  private int tokenizeString(int next, int start, boolean raw) {
    int quoteChar = next;
    next = reader.advance();
    if (quoteChar == next) {
      next = reader.advance();
      if (quoteChar == next) {
        // Multiline string.
        return tokenizeMultiLineString(quoteChar, start, raw);
      } else {
        // Empty string.
        appendStringToken(TokenType.STRING, reader.getString(start, -1));
        return next;
      }
    }
    if (raw) {
      return tokenizeSingleLineRawString(next, quoteChar, start);
    } else {
      return tokenizeSingleLineString(next, quoteChar, start);
    }
  }

  private int tokenizeStringInterpolation(int start) {
    beginToken();
    int next = reader.advance();
    if (next == '{') {
      return tokenizeInterpolatedExpression(next, start);
    } else {
      return tokenizeInterpolatedIdentifier(next, start);
    }
  }

  private int tokenizeTag(int next) {
    // # or #!.*[\n\r]
    if (reader.getOffset() == 0) {
      if (reader.peek() == '!') {
        do {
          next = reader.advance();
        } while (next != '\n' && next != '\r' && next > 0);
        appendStringToken(TokenType.SCRIPT_TAG, reader.getString(tokenStart, 0));
        return next;
      }
    }
    appendTokenOfType(TokenType.HASH);
    return reader.advance();
  }

  private int tokenizeTilde(int next) {
    // ~ ~/ ~/=
    next = reader.advance();
    if (next == '/') {
      return select('=', TokenType.TILDE_SLASH_EQ, TokenType.TILDE_SLASH);
    } else {
      appendTokenOfType(TokenType.TILDE);
      return next;
    }
  }
}

library angular.core.parser.lexer;

import 'package:angular/core/module.dart' show NgInjectableService;
import 'package:angular/core/parser/characters.dart';

class Token {
  final int index;
  final String text;

  var value;
  // Tokens should have one of these set.
  String opKey;
  String key;

  Token(this.index, this.text);

  withOp(op) {
    this.opKey = op;
  }

  withGetterSetter(key) {
    this.key = key;
  }

  withValue(value) { this.value = value; }

  toString() => "Token($text)";
}


@NgInjectableService()
class Lexer {
  List<Token> call(String text) {
    Scanner scanner = new Scanner(text);
    List<Token> tokens = [];
    Token token = scanner.scanToken();
    while (token != null) {
      tokens.add(token);
      token = scanner.scanToken();
    }
    return tokens;
  }
}

class Scanner {
  final String input;
  final int length;

  int peek = 0;
  int index = -1;

  Scanner(String input) : this.input = input, this.length = input.length {
    advance();
  }

  Token scanToken() {
    // Skip whitespace.
    while (peek <= $SPACE) {
      if (++index >= length) {
        peek = $EOF;
        return null;
      } else {
        peek = input.codeUnitAt(index);
      }
    }

    // Handle identifiers and numbers.
    if (isIdentifierStart(peek)) return scanIdentifier();
    if (isDigit(peek)) return scanNumber(index);

    int start = index;
    switch (peek) {
      case $PERIOD:
        advance();
        return isDigit(peek) ? scanNumber(start) : new Token(start, '.');
      case $LPAREN:
      case $RPAREN:
      case $LBRACE:
      case $RBRACE:
      case $LBRACKET:
      case $RBRACKET:
      case $COMMA:
      case $COLON:
      case $SEMICOLON:
        return scanCharacter(start, new String.fromCharCode(peek));
      case $SQ:
      case $DQ:
        return scanString();
      case $PLUS:
      case $MINUS:
      case $STAR:
      case $SLASH:
      case $PERCENT:
      case $CARET:
      case $QUESTION:
        return scanOperator(start, new String.fromCharCode(peek));
      case $LT:
      case $GT:
      case $BANG:
      case $EQ:
        return scanComplexOperator(start, $EQ, new String.fromCharCode(peek), '=');
      case $AMPERSAND:
        return scanComplexOperator(start, $AMPERSAND, '&', '&');
      case $BAR:
        return scanComplexOperator(start, $BAR, '|', '|');
      case $TILDE:
        return scanComplexOperator(start, $SLASH, '~', '/');
      case $NBSP:
        while (isWhitespace(peek)) advance();
        return scanToken();
    }

    String character = new String.fromCharCode(peek);
    error('Unexpected character [$character]');
    return null;
  }

  Token scanCharacter(int start, String string) {
    assert(peek == string.codeUnitAt(0));
    advance();
    return new Token(start, string);
  }

  Token scanOperator(int start, String string) {
    assert(peek == string.codeUnitAt(0));
    assert(OPERATORS.contains(string));
    advance();
    return new Token(start, string)..withOp(string);
  }

  Token scanComplexOperator(int start, int code, String one, String two) {
    assert(peek == one.codeUnitAt(0));
    advance();
    String string = one;
    if (peek == code) {
      advance();
      string += two;
    }
    assert(OPERATORS.contains(string));
    return new Token(start, string)..withOp(string);
  }

  Token scanIdentifier() {
    assert(isIdentifierStart(peek));
    int start = index;
    advance();
    while (isIdentifierPart(peek)) advance();
    String string = input.substring(start, index);
    Token result = new Token(start, string);
    // TODO(kasperl): Deal with null, undefined, true, and false in
    // a cleaner and faster way.
    if (OPERATORS.contains(string)) {
      result.withOp(string);
    } else {
      result.withGetterSetter(string);
    }
    return result;
  }

  Token scanNumber(int start) {
    assert(isDigit(peek));
    bool simple = (index == start);
    advance();  // Skip initial digit.
    while (true) {
      if (isDigit(peek)) {
        // Do nothing.
      } else if (peek == $PERIOD) {
        simple = false;
      } else if (isExponentStart(peek)) {
        advance();
        if (isExponentSign(peek)) advance();
        if (!isDigit(peek)) error('Invalid exponent', -1);
        simple = false;
      } else {
        break;
      }
      advance();
    }
    String string = input.substring(start, index);
    num value = simple ? int.parse(string) : double.parse(string);
    return new Token(start, string)..withValue(value);
  }

  Token scanString() {
    assert(peek == $SQ || peek == $DQ);
    int start = index;
    int quote = peek;
    advance();  // Skip initial quote.

    StringBuffer buffer;
    int marker = index;

    while (peek != quote) {
      if (peek == $BACKSLASH) {
        if (buffer == null) buffer = new StringBuffer();
        buffer.write(input.substring(marker, index));
        advance();
        int unescaped;
        if (peek == $u) {
          // TODO(kasperl): Check bounds? Make sure we have test
          // coverage for this.
          String hex = input.substring(index + 1, index + 5);
          unescaped = int.parse(hex, radix: 16, onError: (ignore) {
            error('Invalid unicode escape [\\u$hex]'); });
          for (int i = 0; i < 5; i++) advance();
        } else {
          unescaped = unescape(peek);
          advance();
        }
        buffer.writeCharCode(unescaped);
        marker = index;
      } else if (peek == $EOF) {
        error('Unterminated quote');
      } else {
        advance();
      }
    }

    String last = input.substring(marker, index);
    advance();  // Skip terminating quote.
    String string = input.substring(start, index);

    // Compute the unescaped string value.
    String unescaped = last;
    if (buffer != null) {
      buffer.write(last);
      unescaped = buffer.toString();
    }
    return new Token(start, string)..withValue(unescaped);
  }

  void advance() {
    if (++index >= length) peek = $EOF;
    else peek = input.codeUnitAt(index);
  }

  void error(String message, [int offset = 0]) {
    // TODO(kasperl): Try to get rid of the offset. It is only used to match
    // the error expectations in the lexer tests for numbers with exponents.
    int position = index + offset;
    throw "Lexer Error: $message at column $position in expression [$input]";
  }
}

Set<String> OPERATORS = new Set<String>.from([
    'undefined',
    'null',
    'true',
    'false',
    '+',
    '-',
    '*',
    '/',
    '~/',
    '%',
    '^',
    '=',
    '==',
    '!=',
    '<',
    '>',
    '<=',
    '>=',
    '&&',
    '||',
    '&',
    '|',
    '!',
    '?',
]);


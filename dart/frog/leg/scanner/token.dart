// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

final int EOF_TOKEN = 0;

final int KEYWORD_TOKEN = $k;
final int IDENTIFIER_TOKEN = $a;
final int DOUBLE_TOKEN = $d;
final int INT_TOKEN = $i;
final int HEXADECIMAL_TOKEN = $x;
final int STRING_TOKEN = $SQ;

final int AMPERSAND_TOKEN = $AMPERSAND;
final int BACKPING_TOKEN = $BACKPING;
final int BACKSLASH_TOKEN = $BACKSLASH;
final int BANG_TOKEN = $BANG;
final int BAR_TOKEN = $BAR;
final int COLON_TOKEN = $COLON;
final int COMMA_TOKEN = $COMMA;
final int EQ_TOKEN = $EQ;
final int GT_TOKEN = $GT;
final int HASH_TOKEN = $HASH;
final int OPEN_CURLY_BRACKET_TOKEN = $OPEN_CURLY_BRACKET;
final int OPEN_SQUARE_BRACKET_TOKEN = $OPEN_SQUARE_BRACKET;
final int LPAREN_TOKEN = $LPAREN;
// TODO(ahe): Clean this up. Adding 128 is safe because all the $FOO
// variables are below 127 (7bit ASCII).
final int LT_EQ_TOKEN = $LT + 128;
final int LT_TOKEN = $LT;
final int MINUS_TOKEN = $MINUS;
final int PERIOD_TOKEN = $PERIOD;
final int PLUS_TOKEN = $PLUS;
final int QUESTION_TOKEN = $QUESTION;
final int CLOSE_CURLY_BRACKET_TOKEN = $CLOSE_CURLY_BRACKET;
final int CLOSE_SQUARE_BRACKET_TOKEN = $CLOSE_SQUARE_BRACKET;
final int RPAREN_TOKEN = $RPAREN;
final int SEMICOLON_TOKEN = $SEMICOLON;
final int SLASH_TOKEN = $SLASH;
final int TILDE_TOKEN = $TILDE;
final int FUNCTION_TOKEN = $GT + 128;

final int UNKNOWN_TOKEN = 1024;

/**
 * A token that doubles as a linked list.
 */
class Token {
  final int kind;
  final int charOffset;
  Token next;

  Token(int this.kind, int this.charOffset);

  get value() => const SourceString('EOF');
  String get stringValue() => 'EOF';

  String toString() => new String.fromCharCodes([kind]);
}

/**
 * A keyword token.
 */
class KeywordToken extends Token {
  final Keyword value;
  String get stringValue() => value.syntax;

  KeywordToken(Keyword this.value, int charOffset)
    : super(KEYWORD_TOKEN, charOffset);

  String toString() => value.syntax;
}

/**
 * A String-valued token.
 */
class StringToken extends Token {
  final SourceString value;
  String get stringValue() => value.stringValue;

  StringToken(int kind, String value, int charOffset)
    : this.fromSource(kind, new SourceString(value), charOffset);

  StringToken.fromSource(int kind, SourceString this.value, int charOffset)
    : super(kind, charOffset);

  String toString() => value.toString();
}

interface SourceString extends Hashable factory StringWrapper {
  const SourceString(String string);

  void printOn(StringBuffer sb);

  String get stringValue();
}

class StringWrapper implements SourceString {
  final String stringValue;

  const StringWrapper(String this.stringValue);

  int hashCode() => stringValue.hashCode();

  bool operator ==(other) {
    return other is SourceString && toString() == other.toString();
  }

  void printOn(StringBuffer sb) {
    sb.add(stringValue);
  }

  String toString() => stringValue;
}

class BeginGroupToken extends StringToken {
  Token endGroup;
  BeginGroupToken(int kind, String value, int charOffset)
    : super(kind, value, charOffset);
}

// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A keyword in the Dart programming language.
 */
class Keyword implements SourceString {
  // TODO(ahe): Remove "false, KEYWORD_INFO" which works around a Frog bug.
  static final Keyword BREAK = const Keyword("break", false, KEYWORD_INFO);
  static final Keyword CASE = const Keyword("case", false, KEYWORD_INFO);
  static final Keyword CATCH = const Keyword("catch", false, KEYWORD_INFO);
  static final Keyword CLASS = const Keyword("class", false, KEYWORD_INFO);
  static final Keyword CONST = const Keyword("const", false, KEYWORD_INFO);
  static final Keyword CONTINUE = const Keyword("continue", false,
                                                KEYWORD_INFO);
  static final Keyword DEFAULT = const Keyword("default", false, KEYWORD_INFO);
  static final Keyword DO = const Keyword("do", false, KEYWORD_INFO);
  static final Keyword ELSE = const Keyword("else", false, KEYWORD_INFO);
  static final Keyword EXTENDS = const Keyword("extends", false, KEYWORD_INFO);
  static final Keyword FALSE = const Keyword("false", false, KEYWORD_INFO);
  static final Keyword FINAL = const Keyword("final", false, KEYWORD_INFO);
  static final Keyword FINALLY = const Keyword("finally", false, KEYWORD_INFO);
  static final Keyword FOR = const Keyword("for", false, KEYWORD_INFO);
  static final Keyword IF = const Keyword("if", false, KEYWORD_INFO);
  static final Keyword IN = const Keyword("in", false, KEYWORD_INFO);
  static final Keyword IS = const Keyword("is", false, IS_INFO);
  static final Keyword NEW = const Keyword("new", false, KEYWORD_INFO);
  static final Keyword NULL = const Keyword("null", false, KEYWORD_INFO);
  static final Keyword RETURN = const Keyword("return", false, KEYWORD_INFO);
  static final Keyword SUPER = const Keyword("super", false, KEYWORD_INFO);
  static final Keyword SWITCH = const Keyword("switch", false, KEYWORD_INFO);
  static final Keyword THIS = const Keyword("this", false, KEYWORD_INFO);
  static final Keyword THROW = const Keyword("throw", false, KEYWORD_INFO);
  static final Keyword TRUE = const Keyword("true", false, KEYWORD_INFO);
  static final Keyword TRY = const Keyword("try", false, KEYWORD_INFO);
  static final Keyword VAR = const Keyword("var", false, KEYWORD_INFO);
  static final Keyword VOID = const Keyword("void", false, KEYWORD_INFO);
  static final Keyword WHILE = const Keyword("while", false, KEYWORD_INFO);

  // Pseudo keywords:
  static final Keyword ABSTRACT = const Keyword("abstract", true, KEYWORD_INFO);
  static final Keyword ASSERT = const Keyword("assert", true, KEYWORD_INFO);
  static final Keyword FACTORY = const Keyword("factory", true, KEYWORD_INFO);
  static final Keyword GET = const Keyword("get", true, KEYWORD_INFO);
  static final Keyword IMPLEMENTS = const Keyword("implements", true,
                                                  KEYWORD_INFO);
  static final Keyword IMPORT = const Keyword("import", true, KEYWORD_INFO);
  static final Keyword INTERFACE = const Keyword("interface", true,
                                                 KEYWORD_INFO);
  static final Keyword LIBRARY = const Keyword("library", true, KEYWORD_INFO);
  static final Keyword NATIVE = const Keyword("native", true, KEYWORD_INFO);
  static final Keyword NEGATE = const Keyword("negate", true, KEYWORD_INFO);
  static final Keyword OPERATOR = const Keyword("operator", true, KEYWORD_INFO);
  static final Keyword SET = const Keyword("set", true, KEYWORD_INFO);
  static final Keyword SOURCE = const Keyword("source", true, KEYWORD_INFO);
  static final Keyword STATIC = const Keyword("static", true, KEYWORD_INFO);
  static final Keyword TYPEDEF = const Keyword("typedef", true, KEYWORD_INFO);

  static final List<Keyword> values = const <Keyword> [
      BREAK,
      CASE,
      CATCH,
      CONST,
      CONTINUE,
      DEFAULT,
      DO,
      ELSE,
      FALSE,
      FINAL,
      FINALLY,
      FOR,
      IF,
      IN,
      IS,
      NEW,
      NULL,
      RETURN,
      SUPER,
      SWITCH,
      THIS,
      THROW,
      TRUE,
      TRY,
      VAR,
      VOID,
      WHILE,
      ABSTRACT,
      ASSERT,
      CLASS,
      EXTENDS,
      FACTORY,
      GET,
      IMPLEMENTS,
      IMPORT,
      INTERFACE,
      LIBRARY,
      NATIVE,
      NEGATE,
      OPERATOR,
      SET,
      SOURCE,
      STATIC,
      TYPEDEF ];

  final String syntax;
  final bool isPseudo;
  final PrecedenceInfo info;

  static Map<String, Keyword> _keywords;
  static Map<String, Keyword> get keywords() {
    if (_keywords === null) {
      _keywords = computeKeywordMap();
    }
    return _keywords;
  }

  const Keyword(String this.syntax,
                [bool this.isPseudo = false,
                 PrecedenceInfo this.info = KEYWORD_INFO]);

  static Map<String, Keyword> computeKeywordMap() {
    Map<String, Keyword> result = new LinkedHashMap<String, Keyword>();
    for (Keyword keyword in values) {
      result[keyword.syntax] = keyword;
    }
    return result;
  }

  int hashCode() => syntax.hashCode();

  bool operator ==(other) {
    return other is SourceString && toString() == other.toString();
  }

  void printOn(StringBuffer sb) {
    sb.add(syntax);
  }

  String toString() => syntax;
  String get stringValue() => syntax;
}

/**
 * Abstract state in a state machine for scanning keywords.
 */
class KeywordState {
  abstract bool isLeaf();
  abstract KeywordState next(int c);
  abstract Keyword get keyword();

  static KeywordState _KEYWORD_STATE;
  static KeywordState get KEYWORD_STATE() {
    if (_KEYWORD_STATE === null) {
      List<String> strings = new List<String>(Keyword.values.length);
      for (int i = 0; i < Keyword.values.length; i++) {
        strings[i] = Keyword.values[i].syntax;
      }
      strings.sort((a,b) => a.compareTo(b));
      _KEYWORD_STATE = computeKeywordStateTable(0, strings, 0, strings.length);
    }
    return _KEYWORD_STATE;
  }

  static KeywordState computeKeywordStateTable(int start, List<String> strings,
                                               int offset, int length) {
    List<KeywordState> result = new List<KeywordState>(26);
    assert(length != 0);
    int chunk = 0;
    int chunkStart = -1;
    bool isLeaf = false;
    for (int i = offset; i < offset + length; i++) {
      if (strings[i].length == start) {
        isLeaf = true;
      }
      if (strings[i].length > start) {
        int c = strings[i].charCodeAt(start);
        if (chunk != c) {
          if (chunkStart != -1) {
            assert(result[chunk - $a] === null);
            result[chunk - $a] = computeKeywordStateTable(start + 1, strings,
                                                          chunkStart,
                                                          i - chunkStart);
          }
          chunkStart = i;
          chunk = c;
        }
      }
    }
    if (chunkStart != -1) {
      assert(result[chunk - $a] === null);
      result[chunk - $a] =
        computeKeywordStateTable(start + 1, strings, chunkStart,
                                 offset + length - chunkStart);
    } else {
      assert(length == 1);
      return new LeafKeywordState(strings[offset]);
    }
    if (isLeaf) {
      return new ArrayKeywordState(result, strings[offset]);
    } else {
      return new ArrayKeywordState(result, null);
    }
  }
}

/**
 * A state with multiple outgoing transitions.
 */
class ArrayKeywordState extends KeywordState {
  final List<KeywordState> table;
  final Keyword keyword;

  ArrayKeywordState(List<KeywordState> this.table, String syntax)
    : keyword = (syntax === null) ? null : Keyword.keywords[syntax];

  bool isLeaf() => false;

  KeywordState next(int c) => table[c - $a];

  String toString() {
    StringBuffer sb = new StringBuffer();
    sb.add("[");
    if (keyword !== null) {
      sb.add("*");
      sb.add(keyword);
      sb.add(" ");
    }
    List<KeywordState> foo = table;
    for (int i = 0; i < foo.length; i++) {
      if (foo[i] != null) {
        sb.add("${new String.fromCharCodes([i + $a])}: ${foo[i]}; ");
      }
    }
    sb.add("]");
    return sb.toString();
  }
}

/**
 * A state that has no outgoing transitions.
 */
class LeafKeywordState extends KeywordState {
  final Keyword keyword;

  LeafKeywordState(String syntax) : keyword = Keyword.keywords[syntax];

  bool isLeaf() => true;

  KeywordState next(int c) => null;

  String toString() => keyword.syntax;
}

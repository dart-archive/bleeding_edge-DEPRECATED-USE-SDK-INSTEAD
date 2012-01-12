# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

class Token:
  def __init__(self, name, text, precedence, customFinishCode=None,
               stringRepr=None):
    self.name = name
    self.text = text
    self.precedence = precedence
    self.customFinishCode = customFinishCode

    if stringRepr is None:
      if self.text:
        stringRepr = self.text
      elif self.name is not None:
        stringRepr = self.name.lower().replace('_', ' ')

    self.stringRepr = stringRepr

    # roughly the user-definable operators
    EXCLUDES = ['!=', '===', '!==', '&&', '||']
    INCLUDES = ['[]', '[]=', '~']
    if text in INCLUDES or (self.precedence >= 6 and text not in EXCLUDES):
      self.methodName = ':' + name.lower()
    else:
      self.methodName = None

  def getFinishCode(self):
    if self.customFinishCode:
      return self.customFinishCode
    else:
      return '_finishToken(TokenKind.%s)' % self.name


class Keyword:
  def __init__(self, name, text, isPseudo, precedence=0):
    self.name = name
    self.text = text
    self.isPseudo = isPseudo
    self.precedence = precedence

    if isPseudo:
      prefix = 'pseudo-'
    else:
      prefix = ''

    self.stringRepr = '%skeyword %r' % (prefix, self.text)

  def __repr__(self):
    return 'Keyword(%r)' % self.text


tokens = [
  Token('END_OF_FILE', "", 0),

  Token('LPAREN', "(", 0),
  Token('RPAREN', ")", 0),
  Token('LBRACK', "[", 0),
  Token('RBRACK', "]", 0),
  Token('LBRACE', "{", 0, '_finishOpenBrace()'),
  Token('RBRACE', "}", 0, '_finishCloseBrace()'),
  Token('COLON',  ":", 0),
  Token('ARROW', "=>", 0),
  Token('SEMICOLON', ";", 0),
  Token('COMMA', ",", 0),
  Token('HASH', "#", 0),
  Token('HASHBANG', "#!", 0, 'finishHashBang()'),

  Token('DOT', ".", 0, 'finishDot()'),
  Token('ELLIPSIS', "...", 0),
  Token('INCR',   "++", 0),
  Token('DECR',   "--", 0),

  Token('BIT_NOT', "~", 0),
  Token('NOT', "!", 0),

  # Assignment operators
  Token('ASSIGN', "=", 2),
  Token('ASSIGN_OR', "|=", 2),
  Token('ASSIGN_XOR', "^=", 2),
  Token('ASSIGN_AND', "&=", 2),
  Token('ASSIGN_SHL', "<<=", 2),
  Token('ASSIGN_SAR', ">>=", 2),
  Token('ASSIGN_SHR', ">>>=", 2),
  Token('ASSIGN_ADD', "+=", 2),
  Token('ASSIGN_SUB', "-=", 2),
  Token('ASSIGN_MUL', "*=", 2),
  Token('ASSIGN_DIV', "/=", 2),
  Token('ASSIGN_TRUNCDIV', "~/=", 2),
  Token('ASSIGN_MOD', "%=", 2),

  Token('CONDITIONAL', "?", 3),

  Token('OR', "||", 4),
  Token('AND', "&&", 5),
  Token('BIT_OR', "|", 6),
  Token('BIT_XOR', "^", 7),
  Token('BIT_AND', "&", 8),

  # Shift operators
  Token('SHL', "<<", 11),
  Token('SAR', ">>", 11),
  Token('SHR', ">>>", 11),

  # Additive operators
  Token('ADD', "+", 12),
  Token('SUB', "-", 12),

  # Multiplicative operators
  Token('MUL', "*", 13),
  Token('DIV', "/", 13),
  Token('TRUNCDIV', "~/", 13),
  Token('MOD', "%", 13),

  # Equality operators
  Token('EQ', "==", 9),
  Token('NE', "!=", 9),
  Token('EQ_STRICT', "===", 9),
  Token('NE_STRICT', "!==", 9),

  # Relational operators
  Token('LT', "<", 10),
  Token('GT', ">", 10),
  Token('LTE', "<=", 10),
  Token('GTE', ">=", 10),

  # Special tokens for index operator methods
  Token('INDEX', "[]", 0),
  Token('SETINDEX', "[]=", 0),


  Token('STRING', "", 0),
  Token('STRING_PART', "", 0),
  Token('INTEGER', "", 0),
  Token('HEX_INTEGER', "", 0),
  Token('DOUBLE', "", 0),

  Token('WHITESPACE', "", 0),
  Token('COMMENT', "", 0),
  Token('ERROR', "", 0),

  Token('INCOMPLETE_STRING', "", 0),
  Token('INCOMPLETE_COMMENT', "", 0),
  Token('INCOMPLETE_MULTILINE_STRING_DQ', "", 0),
  Token('INCOMPLETE_MULTILINE_STRING_SQ', "", 0),


  Token(None, '//', 0, 'finishSingleLineComment()'),
  Token(None, '/*', 0, 'finishMultiLineComment()'),
  Token(None, '$"', 0, 'finishString(34/*"*/)'),
  Token(None, '$\'', 0, 'finishString(39/*\'*/)'),
  Token(None, '$', 0, 'finishIdentifier(36/*$*/)'),
  Token(None, '@"', 0, 'finishRawString(34/*"*/)'),
  Token(None, '@\'', 0, 'finishRawString(39/*\'*/)'),
  Token(None, '"', 0, 'finishString(34/*"*/)'),
  Token(None, '\'', 0, 'finishString(39/*\'*/)'),
  Token(None, '0', 0, 'finishNumber()'),
  Token(None, '0x', 0, 'finishHex()'),
  Token(None, '0X', 0, 'finishHex()'),

  Token('IDENTIFIER', "", 0), # must be last

  ]

# TODO(jimhug): Validate this list with Dart.g and with the lang spec.
keywords = [
  Keyword('ABSTRACT', "abstract", True),
  Keyword('ASSERT', "assert", True),
  Keyword('AWAIT', "await", False), # experimental feature
  Keyword('BREAK', "break", False),
  Keyword('CALL', "call", True),
  Keyword('CASE', "case", False),
  Keyword('CATCH', "catch", False),
  Keyword('CLASS', "class", False),
  Keyword('CONST', "const", False),
  Keyword('CONTINUE', "continue", False),
  Keyword('DEFAULT', "default", False),
  Keyword('DO', "do", False),
  Keyword('ELSE', "else", False),
  Keyword('EXTENDS', "extends", False),
  Keyword('FACTORY', "factory", True),
  Keyword('FALSE', "false", False),
  Keyword('FINAL', "final", False),
  Keyword('FINALLY', "finally", False),
  Keyword('FOR', "for", False),
  Keyword('GET', "get", True),
  Keyword('IF', "if", False),
  Keyword('IMPLEMENTS', "implements", True),
  Keyword('IMPORT', "import", True),
  Keyword('IN', "in", False),
  Keyword('INTERFACE', "interface", True),
  Keyword('IS', "is", False, 10),
  Keyword('LIBRARY', "library", True),
  Keyword('NATIVE', "native", True),
  Keyword('NEGATE', "negate", True),
  Keyword('NEW', "new", False),
  Keyword('NULL', "null", False),
  Keyword('OPERATOR', "operator", True),
  Keyword('RETURN', "return", False),
  Keyword('SET', "set", True),
  Keyword('SOURCE', "source", True),
  Keyword('STATIC', "static", True),
  Keyword('SUPER', "super", False),
  Keyword('SWITCH', "switch", False),
  Keyword('THIS', "this", False),
  Keyword('THROW', "throw", False),
  Keyword('TRUE', "true", False),
  Keyword('TYPEDEF', "typedef", True),
  Keyword('TRY', "try", False),
  Keyword('VAR', "var", False),
  Keyword('VOID', "void", False),
  Keyword('WHILE', "while", False)]

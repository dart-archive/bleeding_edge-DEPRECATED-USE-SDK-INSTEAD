#!/usr/bin/env python
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

'''Generates the Tokenizer class into tokenenizer.g.dart.'''

import re
from token_info import tokens, keywords
from codegen import CodeWriter, HEADER

def makeSafe(ch):
  ch_s = ch
  if ch in ' \t\n\r*/': ch_s = repr(ch)
  return '%d/*%s*/' % (ord(ch), ch_s)


class Case:
  def __init__(self, ch, token, includeWhitespace=False):
    self.ch = ch
    self.cases = {}
    self.token = None
    self.includeWhitespace = includeWhitespace
    if len(ch) > 0:
      self.cases[ch[0]] = Case(ch[1:], token)
    else:
      self.token = token

  def addCase(self, ch, token):
    if len(ch) == 0:
      self.token = token
    else:
      searchChar = ch[0]
      if self.cases.has_key(searchChar):
        self.cases[searchChar].addCase(ch[1:], token)
      else:
        self.cases[searchChar] = Case(ch[1:], token)

  def defaultReturn(self):
    if self.token is not None:
      return 'return %s;' % self.token.getFinishCode()
    else:
      return 'return _errorToken();'

  def writeCases(self, cw):
    ret = []
    if len(self.cases) == 0:
      cw.writeln(self.defaultReturn())
    elif len(self.cases) < 4 and not self.includeWhitespace:
      optElse = ''
      for key, case in sorted(self.cases.items()):
        cw.enterBlock('%sif (_maybeEatChar(%s)) {' % (optElse, makeSafe(key)))
        case.writeCases(cw)
        cw.exitBlock()
        optElse = '} else '
      cw.enterBlock('} else {')
      cw.writeln(self.defaultReturn())

      cw.exitBlock('}')
    else:
      cw.writeln('ch = _nextChar();')
      cw.enterBlock('switch(ch) {')
      if self.includeWhitespace:
        self.writeWhitespace(cw)
      for key, case in sorted(self.cases.items()):
        cw.enterBlock('case %s:' % makeSafe(key))

        case.writeCases(cw)
        cw.exitBlock()
      if self.includeWhitespace:
        cw.enterBlock('default:')
        cw.enterBlock('if (TokenizerHelpers.isIdentifierStart(ch)) {')
        cw.writeln('return this.finishIdentifier(ch);')
        cw.exitBlock('} else if (TokenizerHelpers.isDigit(ch)) {')
        cw.enterBlock()
        cw.writeln('return this.finishNumber();')
        cw.exitBlock('} else {')
        cw.enterBlock()
        cw.writeln(self.defaultReturn())
        cw.exitBlock('}')
      else:
        cw.writeln('default: ' + self.defaultReturn())
      cw.exitBlock('}')

  def writeWhitespace(self, cw):
    cw.writeln('case 0: return _finishToken(TokenKind.END_OF_FILE);')
    cw.enterBlock(r"case %s: case %s: case %s: case %s:" %
      tuple([makeSafe(ch) for ch in ' \t\n\r']))
    cw.writeln('return finishWhitespace();')
    cw.exitBlock()

def computeCases():
  top = Case('', None, True)
  for tok in tokens:
    #print tok.text
    if tok.text != '':
      top.addCase(tok.text, tok)
  return top

cases = computeCases()

TOKENIZER = '''
/** A generated file that extends the hand coded methods in TokenizerBase. */
class Tokenizer extends TokenizerBase {

  Tokenizer(SourceFile source, bool skipWhitespace, [int index = 0])
    : super(source, skipWhitespace, index);

  Token next() {
    // keep track of our starting position
    _startIndex = _index;

    if (_interpStack != null && _interpStack.depth == 0) {
      var istack = _interpStack;
      _interpStack = _interpStack.pop();
      if (istack.isMultiline) {
        return finishMultilineString(istack.quote);
      } else {
        return finishStringBody(istack.quote);
      }
    }

    int ch;
%(cases)s
  }

%(extraMethods)s
}

/** Static helper methods. */
class TokenizerHelpers {
%(helperMethods)s
}
'''



def charAsInt(ch):
  return '%d/*%r*/' % (ord(ch), ch)

class CharTest:
  def __init__(self, fromChar, toChar=None):
    self.fromChar = fromChar
    self.toChar = toChar

  def toCode(self):
    if self.toChar is None:
      return 'c == %s' % makeSafe(self.fromChar)
    else:
      return '(c >= %s && c <= %s)' % (
        makeSafe(self.fromChar), makeSafe(self.toChar))

class OrTest:
  def __init__(self, *args):
    self.tests = args

  def toCode(self):
    return '(' + ' || '.join([test.toCode() for test in self.tests]) + ')'

class ExplicitTest:
  def __init__(self, text):
    self.text = text

  def toCode(self):
    return self.text


def writeClass(cw, name, test):
  cw.enterBlock('static bool is%s(int c) {' % name)
  cw.writeln('return %s;' % test.toCode())
  cw.exitBlock('}')
  cw.writeln()

# TODO(jimhug): if (_restMatches(_text, i0+1, 'ase')) would be good!
class LengthGroup:
  def __init__(self, length):
    self.length = length
    self.kws = []

  def add(self, kw):
    self.kws.append(kw)

  def writeCode(self, cw):
    cw.enterBlock('case %d:' % self.length)
    self.writeTests(cw, self.kws)
    cw.writeln('return TokenKind.IDENTIFIER;')
    cw.exitBlock()


  def writeTests(self, cw, kws, index=0):
    if len(kws) == 1:
      kw = kws[0].text
      if index == len(kw):
        cw.writeln('return TokenKind.%s;' % (kws[0].name))
      else:
        clauses = [
            "_text.charCodeAt(%s) == %s" % (
                makeIndex('i0', i), makeSafe(kw[i]))
            for i in range(index, len(kw))]
        test = 'if (%s) return TokenKind.%s;' % (
            ' && '.join(clauses), kws[0].name)
        cw.writeln(test)
    else:
      starts = {}
      for kw in kws:
        c0 = kw.text[index]
        if not starts.has_key(c0):
          starts[c0] = []
        starts[c0].append(kw)

      cw.writeln('ch = _text.charCodeAt(%s);' % makeIndex('i0', index))
      prefix = ''
      for key, value in sorted(starts.items()):
        cw.enterBlock('%sif (ch == %s) {' % (prefix, makeSafe(key)))
        #cw.writeln(repr(value))
        self.writeTests(cw, value, index+1)
        cw.exitBlock()
        prefix = '} else '
      cw.writeln('}')
      #cw.writeln(repr(kws))

  def __str__(self):
    return '%d: %r' % (self.length, self.kws)

def makeIndex(index, offset):
  if offset == 0:
    return index
  else:
    return '%s+%d' % (index, offset)

def writeHelperMethods(cw):
  cw.enterBlock()
  cw.writeln()
  writeClass(cw, 'IdentifierStart', OrTest(
    CharTest('a', 'z'), CharTest('A', 'Z'), CharTest('_'))) #TODO: CharTest('$')
  writeClass(cw, 'Digit', CharTest('0', '9'))
  writeClass(cw, 'HexDigit', OrTest(
    ExplicitTest('isDigit(c)'), CharTest('a', 'f'), CharTest('A', 'F')))
  writeClass(cw, 'Whitespace', OrTest(
    CharTest(' '), CharTest('\t'), CharTest('\n'), CharTest('\r')))
  writeClass(cw, 'IdentifierPart', OrTest(
    ExplicitTest('isIdentifierStart(c)'),
    ExplicitTest('isDigit(c)'),
    CharTest('$')))
  # This is like IdentifierPart, but without $
  writeClass(cw, 'InterpIdentifierPart', OrTest(
    ExplicitTest('isIdentifierStart(c)'),
    ExplicitTest('isDigit(c)')))

def writeExtraMethods(cw):
  lengths = {}
  for kw in keywords:
    l = len(kw.text)
    if not lengths.has_key(l):
      lengths[l] = LengthGroup(l)
    lengths[l].add(kw)

  # TODO(jimhug): Consider merging this with the finishIdentifier code.
  cw.enterBlock()
  cw.enterBlock('int getIdentifierKind() {')
  cw.writeln('final i0 = _startIndex;')
  cw.writeln('int ch;')
  cw.enterBlock('switch (_index - i0) {')
  for key, value in sorted(lengths.items()):
    value.writeCode(cw)
  cw.writeln('default: return TokenKind.IDENTIFIER;')
  cw.exitBlock('}')
  cw.exitBlock('}')

def makeSafe1(match):
  return makeSafe(match.group(1))

def main():
  cw = CodeWriter(__file__)
  cw._indent += 2;
  cases.writeCases(cw)
  casesCode = str(cw)

  cw = CodeWriter(__file__)
  writeExtraMethods(cw)
  extraMethods = str(cw)

  cw = CodeWriter(__file__)
  writeHelperMethods(cw)
  helperMethods = str(cw)

  out = open('tokenizer.g.dart', 'w')
  out.write(HEADER % __file__)
  pat = re.compile('@(.)', re.DOTALL)
  text = pat.sub(makeSafe1, TOKENIZER)
  out.write(text % {
      'cases': casesCode,
      'extraMethods': extraMethods,
      'helperMethods': helperMethods })
  out.close()


if __name__ == '__main__': main()

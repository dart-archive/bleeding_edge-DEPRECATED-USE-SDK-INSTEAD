# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
from token_info import tokens, keywords
from codegen import CodeWriter

# TODO(jimhug): Compare perf of the switch pattern used here with maps and ifs

def main():
  '''Generates the TokenKind class into token_kind.g.dart.'''
  cw = CodeWriter(__file__)
  cw.enterBlock('class TokenKind {')
  index = 1

  trueKeywords = [kw for kw in keywords if not kw.isPseudo]
  pseudoKeywords = [kw for kw in keywords if kw.isPseudo]
  allTokens = tokens + pseudoKeywords + trueKeywords

  for tok in allTokens:
    if tok.name is None: continue
    tok.index = index
    index += 1
    cw.writeln('/** [TokenKind] representing %s tokens. */', tok.stringRepr)
    cw.writeln('static final int %s = %d;', tok.name, tok.index)
    cw.writeln('')

  cw.enterBlock('static String kindToString(int kind) {')
  cw.writeln('switch(kind) {')
  cw.enterBlock()

  for tok in allTokens:
    if tok.name is None: continue
    cw.writeln('case TokenKind.%s: return "%s";', tok.name, tok.stringRepr)

  cw.writeln('default: return "TokenKind(" + kind.toString() + ")";')
  cw.exitBlock('}')
  cw.exitBlock('}')

  cw.writeln()

  cw.enterBlock('static bool isIdentifier(int kind) {')
  cw.writeln('return kind >= IDENTIFIER && kind < %s;' % trueKeywords[0].name)
  cw.exitBlock('}')

  cw.writeln()

  cw.enterBlock('static int infixPrecedence(int kind) {')
  cw.enterBlock('switch(kind) {')
  for tok in tokens + keywords:
    if tok.precedence > 0:
      cw.writeln('case %s: return %d;' % (tok.name, tok.precedence))


  cw.writeln('default: return -1;')
  cw.exitBlock('}')
  cw.exitBlock('}')

  cw.writeln()
  cw.enterBlock('static String rawOperatorFromMethod(String name) {')
  cw.enterBlock('switch(name) {')
  for tok in tokens:
    if tok.methodName is not None:
      cw.writeln('case %r: return %r;' % (tok.methodName, tok.text))
  cw.writeln("case ':ne': return '!=';");
  cw.exitBlock('}')
  cw.exitBlock('}')

  cw.writeln()
  cw.enterBlock('static String binaryMethodName(int kind) {')
  cw.enterBlock('switch(kind) {')
  for tok in tokens:
    if tok.methodName is not None:
      dname = repr(tok.methodName).replace('$', '\\$')
      cw.writeln('case %s: return %s;' % (tok.name, dname))
  cw.exitBlock('}')
  cw.exitBlock('}')

  cw.writeln()
  cw.enterBlock('static String unaryMethodName(int kind) {')

  cw.exitBlock('}')

  cw.writeln()
  cw.enterBlock('static int kindFromAssign(int kind) {')
  cw.writeln('if (kind == ASSIGN) return 0;')
  cw.enterBlock('if (kind > ASSIGN && kind <= ASSIGN_MOD) {')
  cw.writeln('return kind + (ADD - ASSIGN_ADD);')
  cw.exitBlock('}')
  cw.writeln('return -1;')
  cw.exitBlock('}')



  cw.exitBlock('}')

  cw.writeToFile('token_kind')

if __name__ == '__main__': main()

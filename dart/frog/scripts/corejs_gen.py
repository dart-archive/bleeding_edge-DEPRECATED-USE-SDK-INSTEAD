# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
from token_info import tokens, keywords
from codegen import CodeWriter

EXCLUDES = ['$add']

def main():
  '''Generates the TokenKind class into token_kind.g.dart.'''
  cw = CodeWriter(__file__)
  for tok in tokens:
    if tok.methodName is not None and tok.methodName not in EXCLUDES:
      dname = repr(tok.methodName).replace('$', '\\$')
      cw.enterBlock('function %s(x, y) {' % tok.methodName)
      cw.writeln("return (typeof(x) == 'number' && typeof(y) == 'number')")
      cw.writeln('  ? x %s y : x.%s(y);' % (tok.text, tok.methodName))
      cw.exitBlock('}')

  cw.writeToFile('tests/core.g.js')

if __name__ == '__main__': main()

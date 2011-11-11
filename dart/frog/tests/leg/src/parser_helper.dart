// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('parser_helper');

#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import('../../../leg/scanner/scannerlib.dart');
#import("../../../leg/leg.dart");

class LoggerCanceler implements Logger, Canceler {
  void cancel([String reason]) {
    throw new CompilerCancelledException(reason);
  }

  void log(message) {
    // print(message);
  }
}

Token scan(String text) => new StringScanner(text).tokenize();

Node parseBodyCode(String text, Function parseMethod) {
  Token tokens = scan(text);
  LoggerCanceler lc = new LoggerCanceler();
  NodeListener listener = new NodeListener(lc, lc);
  Parser parser = new Parser(listener);
  Token endToken = parseMethod(parser, tokens);
  assert(endToken.kind == EOF_TOKEN);
  return listener.popNode();
}

Node parseStatement(String text) =>
  parseBodyCode(text, (parser, tokens) => parser.parseStatement(tokens));

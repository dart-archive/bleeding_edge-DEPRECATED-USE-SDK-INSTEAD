// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('parser_helper');

#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import('../../../leg/scanner/scannerlib.dart');
#import("../../../leg/leg.dart");
#import("../../../leg/util/util.dart");

class LoggerCanceler implements Logger, Canceler {
  void cancel([String reason, node, token, instruction]) {
    throw new CompilerCancelledException(reason);
  }

  void log(message) {
    print(message);
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
  Node node = listener.popNode();
  Expect.isNotNull(node);
  Expect.isTrue(listener.nodes.isEmpty(), 'Not empty: ${listener.nodes}');
  return node;
}

Node parseStatement(String text) =>
  parseBodyCode(text, (parser, tokens) => parser.parseStatement(tokens));

Node parseFunction(String text, Compiler compiler) {
  Element element = parseUnit(text, compiler).head;
  Expect.equals(ElementKind.FUNCTION, element.kind);
  compiler.universe.define(element);
  return element.parseNode(compiler, compiler);
}

Link<Element> parseUnit(String text, Compiler compiler) {
  Token tokens = scan(text);
  ElementListener listener = new ElementListener(compiler);
  PartialParser parser = new PartialParser(listener);
  parser.parseUnit(tokens);
  return listener.topLevelElements;
}

// TODO(ahe): We define this method to avoid having to import
// the scanner in the tests. We should move SourceString to another
// location instead.
SourceString buildSourceString(String name) {
  return new SourceString(name);
}

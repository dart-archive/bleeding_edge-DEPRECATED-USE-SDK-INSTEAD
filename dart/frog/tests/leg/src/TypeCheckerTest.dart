// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("../../../leg/leg.dart");
#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import('../../../leg/scanner/scannerlib.dart');
#import("../../../leg/util/util.dart");
#import("parser_helper.dart");

main() {
  testSimpleTypes();
  testReturn();
  testFor();
  testWhile();
  testOperators();
}

testSimpleTypes() {
  setup();
  Expect.equals(types.intType, analyzeType("3"));
  Expect.equals(types.boolType, analyzeType("false"));
  Expect.equals(types.boolType, analyzeType("true"));
  Expect.equals(types.stringType, analyzeType("'hestfisk'"));
}

testReturn() {
  setup();
  analyzeTopLevel("void foo() { return 3; }", MessageKind.RETURN_VALUE_IN_VOID);
  analyzeTopLevel("int main() { return 'hest'; }", MessageKind.NOT_ASSIGNABLE);
  analyzeTopLevel("void main() { var x; return x; }");
  analyzeTopLevel(returnWithType("int", "'string'"),
                  MessageKind.NOT_ASSIGNABLE);
  analyzeTopLevel(returnWithType("", "'string'"));
  analyzeTopLevel(returnWithType("Object", "'string'"));
  analyzeTopLevel(returnWithType("String", "'string'"));
  analyzeTopLevel(returnWithType("String", null));
  analyzeTopLevel(returnWithType("int", null));
  analyzeTopLevel(returnWithType("void", ""));
  analyzeTopLevel(returnWithType("void", 1), MessageKind.RETURN_VALUE_IN_VOID);
  analyzeTopLevel(returnWithType("void", null));
  analyzeTopLevel(returnWithType("String", ""), MessageKind.RETURN_NOTHING);
  // analyzeTopLevel("String foo() {};"); // Should probably fail.
}

testFor() {
  setup();
  analyze("for (var x;true;x = x + 1) {}");
  analyze("for (var x;null;x = x + 1) {}");
  analyze("for (var x;0;x = x + 1) {}", MessageKind.NOT_ASSIGNABLE);
  analyze("for (var x;'';x = x + 1) {}", MessageKind.NOT_ASSIGNABLE);

  // TODO(karlklose): These tests do not work because they use empty
  // statements, which we cannot parse.
  // analyze("for (;true;) {}");
  // analyze("for (;null;) {}");
  // analyze("for (;0;) {}", MessageKind.NOT_ASSIGNABLE);
  // analyze("for (;'';) {}", MessageKind.NOT_ASSIGNABLE);
}

testWhile() {
  analyze("while (true) {}");
  analyze("while (null) {}");
  analyze("while (0) {}", MessageKind.NOT_ASSIGNABLE);
  analyze("while ('') {}", MessageKind.NOT_ASSIGNABLE);

  analyze("do {} while (true);");
  analyze("do {} while (null);");
  analyze("do {} while (0);", MessageKind.NOT_ASSIGNABLE);
  analyze("do {} while ('');", MessageKind.NOT_ASSIGNABLE);
  analyze("do { int i = 0.5; } while (true);", MessageKind.NOT_ASSIGNABLE);
  analyze("do { int i = 0.5; } while (null);", MessageKind.NOT_ASSIGNABLE);
}

testOperators() {
  // TODO(karlklose): add the DartC tests for operators when we can parse
  // classes with operators.
  for (final op in ['+', '-', '*', '/', '%', '~/', '|', '&']) {
    analyze("{ var i = 1 ${op} 2; }");
    analyze("{ var i = 1; i ${op}= 2; }");
    analyze("{ int i; var j = (i = true) ${op} 2; }",
            MessageKind.NOT_ASSIGNABLE);
    analyze("{ int i; var j = 1 ${op} (i = true); }",
            MessageKind.NOT_ASSIGNABLE);
  }
  for (final op in ['-', '~']) {
    analyze("{ var i = ${op}1; }");
    analyze("{ int i; var j = ${op}(i = true); }", MessageKind.NOT_ASSIGNABLE);
  }
  for (final op in ['++', '--']) {
    analyze("{ int i = 1; int j = i${op}; }");
    analyze("{ int i = 1; bool j = i${op}; }", MessageKind.NOT_ASSIGNABLE);
    analyze("{ bool b = true; bool j = b${op}; }");
    analyze("{ bool b = true; int j = ${op}b; }");
  }
  for (final op in ['||', '&&']) {
    analyze("{ bool b = (true ${op} false); }");
    analyze("{ int b = true ${op} false; }", MessageKind.NOT_ASSIGNABLE);
    analyze("{ bool b = (1 ${op} false); }", MessageKind.NOT_ASSIGNABLE);
    analyze("{ bool b = (true ${op} 2); }", MessageKind.NOT_ASSIGNABLE);
  }
  for (final op in ['>', '<', '<=', '>=', '==']) {
    analyze("{ bool b = 1 ${op} 2; }");
    analyze("{ int i = 1 ${op} 2; }", MessageKind.NOT_ASSIGNABLE);
    analyze("{ int i; bool b = (i = true) ${op} 2; }",
            MessageKind.NOT_ASSIGNABLE);
    analyze("{ int i; bool b = 1 ${op} (i = true); }",
            MessageKind.NOT_ASSIGNABLE);
  }
}

String returnWithType(String type, expression)
    => "$type foo() { return $expression; }";


final String CORELIB =
    'lt() {} add() {} sub() {} mul() {} div() {} tdiv() {} mod() {}' +
    ' neg() {} shl() {} shr() {} eq() {} le() {} gt() {} ge() {}' +
    ' or() {} and() {} not() {}';

Node parseExpression(String text) =>
  parseBodyCode(text, (parser, token) => parser.parseExpression(token));

class MockCompiler extends Compiler {
  List<MessageKind> warnings;
  Node parsedTree;

  MockCompiler() : super(null), warnings = [];

  void reportWarning(Node node, var warning) {
    warnings.add(warning.message.kind);
  }

  void clearWarnings() {
    warnings = [];
  }

  parseScript(String text) {
    for (Link<Element> link = parseUnit(text, this);
         !link.isEmpty();
         link = link.tail) {
      universe.define(link.head);
    }
  }
}

// TODO(karlklose): implement with closures instead of global variables.
void setup() {
  compiler = new MockCompiler();
  types = new Types();
}

Types types;
MockCompiler compiler;

Type analyzeType(String text) {
  var node = parseExpression(text);
  TypeCheckerVisitor visitor =
      new TypeCheckerVisitor(compiler, new TreeElements(), types);
  return visitor.type(node);
}

analyzeTopLevel(String text, [expectedWarnings]) {
  if (expectedWarnings === null) expectedWarnings = [];
  if (expectedWarnings is !List) expectedWarnings = [expectedWarnings];

  Token tokens = scan(text);

  ElementListener listener = new ElementListener(compiler);

  PartialParser parser = new PartialParser(listener);
  parser.parseUnit(tokens);

  compiler.universe = new Universe();
  compiler.parseScript(CORELIB);

  for (Link<Element> elements = listener.topLevelElements;
       !elements.isEmpty();
       elements = elements.tail) {
    compiler.universe.define(elements.head);
  }

  for (Link<Element> elements = listener.topLevelElements;
       !elements.isEmpty();
       elements = elements.tail) {
    Node node = elements.head.parseNode(compiler, compiler);
    FullResolverVisitor visitor = new FullResolverVisitor(compiler);
    visitor.visit(node);
    TypeCheckerVisitor checker =
        new TypeCheckerVisitor(compiler, visitor.mapping, types);
    compiler.clearWarnings();
    checker.type(node);
    compareWarningKinds(text, expectedWarnings, compiler.warnings);
  }
}

analyze(String text, [expectedWarnings]) {
  if (expectedWarnings === null) expectedWarnings = [];
  if (expectedWarnings is !List) expectedWarnings = [expectedWarnings];

  Token tokens = scan(text);

  NodeListener listener = new NodeListener(compiler, compiler);

  Parser parser = new Parser(listener);
  parser.parseStatement(tokens);
  Node node = listener.popNode();

  compiler.universe = new Universe();
  compiler.parseScript(CORELIB);

  FullResolverVisitor visitor = new FullResolverVisitor(compiler);
  visitor.visit(node);
  TreeElements elements = visitor.mapping;
  TypeCheckerVisitor checker = new TypeCheckerVisitor(compiler, elements,
                                                                types);
  compiler.clearWarnings();
  checker.type(node);
  compareWarningKinds(text, expectedWarnings, compiler.warnings);
}

void compareWarningKinds(String text, expectedWarnings, foundWarnings) {
  var fail = (message) => Expect.fail('$text: $message');
  Iterator<MessageKind> expected = expectedWarnings.iterator();
  Iterator<MessageKind> found = foundWarnings.iterator();
  while (expected.hasNext() && found.hasNext()) {
    Expect.equals(expected.next(), found.next());
  }
  if (expected.hasNext()) {
    do {
      print('Expected warning "${expected.next()}" did not occur');
    } while (expected.hasNext());
    fail('Too few warnings');
  }
  if (found.hasNext()) {
    do {
      print('Additional warning "${found.next()}"');
    } while (found.hasNext());
    fail('Too many warnings');
  }
}

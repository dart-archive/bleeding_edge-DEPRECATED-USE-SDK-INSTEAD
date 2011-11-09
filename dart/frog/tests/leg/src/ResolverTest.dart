// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("../../../leg/leg.dart");
#import("../../../leg/scanner/scannerlib.dart");
#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import("../../../leg/util/util.dart");

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
  BodyListener listener = new BodyListener(lc, lc);
  BodyParser parser = new BodyParser(listener);
  Token endToken = parseMethod(parser, tokens);
  assert(endToken.kind == EOF_TOKEN);
  return listener.popNode();
}

Node parseStatement(String text) =>
  parseBodyCode(text, (parser, tokens) => parser.parseStatement(tokens));

Node parseFunction(String text, Compiler compiler) {
  Token tokens = scan(text);
  Listener listener = new ElementListener(compiler);
  Parser parser = new Parser(listener);
  parser.parseUnit(tokens);
  Element element = listener.topLevelElements.head;
  Expect.equals(ElementKind.FUNCTION, element.kind);
  compiler.universe.define(element);
  return element.parseNode(compiler, compiler);
}

Node buildIdentifier(String name) => new Identifier(scan(name));

Node buildInitialization(String name) =>
  parseBodyCode('$name = 1',
      (parser, tokens) => parser.parseOptionallyInitializedIdentifier(tokens));


createLocals(List variables) {
  var locals = [];
  for (final variable in variables) {
    String name = variable[0];
    bool init = variable[1];
    if (init) {
      locals.add(buildInitialization(name));
    } else {
      locals.add(buildIdentifier(name));
    }
  }
  var definitions = new NodeList(null, new Link.fromList(locals), null, null);
  return new VariableDefinitions(null, null, definitions, null);
}

testLocals(List variables) {
  ResolverVisitor visitor = new ResolverVisitor(new Compiler(null));
  Element element = visitor.visit(createLocals(variables));
  // A VariableDefinitions does not have an element.
  Expect.equals(null, element);
  Expect.equals(variables.length, visitor.context.elements.length);
  Expect.equals(variables.length, visitor.mapping.length);

  for (final variable in variables) {
    final name = variable[0];
    Identifier id = buildIdentifier(name);
    final element = visitor.visit(id);
    Expect.equals(element, visitor.context.elements[new SourceString(name)]);
  }
}

main() {
  testLocalsOne();
  testLocalsTwo();
  testLocalsThree();
  testLocalsFour();
  testLocalsFive();
  testParametersOne();
  testFor();
}

testLocalsOne() {
  testLocals([["foo", false]]);
  testLocals([["foo", false], ["bar", false]]);
  testLocals([["foo", false], ["bar", false], ["foobar", false]]);

  testLocals([["foo", true]]);
  testLocals([["foo", false], ["bar", true]]);
  testLocals([["foo", true], ["bar", true]]);

  testLocals([["foo", false], ["bar", false], ["foobar", true]]);
  testLocals([["foo", false], ["bar", true], ["foobar", true]]);
  testLocals([["foo", true], ["bar", true], ["foobar", true]]);

  String msg = '';
  try {
    testLocals([["foo", false], ["foo", false]]);
  } catch (CompilerCancelledException ex) {
    msg = ex.reason;
  }
  Expect.equals(msg, ErrorMessages.duplicateDefinition("foo"));
}


testLocalsTwo() {
  ResolverVisitor visitor = new ResolverVisitor(new Compiler(null));
  Node tree = parseStatement("if (true) { var a = 1; var b = 2; }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.length);

  List<Element> elements = visitor.mapping.getValues();
  Expect.notEquals(elements[0], elements[1]);
}

testLocalsThree() {
  ResolverVisitor visitor = new ResolverVisitor(new Compiler(null));
  Node tree = parseStatement("{ var a = 1; if (true) { a; } }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.length);
  List<Element> elements = visitor.mapping.getValues();
  Expect.equals(elements[0], elements[1]);
}

testLocalsFour() {
  ResolverVisitor visitor = new ResolverVisitor(new Compiler(null));
  Node tree = parseStatement("{ var a = 1; if (true) { var a = 1; } }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.length);
  List<Element> elements = visitor.mapping.getValues();
  Expect.notEquals(elements[0], elements[1]);
}

testLocalsFive() {
  ResolverVisitor visitor = new ResolverVisitor(new Compiler(null));
  Node tree =
      parseStatement("if (true) { var a = 1; a; } else { var a = 2; a;}");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(4, visitor.mapping.length);

  List statements1 = tree.thenPart.statements.nodes.toList();
  Node def1 = statements1[0].definitions.nodes.head;
  Node id1 = statements1[1].expression;
  Expect.equals(visitor.mapping[def1], visitor.mapping[id1]);

  List statements2 = tree.elsePart.statements.nodes.toList();
  Node def2 = statements2[0].definitions.nodes.head;
  Node id2 = statements2[1].expression;
  Expect.equals(visitor.mapping[def2], visitor.mapping[id2]);

  Expect.notEquals(visitor.mapping[def1], visitor.mapping[def2]);
  Expect.notEquals(visitor.mapping[id1], visitor.mapping[id2]);
}

testParametersOne() {
  Compiler compiler = new Compiler(null);
  ResolverVisitor visitor = new ResolverVisitor(compiler);
  Node tree = parseFunction("void foo(int a) { return a; }", compiler);
  Element element = visitor.visit(tree);
  Expect.equals(ElementKind.FUNCTION, element.kind);

  // Check that an element has been created for the parameter.
  Node param = tree.parameters.nodes.head.definitions.nodes.head;
  Expect.equals(ElementKind.VARIABLE, visitor.mapping[param].kind);

  // Check that 'a' in 'return a' is resolved to the parameter.
  Return ret = tree.body.statements.nodes.head;
  Identifier use = ret.expression;
  Expect.equals(ElementKind.VARIABLE, visitor.mapping[use].kind);
  Expect.equals(visitor.mapping[param], visitor.mapping[use]);
}

testFor() {
  Compiler compiler = new Compiler(null);
  ResolverVisitor visitor = new ResolverVisitor(compiler);
  Node tree = parseStatement("for (int i = 0; i < 10; i = i + 1) { i = 5; }");
  visitor.visit(tree);

  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(5, visitor.mapping.length);

  Node iNode = tree.initializer.definitions.nodes.head;
  Element iElement = visitor.mapping[iNode];
  // Check that all 'i' have been resolved to the same element.
  visitor.mapping.forEach((node, element) => Expect.equals(iElement, element));

  // Check that we have the expected nodes. This test relies on the mapping
  // field to be a linked hash map (preserving insertion order).
  Expect.isTrue(visitor.mapping is LinkedHashMap);
  List<Node> nodes = visitor.mapping.getKeys();

  Expect.isTrue(nodes[0] is SendSet);  // i = 0

  Expect.isTrue(nodes[1] is Send);     // i (in i < 10)
  Expect.isTrue(nodes[1] is !SendSet);

  Expect.isTrue(nodes[2] is Send);     // i (in i + 1)
  Expect.isTrue(nodes[2] is !SendSet);

  Expect.isTrue(nodes[3] is SendSet);  // i = i + 1

  Expect.isTrue(nodes[4] is SendSet);  // i = 5
}

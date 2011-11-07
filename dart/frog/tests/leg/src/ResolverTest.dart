// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("../../../leg/leg.dart");
#import("../../../leg/scanner.dart");
#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree.dart");
#import("../../../leg/util.dart");

class LoggerCanceler implements Logger, Canceler {
  void cancel([String reason]) {
    throw new CompilerCancelledException(reason);
  }

  void log(message) {
    // print(message);
  }
}

Node parse(String text) {
  Token tokens = scan(text);
  LoggerCanceler lc = new LoggerCanceler();
  BodyListener listener = new BodyListener(lc, lc);
  BodyParser parser = new BodyParser(listener);
  Token endToken = parser.parseOptionallyInitializedIdentifier(tokens);
  assert(endToken.kind == EOF_TOKEN);
  return listener.popNode();
}

Token scan(String text) => new StringScanner(text).tokenize();

buildIdentifier(String name) {
  return new Identifier(scan(name));
}

buildInitialization(String name) => parse('$name = 1');

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


createStatement(String text) {
  Token tokens = scan(text);
  LoggerCanceler lc = new LoggerCanceler();
  BodyListener listener = new BodyListener(lc, lc);
  BodyParser parser = new BodyParser(listener);
  Token endToken = parser.parseStatement(tokens);
  assert(endToken.kind == EOF_TOKEN);
  return listener.popNode();
}

main() {
  testLocalsOne();
  testLocalsTwo();
  testLocalsThree();
  testLocalsFour();
  testLocalsFive();
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
  Node tree = createStatement("if (true) { var a = 1; var b = 2; }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.length);

  List<Element> elements = visitor.mapping.getValues();
  Expect.notEquals(elements[0], elements[1]);
}

testLocalsThree() {
  ResolverVisitor visitor = new ResolverVisitor(new Compiler(null));
  Node tree = createStatement("{ var a = 1; if (true) { a; } }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.length);
  List<Element> elements = visitor.mapping.getValues();
  Expect.equals(elements[0], elements[1]);
}

testLocalsFour() {
  ResolverVisitor visitor = new ResolverVisitor(new Compiler(null));
  Node tree = createStatement("{ var a = 1; if (true) { var a = 1; } }");
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
      createStatement("if (true) { var a = 1; a; } else { var a = 2; a;}");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(4, visitor.mapping.length);

  List statements1 = tree.thenPart.statements.nodes.toList();
  Identifier def1 = statements1[0].definitions.nodes.head.receiver;
  Identifier id1 = statements1[1].expression.selector;
  Expect.equals(visitor.mapping[def1], visitor.mapping[id1]);

  List statements2 = tree.elsePart.statements.nodes.toList();
  Identifier def2 = statements2[0].definitions.nodes.head.receiver;
  Identifier id2 = statements2[1].expression.selector;
  Expect.equals(visitor.mapping[def2], visitor.mapping[id2]);

  Expect.notEquals(visitor.mapping[def1], visitor.mapping[def2]);
  Expect.notEquals(visitor.mapping[id1], visitor.mapping[id2]);
}

// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("../../../leg/leg.dart");
#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import("../../../leg/util/util.dart");
#import("parser_helper.dart");

class WarningMessage {
  Node node;
  String message;
  WarningMessage(this.node, this.message);
}

class MockCompiler extends Compiler {
  List warnings;
  Node parsedTree;

  MockCompiler() : super(null), warnings = [];

  void reportWarning(Node node, String message) {
    warnings.add(new WarningMessage(node, message));
  }

  void clearWarnings() {
    warnings = [];
  }

  resolveStatement(String text) {
    parsedTree = parseStatement(text);
    return resolver.resolve(parsedTree);
  }

  parseScript(String text) {
    for (Link<Element> link = parseUnit(text, this);
         !link.isEmpty();
         link = link.tail) {
      universe.define(link.head);
    }
  }

  resolve(ClassElement element) {
    return resolver.resolveType(element.parseNode(this, this));
  }
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
    Expect.equals(element, visitor.context.elements[buildSourceString(name)]);
  }
}

main() {
  testLocalsOne();
  testLocalsTwo();
  testLocalsThree();
  testLocalsFour();
  testLocalsFive();
  testParametersOne();
  // testFor();  // TODO(ngeoffray): not working because < cannot be resolved.
  testTypeAnnotation();
  testSuperclass();
  // testVarSuperclass(); // The parser crashes with 'class Foo extends var'.
  // testOneInterface(); // The parser does not handle interfaces.
  // testTwoInterfaces(); // The parser does not handle interfaces.
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
  If tree = parseStatement("if (true) { var a = 1; a; } else { var a = 2; a;}");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(4, visitor.mapping.length);

  Block thenPart = tree.thenPart;
  List statements1 = thenPart.statements.nodes.toList();
  Node def1 = statements1[0].definitions.nodes.head;
  Node id1 = statements1[1].expression;
  Expect.equals(visitor.mapping[def1], visitor.mapping[id1]);

  Block elsePart = tree.elsePart;
  List statements2 = elsePart.statements.nodes.toList();
  Node def2 = statements2[0].definitions.nodes.head;
  Node id2 = statements2[1].expression;
  Expect.equals(visitor.mapping[def2], visitor.mapping[id2]);

  Expect.notEquals(visitor.mapping[def1], visitor.mapping[def2]);
  Expect.notEquals(visitor.mapping[id1], visitor.mapping[id2]);
}

testParametersOne() {
  Compiler compiler = new Compiler(null);
  ResolverVisitor visitor = new ResolverVisitor(compiler);
  FunctionExpression tree =
      parseFunction("void foo(int a) { return a; }", compiler);
  Element element = visitor.visit(tree);
  Expect.equals(ElementKind.FUNCTION, element.kind);

  // Check that an element has been created for the parameter.
  Node param = tree.parameters.nodes.head.definitions.nodes.head;
  Expect.equals(ElementKind.VARIABLE, visitor.mapping[param].kind);

  // Check that 'a' in 'return a' is resolved to the parameter.
  Block body = tree.body;
  Return ret = body.statements.nodes.head;
  Identifier use = ret.expression;
  Expect.equals(ElementKind.VARIABLE, visitor.mapping[use].kind);
  Expect.equals(visitor.mapping[param], visitor.mapping[use]);
}

testFor() {
  Compiler compiler = new Compiler(null);
  ResolverVisitor visitor = new ResolverVisitor(compiler);
  For tree = parseStatement("for (int i = 0; i < 10; i = i + 1) { i = 5; }");
  visitor.visit(tree);

  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(5, visitor.mapping.length);

  VariableDefinitions initializer = tree.initializer;
  Node iNode = initializer.definitions.nodes.head;
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

testTypeAnnotation() {
  MockCompiler compiler = new MockCompiler();
  String statement = "Foo bar;";

  // Test that we get a warning when Foo is not defined.
  Map mapping = compiler.resolveStatement(statement);

  Expect.equals(1, mapping.length); // bar has an element.
  Expect.equals(1, compiler.warnings.length);

  Node warningNode = compiler.warnings[0].node;
  String warningMessage = compiler.warnings[0].message;

  Expect.equals(warningMessage, ErrorMessages.cannotResolveType("Foo"));
  VariableDefinitions definition = compiler.parsedTree; 
  Expect.equals(warningNode, definition.type);
  compiler.clearWarnings();

  // Test that there is no warning after defining Foo.
  compiler.parseScript("class Foo {}");
  mapping = compiler.resolveStatement(statement);
  Expect.equals(2, mapping.length);
  Expect.equals(0, compiler.warnings.length);

  // Test that 'var' does not create a warning.
  mapping = compiler.resolveStatement("var foo;");
  Expect.equals(1, mapping.length);
  Expect.equals(0, compiler.warnings.length);
}

testSuperclass() {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript("class Foo extends Bar {}");
  String msg = '';
  try {
    compiler.resolveStatement("Foo bar;");
  } catch (CompilerCancelledException ex) {
    // TODO(ngeoffray): Once it's there, use error reporting framework.
    msg = ex.reason;
  }
  Expect.equals(msg, ErrorMessages.cannotResolveType("Bar"));

  compiler.parseScript("class Bar {}");
  Map mapping = compiler.resolveStatement("Foo bar;");
  Expect.equals(2, mapping.length);

  ClassElement fooElement = compiler.universe.find(buildSourceString('Foo'));
  ClassElement barElement = compiler.universe.find(buildSourceString('Bar'));
  Expect.equals(barElement.computeType(compiler, null),
                fooElement.supertype);
  Expect.isTrue(fooElement.interfaces.isEmpty());
  Expect.isTrue(barElement.interfaces.isEmpty());
}

testVarSuperclass() {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript("class Foo extends var {}");
  String msg = '';
  try {
    compiler.resolveStatement("Foo bar;");
  } catch (CompilerCancelledException ex) {
    // TODO(ngeoffray): Once it's there, use error reporting framework.
    msg = ex.reason;
  }
  Expect.equals(msg, ErrorMessages.cannotResolveType("var"));
}

testOneInterface() {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript("class Foo implements Bar {}");
  String msg = '';
  try {
    compiler.resolveStatement("Foo bar;");
  } catch (CompilerCancelledException ex) {
    // TODO(ngeoffray): Once it's there, use error reporting framework.
    msg = ex.reason;
  }
  Expect.equals(msg, ErrorMessages.cannotResolveType("Bar"));

  // Add the interface to the world and make sure everything is setup correctly.
  compiler.parseScript("interface Bar {}");

  visitor = new ResolverVisitor(compiler);
  compiler.resolverStatement("Foo bar;");

  Element fooElement = compiler.universe.find(buildSourceString('Foo'));
  Element barElement = compiler.universe.find(buildSourceString('Bar'));

  Expect.equals(null, barElement.supertype);
  Expect.isTrue(barElement.interfaces.isEmpty());

  Expect.equals(barElement.computeType(compiler, null),
                fooElement.interfaces[0]);
  Expect.equals(1, fooElement.interfaces.length);
}

testTwoInterfaces() {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript(
      "interface I1 {} interface I2 {} class C implements I1, I2 {}");
  compiler.resolveStatement("Foo bar;");

  Element c = compiler.universe.find(buildSourceString('C'));
  Element i1 = compiler.universe.find(buildSourceString('I1'));
  Element i2 = compiler.universe.find(buildSourceString('I2'));

  Expect.equals(2, c.interfaces.length);
  Expect.equals(i1.computeType(compiler, null), c.interfaces[0]);
  Expect.equals(i2.computeType(compiler, null), c.interfaces[1]);
}

// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("../../../leg/leg.dart");
#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import("../../../leg/util/util.dart");
#import("mock_compiler.dart");
#import("parser_helper.dart");

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
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  Element element = visitor.visit(createLocals(variables));
  // A VariableDefinitions does not have an element.
  Expect.equals(null, element);
  Expect.equals(variables.length, visitor.mapping.map.length);

  for (final variable in variables) {
    final name = variable[0];
    Identifier id = buildIdentifier(name);
    final element = visitor.visit(id);
    Expect.equals(element, visitor.context.elements[buildSourceString(name)]);
  }
  return compiler;
}

main() {
  testLocalsOne();
  testLocalsTwo();
  testLocalsThree();
  testLocalsFour();
  testLocalsFive();
  testParametersOne();
  testFor();
  testTypeAnnotation();
  testSuperclass();
  // testVarSuperclass(); // The parser crashes with 'class Foo extends var'.
  // testOneInterface(); // The parser does not handle interfaces.
  // testTwoInterfaces(); // The parser does not handle interfaces.
  testFunctionExpression();
  testNewExpression();
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

  MockCompiler compiler = testLocals([["foo", false], ["foo", false]]);
  Expect.equals(1, compiler.errors.length);
  Expect.equals(
      new Message(MessageKind.DUPLICATE_DEFINITION, ['foo']),
      compiler.errors[0].message);
}


testLocalsTwo() {
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  Node tree = parseStatement("if (true) { var a = 1; var b = 2; }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.map.length);

  List<Element> elements = visitor.mapping.map.getValues();
  Expect.notEquals(elements[0], elements[1]);
}

testLocalsThree() {
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  Node tree = parseStatement("{ var a = 1; if (true) { a; } }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.map.length);
  List<Element> elements = visitor.mapping.map.getValues();
  Expect.equals(elements[0], elements[1]);
}

testLocalsFour() {
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  Node tree = parseStatement("{ var a = 1; if (true) { var a = 1; } }");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(2, visitor.mapping.map.length);
  List<Element> elements = visitor.mapping.map.getValues();
  Expect.notEquals(elements[0], elements[1]);
}

testLocalsFive() {
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  If tree = parseStatement("if (true) { var a = 1; a; } else { var a = 2; a;}");
  Element element = visitor.visit(tree);
  Expect.equals(null, element);
  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(4, visitor.mapping.map.length);

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
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  FunctionExpression tree =
      parseFunction("void foo(int a) { return a; }", compiler);
  Element element = visitor.visit(tree);
  Expect.equals(ElementKind.FUNCTION, element.kind);

  // Check that an element has been created for the parameter.
  Node param = tree.parameters.nodes.head.definitions.nodes.head;
  Expect.equals(ElementKind.PARAMETER, visitor.mapping[param].kind);

  // Check that 'a' in 'return a' is resolved to the parameter.
  Block body = tree.body;
  Return ret = body.statements.nodes.head;
  Send use = ret.expression;
  Expect.equals(ElementKind.PARAMETER, visitor.mapping[use].kind);
  Expect.equals(visitor.mapping[param], visitor.mapping[use]);
}

testFor() {
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  For tree = parseStatement("for (int i = 0; i < 10; i = i + 1) { i = 5; }");
  visitor.visit(tree);

  Expect.equals(0, visitor.context.elements.length);
  Expect.equals(7, visitor.mapping.map.length);

  VariableDefinitions initializer = tree.initializer;
  Node iNode = initializer.definitions.nodes.head;
  Element iElement = visitor.mapping[iNode];

  // Check that we have the expected nodes. This test relies on the mapping
  // field to be a linked hash map (preserving insertion order).
  Expect.isTrue(visitor.mapping.map is LinkedHashMap);
  List<Node> nodes = visitor.mapping.map.getKeys();
  List<Element> elements = visitor.mapping.map.getValues();

  Expect.isTrue(nodes[0] is SendSet);  // i = 0
  Expect.equals(elements[0], iElement);

  Expect.isTrue(nodes[1] is Send);     // i (in i < 10)
  Expect.isTrue(nodes[1] is !SendSet);
  Expect.equals(elements[1], iElement);

  Expect.isTrue(nodes[2] is Send);     // < call
  Expect.isTrue(nodes[2] is !SendSet);

  Expect.isTrue(nodes[3] is Send);     // i (in i + 1)
  Expect.isTrue(nodes[3] is !SendSet);
  Expect.equals(elements[3], iElement);

  Expect.isTrue(nodes[4] is Send);     // + call
  Expect.isTrue(nodes[4] is !SendSet);

  Expect.isTrue(nodes[5] is SendSet);  // i = i + 1
  Expect.equals(elements[5], iElement);

  Expect.isTrue(nodes[6] is SendSet);  // i = 5
  Expect.equals(elements[6], iElement);
}

testTypeAnnotation() {
  MockCompiler compiler = new MockCompiler();
  String statement = "Foo bar;";

  // Test that we get a warning when Foo is not defined.
  Map mapping = compiler.resolveStatement(statement).map;

  Expect.equals(1, mapping.length); // bar has an element.
  Expect.equals(1, compiler.warnings.length);

  Node warningNode = compiler.warnings[0].node;

  Expect.equals(
      new Message(MessageKind.CANNOT_RESOLVE_TYPE, ['Foo']),
      compiler.warnings[0].message);
  VariableDefinitions definition = compiler.parsedTree;
  Expect.equals(warningNode, definition.type);
  compiler.clearWarnings();

  // Test that there is no warning after defining Foo.
  compiler.parseScript("class Foo {}");
  mapping = compiler.resolveStatement(statement).map;
  Expect.equals(2, mapping.length);
  Expect.equals(0, compiler.warnings.length);

  // Test that 'var' does not create a warning.
  mapping = compiler.resolveStatement("var foo;").map;
  Expect.equals(1, mapping.length);
  Expect.equals(0, compiler.warnings.length);
}

testSuperclass() {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript("class Foo extends Bar {}");
  compiler.resolveStatement("Foo bar;");
  Expect.equals(1, compiler.errors.length);
  Expect.equals(
      new Message(MessageKind.CANNOT_RESOLVE_TYPE, ['Bar']),
      compiler.errors[0].message);
  compiler.clearErrors();

  compiler = new MockCompiler();
  compiler.parseScript("class Foo extends Bar {}");
  compiler.parseScript("class Bar {}");
  Map mapping = compiler.resolveStatement("Foo bar;").map;
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
  compiler.resolveStatement("Foo bar;");
  Expect.equals(1, compiler.errors.length);
  Expect.equals(
      new Message(MessageKind.CANNOT_RESOLVE_TYPE, ['var']),
      compiler.errors[0].message);
  compiler.clearErrors();
}

testOneInterface() {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript("class Foo implements Bar {}");
  compiler.resolveStatement("Foo bar;");
  Expect.equals(1, compiler.errors.length);
  Expect.equals(
      new Message(MessageKind.CANNOT_RESOLVE_TYPE, ['bar']),
      compiler.errors[0].message);
  compiler.clearErrors();

  // Add the interface to the world and make sure everything is setup correctly.
  compiler.parseScript("interface Bar {}");

  visitor = new FullResolverVisitor(compiler);
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

testFunctionExpression() {
  MockCompiler compiler = new MockCompiler();
  ResolverVisitor visitor = compiler.resolverVisitor();
  Map mapping = compiler.resolveStatement("int f() {}").map;
  Expect.equals(1, mapping.length);
  Element element;
  Node node;
  mapping.forEach((Node n, Element e) {
    element = e;
    node = n;
  });
  Expect.equals(ElementKind.FUNCTION, element.kind);
  Expect.equals(buildSourceString('f'), element.name);
  Expect.equals(element.parseNode(compiler, compiler), node);
}

testNewExpression() {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript("class A {} foo() { print(new A()); }");
  ClassElement aElement = compiler.universe.find(buildSourceString('A'));
  FunctionElement fooElement = compiler.universe.find(buildSourceString('foo'));
  Expect.isTrue(aElement !== null);
  Expect.isTrue(fooElement !== null);

  fooElement.parseNode(compiler, compiler);
  compiler.resolver.resolve(fooElement);

  TreeElements elements = compiler.resolveStatement("new A();");
  Element element =
      elements[compiler.parsedTree.asExpressionStatement().expression];
  Expect.equals(ElementKind.CONSTRUCTOR, element.kind);
  Expect.isTrue(element is SynthesizedConstructorElement);
}

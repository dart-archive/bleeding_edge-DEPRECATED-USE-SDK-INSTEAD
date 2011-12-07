// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("../../../leg/leg.dart");
#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import('../../../leg/scanner/scannerlib.dart');
#import("../../../leg/util/util.dart");
#import("mock_compiler.dart");
#import("parser_helper.dart");

Type intType;
Type boolType;
Type stringType;
Type doubleType;
Type objectType;

main() {
  testSimpleTypes();
  testReturn();
  testFor();
  testWhile();
  testOperators();
  testMethodInvocationArgumentCount();
  testMethodInvocations();
}

testSimpleTypes() {
  setup();
  Expect.equals(intType, analyzeType("3"));
  Expect.equals(boolType, analyzeType("false"));
  Expect.equals(boolType, analyzeType("true"));
  Expect.equals(stringType, analyzeType("'hestfisk'"));
}

testReturn() {
  setup();
  analyzeTopLevel("void foo() { return 3; }", MessageKind.RETURN_VALUE_IN_VOID);
  analyzeTopLevel("int bar() { return 'hest'; }", MessageKind.NOT_ASSIGNABLE);
  analyzeTopLevel("void baz() { var x; return x; }");
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

void testMethodInvocationArgumentCount() {
  setup();
  compiler.parseScript(CLASS_WITH_METHODS);
  final String header = "{ ClassWithMethods c; ";
  analyze(header + "c.untypedNoArgumentMethod(1); }",
    MessageKind.ADDITIONAL_ARGUMENT);
  analyze(header + "c.untypedOneArgumentMethod(); }",
    MessageKind.MISSING_ARGUMENT);
  analyze(header + "c.untypedOneArgumentMethod(1, 1); }",
    MessageKind.ADDITIONAL_ARGUMENT);
  analyze(header + "c.untypedTwoArgumentMethod(); }",
      MessageKind.MISSING_ARGUMENT);
  analyze(header + "c.untypedTwoArgumentMethod(1, 2, 3); }",
    MessageKind.ADDITIONAL_ARGUMENT);
  analyze(header + "c.intNoArgumentMethod(1); }",
    MessageKind.ADDITIONAL_ARGUMENT);
  analyze(header + "c.intOneArgumentMethod(); }",
      MessageKind.MISSING_ARGUMENT);
  analyze(header + "c.intOneArgumentMethod(1, 1); }",
    MessageKind.ADDITIONAL_ARGUMENT);
  analyze(header + "c.intTwoArgumentMethod(); }",
      MessageKind.MISSING_ARGUMENT);
  analyze(header + "c.intTwoArgumentMethod(1, 2, 3); }",
    MessageKind.ADDITIONAL_ARGUMENT);
  // analyze(header + "c.untypedField(); }");
}

void testMethodInvocations() {
  setup();
  compiler.parseScript(CLASS_WITH_METHODS);
  final String header = "{ ClassWithMethods c; int i; int j; ";

  analyze(header + "int k = c.untypedNoArgumentMethod(); }");
  analyze(header + "ClassWithMethods x = c.untypedNoArgumentMethod(); }");

  analyze(header + "int k = c.untypedOneArgumentMethod(c); }");
  analyze(header + "ClassWithMethods x = c.untypedOneArgumentMethod(1); }");
  analyze(header + "int k = c.untypedOneArgumentMethod('string'); }");
  analyze(header + "int k = c.untypedOneArgumentMethod(i); }");

  analyze(header + "int k = c.untypedTwoArgumentMethod(1, 'string'); }");
  analyze(header + "int k = c.untypedTwoArgumentMethod(i, j); }");
  analyze(header + "ClassWithMethods x = c.untypedTwoArgumentMethod(i, c); }");

  analyze(header + "int k = c.intNoArgumentMethod(); }");
  analyze(header + "ClassWithMethods x = c.intNoArgumentMethod(); }",
      MessageKind.NOT_ASSIGNABLE);

  analyze(header + "int k = c.intOneArgumentMethod(c); }",
      MessageKind.NOT_ASSIGNABLE);
  analyze(header + "ClassWithMethods x = c.intOneArgumentMethod(1); }",
      MessageKind.NOT_ASSIGNABLE);
  analyze(header + "int k = c.intOneArgumentMethod('string'); }",
    MessageKind.NOT_ASSIGNABLE);
  analyze(header + "int k = c.intOneArgumentMethod(i); }");

  analyze(header + "int k = c.intTwoArgumentMethod(1, 'string'); }",
    MessageKind.NOT_ASSIGNABLE);
  analyze(header + "int k = c.intTwoArgumentMethod(i, j); }");
  analyze(header + "ClassWithMethods x = c.intTwoArgumentMethod(i, j); }",
      MessageKind.NOT_ASSIGNABLE);
}

String returnWithType(String type, expression)
    => "$type foo() { return $expression; }";


final CLASS_WITH_METHODS = '''
class ClassWithMethods {
  untypedNoArgumentMethod() {}
  untypedOneArgumentMethod(argument) {}
  untypedTwoArgumentMethod(argument1, argument2) {}

  int intNoArgumentMethod() {}
  int intOneArgumentMethod(int argument) {}
  int intTwoArgumentMethod(int argument1, int argument2) {}

// TODO(karlklose): uncomment when fields are implemented.
//  Function functionField;
//  var untypedField;
//  int intField;
}''';


Node parseExpression(String text) =>
  parseBodyCode(text, (parser, token) => parser.parseExpression(token));

// TODO(karlklose): implement with closures instead of global variables.
void setup() {
  compiler = new MockCompiler();
  types = new Types();
  intType = lookupType(Types.INT, compiler, types);
  doubleType = lookupType(Types.DOUBLE, compiler, types);
  boolType = lookupType(Types.BOOL, compiler, types);
  stringType = lookupType(Types.STRING, compiler, types);
  objectType = lookupType(Types.OBJECT, compiler, types);
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

  Universe universe = compiler.universe;
  compiler.universe = new ExtendedUniverse(compiler.universe);

  Token tokens = scan(text);
  ElementListener listener = new ElementListener(compiler);
  PartialParser parser = new PartialParser(listener);
  parser.parseUnit(tokens);

  for (Link<Element> elements = listener.topLevelElements;
       !elements.isEmpty();
       elements = elements.tail) {
    compiler.universe.define(elements.head);
  }

  for (Link<Element> elements = listener.topLevelElements;
       !elements.isEmpty();
       elements = elements.tail) {
    Node node = elements.head.parseNode(compiler, compiler);
    TreeElements mapping = compiler.resolver.resolve(elements.head);
    TypeCheckerVisitor checker =
        new TypeCheckerVisitor(compiler, mapping, types);
    compiler.clearWarnings();
    checker.type(node);
    compareWarningKinds(text, expectedWarnings, compiler.warnings);
  }

  compiler.universe = universe;
}

analyze(String text, [expectedWarnings]) {
  if (expectedWarnings === null) expectedWarnings = [];
  if (expectedWarnings is !List) expectedWarnings = [expectedWarnings];

  Universe universe = compiler.universe;
  compiler.universe = new ExtendedUniverse(compiler.universe);

  Token tokens = scan(text);
  NodeListener listener = new NodeListener(compiler, compiler);
  Parser parser = new Parser(listener);
  parser.parseStatement(tokens);
  Node node = listener.popNode();
  TreeElements elements = compiler.resolveNodeStatement(node);
  TypeCheckerVisitor checker = new TypeCheckerVisitor(compiler, elements,
                                                                types);
  compiler.clearWarnings();
  checker.type(node);
  compareWarningKinds(text, expectedWarnings, compiler.warnings);

  compiler.universe = universe;
}

void compareWarningKinds(String text, expectedWarnings, foundWarnings) {
  var fail = (message) => Expect.fail('$text: $message');
  Iterator<MessageKind> expected = expectedWarnings.iterator();
  Iterator<WarningMessage> found = foundWarnings.iterator();
  while (expected.hasNext() && found.hasNext()) {
    Expect.equals(expected.next(), found.next().message.kind);
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

class ExtendedUniverse extends Universe {
  final Universe base;

  ExtendedUniverse(Universe this.base);

  Element find(SourceString name) {
    Element result = elements[name];
    if (result == null) result = base.find(name);
    return result;
  }

  void define(Element element) {
    assert(base.elements[element.name] == null);
    super.define(element);
  }
}

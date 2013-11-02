// This code was auto-generated, is not intended to be edited, and is subject to
// significant change. Please see the README file for more information.
library engine.resolver_test;
import 'package:analyzer/src/generated/java_core.dart';
import 'package:analyzer/src/generated/java_junit.dart';
import 'package:analyzer/src/generated/source_io.dart';
import 'package:analyzer/src/generated/error.dart';
import 'package:analyzer/src/generated/scanner.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/parser.dart' show ParserErrorCode;
import 'package:analyzer/src/generated/element.dart';
import 'package:analyzer/src/generated/resolver.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/java_engine_io.dart';
import 'package:analyzer/src/generated/sdk.dart' show DartSdk;
import 'package:analyzer/src/generated/sdk_io.dart' show DirectoryBasedDartSdk;
import 'package:unittest/unittest.dart' as _ut;
import 'test_support.dart';
import 'ast_test.dart' show ASTFactory;
import 'element_test.dart' show ElementFactory;
class TypePropagationTest extends ResolverTestCase {
  void fail_propagatedReturnType_functionExpression() {
    String code = EngineTestCase.createSource(["main() {", "  var v = (() {return 42;})();", "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.intType);
  }
  void test_as() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  bool get g => true;",
        "}",
        "A f(var p) {",
        "  if ((p as A).g) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    IfStatement ifStatement = body.block.statements[0] as IfStatement;
    ReturnStatement statement = ((ifStatement.thenStatement as Block)).statements[0] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_assert() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  assert (p is A);",
        "  return p;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_assignment() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var v;", "  v = 0;", "  return v;", "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[2] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeProvider.intType, variableName.propagatedType);
  }
  void test_assignment_afterInitializer() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var v = 0;", "  v = 1.0;", "  return v;", "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[2] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeProvider.doubleType, variableName.propagatedType);
  }
  void test_forEach() {
    String code = EngineTestCase.createSource([
        "f(List<String> p) {",
        "  for (var e in p) {",
        "    e;",
        "  }",
        "}"]);
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    InterfaceType stringType = typeProvider.stringType;
    {
      SimpleIdentifier identifier = EngineTestCase.findNode(unit, code, "e in", SimpleIdentifier);
      JUnitTestCase.assertSame(stringType, identifier.propagatedType);
    }
    {
      SimpleIdentifier identifier = EngineTestCase.findNode(unit, code, "e;", SimpleIdentifier);
      JUnitTestCase.assertSame(stringType, identifier.propagatedType);
    }
  }
  void test_functionExpression_asInvocationArgument() {
    String code = EngineTestCase.createSource([
        "class MyMap<K, V> {",
        "  forEach(f(K key, V value)) {}",
        "}",
        "f(MyMap<int, String> m) {",
        "  m.forEach((k, v) {",
        "    k;",
        "    v;",
        "  });",
        "}"]);
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    Type2 intType = typeProvider.intType;
    FormalParameter kParameter = EngineTestCase.findNode(unit, code, "k, ", SimpleFormalParameter);
    JUnitTestCase.assertSame(intType, kParameter.identifier.propagatedType);
    SimpleIdentifier kIdentifier = EngineTestCase.findNode(unit, code, "k;", SimpleIdentifier);
    JUnitTestCase.assertSame(intType, kIdentifier.propagatedType);
    JUnitTestCase.assertSame(typeProvider.dynamicType, kIdentifier.staticType);
    Type2 stringType = typeProvider.stringType;
    FormalParameter vParameter = EngineTestCase.findNode(unit, code, "v)", SimpleFormalParameter);
    JUnitTestCase.assertSame(stringType, vParameter.identifier.propagatedType);
    SimpleIdentifier vIdentifier = EngineTestCase.findNode(unit, code, "v;", SimpleIdentifier);
    JUnitTestCase.assertSame(stringType, vIdentifier.propagatedType);
    JUnitTestCase.assertSame(typeProvider.dynamicType, vIdentifier.staticType);
  }
  void test_functionExpression_asInvocationArgument_fromInferredInvocation() {
    String code = EngineTestCase.createSource([
        "class MyMap<K, V> {",
        "  forEach(f(K key, V value)) {}",
        "}",
        "f(MyMap<int, String> m) {",
        "  var m2 = m;",
        "  m2.forEach((k, v) {});",
        "}"]);
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    Type2 intType = typeProvider.intType;
    FormalParameter kParameter = EngineTestCase.findNode(unit, code, "k, ", SimpleFormalParameter);
    JUnitTestCase.assertSame(intType, kParameter.identifier.propagatedType);
    Type2 stringType = typeProvider.stringType;
    FormalParameter vParameter = EngineTestCase.findNode(unit, code, "v)", SimpleFormalParameter);
    JUnitTestCase.assertSame(stringType, vParameter.identifier.propagatedType);
  }
  void test_functionExpression_asInvocationArgument_functionExpressionInvocation() {
    String code = EngineTestCase.createSource([
        "main() {",
        "  (f(String value)) {} ((v) {",
        "    v;",
        "  });",
        "}"]);
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    Type2 dynamicType = typeProvider.dynamicType;
    Type2 stringType = typeProvider.stringType;
    FormalParameter vParameter = EngineTestCase.findNode(unit, code, "v)", FormalParameter);
    JUnitTestCase.assertSame(stringType, vParameter.identifier.propagatedType);
    JUnitTestCase.assertSame(dynamicType, vParameter.identifier.staticType);
    SimpleIdentifier vIdentifier = EngineTestCase.findNode(unit, code, "v;", SimpleIdentifier);
    JUnitTestCase.assertSame(stringType, vIdentifier.propagatedType);
    JUnitTestCase.assertSame(dynamicType, vIdentifier.staticType);
  }
  void test_functionExpression_asInvocationArgument_keepIfLessSpecific() {
    String code = EngineTestCase.createSource([
        "class MyList {",
        "  forEach(f(Object value)) {}",
        "}",
        "f(MyList list) {",
        "  list.forEach((int v) {",
        "    v;",
        "  });",
        "}"]);
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    Type2 intType = typeProvider.intType;
    FormalParameter vParameter = EngineTestCase.findNode(unit, code, "v)", SimpleFormalParameter);
    JUnitTestCase.assertSame(null, vParameter.identifier.propagatedType);
    JUnitTestCase.assertSame(intType, vParameter.identifier.staticType);
    SimpleIdentifier vIdentifier = EngineTestCase.findNode(unit, code, "v;", SimpleIdentifier);
    JUnitTestCase.assertSame(intType, vIdentifier.staticType);
    JUnitTestCase.assertSame(null, vIdentifier.propagatedType);
  }
  void test_functionExpression_asInvocationArgument_replaceIfMoreSpecific() {
    String code = EngineTestCase.createSource([
        "class MyList<E> {",
        "  forEach(f(E value)) {}",
        "}",
        "f(MyList<String> list) {",
        "  list.forEach((Object v) {",
        "    v;",
        "  });",
        "}"]);
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    Type2 stringType = typeProvider.stringType;
    FormalParameter vParameter = EngineTestCase.findNode(unit, code, "v)", SimpleFormalParameter);
    JUnitTestCase.assertSame(stringType, vParameter.identifier.propagatedType);
    JUnitTestCase.assertSame(typeProvider.objectType, vParameter.identifier.staticType);
    SimpleIdentifier vIdentifier = EngineTestCase.findNode(unit, code, "v;", SimpleIdentifier);
    JUnitTestCase.assertSame(stringType, vIdentifier.propagatedType);
  }
  void test_Future_then() {
    String code = EngineTestCase.createSource([
        "import 'dart:async';",
        "main(Future<int> firstFuture) {",
        "  firstFuture.then((p1) {",
        "    return 1.0;",
        "  }).then((p2) {",
        "    return new Future<String>.value('str');",
        "  }).then((p3) {",
        "  });",
        "}"]);
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FormalParameter p1 = EngineTestCase.findNode(unit, code, "p1) {", SimpleFormalParameter);
    JUnitTestCase.assertSame(typeProvider.intType, p1.identifier.propagatedType);
    FormalParameter p2 = EngineTestCase.findNode(unit, code, "p2) {", SimpleFormalParameter);
    JUnitTestCase.assertSame(typeProvider.doubleType, p2.identifier.propagatedType);
    FormalParameter p3 = EngineTestCase.findNode(unit, code, "p3) {", SimpleFormalParameter);
    JUnitTestCase.assertSame(typeProvider.stringType, p3.identifier.propagatedType);
  }
  void test_initializer() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var v = 0;", "  return v;", "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    NodeList<Statement> statements = body.block.statements;
    {
      VariableDeclarationStatement statement = statements[0] as VariableDeclarationStatement;
      SimpleIdentifier variableName = statement.variables.variables[0].name;
      JUnitTestCase.assertSame(typeProvider.dynamicType, variableName.staticType);
      JUnitTestCase.assertSame(typeProvider.intType, variableName.propagatedType);
    }
    {
      ReturnStatement statement = statements[1] as ReturnStatement;
      SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
      JUnitTestCase.assertSame(typeProvider.intType, variableName.propagatedType);
    }
  }
  void test_initializer_dereference() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var v = 'String';", "  v.", "}"]));
    LibraryElement library = resolve(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ExpressionStatement statement = body.block.statements[1] as ExpressionStatement;
    PrefixedIdentifier invocation = statement.expression as PrefixedIdentifier;
    SimpleIdentifier variableName = invocation.prefix;
    JUnitTestCase.assertSame(typeProvider.stringType, variableName.propagatedType);
  }
  void test_is_conditional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  return (p is A) ? p : null;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[0] as ReturnStatement;
    ConditionalExpression conditional = statement.expression as ConditionalExpression;
    SimpleIdentifier variableName = conditional.thenExpression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_is_if() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  if (p is A) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    IfStatement ifStatement = body.block.statements[0] as IfStatement;
    ReturnStatement statement = ((ifStatement.thenStatement as Block)).statements[0] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_is_if_lessSpecific() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(A p) {",
        "  if (p is String) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    IfStatement ifStatement = body.block.statements[0] as IfStatement;
    ReturnStatement statement = ((ifStatement.thenStatement as Block)).statements[0] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(null, variableName.propagatedType);
  }
  void test_is_if_logicalAnd() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  if (p is A && p != null) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    IfStatement ifStatement = body.block.statements[0] as IfStatement;
    ReturnStatement statement = ((ifStatement.thenStatement as Block)).statements[0] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_is_postConditional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  A a = (p is A) ? p : throw null;",
        "  return p;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_is_postIf() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  if (p is A) {",
        "    A a = p;",
        "  } else {",
        "    return null;",
        "  }",
        "  return p;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_is_subclass() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  B m() => this;",
        "}",
        "A f(A p) {",
        "  if (p is B) {",
        "    return p.m();",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[2] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    IfStatement ifStatement = body.block.statements[0] as IfStatement;
    ReturnStatement statement = ((ifStatement.thenStatement as Block)).statements[0] as ReturnStatement;
    MethodInvocation invocation = statement.expression as MethodInvocation;
    JUnitTestCase.assertNotNull(invocation.methodName.propagatedElement);
  }
  void test_is_while() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  while (p is A) {",
        "    return p;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    WhileStatement whileStatement = body.block.statements[0] as WhileStatement;
    ReturnStatement statement = ((whileStatement.body as Block)).statements[0] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_isNot_conditional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  return (p is! A) ? null : p;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[0] as ReturnStatement;
    ConditionalExpression conditional = statement.expression as ConditionalExpression;
    SimpleIdentifier variableName = conditional.elseExpression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_isNot_if() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  if (p is! A) {",
        "    return null;",
        "  } else {",
        "    return p;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    IfStatement ifStatement = body.block.statements[0] as IfStatement;
    ReturnStatement statement = ((ifStatement.elseStatement as Block)).statements[0] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_isNot_if_logicalOr() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  if (p is! A || null == p) {",
        "    return null;",
        "  } else {",
        "    return p;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    IfStatement ifStatement = body.block.statements[0] as IfStatement;
    ReturnStatement statement = ((ifStatement.elseStatement as Block)).statements[0] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_isNot_postConditional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  A a = (p is! A) ? throw null : p;",
        "  return p;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_isNot_postIf() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "A f(var p) {",
        "  if (p is! A) {",
        "    return null;",
        "  }",
        "  return p;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = unit.declarations[0] as ClassDeclaration;
    InterfaceType typeA = classA.element.type;
    FunctionDeclaration function = unit.declarations[1] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    SimpleIdentifier variableName = statement.expression as SimpleIdentifier;
    JUnitTestCase.assertSame(typeA, variableName.propagatedType);
  }
  void test_listLiteral_different() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var v = [0, '1', 2];", "  return v[2];", "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    IndexExpression indexExpression = statement.expression as IndexExpression;
    JUnitTestCase.assertNull(indexExpression.propagatedType);
  }
  void test_listLiteral_same() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var v = [0, 1, 2];", "  return v[2];", "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    IndexExpression indexExpression = statement.expression as IndexExpression;
    JUnitTestCase.assertSame(typeProvider.intType, indexExpression.propagatedType);
  }
  void test_mapLiteral_different() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  var v = {'0' : 0, 1 : '1', '2' : 2};",
        "  return v;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    SimpleIdentifier identifier = statement.expression as SimpleIdentifier;
    InterfaceType propagatedType = identifier.propagatedType as InterfaceType;
    JUnitTestCase.assertSame(typeProvider.mapType.element, propagatedType.element);
    List<Type2> typeArguments = propagatedType.typeArguments;
    EngineTestCase.assertLength(2, typeArguments);
    JUnitTestCase.assertSame(typeProvider.dynamicType, typeArguments[0]);
    JUnitTestCase.assertSame(typeProvider.dynamicType, typeArguments[1]);
  }
  void test_mapLiteral_same() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  var v = {'a' : 0, 'b' : 1, 'c' : 2};",
        "  return v;",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = function.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[1] as ReturnStatement;
    SimpleIdentifier identifier = statement.expression as SimpleIdentifier;
    InterfaceType propagatedType = identifier.propagatedType as InterfaceType;
    JUnitTestCase.assertSame(typeProvider.mapType.element, propagatedType.element);
    List<Type2> typeArguments = propagatedType.typeArguments;
    EngineTestCase.assertLength(2, typeArguments);
    JUnitTestCase.assertSame(typeProvider.stringType, typeArguments[0]);
    JUnitTestCase.assertSame(typeProvider.intType, typeArguments[1]);
  }
  void test_propagatedReturnType_function_hasReturnType_returnsNull() {
    String code = EngineTestCase.createSource(["String f() => null;", "main() {", "  var v = f();", "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.stringType);
  }
  void test_propagatedReturnType_function_lessSpecificStaticReturnType() {
    String code = EngineTestCase.createSource(["Object f() => 42;", "main() {", "  var v = f();", "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.intType);
  }
  void test_propagatedReturnType_function_moreSpecificStaticReturnType() {
    String code = EngineTestCase.createSource([
        "int f(v) => (v as num);",
        "main() {",
        "  var v = f(3);",
        "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.intType);
  }
  void test_propagatedReturnType_function_noReturnTypeName_blockBody_multipleReturns() {
    String code = EngineTestCase.createSource([
        "f() {",
        "  if (true) return 0;",
        "  return 1.0;",
        "}",
        "main() {",
        "  var v = f();",
        "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.numType);
  }
  void test_propagatedReturnType_function_noReturnTypeName_blockBody_oneReturn() {
    String code = EngineTestCase.createSource([
        "f() {",
        "  var z = 42;",
        "  return z;",
        "}",
        "main() {",
        "  var v = f();",
        "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.intType);
  }
  void test_propagatedReturnType_function_noReturnTypeName_expressionBody() {
    String code = EngineTestCase.createSource(["f() => 42;", "main() {", "  var v = f();", "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.intType);
  }
  void test_propagatedReturnType_localFunction() {
    String code = EngineTestCase.createSource(["main() {", "  f() => 42;", "  var v = f();", "}"]);
    check_propagatedReturnType(code, typeProvider.dynamicType, typeProvider.intType);
  }
  void test_query() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'dart:html';",
        "",
        "main() {",
        "  var v1 = query('a');",
        "  var v2 = query('A');",
        "  var v3 = query('body:active');",
        "  var v4 = query('button[foo=\"bar\"]');",
        "  var v5 = query('div.class');",
        "  var v6 = query('input#id');",
        "  var v7 = query('select#id');",
        "  // invocation of method",
        "  var m1 = document.query('div');",
        " // unsupported currently",
        "  var b1 = query('noSuchTag');",
        "  var b2 = query('DART_EDITOR_NO_SUCH_TYPE');",
        "  var b3 = query('body div');",
        "  return [v1, v2, v3, v4, v5, v6, v7, m1, b1, b2, b3];",
        "}"]));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration main = unit.declarations[0] as FunctionDeclaration;
    BlockFunctionBody body = main.functionExpression.body as BlockFunctionBody;
    ReturnStatement statement = body.block.statements[11] as ReturnStatement;
    NodeList<Expression> elements = ((statement.expression as ListLiteral)).elements;
    JUnitTestCase.assertEquals("AnchorElement", elements[0].propagatedType.name);
    JUnitTestCase.assertEquals("AnchorElement", elements[1].propagatedType.name);
    JUnitTestCase.assertEquals("BodyElement", elements[2].propagatedType.name);
    JUnitTestCase.assertEquals("ButtonElement", elements[3].propagatedType.name);
    JUnitTestCase.assertEquals("DivElement", elements[4].propagatedType.name);
    JUnitTestCase.assertEquals("InputElement", elements[5].propagatedType.name);
    JUnitTestCase.assertEquals("SelectElement", elements[6].propagatedType.name);
    JUnitTestCase.assertEquals("DivElement", elements[7].propagatedType.name);
    JUnitTestCase.assertEquals("Element", elements[8].propagatedType.name);
    JUnitTestCase.assertEquals("Element", elements[9].propagatedType.name);
    JUnitTestCase.assertEquals("Element", elements[10].propagatedType.name);
  }

  /**
   * @param code the code that assigns the value to the variable "v", no matter how. We check that
   *          "v" has expected static and propagated type.
   */
  void check_propagatedReturnType(String code, Type2 expectedStaticType, Type2 expectedPropagatedType) {
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify([source]);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    SimpleIdentifier identifier = EngineTestCase.findNode(unit, code, "v = ", SimpleIdentifier);
    JUnitTestCase.assertSame(expectedStaticType, identifier.staticType);
    JUnitTestCase.assertSame(expectedPropagatedType, identifier.propagatedType);
  }
  static dartSuite() {
    _ut.group('TypePropagationTest', () {
      _ut.test('test_Future_then', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_Future_then);
      });
      _ut.test('test_as', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_as);
      });
      _ut.test('test_assert', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_assert);
      });
      _ut.test('test_assignment', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_assignment);
      });
      _ut.test('test_assignment_afterInitializer', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_assignment_afterInitializer);
      });
      _ut.test('test_forEach', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_forEach);
      });
      _ut.test('test_functionExpression_asInvocationArgument', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_functionExpression_asInvocationArgument);
      });
      _ut.test('test_functionExpression_asInvocationArgument_fromInferredInvocation', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_functionExpression_asInvocationArgument_fromInferredInvocation);
      });
      _ut.test('test_functionExpression_asInvocationArgument_functionExpressionInvocation', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_functionExpression_asInvocationArgument_functionExpressionInvocation);
      });
      _ut.test('test_functionExpression_asInvocationArgument_keepIfLessSpecific', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_functionExpression_asInvocationArgument_keepIfLessSpecific);
      });
      _ut.test('test_functionExpression_asInvocationArgument_replaceIfMoreSpecific', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_functionExpression_asInvocationArgument_replaceIfMoreSpecific);
      });
      _ut.test('test_initializer', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_initializer);
      });
      _ut.test('test_initializer_dereference', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_initializer_dereference);
      });
      _ut.test('test_isNot_conditional', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_isNot_conditional);
      });
      _ut.test('test_isNot_if', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_isNot_if);
      });
      _ut.test('test_isNot_if_logicalOr', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_isNot_if_logicalOr);
      });
      _ut.test('test_isNot_postConditional', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_isNot_postConditional);
      });
      _ut.test('test_isNot_postIf', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_isNot_postIf);
      });
      _ut.test('test_is_conditional', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_conditional);
      });
      _ut.test('test_is_if', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_if);
      });
      _ut.test('test_is_if_lessSpecific', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_if_lessSpecific);
      });
      _ut.test('test_is_if_logicalAnd', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_if_logicalAnd);
      });
      _ut.test('test_is_postConditional', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_postConditional);
      });
      _ut.test('test_is_postIf', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_postIf);
      });
      _ut.test('test_is_subclass', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_subclass);
      });
      _ut.test('test_is_while', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_is_while);
      });
      _ut.test('test_listLiteral_different', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_listLiteral_different);
      });
      _ut.test('test_listLiteral_same', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_listLiteral_same);
      });
      _ut.test('test_mapLiteral_different', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_mapLiteral_different);
      });
      _ut.test('test_mapLiteral_same', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_mapLiteral_same);
      });
      _ut.test('test_propagatedReturnType_function_hasReturnType_returnsNull', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_propagatedReturnType_function_hasReturnType_returnsNull);
      });
      _ut.test('test_propagatedReturnType_function_lessSpecificStaticReturnType', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_propagatedReturnType_function_lessSpecificStaticReturnType);
      });
      _ut.test('test_propagatedReturnType_function_moreSpecificStaticReturnType', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_propagatedReturnType_function_moreSpecificStaticReturnType);
      });
      _ut.test('test_propagatedReturnType_function_noReturnTypeName_blockBody_multipleReturns', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_propagatedReturnType_function_noReturnTypeName_blockBody_multipleReturns);
      });
      _ut.test('test_propagatedReturnType_function_noReturnTypeName_blockBody_oneReturn', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_propagatedReturnType_function_noReturnTypeName_blockBody_oneReturn);
      });
      _ut.test('test_propagatedReturnType_function_noReturnTypeName_expressionBody', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_propagatedReturnType_function_noReturnTypeName_expressionBody);
      });
      _ut.test('test_propagatedReturnType_localFunction', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_propagatedReturnType_localFunction);
      });
      _ut.test('test_query', () {
        final __test = new TypePropagationTest();
        runJUnitTest(__test, __test.test_query);
      });
    });
  }
}
class NonErrorResolverTest extends ResolverTestCase {
  void test_ambiguousExport() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class M {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_ambiguousExport_combinators_hide() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart' hide B;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library L1;", "class A {}", "class B {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library L2;", "class B {}", "class C {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_ambiguousExport_combinators_show() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart' show C;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library L1;", "class A {}", "class B {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library L2;", "class B {}", "class C {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentDefinitionTestNonParameter_formalParameter() {
    Source source = addSource(EngineTestCase.createSource(["f(var v) {", "  return ?v;", "}"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.DEPRECATED_ARGUMENT_DEFINITION_TEST]);
    verify([source]);
  }
  void test_argumentDefinitionTestNonParameter_namedParameter() {
    Source source = addSource(EngineTestCase.createSource(["f({var v : 0}) {", "  return ?v;", "}"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.DEPRECATED_ARGUMENT_DEFINITION_TEST]);
    verify([source]);
  }
  void test_argumentDefinitionTestNonParameter_optionalParameter() {
    Source source = addSource(EngineTestCase.createSource(["f([var v]) {", "  return ?v;", "}"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.DEPRECATED_ARGUMENT_DEFINITION_TEST]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_classWithCall_Function() {
    Source source = addSource(EngineTestCase.createSource([
        "  caller(Function callee) {",
        "    callee();",
        "  }",
        "",
        "  class CallMeBack {",
        "    call() => 0;",
        "  }",
        "",
        "  main() {",
        "    caller(new CallMeBack());",
        "  }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_fieldFormalParameterElement_member() {
    Source source = addSource(EngineTestCase.createSource([
        "class ObjectSink<T> {",
        "  void sink(T object) {",
        "    new TimestampedObject<T>(object);",
        "  }",
        "}",
        "class TimestampedObject<E> {",
        "  E object2;",
        "  TimestampedObject(this.object2);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_functionParameter_generic() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<K> {",
        "  m(f(K k), K v) {",
        "    f(v);",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_typedef_generic() {
    Source source = addSource(EngineTestCase.createSource(["typedef A<T>(T p);", "f(A<int> a) {", "  a(1);", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_Object_Function() {
    Source source = addSource(EngineTestCase.createSource([
        "main() {",
        "  process(() {});",
        "}",
        "process(Object x) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_typedef_local() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef A(int p1, String p2);",
        "A getA() => null;",
        "f() {",
        "  A a = getA();",
        "  a(1, '2');",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_typedef_parameter() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef A(int p1, String p2);",
        "f(A a) {",
        "  a(1, '2');",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_assignmentToFinal_prefixNegate() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  final x = 0;", "  -x;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_assignmentToFinals_importWithPrefix() {
    Source source = addSource(EngineTestCase.createSource([
        "library lib;",
        "import 'lib1.dart' as foo;",
        "main() {",
        "  foo.x = true;",
        "}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "bool x = false;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_breakWithoutLabelInSwitch() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m(int i) {",
        "    switch (i) {",
        "      case 0:",
        "        break;",
        "    }",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_builtInIdentifierAsType_dynamic() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  dynamic x;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_caseBlockNotTerminated() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int p) {",
        "  for (int i = 0; i < 10; i++) {",
        "    switch (p) {",
        "      case 0:",
        "        break;",
        "      case 1:",
        "        continue;",
        "      case 2:",
        "        return;",
        "      case 3:",
        "        throw new Object();",
        "      case 4:",
        "      case 5:",
        "        return;",
        "      case 6:",
        "      default:",
        "        return;",
        "    }",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_caseBlockNotTerminated_lastCase() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int p) {",
        "  switch (p) {",
        "    case 0:",
        "      p = p + 1;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_caseExpressionTypeImplementsEquals_int() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int i) {",
        "  switch(i) {",
        "    case(1) : return 1;",
        "    default: return 0;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_caseExpressionTypeImplementsEquals_Object() {
    Source source = addSource(EngineTestCase.createSource([
        "class IntWrapper {",
        "  final int value;",
        "  const IntWrapper(this.value);",
        "}",
        "",
        "f(IntWrapper intWrapper) {",
        "  switch(intWrapper) {",
        "    case(const IntWrapper(1)) : return 1;",
        "    default: return 0;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_caseExpressionTypeImplementsEquals_String() {
    Source source = addSource(EngineTestCase.createSource([
        "f(String s) {",
        "  switch(s) {",
        "    case('1') : return 1;",
        "    default: return 0;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_concreteClassWithAbstractMember() {
    Source source = addSource(EngineTestCase.createSource(["abstract class A {", "  m();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_conflictingInstanceGetterAndSuperclassMember_instance() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  get v => 0;",
        "}",
        "class B extends A {",
        "  get v => 1;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_conflictingStaticGetterAndInstanceSetter_thisClass() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static get x => 0;",
        "  static set x(int p) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_conflictingStaticSetterAndInstanceMember_thisClass_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static x() {}",
        "  static set x(int p) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constConstructorWithNonConstSuper_redirectingFactory() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A();",
        "}",
        "class B implements C {",
        "  const B();",
        "}",
        "class C extends A {",
        "  const factory C() = B;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constConstructorWithNonFinalField_finalInstanceVar() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final int x = 0;", "  const A();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constConstructorWithNonFinalField_mixin() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  a() {}",
        "}",
        "class B extends Object with A {",
        "  const B();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constConstructorWithNonFinalField_static() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static int x;", "  const A();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constConstructorWithNonFinalField_syntheticField() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "  set x(value) {}",
        "  get x {return 0;}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constEval_propertyExtraction_fieldStatic_targetType() {
    addSource2("/math.dart", EngineTestCase.createSource(["library math;", "const PI = 3.14;"]));
    Source source = addSource(EngineTestCase.createSource(["import 'math.dart' as math;", "const C = math.PI;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constEval_propertyExtraction_methodStatic_targetType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "  static m() {}",
        "}",
        "const C = A.m;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constEval_symbol() {
    addSource2("/math.dart", EngineTestCase.createSource(["library math;", "const PI = 3.14;"]));
    Source source = addSource(EngineTestCase.createSource(["const C = #foo;", "foo() {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constEvalTypeBoolNumString_equal() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "class B {",
        "  final v;",
        "  const B.a1(bool p) : v = p == true;",
        "  const B.a2(bool p) : v = p == false;",
        "  const B.a3(bool p) : v = p == 0;",
        "  const B.a4(bool p) : v = p == 0.0;",
        "  const B.a5(bool p) : v = p == '';",
        "  const B.b1(int p) : v = p == true;",
        "  const B.b2(int p) : v = p == false;",
        "  const B.b3(int p) : v = p == 0;",
        "  const B.b4(int p) : v = p == 0.0;",
        "  const B.b5(int p) : v = p == '';",
        "  const B.c1(String p) : v = p == true;",
        "  const B.c2(String p) : v = p == false;",
        "  const B.c3(String p) : v = p == 0;",
        "  const B.c4(String p) : v = p == 0.0;",
        "  const B.c5(String p) : v = p == '';",
        "  const B.n1(num p) : v = p == null;",
        "  const B.n2(num p) : v = null == p;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_constEvalTypeBoolNumString_notEqual() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "class B {",
        "  final v;",
        "  const B.a1(bool p) : v = p != true;",
        "  const B.a2(bool p) : v = p != false;",
        "  const B.a3(bool p) : v = p != 0;",
        "  const B.a4(bool p) : v = p != 0.0;",
        "  const B.a5(bool p) : v = p != '';",
        "  const B.b1(int p) : v = p != true;",
        "  const B.b2(int p) : v = p != false;",
        "  const B.b3(int p) : v = p != 0;",
        "  const B.b4(int p) : v = p != 0.0;",
        "  const B.b5(int p) : v = p != '';",
        "  const B.c1(String p) : v = p != true;",
        "  const B.c2(String p) : v = p != false;",
        "  const B.c3(String p) : v = p != 0;",
        "  const B.c4(String p) : v = p != 0.0;",
        "  const B.c5(String p) : v = p != '';",
        "  const B.n1(num p) : v = p != null;",
        "  const B.n2(num p) : v = null != p;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constNotInitialized_field() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static const int x = 0;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constNotInitialized_local() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  const int x = 0;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constructorDeclaration_scope_signature() {
    Source source = addSource(EngineTestCase.createSource([
        "const app = 0;",
        "class A {",
        "  A(@app int app) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constWithNonConstantArgument_literals() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(a, b, c, d);",
        "}",
        "f() { return const A(true, 0, 1.0, '2'); }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constWithTypeParameters_direct() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> {",
        "  static const V = const A<int>();",
        "  const A();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constWithUndefinedConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A.name();",
        "}",
        "f() {",
        "  return const A.name();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_constWithUndefinedConstructorDefault() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "f() {",
        "  return const A();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_defaultValueInFunctionTypeAlias() {
    Source source = addSource(EngineTestCase.createSource(["typedef F([x]);"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_defaultValueInFunctionTypedParameter_named() {
    Source source = addSource(EngineTestCase.createSource(["f(g({p})) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_defaultValueInFunctionTypedParameter_optional() {
    Source source = addSource(EngineTestCase.createSource(["f(g([p])) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_duplicateDefinition_emptyName() {
    Source source = addSource(EngineTestCase.createSource([
        "Map _globalMap = {",
        "  'a' : () {},",
        "  'b' : () {}",
        "};"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_duplicateDefinition_getter() {
    Source source = addSource(EngineTestCase.createSource(["bool get a => true;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_dynamicIdentifier() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  var v = dynamic;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_expectedOneListTypeArgument() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  <int> [];", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_expectedTwoMapTypeArguments() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  <int, int> {};", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_exportOfNonLibrary_libraryDeclared() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "export 'lib1.dart';"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_exportOfNonLibrary_libraryNotDeclared() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "export 'lib1.dart';"]));
    addSource2("/lib1.dart", EngineTestCase.createSource([""]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_extraPositionalArguments_function() {
    Source source = addSource(EngineTestCase.createSource(["f(p1, p2) {}", "main() {", "  f(1, 2);", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_extraPositionalArguments_Function() {
    Source source = addSource(EngineTestCase.createSource(["f(Function a) {", "  a(1, 2);", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_extraPositionalArguments_typedef_local() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef A(p1, p2);",
        "A getA() => null;",
        "f() {",
        "  A a = getA();",
        "  a(1, 2);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_extraPositionalArguments_typedef_parameter() {
    Source source = addSource(EngineTestCase.createSource(["typedef A(p1, p2);", "f(A a) {", "  a(1, 2);", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_fieldInitializedByMultipleInitializers() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int x;",
        "  int y;",
        "  A() : x = 0, y = 0 {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_fieldInitializedInInitializerAndDeclaration_fieldNotFinal() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x = 0;", "  A() : x = 1 {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_fieldInitializedInInitializerAndDeclaration_finalFieldNotSet() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final int x;", "  A() : x = 1 {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_fieldInitializerOutsideConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A(this.x) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_fieldInitializerOutsideConstructor_defaultParameters() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A([this.x]) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_fieldInitializerRedirectingConstructor_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "class B extends A {",
        "  int x;",
        "  B(this.x) : super();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_finalInitializedInDeclarationAndConstructor_initializer() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x;", "  A() : x = 1 {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_finalInitializedInDeclarationAndConstructor_initializingFormal() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x;", "  A(this.x) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_finalNotInitialized_atDeclaration() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final int x = 0;", "  A() {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_finalNotInitialized_fieldFormal() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final int x = 0;", "  A() {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_finalNotInitialized_functionTypedFieldFormal() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final Function x;",
        "  A(int this.x(int p)) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_finalNotInitialized_hasNativeClause_hasConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A native 'something' {",
        "  final int x;",
        "  A() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.NATIVE_CLAUSE_IN_NON_SDK_CODE]);
    verify([source]);
  }
  void test_finalNotInitialized_hasNativeClause_noConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A native 'something' {", "  final int x;", "}"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.NATIVE_CLAUSE_IN_NON_SDK_CODE]);
    verify([source]);
  }
  void test_finalNotInitialized_initializer() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final int x;", "  A() : x = 0 {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_finalNotInitialized_redirectingConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final int x;",
        "  A(this.x);",
        "  A.named() : this (42);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_functionDeclaration_scope_returnType() {
    Source source = addSource(EngineTestCase.createSource(["int f(int) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_functionDeclaration_scope_signature() {
    Source source = addSource(EngineTestCase.createSource(["const app = 0;", "f(@app int app) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_functionTypeAlias_scope_returnType() {
    Source source = addSource(EngineTestCase.createSource(["typedef int f(int);"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_functionTypeAlias_scope_signature() {
    Source source = addSource(EngineTestCase.createSource(["const app = 0;", "typedef int f(@app int app);"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_functionWithoutCall() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A implements Function {",
        "}",
        "class B implements A {",
        "  void call() {}",
        "}",
        "class C extends A {",
        "  void call() {}",
        "}",
        "class D extends C {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_constructorName() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.named() {}",
        "}",
        "class B {",
        "  var v;",
        "  B() : v = new A.named();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_prefixedIdentifier() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var f;",
        "}",
        "class B {",
        "  var v;",
        "  B(A a) : v = a.f;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_qualifiedMethodInvocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  f() {}",
        "}",
        "class B {",
        "  var v;",
        "  B() : v = new A().f();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_qualifiedPropertyAccess() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var f;",
        "}",
        "class B {",
        "  var v;",
        "  B() : v = new A().f;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_staticField_thisClass() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f;",
        "  static var f;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_staticGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f;",
        "  static get f => 42;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_staticMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f();",
        "  static f() => 42;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_topLevelField() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f;",
        "}",
        "var f = 42;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_topLevelFunction() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f();",
        "}",
        "f() => 42;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_topLevelGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f;",
        "}",
        "get f => 42;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_typeParameter() {
    Source source = addSource(EngineTestCase.createSource(["class A<T> {", "  var v;", "  A(p) : v = (p is T);", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_importDuplicatedLibraryName() {
    Source source = addSource(EngineTestCase.createSource([
        "library test;",
        "import 'lib.dart';",
        "import 'lib.dart';"]));
    addSource2("/lib.dart", "library lib;");
    resolve(source);
    assertErrors(source, [
        HintCode.UNUSED_IMPORT,
        HintCode.UNUSED_IMPORT,
        HintCode.DUPLICATE_IMPORT]);
    verify([source]);
  }
  void test_importOfNonLibrary_libraryDeclared() {
    Source source = addSource(EngineTestCase.createSource(["library lib;", "import 'part.dart';", "A a;"]));
    addSource2("/part.dart", EngineTestCase.createSource(["library lib1;", "class A {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_importOfNonLibrary_libraryNotDeclared() {
    Source source = addSource(EngineTestCase.createSource(["library lib;", "import 'part.dart';", "A a;"]));
    addSource2("/part.dart", EngineTestCase.createSource(["class A {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_importPrefixes_withFirstLetterDifference() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart' as math;",
        "import 'lib2.dart' as path;",
        "main() {",
        "  math.test1();",
        "  path.test2();",
        "}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "test1() {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "test2() {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_inconsistentCaseExpressionTypes() {
    Source source = addSource(EngineTestCase.createSource([
        "f(var p) {",
        "  switch (p) {",
        "    case 1:",
        "      break;",
        "    case 2:",
        "      break;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_accessors_typeParameter2() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A<E> {",
        "  E get x {return 1;}",
        "}",
        "class B<E> {",
        "  E get x {return 1;}",
        "}",
        "class C<E> extends A<E> implements B<E> {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_accessors_typeParameters_diamond() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class F<E> extends B<E> {}",
        "class D<E> extends F<E> {",
        "  external E get g;",
        "}",
        "abstract class C<E> {",
        "  E get g;",
        "}",
        "abstract class B<E> implements C<E> {",
        "  E get g { return null; }",
        "}",
        "class A<E> extends B<E> implements D<E> {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_accessors_typeParameters1() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A<E> {",
        "  E get x;",
        "}",
        "abstract class B<E> {",
        "  E get x;",
        "}",
        "class C<E> implements A<E>, B<E> {",
        "  E get x => 1;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_methods_typeParameter2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<E> {",
        "  x(E e) {}",
        "}",
        "class B<E> {",
        "  x(E e) {}",
        "}",
        "class C<E> extends A<E> implements B<E> {",
        "  x(E e) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_methods_typeParameters1() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<E> {",
        "  x(E e) {}",
        "}",
        "class B<E> {",
        "  x(E e) {}",
        "}",
        "class C<E> implements A<E>, B<E> {",
        "  x(E e) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_simple() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  x();",
        "}",
        "abstract class B {",
        "  x();",
        "}",
        "class C implements A, B {",
        "  x() {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_initializingFormalForNonExistantField() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A(this.x) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_instanceAccessToStaticMember_fromComment() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static m() {}",
        "}",
        "/// [A.m]",
        "main() {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_instanceAccessToStaticMember_topLevel() {
    Source source = addSource(EngineTestCase.createSource(["m() {}", "main() {", "  m();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_instanceMemberAccessFromStatic_fromComment() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "  /// [m]",
        "  static foo() {",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_field() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart';",
        "class B extends A {",
        "  _m() {}",
        "}"]));
    addSource2("/lib.dart", EngineTestCase.createSource(["library L;", "class A {", "  static var _m;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_method() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart';",
        "class B extends A {",
        "  _m() {}",
        "}"]));
    addSource2("/lib.dart", EngineTestCase.createSource(["library L;", "class A {", "  static _m() {}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.OVERRIDDING_PRIVATE_MEMBER]);
    verify([source]);
  }
  void test_invalidAnnotation_constantVariable() {
    Source source = addSource(EngineTestCase.createSource(["const C = 0;", "@C", "main() {", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidAnnotation_importWithPrefix_constantVariable() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "const C = 0;"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "@p.C", "main() {", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidAnnotation_importWithPrefix_constConstructor() {
    addSource2("/lib.dart", EngineTestCase.createSource([
        "library lib;",
        "class A {",
        "  const A.named(int p);",
        "}"]));
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart' as p;",
        "@p.A.named(42)",
        "main() {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidAssignment() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var x;", "  var y;", "  x = y;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidAssignment_compoundAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "class byte {",
        "  int _value;",
        "  byte(this._value);",
        "  byte operator +(int val) {}",
        "}",
        "",
        "void main() {",
        "  byte b = new byte(52);",
        "  b += 3;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidAssignment_defaultValue_named() {
    Source source = addSource(EngineTestCase.createSource(["f({String x: '0'}) {", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidAssignment_defaultValue_optional() {
    Source source = addSource(EngineTestCase.createSource(["f([String x = '0']) {", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidAssignment_toDynamic() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var g;", "  g = () => 0;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidFactoryNameNotAClass() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  factory A() {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidMethodOverrideNamedParamType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m({int a}) {}",
        "}",
        "class B implements A {",
        "  m({int a, int b}) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideDifferentDefaultValues_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m({int p : 0}) {}",
        "}",
        "class B extends A {",
        "  m({int p : 0}) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideDifferentDefaultValues_positional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m([int p = 0]) {}",
        "}",
        "class B extends A {",
        "  m([int p = 0]) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideDifferentDefaultValues_positional_changedOrder() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m([int a = 0, String b = '0']) {}",
        "}",
        "class B extends A {",
        "  m([int b = 0, String a = '0']) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideNamed_unorderedNamedParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m({a, b}) {}",
        "}",
        "class B extends A {",
        "  m({b, a}) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideRequired_less() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m(a, b) {}",
        "}",
        "class B extends A {",
        "  m(a, [b]) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideRequired_same() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m(a) {}",
        "}",
        "class B extends A {",
        "  m(a) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_interface() {
    Source source = addSource2("/test.dart", EngineTestCase.createSource([
        "abstract class A {",
        "  num m();",
        "}",
        "class B implements A {",
        "  int m() { return 1; }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_interface2() {
    Source source = addSource2("/test.dart", EngineTestCase.createSource([
        "abstract class A {",
        "  num m();",
        "}",
        "abstract class B implements A {",
        "}",
        "class C implements B {",
        "  int m() { return 1; }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_mixin() {
    Source source = addSource2("/test.dart", EngineTestCase.createSource([
        "class A {",
        "  num m() { return 0; }",
        "}",
        "class B extends Object with A {",
        "  int m() { return 1; }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_parameterizedTypes() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A<E> {",
        "  List<E> m();",
        "}",
        "class B extends A<dynamic> {",
        "  List<dynamic> m() { return new List<dynamic>(); }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_sameType() {
    Source source = addSource2("/test.dart", EngineTestCase.createSource([
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "  int m() { return 1; }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_superclass() {
    Source source = addSource2("/test.dart", EngineTestCase.createSource([
        "class A {",
        "  num m() { return 0; }",
        "}",
        "class B extends A {",
        "  int m() { return 1; }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_superclass2() {
    Source source = addSource2("/test.dart", EngineTestCase.createSource([
        "class A {",
        "  num m() { return 0; }",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  int m() { return 1; }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidOverrideReturnType_returnType_void() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m() {}",
        "}",
        "class B extends A {",
        "  int m() {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidReferenceToThis_constructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() {", "    var v = this;", "  }", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidReferenceToThis_instanceMethod() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  m() {", "    var v = this;", "  }", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidTypeArgumentForKey() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {",
        "    return const <int, int>{};",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidTypeArgumentInConstList() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<E> {",
        "  m() {",
        "    return <E>[];",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invalidTypeArgumentInConstMap() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<E> {",
        "  m() {",
        "    return <String, E>{};",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invocationOfNonFunction_dynamic() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var f;",
        "}",
        "class B extends A {",
        "  g() {",
        "    f();",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invocationOfNonFunction_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var g;",
        "}",
        "f() {",
        "  A a;",
        "  a.g();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invocationOfNonFunction_localVariable() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var g;", "  g();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invocationOfNonFunction_localVariable_dynamic() {
    Source source = addSource(EngineTestCase.createSource(["f() {}", "main() {", "  var v = f;", "  v();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invocationOfNonFunction_localVariable_dynamic2() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {}",
        "main() {",
        "  var v = f;",
        "  v = 1;",
        "  v();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invocationOfNonFunction_Object() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  Object v = null;", "  v();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_listElementTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v1 = <int> [42];", "var v2 = const <int> [42];"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_mapKeyTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v = <String, int > {'a' : 1};"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_memberWithClassName_setter() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  set A(v) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_methodDeclaration_scope_signature() {
    Source source = addSource(EngineTestCase.createSource([
        "const app = 0;",
        "class A {",
        "  foo(@app int app) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_misMatchedGetterAndSetterTypes_instance_sameTypes() {
    Source source = addSource(EngineTestCase.createSource([
        "class C {",
        "  int get x => 0;",
        "  set x(int v) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_misMatchedGetterAndSetterTypes_instance_unspecifiedGetter() {
    Source source = addSource(EngineTestCase.createSource(["class C {", "  get x => 0;", "  set x(String v) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_misMatchedGetterAndSetterTypes_instance_unspecifiedSetter() {
    Source source = addSource(EngineTestCase.createSource(["class C {", "  int get x => 0;", "  set x(v) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_misMatchedGetterAndSetterTypes_topLevel_sameTypes() {
    Source source = addSource(EngineTestCase.createSource(["int get x => 0;", "set x(int v) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_misMatchedGetterAndSetterTypes_topLevel_unspecifiedGetter() {
    Source source = addSource(EngineTestCase.createSource(["get x => 0;", "set x(String v) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_misMatchedGetterAndSetterTypes_topLevel_unspecifiedSetter() {
    Source source = addSource(EngineTestCase.createSource(["int get x => 0;", "set x(v) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_mixedReturnTypes_differentScopes() {
    Source source = addSource(EngineTestCase.createSource([
        "class C {",
        "  m(int x) {",
        "    f(int y) {",
        "      return;",
        "    }",
        "    f(x);",
        "    return 0;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_mixedReturnTypes_sameKind() {
    Source source = addSource(EngineTestCase.createSource([
        "class C {",
        "  m(int x) {",
        "    if (x < 0) {",
        "      return 1;",
        "    }",
        "    return 0;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_mixinDeclaresConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "class B extends Object with A {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_mixinDeclaresConstructor_factory() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  factory A() {}",
        "}",
        "class B extends Object with A {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_mixinInheritsFromNotObject_classDeclaration_mixTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B = Object with A;",
        "class C extends Object with B {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_mixinInheritsFromNotObject_typedef_mixTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B = Object with A;",
        "class C = Object with B;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_multipleSuperInitializers_no() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "class B extends A {", "  B() {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_multipleSuperInitializers_single() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  B() : super() {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_newWithAbstractClass_factory() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  factory A() { return new B(); }",
        "}",
        "class B implements A {",
        "  B() {}",
        "}",
        "A f() {",
        "  return new A();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_newWithUndefinedConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.name() {}",
        "}",
        "f() {",
        "  new A.name();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_newWithUndefinedConstructorDefault() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() {}", "}", "f() {", "  new A();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_abstractOverridesConcrete_accessor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get g => 0;",
        "}",
        "abstract class B extends A {",
        "  int get g;",
        "}",
        "class C extends B {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_abstractOverridesConcrete_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m(p) {}",
        "}",
        "abstract class B extends A {",
        "  m(p);",
        "}",
        "class C extends B {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_noSuchMethod_accessor() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int get g;",
        "}",
        "class B extends A {",
        "  noSuchMethod(v) => '';",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_noSuchMethod_method() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  m(p);",
        "}",
        "class B extends A {",
        "  noSuchMethod(v) => '';",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonBoolExpression_functionType() {
    Source source = addSource(EngineTestCase.createSource([
        "bool makeAssertion() => true;",
        "f() {",
        "  assert(makeAssertion);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonBoolExpression_interfaceType() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  assert(true);", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonBoolNegationExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "f(bool pb, pd) {",
        "  !true;",
        "  !false;",
        "  !pb;",
        "  !pd;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstantDefaultValue_function_named() {
    Source source = addSource(EngineTestCase.createSource(["f({x : 2 + 3}) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstantDefaultValue_function_positional() {
    Source source = addSource(EngineTestCase.createSource(["f([x = 2 + 3]) {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstantDefaultValue_inConstructor_named() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A({x : 2 + 3}) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstantDefaultValue_inConstructor_positional() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A([x = 2 + 3]) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstantDefaultValue_method_named() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  m({x : 2 + 3}) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstantDefaultValue_method_positional() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  m([x = 2 + 3]) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstantValueInInitializer_namedArgument() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final a;",
        "  const A({this.a});",
        "}",
        "class B extends A {",
        "  const B({b}) : super(a: b);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstCaseExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "f(Type t) {",
        "  switch (t) {",
        "    case bool:",
        "    case int:",
        "      return true;",
        "    default:",
        "      return false;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstMapAsExpressionStatement_const() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  const {'a' : 0, 'b' : 1};", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstMapAsExpressionStatement_notExpressionStatement() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  var m = {'a' : 0, 'b' : 1};", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstMapAsExpressionStatement_typeArguments() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  <String, int> {'a' : 0, 'b' : 1};", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstValueInInitializer_binary_bool() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final v;",
        "  const A.a1(bool p) : v = p && true;",
        "  const A.a2(bool p) : v = true && p;",
        "  const A.b1(bool p) : v = p || true;",
        "  const A.b2(bool p) : v = true || p;",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_binary_dynamic() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final v;",
        "  const A.a1(p) : v = p + 5;",
        "  const A.a2(p) : v = 5 + p;",
        "  const A.b1(p) : v = p - 5;",
        "  const A.b2(p) : v = 5 - p;",
        "  const A.c1(p) : v = p * 5;",
        "  const A.c2(p) : v = 5 * p;",
        "  const A.d1(p) : v = p / 5;",
        "  const A.d2(p) : v = 5 / p;",
        "  const A.e1(p) : v = p ~/ 5;",
        "  const A.e2(p) : v = 5 ~/ p;",
        "  const A.f1(p) : v = p > 5;",
        "  const A.f2(p) : v = 5 > p;",
        "  const A.g1(p) : v = p < 5;",
        "  const A.g2(p) : v = 5 < p;",
        "  const A.h1(p) : v = p >= 5;",
        "  const A.h2(p) : v = 5 >= p;",
        "  const A.i1(p) : v = p <= 5;",
        "  const A.i2(p) : v = 5 <= p;",
        "  const A.j1(p) : v = p % 5;",
        "  const A.j2(p) : v = 5 % p;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_nonConstValueInInitializer_binary_int() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final v;",
        "  const A.a1(int p) : v = p ^ 5;",
        "  const A.a2(int p) : v = 5 ^ p;",
        "  const A.b1(int p) : v = p & 5;",
        "  const A.b2(int p) : v = 5 & p;",
        "  const A.c1(int p) : v = p | 5;",
        "  const A.c2(int p) : v = 5 | p;",
        "  const A.d1(int p) : v = p >> 5;",
        "  const A.d2(int p) : v = 5 >> p;",
        "  const A.e1(int p) : v = p << 5;",
        "  const A.e2(int p) : v = 5 << p;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstValueInInitializer_binary_num() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final v;",
        "  const A.a1(num p) : v = p + 5;",
        "  const A.a2(num p) : v = 5 + p;",
        "  const A.b1(num p) : v = p - 5;",
        "  const A.b2(num p) : v = 5 - p;",
        "  const A.c1(num p) : v = p * 5;",
        "  const A.c2(num p) : v = 5 * p;",
        "  const A.d1(num p) : v = p / 5;",
        "  const A.d2(num p) : v = 5 / p;",
        "  const A.e1(num p) : v = p ~/ 5;",
        "  const A.e2(num p) : v = 5 ~/ p;",
        "  const A.f1(num p) : v = p > 5;",
        "  const A.f2(num p) : v = 5 > p;",
        "  const A.g1(num p) : v = p < 5;",
        "  const A.g2(num p) : v = 5 < p;",
        "  const A.h1(num p) : v = p >= 5;",
        "  const A.h2(num p) : v = 5 >= p;",
        "  const A.i1(num p) : v = p <= 5;",
        "  const A.i2(num p) : v = 5 <= p;",
        "  const A.j1(num p) : v = p % 5;",
        "  const A.j2(num p) : v = 5 % p;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstValueInInitializer_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final int a;",
        "  const A() : a = 5;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstValueInInitializer_redirecting() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A.named(p);",
        "  const A() : this.named(42);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstValueInInitializer_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(p);",
        "}",
        "class B extends A {",
        "  const B() : super(42);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonConstValueInInitializer_unary() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final v;",
        "  const A.a(bool p) : v = !p;",
        "  const A.b(int p) : v = ~p;",
        "  const A.c(num p) : v = -p;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonGenerativeConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.named() {}",
        "  factory A() {}",
        "}",
        "class B extends A {",
        "  B() : super.named();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonTypeInCatchClause_isClass() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  try {",
        "  } on String catch (e) {",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonTypeInCatchClause_isFunctionTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef F();",
        "f() {",
        "  try {",
        "  } on F catch (e) {",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonTypeInCatchClause_isTypeParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> {",
        "  f() {",
        "    try {",
        "    } on T catch (e) {",
        "    }",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonTypeInCatchClause_noType() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  try {", "  } catch (e) {", "  }", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonVoidReturnForOperator_no() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator []=(a, b) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonVoidReturnForOperator_void() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  void operator []=(a, b) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonVoidReturnForSetter_function_no() {
    Source source = addSource("set x(v) {}");
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonVoidReturnForSetter_function_void() {
    Source source = addSource("void set x(v) {}");
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonVoidReturnForSetter_method_no() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  set x(v) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_nonVoidReturnForSetter_method_void() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  void set x(v) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_null_callMethod() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  null.m();", "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_null_callOperator() {
    Source source = addSource(EngineTestCase.createSource([
        "main() {",
        "  null + 5;",
        "  null == 5;",
        "  null[0];",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_optionalParameterInOperator_required() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator +(p) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_prefixCollidesWithTopLevelMembers() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A {}"]));
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart' as p;",
        "typedef P();",
        "p2() {}",
        "var p3;",
        "class p4 {}",
        "p.A a;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_propagateTypeArgs_intoBounds() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A<E> {}",
        "abstract class B<F> implements A<F>{}",
        "abstract class C<G, H extends A<G>> {}",
        "class D<I> extends C<I, B<I>> {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_propagateTypeArgs_intoSupertype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> {",
        "  A(T p);",
        "  A.named(T p);",
        "}",
        "class B<S> extends A<S> {",
        "  B(S p) : super(p);",
        "  B.named(S p) : super.named(p);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_proxy_annotation_prefixed() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'meta.dart';",
        "@proxy",
        "class A {}",
        "f(A a) {",
        "  a.m();",
        "  var x = a.g;",
        "  a.s = 1;",
        "  var y = a + a;",
        "  a++;",
        "  ++a;",
        "}"]));
    addSource2("/meta.dart", EngineTestCase.createSource([
        "library meta;",
        "const proxy = const _Proxy();",
        "class _Proxy { const _Proxy(); }"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_proxy_annotation_prefixed2() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'meta.dart';",
        "@proxy",
        "class A {}",
        "class B {",
        "  f(A a) {",
        "    a.m();",
        "    var x = a.g;",
        "    a.s = 1;",
        "    var y = a + a;",
        "    a++;",
        "    ++a;",
        "  }",
        "}"]));
    addSource2("/meta.dart", EngineTestCase.createSource([
        "library meta;",
        "const proxy = const _Proxy();",
        "class _Proxy { const _Proxy(); }"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_proxy_annotation_prefixed3() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'meta.dart';",
        "class B {",
        "  f(A a) {",
        "    a.m();",
        "    var x = a.g;",
        "    a.s = 1;",
        "    var y = a + a;",
        "    a++;",
        "    ++a;",
        "  }",
        "}",
        "@proxy",
        "class A {}"]));
    addSource2("/meta.dart", EngineTestCase.createSource([
        "library meta;",
        "const proxy = const _Proxy();",
        "class _Proxy { const _Proxy(); }"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_proxy_annotation_simple() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'meta.dart';",
        "@proxy",
        "class B {",
        "  m() {",
        "    n();",
        "    var x = g;",
        "    s = 1;",
        "    var y = this + this;",
        "  }",
        "}"]));
    addSource2("/meta.dart", EngineTestCase.createSource([
        "library meta;",
        "const proxy = const _Proxy();",
        "class _Proxy { const _Proxy(); }"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_recursiveConstructorRedirect() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.a() : this.b();",
        "  A.b() : this.c();",
        "  A.c() {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_recursiveFactoryRedirect() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  factory A() = B;",
        "}",
        "class B implements A {",
        "  factory B() = C;",
        "}",
        "class C implements B {",
        "  factory C() {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_redirectToInvalidFunctionType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B {",
        "  A(int p) {}",
        "}",
        "class B {",
        "  factory B(int p) = A;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_redirectToInvalidReturnType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "class B extends A {",
        "  factory B() = A;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_redirectToNonConstConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A.a();",
        "  const factory A.b() = A.a;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_referenceToDeclaredVariableInInitializer_constructorName() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.x() {}",
        "}",
        "f() {",
        "  var x = new A.x();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_referenceToDeclaredVariableInInitializer_methodName() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  x() {}",
        "}",
        "f(A a) {",
        "  var x = a.x();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_referenceToDeclaredVariableInInitializer_propertyName() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var x;",
        "}",
        "f(A a) {",
        "  var x = a.x;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_rethrowOutsideCatch() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m() {",
        "    try {} catch (e) {rethrow;}",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnInGenerativeConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() { return; }", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnOfInvalidType_dynamic() {
    Source source = addSource(EngineTestCase.createSource([
        "class TypeError {}",
        "class A {",
        "  static void testLogicalOp() {",
        "    testOr(a, b, onTypeError) {",
        "      try {",
        "        return a || b;",
        "      } on TypeError catch (t) {",
        "        return onTypeError;",
        "      }",
        "    }",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnOfInvalidType_dynamicAsTypeArgument() {
    Source source = addSource(EngineTestCase.createSource([
        "class I<T> {",
        "  factory I() => new A<T>();",
        "}",
        "class A<T> implements I {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnOfInvalidType_subtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "A f(B b) { return b; }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnOfInvalidType_supertype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "B f(A a) { return a; }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnOfInvalidType_void() {
    Source source = addSource(EngineTestCase.createSource([
        "void f1() {}",
        "void f2() { return; }",
        "void f3() { return null; }",
        "void f4() { return g1(); }",
        "void f5() { return g2(); }",
        "g1() {}",
        "void g2() {}",
        ""]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnWithoutValue_noReturnType() {
    Source source = addSource(EngineTestCase.createSource(["f() { return; }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_returnWithoutValue_void() {
    Source source = addSource(EngineTestCase.createSource(["void f() { return; }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_reversedTypeArguments() {
    Source source = addSource(EngineTestCase.createSource([
        "class Codec<S1, T1> {",
        "  Codec<T1, S1> get inverted => new _InvertedCodec<T1, S1>(this);",
        "}",
        "class _InvertedCodec<T2, S2> extends Codec<T2, S2> {",
        "  _InvertedCodec(Codec<S2, T2> codec);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_annotation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A.name();",
        "}",
        "@A.name()",
        "main() {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static m() {}",
        "}",
        "main() {",
        "  A.m;",
        "  A.m();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_propertyAccess_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static var f;",
        "}",
        "main() {",
        "  A.f;",
        "  A.f = 1;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_propertyAccess_propertyAccessor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static get f => 42;",
        "  static set f(x) {}",
        "}",
        "main() {",
        "  A.f;",
        "  A.f = 1;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_superInInvalidContext() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  B() {",
        "    var v = super.m();",
        "  }",
        "  n() {",
        "    var v = super.m();",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_returnClass_withTypeAlias() {
    Source source = addSource(EngineTestCase.createSource(["typedef B A();", "class B {", "  A a;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_const() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "class G<E extends A> {",
        "  const G();",
        "}",
        "f() { return const G<B>(); }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_new() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_typeArgumentList_0() {
    Source source = addSource(EngineTestCase.createSource(["abstract class A<T extends A>{}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_typeArgumentList_1() {
    Source source = addSource(EngineTestCase.createSource(["abstract class A<T extends A<A>>{}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_typeArgumentList_20() {
    Source source = addSource(EngineTestCase.createSource(["abstract class A<T extends A<A<A<A<A<A<A<A<A<A<A<A<A<A<A<A<A<A<A<A<A>>>>>>>>>>>>>>>>>>>>>{}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_booleanAnd_useInRight() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  p is String && p.length != 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_booleanAnd_useInRight_accessedInClosureRight_noAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  (p is String) && callMe(() { p.length; });",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_conditional_useInThen() {
    Source source = addSource(EngineTestCase.createSource(["main(Object p) {", "  p is String ? p.length : 0;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_conditional_useInThen_accessedInClosure_noAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  p is String ? callMe(() { p.length; }) : 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_functionType_arg_ignoreIfNotMoreSpecific() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef FuncB(B b);",
        "typedef FuncA(A a);",
        "class A {}",
        "class B {}",
        "main(FuncA f) {",
        "  if (f is FuncB) {",
        "    f(new A());",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_functionType_return_ignoreIfNotMoreSpecific() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef FuncDynToDyn(x);",
        "typedef void FuncDynToVoid(x);",
        "class A {}",
        "main(FuncDynToDyn f) {",
        "  if (f is FuncDynToVoid) {",
        "    A a = f(null);",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_functionType_return_voidToDynamic() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef FuncDynToDyn(x);",
        "typedef void FuncDynToVoid(x);",
        "class A {}",
        "main(FuncDynToVoid f) {",
        "  if (f is FuncDynToDyn) {",
        "    A a = f(null);",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_accessedInClosure_noAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  if (p is String) {",
        "    callMe(() {",
        "      p.length;",
        "    });",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_extends_moreSpecific() {
    Source source = addSource(EngineTestCase.createSource([
        "class V {}",
        "class VP extends V {}",
        "class A<T> {}",
        "class B<S> extends A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B<VP>) {",
        "    p.b;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_hasAssignment_outsideAfter() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "  p = 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_hasAssignment_outsideBefore() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p, Object p2) {",
        "  p = p2;",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_implements_moreSpecific() {
    Source source = addSource(EngineTestCase.createSource([
        "class V {}",
        "class VP extends V {}",
        "class A<T> {}",
        "class B<S> implements A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B<VP>) {",
        "    p.b;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_inClosure_assignedAfter_inSameFunction() {
    Source source = addSource(EngineTestCase.createSource([
        "main() {",
        "  f(Object p) {",
        "    if (p is String) {",
        "      p.length;",
        "    }",
        "    p = 0;",
        "  };",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_is_and_left() {
    Source source = addSource(EngineTestCase.createSource([
        "bool tt() => true;",
        "main(Object p) {",
        "  if (p is String && tt()) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_is_and_right() {
    Source source = addSource(EngineTestCase.createSource([
        "bool tt() => true;",
        "main(Object p) {",
        "  if (tt() && p is String) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_is_and_subThenSuper() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var a;",
        "}",
        "class B extends A {",
        "  var b;",
        "}",
        "main(Object p) {",
        "  if (p is B && p is A) {",
        "    p.a;",
        "    p.b;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_is_parenthesized() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if ((p is String)) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typePromotion_if_is_single() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeType_class() {
    Source source = addSource(EngineTestCase.createSource(["class C {}", "f(Type t) {}", "main() {", "  f(C);", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeType_class_prefixed() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class C {}"]));
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart' as p;",
        "f(Type t) {}",
        "main() {",
        "  f(p.C);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeType_functionTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef F();",
        "f(Type t) {}",
        "main() {",
        "  f(F);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_typeType_functionTypeAlias_prefixed() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "typedef F();"]));
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart' as p;",
        "f(Type t) {}",
        "main() {",
        "  f(p.F);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_explicit_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.named() {}",
        "}",
        "class B extends A {",
        "  B() : super.named();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_explicit_unnamed() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "class B extends A {",
        "  B() : super();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_hasOptionalParameters() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A([p]) {}",
        "}",
        "class B extends A {",
        "  B();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_implicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "class B extends A {",
        "  B();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_implicit_typeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "class M {}",
        "class A = Object with M;",
        "class B extends A {",
        "  B();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_redirecting() {
    Source source = addSource(EngineTestCase.createSource([
        "class Foo {",
        "  Foo.ctor();",
        "}",
        "class Bar extends Foo {",
        "  Bar() : this.ctor();",
        "  Bar.ctor() : super.ctor();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedGetter_typeSubstitution() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<E> {",
        "  E element;",
        "}",
        "class B extends A<List> {",
        "  m() {",
        "    element.last;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedIdentifier_hide() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "export 'lib1.dart' hide a;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedIdentifier_show() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "export 'lib1.dart' show a;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedMethod_functionExpression_callMethod() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  (() => null).call();", "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedMethod_functionExpression_directCall() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  (() => null)();", "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedOperator_index() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  operator [](a) {}",
        "  operator []=(a, b) {}",
        "}",
        "f(A a) {",
        "  a[0];",
        "  a[0] = 1;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedOperator_tilde() {
    Source source = addSource(EngineTestCase.createSource(["const A = 3;", "const B = ~((1 << A) - 1);"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedSetter_importWithPrefix() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "set y(int value) {}"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as x;", "main() {", "  x.y = 0;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedSuperMethod_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var m;",
        "}",
        "class B extends A {",
        "  f() {",
        "    super.m();",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_undefinedSuperMethod_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  f() {",
        "    super.m();",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unqualifiedReferenceToNonLocalStaticMember_fromComment_new() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "  A.named() {}",
        "}",
        "/// [new A] or [new A.named]",
        "main() {",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_wrongNumberOfParametersForOperator_index() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator []=(a, b) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_wrongNumberOfParametersForOperator_minus() {
    check_wrongNumberOfParametersForOperator("-", "");
    check_wrongNumberOfParametersForOperator("-", "a");
  }
  void test_wrongNumberOfParametersForOperator1() {
    check_wrongNumberOfParametersForOperator1("<");
    check_wrongNumberOfParametersForOperator1(">");
    check_wrongNumberOfParametersForOperator1("<=");
    check_wrongNumberOfParametersForOperator1(">=");
    check_wrongNumberOfParametersForOperator1("+");
    check_wrongNumberOfParametersForOperator1("/");
    check_wrongNumberOfParametersForOperator1("~/");
    check_wrongNumberOfParametersForOperator1("*");
    check_wrongNumberOfParametersForOperator1("%");
    check_wrongNumberOfParametersForOperator1("|");
    check_wrongNumberOfParametersForOperator1("^");
    check_wrongNumberOfParametersForOperator1("&");
    check_wrongNumberOfParametersForOperator1("<<");
    check_wrongNumberOfParametersForOperator1(">>");
    check_wrongNumberOfParametersForOperator1("[]");
  }
  void test_wrongNumberOfParametersForSetter() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  set x(a) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void check_wrongNumberOfParametersForOperator(String name, String parameters) {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator ${name}(${parameters}) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
    reset();
  }
  void check_wrongNumberOfParametersForOperator1(String name) {
    check_wrongNumberOfParametersForOperator(name, "a");
  }
  static dartSuite() {
    _ut.group('NonErrorResolverTest', () {
      _ut.test('test_ambiguousExport', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_ambiguousExport);
      });
      _ut.test('test_ambiguousExport_combinators_hide', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_ambiguousExport_combinators_hide);
      });
      _ut.test('test_ambiguousExport_combinators_show', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_ambiguousExport_combinators_show);
      });
      _ut.test('test_argumentDefinitionTestNonParameter_formalParameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentDefinitionTestNonParameter_formalParameter);
      });
      _ut.test('test_argumentDefinitionTestNonParameter_namedParameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentDefinitionTestNonParameter_namedParameter);
      });
      _ut.test('test_argumentDefinitionTestNonParameter_optionalParameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentDefinitionTestNonParameter_optionalParameter);
      });
      _ut.test('test_argumentTypeNotAssignable_Object_Function', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_Object_Function);
      });
      _ut.test('test_argumentTypeNotAssignable_classWithCall_Function', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_classWithCall_Function);
      });
      _ut.test('test_argumentTypeNotAssignable_fieldFormalParameterElement_member', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_fieldFormalParameterElement_member);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_functionParameter_generic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_functionParameter_generic);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_typedef_generic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_typedef_generic);
      });
      _ut.test('test_argumentTypeNotAssignable_typedef_local', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_typedef_local);
      });
      _ut.test('test_argumentTypeNotAssignable_typedef_parameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_typedef_parameter);
      });
      _ut.test('test_assignmentToFinal_prefixNegate', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_prefixNegate);
      });
      _ut.test('test_assignmentToFinals_importWithPrefix', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_assignmentToFinals_importWithPrefix);
      });
      _ut.test('test_breakWithoutLabelInSwitch', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_breakWithoutLabelInSwitch);
      });
      _ut.test('test_builtInIdentifierAsType_dynamic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsType_dynamic);
      });
      _ut.test('test_caseBlockNotTerminated', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_caseBlockNotTerminated);
      });
      _ut.test('test_caseBlockNotTerminated_lastCase', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_caseBlockNotTerminated_lastCase);
      });
      _ut.test('test_caseExpressionTypeImplementsEquals_Object', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_caseExpressionTypeImplementsEquals_Object);
      });
      _ut.test('test_caseExpressionTypeImplementsEquals_String', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_caseExpressionTypeImplementsEquals_String);
      });
      _ut.test('test_caseExpressionTypeImplementsEquals_int', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_caseExpressionTypeImplementsEquals_int);
      });
      _ut.test('test_concreteClassWithAbstractMember', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_concreteClassWithAbstractMember);
      });
      _ut.test('test_conflictingInstanceGetterAndSuperclassMember_instance', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_conflictingInstanceGetterAndSuperclassMember_instance);
      });
      _ut.test('test_conflictingStaticGetterAndInstanceSetter_thisClass', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_conflictingStaticGetterAndInstanceSetter_thisClass);
      });
      _ut.test('test_conflictingStaticSetterAndInstanceMember_thisClass_method', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_conflictingStaticSetterAndInstanceMember_thisClass_method);
      });
      _ut.test('test_constConstructorWithNonConstSuper_redirectingFactory', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonConstSuper_redirectingFactory);
      });
      _ut.test('test_constConstructorWithNonFinalField_finalInstanceVar', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonFinalField_finalInstanceVar);
      });
      _ut.test('test_constConstructorWithNonFinalField_mixin', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonFinalField_mixin);
      });
      _ut.test('test_constConstructorWithNonFinalField_static', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonFinalField_static);
      });
      _ut.test('test_constConstructorWithNonFinalField_syntheticField', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonFinalField_syntheticField);
      });
      _ut.test('test_constEvalTypeBoolNumString_equal', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constEvalTypeBoolNumString_equal);
      });
      _ut.test('test_constEvalTypeBoolNumString_notEqual', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constEvalTypeBoolNumString_notEqual);
      });
      _ut.test('test_constEval_propertyExtraction_fieldStatic_targetType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constEval_propertyExtraction_fieldStatic_targetType);
      });
      _ut.test('test_constEval_propertyExtraction_methodStatic_targetType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constEval_propertyExtraction_methodStatic_targetType);
      });
      _ut.test('test_constEval_symbol', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constEval_symbol);
      });
      _ut.test('test_constNotInitialized_field', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constNotInitialized_field);
      });
      _ut.test('test_constNotInitialized_local', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constNotInitialized_local);
      });
      _ut.test('test_constWithNonConstantArgument_literals', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constWithNonConstantArgument_literals);
      });
      _ut.test('test_constWithTypeParameters_direct', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constWithTypeParameters_direct);
      });
      _ut.test('test_constWithUndefinedConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constWithUndefinedConstructor);
      });
      _ut.test('test_constWithUndefinedConstructorDefault', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constWithUndefinedConstructorDefault);
      });
      _ut.test('test_constructorDeclaration_scope_signature', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_constructorDeclaration_scope_signature);
      });
      _ut.test('test_defaultValueInFunctionTypeAlias', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_defaultValueInFunctionTypeAlias);
      });
      _ut.test('test_defaultValueInFunctionTypedParameter_named', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_defaultValueInFunctionTypedParameter_named);
      });
      _ut.test('test_defaultValueInFunctionTypedParameter_optional', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_defaultValueInFunctionTypedParameter_optional);
      });
      _ut.test('test_duplicateDefinition_emptyName', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_emptyName);
      });
      _ut.test('test_duplicateDefinition_getter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_getter);
      });
      _ut.test('test_dynamicIdentifier', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_dynamicIdentifier);
      });
      _ut.test('test_expectedOneListTypeArgument', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_expectedOneListTypeArgument);
      });
      _ut.test('test_expectedTwoMapTypeArguments', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_expectedTwoMapTypeArguments);
      });
      _ut.test('test_exportOfNonLibrary_libraryDeclared', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_exportOfNonLibrary_libraryDeclared);
      });
      _ut.test('test_exportOfNonLibrary_libraryNotDeclared', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_exportOfNonLibrary_libraryNotDeclared);
      });
      _ut.test('test_extraPositionalArguments_Function', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments_Function);
      });
      _ut.test('test_extraPositionalArguments_function', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments_function);
      });
      _ut.test('test_extraPositionalArguments_typedef_local', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments_typedef_local);
      });
      _ut.test('test_extraPositionalArguments_typedef_parameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments_typedef_parameter);
      });
      _ut.test('test_fieldInitializedByMultipleInitializers', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_fieldInitializedByMultipleInitializers);
      });
      _ut.test('test_fieldInitializedInInitializerAndDeclaration_fieldNotFinal', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_fieldInitializedInInitializerAndDeclaration_fieldNotFinal);
      });
      _ut.test('test_fieldInitializedInInitializerAndDeclaration_finalFieldNotSet', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_fieldInitializedInInitializerAndDeclaration_finalFieldNotSet);
      });
      _ut.test('test_fieldInitializerOutsideConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_fieldInitializerOutsideConstructor);
      });
      _ut.test('test_fieldInitializerOutsideConstructor_defaultParameters', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_fieldInitializerOutsideConstructor_defaultParameters);
      });
      _ut.test('test_fieldInitializerRedirectingConstructor_super', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_fieldInitializerRedirectingConstructor_super);
      });
      _ut.test('test_finalInitializedInDeclarationAndConstructor_initializer', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalInitializedInDeclarationAndConstructor_initializer);
      });
      _ut.test('test_finalInitializedInDeclarationAndConstructor_initializingFormal', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalInitializedInDeclarationAndConstructor_initializingFormal);
      });
      _ut.test('test_finalNotInitialized_atDeclaration', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_atDeclaration);
      });
      _ut.test('test_finalNotInitialized_fieldFormal', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_fieldFormal);
      });
      _ut.test('test_finalNotInitialized_functionTypedFieldFormal', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_functionTypedFieldFormal);
      });
      _ut.test('test_finalNotInitialized_hasNativeClause_hasConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_hasNativeClause_hasConstructor);
      });
      _ut.test('test_finalNotInitialized_hasNativeClause_noConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_hasNativeClause_noConstructor);
      });
      _ut.test('test_finalNotInitialized_initializer', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_initializer);
      });
      _ut.test('test_finalNotInitialized_redirectingConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_redirectingConstructor);
      });
      _ut.test('test_functionDeclaration_scope_returnType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_functionDeclaration_scope_returnType);
      });
      _ut.test('test_functionDeclaration_scope_signature', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_functionDeclaration_scope_signature);
      });
      _ut.test('test_functionTypeAlias_scope_returnType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_functionTypeAlias_scope_returnType);
      });
      _ut.test('test_functionTypeAlias_scope_signature', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_functionTypeAlias_scope_signature);
      });
      _ut.test('test_functionWithoutCall', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_functionWithoutCall);
      });
      _ut.test('test_implicitThisReferenceInInitializer_constructorName', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_constructorName);
      });
      _ut.test('test_implicitThisReferenceInInitializer_prefixedIdentifier', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_prefixedIdentifier);
      });
      _ut.test('test_implicitThisReferenceInInitializer_qualifiedMethodInvocation', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_qualifiedMethodInvocation);
      });
      _ut.test('test_implicitThisReferenceInInitializer_qualifiedPropertyAccess', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_qualifiedPropertyAccess);
      });
      _ut.test('test_implicitThisReferenceInInitializer_staticField_thisClass', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_staticField_thisClass);
      });
      _ut.test('test_implicitThisReferenceInInitializer_staticGetter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_staticGetter);
      });
      _ut.test('test_implicitThisReferenceInInitializer_staticMethod', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_staticMethod);
      });
      _ut.test('test_implicitThisReferenceInInitializer_topLevelField', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_topLevelField);
      });
      _ut.test('test_implicitThisReferenceInInitializer_topLevelFunction', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_topLevelFunction);
      });
      _ut.test('test_implicitThisReferenceInInitializer_topLevelGetter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_topLevelGetter);
      });
      _ut.test('test_implicitThisReferenceInInitializer_typeParameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_typeParameter);
      });
      _ut.test('test_importDuplicatedLibraryName', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_importDuplicatedLibraryName);
      });
      _ut.test('test_importOfNonLibrary_libraryDeclared', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_importOfNonLibrary_libraryDeclared);
      });
      _ut.test('test_importOfNonLibrary_libraryNotDeclared', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_importOfNonLibrary_libraryNotDeclared);
      });
      _ut.test('test_importPrefixes_withFirstLetterDifference', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_importPrefixes_withFirstLetterDifference);
      });
      _ut.test('test_inconsistentCaseExpressionTypes', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_inconsistentCaseExpressionTypes);
      });
      _ut.test('test_inconsistentMethodInheritance_accessors_typeParameter2', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_accessors_typeParameter2);
      });
      _ut.test('test_inconsistentMethodInheritance_accessors_typeParameters1', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_accessors_typeParameters1);
      });
      _ut.test('test_inconsistentMethodInheritance_accessors_typeParameters_diamond', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_accessors_typeParameters_diamond);
      });
      _ut.test('test_inconsistentMethodInheritance_methods_typeParameter2', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_methods_typeParameter2);
      });
      _ut.test('test_inconsistentMethodInheritance_methods_typeParameters1', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_methods_typeParameters1);
      });
      _ut.test('test_inconsistentMethodInheritance_simple', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_simple);
      });
      _ut.test('test_initializingFormalForNonExistantField', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_initializingFormalForNonExistantField);
      });
      _ut.test('test_instanceAccessToStaticMember_fromComment', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_instanceAccessToStaticMember_fromComment);
      });
      _ut.test('test_instanceAccessToStaticMember_topLevel', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_instanceAccessToStaticMember_topLevel);
      });
      _ut.test('test_instanceMemberAccessFromStatic_fromComment', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_instanceMemberAccessFromStatic_fromComment);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_field', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_field);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_method', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_method);
      });
      _ut.test('test_invalidAnnotation_constantVariable', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_constantVariable);
      });
      _ut.test('test_invalidAnnotation_importWithPrefix_constConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_importWithPrefix_constConstructor);
      });
      _ut.test('test_invalidAnnotation_importWithPrefix_constantVariable', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_importWithPrefix_constantVariable);
      });
      _ut.test('test_invalidAssignment', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAssignment);
      });
      _ut.test('test_invalidAssignment_compoundAssignment', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAssignment_compoundAssignment);
      });
      _ut.test('test_invalidAssignment_defaultValue_named', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAssignment_defaultValue_named);
      });
      _ut.test('test_invalidAssignment_defaultValue_optional', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAssignment_defaultValue_optional);
      });
      _ut.test('test_invalidAssignment_toDynamic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidAssignment_toDynamic);
      });
      _ut.test('test_invalidFactoryNameNotAClass', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidFactoryNameNotAClass);
      });
      _ut.test('test_invalidMethodOverrideNamedParamType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideNamedParamType);
      });
      _ut.test('test_invalidOverrideDifferentDefaultValues_named', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideDifferentDefaultValues_named);
      });
      _ut.test('test_invalidOverrideDifferentDefaultValues_positional', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideDifferentDefaultValues_positional);
      });
      _ut.test('test_invalidOverrideDifferentDefaultValues_positional_changedOrder', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideDifferentDefaultValues_positional_changedOrder);
      });
      _ut.test('test_invalidOverrideNamed_unorderedNamedParameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideNamed_unorderedNamedParameter);
      });
      _ut.test('test_invalidOverrideRequired_less', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideRequired_less);
      });
      _ut.test('test_invalidOverrideRequired_same', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideRequired_same);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_interface', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_interface);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_interface2', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_interface2);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_mixin', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_mixin);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_parameterizedTypes', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_parameterizedTypes);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_sameType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_sameType);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_superclass', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_superclass);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_superclass2', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_superclass2);
      });
      _ut.test('test_invalidOverrideReturnType_returnType_void', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidOverrideReturnType_returnType_void);
      });
      _ut.test('test_invalidReferenceToThis_constructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_constructor);
      });
      _ut.test('test_invalidReferenceToThis_instanceMethod', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_instanceMethod);
      });
      _ut.test('test_invalidTypeArgumentForKey', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidTypeArgumentForKey);
      });
      _ut.test('test_invalidTypeArgumentInConstList', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidTypeArgumentInConstList);
      });
      _ut.test('test_invalidTypeArgumentInConstMap', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invalidTypeArgumentInConstMap);
      });
      _ut.test('test_invocationOfNonFunction_Object', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_Object);
      });
      _ut.test('test_invocationOfNonFunction_dynamic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_dynamic);
      });
      _ut.test('test_invocationOfNonFunction_getter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_getter);
      });
      _ut.test('test_invocationOfNonFunction_localVariable', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_localVariable);
      });
      _ut.test('test_invocationOfNonFunction_localVariable_dynamic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_localVariable_dynamic);
      });
      _ut.test('test_invocationOfNonFunction_localVariable_dynamic2', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_localVariable_dynamic2);
      });
      _ut.test('test_listElementTypeNotAssignable', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_listElementTypeNotAssignable);
      });
      _ut.test('test_mapKeyTypeNotAssignable', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_mapKeyTypeNotAssignable);
      });
      _ut.test('test_memberWithClassName_setter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_memberWithClassName_setter);
      });
      _ut.test('test_methodDeclaration_scope_signature', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_methodDeclaration_scope_signature);
      });
      _ut.test('test_misMatchedGetterAndSetterTypes_instance_sameTypes', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_misMatchedGetterAndSetterTypes_instance_sameTypes);
      });
      _ut.test('test_misMatchedGetterAndSetterTypes_instance_unspecifiedGetter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_misMatchedGetterAndSetterTypes_instance_unspecifiedGetter);
      });
      _ut.test('test_misMatchedGetterAndSetterTypes_instance_unspecifiedSetter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_misMatchedGetterAndSetterTypes_instance_unspecifiedSetter);
      });
      _ut.test('test_misMatchedGetterAndSetterTypes_topLevel_sameTypes', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_misMatchedGetterAndSetterTypes_topLevel_sameTypes);
      });
      _ut.test('test_misMatchedGetterAndSetterTypes_topLevel_unspecifiedGetter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_misMatchedGetterAndSetterTypes_topLevel_unspecifiedGetter);
      });
      _ut.test('test_misMatchedGetterAndSetterTypes_topLevel_unspecifiedSetter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_misMatchedGetterAndSetterTypes_topLevel_unspecifiedSetter);
      });
      _ut.test('test_mixedReturnTypes_differentScopes', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_mixedReturnTypes_differentScopes);
      });
      _ut.test('test_mixedReturnTypes_sameKind', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_mixedReturnTypes_sameKind);
      });
      _ut.test('test_mixinDeclaresConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_mixinDeclaresConstructor);
      });
      _ut.test('test_mixinDeclaresConstructor_factory', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_mixinDeclaresConstructor_factory);
      });
      _ut.test('test_mixinInheritsFromNotObject_classDeclaration_mixTypeAlias', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_mixinInheritsFromNotObject_classDeclaration_mixTypeAlias);
      });
      _ut.test('test_mixinInheritsFromNotObject_typedef_mixTypeAlias', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_mixinInheritsFromNotObject_typedef_mixTypeAlias);
      });
      _ut.test('test_multipleSuperInitializers_no', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_multipleSuperInitializers_no);
      });
      _ut.test('test_multipleSuperInitializers_single', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_multipleSuperInitializers_single);
      });
      _ut.test('test_newWithAbstractClass_factory', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_newWithAbstractClass_factory);
      });
      _ut.test('test_newWithUndefinedConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_newWithUndefinedConstructor);
      });
      _ut.test('test_newWithUndefinedConstructorDefault', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_newWithUndefinedConstructorDefault);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_abstractOverridesConcrete_accessor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_abstractOverridesConcrete_accessor);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_abstractOverridesConcrete_method', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_abstractOverridesConcrete_method);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_noSuchMethod_accessor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_noSuchMethod_accessor);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_noSuchMethod_method', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_noSuchMethod_method);
      });
      _ut.test('test_nonBoolExpression_functionType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonBoolExpression_functionType);
      });
      _ut.test('test_nonBoolExpression_interfaceType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonBoolExpression_interfaceType);
      });
      _ut.test('test_nonBoolNegationExpression', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonBoolNegationExpression);
      });
      _ut.test('test_nonConstCaseExpression', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstCaseExpression);
      });
      _ut.test('test_nonConstMapAsExpressionStatement_const', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstMapAsExpressionStatement_const);
      });
      _ut.test('test_nonConstMapAsExpressionStatement_notExpressionStatement', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstMapAsExpressionStatement_notExpressionStatement);
      });
      _ut.test('test_nonConstMapAsExpressionStatement_typeArguments', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstMapAsExpressionStatement_typeArguments);
      });
      _ut.test('test_nonConstValueInInitializer_binary_bool', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_bool);
      });
      _ut.test('test_nonConstValueInInitializer_binary_dynamic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_dynamic);
      });
      _ut.test('test_nonConstValueInInitializer_binary_int', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_int);
      });
      _ut.test('test_nonConstValueInInitializer_binary_num', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_num);
      });
      _ut.test('test_nonConstValueInInitializer_field', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_field);
      });
      _ut.test('test_nonConstValueInInitializer_redirecting', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_redirecting);
      });
      _ut.test('test_nonConstValueInInitializer_super', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_super);
      });
      _ut.test('test_nonConstValueInInitializer_unary', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_unary);
      });
      _ut.test('test_nonConstantDefaultValue_function_named', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_function_named);
      });
      _ut.test('test_nonConstantDefaultValue_function_positional', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_function_positional);
      });
      _ut.test('test_nonConstantDefaultValue_inConstructor_named', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_inConstructor_named);
      });
      _ut.test('test_nonConstantDefaultValue_inConstructor_positional', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_inConstructor_positional);
      });
      _ut.test('test_nonConstantDefaultValue_method_named', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_method_named);
      });
      _ut.test('test_nonConstantDefaultValue_method_positional', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_method_positional);
      });
      _ut.test('test_nonConstantValueInInitializer_namedArgument', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonConstantValueInInitializer_namedArgument);
      });
      _ut.test('test_nonGenerativeConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonGenerativeConstructor);
      });
      _ut.test('test_nonTypeInCatchClause_isClass', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonTypeInCatchClause_isClass);
      });
      _ut.test('test_nonTypeInCatchClause_isFunctionTypeAlias', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonTypeInCatchClause_isFunctionTypeAlias);
      });
      _ut.test('test_nonTypeInCatchClause_isTypeParameter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonTypeInCatchClause_isTypeParameter);
      });
      _ut.test('test_nonTypeInCatchClause_noType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonTypeInCatchClause_noType);
      });
      _ut.test('test_nonVoidReturnForOperator_no', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForOperator_no);
      });
      _ut.test('test_nonVoidReturnForOperator_void', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForOperator_void);
      });
      _ut.test('test_nonVoidReturnForSetter_function_no', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForSetter_function_no);
      });
      _ut.test('test_nonVoidReturnForSetter_function_void', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForSetter_function_void);
      });
      _ut.test('test_nonVoidReturnForSetter_method_no', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForSetter_method_no);
      });
      _ut.test('test_nonVoidReturnForSetter_method_void', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForSetter_method_void);
      });
      _ut.test('test_null_callMethod', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_null_callMethod);
      });
      _ut.test('test_null_callOperator', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_null_callOperator);
      });
      _ut.test('test_optionalParameterInOperator_required', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_optionalParameterInOperator_required);
      });
      _ut.test('test_prefixCollidesWithTopLevelMembers', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_prefixCollidesWithTopLevelMembers);
      });
      _ut.test('test_propagateTypeArgs_intoBounds', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_propagateTypeArgs_intoBounds);
      });
      _ut.test('test_propagateTypeArgs_intoSupertype', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_propagateTypeArgs_intoSupertype);
      });
      _ut.test('test_proxy_annotation_prefixed', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_proxy_annotation_prefixed);
      });
      _ut.test('test_proxy_annotation_prefixed2', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_proxy_annotation_prefixed2);
      });
      _ut.test('test_proxy_annotation_prefixed3', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_proxy_annotation_prefixed3);
      });
      _ut.test('test_proxy_annotation_simple', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_proxy_annotation_simple);
      });
      _ut.test('test_recursiveConstructorRedirect', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_recursiveConstructorRedirect);
      });
      _ut.test('test_recursiveFactoryRedirect', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_recursiveFactoryRedirect);
      });
      _ut.test('test_redirectToInvalidFunctionType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_redirectToInvalidFunctionType);
      });
      _ut.test('test_redirectToInvalidReturnType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_redirectToInvalidReturnType);
      });
      _ut.test('test_redirectToNonConstConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_redirectToNonConstConstructor);
      });
      _ut.test('test_referenceToDeclaredVariableInInitializer_constructorName', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_referenceToDeclaredVariableInInitializer_constructorName);
      });
      _ut.test('test_referenceToDeclaredVariableInInitializer_methodName', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_referenceToDeclaredVariableInInitializer_methodName);
      });
      _ut.test('test_referenceToDeclaredVariableInInitializer_propertyName', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_referenceToDeclaredVariableInInitializer_propertyName);
      });
      _ut.test('test_rethrowOutsideCatch', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_rethrowOutsideCatch);
      });
      _ut.test('test_returnInGenerativeConstructor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnInGenerativeConstructor);
      });
      _ut.test('test_returnOfInvalidType_dynamic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_dynamic);
      });
      _ut.test('test_returnOfInvalidType_dynamicAsTypeArgument', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_dynamicAsTypeArgument);
      });
      _ut.test('test_returnOfInvalidType_subtype', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_subtype);
      });
      _ut.test('test_returnOfInvalidType_supertype', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_supertype);
      });
      _ut.test('test_returnOfInvalidType_void', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_void);
      });
      _ut.test('test_returnWithoutValue_noReturnType', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnWithoutValue_noReturnType);
      });
      _ut.test('test_returnWithoutValue_void', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_returnWithoutValue_void);
      });
      _ut.test('test_reversedTypeArguments', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_reversedTypeArguments);
      });
      _ut.test('test_staticAccessToInstanceMember_annotation', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_annotation);
      });
      _ut.test('test_staticAccessToInstanceMember_method', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_method);
      });
      _ut.test('test_staticAccessToInstanceMember_propertyAccess_field', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_propertyAccess_field);
      });
      _ut.test('test_staticAccessToInstanceMember_propertyAccess_propertyAccessor', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_propertyAccess_propertyAccessor);
      });
      _ut.test('test_superInInvalidContext', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_superInInvalidContext);
      });
      _ut.test('test_typeAliasCannotReferenceItself_returnClass_withTypeAlias', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_returnClass_withTypeAlias);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_const', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_const);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_new', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_new);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_typeArgumentList_0', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_typeArgumentList_0);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_typeArgumentList_1', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_typeArgumentList_1);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_typeArgumentList_20', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_typeArgumentList_20);
      });
      _ut.test('test_typePromotion_booleanAnd_useInRight', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_booleanAnd_useInRight);
      });
      _ut.test('test_typePromotion_booleanAnd_useInRight_accessedInClosureRight_noAssignment', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_booleanAnd_useInRight_accessedInClosureRight_noAssignment);
      });
      _ut.test('test_typePromotion_conditional_useInThen', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_conditional_useInThen);
      });
      _ut.test('test_typePromotion_conditional_useInThen_accessedInClosure_noAssignment', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_conditional_useInThen_accessedInClosure_noAssignment);
      });
      _ut.test('test_typePromotion_functionType_arg_ignoreIfNotMoreSpecific', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_functionType_arg_ignoreIfNotMoreSpecific);
      });
      _ut.test('test_typePromotion_functionType_return_ignoreIfNotMoreSpecific', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_functionType_return_ignoreIfNotMoreSpecific);
      });
      _ut.test('test_typePromotion_functionType_return_voidToDynamic', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_functionType_return_voidToDynamic);
      });
      _ut.test('test_typePromotion_if_accessedInClosure_noAssignment', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_accessedInClosure_noAssignment);
      });
      _ut.test('test_typePromotion_if_extends_moreSpecific', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_extends_moreSpecific);
      });
      _ut.test('test_typePromotion_if_hasAssignment_outsideAfter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_outsideAfter);
      });
      _ut.test('test_typePromotion_if_hasAssignment_outsideBefore', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_outsideBefore);
      });
      _ut.test('test_typePromotion_if_implements_moreSpecific', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_implements_moreSpecific);
      });
      _ut.test('test_typePromotion_if_inClosure_assignedAfter_inSameFunction', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_inClosure_assignedAfter_inSameFunction);
      });
      _ut.test('test_typePromotion_if_is_and_left', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_is_and_left);
      });
      _ut.test('test_typePromotion_if_is_and_right', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_is_and_right);
      });
      _ut.test('test_typePromotion_if_is_and_subThenSuper', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_is_and_subThenSuper);
      });
      _ut.test('test_typePromotion_if_is_parenthesized', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_is_parenthesized);
      });
      _ut.test('test_typePromotion_if_is_single', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typePromotion_if_is_single);
      });
      _ut.test('test_typeType_class', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeType_class);
      });
      _ut.test('test_typeType_class_prefixed', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeType_class_prefixed);
      });
      _ut.test('test_typeType_functionTypeAlias', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeType_functionTypeAlias);
      });
      _ut.test('test_typeType_functionTypeAlias_prefixed', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_typeType_functionTypeAlias_prefixed);
      });
      _ut.test('test_undefinedConstructorInInitializer_explicit_named', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_explicit_named);
      });
      _ut.test('test_undefinedConstructorInInitializer_explicit_unnamed', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_explicit_unnamed);
      });
      _ut.test('test_undefinedConstructorInInitializer_hasOptionalParameters', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_hasOptionalParameters);
      });
      _ut.test('test_undefinedConstructorInInitializer_implicit', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_implicit);
      });
      _ut.test('test_undefinedConstructorInInitializer_implicit_typeAlias', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_implicit_typeAlias);
      });
      _ut.test('test_undefinedConstructorInInitializer_redirecting', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_redirecting);
      });
      _ut.test('test_undefinedGetter_typeSubstitution', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedGetter_typeSubstitution);
      });
      _ut.test('test_undefinedIdentifier_hide', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_hide);
      });
      _ut.test('test_undefinedIdentifier_show', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_show);
      });
      _ut.test('test_undefinedMethod_functionExpression_callMethod', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedMethod_functionExpression_callMethod);
      });
      _ut.test('test_undefinedMethod_functionExpression_directCall', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedMethod_functionExpression_directCall);
      });
      _ut.test('test_undefinedOperator_index', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedOperator_index);
      });
      _ut.test('test_undefinedOperator_tilde', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedOperator_tilde);
      });
      _ut.test('test_undefinedSetter_importWithPrefix', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedSetter_importWithPrefix);
      });
      _ut.test('test_undefinedSuperMethod_field', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedSuperMethod_field);
      });
      _ut.test('test_undefinedSuperMethod_method', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_undefinedSuperMethod_method);
      });
      _ut.test('test_unqualifiedReferenceToNonLocalStaticMember_fromComment_new', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_unqualifiedReferenceToNonLocalStaticMember_fromComment_new);
      });
      _ut.test('test_wrongNumberOfParametersForOperator1', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForOperator1);
      });
      _ut.test('test_wrongNumberOfParametersForOperator_index', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForOperator_index);
      });
      _ut.test('test_wrongNumberOfParametersForOperator_minus', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForOperator_minus);
      });
      _ut.test('test_wrongNumberOfParametersForSetter', () {
        final __test = new NonErrorResolverTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter);
      });
    });
  }
}
class LibraryTest extends EngineTestCase {

  /**
   * The error listener to which all errors will be reported.
   */
  GatheringErrorListener _errorListener;

  /**
   * The source factory used to create libraries.
   */
  SourceFactory _sourceFactory;

  /**
   * The analysis context to pass in to all libraries created by the tests.
   */
  AnalysisContextImpl _analysisContext;

  /**
   * The library used by the tests.
   */
  Library _library5;
  void setUp() {
    _sourceFactory = new SourceFactory.con2([new FileUriResolver()]);
    _analysisContext = new AnalysisContextImpl();
    _analysisContext.sourceFactory = _sourceFactory;
    _errorListener = new GatheringErrorListener();
    _library5 = library("/lib.dart");
  }
  void test_getExplicitlyImportsCore() {
    JUnitTestCase.assertFalse(_library5.explicitlyImportsCore);
    _errorListener.assertNoErrors();
  }
  void test_getExports() {
    EngineTestCase.assertLength(0, _library5.exports);
    _errorListener.assertNoErrors();
  }
  void test_getImports() {
    EngineTestCase.assertLength(0, _library5.imports);
    _errorListener.assertNoErrors();
  }
  void test_getImportsAndExports() {
    _library5.importedLibraries = <Library> [library("/imported.dart")];
    _library5.exportedLibraries = <Library> [library("/exported.dart")];
    EngineTestCase.assertLength(2, _library5.importsAndExports);
    _errorListener.assertNoErrors();
  }
  void test_getLibraryScope() {
    LibraryElementImpl element = new LibraryElementImpl(_analysisContext, ASTFactory.libraryIdentifier2(["lib"]));
    element.definingCompilationUnit = new CompilationUnitElementImpl("lib.dart");
    _library5.libraryElement = element;
    JUnitTestCase.assertNotNull(_library5.libraryScope);
    _errorListener.assertNoErrors();
  }
  void test_getLibrarySource() {
    JUnitTestCase.assertNotNull(_library5.librarySource);
  }
  void test_setExplicitlyImportsCore() {
    _library5.explicitlyImportsCore = true;
    JUnitTestCase.assertTrue(_library5.explicitlyImportsCore);
    _errorListener.assertNoErrors();
  }
  void test_setExportedLibraries() {
    Library exportLibrary = library("/exported.dart");
    _library5.exportedLibraries = <Library> [exportLibrary];
    List<Library> exports = _library5.exports;
    EngineTestCase.assertLength(1, exports);
    JUnitTestCase.assertSame(exportLibrary, exports[0]);
    _errorListener.assertNoErrors();
  }
  void test_setImportedLibraries() {
    Library importLibrary = library("/imported.dart");
    _library5.importedLibraries = <Library> [importLibrary];
    List<Library> imports = _library5.imports;
    EngineTestCase.assertLength(1, imports);
    JUnitTestCase.assertSame(importLibrary, imports[0]);
    _errorListener.assertNoErrors();
  }
  void test_setLibraryElement() {
    LibraryElementImpl element = new LibraryElementImpl(_analysisContext, ASTFactory.libraryIdentifier2(["lib"]));
    _library5.libraryElement = element;
    JUnitTestCase.assertSame(element, _library5.libraryElement);
  }
  Library library(String definingCompilationUnitPath) => new Library(_analysisContext, _errorListener, new FileBasedSource.con1(_sourceFactory.contentCache, FileUtilities2.createFile(definingCompilationUnitPath)));
  static dartSuite() {
    _ut.group('LibraryTest', () {
      _ut.test('test_getExplicitlyImportsCore', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_getExplicitlyImportsCore);
      });
      _ut.test('test_getExports', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_getExports);
      });
      _ut.test('test_getImports', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_getImports);
      });
      _ut.test('test_getImportsAndExports', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_getImportsAndExports);
      });
      _ut.test('test_getLibraryScope', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_getLibraryScope);
      });
      _ut.test('test_getLibrarySource', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_getLibrarySource);
      });
      _ut.test('test_setExplicitlyImportsCore', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_setExplicitlyImportsCore);
      });
      _ut.test('test_setExportedLibraries', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_setExportedLibraries);
      });
      _ut.test('test_setImportedLibraries', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_setImportedLibraries);
      });
      _ut.test('test_setLibraryElement', () {
        final __test = new LibraryTest();
        runJUnitTest(__test, __test.test_setLibraryElement);
      });
    });
  }
}
class StaticTypeWarningCodeTest extends ResolverTestCase {
  void fail_inaccessibleSetter() {
    Source source = addSource(EngineTestCase.createSource([]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INACCESSIBLE_SETTER]);
    verify([source]);
  }
  void test_expectedOneListTypeArgument() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  <int, int> [];", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.EXPECTED_ONE_LIST_TYPE_ARGUMENTS]);
    verify([source]);
  }
  void test_expectedTwoMapTypeArguments_one() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  <int> {};", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.EXPECTED_TWO_MAP_TYPE_ARGUMENTS]);
    verify([source]);
  }
  void test_expectedTwoMapTypeArguments_three() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  <int, int, int> {};", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.EXPECTED_TWO_MAP_TYPE_ARGUMENTS]);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_paramCount() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int x();",
        "}",
        "abstract class B {",
        "  int x(int y);",
        "}",
        "class C implements A, B {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE]);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_paramType() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  x(int i);",
        "}",
        "abstract class B {",
        "  x(String s);",
        "}",
        "abstract class C implements A, B {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE]);
    verify([source]);
  }
  void test_inconsistentMethodInheritance_returnType() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int x();",
        "}",
        "abstract class B {",
        "  String x();",
        "}",
        "abstract class C implements A, B {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE]);
    verify([source]);
  }
  void test_instanceAccessToStaticMember_method_invocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static m() {}",
        "}",
        "main(A a) {",
        "  a.m();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER]);
    verify([source]);
  }
  void test_instanceAccessToStaticMember_method_reference() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static m() {}",
        "}",
        "main(A a) {",
        "  a.m;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER]);
    verify([source]);
  }
  void test_instanceAccessToStaticMember_propertyAccess_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static var f;",
        "}",
        "main(A a) {",
        "  a.f;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER]);
    verify([source]);
  }
  void test_instanceAccessToStaticMember_propertyAccess_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static get f => 42;",
        "}",
        "main(A a) {",
        "  a.f;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER]);
    verify([source]);
  }
  void test_instanceAccessToStaticMember_propertyAccess_setter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static set f(x) {}",
        "}",
        "main(A a) {",
        "  a.f = 42;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER]);
    verify([source]);
  }
  void test_invalidAssignment_compoundAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "class byte {",
        "  int _value;",
        "  byte(this._value);",
        "  int operator +(int val) {}",
        "}",
        "",
        "void main() {",
        "  byte b = new byte(52);",
        "  b += 3;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invalidAssignment_defaultValue_named() {
    Source source = addSource(EngineTestCase.createSource(["f({String x: 0}) {", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invalidAssignment_defaultValue_optional() {
    Source source = addSource(EngineTestCase.createSource(["f([String x = 0]) {", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invalidAssignment_instanceVariable() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int x;",
        "}",
        "f() {",
        "  A a;",
        "  a.x = '0';",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invalidAssignment_localVariable() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  int x;", "  x = '0';", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invalidAssignment_staticVariable() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int x;",
        "}",
        "f() {",
        "  A.x = '0';",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invalidAssignment_topLevelVariableDeclaration() {
    Source source = addSource(EngineTestCase.createSource(["int x = 'string';"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invalidAssignment_variableDeclaration() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x = 'string';", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_invocationOfNonFunction_class() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  void m() {", "    A();", "  }", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION]);
  }
  void test_invocationOfNonFunction_localVariable() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  int x;", "  return x();", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION]);
    verify([source]);
  }
  void test_invocationOfNonFunction_ordinaryInvocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int x;",
        "}",
        "class B {",
        "  m() {",
        "    A.x();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION]);
  }
  void test_invocationOfNonFunction_staticInvocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int get g => 0;",
        "  f() {",
        "    A.g();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION]);
  }
  void test_invocationOfNonFunction_superExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get g => 0;",
        "}",
        "class B extends A {",
        "  m() {",
        "    var v = super.g();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION]);
    verify([source]);
  }
  void test_invocationOfNonFunctionExpression_literal() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  3(5);", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION_EXPRESSION]);
    verify([source]);
  }
  void test_nonBoolCondition_conditional() {
    Source source = addSource(EngineTestCase.createSource(["f() { return 3 ? 2 : 1; }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_BOOL_CONDITION]);
    verify([source]);
  }
  void test_nonBoolCondition_do() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  do {} while (3);", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_BOOL_CONDITION]);
    verify([source]);
  }
  void test_nonBoolCondition_if() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  if (3) return 2; else return 1;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_BOOL_CONDITION]);
    verify([source]);
  }
  void test_nonBoolCondition_while() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  while (3) {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_BOOL_CONDITION]);
    verify([source]);
  }
  void test_nonBoolExpression_functionType() {
    Source source = addSource(EngineTestCase.createSource([
        "int makeAssertion() => 1;",
        "f() {",
        "  assert(makeAssertion);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_BOOL_EXPRESSION]);
    verify([source]);
  }
  void test_nonBoolExpression_interfaceType() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  assert(0);", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_BOOL_EXPRESSION]);
    verify([source]);
  }
  void test_nonBoolNegationExpression() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  !42;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_BOOL_NEGATION_EXPRESSION]);
    verify([source]);
  }
  void test_nonTypeAsTypeArgument_notAType() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "class B<E> {}", "f(B<A> b) {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT]);
    verify([source]);
  }
  void test_nonTypeAsTypeArgument_undefinedIdentifier() {
    Source source = addSource(EngineTestCase.createSource(["class B<E> {}", "f(B<A> b) {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT]);
    verify([source]);
  }
  void test_returnOfInvalidType_expressionFunctionBody_function() {
    Source source = addSource(EngineTestCase.createSource(["int f() => '0';"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_expressionFunctionBody_getter() {
    Source source = addSource(EngineTestCase.createSource(["int get g => '0';"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_expressionFunctionBody_localFunction() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  String m() {",
        "    int f() => '0';",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_expressionFunctionBody_method() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int f() => '0';", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_expressionFunctionBody_void() {
    Source source = addSource(EngineTestCase.createSource(["void f() => 42;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_function() {
    Source source = addSource(EngineTestCase.createSource(["int f() { return '0'; }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_getter() {
    Source source = addSource(EngineTestCase.createSource(["int get g { return '0'; }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_localFunction() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  String m() {",
        "    int f() { return '0'; }",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_method() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int f() { return '0'; }", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_returnOfInvalidType_void() {
    Source source = addSource(EngineTestCase.createSource(["void f() { return 42; }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.RETURN_OF_INVALID_TYPE]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_classTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class C {}",
        "class G<E extends A> {}",
        "class D = G<B> with C;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_extends() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C extends G<B>{}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_fieldFormalParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C {",
        "  var f;",
        "  C(G<B> this.f) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_functionReturnType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "G<B> f() {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_functionTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "typedef G<B> f();"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_functionTypedFormalParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f(G<B> h()) {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_implements() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C implements G<B>{}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_is() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "var b = 1 is G<B>;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_methodReturnType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C {",
        "  G<B> m() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_new() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_new_superTypeOfUpperBound() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "class C extends B {}",
        "class G<E extends B> {}",
        "f() { return new G<A>(); }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_parameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f(G<B> g) {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_redirectingConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class X<T extends A> {",
        "  X(int x, int y) {}",
        "  factory X.name(int x, int y) = X<B>;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_typeArgumentList() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class C<E> {}",
        "class D<E extends A> {}",
        "C<D<B>> Var;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_typeParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class C {}",
        "class G<E extends A> {}",
        "class D<F extends G<B>> {}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_variableDeclaration() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "G<B> g;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_with() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C extends Object with G<B>{}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_typeParameterSupertypeOfItsBound() {
    Source source = addSource(EngineTestCase.createSource(["class A<T extends T> {", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.TYPE_PARAMETER_SUPERTYPE_OF_ITS_BOUND]);
    verify([source]);
  }
  void test_typePromotion_booleanAnd_useInRight_accessedInClosureRight_mutated() {
    Source source = addSource(EngineTestCase.createSource([
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  (p is String) && callMe(() { p.length; });",
        "  p = 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_booleanAnd_useInRight_mutatedInLeft() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  ((p is String) && ((p = 42) == 42)) && p.length != 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_booleanAnd_useInRight_mutatedInRight() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  (p is String) && (((p = 42) == 42) && p.length != 0);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_after() {
    Source source = addSource(EngineTestCase.createSource([
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  p is String ? callMe(() { p.length; }) : 0;",
        "  p = 42;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_before() {
    Source source = addSource(EngineTestCase.createSource([
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  p = 42;",
        "  p is String ? callMe(() { p.length; }) : 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_conditional_useInThen_hasAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  p is String ? (p.length + (p = 42)) : 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_accessedInClosure_hasAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  if (p is String) {",
        "    callMe(() {",
        "      p.length;",
        "    });",
        "  }",
        "  p = 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_and_right_hasAssignment() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if (p is String && (p = null) == null) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_extends_notMoreSpecific_dynamic() {
    Source source = addSource(EngineTestCase.createSource([
        "class V {}",
        "class A<T> {}",
        "class B<S> extends A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B) {",
        "    p.b;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_extends_notMoreSpecific_notMoreSpecificTypeArg() {
    Source source = addSource(EngineTestCase.createSource([
        "class V {}",
        "class A<T> {}",
        "class B<S> extends A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B<int>) {",
        "    p.b;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_hasAssignment_after() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "    p = 0;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_hasAssignment_before() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if (p is String) {",
        "    p = 0;",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_hasAssignment_inClosure_anonymous_after() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "  () {p = 0;};",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_hasAssignment_inClosure_anonymous_before() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  () {p = 0;};",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_hasAssignment_inClosure_function_after() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "  f() {p = 0;};",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_hasAssignment_inClosure_function_before() {
    Source source = addSource(EngineTestCase.createSource([
        "main(Object p) {",
        "  f() {p = 0;};",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_implements_notMoreSpecific_dynamic() {
    Source source = addSource(EngineTestCase.createSource([
        "class V {}",
        "class A<T> {}",
        "class B<S> implements A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B) {",
        "    p.b;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_typePromotion_if_with_notMoreSpecific_dynamic() {
    Source source = addSource(EngineTestCase.createSource([
        "class V {}",
        "class A<T> {}",
        "class B<S> extends Object with A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B) {",
        "    p.b;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_undefinedGetter() {
    Source source = addSource(EngineTestCase.createSource(["class T {}", "f(T e) { return e.m; }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_undefinedGetter_static() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "var a = A.B;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_GETTER]);
  }
  void test_undefinedMethod() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  void m() {", "    n();", "  }", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_METHOD]);
  }
  void test_undefinedMethod_assignmentExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {",
        "  f(A a) {",
        "    A a2 = new A();",
        "    a += a2;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_METHOD]);
  }
  void test_undefinedMethod_ignoreTypePropagation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  m() {}",
        "}",
        "class C {",
        "  f() {",
        "    A a = new B();",
        "    a.m();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_METHOD]);
  }
  void test_undefinedMethod_private() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A {", "  _foo() {}", "}"]));
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart';",
        "class B extends A {",
        "  test() {",
        "    _foo();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_METHOD]);
  }
  void test_undefinedOperator_indexBoth() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f(A a) {", "  a[0]++;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_indexGetter() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f(A a) {", "  a[0];", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_indexSetter() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f(A a) {", "  a[0] = 1;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_plus() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f(A a) {", "  a + 1;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_postfixExpression() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f(A a) {", "  a++;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_prefixExpression() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f(A a) {", "  ++a;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedSetter() {
    Source source = addSource(EngineTestCase.createSource(["class T {}", "f(T e1) { e1.m = 0; }"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_SETTER]);
  }
  void test_undefinedSetter_static() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f() { A.B = 0;}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_SETTER]);
  }
  void test_undefinedSuperMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  m() { return super.m(); }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_SUPER_METHOD]);
  }
  void test_unqualifiedReferenceToNonLocalStaticMember_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int get a => 0;",
        "}",
        "class B extends A {",
        "  int b() {",
        "    return a;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER]);
    verify([source]);
  }
  void test_unqualifiedReferenceToNonLocalStaticMember_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static void a() {}",
        "}",
        "class B extends A {",
        "  void b() {",
        "    a();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER]);
    verify([source]);
  }
  void test_unqualifiedReferenceToNonLocalStaticMember_setter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static set a(x) {}",
        "}",
        "class B extends A {",
        "  b(y) {",
        "    a = y;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER]);
    verify([source]);
  }
  void test_wrongNumberOfTypeArguments_tooFew() {
    Source source = addSource(EngineTestCase.createSource(["class A<E, F> {}", "A<A> a = null;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS]);
    verify([source]);
  }
  void test_wrongNumberOfTypeArguments_tooMany() {
    Source source = addSource(EngineTestCase.createSource(["class A<E> {}", "A<A, A> a = null;"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS]);
    verify([source]);
  }
  void test_wrongNumberOfTypeArguments_typeTest_tooFew() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class C<K, V> {}",
        "f(p) {",
        "  return p is C<A>;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS]);
    verify([source]);
  }
  void test_wrongNumberOfTypeArguments_typeTest_tooMany() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class C<E> {}",
        "f(p) {",
        "  return p is C<A, A>;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS]);
    verify([source]);
  }
  static dartSuite() {
    _ut.group('StaticTypeWarningCodeTest', () {
      _ut.test('test_expectedOneListTypeArgument', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_expectedOneListTypeArgument);
      });
      _ut.test('test_expectedTwoMapTypeArguments_one', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_expectedTwoMapTypeArguments_one);
      });
      _ut.test('test_expectedTwoMapTypeArguments_three', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_expectedTwoMapTypeArguments_three);
      });
      _ut.test('test_inconsistentMethodInheritance_paramCount', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_paramCount);
      });
      _ut.test('test_inconsistentMethodInheritance_paramType', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_paramType);
      });
      _ut.test('test_inconsistentMethodInheritance_returnType', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritance_returnType);
      });
      _ut.test('test_instanceAccessToStaticMember_method_invocation', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceAccessToStaticMember_method_invocation);
      });
      _ut.test('test_instanceAccessToStaticMember_method_reference', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceAccessToStaticMember_method_reference);
      });
      _ut.test('test_instanceAccessToStaticMember_propertyAccess_field', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceAccessToStaticMember_propertyAccess_field);
      });
      _ut.test('test_instanceAccessToStaticMember_propertyAccess_getter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceAccessToStaticMember_propertyAccess_getter);
      });
      _ut.test('test_instanceAccessToStaticMember_propertyAccess_setter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceAccessToStaticMember_propertyAccess_setter);
      });
      _ut.test('test_invalidAssignment_compoundAssignment', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_compoundAssignment);
      });
      _ut.test('test_invalidAssignment_defaultValue_named', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_defaultValue_named);
      });
      _ut.test('test_invalidAssignment_defaultValue_optional', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_defaultValue_optional);
      });
      _ut.test('test_invalidAssignment_instanceVariable', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_instanceVariable);
      });
      _ut.test('test_invalidAssignment_localVariable', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_localVariable);
      });
      _ut.test('test_invalidAssignment_staticVariable', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_staticVariable);
      });
      _ut.test('test_invalidAssignment_topLevelVariableDeclaration', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_topLevelVariableDeclaration);
      });
      _ut.test('test_invalidAssignment_variableDeclaration', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidAssignment_variableDeclaration);
      });
      _ut.test('test_invocationOfNonFunctionExpression_literal', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunctionExpression_literal);
      });
      _ut.test('test_invocationOfNonFunction_class', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_class);
      });
      _ut.test('test_invocationOfNonFunction_localVariable', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_localVariable);
      });
      _ut.test('test_invocationOfNonFunction_ordinaryInvocation', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_ordinaryInvocation);
      });
      _ut.test('test_invocationOfNonFunction_staticInvocation', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_staticInvocation);
      });
      _ut.test('test_invocationOfNonFunction_superExpression', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_invocationOfNonFunction_superExpression);
      });
      _ut.test('test_nonBoolCondition_conditional', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonBoolCondition_conditional);
      });
      _ut.test('test_nonBoolCondition_do', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonBoolCondition_do);
      });
      _ut.test('test_nonBoolCondition_if', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonBoolCondition_if);
      });
      _ut.test('test_nonBoolCondition_while', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonBoolCondition_while);
      });
      _ut.test('test_nonBoolExpression_functionType', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonBoolExpression_functionType);
      });
      _ut.test('test_nonBoolExpression_interfaceType', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonBoolExpression_interfaceType);
      });
      _ut.test('test_nonBoolNegationExpression', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonBoolNegationExpression);
      });
      _ut.test('test_nonTypeAsTypeArgument_notAType', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonTypeAsTypeArgument_notAType);
      });
      _ut.test('test_nonTypeAsTypeArgument_undefinedIdentifier', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_nonTypeAsTypeArgument_undefinedIdentifier);
      });
      _ut.test('test_returnOfInvalidType_expressionFunctionBody_function', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_expressionFunctionBody_function);
      });
      _ut.test('test_returnOfInvalidType_expressionFunctionBody_getter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_expressionFunctionBody_getter);
      });
      _ut.test('test_returnOfInvalidType_expressionFunctionBody_localFunction', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_expressionFunctionBody_localFunction);
      });
      _ut.test('test_returnOfInvalidType_expressionFunctionBody_method', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_expressionFunctionBody_method);
      });
      _ut.test('test_returnOfInvalidType_expressionFunctionBody_void', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_expressionFunctionBody_void);
      });
      _ut.test('test_returnOfInvalidType_function', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_function);
      });
      _ut.test('test_returnOfInvalidType_getter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_getter);
      });
      _ut.test('test_returnOfInvalidType_localFunction', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_localFunction);
      });
      _ut.test('test_returnOfInvalidType_method', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_method);
      });
      _ut.test('test_returnOfInvalidType_void', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_returnOfInvalidType_void);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_classTypeAlias', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_classTypeAlias);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_extends', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_extends);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_fieldFormalParameter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_fieldFormalParameter);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_functionReturnType', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_functionReturnType);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_functionTypeAlias', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_functionTypeAlias);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_functionTypedFormalParameter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_functionTypedFormalParameter);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_implements', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_implements);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_is', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_is);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_methodReturnType', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_methodReturnType);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_new', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_new);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_new_superTypeOfUpperBound', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_new_superTypeOfUpperBound);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_parameter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_parameter);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_redirectingConstructor', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_redirectingConstructor);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_typeArgumentList', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_typeArgumentList);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_typeParameter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_typeParameter);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_variableDeclaration', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_variableDeclaration);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_with', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_with);
      });
      _ut.test('test_typeParameterSupertypeOfItsBound', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typeParameterSupertypeOfItsBound);
      });
      _ut.test('test_typePromotion_booleanAnd_useInRight_accessedInClosureRight_mutated', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_booleanAnd_useInRight_accessedInClosureRight_mutated);
      });
      _ut.test('test_typePromotion_booleanAnd_useInRight_mutatedInLeft', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_booleanAnd_useInRight_mutatedInLeft);
      });
      _ut.test('test_typePromotion_booleanAnd_useInRight_mutatedInRight', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_booleanAnd_useInRight_mutatedInRight);
      });
      _ut.test('test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_after', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_after);
      });
      _ut.test('test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_before', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_before);
      });
      _ut.test('test_typePromotion_conditional_useInThen_hasAssignment', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_conditional_useInThen_hasAssignment);
      });
      _ut.test('test_typePromotion_if_accessedInClosure_hasAssignment', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_accessedInClosure_hasAssignment);
      });
      _ut.test('test_typePromotion_if_and_right_hasAssignment', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_and_right_hasAssignment);
      });
      _ut.test('test_typePromotion_if_extends_notMoreSpecific_dynamic', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_extends_notMoreSpecific_dynamic);
      });
      _ut.test('test_typePromotion_if_extends_notMoreSpecific_notMoreSpecificTypeArg', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_extends_notMoreSpecific_notMoreSpecificTypeArg);
      });
      _ut.test('test_typePromotion_if_hasAssignment_after', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_after);
      });
      _ut.test('test_typePromotion_if_hasAssignment_before', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_before);
      });
      _ut.test('test_typePromotion_if_hasAssignment_inClosure_anonymous_after', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_inClosure_anonymous_after);
      });
      _ut.test('test_typePromotion_if_hasAssignment_inClosure_anonymous_before', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_inClosure_anonymous_before);
      });
      _ut.test('test_typePromotion_if_hasAssignment_inClosure_function_after', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_inClosure_function_after);
      });
      _ut.test('test_typePromotion_if_hasAssignment_inClosure_function_before', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_hasAssignment_inClosure_function_before);
      });
      _ut.test('test_typePromotion_if_implements_notMoreSpecific_dynamic', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_implements_notMoreSpecific_dynamic);
      });
      _ut.test('test_typePromotion_if_with_notMoreSpecific_dynamic', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_if_with_notMoreSpecific_dynamic);
      });
      _ut.test('test_undefinedGetter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedGetter);
      });
      _ut.test('test_undefinedGetter_static', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedGetter_static);
      });
      _ut.test('test_undefinedMethod', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod);
      });
      _ut.test('test_undefinedMethod_assignmentExpression', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod_assignmentExpression);
      });
      _ut.test('test_undefinedMethod_ignoreTypePropagation', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod_ignoreTypePropagation);
      });
      _ut.test('test_undefinedMethod_private', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod_private);
      });
      _ut.test('test_undefinedOperator_indexBoth', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexBoth);
      });
      _ut.test('test_undefinedOperator_indexGetter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexGetter);
      });
      _ut.test('test_undefinedOperator_indexSetter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexSetter);
      });
      _ut.test('test_undefinedOperator_plus', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_plus);
      });
      _ut.test('test_undefinedOperator_postfixExpression', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_postfixExpression);
      });
      _ut.test('test_undefinedOperator_prefixExpression', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_prefixExpression);
      });
      _ut.test('test_undefinedSetter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedSetter);
      });
      _ut.test('test_undefinedSetter_static', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedSetter_static);
      });
      _ut.test('test_undefinedSuperMethod', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedSuperMethod);
      });
      _ut.test('test_unqualifiedReferenceToNonLocalStaticMember_getter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_unqualifiedReferenceToNonLocalStaticMember_getter);
      });
      _ut.test('test_unqualifiedReferenceToNonLocalStaticMember_method', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_unqualifiedReferenceToNonLocalStaticMember_method);
      });
      _ut.test('test_unqualifiedReferenceToNonLocalStaticMember_setter', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_unqualifiedReferenceToNonLocalStaticMember_setter);
      });
      _ut.test('test_wrongNumberOfTypeArguments_tooFew', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfTypeArguments_tooFew);
      });
      _ut.test('test_wrongNumberOfTypeArguments_tooMany', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfTypeArguments_tooMany);
      });
      _ut.test('test_wrongNumberOfTypeArguments_typeTest_tooFew', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfTypeArguments_typeTest_tooFew);
      });
      _ut.test('test_wrongNumberOfTypeArguments_typeTest_tooMany', () {
        final __test = new StaticTypeWarningCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfTypeArguments_typeTest_tooMany);
      });
    });
  }
}
class HintCodeTest extends ResolverTestCase {
  void fail_isInt() {
    Source source = addSource(EngineTestCase.createSource(["var v = 1 is int;"]));
    resolve(source);
    assertErrors(source, [HintCode.IS_INT]);
    verify([source]);
  }
  void fail_isNotInt() {
    Source source = addSource(EngineTestCase.createSource(["var v = 1 is! int;"]));
    resolve(source);
    assertErrors(source, [HintCode.IS_NOT_INT]);
    verify([source]);
  }
  void fail_overrideEqualsButNotHashCode() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  bool operator ==(x) {}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.OVERRIDE_EQUALS_BUT_NOT_HASH_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_conditionalElse() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  true ? 1 : 2;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_conditionalElse_nested() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  true ? true : false && false;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_conditionalIf() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  false ? 1 : 2;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_conditionalIf_nested() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  false ? false && false : true;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_else() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  if(true) {} else {}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_else_nested() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  if(true) {} else {if (false) {}}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_if() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  if(false) {}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_if_nested() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  if(false) {if(false) {}}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_while() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  while(false) {}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadBlock_while_nested() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  while(false) {if(false) {}}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadCatch_catchFollowingCatch() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f() {",
        "  try {} catch (e) {} catch (e) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH]);
    verify([source]);
  }
  void test_deadCode_deadCatch_catchFollowingCatch_nested() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f() {",
        "  try {} catch (e) {} catch (e) {if(false) {}}",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH]);
    verify([source]);
  }
  void test_deadCode_deadCatch_catchFollowingCatch_object() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  try {} on Object catch (e) {} catch (e) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH]);
    verify([source]);
  }
  void test_deadCode_deadCatch_catchFollowingCatch_object_nested() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  try {} on Object catch (e) {} catch (e) {if(false) {}}",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH]);
    verify([source]);
  }
  void test_deadCode_deadCatch_onCatchSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "f() {",
        "  try {} on A catch (e) {} on B catch (e) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE_ON_CATCH_SUBTYPE]);
    verify([source]);
  }
  void test_deadCode_deadCatch_onCatchSubtype_nested() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "f() {",
        "  try {} on A catch (e) {} on B catch (e) {if(false) {}}",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE_ON_CATCH_SUBTYPE]);
    verify([source]);
  }
  void test_deadCode_deadOperandLHS_and() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  bool b = false && false;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadOperandLHS_and_nested() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  bool b = false && (false && false);", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadOperandLHS_or() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  bool b = true || true;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_deadOperandLHS_or_nested() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  bool b = true || (false && false);", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_statementAfterReturn_function() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  var one = 1;",
        "  return;",
        "  var two = 2;",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_statementAfterReturn_ifStatement() {
    Source source = addSource(EngineTestCase.createSource([
        "f(bool b) {",
        "  if(b) {",
        "    var one = 1;",
        "    return;",
        "    var two = 2;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_statementAfterReturn_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {",
        "    var one = 1;",
        "    return;",
        "    var two = 2;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_statementAfterReturn_nested() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  var one = 1;",
        "  return;",
        "  if(false) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_deadCode_statementAfterReturn_twoReturns() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  var one = 1;",
        "  return;",
        "  var two = 2;",
        "  return;",
        "  var three = 3;",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_divisionOptimization_double() {
    Source source = addSource(EngineTestCase.createSource([
        "f(double x, double y) {",
        "  var v = (x / y).toInt();",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DIVISION_OPTIMIZATION]);
    verify([source]);
  }
  void test_divisionOptimization_int() {
    Source source = addSource(EngineTestCase.createSource(["f(int x, int y) {", "  var v = (x / y).toInt();", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DIVISION_OPTIMIZATION]);
    verify([source]);
  }
  void test_divisionOptimization_propagatedType() {
    Source source = addSource(EngineTestCase.createSource([
        "f(x, y) {",
        "  x = 1;",
        "  y = 1;",
        "  var v = (x / y).toInt();",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DIVISION_OPTIMIZATION]);
    verify([source]);
  }
  void test_divisionOptimization_wrappedBinaryExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int x, int y) {",
        "  var v = (((x / y))).toInt();",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.DIVISION_OPTIMIZATION]);
    verify([source]);
  }
  void test_duplicateImport() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart';",
        "A a;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}"]));
    resolve(source);
    assertErrors(source, [HintCode.DUPLICATE_IMPORT]);
    verify([source]);
  }
  void test_duplicateImport2() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart';",
        "import 'lib1.dart';",
        "A a;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}"]));
    resolve(source);
    assertErrors(source, [HintCode.DUPLICATE_IMPORT, HintCode.DUPLICATE_IMPORT]);
    verify([source]);
  }
  void test_duplicateImport3() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart' as M show A hide B;",
        "import 'lib1.dart' as M show A hide B;",
        "M.A a;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}", "class B {}"]));
    resolve(source);
    assertErrors(source, [HintCode.DUPLICATE_IMPORT, HintCode.UNUSED_IMPORT]);
    verify([source]);
  }
  void test_isDouble() {
    Source source = addSource(EngineTestCase.createSource(["var v = 1 is double;"]));
    resolve(source);
    assertErrors(source, [HintCode.IS_DOUBLE]);
    verify([source]);
  }
  void test_isNotDouble() {
    Source source = addSource(EngineTestCase.createSource(["var v = 1 is! double;"]));
    resolve(source);
    assertErrors(source, [HintCode.IS_NOT_DOUBLE]);
    verify([source]);
  }
  void test_overriddingPrivateMember_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "class B extends A {",
        "  get _g => 0;",
        "}"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {", "  get _g => 0;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.OVERRIDDING_PRIVATE_MEMBER]);
    verify([source, source2]);
  }
  void test_overriddingPrivateMember_method() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "class B extends A {",
        "  _m(int x) => 0;",
        "}"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {", "  _m(int x) => 0;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.OVERRIDDING_PRIVATE_MEMBER]);
    verify([source, source2]);
  }
  void test_overriddingPrivateMember_method2() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "class B extends A {}",
        "class C extends B {",
        "  _m(int x) => 0;",
        "}"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {", "  _m(int x) => 0;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.OVERRIDDING_PRIVATE_MEMBER]);
    verify([source, source2]);
  }
  void test_overriddingPrivateMember_setter() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "class B extends A {",
        "  set _s(int x) {}",
        "}"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {", "  set _s(int x) {}", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.OVERRIDDING_PRIVATE_MEMBER]);
    verify([source, source2]);
  }
  void test_typeCheck_type_is_Null() {
    Source source = addSource(EngineTestCase.createSource(["m(i) {", "  bool b = i is Null;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.TYPE_CHECK_IS_NULL]);
    verify([source]);
  }
  void test_typeCheck_type_not_Null() {
    Source source = addSource(EngineTestCase.createSource(["m(i) {", "  bool b = i is! Null;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.TYPE_CHECK_IS_NOT_NULL]);
    verify([source]);
  }
  void test_undefinedGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    return a.m;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_GETTER]);
  }
  void test_undefinedGetter_message() {
    JUnitTestCase.assertEquals(StaticTypeWarningCode.UNDEFINED_GETTER.message, StaticWarningCode.UNDEFINED_GETTER.message);
  }
  void test_undefinedMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  var a = 'str';",
        "  a.notAMethodOnString();",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_METHOD]);
  }
  void test_undefinedMethod_assignmentExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {",
        "  f(var a, var a2) {",
        "    a = new A();",
        "    a2 = new A();",
        "    a += a2;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_METHOD]);
  }
  void test_undefinedOperator_binaryExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a + 1;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_indexBoth() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0]++;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_indexGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0];",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_indexSetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0] = 1;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_postfixExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a++;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedOperator_prefixExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    ++a;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_OPERATOR]);
  }
  void test_undefinedSetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a.m = 0;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNDEFINED_SETTER]);
  }
  void test_undefinedSetter_message() {
    JUnitTestCase.assertEquals(StaticTypeWarningCode.UNDEFINED_SETTER.message, StaticWarningCode.UNDEFINED_SETTER.message);
  }
  void test_unnecessaryCast_type_supertype() {
    Source source = addSource(EngineTestCase.createSource(["m(int i) {", "  var b = i as Object;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_CAST]);
    verify([source]);
  }
  void test_unnecessaryCast_type_type() {
    Source source = addSource(EngineTestCase.createSource(["m(num i) {", "  var b = i as num;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_CAST]);
    verify([source]);
  }
  void test_unnecessaryTypeCheck_null_is_Null() {
    Source source = addSource(EngineTestCase.createSource(["bool b = null is Null;"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_TYPE_CHECK_TRUE]);
    verify([source]);
  }
  void test_unnecessaryTypeCheck_null_not_Null() {
    Source source = addSource(EngineTestCase.createSource(["bool b = null is! Null;"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_TYPE_CHECK_FALSE]);
    verify([source]);
  }
  void test_unnecessaryTypeCheck_type_is_dynamic() {
    Source source = addSource(EngineTestCase.createSource(["m(i) {", "  bool b = i is dynamic;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_TYPE_CHECK_TRUE]);
    verify([source]);
  }
  void test_unnecessaryTypeCheck_type_is_object() {
    Source source = addSource(EngineTestCase.createSource(["m(i) {", "  bool b = i is Object;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_TYPE_CHECK_TRUE]);
    verify([source]);
  }
  void test_unnecessaryTypeCheck_type_not_dynamic() {
    Source source = addSource(EngineTestCase.createSource(["m(i) {", "  bool b = i is! dynamic;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_TYPE_CHECK_FALSE]);
    verify([source]);
  }
  void test_unnecessaryTypeCheck_type_not_object() {
    Source source = addSource(EngineTestCase.createSource(["m(i) {", "  bool b = i is! Object;", "}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNNECESSARY_TYPE_CHECK_FALSE]);
    verify([source]);
  }
  void test_unusedImport() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "import 'lib1.dart';"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;"]));
    resolve(source);
    assertErrors(source, [HintCode.UNUSED_IMPORT]);
    assertNoErrors(source2);
    verify([source, source2]);
  }
  void test_unusedImport_as() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart' as one;",
        "one.A a;"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNUSED_IMPORT]);
    assertNoErrors(source2);
    verify([source, source2]);
  }
  void test_unusedImport_hide() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart' hide A;",
        "A a;"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNUSED_IMPORT]);
    assertNoErrors(source2);
    verify([source, source2]);
  }
  void test_unusedImport_show() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart' show A;",
        "import 'lib1.dart' show B;",
        "A a;"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}", "class B {}"]));
    resolve(source);
    assertErrors(source, [HintCode.UNUSED_IMPORT]);
    assertNoErrors(source2);
    verify([source, source2]);
  }
  static dartSuite() {
    _ut.group('HintCodeTest', () {
      _ut.test('test_deadCode_deadBlock_conditionalElse', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_conditionalElse);
      });
      _ut.test('test_deadCode_deadBlock_conditionalElse_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_conditionalElse_nested);
      });
      _ut.test('test_deadCode_deadBlock_conditionalIf', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_conditionalIf);
      });
      _ut.test('test_deadCode_deadBlock_conditionalIf_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_conditionalIf_nested);
      });
      _ut.test('test_deadCode_deadBlock_else', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_else);
      });
      _ut.test('test_deadCode_deadBlock_else_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_else_nested);
      });
      _ut.test('test_deadCode_deadBlock_if', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_if);
      });
      _ut.test('test_deadCode_deadBlock_if_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_if_nested);
      });
      _ut.test('test_deadCode_deadBlock_while', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_while);
      });
      _ut.test('test_deadCode_deadBlock_while_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_while_nested);
      });
      _ut.test('test_deadCode_deadCatch_catchFollowingCatch', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadCatch_catchFollowingCatch);
      });
      _ut.test('test_deadCode_deadCatch_catchFollowingCatch_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadCatch_catchFollowingCatch_nested);
      });
      _ut.test('test_deadCode_deadCatch_catchFollowingCatch_object', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadCatch_catchFollowingCatch_object);
      });
      _ut.test('test_deadCode_deadCatch_catchFollowingCatch_object_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadCatch_catchFollowingCatch_object_nested);
      });
      _ut.test('test_deadCode_deadCatch_onCatchSubtype', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadCatch_onCatchSubtype);
      });
      _ut.test('test_deadCode_deadCatch_onCatchSubtype_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadCatch_onCatchSubtype_nested);
      });
      _ut.test('test_deadCode_deadOperandLHS_and', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadOperandLHS_and);
      });
      _ut.test('test_deadCode_deadOperandLHS_and_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadOperandLHS_and_nested);
      });
      _ut.test('test_deadCode_deadOperandLHS_or', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadOperandLHS_or);
      });
      _ut.test('test_deadCode_deadOperandLHS_or_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadOperandLHS_or_nested);
      });
      _ut.test('test_deadCode_statementAfterReturn_function', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_statementAfterReturn_function);
      });
      _ut.test('test_deadCode_statementAfterReturn_ifStatement', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_statementAfterReturn_ifStatement);
      });
      _ut.test('test_deadCode_statementAfterReturn_method', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_statementAfterReturn_method);
      });
      _ut.test('test_deadCode_statementAfterReturn_nested', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_statementAfterReturn_nested);
      });
      _ut.test('test_deadCode_statementAfterReturn_twoReturns', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_statementAfterReturn_twoReturns);
      });
      _ut.test('test_divisionOptimization_double', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_divisionOptimization_double);
      });
      _ut.test('test_divisionOptimization_int', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_divisionOptimization_int);
      });
      _ut.test('test_divisionOptimization_propagatedType', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_divisionOptimization_propagatedType);
      });
      _ut.test('test_divisionOptimization_wrappedBinaryExpression', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_divisionOptimization_wrappedBinaryExpression);
      });
      _ut.test('test_duplicateImport', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_duplicateImport);
      });
      _ut.test('test_duplicateImport2', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_duplicateImport2);
      });
      _ut.test('test_duplicateImport3', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_duplicateImport3);
      });
      _ut.test('test_isDouble', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_isDouble);
      });
      _ut.test('test_isNotDouble', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_isNotDouble);
      });
      _ut.test('test_overriddingPrivateMember_getter', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_overriddingPrivateMember_getter);
      });
      _ut.test('test_overriddingPrivateMember_method', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_overriddingPrivateMember_method);
      });
      _ut.test('test_overriddingPrivateMember_method2', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_overriddingPrivateMember_method2);
      });
      _ut.test('test_overriddingPrivateMember_setter', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_overriddingPrivateMember_setter);
      });
      _ut.test('test_typeCheck_type_is_Null', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_typeCheck_type_is_Null);
      });
      _ut.test('test_typeCheck_type_not_Null', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_typeCheck_type_not_Null);
      });
      _ut.test('test_undefinedGetter', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedGetter);
      });
      _ut.test('test_undefinedGetter_message', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedGetter_message);
      });
      _ut.test('test_undefinedMethod', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod);
      });
      _ut.test('test_undefinedMethod_assignmentExpression', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod_assignmentExpression);
      });
      _ut.test('test_undefinedOperator_binaryExpression', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_binaryExpression);
      });
      _ut.test('test_undefinedOperator_indexBoth', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexBoth);
      });
      _ut.test('test_undefinedOperator_indexGetter', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexGetter);
      });
      _ut.test('test_undefinedOperator_indexSetter', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexSetter);
      });
      _ut.test('test_undefinedOperator_postfixExpression', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_postfixExpression);
      });
      _ut.test('test_undefinedOperator_prefixExpression', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_prefixExpression);
      });
      _ut.test('test_undefinedSetter', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedSetter);
      });
      _ut.test('test_undefinedSetter_message', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_undefinedSetter_message);
      });
      _ut.test('test_unnecessaryCast_type_supertype', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryCast_type_supertype);
      });
      _ut.test('test_unnecessaryCast_type_type', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryCast_type_type);
      });
      _ut.test('test_unnecessaryTypeCheck_null_is_Null', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryTypeCheck_null_is_Null);
      });
      _ut.test('test_unnecessaryTypeCheck_null_not_Null', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryTypeCheck_null_not_Null);
      });
      _ut.test('test_unnecessaryTypeCheck_type_is_dynamic', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryTypeCheck_type_is_dynamic);
      });
      _ut.test('test_unnecessaryTypeCheck_type_is_object', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryTypeCheck_type_is_object);
      });
      _ut.test('test_unnecessaryTypeCheck_type_not_dynamic', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryTypeCheck_type_not_dynamic);
      });
      _ut.test('test_unnecessaryTypeCheck_type_not_object', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryTypeCheck_type_not_object);
      });
      _ut.test('test_unusedImport', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport);
      });
      _ut.test('test_unusedImport_as', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_as);
      });
      _ut.test('test_unusedImport_hide', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_hide);
      });
      _ut.test('test_unusedImport_show', () {
        final __test = new HintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_show);
      });
    });
  }
}
class TypeResolverVisitorTest extends EngineTestCase {

  /**
   * The error listener to which errors will be reported.
   */
  GatheringErrorListener _listener;

  /**
   * The object representing the information about the library in which the types are being
   * resolved.
   */
  Library _library;

  /**
   * The type provider used to access the types.
   */
  TestTypeProvider _typeProvider;

  /**
   * The visitor used to resolve types needed to form the type hierarchy.
   */
  TypeResolverVisitor _visitor;
  void fail_visitConstructorDeclaration() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitFunctionDeclaration() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitFunctionTypeAlias() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitFunctionTypedFormalParameter() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitMethodDeclaration() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitVariableDeclaration() {
    JUnitTestCase.fail("Not yet tested");
    ClassElement type = ElementFactory.classElement2("A", []);
    VariableDeclaration node = ASTFactory.variableDeclaration("a");
    ASTFactory.variableDeclarationList(null, ASTFactory.typeName(type, []), [node]);
    JUnitTestCase.assertSame(type.type, node.name.staticType);
    _listener.assertNoErrors();
  }
  void setUp() {
    _listener = new GatheringErrorListener();
    SourceFactory factory = new SourceFactory.con2([new FileUriResolver()]);
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.sourceFactory = factory;
    Source librarySource = new FileBasedSource.con1(factory.contentCache, FileUtilities2.createFile("/lib.dart"));
    _library = new Library(context, _listener, librarySource);
    LibraryElementImpl element = new LibraryElementImpl(context, ASTFactory.libraryIdentifier2(["lib"]));
    element.definingCompilationUnit = new CompilationUnitElementImpl("lib.dart");
    _library.libraryElement = element;
    _typeProvider = new TestTypeProvider();
    _visitor = new TypeResolverVisitor.con1(_library, librarySource, _typeProvider);
  }
  void test_visitCatchClause_exception() {
    CatchClause clause = ASTFactory.catchClause("e", []);
    SimpleIdentifier exceptionParameter = clause.exceptionParameter;
    exceptionParameter.staticElement = new LocalVariableElementImpl(exceptionParameter);
    resolve(clause, _typeProvider.dynamicType, null, []);
    _listener.assertNoErrors();
  }
  void test_visitCatchClause_exception_stackTrace() {
    CatchClause clause = ASTFactory.catchClause2("e", "s", []);
    SimpleIdentifier exceptionParameter = clause.exceptionParameter;
    exceptionParameter.staticElement = new LocalVariableElementImpl(exceptionParameter);
    SimpleIdentifier stackTraceParameter = clause.stackTraceParameter;
    stackTraceParameter.staticElement = new LocalVariableElementImpl(stackTraceParameter);
    resolve(clause, _typeProvider.dynamicType, _typeProvider.stackTraceType, []);
    _listener.assertNoErrors();
  }
  void test_visitCatchClause_on_exception() {
    ClassElement exceptionElement = ElementFactory.classElement2("E", []);
    TypeName exceptionType = ASTFactory.typeName(exceptionElement, []);
    CatchClause clause = ASTFactory.catchClause4(exceptionType, "e", []);
    SimpleIdentifier exceptionParameter = clause.exceptionParameter;
    exceptionParameter.staticElement = new LocalVariableElementImpl(exceptionParameter);
    resolve(clause, exceptionElement.type, null, [exceptionElement]);
    _listener.assertNoErrors();
  }
  void test_visitCatchClause_on_exception_stackTrace() {
    ClassElement exceptionElement = ElementFactory.classElement2("E", []);
    TypeName exceptionType = ASTFactory.typeName(exceptionElement, []);
    ((exceptionType.name as SimpleIdentifier)).staticElement = exceptionElement;
    CatchClause clause = ASTFactory.catchClause5(exceptionType, "e", "s", []);
    SimpleIdentifier exceptionParameter = clause.exceptionParameter;
    exceptionParameter.staticElement = new LocalVariableElementImpl(exceptionParameter);
    SimpleIdentifier stackTraceParameter = clause.stackTraceParameter;
    stackTraceParameter.staticElement = new LocalVariableElementImpl(stackTraceParameter);
    resolve(clause, exceptionElement.type, _typeProvider.stackTraceType, [exceptionElement]);
    _listener.assertNoErrors();
  }
  void test_visitClassDeclaration() {
    ClassElement elementA = ElementFactory.classElement2("A", []);
    ClassElement elementB = ElementFactory.classElement2("B", []);
    ClassElement elementC = ElementFactory.classElement2("C", []);
    ClassElement elementD = ElementFactory.classElement2("D", []);
    ExtendsClause extendsClause = ASTFactory.extendsClause(ASTFactory.typeName(elementB, []));
    WithClause withClause = ASTFactory.withClause([ASTFactory.typeName(elementC, [])]);
    ImplementsClause implementsClause = ASTFactory.implementsClause([ASTFactory.typeName(elementD, [])]);
    ClassDeclaration declaration = ASTFactory.classDeclaration(null, "A", null, extendsClause, withClause, implementsClause, []);
    declaration.name.staticElement = elementA;
    resolveNode(declaration, [elementA, elementB, elementC, elementD]);
    JUnitTestCase.assertSame(elementB.type, elementA.supertype);
    List<InterfaceType> mixins = elementA.mixins;
    EngineTestCase.assertLength(1, mixins);
    JUnitTestCase.assertSame(elementC.type, mixins[0]);
    List<InterfaceType> interfaces = elementA.interfaces;
    EngineTestCase.assertLength(1, interfaces);
    JUnitTestCase.assertSame(elementD.type, interfaces[0]);
    _listener.assertNoErrors();
  }
  void test_visitClassTypeAlias() {
    ClassElement elementA = ElementFactory.classElement2("A", []);
    ClassElement elementB = ElementFactory.classElement2("B", []);
    ClassElement elementC = ElementFactory.classElement2("C", []);
    ClassElement elementD = ElementFactory.classElement2("D", []);
    WithClause withClause = ASTFactory.withClause([ASTFactory.typeName(elementC, [])]);
    ImplementsClause implementsClause = ASTFactory.implementsClause([ASTFactory.typeName(elementD, [])]);
    ClassTypeAlias alias = ASTFactory.classTypeAlias("A", null, null, ASTFactory.typeName(elementB, []), withClause, implementsClause);
    alias.name.staticElement = elementA;
    resolveNode(alias, [elementA, elementB, elementC, elementD]);
    JUnitTestCase.assertSame(elementB.type, elementA.supertype);
    List<InterfaceType> mixins = elementA.mixins;
    EngineTestCase.assertLength(1, mixins);
    JUnitTestCase.assertSame(elementC.type, mixins[0]);
    List<InterfaceType> interfaces = elementA.interfaces;
    EngineTestCase.assertLength(1, interfaces);
    JUnitTestCase.assertSame(elementD.type, interfaces[0]);
    _listener.assertNoErrors();
  }
  void test_visitFieldFormalParameter_functionType() {
    InterfaceType intType = _typeProvider.intType;
    TypeName intTypeName = ASTFactory.typeName4("int", []);
    String innerParameterName = "a";
    SimpleFormalParameter parameter = ASTFactory.simpleFormalParameter3(innerParameterName);
    parameter.identifier.staticElement = ElementFactory.requiredParameter(innerParameterName);
    String outerParameterName = "p";
    FormalParameter node = ASTFactory.fieldFormalParameter2(null, intTypeName, outerParameterName, ASTFactory.formalParameterList([parameter]));
    node.identifier.staticElement = ElementFactory.requiredParameter(outerParameterName);
    Type2 parameterType = resolve6(node, [intType.element]);
    EngineTestCase.assertInstanceOf(FunctionType, parameterType);
    FunctionType functionType = parameterType as FunctionType;
    JUnitTestCase.assertSame(intType, functionType.returnType);
    EngineTestCase.assertLength(1, functionType.parameters);
    _listener.assertNoErrors();
  }
  void test_visitFieldFormalParameter_noType() {
    String parameterName = "p";
    FormalParameter node = ASTFactory.fieldFormalParameter(Keyword.VAR, null, parameterName);
    node.identifier.staticElement = ElementFactory.requiredParameter(parameterName);
    JUnitTestCase.assertSame(_typeProvider.dynamicType, resolve6(node, []));
    _listener.assertNoErrors();
  }
  void test_visitFieldFormalParameter_type() {
    InterfaceType intType = _typeProvider.intType;
    TypeName intTypeName = ASTFactory.typeName4("int", []);
    String parameterName = "p";
    FormalParameter node = ASTFactory.fieldFormalParameter(null, intTypeName, parameterName);
    node.identifier.staticElement = ElementFactory.requiredParameter(parameterName);
    JUnitTestCase.assertSame(intType, resolve6(node, [intType.element]));
    _listener.assertNoErrors();
  }
  void test_visitSimpleFormalParameter_noType() {
    FormalParameter node = ASTFactory.simpleFormalParameter3("p");
    node.identifier.staticElement = new ParameterElementImpl.con1(ASTFactory.identifier3("p"));
    JUnitTestCase.assertSame(_typeProvider.dynamicType, resolve6(node, []));
    _listener.assertNoErrors();
  }
  void test_visitSimpleFormalParameter_type() {
    InterfaceType intType = _typeProvider.intType;
    ClassElement intElement = intType.element;
    FormalParameter node = ASTFactory.simpleFormalParameter4(ASTFactory.typeName(intElement, []), "p");
    SimpleIdentifier identifier = node.identifier;
    ParameterElementImpl element = new ParameterElementImpl.con1(identifier);
    identifier.staticElement = element;
    JUnitTestCase.assertSame(intType, resolve6(node, [intElement]));
    _listener.assertNoErrors();
  }
  void test_visitTypeName_noParameters_noArguments() {
    ClassElement classA = ElementFactory.classElement2("A", []);
    TypeName typeName = ASTFactory.typeName(classA, []);
    typeName.type = null;
    resolveNode(typeName, [classA]);
    JUnitTestCase.assertSame(classA.type, typeName.type);
    _listener.assertNoErrors();
  }
  void test_visitTypeName_parameters_arguments() {
    ClassElement classA = ElementFactory.classElement2("A", ["E"]);
    ClassElement classB = ElementFactory.classElement2("B", []);
    TypeName typeName = ASTFactory.typeName(classA, [ASTFactory.typeName(classB, [])]);
    typeName.type = null;
    resolveNode(typeName, [classA, classB]);
    InterfaceType resultType = typeName.type as InterfaceType;
    JUnitTestCase.assertSame(classA, resultType.element);
    List<Type2> resultArguments = resultType.typeArguments;
    EngineTestCase.assertLength(1, resultArguments);
    JUnitTestCase.assertSame(classB.type, resultArguments[0]);
    _listener.assertNoErrors();
  }
  void test_visitTypeName_parameters_noArguments() {
    ClassElement classA = ElementFactory.classElement2("A", ["E"]);
    TypeName typeName = ASTFactory.typeName(classA, []);
    typeName.type = null;
    resolveNode(typeName, [classA]);
    InterfaceType resultType = typeName.type as InterfaceType;
    JUnitTestCase.assertSame(classA, resultType.element);
    List<Type2> resultArguments = resultType.typeArguments;
    EngineTestCase.assertLength(1, resultArguments);
    JUnitTestCase.assertSame(DynamicTypeImpl.instance, resultArguments[0]);
    _listener.assertNoErrors();
  }
  void test_visitTypeName_void() {
    ClassElement classA = ElementFactory.classElement2("A", []);
    TypeName typeName = ASTFactory.typeName4("void", []);
    resolveNode(typeName, [classA]);
    JUnitTestCase.assertSame(VoidTypeImpl.instance, typeName.type);
    _listener.assertNoErrors();
  }

  /**
   * Analyze the given catch clause and assert that the types of the parameters have been set to the
   * given types. The types can be null if the catch clause does not have the corresponding
   * parameter.
   *
   * @param node the catch clause to be analyzed
   * @param exceptionType the expected type of the exception parameter
   * @param stackTraceType the expected type of the stack trace parameter
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   */
  void resolve(CatchClause node, Type2 exceptionType, InterfaceType stackTraceType, List<Element> definedElements) {
    resolveNode(node, definedElements);
    SimpleIdentifier exceptionParameter = node.exceptionParameter;
    if (exceptionParameter != null) {
      JUnitTestCase.assertSame(exceptionType, exceptionParameter.staticType);
    }
    SimpleIdentifier stackTraceParameter = node.stackTraceParameter;
    if (stackTraceParameter != null) {
      JUnitTestCase.assertSame(stackTraceType, stackTraceParameter.staticType);
    }
  }

  /**
   * Return the type associated with the given parameter after the static type analyzer has computed
   * a type for it.
   *
   * @param node the parameter with which the type is associated
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the type associated with the parameter
   */
  Type2 resolve6(FormalParameter node, List<Element> definedElements) {
    resolveNode(node, definedElements);
    return ((node.identifier.staticElement as ParameterElement)).type;
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   *
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  void resolveNode(ASTNode node, List<Element> definedElements) {
    for (Element element in definedElements) {
      _library.libraryScope.define(element);
    }
    node.accept(_visitor);
  }
  static dartSuite() {
    _ut.group('TypeResolverVisitorTest', () {
      _ut.test('test_visitCatchClause_exception', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitCatchClause_exception);
      });
      _ut.test('test_visitCatchClause_exception_stackTrace', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitCatchClause_exception_stackTrace);
      });
      _ut.test('test_visitCatchClause_on_exception', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitCatchClause_on_exception);
      });
      _ut.test('test_visitCatchClause_on_exception_stackTrace', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitCatchClause_on_exception_stackTrace);
      });
      _ut.test('test_visitClassDeclaration', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitClassDeclaration);
      });
      _ut.test('test_visitClassTypeAlias', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitClassTypeAlias);
      });
      _ut.test('test_visitFieldFormalParameter_functionType', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitFieldFormalParameter_functionType);
      });
      _ut.test('test_visitFieldFormalParameter_noType', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitFieldFormalParameter_noType);
      });
      _ut.test('test_visitFieldFormalParameter_type', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitFieldFormalParameter_type);
      });
      _ut.test('test_visitSimpleFormalParameter_noType', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitSimpleFormalParameter_noType);
      });
      _ut.test('test_visitSimpleFormalParameter_type', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitSimpleFormalParameter_type);
      });
      _ut.test('test_visitTypeName_noParameters_noArguments', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitTypeName_noParameters_noArguments);
      });
      _ut.test('test_visitTypeName_parameters_arguments', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitTypeName_parameters_arguments);
      });
      _ut.test('test_visitTypeName_parameters_noArguments', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitTypeName_parameters_noArguments);
      });
      _ut.test('test_visitTypeName_void', () {
        final __test = new TypeResolverVisitorTest();
        runJUnitTest(__test, __test.test_visitTypeName_void);
      });
    });
  }
}
class ResolverTestCase extends EngineTestCase {

  /**
   * The source factory used to create [Source].
   */
  SourceFactory sourceFactory;

  /**
   * The analysis context used to parse the compilation units being resolved.
   */
  AnalysisContextImpl analysisContext;
  void setUp() {
    reset();
  }

  /**
   * Add a source file to the content provider.
   *
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  Source addSource(String contents) => addSource2("/test.dart", contents);

  /**
   * Add a source file to the content provider. The file path should be absolute.
   *
   * @param filePath the path of the file being added
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  Source addSource2(String filePath, String contents) {
    Source source = cacheSource(filePath, contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    analysisContext.applyChanges(changeSet);
    return source;
  }

  /**
   * Assert that the number of errors reported against the given source matches the number of errors
   * that are given and that they have the expected error codes. The order in which the errors were
   * gathered is ignored.
   *
   * @param source the source against which the errors should have been reported
   * @param expectedErrorCodes the error codes of the errors that should have been reported
   * @throws AnalysisException if the reported errors could not be computed
   * @throws AssertionFailedError if a different number of errors have been reported than were
   *           expected
   */
  void assertErrors(Source source, List<ErrorCode> expectedErrorCodes) {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    for (AnalysisError error in analysisContext.computeErrors(source)) {
      errorListener.onError(error);
    }
    errorListener.assertErrors2(expectedErrorCodes);
  }

  /**
   * Assert that no errors have been reported against the given source.
   *
   * @param source the source against which no errors should have been reported
   * @throws AnalysisException if the reported errors could not be computed
   * @throws AssertionFailedError if any errors have been reported
   */
  void assertNoErrors(Source source) {
    assertErrors(source, []);
  }

  /**
   * Cache the source file content in the source factory but don't add the source to the analysis
   * context. The file path should be absolute.
   *
   * @param filePath the path of the file being cached
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the cached file
   */
  Source cacheSource(String filePath, String contents) {
    Source source = new FileBasedSource.con1(sourceFactory.contentCache, FileUtilities2.createFile(filePath));
    sourceFactory.setContents(source, contents);
    return source;
  }

  /**
   * Create a library element that represents a library named `"test"` containing a single
   * empty compilation unit.
   *
   * @return the library element that was created
   */
  LibraryElementImpl createTestLibrary() => createTestLibrary2(new AnalysisContextImpl(), "test", []);

  /**
   * Create a library element that represents a library with the given name containing a single
   * empty compilation unit.
   *
   * @param libraryName the name of the library to be created
   * @return the library element that was created
   */
  LibraryElementImpl createTestLibrary2(AnalysisContext context, String libraryName, List<String> typeNames) {
    int count = typeNames.length;
    List<CompilationUnitElementImpl> sourcedCompilationUnits = new List<CompilationUnitElementImpl>(count);
    for (int i = 0; i < count; i++) {
      String typeName = typeNames[i];
      ClassElementImpl type = new ClassElementImpl(ASTFactory.identifier3(typeName));
      String fileName = "${typeName}.dart";
      CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
      compilationUnit.source = createSource2(fileName);
      compilationUnit.types = <ClassElement> [type];
      sourcedCompilationUnits[i] = compilationUnit;
    }
    String fileName = "${libraryName}.dart";
    CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
    compilationUnit.source = createSource2(fileName);
    LibraryElementImpl library = new LibraryElementImpl(context, ASTFactory.libraryIdentifier2([libraryName]));
    library.definingCompilationUnit = compilationUnit;
    library.parts = sourcedCompilationUnits;
    return library;
  }

  /**
   * Return a type provider that can be used to test the results of resolution.
   *
   * @return a type provider
   */
  TypeProvider get typeProvider {
    Source coreSource = analysisContext.sourceFactory.forUri(DartSdk.DART_CORE);
    LibraryElement coreElement = analysisContext.getLibraryElement(coreSource);
    return new TypeProviderImpl(coreElement);
  }

  /**
   * In the rare cases we want to group several tests into single "test_" method, so need a way to
   * reset test instance to reuse it.
   */
  void reset() {
    analysisContext = AnalysisContextFactory.contextWithCore();
    sourceFactory = analysisContext.sourceFactory;
  }

  /**
   * Given a library and all of its parts, resolve the contents of the library and the contents of
   * the parts. This assumes that the sources for the library and its parts have already been added
   * to the content provider using the method [addSource].
   *
   * @param librarySource the source for the compilation unit that defines the library
   * @return the element representing the resolved library
   * @throws AnalysisException if the analysis could not be performed
   */
  LibraryElement resolve(Source librarySource) => analysisContext.computeLibraryElement(librarySource);

  /**
   * Return the resolved compilation unit corresponding to the given source in the given library.
   *
   * @param source the source of the compilation unit to be returned
   * @param library the library in which the compilation unit is to be resolved
   * @return the resolved compilation unit
   * @throws Exception if the compilation unit could not be resolved
   */
  CompilationUnit resolveCompilationUnit(Source source, LibraryElement library) => analysisContext.resolveCompilationUnit(source, library);

  /**
   * Verify that all of the identifiers in the compilation units associated with the given sources
   * have been resolved.
   *
   * @param resolvedElementMap a table mapping the AST nodes that have been resolved to the element
   *          to which they were resolved
   * @param sources the sources identifying the compilation units to be verified
   * @throws Exception if the contents of the compilation unit cannot be accessed
   */
  void verify(List<Source> sources) {
    ResolutionVerifier verifier = new ResolutionVerifier();
    for (Source source in sources) {
      analysisContext.parseCompilationUnit(source).accept(verifier);
    }
    verifier.assertResolved();
  }

  /**
   * Create a source object representing a file with the given name and give it an empty content.
   *
   * @param fileName the name of the file for which a source is to be created
   * @return the source that was created
   */
  FileBasedSource createSource2(String fileName) {
    FileBasedSource source = new FileBasedSource.con1(sourceFactory.contentCache, FileUtilities2.createFile(fileName));
    sourceFactory.setContents(source, "");
    return source;
  }
  static dartSuite() {
    _ut.group('ResolverTestCase', () {
    });
  }
}
class TypeProviderImplTest extends EngineTestCase {
  void test_creation() {
    InterfaceType objectType = classElement("Object", null, []).type;
    InterfaceType boolType = classElement("bool", objectType, []).type;
    InterfaceType numType = classElement("num", objectType, []).type;
    InterfaceType doubleType = classElement("double", numType, []).type;
    InterfaceType functionType = classElement("Function", objectType, []).type;
    InterfaceType intType = classElement("int", numType, []).type;
    InterfaceType listType = classElement("List", objectType, ["E"]).type;
    InterfaceType mapType = classElement("Map", objectType, ["K", "V"]).type;
    InterfaceType stackTraceType = classElement("StackTrace", objectType, []).type;
    InterfaceType stringType = classElement("String", objectType, []).type;
    InterfaceType symbolType = classElement("Symbol", objectType, []).type;
    InterfaceType typeType = classElement("Type", objectType, []).type;
    CompilationUnitElementImpl unit = new CompilationUnitElementImpl("lib.dart");
    unit.types = <ClassElement> [
        boolType.element,
        doubleType.element,
        functionType.element,
        intType.element,
        listType.element,
        mapType.element,
        objectType.element,
        stackTraceType.element,
        stringType.element,
        symbolType.element,
        typeType.element];
    LibraryElementImpl library = new LibraryElementImpl(new AnalysisContextImpl(), ASTFactory.libraryIdentifier2(["lib"]));
    library.definingCompilationUnit = unit;
    TypeProviderImpl provider = new TypeProviderImpl(library);
    JUnitTestCase.assertSame(boolType, provider.boolType);
    JUnitTestCase.assertNotNull(provider.bottomType);
    JUnitTestCase.assertSame(doubleType, provider.doubleType);
    JUnitTestCase.assertNotNull(provider.dynamicType);
    JUnitTestCase.assertSame(functionType, provider.functionType);
    JUnitTestCase.assertSame(intType, provider.intType);
    JUnitTestCase.assertSame(listType, provider.listType);
    JUnitTestCase.assertSame(mapType, provider.mapType);
    JUnitTestCase.assertSame(objectType, provider.objectType);
    JUnitTestCase.assertSame(stackTraceType, provider.stackTraceType);
    JUnitTestCase.assertSame(stringType, provider.stringType);
    JUnitTestCase.assertSame(symbolType, provider.symbolType);
    JUnitTestCase.assertSame(typeType, provider.typeType);
  }
  ClassElement classElement(String typeName, InterfaceType superclassType, List<String> parameterNames) {
    ClassElementImpl element = new ClassElementImpl(ASTFactory.identifier3(typeName));
    element.supertype = superclassType;
    InterfaceTypeImpl type = new InterfaceTypeImpl.con1(element);
    element.type = type;
    int count = parameterNames.length;
    if (count > 0) {
      List<TypeParameterElementImpl> typeParameters = new List<TypeParameterElementImpl>(count);
      List<TypeParameterTypeImpl> typeArguments = new List<TypeParameterTypeImpl>(count);
      for (int i = 0; i < count; i++) {
        TypeParameterElementImpl typeParameter = new TypeParameterElementImpl(ASTFactory.identifier3(parameterNames[i]));
        typeParameters[i] = typeParameter;
        typeArguments[i] = new TypeParameterTypeImpl(typeParameter);
        typeParameter.type = typeArguments[i];
      }
      element.typeParameters = typeParameters;
      type.typeArguments = typeArguments;
    }
    return element;
  }
  static dartSuite() {
    _ut.group('TypeProviderImplTest', () {
      _ut.test('test_creation', () {
        final __test = new TypeProviderImplTest();
        runJUnitTest(__test, __test.test_creation);
      });
    });
  }
}
class InheritanceManagerTest extends EngineTestCase {

  /**
   * The type provider used to access the types.
   */
  TestTypeProvider _typeProvider;

  /**
   * The library containing the code being resolved.
   */
  LibraryElementImpl _definingLibrary;

  /**
   * The inheritance manager being tested.
   */
  InheritanceManager _inheritanceManager;

  /**
   * The number of members that Object implements (as determined by [TestTypeProvider]).
   */
  int _numOfMembersInObject = 0;
  void setUp() {
    _typeProvider = new TestTypeProvider();
    _inheritanceManager = createInheritanceManager();
    InterfaceType objectType = _typeProvider.objectType;
    _numOfMembersInObject = objectType.methods.length + objectType.accessors.length;
  }
  void test_getMapOfMembersInheritedFromClasses_accessor_extends() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromClasses(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromClasses_accessor_implements() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromClasses(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapB.size);
    JUnitTestCase.assertNull(mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromClasses_accessor_with() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromClasses(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromClasses_method_extends() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.supertype = classA.type;
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromClasses(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromClasses_method_implements() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromClasses(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapB.size);
    JUnitTestCase.assertNull(mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromClasses_method_with() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromClasses(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromInterfaces_accessor_extends() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromInterfaces_accessor_implements() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromInterfaces_accessor_with() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromInterfaces_method_extends() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromInterfaces_method_implements() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_getMapOfMembersInheritedFromInterfaces_method_with() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    MemberMap mapB = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = _inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    JUnitTestCase.assertEquals(_numOfMembersInObject, mapA.size);
    JUnitTestCase.assertEquals(_numOfMembersInObject + 1, mapB.size);
    JUnitTestCase.assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_interface_getter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    JUnitTestCase.assertSame(getterG, _inheritanceManager.lookupInheritance(classB, getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_interface_method() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    JUnitTestCase.assertSame(methodM, _inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_interface_setter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String setterName = "s";
    PropertyAccessorElement setterS = ElementFactory.setterElement(setterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [setterS];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    JUnitTestCase.assertSame(setterS, _inheritanceManager.lookupInheritance(classB, "${setterName}="));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_interface_staticMember() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    ((methodM as MethodElementImpl)).static = true;
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_interfaces_infiniteLoop() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    classA.interfaces = <InterfaceType> [classA.type];
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
  }
  void test_lookupInheritance_interfaces_infiniteLoop2() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classA.interfaces = <InterfaceType> [classB.type];
    classB.interfaces = <InterfaceType> [classA.type];
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_interfaces_STWC_inconsistentMethodInheritance() {
    ClassElementImpl classI1 = ElementFactory.classElement2("I1", []);
    String methodName = "m";
    MethodElement methodM1 = ElementFactory.methodElement(methodName, null, [_typeProvider.intType]);
    classI1.methods = <MethodElement> [methodM1];
    ClassElementImpl classI2 = ElementFactory.classElement2("I2", []);
    MethodElement methodM2 = ElementFactory.methodElement(methodName, null, [_typeProvider.stringType]);
    classI2.methods = <MethodElement> [methodM2];
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    classA.interfaces = <InterfaceType> [classI1.type, classI2.type];
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classA, methodName));
    assertNoErrors(classI1);
    assertNoErrors(classI2);
    assertErrors(classA, [StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE]);
  }
  void test_lookupInheritance_interfaces_SWC_inconsistentMethodInheritance() {
    ClassElementImpl classI1 = ElementFactory.classElement2("I1", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classI1.methods = <MethodElement> [methodM];
    ClassElementImpl classI2 = ElementFactory.classElement2("I2", []);
    PropertyAccessorElement getter = ElementFactory.getterElement(methodName, false, _typeProvider.intType);
    classI2.accessors = <PropertyAccessorElement> [getter];
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    classA.interfaces = <InterfaceType> [classI1.type, classI2.type];
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classA, methodName));
    assertNoErrors(classI1);
    assertNoErrors(classI2);
    assertErrors(classA, [StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD]);
  }
  void test_lookupInheritance_interfaces_union1() {
    ClassElementImpl classI1 = ElementFactory.classElement2("I1", []);
    String methodName1 = "m1";
    MethodElement methodM1 = ElementFactory.methodElement(methodName1, _typeProvider.intType, []);
    classI1.methods = <MethodElement> [methodM1];
    ClassElementImpl classI2 = ElementFactory.classElement2("I2", []);
    String methodName2 = "m2";
    MethodElement methodM2 = ElementFactory.methodElement(methodName2, _typeProvider.intType, []);
    classI2.methods = <MethodElement> [methodM2];
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    classA.interfaces = <InterfaceType> [classI1.type, classI2.type];
    JUnitTestCase.assertSame(methodM1, _inheritanceManager.lookupInheritance(classA, methodName1));
    JUnitTestCase.assertSame(methodM2, _inheritanceManager.lookupInheritance(classA, methodName2));
    assertNoErrors(classI1);
    assertNoErrors(classI2);
    assertNoErrors(classA);
  }
  void test_lookupInheritance_interfaces_union2() {
    ClassElementImpl classI1 = ElementFactory.classElement2("I1", []);
    String methodName1 = "m1";
    MethodElement methodM1 = ElementFactory.methodElement(methodName1, _typeProvider.intType, []);
    classI1.methods = <MethodElement> [methodM1];
    ClassElementImpl classI2 = ElementFactory.classElement2("I2", []);
    String methodName2 = "m2";
    MethodElement methodM2 = ElementFactory.methodElement(methodName2, _typeProvider.intType, []);
    classI2.methods = <MethodElement> [methodM2];
    classI2.interfaces = <InterfaceType> [classI1.type];
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    classA.interfaces = <InterfaceType> [classI2.type];
    JUnitTestCase.assertSame(methodM1, _inheritanceManager.lookupInheritance(classA, methodName1));
    JUnitTestCase.assertSame(methodM2, _inheritanceManager.lookupInheritance(classA, methodName2));
    assertNoErrors(classI1);
    assertNoErrors(classI2);
    assertNoErrors(classA);
  }
  void test_lookupInheritance_mixin_getter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    JUnitTestCase.assertSame(getterG, _inheritanceManager.lookupInheritance(classB, getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_mixin_method() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    JUnitTestCase.assertSame(methodM, _inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_mixin_setter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String setterName = "s";
    PropertyAccessorElement setterS = ElementFactory.setterElement(setterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [setterS];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    JUnitTestCase.assertSame(setterS, _inheritanceManager.lookupInheritance(classB, "${setterName}="));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_mixin_staticMember() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    ((methodM as MethodElementImpl)).static = true;
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.mixins = <InterfaceType> [classA.type];
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_noMember() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classA, "a"));
    assertNoErrors(classA);
  }
  void test_lookupInheritance_superclass_getter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    JUnitTestCase.assertSame(getterG, _inheritanceManager.lookupInheritance(classB, getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_superclass_infiniteLoop() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    classA.supertype = classA.type;
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
  }
  void test_lookupInheritance_superclass_infiniteLoop2() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classA.supertype = classB.type;
    classB.supertype = classA.type;
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_superclass_method() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    JUnitTestCase.assertSame(methodM, _inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_superclass_setter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String setterName = "s";
    PropertyAccessorElement setterS = ElementFactory.setterElement(setterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [setterS];
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    JUnitTestCase.assertSame(setterS, _inheritanceManager.lookupInheritance(classB, "${setterName}="));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupInheritance_superclass_staticMember() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    ((methodM as MethodElementImpl)).static = true;
    classA.methods = <MethodElement> [methodM];
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    JUnitTestCase.assertNull(_inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }
  void test_lookupMember_getter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    JUnitTestCase.assertSame(getterG, _inheritanceManager.lookupMember(classA, getterName));
    assertNoErrors(classA);
  }
  void test_lookupMember_getter_static() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "g";
    PropertyAccessorElement getterG = ElementFactory.getterElement(getterName, true, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getterG];
    JUnitTestCase.assertNull(_inheritanceManager.lookupMember(classA, getterName));
    assertNoErrors(classA);
  }
  void test_lookupMember_method() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    classA.methods = <MethodElement> [methodM];
    JUnitTestCase.assertSame(methodM, _inheritanceManager.lookupMember(classA, methodName));
    assertNoErrors(classA);
  }
  void test_lookupMember_method_static() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    MethodElement methodM = ElementFactory.methodElement(methodName, _typeProvider.intType, []);
    ((methodM as MethodElementImpl)).static = true;
    classA.methods = <MethodElement> [methodM];
    JUnitTestCase.assertNull(_inheritanceManager.lookupMember(classA, methodName));
    assertNoErrors(classA);
  }
  void test_lookupMember_noMember() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    JUnitTestCase.assertNull(_inheritanceManager.lookupMember(classA, "a"));
    assertNoErrors(classA);
  }
  void test_lookupMember_setter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String setterName = "s";
    PropertyAccessorElement setterS = ElementFactory.setterElement(setterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [setterS];
    JUnitTestCase.assertSame(setterS, _inheritanceManager.lookupMember(classA, "${setterName}="));
    assertNoErrors(classA);
  }
  void test_lookupMember_setter_static() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String setterName = "s";
    PropertyAccessorElement setterS = ElementFactory.setterElement(setterName, true, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [setterS];
    JUnitTestCase.assertNull(_inheritanceManager.lookupMember(classA, setterName));
    assertNoErrors(classA);
  }
  void assertErrors(ClassElement classElt, List<ErrorCode> expectedErrorCodes) {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Set<AnalysisError> actualErrors = _inheritanceManager.getErrors(classElt);
    if (actualErrors != null) {
      for (AnalysisError error in actualErrors) {
        errorListener.onError(error);
      }
    }
    errorListener.assertErrors2(expectedErrorCodes);
  }
  void assertNoErrors(ClassElement classElt) {
    assertErrors(classElt, []);
  }

  /**
   * Create the inheritance manager used by the tests.
   *
   * @return the inheritance manager that was created
   */
  InheritanceManager createInheritanceManager() {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    FileBasedSource source = new FileBasedSource.con1(new ContentCache(), FileUtilities2.createFile("/test.dart"));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("test.dart");
    definingCompilationUnit.source = source;
    _definingLibrary = ElementFactory.library(context, "test");
    _definingLibrary.definingCompilationUnit = definingCompilationUnit;
    return new InheritanceManager(_definingLibrary);
  }
  static dartSuite() {
    _ut.group('InheritanceManagerTest', () {
      _ut.test('test_getMapOfMembersInheritedFromClasses_accessor_extends', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromClasses_accessor_extends);
      });
      _ut.test('test_getMapOfMembersInheritedFromClasses_accessor_implements', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromClasses_accessor_implements);
      });
      _ut.test('test_getMapOfMembersInheritedFromClasses_accessor_with', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromClasses_accessor_with);
      });
      _ut.test('test_getMapOfMembersInheritedFromClasses_method_extends', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromClasses_method_extends);
      });
      _ut.test('test_getMapOfMembersInheritedFromClasses_method_implements', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromClasses_method_implements);
      });
      _ut.test('test_getMapOfMembersInheritedFromClasses_method_with', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromClasses_method_with);
      });
      _ut.test('test_getMapOfMembersInheritedFromInterfaces_accessor_extends', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromInterfaces_accessor_extends);
      });
      _ut.test('test_getMapOfMembersInheritedFromInterfaces_accessor_implements', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromInterfaces_accessor_implements);
      });
      _ut.test('test_getMapOfMembersInheritedFromInterfaces_accessor_with', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromInterfaces_accessor_with);
      });
      _ut.test('test_getMapOfMembersInheritedFromInterfaces_method_extends', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromInterfaces_method_extends);
      });
      _ut.test('test_getMapOfMembersInheritedFromInterfaces_method_implements', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromInterfaces_method_implements);
      });
      _ut.test('test_getMapOfMembersInheritedFromInterfaces_method_with', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_getMapOfMembersInheritedFromInterfaces_method_with);
      });
      _ut.test('test_lookupInheritance_interface_getter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interface_getter);
      });
      _ut.test('test_lookupInheritance_interface_method', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interface_method);
      });
      _ut.test('test_lookupInheritance_interface_setter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interface_setter);
      });
      _ut.test('test_lookupInheritance_interface_staticMember', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interface_staticMember);
      });
      _ut.test('test_lookupInheritance_interfaces_STWC_inconsistentMethodInheritance', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interfaces_STWC_inconsistentMethodInheritance);
      });
      _ut.test('test_lookupInheritance_interfaces_SWC_inconsistentMethodInheritance', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interfaces_SWC_inconsistentMethodInheritance);
      });
      _ut.test('test_lookupInheritance_interfaces_infiniteLoop', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interfaces_infiniteLoop);
      });
      _ut.test('test_lookupInheritance_interfaces_infiniteLoop2', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interfaces_infiniteLoop2);
      });
      _ut.test('test_lookupInheritance_interfaces_union1', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interfaces_union1);
      });
      _ut.test('test_lookupInheritance_interfaces_union2', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_interfaces_union2);
      });
      _ut.test('test_lookupInheritance_mixin_getter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_mixin_getter);
      });
      _ut.test('test_lookupInheritance_mixin_method', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_mixin_method);
      });
      _ut.test('test_lookupInheritance_mixin_setter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_mixin_setter);
      });
      _ut.test('test_lookupInheritance_mixin_staticMember', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_mixin_staticMember);
      });
      _ut.test('test_lookupInheritance_noMember', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_noMember);
      });
      _ut.test('test_lookupInheritance_superclass_getter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_superclass_getter);
      });
      _ut.test('test_lookupInheritance_superclass_infiniteLoop', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_superclass_infiniteLoop);
      });
      _ut.test('test_lookupInheritance_superclass_infiniteLoop2', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_superclass_infiniteLoop2);
      });
      _ut.test('test_lookupInheritance_superclass_method', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_superclass_method);
      });
      _ut.test('test_lookupInheritance_superclass_setter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_superclass_setter);
      });
      _ut.test('test_lookupInheritance_superclass_staticMember', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupInheritance_superclass_staticMember);
      });
      _ut.test('test_lookupMember_getter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupMember_getter);
      });
      _ut.test('test_lookupMember_getter_static', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupMember_getter_static);
      });
      _ut.test('test_lookupMember_method', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupMember_method);
      });
      _ut.test('test_lookupMember_method_static', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupMember_method_static);
      });
      _ut.test('test_lookupMember_noMember', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupMember_noMember);
      });
      _ut.test('test_lookupMember_setter', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupMember_setter);
      });
      _ut.test('test_lookupMember_setter_static', () {
        final __test = new InheritanceManagerTest();
        runJUnitTest(__test, __test.test_lookupMember_setter_static);
      });
    });
  }
}
class CompileTimeErrorCodeTest extends ResolverTestCase {
  void fail_compileTimeConstantRaisesException() {
    Source source = addSource(EngineTestCase.createSource([]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.COMPILE_TIME_CONSTANT_RAISES_EXCEPTION]);
    verify([source]);
  }
  void fail_constEvalThrowsException() {
    Source source = addSource(EngineTestCase.createSource([
        "class C {",
        "  const C();",
        "}",
        "f() { return const C(); }"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_CONSTRUCTOR_THROWS_EXCEPTION]);
    verify([source]);
  }
  void fail_mixinDeclaresConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "class B extends Object mixin A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR]);
    verify([source]);
  }
  void fail_mixinOfNonClass() {
    Source source = addSource(EngineTestCase.createSource(["var A;", "class B extends Object mixin A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_NON_CLASS]);
    verify([source]);
  }
  void fail_objectCannotExtendAnotherClass() {
    Source source = addSource(EngineTestCase.createSource([]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.OBJECT_CANNOT_EXTEND_ANOTHER_CLASS]);
    verify([source]);
  }
  void fail_recursiveCompileTimeConstant() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "  final m = const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_COMPILE_TIME_CONSTANT]);
    verify([source]);
  }
  void fail_recursiveCompileTimeConstant_cycle() {
    Source source = addSource(EngineTestCase.createSource(["const x = y + 1;", "const y = x + 1;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_COMPILE_TIME_CONSTANT]);
    verify([source]);
  }
  void fail_superInitializerInObject() {
    Source source = addSource(EngineTestCase.createSource([]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_INITIALIZER_IN_OBJECT]);
    verify([source]);
  }
  void test_ambiguousExport() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.AMBIGUOUS_EXPORT]);
    verify([source]);
  }
  void test_ambiguousImport_function() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "g() { return f(); }"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "f() {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "f() {}"]));
    resolve(source);
    assertErrors(source, [
        StaticWarningCode.AMBIGUOUS_IMPORT,
        CompileTimeErrorCode.UNDEFINED_FUNCTION]);
  }
  void test_argumentDefinitionTestNonParameter() {
    Source source = addSource(EngineTestCase.createSource(["f() {", " var v = 0;", " return ?v;", "}"]));
    resolve(source);
    assertErrors(source, [
        ParserErrorCode.DEPRECATED_ARGUMENT_DEFINITION_TEST,
        CompileTimeErrorCode.ARGUMENT_DEFINITION_TEST_NON_PARAMETER]);
    verify([source]);
  }
  void test_builtInIdentifierAsMixinName_classTypeAlias() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "class B {}", "class as = A with B;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME]);
    verify([source]);
  }
  void test_builtInIdentifierAsType_formalParameter_field() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  var x;", "  A(static this.x);", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE]);
    verify([source]);
  }
  void test_builtInIdentifierAsType_formalParameter_simple() {
    Source source = addSource(EngineTestCase.createSource(["f(static x) {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE]);
    verify([source]);
  }
  void test_builtInIdentifierAsType_variableDeclaration() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  typedef x;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE]);
    verify([source]);
  }
  void test_builtInIdentifierAsTypedefName_functionTypeAlias() {
    Source source = addSource(EngineTestCase.createSource(["typedef bool as();"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME]);
    verify([source]);
  }
  void test_builtInIdentifierAsTypeName() {
    Source source = addSource(EngineTestCase.createSource(["class as {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME]);
    verify([source]);
  }
  void test_builtInIdentifierAsTypeParameterName() {
    Source source = addSource(EngineTestCase.createSource(["class A<as> {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_PARAMETER_NAME]);
    verify([source]);
  }
  void test_caseExpressionTypeImplementsEquals() {
    Source source = addSource(EngineTestCase.createSource([
        "class IntWrapper {",
        "  final int value;",
        "  const IntWrapper(this.value);",
        "  bool operator ==(IntWrapper x) {",
        "    return value == x.value;",
        "  }",
        "  get hashCode => value;",
        "}",
        "",
        "f(var a) {",
        "  switch(a) {",
        "    case(const IntWrapper(1)) : return 1;",
        "    default: return 0;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS]);
    verify([source]);
  }
  void test_conflictingConstructorNameAndMember_field() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A.x() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_FIELD]);
    verify([source]);
  }
  void test_conflictingConstructorNameAndMember_method() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  const A.x();", "  void x() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_METHOD]);
    verify([source]);
  }
  void test_conflictingGetterAndMethod_field_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int m;",
        "}",
        "class B extends A {",
        "  m() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_GETTER_AND_METHOD]);
    verify([source]);
  }
  void test_conflictingGetterAndMethod_getter_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  get m => 0;",
        "}",
        "class B extends A {",
        "  m() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_GETTER_AND_METHOD]);
    verify([source]);
  }
  void test_conflictingGetterAndMethod_method_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  int m;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_METHOD_AND_GETTER]);
    verify([source]);
  }
  void test_conflictingGetterAndMethod_method_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  get m => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_METHOD_AND_GETTER]);
    verify([source]);
  }
  void test_conflictingTypeVariableAndClass() {
    Source source = addSource(EngineTestCase.createSource(["class T<T> {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_CLASS]);
    verify([source]);
  }
  void test_conflictingTypeVariableAndMember_field() {
    Source source = addSource(EngineTestCase.createSource(["class A<T> {", "  var T;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER]);
    verify([source]);
  }
  void test_conflictingTypeVariableAndMember_getter() {
    Source source = addSource(EngineTestCase.createSource(["class A<T> {", "  get T => null;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER]);
    verify([source]);
  }
  void test_conflictingTypeVariableAndMember_method() {
    Source source = addSource(EngineTestCase.createSource(["class A<T> {", "  T() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER]);
    verify([source]);
  }
  void test_conflictingTypeVariableAndMember_method_static() {
    Source source = addSource(EngineTestCase.createSource(["class A<T> {", "  static T() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER]);
    verify([source]);
  }
  void test_conflictingTypeVariableAndMember_setter() {
    Source source = addSource(EngineTestCase.createSource(["class A<T> {", "  set T(x) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER]);
    verify([source]);
  }
  void test_constConstructorWithNonConstSuper_explicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A();",
        "}",
        "class B extends A {",
        "  const B(): super();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER]);
    verify([source]);
  }
  void test_constConstructorWithNonConstSuper_implicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A();",
        "}",
        "class B extends A {",
        "  const B();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER]);
    verify([source]);
  }
  void test_constConstructorWithNonFinalField_mixin() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var a;",
        "}",
        "class B extends Object with A {",
        "  const B();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD]);
    verify([source]);
  }
  void test_constConstructorWithNonFinalField_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var a;",
        "}",
        "class B extends A {",
        "  const B();",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD,
        CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER]);
    verify([source]);
  }
  void test_constConstructorWithNonFinalField_this() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  const A();", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD]);
    verify([source]);
  }
  void test_constEval_newInstance_constConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  const A();", "}", "const a = new A();"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE]);
    verify([source]);
  }
  void test_constEval_propertyExtraction_targetNotConst() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "  m() {}",
        "}",
        "final a = const A();",
        "const C = a.m;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE]);
    verify([source]);
  }
  void test_constEvalThrowsException_binaryMinus_null() {
    check_constEvalThrowsException_binary_null("null - 5", false);
    check_constEvalThrowsException_binary_null("5 - null", true);
  }
  void test_constEvalThrowsException_binaryPlus_null() {
    check_constEvalThrowsException_binary_null("null + 5", false);
    check_constEvalThrowsException_binary_null("5 + null", true);
  }
  void test_constEvalThrowsException_divisionByZero() {
    Source source = addSource("const C = 1 ~/ 0;");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_THROWS_IDBZE]);
    verify([source]);
  }
  void test_constEvalThrowsException_unaryBitNot_null() {
    Source source = addSource("const C = ~null;");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION]);
  }
  void test_constEvalThrowsException_unaryNegated_null() {
    Source source = addSource("const C = -null;");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION]);
  }
  void test_constEvalThrowsException_unaryNot_null() {
    Source source = addSource("const C = !null;");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION]);
    verify([source]);
  }
  void test_constEvalTypeBool_binary() {
    check_constEvalTypeBool_withParameter_binary("p && ''");
    check_constEvalTypeBool_withParameter_binary("p || ''");
  }
  void test_constEvalTypeBool_binary_leftTrue() {
    Source source = addSource("const C = (true || 0);");
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL,
        HintCode.DEAD_CODE]);
    verify([source]);
  }
  void test_constEvalTypeBoolNumString_equal() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "class B {",
        "  final a;",
        "  const B(num p) : a = p == const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING]);
    verify([source]);
  }
  void test_constEvalTypeBoolNumString_notEqual() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "class B {",
        "  final a;",
        "  const B(String p) : a = p != const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING]);
    verify([source]);
  }
  void test_constEvalTypeInt_binary() {
    check_constEvalTypeInt_withParameter_binary("p ^ ''");
    check_constEvalTypeInt_withParameter_binary("p & ''");
    check_constEvalTypeInt_withParameter_binary("p | ''");
    check_constEvalTypeInt_withParameter_binary("p >> ''");
    check_constEvalTypeInt_withParameter_binary("p << ''");
  }
  void test_constEvalTypeNum_binary() {
    check_constEvalTypeNum_withParameter_binary("p + ''");
    check_constEvalTypeNum_withParameter_binary("p - ''");
    check_constEvalTypeNum_withParameter_binary("p * ''");
    check_constEvalTypeNum_withParameter_binary("p / ''");
    check_constEvalTypeNum_withParameter_binary("p ~/ ''");
    check_constEvalTypeNum_withParameter_binary("p > ''");
    check_constEvalTypeNum_withParameter_binary("p < ''");
    check_constEvalTypeNum_withParameter_binary("p >= ''");
    check_constEvalTypeNum_withParameter_binary("p <= ''");
    check_constEvalTypeNum_withParameter_binary("p % ''");
  }
  void test_constEvalTypeNum_plus_String() {
    Source source = addSource("const C = 'a' + 'b';");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_TYPE_NUM]);
    verify([source]);
  }
  void test_constFormalParameter_fieldFormalParameter() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  var x;", "  A(const this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_FORMAL_PARAMETER]);
    verify([source]);
  }
  void test_constFormalParameter_simpleFormalParameter() {
    Source source = addSource(EngineTestCase.createSource(["f(const x) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_FORMAL_PARAMETER]);
    verify([source]);
  }
  void test_constInitializedWithNonConstValue() {
    Source source = addSource(EngineTestCase.createSource(["f(p) {", "  const C = p;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE]);
    verify([source]);
  }
  void test_constInitializedWithNonConstValue_missingConstInListLiteral() {
    Source source = addSource("const List L = [0];");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE]);
    verify([source]);
  }
  void test_constInitializedWithNonConstValue_missingConstInMapLiteral() {
    Source source = addSource("const Map M = {'a' : 0};");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE]);
    verify([source]);
  }
  void test_constInstanceField() {
    Source source = addSource(EngineTestCase.createSource(["class C {", "  const int f = 0;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_INSTANCE_FIELD]);
    verify([source]);
  }
  void test_constMapKeyTypeImplementsEquals_direct() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "  operator ==(other) => false;",
        "}",
        "main() {",
        "  const {const A() : 0};",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS]);
    verify([source]);
  }
  void test_constMapKeyTypeImplementsEquals_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "  operator ==(other) => false;",
        "}",
        "class B extends A {",
        "  const B();",
        "}",
        "main() {",
        "  const {const B() : 0};",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS]);
    verify([source]);
  }
  void test_constWithInvalidTypeParameters() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "f() { return const A<A>(); }"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS]);
    verify([source]);
  }
  void test_constWithInvalidTypeParameters_tooFew() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class C<K, V> {",
        "  const C();",
        "}",
        "f(p) {",
        "  return const C<A>();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS]);
    verify([source]);
  }
  void test_constWithInvalidTypeParameters_tooMany() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class C<E> {",
        "  const C();",
        "}",
        "f(p) {",
        "  return const C<A, A>();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS]);
    verify([source]);
  }
  void test_constWithNonConst() {
    Source source = addSource(EngineTestCase.createSource([
        "class T {",
        "  T(a, b, {c, d}) {}",
        "}",
        "f() { return const T(0, 1, c: 2, d: 3); }"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_NON_CONST]);
    verify([source]);
  }
  void test_constWithNonConstantArgument_annotation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(int p);",
        "}",
        "var v = 42;",
        "@A(v)",
        "main() {",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_NON_CONSTANT_ARGUMENT]);
    verify([source]);
  }
  void test_constWithNonConstantArgument_instanceCreation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(a);",
        "}",
        "f(p) { return const A(p); }"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_NON_CONSTANT_ARGUMENT]);
    verify([source]);
  }
  void test_constWithNonType() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "f() {", "  return const A();", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_NON_TYPE]);
    verify([source]);
  }
  void test_constWithNonType_fromLibrary() {
    Source source1 = addSource2("lib.dart", "");
    Source source2 = addSource2("lib2.dart", EngineTestCase.createSource([
        "import 'lib.dart' as lib;",
        "void f() {",
        "  const lib.A();",
        "}"]));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, [CompileTimeErrorCode.CONST_WITH_NON_TYPE]);
    verify([source1]);
  }
  void test_constWithTypeParameters_direct() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> {",
        "  static const V = const A<T>();",
        "  const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_WITH_TYPE_PARAMETERS,
        StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_constWithTypeParameters_indirect() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> {",
        "  static const V = const A<List<T>>();",
        "  const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_WITH_TYPE_PARAMETERS,
        StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_constWithUndefinedConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "f() {",
        "  return const A.noSuchConstructor();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR]);
  }
  void test_constWithUndefinedConstructorDefault() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A.name();",
        "}",
        "f() {",
        "  return const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT]);
    verify([source]);
  }
  void test_defaultValueInFunctionTypeAlias() {
    Source source = addSource(EngineTestCase.createSource(["typedef F([x = 0]);"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPE_ALIAS]);
    verify([source]);
  }
  void test_defaultValueInFunctionTypedParameter_named() {
    Source source = addSource(EngineTestCase.createSource(["f(g({p: null})) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPED_PARAMETER]);
    verify([source]);
  }
  void test_defaultValueInFunctionTypedParameter_optional() {
    Source source = addSource(EngineTestCase.createSource(["f(g([p = null])) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPED_PARAMETER]);
    verify([source]);
  }
  void test_defaultValueInRedirectingFactoryConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  factory A([int x = 0]) = B;",
        "}",
        "",
        "class B implements A {",
        "  B([int x = 1]) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DEFAULT_VALUE_IN_REDIRECTING_FACTORY_CONSTRUCTOR]);
    verify([source]);
  }
  void test_duplicateConstructorName_named() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A.a() {}", "  A.a() {}", "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_NAME,
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_NAME]);
    verify([source]);
  }
  void test_duplicateConstructorName_unnamed() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() {}", "  A() {}", "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_DEFAULT,
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_DEFAULT]);
    verify([source]);
  }
  void test_duplicateDefinition() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  int m = 0;", "  m(a) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([source]);
  }
  void test_duplicateDefinition_acrossLibraries() {
    Source librarySource = addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "", "part 'a.dart';", "part 'b.dart';"]));
    Source sourceA = addSource2("/a.dart", EngineTestCase.createSource(["part of lib;", "", "class A {}"]));
    Source sourceB = addSource2("/b.dart", EngineTestCase.createSource(["part of lib;", "", "class A {}"]));
    resolve(librarySource);
    assertErrors(sourceB, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([librarySource, sourceA, sourceB]);
  }
  void test_duplicateDefinition_classMembers_fields() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int a;", "  int a;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([source]);
  }
  void test_duplicateDefinition_classMembers_fields_oneStatic() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  static int x;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([source]);
  }
  void test_duplicateDefinition_classMembers_methods() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  m() {}", "  m() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([source]);
  }
  void test_duplicateDefinition_localFields() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {",
        "    int a;",
        "    int a;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([source]);
  }
  void test_duplicateDefinition_parameterWithFunctionName_local() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  f(f) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([source]);
  }
  void test_duplicateDefinition_parameterWithFunctionName_topLevel() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  f(f) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION]);
    verify([source]);
  }
  void test_duplicateDefinitionInheritance_instanceGetter_staticGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get x => 0;",
        "}",
        "class B extends A {",
        "  static int get x => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE]);
    verify([source]);
  }
  void test_duplicateDefinitionInheritance_instanceGetterAbstract_staticGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int get x;",
        "}",
        "class B extends A {",
        "  static int get x => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE]);
    verify([source]);
  }
  void test_duplicateDefinitionInheritance_instanceMethod_staticMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  x() {}",
        "}",
        "class B extends A {",
        "  static x() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE]);
    verify([source]);
  }
  void test_duplicateDefinitionInheritance_instanceMethodAbstract_staticMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  x();",
        "}",
        "abstract class B extends A {",
        "  static x() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE]);
    verify([source]);
  }
  void test_duplicateDefinitionInheritance_instanceSetter_staticSetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  set x(value) {}",
        "}",
        "class B extends A {",
        "  static set x(value) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE]);
    verify([source]);
  }
  void test_duplicateDefinitionInheritance_instanceSetterAbstract_staticSetter() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  set x(value);",
        "}",
        "class B extends A {",
        "  static set x(value) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE]);
    verify([source]);
  }
  void test_duplicateNamedArgument() {
    Source source = addSource(EngineTestCase.createSource(["f({a, b}) {}", "main() {", "  f(a: 1, a: 2);", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.DUPLICATE_NAMED_ARGUMENT]);
    verify([source]);
  }
  void test_exportInternalLibrary() {
    Source source = addSource(EngineTestCase.createSource(["export 'dart:_interceptors';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.EXPORT_INTERNAL_LIBRARY]);
    verify([source]);
  }
  void test_exportOfNonLibrary() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "export 'lib1.dart';"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["part of lib;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.EXPORT_OF_NON_LIBRARY]);
    verify([source]);
  }
  void test_extendsDisallowedClass_bool() {
    Source source = addSource(EngineTestCase.createSource(["class A extends bool {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_extendsDisallowedClass_double() {
    Source source = addSource(EngineTestCase.createSource(["class A extends double {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_extendsDisallowedClass_int() {
    Source source = addSource(EngineTestCase.createSource(["class A extends int {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_extendsDisallowedClass_Null() {
    Source source = addSource(EngineTestCase.createSource(["class A extends Null {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_extendsDisallowedClass_num() {
    Source source = addSource(EngineTestCase.createSource(["class A extends num {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_extendsDisallowedClass_String() {
    Source source = addSource(EngineTestCase.createSource(["class A extends String {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_extendsNonClass_class() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "class B extends A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.EXTENDS_NON_CLASS]);
    verify([source]);
  }
  void test_extendsNonClass_dynamic() {
    Source source = addSource(EngineTestCase.createSource(["class B extends dynamic {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.EXTENDS_NON_CLASS]);
    verify([source]);
  }
  void test_extraPositionalArguments_const() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "main() {",
        "  const A(0);",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.EXTRA_POSITIONAL_ARGUMENTS]);
    verify([source]);
  }
  void test_extraPositionalArguments_const_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "class B extends A {",
        "  const B() : super(0);",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.EXTRA_POSITIONAL_ARGUMENTS]);
    verify([source]);
  }
  void test_fieldInitializedByMultipleInitializers() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A() : x = 0, x = 1 {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS]);
    verify([source]);
  }
  void test_fieldInitializedByMultipleInitializers_multipleInits() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int x;",
        "  A() : x = 0, x = 1, x = 2 {}",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS]);
    verify([source]);
  }
  void test_fieldInitializedByMultipleInitializers_multipleNames() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int x;",
        "  int y;",
        "  A() : x = 0, x = 1, y = 0, y = 1 {}",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS]);
    verify([source]);
  }
  void test_fieldInitializedInParameterAndInitializer() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A(this.x) : x = 1 {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER]);
    verify([source]);
  }
  void test_fieldInitializerFactoryConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  factory A(this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZER_FACTORY_CONSTRUCTOR]);
    verify([source]);
  }
  void test_fieldInitializerNotAssignable() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final int x;",
        "  const A() : x = '';",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_fieldInitializerOutsideConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  m(this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [
        ParserErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR,
        CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR]);
    verify([source]);
  }
  void test_fieldInitializerOutsideConstructor_defaultParameter() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  m([this.x]) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR]);
    verify([source]);
  }
  void test_fieldInitializerRedirectingConstructor_afterRedirection() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int x;",
        "  A.named() {}",
        "  A() : this.named(), x = 42;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR]);
    verify([source]);
  }
  void test_fieldInitializerRedirectingConstructor_beforeRedirection() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int x;",
        "  A.named() {}",
        "  A() : x = 42, this.named();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR]);
    verify([source]);
  }
  void test_fieldInitializingFormalRedirectingConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int x;",
        "  A.named() {}",
        "  A(this.x) : this.named();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR]);
    verify([source]);
  }
  void test_finalInitializedMultipleTimes_initializers() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x;", "  A() : x = 0, x = 0 {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS]);
    verify([source]);
  }

  /**
   * This test doesn't test the FINAL_INITIALIZED_MULTIPLE_TIMES code, but tests the
   * FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER code instead. It is provided here to show
   * coverage over all of the permutations of initializers in constructor declarations.
   *
   * Note: FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER covers a subset of
   * FINAL_INITIALIZED_MULTIPLE_TIMES, since it more specific, we use it instead of the broader code
   */
  void test_finalInitializedMultipleTimes_initializingFormal_initializer() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x;", "  A(this.x) : x = 0 {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER]);
    verify([source]);
  }
  void test_finalInitializedMultipleTimes_initializingFormals() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x;", "  A(this.x, this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.FINAL_INITIALIZED_MULTIPLE_TIMES]);
    verify([source]);
  }
  void test_finalNotInitialized_instanceField_const_static() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static const F;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_finalNotInitialized_library_const() {
    Source source = addSource(EngineTestCase.createSource(["const F;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_finalNotInitialized_local_const() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  const int x;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_getterAndMethodWithSameName() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  x(y) {}", "  get x => 0;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.GETTER_AND_METHOD_WITH_SAME_NAME]);
    verify([source]);
  }
  void test_implementsDisallowedClass_bool() {
    Source source = addSource(EngineTestCase.createSource(["class A implements bool {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_implementsDisallowedClass_double() {
    Source source = addSource(EngineTestCase.createSource(["class A implements double {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_implementsDisallowedClass_int() {
    Source source = addSource(EngineTestCase.createSource(["class A implements int {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_implementsDisallowedClass_Null() {
    Source source = addSource(EngineTestCase.createSource(["class A implements Null {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_implementsDisallowedClass_num() {
    Source source = addSource(EngineTestCase.createSource(["class A implements num {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_implementsDisallowedClass_String() {
    Source source = addSource(EngineTestCase.createSource(["class A implements String {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_implementsDynamic() {
    Source source = addSource(EngineTestCase.createSource(["class A implements dynamic {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_DYNAMIC]);
    verify([source]);
  }
  void test_implementsNonClass_class() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "class B implements A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_NON_CLASS]);
    verify([source]);
  }
  void test_implementsNonClass_typeAlias() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "int B;", "class C = A implements B;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_NON_CLASS]);
    verify([source]);
  }
  void test_implementsRepeated() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "class B implements A, A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_REPEATED]);
    verify([source]);
  }
  void test_implementsRepeated_3times() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {} class C{}",
        "class B implements A, A, A, A {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.IMPLEMENTS_REPEATED,
        CompileTimeErrorCode.IMPLEMENTS_REPEATED,
        CompileTimeErrorCode.IMPLEMENTS_REPEATED]);
    verify([source]);
  }
  void test_implementsSuperClass() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "class B extends A implements A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_SUPER_CLASS]);
    verify([source]);
  }
  void test_implementsSuperClass_Object() {
    Source source = addSource(EngineTestCase.createSource(["class A implements Object {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLEMENTS_SUPER_CLASS]);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f;",
        "  var f;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_field2() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x = 0;", "  final y = x;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_invocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var v;",
        "  A() : v = f();",
        "  f() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_invocationInStatic() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static var F = m();", "  m() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_redirectingConstructorInvocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A(p) {}",
        "  A.named() : this(f);",
        "  var f;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_implicitThisReferenceInInitializer_superConstructorInvocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A(p) {}",
        "}",
        "class B extends A {",
        "  B() : super(f);",
        "  var f;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_importInternalLibrary() {
    Source source = addSource(EngineTestCase.createSource(["import 'dart:_interceptors';"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.IMPORT_INTERNAL_LIBRARY,
        HintCode.UNUSED_IMPORT]);
    verify([source]);
  }
  void test_importInternalLibrary_collection() {
    Source source = addSource(EngineTestCase.createSource(["import 'dart:_collection-dev';"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.IMPORT_INTERNAL_LIBRARY,
        HintCode.UNUSED_IMPORT]);
    verify([source]);
  }
  void test_importOfNonLibrary() {
    Source source = addSource(EngineTestCase.createSource(["library lib;", "import 'part.dart';", "A a;"]));
    addSource2("/part.dart", EngineTestCase.createSource(["part of lib;", "class A{}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.IMPORT_OF_NON_LIBRARY]);
    verify([source]);
  }
  void test_inconsistentCaseExpressionTypes() {
    Source source = addSource(EngineTestCase.createSource([
        "f(var p) {",
        "  switch (p) {",
        "    case 1:",
        "      break;",
        "    case 'a':",
        "      break;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES]);
    verify([source]);
  }
  void test_inconsistentCaseExpressionTypes_repeated() {
    Source source = addSource(EngineTestCase.createSource([
        "f(var p) {",
        "  switch (p) {",
        "    case 1:",
        "      break;",
        "    case 'a':",
        "      break;",
        "    case 'b':",
        "      break;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES,
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES]);
    verify([source]);
  }
  void test_initializerForNonExistant_initializer() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() : x = 0 {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INITIALIZER_FOR_NON_EXISTANT_FIELD]);
  }
  void test_initializerForStaticField() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static int x;", "  A() : x = 0 {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INITIALIZER_FOR_STATIC_FIELD]);
    verify([source]);
  }
  void test_initializingFormalForNonExistantField() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A(this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD]);
    verify([source]);
  }
  void test_initializingFormalForNonExistantField_notInEnclosingClass() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "int x;",
        "}",
        "class B extends A {",
        "  B(this.x) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD]);
    verify([source]);
  }
  void test_initializingFormalForNonExistantField_optional() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A([this.x]) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD]);
    verify([source]);
  }
  void test_initializingFormalForNonExistantField_synthetic() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int get x => 1;", "  A(this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD]);
    verify([source]);
  }
  void test_initializingFormalForStaticField() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static int x;", "  A([this.x]) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_STATIC_FIELD]);
    verify([source]);
  }
  void test_instanceMemberAccessFromStatic_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int f;",
        "  static foo() {",
        "    f;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_STATIC]);
    verify([source]);
  }
  void test_instanceMemberAccessFromStatic_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  get g => null;",
        "  static foo() {",
        "    g;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_STATIC]);
    verify([source]);
  }
  void test_instanceMemberAccessFromStatic_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "  static foo() {",
        "    m();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_STATIC]);
    verify([source]);
  }
  void test_invalidAnnotation_getter() {
    Source source = addSource(EngineTestCase.createSource(["get V => 0;", "@V", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
    verify([source]);
  }
  void test_invalidAnnotation_importWithPrefix_getter() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "get V => 0;"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "@p.V", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
    verify([source]);
  }
  void test_invalidAnnotation_importWithPrefix_notConstantVariable() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "final V = 0;"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "@p.V", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
    verify([source]);
  }
  void test_invalidAnnotation_importWithPrefix_notVariableOrConstructorInvocation() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "typedef V();"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "@p.V", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
    verify([source]);
  }
  void test_invalidAnnotation_notConstantVariable() {
    Source source = addSource(EngineTestCase.createSource(["final V = 0;", "@V", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
    verify([source]);
  }
  void test_invalidAnnotation_notVariableOrConstructorInvocation() {
    Source source = addSource(EngineTestCase.createSource(["typedef V();", "@V", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
    verify([source]);
  }
  void test_invalidAnnotation_staticMethodReference() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static f() {}",
        "}",
        "@A.f",
        "main() {",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
    verify([source]);
  }
  void test_invalidAnnotation_unresolved_identifier() {
    Source source = addSource(EngineTestCase.createSource(["@unresolved", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
  }
  void test_invalidAnnotation_unresolved_invocation() {
    Source source = addSource(EngineTestCase.createSource(["@Unresolved()", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
  }
  void test_invalidAnnotation_unresolved_prefixedIdentifier() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'dart:math' as p;",
        "@p.unresolved",
        "main() {",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_ANNOTATION]);
  }
  void test_invalidConstructorName_notEnclosingClassName_defined() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  B() : super();", "}", "class B {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME]);
  }
  void test_invalidConstructorName_notEnclosingClassName_undefined() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  B() : super();", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME]);
  }
  void test_invalidFactoryNameNotAClass_notClassName() {
    Source source = addSource(EngineTestCase.createSource(["int B;", "class A {", "  factory B() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_FACTORY_NAME_NOT_A_CLASS]);
    verify([source]);
  }
  void test_invalidFactoryNameNotAClass_notEnclosingClassName() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  factory B() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_FACTORY_NAME_NOT_A_CLASS]);
  }
  void test_invalidReferenceToThis_factoryConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  factory A() { return this; }", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidReferenceToThis_instanceVariableInitializer_inConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  var f;", "  A() : f = this;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidReferenceToThis_instanceVariableInitializer_inDeclaration() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  var f = this;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidReferenceToThis_staticMethod() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static m() { return this; }", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidReferenceToThis_staticVariableInitializer() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static A f = this;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidReferenceToThis_superInitializer() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A(var x) {}",
        "}",
        "class B extends A {",
        "  B() : super(this);",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidReferenceToThis_topLevelFunction() {
    Source source = addSource("f() { return this; }");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidReferenceToThis_variableInitializer() {
    Source source = addSource("int x = this;");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS]);
    verify([source]);
  }
  void test_invalidTypeArgumentInConstList() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<E> {",
        "  m() {",
        "    return const <E>[];",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_LIST]);
    verify([source]);
  }
  void test_invalidTypeArgumentInConstMap() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<E> {",
        "  m() {",
        "    return const <String, E>{};",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_MAP]);
    verify([source]);
  }
  void test_invalidUri_export() {
    Source source = addSource(EngineTestCase.createSource(["export 'ht:';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_URI]);
  }
  void test_invalidUri_import() {
    Source source = addSource(EngineTestCase.createSource(["import 'ht:';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_URI]);
  }
  void test_invalidUri_part() {
    Source source = addSource(EngineTestCase.createSource(["part 'ht:';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.INVALID_URI]);
  }
  void test_labelInOuterScope() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m(int i) {",
        "    l: while (i > 0) {",
        "      void f() {",
        "        break l;",
        "      };",
        "    }",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.LABEL_IN_OUTER_SCOPE]);
  }
  void test_labelUndefined_break() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  x: while (true) {",
        "    break y;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.LABEL_UNDEFINED]);
  }
  void test_labelUndefined_continue() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  x: while (true) {",
        "    continue y;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.LABEL_UNDEFINED]);
  }
  void test_listElementTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v = const <String> [42];"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.LIST_ELEMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_mapKeyTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v = const <String, int > {1 : 2};"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MAP_KEY_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_mapValueTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v = const <String, String> {'a' : 2};"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MAP_VALUE_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_memberWithClassName_field() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int A = 0;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME]);
    verify([source]);
  }
  void test_memberWithClassName_field2() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int z, A, b = 0;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME]);
    verify([source]);
  }
  void test_memberWithClassName_getter() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  get A => 0;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME]);
    verify([source]);
  }
  void test_memberWithClassName_method() {
  }
  void test_methodAndGetterWithSameName() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  get x => 0;", "  x(y) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.METHOD_AND_GETTER_WITH_SAME_NAME]);
    verify([source]);
  }
  void test_mixinDeclaresConstructor_classDeclaration() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "class B extends Object with A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR]);
    verify([source]);
  }
  void test_mixinDeclaresConstructor_typeAlias() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() {}", "}", "class B = Object with A;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR]);
    verify([source]);
  }
  void test_mixinInheritsFromNotObject_classDeclaration_extends() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "class C extends Object with B {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT]);
    verify([source]);
  }
  void test_mixinInheritsFromNotObject_classDeclaration_with() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends Object with A {}",
        "class C extends Object with B {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT]);
    verify([source]);
  }
  void test_mixinInheritsFromNotObject_typeAlias_extends() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "class C = Object with B;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT]);
    verify([source]);
  }
  void test_mixinInheritsFromNotObject_typeAlias_with() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends Object with A {}",
        "class C = Object with B;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT]);
    verify([source]);
  }
  void test_mixinOfDisallowedClass_bool() {
    Source source = addSource(EngineTestCase.createSource(["class A extends Object with bool {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_mixinOfDisallowedClass_double() {
    Source source = addSource(EngineTestCase.createSource(["class A extends Object with double {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_mixinOfDisallowedClass_int() {
    Source source = addSource(EngineTestCase.createSource(["class A extends Object with int {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_mixinOfDisallowedClass_Null() {
    Source source = addSource(EngineTestCase.createSource(["class A extends Object with Null {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_mixinOfDisallowedClass_num() {
    Source source = addSource(EngineTestCase.createSource(["class A extends Object with num {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_mixinOfDisallowedClass_String() {
    Source source = addSource(EngineTestCase.createSource(["class A extends Object with String {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS]);
    verify([source]);
  }
  void test_mixinOfNonClass_class() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "class B extends Object with A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_NON_CLASS]);
    verify([source]);
  }
  void test_mixinOfNonClass_typeAlias() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "int B;", "class C = A with B;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_OF_NON_CLASS]);
    verify([source]);
  }
  void test_mixinReferencesSuper() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  toString() => super.toString();",
        "}",
        "class B extends Object with A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_REFERENCES_SUPER]);
    verify([source]);
  }
  void test_mixinWithNonClassSuperclass_class() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "class B {}", "class C extends A with B {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_WITH_NON_CLASS_SUPERCLASS]);
    verify([source]);
  }
  void test_mixinWithNonClassSuperclass_typeAlias() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "class B {}", "class C = A with B;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MIXIN_WITH_NON_CLASS_SUPERCLASS]);
    verify([source]);
  }
  void test_multipleRedirectingConstructorInvocations() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() : this.a(), this.b();",
        "  A.a() {}",
        "  A.b() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MULTIPLE_REDIRECTING_CONSTRUCTOR_INVOCATIONS]);
    verify([source]);
  }
  void test_multipleSuperInitializers() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  B() : super(), super() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.MULTIPLE_SUPER_INITIALIZERS]);
    verify([source]);
  }
  void test_nativeClauseInNonSDKCode() {
    Source source = addSource(EngineTestCase.createSource(["class A native 'string' {}"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.NATIVE_CLAUSE_IN_NON_SDK_CODE]);
    verify([source]);
  }
  void test_nativeFunctionBodyInNonSDKCode_function() {
    Source source = addSource(EngineTestCase.createSource(["int m(a) native 'string';"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE]);
    verify([source]);
  }
  void test_nativeFunctionBodyInNonSDKCode_method() {
    Source source = addSource(EngineTestCase.createSource(["class A{", "  static int m(a) native 'string';", "}"]));
    resolve(source);
    assertErrors(source, [ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE]);
    verify([source]);
  }
  void test_noAnnotationConstructorArguments() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  const A();", "}", "@A", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NO_ANNOTATION_CONSTRUCTOR_ARGUMENTS]);
    verify([source]);
  }
  void test_noDefaultSuperConstructorExplicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A(p);",
        "}",
        "class B extends A {",
        "  B() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT]);
    verify([source]);
  }
  void test_noDefaultSuperConstructorImplicit_superHasParameters() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A(p);", "}", "class B extends A {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_noDefaultSuperConstructorImplicit_superOnlyNamed() {
    Source source = addSource(EngineTestCase.createSource(["class A { A.named() {} }", "class B extends A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT]);
    verify([source]);
  }
  void test_nonConstantAnnotationConstructor_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.fromInt() {}",
        "}",
        "@A.fromInt()",
        "main() {",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_ANNOTATION_CONSTRUCTOR]);
    verify([source]);
  }
  void test_nonConstantAnnotationConstructor_unnamed() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() {}", "}", "@A()", "main() {", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_ANNOTATION_CONSTRUCTOR]);
    verify([source]);
  }
  void test_nonConstantDefaultValue_function_named() {
    Source source = addSource(EngineTestCase.createSource(["int y;", "f({x : y}) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE]);
    verify([source]);
  }
  void test_nonConstantDefaultValue_function_positional() {
    Source source = addSource(EngineTestCase.createSource(["int y;", "f([x = y]) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE]);
    verify([source]);
  }
  void test_nonConstantDefaultValue_inConstructor_named() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int y;", "  A({x : y}) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE]);
    verify([source]);
  }
  void test_nonConstantDefaultValue_inConstructor_positional() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int y;", "  A([x = y]) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE]);
    verify([source]);
  }
  void test_nonConstantDefaultValue_method_named() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int y;", "  m({x : y}) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE]);
    verify([source]);
  }
  void test_nonConstantDefaultValue_method_positional() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int y;", "  m([x = y]) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE]);
    verify([source]);
  }
  void test_nonConstCaseExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int p, int q) {",
        "  switch (p) {",
        "    case 3 + q:",
        "      break;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION]);
    verify([source]);
  }
  void test_nonConstListElement() {
    Source source = addSource(EngineTestCase.createSource(["f(a) {", "  return const [a];", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT]);
    verify([source]);
  }
  void test_nonConstMapAsExpressionStatement_begin() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  {'a' : 0, 'b' : 1}.length;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONST_MAP_AS_EXPRESSION_STATEMENT]);
    verify([source]);
  }
  void test_nonConstMapAsExpressionStatement_only() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  {'a' : 0, 'b' : 1};", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONST_MAP_AS_EXPRESSION_STATEMENT]);
    verify([source]);
  }
  void test_nonConstMapKey() {
    Source source = addSource(EngineTestCase.createSource(["f(a) {", "  return const {a : 0};", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_MAP_KEY]);
    verify([source]);
  }
  void test_nonConstMapValue() {
    Source source = addSource(EngineTestCase.createSource(["f(a) {", "  return const {'a' : a};", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_binary_notBool_left() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final bool a;",
        "  const A(String p) : a = p && true;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_binary_notBool_right() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final bool a;",
        "  const A(String p) : a = true && p;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_binary_notInt() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final int a;",
        "  const A(String p) : a = 5 & p;",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_EVAL_TYPE_INT,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_binary_notNum() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final int a;",
        "  const A(String p) : a = 5 + p;",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_EVAL_TYPE_NUM,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int C;",
        "  final int a;",
        "  const A() : a = C;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_redirecting() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static var C;",
        "  const A.named(p);",
        "  const A() : this.named(C);",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_nonConstValueInInitializer_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(p);",
        "}",
        "class B extends A {",
        "  static var C;",
        "  const B() : super(C);",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER]);
    verify([source]);
  }
  void test_nonGenerativeConstructor_explicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  factory A.named() {}",
        "}",
        "class B extends A {",
        "  B() : super.named();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR]);
    verify([source]);
  }
  void test_nonGenerativeConstructor_implicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  factory A() {}",
        "}",
        "class B extends A {",
        "  B();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR]);
    verify([source]);
  }
  void test_nonGenerativeConstructor_implicit2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  factory A() {}",
        "}",
        "class B extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR]);
    verify([source]);
  }
  void test_notEnoughRequiredArguments_const() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(int p);",
        "}",
        "main() {",
        "  const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NOT_ENOUGH_REQUIRED_ARGUMENTS]);
    verify([source]);
  }
  void test_notEnoughRequiredArguments_const_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(int p);",
        "}",
        "class B extends A {",
        "  const B() : super();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.NOT_ENOUGH_REQUIRED_ARGUMENTS]);
    verify([source]);
  }
  void test_optionalParameterInOperator_named() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator +({p}) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.OPTIONAL_PARAMETER_IN_OPERATOR]);
    verify([source]);
  }
  void test_optionalParameterInOperator_positional() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator +([p]) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.OPTIONAL_PARAMETER_IN_OPERATOR]);
    verify([source]);
  }
  void test_partOfNonPart() {
    Source source = addSource(EngineTestCase.createSource(["library l1;", "part 'l2.dart';"]));
    addSource2("/l2.dart", EngineTestCase.createSource(["library l2;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PART_OF_NON_PART]);
    verify([source]);
  }
  void test_prefixCollidesWithTopLevelMembers_functionTypeAlias() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A{}"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "typedef p();", "p.A a;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER]);
    verify([source]);
  }
  void test_prefixCollidesWithTopLevelMembers_topLevelFunction() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A{}"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "p() {}", "p.A a;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER]);
    verify([source]);
  }
  void test_prefixCollidesWithTopLevelMembers_topLevelVariable() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A{}"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "var p = null;", "p.A a;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER]);
    verify([source]);
  }
  void test_prefixCollidesWithTopLevelMembers_type() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A{}"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as p;", "class p {}", "p.A a;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER]);
    verify([source]);
  }
  void test_privateOptionalParameter() {
    Source source = addSource(EngineTestCase.createSource(["f({var _p}) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER]);
    verify([source]);
  }
  void test_privateOptionalParameter_fieldFormal() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  var _p;", "  A({this._p: 0});", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER]);
    verify([source]);
  }
  void test_privateOptionalParameter_withDefaultValue() {
    Source source = addSource(EngineTestCase.createSource(["f({_p : 0}) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER]);
    verify([source]);
  }
  void test_recursiveConstructorRedirect() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.a() : this.b();",
        "  A.b() : this.a();",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_CONSTRUCTOR_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_CONSTRUCTOR_REDIRECT]);
    verify([source]);
  }
  void test_recursiveConstructorRedirect_directSelfReference() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() : this();", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_CONSTRUCTOR_REDIRECT]);
    verify([source]);
  }
  void test_recursiveFactoryRedirect() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B {",
        "  factory A() = C;",
        "}",
        "class B implements C {",
        "  factory B() = A;",
        "}",
        "class C implements A {",
        "  factory C() = B;",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveFactoryRedirect_directSelfReference() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  factory A() = A;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT]);
    verify([source]);
  }
  void test_recursiveFactoryRedirect_generic() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> implements B<T> {",
        "  factory A() = C;",
        "}",
        "class B<T> implements C<T> {",
        "  factory B() = A;",
        "}",
        "class C<T> implements A<T> {",
        "  factory C() = B;",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveFactoryRedirect_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B {",
        "  factory A.nameA() = C.nameC;",
        "}",
        "class B implements C {",
        "  factory B.nameB() = A.nameA;",
        "}",
        "class C implements A {",
        "  factory C.nameC() = B.nameB;",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }

  /**
   * "A" references "C" which has cycle with "B". But we should not report problem for "A" - it is
   * not the part of a cycle.
   */
  void test_recursiveFactoryRedirect_outsideCycle() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  factory A() = C;",
        "}",
        "class B implements C {",
        "  factory B() = C;",
        "}",
        "class C implements A, B {",
        "  factory C() = B;",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritance_extends() {
    Source source = addSource(EngineTestCase.createSource(["class A extends B {}", "class B extends A {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritance_extends_implements() {
    Source source = addSource(EngineTestCase.createSource(["class A extends B {}", "class B implements A {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritance_implements() {
    Source source = addSource(EngineTestCase.createSource(["class A implements B {}", "class B implements A {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritance_tail() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A implements A {}",
        "class B implements A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritance_tail2() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A implements B {}",
        "abstract class B implements A {}",
        "class C implements A {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritance_tail3() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A implements B {}",
        "abstract class B implements C {}",
        "abstract class C implements A {}",
        "class D implements A {}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritanceBaseCaseExtends() {
    Source source = addSource(EngineTestCase.createSource(["class A extends A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_EXTENDS]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritanceBaseCaseImplements() {
    Source source = addSource(EngineTestCase.createSource(["class A implements A {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS]);
    verify([source]);
  }
  void test_recursiveInterfaceInheritanceBaseCaseImplements_typeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class M {}",
        "class B = A with M implements B;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS]);
    verify([source]);
  }
  void test_redirectToMissingConstructor_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B{",
        "  A() {}",
        "}",
        "class B {",
        "  const factory B() = A.name;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REDIRECT_TO_MISSING_CONSTRUCTOR]);
  }
  void test_redirectToMissingConstructor_unnamed() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B{",
        "  A.name() {}",
        "}",
        "class B {",
        "  const factory B() = A;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REDIRECT_TO_MISSING_CONSTRUCTOR]);
  }
  void test_redirectToNonClass_notAType() {
    Source source = addSource(EngineTestCase.createSource(["int A;", "class B {", "  const factory B() = A;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REDIRECT_TO_NON_CLASS]);
    verify([source]);
  }
  void test_redirectToNonClass_undefinedIdentifier() {
    Source source = addSource(EngineTestCase.createSource(["class B {", "  const factory B() = A;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REDIRECT_TO_NON_CLASS]);
    verify([source]);
  }
  void test_redirectToNonConstConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.a() {}",
        "  const factory A.b() = A.a;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REDIRECT_TO_NON_CONST_CONSTRUCTOR]);
    verify([source]);
  }
  void test_referencedBeforeDeclaration_hideInBlock_function() {
    Source source = addSource(EngineTestCase.createSource([
        "var v = 1;",
        "main() {",
        "  print(v);",
        "  v() {}",
        "}",
        "print(x) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION]);
  }
  void test_referencedBeforeDeclaration_hideInBlock_local() {
    Source source = addSource(EngineTestCase.createSource([
        "var v = 1;",
        "main() {",
        "  print(v);",
        "  var v = 2;",
        "}",
        "print(x) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION]);
  }
  void test_referencedBeforeDeclaration_hideInBlock_subBlock() {
    Source source = addSource(EngineTestCase.createSource([
        "var v = 1;",
        "main() {",
        "  {",
        "    print(v);",
        "  }",
        "  var v = 2;",
        "}",
        "print(x) {}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION]);
  }
  void test_rethrowOutsideCatch() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  rethrow;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RETHROW_OUTSIDE_CATCH]);
    verify([source]);
  }
  void test_returnInGenerativeConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() { return 0; }", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RETURN_IN_GENERATIVE_CONSTRUCTOR]);
    verify([source]);
  }
  void test_returnInGenerativeConstructor_expressionFunctionBody() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() => null;", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.RETURN_IN_GENERATIVE_CONSTRUCTOR]);
    verify([source]);
  }
  void test_superInInvalidContext_binaryExpression() {
    Source source = addSource(EngineTestCase.createSource(["var v = super + 0;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInInvalidContext_constructorFieldInitializer() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  var f;",
        "  B() : f = super.m();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInInvalidContext_factoryConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  factory B() {",
        "    super.m();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInInvalidContext_instanceVariableInitializer() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  var a;",
        "}",
        "class B extends A {",
        " var b = super.a;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInInvalidContext_staticMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static m() {}",
        "}",
        "class B extends A {",
        "  static n() { return super.m(); }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInInvalidContext_staticVariableInitializer() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int a = 0;",
        "}",
        "class B extends A {",
        "  static int b = super.a;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInInvalidContext_topLevelFunction() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  super.f();", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInInvalidContext_topLevelVariableInitializer() {
    Source source = addSource(EngineTestCase.createSource(["var v = super.y;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT]);
  }
  void test_superInRedirectingConstructor_redirectionSuper() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {",
        "  B() : this.name(), super();",
        "  B.name() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_REDIRECTING_CONSTRUCTOR]);
    verify([source]);
  }
  void test_superInRedirectingConstructor_superRedirection() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {",
        "  B() : super(), this.name();",
        "  B.name() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.SUPER_IN_REDIRECTING_CONSTRUCTOR]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_parameterType_named() {
    Source source = addSource(EngineTestCase.createSource(["typedef A({A a});"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_parameterType_positional() {
    Source source = addSource(EngineTestCase.createSource(["typedef A([A a]);"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_parameterType_required() {
    Source source = addSource(EngineTestCase.createSource(["typedef A(A a);"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_parameterType_typeArgument() {
    Source source = addSource(EngineTestCase.createSource(["typedef A(List<A> a);"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_returnClass_withTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef C A();",
        "typedef A B();",
        "class C {",
        "  B a;",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_returnType() {
    Source source = addSource(EngineTestCase.createSource(["typedef A A();"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_returnType_indirect() {
    Source source = addSource(EngineTestCase.createSource(["typedef B A();", "typedef A B();"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF,
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotReferenceItself_typeVariableBounds() {
    Source source = addSource(EngineTestCase.createSource(["typedef A<T extends A>();"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotRereferenceItself_mixin_direct() {
    Source source = addSource(EngineTestCase.createSource(["class M = Object with M;"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeAliasCannotRereferenceItself_mixin_indirect() {
    Source source = addSource(EngineTestCase.createSource([
        "class M1 = Object with M2;",
        "class M2 = Object with M1;"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF,
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF]);
    verify([source]);
  }
  void test_typeArgumentNotMatchingBounds_const() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B {}",
        "class G<E extends A> {",
        "  const G();",
        "}",
        "f() { return const G<B>(); }"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS]);
    verify([source]);
  }
  void test_undefinedClass_const() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  return const A();", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_CLASS]);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_explicit_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  B() : super.named();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER]);
  }
  void test_undefinedConstructorInInitializer_explicit_unnamed() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.named() {}",
        "}",
        "class B extends A {",
        "  B() : super();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT]);
    verify([source]);
  }
  void test_undefinedConstructorInInitializer_implicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.named() {}",
        "}",
        "class B extends A {",
        "  B();",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT]);
    verify([source]);
  }
  void test_undefinedFunction() {
    Source source = addSource(EngineTestCase.createSource(["void f() {", "  g();", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_FUNCTION]);
  }
  void test_undefinedFunction_hasImportPrefix() {
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as f;", "main() { return f(); }"]));
    addSource2("/lib.dart", "library lib;");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_FUNCTION]);
  }
  void test_undefinedFunction_inCatch() {
    Source source = addSource(EngineTestCase.createSource([
        "void f() {",
        "  try {",
        "  } on Object {",
        "    g();",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_FUNCTION]);
  }
  void test_undefinedNamedParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A();",
        "}",
        "main() {",
        "  const A(p: 0);",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.UNDEFINED_NAMED_PARAMETER]);
  }
  void test_uriDoesNotExist_export() {
    Source source = addSource(EngineTestCase.createSource(["export 'unknown.dart';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.URI_DOES_NOT_EXIST]);
  }
  void test_uriDoesNotExist_import() {
    Source source = addSource(EngineTestCase.createSource(["import 'unknown.dart';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.URI_DOES_NOT_EXIST]);
  }
  void test_uriDoesNotExist_part() {
    Source source = addSource(EngineTestCase.createSource(["part 'unknown.dart';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.URI_DOES_NOT_EXIST]);
  }
  void test_uriWithInterpolation_constant() {
    Source source = addSource(EngineTestCase.createSource(["import 'stuff_\$platform.dart';"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.URI_WITH_INTERPOLATION,
        StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_uriWithInterpolation_nonConstant() {
    Source source = addSource(EngineTestCase.createSource(["library lib;", "part '\${'a'}.dart';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.URI_WITH_INTERPOLATION]);
  }
  void test_wrongNumberOfParametersForOperator_minus() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator -(a, b) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR_MINUS]);
    verify([source]);
    reset();
  }
  void test_wrongNumberOfParametersForOperator_tilde() {
    check_wrongNumberOfParametersForOperator("~", "a");
    check_wrongNumberOfParametersForOperator("~", "a, b");
  }
  void test_wrongNumberOfParametersForOperator1() {
    check_wrongNumberOfParametersForOperator1("<");
    check_wrongNumberOfParametersForOperator1(">");
    check_wrongNumberOfParametersForOperator1("<=");
    check_wrongNumberOfParametersForOperator1(">=");
    check_wrongNumberOfParametersForOperator1("+");
    check_wrongNumberOfParametersForOperator1("/");
    check_wrongNumberOfParametersForOperator1("~/");
    check_wrongNumberOfParametersForOperator1("*");
    check_wrongNumberOfParametersForOperator1("%");
    check_wrongNumberOfParametersForOperator1("|");
    check_wrongNumberOfParametersForOperator1("^");
    check_wrongNumberOfParametersForOperator1("&");
    check_wrongNumberOfParametersForOperator1("<<");
    check_wrongNumberOfParametersForOperator1(">>");
    check_wrongNumberOfParametersForOperator1("[]");
  }
  void test_wrongNumberOfParametersForSetter_function_named() {
    Source source = addSource("set x({p}) {}");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void test_wrongNumberOfParametersForSetter_function_optional() {
    Source source = addSource("set x([p]) {}");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void test_wrongNumberOfParametersForSetter_function_tooFew() {
    Source source = addSource("set x() {}");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void test_wrongNumberOfParametersForSetter_function_tooMany() {
    Source source = addSource("set x(a, b) {}");
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void test_wrongNumberOfParametersForSetter_method_named() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  set x({p}) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void test_wrongNumberOfParametersForSetter_method_optional() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  set x([p]) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void test_wrongNumberOfParametersForSetter_method_tooFew() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  set x() {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void test_wrongNumberOfParametersForSetter_method_tooMany() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  set x(a, b) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER]);
    verify([source]);
  }
  void check_constEvalThrowsException_binary_null(String expr, bool resolved) {
    Source source = addSource("const C = ${expr};");
    resolve(source);
    if (resolved) {
      assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION]);
      verify([source]);
    } else {
      assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION]);
    }
    reset();
  }
  void check_constEvalTypeBool_withParameter_binary(String expr) {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final a;",
        "  const A(bool p) : a = ${expr};",
        "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL]);
    verify([source]);
    reset();
  }
  void check_constEvalTypeInt_withParameter_binary(String expr) {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final a;",
        "  const A(int p) : a = ${expr};",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_EVAL_TYPE_INT,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
    reset();
  }
  void check_constEvalTypeNum_withParameter_binary(String expr) {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final a;",
        "  const A(num p) : a = ${expr};",
        "}"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.CONST_EVAL_TYPE_NUM,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
    reset();
  }
  void check_wrongNumberOfParametersForOperator(String name, String parameters) {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  operator ${name}(${parameters}) {}", "}"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR]);
    verify([source]);
    reset();
  }
  void check_wrongNumberOfParametersForOperator1(String name) {
    check_wrongNumberOfParametersForOperator(name, "");
    check_wrongNumberOfParametersForOperator(name, "a, b");
  }
  static dartSuite() {
    _ut.group('CompileTimeErrorCodeTest', () {
      _ut.test('test_ambiguousExport', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_ambiguousExport);
      });
      _ut.test('test_ambiguousImport_function', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_function);
      });
      _ut.test('test_argumentDefinitionTestNonParameter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_argumentDefinitionTestNonParameter);
      });
      _ut.test('test_builtInIdentifierAsMixinName_classTypeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsMixinName_classTypeAlias);
      });
      _ut.test('test_builtInIdentifierAsTypeName', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsTypeName);
      });
      _ut.test('test_builtInIdentifierAsTypeParameterName', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsTypeParameterName);
      });
      _ut.test('test_builtInIdentifierAsType_formalParameter_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsType_formalParameter_field);
      });
      _ut.test('test_builtInIdentifierAsType_formalParameter_simple', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsType_formalParameter_simple);
      });
      _ut.test('test_builtInIdentifierAsType_variableDeclaration', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsType_variableDeclaration);
      });
      _ut.test('test_builtInIdentifierAsTypedefName_functionTypeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_builtInIdentifierAsTypedefName_functionTypeAlias);
      });
      _ut.test('test_caseExpressionTypeImplementsEquals', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_caseExpressionTypeImplementsEquals);
      });
      _ut.test('test_conflictingConstructorNameAndMember_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingConstructorNameAndMember_field);
      });
      _ut.test('test_conflictingConstructorNameAndMember_method', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingConstructorNameAndMember_method);
      });
      _ut.test('test_conflictingGetterAndMethod_field_method', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingGetterAndMethod_field_method);
      });
      _ut.test('test_conflictingGetterAndMethod_getter_method', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingGetterAndMethod_getter_method);
      });
      _ut.test('test_conflictingGetterAndMethod_method_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingGetterAndMethod_method_field);
      });
      _ut.test('test_conflictingGetterAndMethod_method_getter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingGetterAndMethod_method_getter);
      });
      _ut.test('test_conflictingTypeVariableAndClass', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingTypeVariableAndClass);
      });
      _ut.test('test_conflictingTypeVariableAndMember_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingTypeVariableAndMember_field);
      });
      _ut.test('test_conflictingTypeVariableAndMember_getter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingTypeVariableAndMember_getter);
      });
      _ut.test('test_conflictingTypeVariableAndMember_method', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingTypeVariableAndMember_method);
      });
      _ut.test('test_conflictingTypeVariableAndMember_method_static', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingTypeVariableAndMember_method_static);
      });
      _ut.test('test_conflictingTypeVariableAndMember_setter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_conflictingTypeVariableAndMember_setter);
      });
      _ut.test('test_constConstructorWithNonConstSuper_explicit', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonConstSuper_explicit);
      });
      _ut.test('test_constConstructorWithNonConstSuper_implicit', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonConstSuper_implicit);
      });
      _ut.test('test_constConstructorWithNonFinalField_mixin', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonFinalField_mixin);
      });
      _ut.test('test_constConstructorWithNonFinalField_super', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonFinalField_super);
      });
      _ut.test('test_constConstructorWithNonFinalField_this', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constConstructorWithNonFinalField_this);
      });
      _ut.test('test_constEvalThrowsException_binaryMinus_null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalThrowsException_binaryMinus_null);
      });
      _ut.test('test_constEvalThrowsException_binaryPlus_null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalThrowsException_binaryPlus_null);
      });
      _ut.test('test_constEvalThrowsException_divisionByZero', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalThrowsException_divisionByZero);
      });
      _ut.test('test_constEvalThrowsException_unaryBitNot_null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalThrowsException_unaryBitNot_null);
      });
      _ut.test('test_constEvalThrowsException_unaryNegated_null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalThrowsException_unaryNegated_null);
      });
      _ut.test('test_constEvalThrowsException_unaryNot_null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalThrowsException_unaryNot_null);
      });
      _ut.test('test_constEvalTypeBoolNumString_equal', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalTypeBoolNumString_equal);
      });
      _ut.test('test_constEvalTypeBoolNumString_notEqual', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalTypeBoolNumString_notEqual);
      });
      _ut.test('test_constEvalTypeBool_binary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalTypeBool_binary);
      });
      _ut.test('test_constEvalTypeBool_binary_leftTrue', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalTypeBool_binary_leftTrue);
      });
      _ut.test('test_constEvalTypeInt_binary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalTypeInt_binary);
      });
      _ut.test('test_constEvalTypeNum_binary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalTypeNum_binary);
      });
      _ut.test('test_constEvalTypeNum_plus_String', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEvalTypeNum_plus_String);
      });
      _ut.test('test_constEval_newInstance_constConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEval_newInstance_constConstructor);
      });
      _ut.test('test_constEval_propertyExtraction_targetNotConst', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constEval_propertyExtraction_targetNotConst);
      });
      _ut.test('test_constFormalParameter_fieldFormalParameter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constFormalParameter_fieldFormalParameter);
      });
      _ut.test('test_constFormalParameter_simpleFormalParameter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constFormalParameter_simpleFormalParameter);
      });
      _ut.test('test_constInitializedWithNonConstValue', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constInitializedWithNonConstValue);
      });
      _ut.test('test_constInitializedWithNonConstValue_missingConstInListLiteral', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constInitializedWithNonConstValue_missingConstInListLiteral);
      });
      _ut.test('test_constInitializedWithNonConstValue_missingConstInMapLiteral', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constInitializedWithNonConstValue_missingConstInMapLiteral);
      });
      _ut.test('test_constInstanceField', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constInstanceField);
      });
      _ut.test('test_constMapKeyTypeImplementsEquals_direct', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constMapKeyTypeImplementsEquals_direct);
      });
      _ut.test('test_constMapKeyTypeImplementsEquals_super', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constMapKeyTypeImplementsEquals_super);
      });
      _ut.test('test_constWithInvalidTypeParameters', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithInvalidTypeParameters);
      });
      _ut.test('test_constWithInvalidTypeParameters_tooFew', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithInvalidTypeParameters_tooFew);
      });
      _ut.test('test_constWithInvalidTypeParameters_tooMany', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithInvalidTypeParameters_tooMany);
      });
      _ut.test('test_constWithNonConst', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithNonConst);
      });
      _ut.test('test_constWithNonConstantArgument_annotation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithNonConstantArgument_annotation);
      });
      _ut.test('test_constWithNonConstantArgument_instanceCreation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithNonConstantArgument_instanceCreation);
      });
      _ut.test('test_constWithNonType', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithNonType);
      });
      _ut.test('test_constWithNonType_fromLibrary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithNonType_fromLibrary);
      });
      _ut.test('test_constWithTypeParameters_direct', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithTypeParameters_direct);
      });
      _ut.test('test_constWithTypeParameters_indirect', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithTypeParameters_indirect);
      });
      _ut.test('test_constWithUndefinedConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithUndefinedConstructor);
      });
      _ut.test('test_constWithUndefinedConstructorDefault', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_constWithUndefinedConstructorDefault);
      });
      _ut.test('test_defaultValueInFunctionTypeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_defaultValueInFunctionTypeAlias);
      });
      _ut.test('test_defaultValueInFunctionTypedParameter_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_defaultValueInFunctionTypedParameter_named);
      });
      _ut.test('test_defaultValueInFunctionTypedParameter_optional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_defaultValueInFunctionTypedParameter_optional);
      });
      _ut.test('test_defaultValueInRedirectingFactoryConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_defaultValueInRedirectingFactoryConstructor);
      });
      _ut.test('test_duplicateConstructorName_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateConstructorName_named);
      });
      _ut.test('test_duplicateConstructorName_unnamed', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateConstructorName_unnamed);
      });
      _ut.test('test_duplicateDefinition', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition);
      });
      _ut.test('test_duplicateDefinitionInheritance_instanceGetterAbstract_staticGetter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinitionInheritance_instanceGetterAbstract_staticGetter);
      });
      _ut.test('test_duplicateDefinitionInheritance_instanceGetter_staticGetter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinitionInheritance_instanceGetter_staticGetter);
      });
      _ut.test('test_duplicateDefinitionInheritance_instanceMethodAbstract_staticMethod', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinitionInheritance_instanceMethodAbstract_staticMethod);
      });
      _ut.test('test_duplicateDefinitionInheritance_instanceMethod_staticMethod', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinitionInheritance_instanceMethod_staticMethod);
      });
      _ut.test('test_duplicateDefinitionInheritance_instanceSetterAbstract_staticSetter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinitionInheritance_instanceSetterAbstract_staticSetter);
      });
      _ut.test('test_duplicateDefinitionInheritance_instanceSetter_staticSetter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinitionInheritance_instanceSetter_staticSetter);
      });
      _ut.test('test_duplicateDefinition_acrossLibraries', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_acrossLibraries);
      });
      _ut.test('test_duplicateDefinition_classMembers_fields', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_classMembers_fields);
      });
      _ut.test('test_duplicateDefinition_classMembers_fields_oneStatic', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_classMembers_fields_oneStatic);
      });
      _ut.test('test_duplicateDefinition_classMembers_methods', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_classMembers_methods);
      });
      _ut.test('test_duplicateDefinition_localFields', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_localFields);
      });
      _ut.test('test_duplicateDefinition_parameterWithFunctionName_local', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_parameterWithFunctionName_local);
      });
      _ut.test('test_duplicateDefinition_parameterWithFunctionName_topLevel', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateDefinition_parameterWithFunctionName_topLevel);
      });
      _ut.test('test_duplicateNamedArgument', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_duplicateNamedArgument);
      });
      _ut.test('test_exportInternalLibrary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_exportInternalLibrary);
      });
      _ut.test('test_exportOfNonLibrary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_exportOfNonLibrary);
      });
      _ut.test('test_extendsDisallowedClass_Null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsDisallowedClass_Null);
      });
      _ut.test('test_extendsDisallowedClass_String', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsDisallowedClass_String);
      });
      _ut.test('test_extendsDisallowedClass_bool', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsDisallowedClass_bool);
      });
      _ut.test('test_extendsDisallowedClass_double', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsDisallowedClass_double);
      });
      _ut.test('test_extendsDisallowedClass_int', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsDisallowedClass_int);
      });
      _ut.test('test_extendsDisallowedClass_num', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsDisallowedClass_num);
      });
      _ut.test('test_extendsNonClass_class', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsNonClass_class);
      });
      _ut.test('test_extendsNonClass_dynamic', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extendsNonClass_dynamic);
      });
      _ut.test('test_extraPositionalArguments_const', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments_const);
      });
      _ut.test('test_extraPositionalArguments_const_super', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments_const_super);
      });
      _ut.test('test_fieldInitializedByMultipleInitializers', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializedByMultipleInitializers);
      });
      _ut.test('test_fieldInitializedByMultipleInitializers_multipleInits', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializedByMultipleInitializers_multipleInits);
      });
      _ut.test('test_fieldInitializedByMultipleInitializers_multipleNames', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializedByMultipleInitializers_multipleNames);
      });
      _ut.test('test_fieldInitializedInParameterAndInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializedInParameterAndInitializer);
      });
      _ut.test('test_fieldInitializerFactoryConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializerFactoryConstructor);
      });
      _ut.test('test_fieldInitializerNotAssignable', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializerNotAssignable);
      });
      _ut.test('test_fieldInitializerOutsideConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializerOutsideConstructor);
      });
      _ut.test('test_fieldInitializerOutsideConstructor_defaultParameter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializerOutsideConstructor_defaultParameter);
      });
      _ut.test('test_fieldInitializerRedirectingConstructor_afterRedirection', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializerRedirectingConstructor_afterRedirection);
      });
      _ut.test('test_fieldInitializerRedirectingConstructor_beforeRedirection', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializerRedirectingConstructor_beforeRedirection);
      });
      _ut.test('test_fieldInitializingFormalRedirectingConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializingFormalRedirectingConstructor);
      });
      _ut.test('test_finalInitializedMultipleTimes_initializers', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_finalInitializedMultipleTimes_initializers);
      });
      _ut.test('test_finalInitializedMultipleTimes_initializingFormal_initializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_finalInitializedMultipleTimes_initializingFormal_initializer);
      });
      _ut.test('test_finalInitializedMultipleTimes_initializingFormals', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_finalInitializedMultipleTimes_initializingFormals);
      });
      _ut.test('test_finalNotInitialized_instanceField_const_static', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_instanceField_const_static);
      });
      _ut.test('test_finalNotInitialized_library_const', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_library_const);
      });
      _ut.test('test_finalNotInitialized_local_const', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_local_const);
      });
      _ut.test('test_getterAndMethodWithSameName', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_getterAndMethodWithSameName);
      });
      _ut.test('test_implementsDisallowedClass_Null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsDisallowedClass_Null);
      });
      _ut.test('test_implementsDisallowedClass_String', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsDisallowedClass_String);
      });
      _ut.test('test_implementsDisallowedClass_bool', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsDisallowedClass_bool);
      });
      _ut.test('test_implementsDisallowedClass_double', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsDisallowedClass_double);
      });
      _ut.test('test_implementsDisallowedClass_int', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsDisallowedClass_int);
      });
      _ut.test('test_implementsDisallowedClass_num', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsDisallowedClass_num);
      });
      _ut.test('test_implementsDynamic', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsDynamic);
      });
      _ut.test('test_implementsNonClass_class', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsNonClass_class);
      });
      _ut.test('test_implementsNonClass_typeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsNonClass_typeAlias);
      });
      _ut.test('test_implementsRepeated', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsRepeated);
      });
      _ut.test('test_implementsRepeated_3times', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsRepeated_3times);
      });
      _ut.test('test_implementsSuperClass', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsSuperClass);
      });
      _ut.test('test_implementsSuperClass_Object', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implementsSuperClass_Object);
      });
      _ut.test('test_implicitThisReferenceInInitializer_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_field);
      });
      _ut.test('test_implicitThisReferenceInInitializer_field2', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_field2);
      });
      _ut.test('test_implicitThisReferenceInInitializer_invocation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_invocation);
      });
      _ut.test('test_implicitThisReferenceInInitializer_invocationInStatic', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_invocationInStatic);
      });
      _ut.test('test_implicitThisReferenceInInitializer_redirectingConstructorInvocation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_redirectingConstructorInvocation);
      });
      _ut.test('test_implicitThisReferenceInInitializer_superConstructorInvocation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_implicitThisReferenceInInitializer_superConstructorInvocation);
      });
      _ut.test('test_importInternalLibrary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_importInternalLibrary);
      });
      _ut.test('test_importInternalLibrary_collection', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_importInternalLibrary_collection);
      });
      _ut.test('test_importOfNonLibrary', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_importOfNonLibrary);
      });
      _ut.test('test_inconsistentCaseExpressionTypes', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_inconsistentCaseExpressionTypes);
      });
      _ut.test('test_inconsistentCaseExpressionTypes_repeated', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_inconsistentCaseExpressionTypes_repeated);
      });
      _ut.test('test_initializerForNonExistant_initializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_initializerForNonExistant_initializer);
      });
      _ut.test('test_initializerForStaticField', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_initializerForStaticField);
      });
      _ut.test('test_initializingFormalForNonExistantField', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_initializingFormalForNonExistantField);
      });
      _ut.test('test_initializingFormalForNonExistantField_notInEnclosingClass', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_initializingFormalForNonExistantField_notInEnclosingClass);
      });
      _ut.test('test_initializingFormalForNonExistantField_optional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_initializingFormalForNonExistantField_optional);
      });
      _ut.test('test_initializingFormalForNonExistantField_synthetic', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_initializingFormalForNonExistantField_synthetic);
      });
      _ut.test('test_initializingFormalForStaticField', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_initializingFormalForStaticField);
      });
      _ut.test('test_instanceMemberAccessFromStatic_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_instanceMemberAccessFromStatic_field);
      });
      _ut.test('test_instanceMemberAccessFromStatic_getter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_instanceMemberAccessFromStatic_getter);
      });
      _ut.test('test_instanceMemberAccessFromStatic_method', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_instanceMemberAccessFromStatic_method);
      });
      _ut.test('test_invalidAnnotation_getter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_getter);
      });
      _ut.test('test_invalidAnnotation_importWithPrefix_getter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_importWithPrefix_getter);
      });
      _ut.test('test_invalidAnnotation_importWithPrefix_notConstantVariable', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_importWithPrefix_notConstantVariable);
      });
      _ut.test('test_invalidAnnotation_importWithPrefix_notVariableOrConstructorInvocation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_importWithPrefix_notVariableOrConstructorInvocation);
      });
      _ut.test('test_invalidAnnotation_notConstantVariable', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_notConstantVariable);
      });
      _ut.test('test_invalidAnnotation_notVariableOrConstructorInvocation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_notVariableOrConstructorInvocation);
      });
      _ut.test('test_invalidAnnotation_staticMethodReference', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_staticMethodReference);
      });
      _ut.test('test_invalidAnnotation_unresolved_identifier', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_unresolved_identifier);
      });
      _ut.test('test_invalidAnnotation_unresolved_invocation', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_unresolved_invocation);
      });
      _ut.test('test_invalidAnnotation_unresolved_prefixedIdentifier', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidAnnotation_unresolved_prefixedIdentifier);
      });
      _ut.test('test_invalidConstructorName_notEnclosingClassName_defined', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidConstructorName_notEnclosingClassName_defined);
      });
      _ut.test('test_invalidConstructorName_notEnclosingClassName_undefined', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidConstructorName_notEnclosingClassName_undefined);
      });
      _ut.test('test_invalidFactoryNameNotAClass_notClassName', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidFactoryNameNotAClass_notClassName);
      });
      _ut.test('test_invalidFactoryNameNotAClass_notEnclosingClassName', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidFactoryNameNotAClass_notEnclosingClassName);
      });
      _ut.test('test_invalidReferenceToThis_factoryConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_factoryConstructor);
      });
      _ut.test('test_invalidReferenceToThis_instanceVariableInitializer_inConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_instanceVariableInitializer_inConstructor);
      });
      _ut.test('test_invalidReferenceToThis_instanceVariableInitializer_inDeclaration', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_instanceVariableInitializer_inDeclaration);
      });
      _ut.test('test_invalidReferenceToThis_staticMethod', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_staticMethod);
      });
      _ut.test('test_invalidReferenceToThis_staticVariableInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_staticVariableInitializer);
      });
      _ut.test('test_invalidReferenceToThis_superInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_superInitializer);
      });
      _ut.test('test_invalidReferenceToThis_topLevelFunction', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_topLevelFunction);
      });
      _ut.test('test_invalidReferenceToThis_variableInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidReferenceToThis_variableInitializer);
      });
      _ut.test('test_invalidTypeArgumentInConstList', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidTypeArgumentInConstList);
      });
      _ut.test('test_invalidTypeArgumentInConstMap', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidTypeArgumentInConstMap);
      });
      _ut.test('test_invalidUri_export', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidUri_export);
      });
      _ut.test('test_invalidUri_import', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidUri_import);
      });
      _ut.test('test_invalidUri_part', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_invalidUri_part);
      });
      _ut.test('test_labelInOuterScope', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_labelInOuterScope);
      });
      _ut.test('test_labelUndefined_break', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_labelUndefined_break);
      });
      _ut.test('test_labelUndefined_continue', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_labelUndefined_continue);
      });
      _ut.test('test_listElementTypeNotAssignable', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_listElementTypeNotAssignable);
      });
      _ut.test('test_mapKeyTypeNotAssignable', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mapKeyTypeNotAssignable);
      });
      _ut.test('test_mapValueTypeNotAssignable', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mapValueTypeNotAssignable);
      });
      _ut.test('test_memberWithClassName_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_memberWithClassName_field);
      });
      _ut.test('test_memberWithClassName_field2', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_memberWithClassName_field2);
      });
      _ut.test('test_memberWithClassName_getter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_memberWithClassName_getter);
      });
      _ut.test('test_memberWithClassName_method', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_memberWithClassName_method);
      });
      _ut.test('test_methodAndGetterWithSameName', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_methodAndGetterWithSameName);
      });
      _ut.test('test_mixinDeclaresConstructor_classDeclaration', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinDeclaresConstructor_classDeclaration);
      });
      _ut.test('test_mixinDeclaresConstructor_typeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinDeclaresConstructor_typeAlias);
      });
      _ut.test('test_mixinInheritsFromNotObject_classDeclaration_extends', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinInheritsFromNotObject_classDeclaration_extends);
      });
      _ut.test('test_mixinInheritsFromNotObject_classDeclaration_with', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinInheritsFromNotObject_classDeclaration_with);
      });
      _ut.test('test_mixinInheritsFromNotObject_typeAlias_extends', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinInheritsFromNotObject_typeAlias_extends);
      });
      _ut.test('test_mixinInheritsFromNotObject_typeAlias_with', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinInheritsFromNotObject_typeAlias_with);
      });
      _ut.test('test_mixinOfDisallowedClass_Null', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfDisallowedClass_Null);
      });
      _ut.test('test_mixinOfDisallowedClass_String', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfDisallowedClass_String);
      });
      _ut.test('test_mixinOfDisallowedClass_bool', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfDisallowedClass_bool);
      });
      _ut.test('test_mixinOfDisallowedClass_double', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfDisallowedClass_double);
      });
      _ut.test('test_mixinOfDisallowedClass_int', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfDisallowedClass_int);
      });
      _ut.test('test_mixinOfDisallowedClass_num', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfDisallowedClass_num);
      });
      _ut.test('test_mixinOfNonClass_class', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfNonClass_class);
      });
      _ut.test('test_mixinOfNonClass_typeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinOfNonClass_typeAlias);
      });
      _ut.test('test_mixinReferencesSuper', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinReferencesSuper);
      });
      _ut.test('test_mixinWithNonClassSuperclass_class', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinWithNonClassSuperclass_class);
      });
      _ut.test('test_mixinWithNonClassSuperclass_typeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_mixinWithNonClassSuperclass_typeAlias);
      });
      _ut.test('test_multipleRedirectingConstructorInvocations', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_multipleRedirectingConstructorInvocations);
      });
      _ut.test('test_multipleSuperInitializers', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_multipleSuperInitializers);
      });
      _ut.test('test_nativeClauseInNonSDKCode', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nativeClauseInNonSDKCode);
      });
      _ut.test('test_nativeFunctionBodyInNonSDKCode_function', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nativeFunctionBodyInNonSDKCode_function);
      });
      _ut.test('test_nativeFunctionBodyInNonSDKCode_method', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nativeFunctionBodyInNonSDKCode_method);
      });
      _ut.test('test_noAnnotationConstructorArguments', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_noAnnotationConstructorArguments);
      });
      _ut.test('test_noDefaultSuperConstructorExplicit', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_noDefaultSuperConstructorExplicit);
      });
      _ut.test('test_noDefaultSuperConstructorImplicit_superHasParameters', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_noDefaultSuperConstructorImplicit_superHasParameters);
      });
      _ut.test('test_noDefaultSuperConstructorImplicit_superOnlyNamed', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_noDefaultSuperConstructorImplicit_superOnlyNamed);
      });
      _ut.test('test_nonConstCaseExpression', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstCaseExpression);
      });
      _ut.test('test_nonConstListElement', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstListElement);
      });
      _ut.test('test_nonConstMapAsExpressionStatement_begin', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstMapAsExpressionStatement_begin);
      });
      _ut.test('test_nonConstMapAsExpressionStatement_only', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstMapAsExpressionStatement_only);
      });
      _ut.test('test_nonConstMapKey', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstMapKey);
      });
      _ut.test('test_nonConstMapValue', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstMapValue);
      });
      _ut.test('test_nonConstValueInInitializer_binary_notBool_left', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_notBool_left);
      });
      _ut.test('test_nonConstValueInInitializer_binary_notBool_right', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_notBool_right);
      });
      _ut.test('test_nonConstValueInInitializer_binary_notInt', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_notInt);
      });
      _ut.test('test_nonConstValueInInitializer_binary_notNum', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_binary_notNum);
      });
      _ut.test('test_nonConstValueInInitializer_field', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_field);
      });
      _ut.test('test_nonConstValueInInitializer_redirecting', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_redirecting);
      });
      _ut.test('test_nonConstValueInInitializer_super', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstValueInInitializer_super);
      });
      _ut.test('test_nonConstantAnnotationConstructor_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantAnnotationConstructor_named);
      });
      _ut.test('test_nonConstantAnnotationConstructor_unnamed', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantAnnotationConstructor_unnamed);
      });
      _ut.test('test_nonConstantDefaultValue_function_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_function_named);
      });
      _ut.test('test_nonConstantDefaultValue_function_positional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_function_positional);
      });
      _ut.test('test_nonConstantDefaultValue_inConstructor_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_inConstructor_named);
      });
      _ut.test('test_nonConstantDefaultValue_inConstructor_positional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_inConstructor_positional);
      });
      _ut.test('test_nonConstantDefaultValue_method_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_method_named);
      });
      _ut.test('test_nonConstantDefaultValue_method_positional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonConstantDefaultValue_method_positional);
      });
      _ut.test('test_nonGenerativeConstructor_explicit', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonGenerativeConstructor_explicit);
      });
      _ut.test('test_nonGenerativeConstructor_implicit', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonGenerativeConstructor_implicit);
      });
      _ut.test('test_nonGenerativeConstructor_implicit2', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_nonGenerativeConstructor_implicit2);
      });
      _ut.test('test_notEnoughRequiredArguments_const', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_notEnoughRequiredArguments_const);
      });
      _ut.test('test_notEnoughRequiredArguments_const_super', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_notEnoughRequiredArguments_const_super);
      });
      _ut.test('test_optionalParameterInOperator_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_optionalParameterInOperator_named);
      });
      _ut.test('test_optionalParameterInOperator_positional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_optionalParameterInOperator_positional);
      });
      _ut.test('test_partOfNonPart', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_partOfNonPart);
      });
      _ut.test('test_prefixCollidesWithTopLevelMembers_functionTypeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_prefixCollidesWithTopLevelMembers_functionTypeAlias);
      });
      _ut.test('test_prefixCollidesWithTopLevelMembers_topLevelFunction', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_prefixCollidesWithTopLevelMembers_topLevelFunction);
      });
      _ut.test('test_prefixCollidesWithTopLevelMembers_topLevelVariable', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_prefixCollidesWithTopLevelMembers_topLevelVariable);
      });
      _ut.test('test_prefixCollidesWithTopLevelMembers_type', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_prefixCollidesWithTopLevelMembers_type);
      });
      _ut.test('test_privateOptionalParameter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_privateOptionalParameter);
      });
      _ut.test('test_privateOptionalParameter_fieldFormal', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_privateOptionalParameter_fieldFormal);
      });
      _ut.test('test_privateOptionalParameter_withDefaultValue', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_privateOptionalParameter_withDefaultValue);
      });
      _ut.test('test_recursiveConstructorRedirect', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveConstructorRedirect);
      });
      _ut.test('test_recursiveConstructorRedirect_directSelfReference', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveConstructorRedirect_directSelfReference);
      });
      _ut.test('test_recursiveFactoryRedirect', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveFactoryRedirect);
      });
      _ut.test('test_recursiveFactoryRedirect_directSelfReference', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveFactoryRedirect_directSelfReference);
      });
      _ut.test('test_recursiveFactoryRedirect_generic', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveFactoryRedirect_generic);
      });
      _ut.test('test_recursiveFactoryRedirect_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveFactoryRedirect_named);
      });
      _ut.test('test_recursiveFactoryRedirect_outsideCycle', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveFactoryRedirect_outsideCycle);
      });
      _ut.test('test_recursiveInterfaceInheritanceBaseCaseExtends', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritanceBaseCaseExtends);
      });
      _ut.test('test_recursiveInterfaceInheritanceBaseCaseImplements', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritanceBaseCaseImplements);
      });
      _ut.test('test_recursiveInterfaceInheritanceBaseCaseImplements_typeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritanceBaseCaseImplements_typeAlias);
      });
      _ut.test('test_recursiveInterfaceInheritance_extends', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritance_extends);
      });
      _ut.test('test_recursiveInterfaceInheritance_extends_implements', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritance_extends_implements);
      });
      _ut.test('test_recursiveInterfaceInheritance_implements', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritance_implements);
      });
      _ut.test('test_recursiveInterfaceInheritance_tail', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritance_tail);
      });
      _ut.test('test_recursiveInterfaceInheritance_tail2', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritance_tail2);
      });
      _ut.test('test_recursiveInterfaceInheritance_tail3', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_recursiveInterfaceInheritance_tail3);
      });
      _ut.test('test_redirectToMissingConstructor_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_redirectToMissingConstructor_named);
      });
      _ut.test('test_redirectToMissingConstructor_unnamed', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_redirectToMissingConstructor_unnamed);
      });
      _ut.test('test_redirectToNonClass_notAType', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_redirectToNonClass_notAType);
      });
      _ut.test('test_redirectToNonClass_undefinedIdentifier', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_redirectToNonClass_undefinedIdentifier);
      });
      _ut.test('test_redirectToNonConstConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_redirectToNonConstConstructor);
      });
      _ut.test('test_referencedBeforeDeclaration_hideInBlock_function', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_referencedBeforeDeclaration_hideInBlock_function);
      });
      _ut.test('test_referencedBeforeDeclaration_hideInBlock_local', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_referencedBeforeDeclaration_hideInBlock_local);
      });
      _ut.test('test_referencedBeforeDeclaration_hideInBlock_subBlock', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_referencedBeforeDeclaration_hideInBlock_subBlock);
      });
      _ut.test('test_rethrowOutsideCatch', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_rethrowOutsideCatch);
      });
      _ut.test('test_returnInGenerativeConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_returnInGenerativeConstructor);
      });
      _ut.test('test_returnInGenerativeConstructor_expressionFunctionBody', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_returnInGenerativeConstructor_expressionFunctionBody);
      });
      _ut.test('test_superInInvalidContext_binaryExpression', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_binaryExpression);
      });
      _ut.test('test_superInInvalidContext_constructorFieldInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_constructorFieldInitializer);
      });
      _ut.test('test_superInInvalidContext_factoryConstructor', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_factoryConstructor);
      });
      _ut.test('test_superInInvalidContext_instanceVariableInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_instanceVariableInitializer);
      });
      _ut.test('test_superInInvalidContext_staticMethod', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_staticMethod);
      });
      _ut.test('test_superInInvalidContext_staticVariableInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_staticVariableInitializer);
      });
      _ut.test('test_superInInvalidContext_topLevelFunction', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_topLevelFunction);
      });
      _ut.test('test_superInInvalidContext_topLevelVariableInitializer', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInInvalidContext_topLevelVariableInitializer);
      });
      _ut.test('test_superInRedirectingConstructor_redirectionSuper', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInRedirectingConstructor_redirectionSuper);
      });
      _ut.test('test_superInRedirectingConstructor_superRedirection', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_superInRedirectingConstructor_superRedirection);
      });
      _ut.test('test_typeAliasCannotReferenceItself_parameterType_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_parameterType_named);
      });
      _ut.test('test_typeAliasCannotReferenceItself_parameterType_positional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_parameterType_positional);
      });
      _ut.test('test_typeAliasCannotReferenceItself_parameterType_required', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_parameterType_required);
      });
      _ut.test('test_typeAliasCannotReferenceItself_parameterType_typeArgument', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_parameterType_typeArgument);
      });
      _ut.test('test_typeAliasCannotReferenceItself_returnClass_withTypeAlias', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_returnClass_withTypeAlias);
      });
      _ut.test('test_typeAliasCannotReferenceItself_returnType', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_returnType);
      });
      _ut.test('test_typeAliasCannotReferenceItself_returnType_indirect', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_returnType_indirect);
      });
      _ut.test('test_typeAliasCannotReferenceItself_typeVariableBounds', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotReferenceItself_typeVariableBounds);
      });
      _ut.test('test_typeAliasCannotRereferenceItself_mixin_direct', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotRereferenceItself_mixin_direct);
      });
      _ut.test('test_typeAliasCannotRereferenceItself_mixin_indirect', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeAliasCannotRereferenceItself_mixin_indirect);
      });
      _ut.test('test_typeArgumentNotMatchingBounds_const', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_typeArgumentNotMatchingBounds_const);
      });
      _ut.test('test_undefinedClass_const', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedClass_const);
      });
      _ut.test('test_undefinedConstructorInInitializer_explicit_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_explicit_named);
      });
      _ut.test('test_undefinedConstructorInInitializer_explicit_unnamed', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_explicit_unnamed);
      });
      _ut.test('test_undefinedConstructorInInitializer_implicit', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedConstructorInInitializer_implicit);
      });
      _ut.test('test_undefinedFunction', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedFunction);
      });
      _ut.test('test_undefinedFunction_hasImportPrefix', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedFunction_hasImportPrefix);
      });
      _ut.test('test_undefinedFunction_inCatch', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedFunction_inCatch);
      });
      _ut.test('test_undefinedNamedParameter', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_undefinedNamedParameter);
      });
      _ut.test('test_uriDoesNotExist_export', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_uriDoesNotExist_export);
      });
      _ut.test('test_uriDoesNotExist_import', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_uriDoesNotExist_import);
      });
      _ut.test('test_uriDoesNotExist_part', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_uriDoesNotExist_part);
      });
      _ut.test('test_uriWithInterpolation_constant', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_uriWithInterpolation_constant);
      });
      _ut.test('test_uriWithInterpolation_nonConstant', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_uriWithInterpolation_nonConstant);
      });
      _ut.test('test_wrongNumberOfParametersForOperator1', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForOperator1);
      });
      _ut.test('test_wrongNumberOfParametersForOperator_minus', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForOperator_minus);
      });
      _ut.test('test_wrongNumberOfParametersForOperator_tilde', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForOperator_tilde);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_function_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_function_named);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_function_optional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_function_optional);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_function_tooFew', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_function_tooFew);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_function_tooMany', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_function_tooMany);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_method_named', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_method_named);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_method_optional', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_method_optional);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_method_tooFew', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_method_tooFew);
      });
      _ut.test('test_wrongNumberOfParametersForSetter_method_tooMany', () {
        final __test = new CompileTimeErrorCodeTest();
        runJUnitTest(__test, __test.test_wrongNumberOfParametersForSetter_method_tooMany);
      });
    });
  }
}
/**
 * Instances of the class `StaticTypeVerifier` verify that all of the nodes in an AST
 * structure that should have a static type associated with them do have a static type.
 */
class StaticTypeVerifier extends GeneralizingASTVisitor<Object> {

  /**
   * A list containing all of the AST Expression nodes that were not resolved.
   */
  List<Expression> _unresolvedExpressions = new List<Expression>();

  /**
   * A list containing all of the AST Expression nodes for which a propagated type was computed but
   * where that type was not more specific than the static type.
   */
  List<Expression> _invalidlyPropagatedExpressions = new List<Expression>();

  /**
   * A list containing all of the AST TypeName nodes that were not resolved.
   */
  List<TypeName> _unresolvedTypes = new List<TypeName>();

  /**
   * Counter for the number of Expression nodes visited that are resolved.
   */
  int _resolvedExpressionCount = 0;

  /**
   * Counter for the number of Expression nodes visited that have propagated type information.
   */
  int _propagatedExpressionCount = 0;

  /**
   * Counter for the number of TypeName nodes visited that are resolved.
   */
  int _resolvedTypeCount = 0;

  /**
   * Assert that all of the visited nodes have a static type associated with them.
   */
  void assertResolved() {
    if (!_unresolvedExpressions.isEmpty || !_unresolvedTypes.isEmpty) {
      PrintStringWriter writer = new PrintStringWriter();
      int unresolvedTypeCount = _unresolvedTypes.length;
      if (unresolvedTypeCount > 0) {
        writer.print("Failed to resolve ");
        writer.print(unresolvedTypeCount);
        writer.print(" of ");
        writer.print(_resolvedTypeCount + unresolvedTypeCount);
        writer.println(" type names:");
        for (TypeName identifier in _unresolvedTypes) {
          writer.print("  ");
          writer.print(identifier.toString());
          writer.print(" (");
          writer.print(getFileName(identifier));
          writer.print(" : ");
          writer.print(identifier.offset);
          writer.println(")");
        }
      }
      int unresolvedExpressionCount = _unresolvedExpressions.length;
      if (unresolvedExpressionCount > 0) {
        writer.println("Failed to resolve ");
        writer.print(unresolvedExpressionCount);
        writer.print(" of ");
        writer.print(_resolvedExpressionCount + unresolvedExpressionCount);
        writer.println(" expressions:");
        for (Expression expression in _unresolvedExpressions) {
          writer.print("  ");
          writer.print(expression.toString());
          writer.print(" (");
          writer.print(getFileName(expression));
          writer.print(" : ");
          writer.print(expression.offset);
          writer.println(")");
        }
      }
      int invalidlyPropagatedExpressionCount = _invalidlyPropagatedExpressions.length;
      if (invalidlyPropagatedExpressionCount > 0) {
        writer.println("Incorrectly propagated ");
        writer.print(invalidlyPropagatedExpressionCount);
        writer.print(" of ");
        writer.print(_propagatedExpressionCount);
        writer.println(" expressions:");
        for (Expression expression in _invalidlyPropagatedExpressions) {
          writer.print("  ");
          writer.print(expression.toString());
          writer.print(" [");
          writer.print(expression.staticType.displayName);
          writer.print(", ");
          writer.print(expression.propagatedType.displayName);
          writer.println("]");
          writer.print("    ");
          writer.print(getFileName(expression));
          writer.print(" : ");
          writer.print(expression.offset);
          writer.println(")");
        }
      }
      JUnitTestCase.fail(writer.toString());
    }
  }
  Object visitBreakStatement(BreakStatement node) => null;
  Object visitCommentReference(CommentReference node) => null;
  Object visitContinueStatement(ContinueStatement node) => null;
  Object visitExportDirective(ExportDirective node) => null;
  Object visitExpression(Expression node) {
    node.visitChildren(this);
    Type2 staticType = node.staticType;
    if (staticType == null) {
      _unresolvedExpressions.add(node);
    } else {
      _resolvedExpressionCount++;
      Type2 propagatedType = node.propagatedType;
      if (propagatedType != null) {
        _propagatedExpressionCount++;
        if (!propagatedType.isMoreSpecificThan(staticType)) {
          _invalidlyPropagatedExpressions.add(node);
        }
      }
    }
    return null;
  }
  Object visitImportDirective(ImportDirective node) => null;
  Object visitLabel(Label node) => null;
  Object visitLibraryIdentifier(LibraryIdentifier node) => null;
  Object visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (node.staticType == null && identical(node.prefix.staticType, DynamicTypeImpl.instance)) {
      return null;
    }
    return super.visitPrefixedIdentifier(node);
  }
  Object visitSimpleIdentifier(SimpleIdentifier node) {
    ASTNode parent = node.parent;
    if (parent is MethodInvocation && identical(node, ((parent as MethodInvocation)).methodName)) {
      return null;
    } else if (parent is RedirectingConstructorInvocation && identical(node, ((parent as RedirectingConstructorInvocation)).constructorName)) {
      return null;
    } else if (parent is SuperConstructorInvocation && identical(node, ((parent as SuperConstructorInvocation)).constructorName)) {
      return null;
    } else if (parent is ConstructorName && identical(node, ((parent as ConstructorName)).name)) {
      return null;
    } else if (parent is ConstructorFieldInitializer && identical(node, ((parent as ConstructorFieldInitializer)).fieldName)) {
      return null;
    } else if (node.staticElement is PrefixElement) {
      return null;
    }
    return super.visitSimpleIdentifier(node);
  }
  Object visitTypeName(TypeName node) {
    if (node.type == null) {
      _unresolvedTypes.add(node);
    } else {
      _resolvedTypeCount++;
    }
    return null;
  }
  String getFileName(ASTNode node) {
    if (node != null) {
      ASTNode root = node.root;
      if (root is CompilationUnit) {
        CompilationUnit rootCU = root as CompilationUnit;
        if (rootCU.element != null) {
          return rootCU.element.source.fullName;
        } else {
          return "<unknown file- CompilationUnit.getElement() returned null>";
        }
      } else {
        return "<unknown file- CompilationUnit.getRoot() is not a CompilationUnit>";
      }
    }
    return "<unknown file- ASTNode is null>";
  }
}
/**
 * The class `StrictModeTest` contains tests to ensure that the correct errors and warnings
 * are reported when the analysis engine is run in strict mode.
 */
class StrictModeTest extends ResolverTestCase {
  void fail_for() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(List<int> list) {",
        "  num sum = 0;",
        "  for (num i = 0; i < list.length; i++) {",
        "    sum += list[i];",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void setUp() {
    super.setUp();
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    options.strictMode = true;
    options.hint = false;
    analysisContext.analysisOptions = options;
  }
  void test_assert_is() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  assert (n is int);",
        "  return n & 0x0F;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_conditional_and_is() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  return (n is int && n > 0) ? n & 0x0F : 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_conditional_is() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  return (n is int) ? n & 0x0F : 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_conditional_isNot() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  return (n is! int) ? 0 : n & 0x0F;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_conditional_or_is() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  return (n is! int || n < 0) ? 0 : n & 0x0F;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_forEach() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(List<int> list) {",
        "  num sum = 0;",
        "  for (num n in list) {",
        "    sum += n & 0x0F;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_if_and_is() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  if (n is int && n > 0) {",
        "    return n & 0x0F;",
        "  }",
        "  return 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_if_is() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  if (n is int) {",
        "    return n & 0x0F;",
        "  }",
        "  return 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_if_isNot() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  if (n is! int) {",
        "    return 0;",
        "  } else {",
        "    return n & 0x0F;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_if_isNot_abrupt() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  if (n is! int) {",
        "    return 0;",
        "  }",
        "  return n & 0x0F;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_if_or_is() {
    Source source = addSource(EngineTestCase.createSource([
        "int f(num n) {",
        "  if (n is! int || n < 0) {",
        "    return 0;",
        "  } else {",
        "    return n & 0x0F;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  void test_localVar() {
    Source source = addSource(EngineTestCase.createSource(["int f() {", "  num n = 1234;", "  return n & 0x0F;", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.UNDEFINED_OPERATOR]);
  }
  static dartSuite() {
    _ut.group('StrictModeTest', () {
      _ut.test('test_assert_is', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_assert_is);
      });
      _ut.test('test_conditional_and_is', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_conditional_and_is);
      });
      _ut.test('test_conditional_is', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_conditional_is);
      });
      _ut.test('test_conditional_isNot', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_conditional_isNot);
      });
      _ut.test('test_conditional_or_is', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_conditional_or_is);
      });
      _ut.test('test_forEach', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_forEach);
      });
      _ut.test('test_if_and_is', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_if_and_is);
      });
      _ut.test('test_if_is', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_if_is);
      });
      _ut.test('test_if_isNot', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_if_isNot);
      });
      _ut.test('test_if_isNot_abrupt', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_if_isNot_abrupt);
      });
      _ut.test('test_if_or_is', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_if_or_is);
      });
      _ut.test('test_localVar', () {
        final __test = new StrictModeTest();
        runJUnitTest(__test, __test.test_localVar);
      });
    });
  }
}
class ElementResolverTest extends EngineTestCase {

  /**
   * The error listener to which errors will be reported.
   */
  GatheringErrorListener _listener;

  /**
   * The type provider used to access the types.
   */
  TestTypeProvider _typeProvider;

  /**
   * The library containing the code being resolved.
   */
  LibraryElementImpl _definingLibrary;

  /**
   * The resolver visitor that maintains the state for the resolver.
   */
  ResolverVisitor _visitor;

  /**
   * The resolver being used to resolve the test cases.
   */
  ElementResolver _resolver;
  void fail_visitExportDirective_combinators() {
    JUnitTestCase.fail("Not yet tested");
    ExportDirective directive = ASTFactory.exportDirective2(null, [ASTFactory.hideCombinator2(["A"])]);
    resolveNode(directive, []);
    _listener.assertNoErrors();
  }
  void fail_visitFunctionExpressionInvocation() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitImportDirective_combinators_noPrefix() {
    JUnitTestCase.fail("Not yet tested");
    ImportDirective directive = ASTFactory.importDirective2(null, null, [ASTFactory.showCombinator2(["A"])]);
    resolveNode(directive, []);
    _listener.assertNoErrors();
  }
  void fail_visitImportDirective_combinators_prefix() {
    JUnitTestCase.fail("Not yet tested");
    String prefixName = "p";
    _definingLibrary.imports = <ImportElement> [ElementFactory.importFor(null, ElementFactory.prefix(prefixName), [])];
    ImportDirective directive = ASTFactory.importDirective2(null, prefixName, [
        ASTFactory.showCombinator2(["A"]),
        ASTFactory.hideCombinator2(["B"])]);
    resolveNode(directive, []);
    _listener.assertNoErrors();
  }
  void fail_visitRedirectingConstructorInvocation() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void setUp() {
    _listener = new GatheringErrorListener();
    _typeProvider = new TestTypeProvider();
    _resolver = createResolver();
  }
  void test_lookUpMethodInInterfaces() {
    InterfaceType intType = _typeProvider.intType;
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    MethodElement operator = ElementFactory.methodElement("[]", intType, [intType]);
    classA.methods = <MethodElement> [operator];
    ClassElementImpl classB = ElementFactory.classElement2("B", []);
    classB.interfaces = <InterfaceType> [classA.type];
    ClassElementImpl classC = ElementFactory.classElement2("C", []);
    classC.mixins = <InterfaceType> [classB.type];
    ClassElementImpl classD = ElementFactory.classElement("D", classC.type, []);
    SimpleIdentifier array = ASTFactory.identifier3("a");
    array.staticType = classD.type;
    IndexExpression expression = ASTFactory.indexExpression(array, ASTFactory.identifier3("i"));
    JUnitTestCase.assertSame(operator, resolve5(expression, []));
    _listener.assertNoErrors();
  }
  void test_visitAssignmentExpression_compound() {
    InterfaceType intType = _typeProvider.intType;
    SimpleIdentifier leftHandSide = ASTFactory.identifier3("a");
    leftHandSide.staticType = intType;
    AssignmentExpression assignment = ASTFactory.assignmentExpression(leftHandSide, TokenType.PLUS_EQ, ASTFactory.integer(1));
    resolveNode(assignment, []);
    JUnitTestCase.assertSame(getMethod(_typeProvider.numType, "+"), assignment.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitAssignmentExpression_simple() {
    AssignmentExpression expression = ASTFactory.assignmentExpression(ASTFactory.identifier3("x"), TokenType.EQ, ASTFactory.integer(0));
    resolveNode(expression, []);
    JUnitTestCase.assertNull(expression.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression() {
    InterfaceType numType = _typeProvider.numType;
    SimpleIdentifier left = ASTFactory.identifier3("i");
    left.staticType = numType;
    BinaryExpression expression = ASTFactory.binaryExpression(left, TokenType.PLUS, ASTFactory.identifier3("j"));
    resolveNode(expression, []);
    JUnitTestCase.assertEquals(getMethod(numType, "+"), expression.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitBreakStatement_withLabel() {
    String label = "loop";
    LabelElementImpl labelElement = new LabelElementImpl(ASTFactory.identifier3(label), false, false);
    BreakStatement statement = ASTFactory.breakStatement2(label);
    JUnitTestCase.assertSame(labelElement, resolve(statement, labelElement));
    _listener.assertNoErrors();
  }
  void test_visitBreakStatement_withoutLabel() {
    BreakStatement statement = ASTFactory.breakStatement();
    resolveStatement(statement, null);
    _listener.assertNoErrors();
  }
  void test_visitConstructorName_named() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String constructorName = "a";
    ConstructorElement constructor = ElementFactory.constructorElement(classA, constructorName);
    classA.constructors = <ConstructorElement> [constructor];
    ConstructorName name = ASTFactory.constructorName(ASTFactory.typeName(classA, []), constructorName);
    resolveNode(name, []);
    JUnitTestCase.assertSame(constructor, name.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitConstructorName_unnamed() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String constructorName = null;
    ConstructorElement constructor = ElementFactory.constructorElement(classA, constructorName);
    classA.constructors = <ConstructorElement> [constructor];
    ConstructorName name = ASTFactory.constructorName(ASTFactory.typeName(classA, []), constructorName);
    resolveNode(name, []);
    JUnitTestCase.assertSame(constructor, name.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitContinueStatement_withLabel() {
    String label = "loop";
    LabelElementImpl labelElement = new LabelElementImpl(ASTFactory.identifier3(label), false, false);
    ContinueStatement statement = ASTFactory.continueStatement2(label);
    JUnitTestCase.assertSame(labelElement, resolve3(statement, labelElement));
    _listener.assertNoErrors();
  }
  void test_visitContinueStatement_withoutLabel() {
    ContinueStatement statement = ASTFactory.continueStatement();
    resolveStatement(statement, null);
    _listener.assertNoErrors();
  }
  void test_visitExportDirective_noCombinators() {
    ExportDirective directive = ASTFactory.exportDirective2(null, []);
    directive.element = ElementFactory.exportFor(ElementFactory.library(_definingLibrary.context, "lib"), []);
    resolveNode(directive, []);
    _listener.assertNoErrors();
  }
  void test_visitFieldFormalParameter() {
    InterfaceType intType = _typeProvider.intType;
    String fieldName = "f";
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    classA.fields = <FieldElement> [ElementFactory.fieldElement(fieldName, false, false, false, intType)];
    FieldFormalParameter parameter = ASTFactory.fieldFormalParameter3(fieldName);
    parameter.identifier.staticElement = ElementFactory.fieldFormalParameter(parameter.identifier);
    resolveInClass(parameter, classA);
    JUnitTestCase.assertSame(intType, parameter.element.type);
  }
  void test_visitImportDirective_noCombinators_noPrefix() {
    ImportDirective directive = ASTFactory.importDirective2(null, null, []);
    directive.element = ElementFactory.importFor(ElementFactory.library(_definingLibrary.context, "lib"), null, []);
    resolveNode(directive, []);
    _listener.assertNoErrors();
  }
  void test_visitImportDirective_noCombinators_prefix() {
    String prefixName = "p";
    ImportElement importElement = ElementFactory.importFor(ElementFactory.library(_definingLibrary.context, "lib"), ElementFactory.prefix(prefixName), []);
    _definingLibrary.imports = <ImportElement> [importElement];
    ImportDirective directive = ASTFactory.importDirective2(null, prefixName, []);
    directive.element = importElement;
    resolveNode(directive, []);
    _listener.assertNoErrors();
  }
  void test_visitIndexExpression_get() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    InterfaceType intType = _typeProvider.intType;
    MethodElement getter = ElementFactory.methodElement("[]", intType, [intType]);
    classA.methods = <MethodElement> [getter];
    SimpleIdentifier array = ASTFactory.identifier3("a");
    array.staticType = classA.type;
    IndexExpression expression = ASTFactory.indexExpression(array, ASTFactory.identifier3("i"));
    JUnitTestCase.assertSame(getter, resolve5(expression, []));
    _listener.assertNoErrors();
  }
  void test_visitIndexExpression_set() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    InterfaceType intType = _typeProvider.intType;
    MethodElement setter = ElementFactory.methodElement("[]=", intType, [intType]);
    classA.methods = <MethodElement> [setter];
    SimpleIdentifier array = ASTFactory.identifier3("a");
    array.staticType = classA.type;
    IndexExpression expression = ASTFactory.indexExpression(array, ASTFactory.identifier3("i"));
    ASTFactory.assignmentExpression(expression, TokenType.EQ, ASTFactory.integer(0));
    JUnitTestCase.assertSame(setter, resolve5(expression, []));
    _listener.assertNoErrors();
  }
  void test_visitInstanceCreationExpression_named() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String constructorName = "a";
    ConstructorElement constructor = ElementFactory.constructorElement(classA, constructorName);
    classA.constructors = <ConstructorElement> [constructor];
    ConstructorName name = ASTFactory.constructorName(ASTFactory.typeName(classA, []), constructorName);
    name.staticElement = constructor;
    InstanceCreationExpression creation = ASTFactory.instanceCreationExpression(Keyword.NEW, name, []);
    resolveNode(creation, []);
    JUnitTestCase.assertSame(constructor, creation.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitInstanceCreationExpression_unnamed() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String constructorName = null;
    ConstructorElement constructor = ElementFactory.constructorElement(classA, constructorName);
    classA.constructors = <ConstructorElement> [constructor];
    ConstructorName name = ASTFactory.constructorName(ASTFactory.typeName(classA, []), constructorName);
    name.staticElement = constructor;
    InstanceCreationExpression creation = ASTFactory.instanceCreationExpression(Keyword.NEW, name, []);
    resolveNode(creation, []);
    JUnitTestCase.assertSame(constructor, creation.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitInstanceCreationExpression_unnamed_namedParameter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String constructorName = null;
    ConstructorElementImpl constructor = ElementFactory.constructorElement(classA, constructorName);
    String parameterName = "a";
    ParameterElement parameter = ElementFactory.namedParameter(parameterName);
    constructor.parameters = <ParameterElement> [parameter];
    classA.constructors = <ConstructorElement> [constructor];
    ConstructorName name = ASTFactory.constructorName(ASTFactory.typeName(classA, []), constructorName);
    name.staticElement = constructor;
    InstanceCreationExpression creation = ASTFactory.instanceCreationExpression(Keyword.NEW, name, [ASTFactory.namedExpression2(parameterName, ASTFactory.integer(0))]);
    resolveNode(creation, []);
    JUnitTestCase.assertSame(constructor, creation.staticElement);
    JUnitTestCase.assertSame(parameter, ((creation.argumentList.arguments[0] as NamedExpression)).name.label.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitMethodInvocation() {
    InterfaceType numType = _typeProvider.numType;
    SimpleIdentifier left = ASTFactory.identifier3("i");
    left.staticType = numType;
    String methodName = "abs";
    MethodInvocation invocation = ASTFactory.methodInvocation(left, methodName, []);
    resolveNode(invocation, []);
    JUnitTestCase.assertSame(getMethod(numType, methodName), invocation.methodName.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitMethodInvocation_namedParameter() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String methodName = "m";
    String parameterName = "p";
    MethodElementImpl method = ElementFactory.methodElement(methodName, null, []);
    ParameterElement parameter = ElementFactory.namedParameter(parameterName);
    method.parameters = <ParameterElement> [parameter];
    classA.methods = <MethodElement> [method];
    SimpleIdentifier left = ASTFactory.identifier3("i");
    left.staticType = classA.type;
    MethodInvocation invocation = ASTFactory.methodInvocation(left, methodName, [ASTFactory.namedExpression2(parameterName, ASTFactory.integer(0))]);
    resolveNode(invocation, []);
    JUnitTestCase.assertSame(method, invocation.methodName.staticElement);
    JUnitTestCase.assertSame(parameter, ((invocation.argumentList.arguments[0] as NamedExpression)).name.label.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitPostfixExpression() {
    InterfaceType numType = _typeProvider.numType;
    SimpleIdentifier operand = ASTFactory.identifier3("i");
    operand.staticType = numType;
    PostfixExpression expression = ASTFactory.postfixExpression(operand, TokenType.PLUS_PLUS);
    resolveNode(expression, []);
    JUnitTestCase.assertEquals(getMethod(numType, "+"), expression.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitPrefixedIdentifier_dynamic() {
    Type2 dynamicType = _typeProvider.dynamicType;
    SimpleIdentifier target = ASTFactory.identifier3("a");
    VariableElementImpl variable = ElementFactory.localVariableElement(target);
    variable.type = dynamicType;
    target.staticElement = variable;
    target.staticType = dynamicType;
    PrefixedIdentifier identifier = ASTFactory.identifier(target, ASTFactory.identifier3("b"));
    resolveNode(identifier, []);
    JUnitTestCase.assertNull(identifier.staticElement);
    JUnitTestCase.assertNull(identifier.identifier.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitPrefixedIdentifier_nonDynamic() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "b";
    PropertyAccessorElement getter = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getter];
    SimpleIdentifier target = ASTFactory.identifier3("a");
    VariableElementImpl variable = ElementFactory.localVariableElement(target);
    variable.type = classA.type;
    target.staticElement = variable;
    target.staticType = classA.type;
    PrefixedIdentifier identifier = ASTFactory.identifier(target, ASTFactory.identifier3(getterName));
    resolveNode(identifier, []);
    JUnitTestCase.assertSame(getter, identifier.staticElement);
    JUnitTestCase.assertSame(getter, identifier.identifier.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitPrefixExpression() {
    InterfaceType numType = _typeProvider.numType;
    SimpleIdentifier operand = ASTFactory.identifier3("i");
    operand.staticType = numType;
    PrefixExpression expression = ASTFactory.prefixExpression(TokenType.PLUS_PLUS, operand);
    resolveNode(expression, []);
    JUnitTestCase.assertEquals(getMethod(numType, "+"), expression.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitPropertyAccess_getter_identifier() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "b";
    PropertyAccessorElement getter = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getter];
    SimpleIdentifier target = ASTFactory.identifier3("a");
    target.staticType = classA.type;
    PropertyAccess access = ASTFactory.propertyAccess2(target, getterName);
    resolveNode(access, []);
    JUnitTestCase.assertSame(getter, access.propertyName.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitPropertyAccess_getter_super() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String getterName = "b";
    PropertyAccessorElement getter = ElementFactory.getterElement(getterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [getter];
    SuperExpression target = ASTFactory.superExpression();
    target.staticType = ElementFactory.classElement("B", classA.type, []).type;
    PropertyAccess access = ASTFactory.propertyAccess2(target, getterName);
    ASTFactory.methodDeclaration2(null, null, null, null, ASTFactory.identifier3("m"), ASTFactory.formalParameterList([]), ASTFactory.expressionFunctionBody(access));
    resolveNode(access, []);
    JUnitTestCase.assertSame(getter, access.propertyName.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitPropertyAccess_setter_this() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String setterName = "b";
    PropertyAccessorElement setter = ElementFactory.setterElement(setterName, false, _typeProvider.intType);
    classA.accessors = <PropertyAccessorElement> [setter];
    ThisExpression target = ASTFactory.thisExpression();
    target.staticType = classA.type;
    PropertyAccess access = ASTFactory.propertyAccess2(target, setterName);
    ASTFactory.assignmentExpression(access, TokenType.EQ, ASTFactory.integer(0));
    resolveNode(access, []);
    JUnitTestCase.assertSame(setter, access.propertyName.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitSimpleIdentifier_classScope() {
    InterfaceType doubleType = _typeProvider.doubleType;
    String fieldName = "NAN";
    SimpleIdentifier node = ASTFactory.identifier3(fieldName);
    resolveInClass(node, doubleType.element);
    JUnitTestCase.assertEquals(getGetter(doubleType, fieldName), node.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitSimpleIdentifier_dynamic() {
    SimpleIdentifier node = ASTFactory.identifier3("dynamic");
    resolve4(node, []);
    JUnitTestCase.assertSame(_typeProvider.dynamicType.element, node.staticElement);
    JUnitTestCase.assertSame(_typeProvider.typeType, node.staticType);
    _listener.assertNoErrors();
  }
  void test_visitSimpleIdentifier_lexicalScope() {
    SimpleIdentifier node = ASTFactory.identifier3("i");
    VariableElementImpl element = ElementFactory.localVariableElement(node);
    JUnitTestCase.assertSame(element, resolve4(node, [element]));
    _listener.assertNoErrors();
  }
  void test_visitSimpleIdentifier_lexicalScope_field_setter() {
    InterfaceType intType = _typeProvider.intType;
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    String fieldName = "a";
    FieldElement field = ElementFactory.fieldElement(fieldName, false, false, false, intType);
    classA.fields = <FieldElement> [field];
    classA.accessors = <PropertyAccessorElement> [field.getter, field.setter];
    SimpleIdentifier node = ASTFactory.identifier3(fieldName);
    ASTFactory.assignmentExpression(node, TokenType.EQ, ASTFactory.integer(0));
    resolveInClass(node, classA);
    Element element = node.staticElement;
    EngineTestCase.assertInstanceOf(PropertyAccessorElement, element);
    JUnitTestCase.assertTrue(((element as PropertyAccessorElement)).isSetter);
    _listener.assertNoErrors();
  }
  void test_visitSuperConstructorInvocation() {
    ClassElementImpl superclass = ElementFactory.classElement2("A", []);
    ConstructorElementImpl superConstructor = ElementFactory.constructorElement(superclass, null);
    superclass.constructors = <ConstructorElement> [superConstructor];
    ClassElementImpl subclass = ElementFactory.classElement("B", superclass.type, []);
    ConstructorElementImpl subConstructor = ElementFactory.constructorElement(subclass, null);
    subclass.constructors = <ConstructorElement> [subConstructor];
    SuperConstructorInvocation invocation = ASTFactory.superConstructorInvocation([]);
    resolveInClass(invocation, subclass);
    JUnitTestCase.assertEquals(superConstructor, invocation.staticElement);
    _listener.assertNoErrors();
  }
  void test_visitSuperConstructorInvocation_namedParameter() {
    ClassElementImpl superclass = ElementFactory.classElement2("A", []);
    ConstructorElementImpl superConstructor = ElementFactory.constructorElement(superclass, null);
    String parameterName = "p";
    ParameterElement parameter = ElementFactory.namedParameter(parameterName);
    superConstructor.parameters = <ParameterElement> [parameter];
    superclass.constructors = <ConstructorElement> [superConstructor];
    ClassElementImpl subclass = ElementFactory.classElement("B", superclass.type, []);
    ConstructorElementImpl subConstructor = ElementFactory.constructorElement(subclass, null);
    subclass.constructors = <ConstructorElement> [subConstructor];
    SuperConstructorInvocation invocation = ASTFactory.superConstructorInvocation([ASTFactory.namedExpression2(parameterName, ASTFactory.integer(0))]);
    resolveInClass(invocation, subclass);
    JUnitTestCase.assertEquals(superConstructor, invocation.staticElement);
    JUnitTestCase.assertSame(parameter, ((invocation.argumentList.arguments[0] as NamedExpression)).name.label.staticElement);
    _listener.assertNoErrors();
  }

  /**
   * Create the resolver used by the tests.
   *
   * @return the resolver that was created
   */
  ElementResolver createResolver() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    ContentCache contentCache = new ContentCache();
    SourceFactory sourceFactory = new SourceFactory.con1(contentCache, [new DartUriResolver(DirectoryBasedDartSdk.defaultSdk)]);
    context.sourceFactory = sourceFactory;
    FileBasedSource source = new FileBasedSource.con1(contentCache, FileUtilities2.createFile("/test.dart"));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("test.dart");
    definingCompilationUnit.source = source;
    _definingLibrary = ElementFactory.library(context, "test");
    _definingLibrary.definingCompilationUnit = definingCompilationUnit;
    Library library = new Library(context, _listener, source);
    library.libraryElement = _definingLibrary;
    _visitor = new ResolverVisitor.con1(library, source, _typeProvider);
    try {
      return _visitor.elementResolver_J2DAccessor as ElementResolver;
    } on JavaException catch (exception) {
      throw new IllegalArgumentException("Could not create resolver", exception);
    }
  }

  /**
   * Return the element associated with the label of the given statement after the resolver has
   * resolved the statement.
   *
   * @param statement the statement to be resolved
   * @param labelElement the label element to be defined in the statement's label scope
   * @return the element to which the statement's label was resolved
   */
  Element resolve(BreakStatement statement, LabelElementImpl labelElement) {
    resolveStatement(statement, labelElement);
    return statement.label.staticElement;
  }

  /**
   * Return the element associated with the label of the given statement after the resolver has
   * resolved the statement.
   *
   * @param statement the statement to be resolved
   * @param labelElement the label element to be defined in the statement's label scope
   * @return the element to which the statement's label was resolved
   */
  Element resolve3(ContinueStatement statement, LabelElementImpl labelElement) {
    resolveStatement(statement, labelElement);
    return statement.label.staticElement;
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   *
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  Element resolve4(Identifier node, List<Element> definedElements) {
    resolveNode(node, definedElements);
    return node.staticElement;
  }

  /**
   * Return the element associated with the given expression after the resolver has resolved the
   * expression.
   *
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  Element resolve5(IndexExpression node, List<Element> definedElements) {
    resolveNode(node, definedElements);
    return node.staticElement;
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   *
   * @param node the expression to be resolved
   * @param enclosingClass the element representing the class enclosing the identifier
   * @return the element to which the expression was resolved
   */
  void resolveInClass(ASTNode node, ClassElement enclosingClass) {
    try {
      Scope outerScope = _visitor.nameScope_J2DAccessor as Scope;
      try {
        _visitor.enclosingClass_J2DAccessor = enclosingClass;
        EnclosedScope innerScope = new ClassScope(outerScope, enclosingClass);
        _visitor.nameScope_J2DAccessor = innerScope;
        node.accept(_resolver);
      } finally {
        _visitor.enclosingClass_J2DAccessor = null;
        _visitor.nameScope_J2DAccessor = outerScope;
      }
    } on JavaException catch (exception) {
      throw new IllegalArgumentException("Could not resolve node", exception);
    }
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   *
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  void resolveNode(ASTNode node, List<Element> definedElements) {
    try {
      Scope outerScope = _visitor.nameScope_J2DAccessor as Scope;
      try {
        EnclosedScope innerScope = new EnclosedScope(outerScope);
        for (Element element in definedElements) {
          innerScope.define(element);
        }
        _visitor.nameScope_J2DAccessor = innerScope;
        node.accept(_resolver);
      } finally {
        _visitor.nameScope_J2DAccessor = outerScope;
      }
    } on JavaException catch (exception) {
      throw new IllegalArgumentException("Could not resolve node", exception);
    }
  }

  /**
   * Return the element associated with the label of the given statement after the resolver has
   * resolved the statement.
   *
   * @param statement the statement to be resolved
   * @param labelElement the label element to be defined in the statement's label scope
   * @return the element to which the statement's label was resolved
   */
  void resolveStatement(Statement statement, LabelElementImpl labelElement) {
    try {
      LabelScope outerScope = _visitor.labelScope_J2DAccessor as LabelScope;
      try {
        LabelScope innerScope;
        if (labelElement == null) {
          innerScope = new LabelScope.con1(outerScope, false, false);
        } else {
          innerScope = new LabelScope.con2(outerScope, labelElement.name, labelElement);
        }
        _visitor.labelScope_J2DAccessor = innerScope;
        statement.accept(_resolver);
      } finally {
        _visitor.labelScope_J2DAccessor = outerScope;
      }
    } on JavaException catch (exception) {
      throw new IllegalArgumentException("Could not resolve node", exception);
    }
  }
  static dartSuite() {
    _ut.group('ElementResolverTest', () {
      _ut.test('test_lookUpMethodInInterfaces', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_lookUpMethodInInterfaces);
      });
      _ut.test('test_visitAssignmentExpression_compound', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitAssignmentExpression_compound);
      });
      _ut.test('test_visitAssignmentExpression_simple', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitAssignmentExpression_simple);
      });
      _ut.test('test_visitBinaryExpression', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression);
      });
      _ut.test('test_visitBreakStatement_withLabel', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitBreakStatement_withLabel);
      });
      _ut.test('test_visitBreakStatement_withoutLabel', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitBreakStatement_withoutLabel);
      });
      _ut.test('test_visitConstructorName_named', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitConstructorName_named);
      });
      _ut.test('test_visitConstructorName_unnamed', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitConstructorName_unnamed);
      });
      _ut.test('test_visitContinueStatement_withLabel', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitContinueStatement_withLabel);
      });
      _ut.test('test_visitContinueStatement_withoutLabel', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitContinueStatement_withoutLabel);
      });
      _ut.test('test_visitExportDirective_noCombinators', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitExportDirective_noCombinators);
      });
      _ut.test('test_visitFieldFormalParameter', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitFieldFormalParameter);
      });
      _ut.test('test_visitImportDirective_noCombinators_noPrefix', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitImportDirective_noCombinators_noPrefix);
      });
      _ut.test('test_visitImportDirective_noCombinators_prefix', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitImportDirective_noCombinators_prefix);
      });
      _ut.test('test_visitIndexExpression_get', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitIndexExpression_get);
      });
      _ut.test('test_visitIndexExpression_set', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitIndexExpression_set);
      });
      _ut.test('test_visitInstanceCreationExpression_named', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitInstanceCreationExpression_named);
      });
      _ut.test('test_visitInstanceCreationExpression_unnamed', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitInstanceCreationExpression_unnamed);
      });
      _ut.test('test_visitInstanceCreationExpression_unnamed_namedParameter', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitInstanceCreationExpression_unnamed_namedParameter);
      });
      _ut.test('test_visitMethodInvocation', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitMethodInvocation);
      });
      _ut.test('test_visitMethodInvocation_namedParameter', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitMethodInvocation_namedParameter);
      });
      _ut.test('test_visitPostfixExpression', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitPostfixExpression);
      });
      _ut.test('test_visitPrefixExpression', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitPrefixExpression);
      });
      _ut.test('test_visitPrefixedIdentifier_dynamic', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitPrefixedIdentifier_dynamic);
      });
      _ut.test('test_visitPrefixedIdentifier_nonDynamic', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitPrefixedIdentifier_nonDynamic);
      });
      _ut.test('test_visitPropertyAccess_getter_identifier', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitPropertyAccess_getter_identifier);
      });
      _ut.test('test_visitPropertyAccess_getter_super', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitPropertyAccess_getter_super);
      });
      _ut.test('test_visitPropertyAccess_setter_this', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitPropertyAccess_setter_this);
      });
      _ut.test('test_visitSimpleIdentifier_classScope', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitSimpleIdentifier_classScope);
      });
      _ut.test('test_visitSimpleIdentifier_dynamic', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitSimpleIdentifier_dynamic);
      });
      _ut.test('test_visitSimpleIdentifier_lexicalScope', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitSimpleIdentifier_lexicalScope);
      });
      _ut.test('test_visitSimpleIdentifier_lexicalScope_field_setter', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitSimpleIdentifier_lexicalScope_field_setter);
      });
      _ut.test('test_visitSuperConstructorInvocation', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitSuperConstructorInvocation);
      });
      _ut.test('test_visitSuperConstructorInvocation_namedParameter', () {
        final __test = new ElementResolverTest();
        runJUnitTest(__test, __test.test_visitSuperConstructorInvocation_namedParameter);
      });
    });
  }
}
class TypeOverrideManagerTest extends EngineTestCase {
  void test_exitScope_noScopes() {
    TypeOverrideManager manager = new TypeOverrideManager();
    try {
      manager.exitScope();
      JUnitTestCase.fail("Expected IllegalStateException");
    } on IllegalStateException catch (exception) {
    }
  }
  void test_exitScope_oneScope() {
    TypeOverrideManager manager = new TypeOverrideManager();
    manager.enterScope();
    manager.exitScope();
    try {
      manager.exitScope();
      JUnitTestCase.fail("Expected IllegalStateException");
    } on IllegalStateException catch (exception) {
    }
  }
  void test_exitScope_twoScopes() {
    TypeOverrideManager manager = new TypeOverrideManager();
    manager.enterScope();
    manager.exitScope();
    manager.enterScope();
    manager.exitScope();
    try {
      manager.exitScope();
      JUnitTestCase.fail("Expected IllegalStateException");
    } on IllegalStateException catch (exception) {
    }
  }
  void test_getType_enclosedOverride() {
    TypeOverrideManager manager = new TypeOverrideManager();
    LocalVariableElementImpl element = ElementFactory.localVariableElement2("v");
    InterfaceType type = ElementFactory.classElement2("C", []).type;
    manager.enterScope();
    manager.setType(element, type);
    manager.enterScope();
    JUnitTestCase.assertSame(type, manager.getType(element));
  }
  void test_getType_immediateOverride() {
    TypeOverrideManager manager = new TypeOverrideManager();
    LocalVariableElementImpl element = ElementFactory.localVariableElement2("v");
    InterfaceType type = ElementFactory.classElement2("C", []).type;
    manager.enterScope();
    manager.setType(element, type);
    JUnitTestCase.assertSame(type, manager.getType(element));
  }
  void test_getType_noOverride() {
    TypeOverrideManager manager = new TypeOverrideManager();
    manager.enterScope();
    JUnitTestCase.assertNull(manager.getType(ElementFactory.localVariableElement2("v")));
  }
  void test_getType_noScope() {
    TypeOverrideManager manager = new TypeOverrideManager();
    JUnitTestCase.assertNull(manager.getType(ElementFactory.localVariableElement2("v")));
  }
  static dartSuite() {
    _ut.group('TypeOverrideManagerTest', () {
      _ut.test('test_exitScope_noScopes', () {
        final __test = new TypeOverrideManagerTest();
        runJUnitTest(__test, __test.test_exitScope_noScopes);
      });
      _ut.test('test_exitScope_oneScope', () {
        final __test = new TypeOverrideManagerTest();
        runJUnitTest(__test, __test.test_exitScope_oneScope);
      });
      _ut.test('test_exitScope_twoScopes', () {
        final __test = new TypeOverrideManagerTest();
        runJUnitTest(__test, __test.test_exitScope_twoScopes);
      });
      _ut.test('test_getType_enclosedOverride', () {
        final __test = new TypeOverrideManagerTest();
        runJUnitTest(__test, __test.test_getType_enclosedOverride);
      });
      _ut.test('test_getType_immediateOverride', () {
        final __test = new TypeOverrideManagerTest();
        runJUnitTest(__test, __test.test_getType_immediateOverride);
      });
      _ut.test('test_getType_noOverride', () {
        final __test = new TypeOverrideManagerTest();
        runJUnitTest(__test, __test.test_getType_noOverride);
      });
      _ut.test('test_getType_noScope', () {
        final __test = new TypeOverrideManagerTest();
        runJUnitTest(__test, __test.test_getType_noScope);
      });
    });
  }
}
class PubSuggestionCodeTest extends ResolverTestCase {
  void test_import_package() {
    Source source = addSource(EngineTestCase.createSource(["import 'package:somepackage/other.dart';"]));
    resolve(source);
    assertErrors(source, [CompileTimeErrorCode.URI_DOES_NOT_EXIST]);
  }
  void test_import_packageWithDotDot() {
    Source source = addSource(EngineTestCase.createSource(["import 'package:somepackage/../other.dart';"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT]);
  }
  void test_import_packageWithLeadingDotDot() {
    Source source = addSource(EngineTestCase.createSource(["import 'package:../other.dart';"]));
    resolve(source);
    assertErrors(source, [
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT]);
  }
  void test_import_referenceIntoLibDirectory() {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/lib/other.dart", "");
    Source source = addSource2("/myproj/web/test.dart", EngineTestCase.createSource(["import '../lib/other.dart';"]));
    resolve(source);
    assertErrors(source, [PubSuggestionCode.FILE_IMPORT_OUTSIDE_LIB_REFERENCES_FILE_INSIDE]);
  }
  void test_import_referenceIntoLibDirectory_no_pubspec() {
    cacheSource("/myproj/lib/other.dart", "");
    Source source = addSource2("/myproj/web/test.dart", EngineTestCase.createSource(["import '../lib/other.dart';"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_import_referenceOutOfLibDirectory() {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/web/other.dart", "");
    Source source = addSource2("/myproj/lib/test.dart", EngineTestCase.createSource(["import '../web/other.dart';"]));
    resolve(source);
    assertErrors(source, [PubSuggestionCode.FILE_IMPORT_INSIDE_LIB_REFERENCES_FILE_OUTSIDE]);
  }
  void test_import_referenceOutOfLibDirectory_no_pubspec() {
    cacheSource("/myproj/web/other.dart", "");
    Source source = addSource2("/myproj/lib/test.dart", EngineTestCase.createSource(["import '../web/other.dart';"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_import_valid_inside_lib1() {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/lib/other.dart", "");
    Source source = addSource2("/myproj/lib/test.dart", EngineTestCase.createSource(["import 'other.dart';"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_import_valid_inside_lib2() {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/lib/bar/other.dart", "");
    Source source = addSource2("/myproj/lib/foo/test.dart", EngineTestCase.createSource(["import '../bar/other.dart';"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_import_valid_outside_lib() {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/web/other.dart", "");
    Source source = addSource2("/myproj/lib2/test.dart", EngineTestCase.createSource(["import '../web/other.dart';"]));
    resolve(source);
    assertNoErrors(source);
  }
  static dartSuite() {
    _ut.group('PubSuggestionCodeTest', () {
      _ut.test('test_import_package', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_package);
      });
      _ut.test('test_import_packageWithDotDot', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_packageWithDotDot);
      });
      _ut.test('test_import_packageWithLeadingDotDot', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_packageWithLeadingDotDot);
      });
      _ut.test('test_import_referenceIntoLibDirectory', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_referenceIntoLibDirectory);
      });
      _ut.test('test_import_referenceIntoLibDirectory_no_pubspec', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_referenceIntoLibDirectory_no_pubspec);
      });
      _ut.test('test_import_referenceOutOfLibDirectory', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_referenceOutOfLibDirectory);
      });
      _ut.test('test_import_referenceOutOfLibDirectory_no_pubspec', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_referenceOutOfLibDirectory_no_pubspec);
      });
      _ut.test('test_import_valid_inside_lib1', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_valid_inside_lib1);
      });
      _ut.test('test_import_valid_inside_lib2', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_valid_inside_lib2);
      });
      _ut.test('test_import_valid_outside_lib', () {
        final __test = new PubSuggestionCodeTest();
        runJUnitTest(__test, __test.test_import_valid_outside_lib);
      });
    });
  }
}
class StaticWarningCodeTest extends ResolverTestCase {
  void fail_undefinedGetter() {
    Source source = addSource(EngineTestCase.createSource([]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_GETTER]);
    verify([source]);
  }
  void fail_undefinedIdentifier_commentReference() {
    Source source = addSource(EngineTestCase.createSource(["/** [m] xxx [new B.c] */", "class A {", "}"]));
    resolve(source);
    assertErrors(source, [
        StaticWarningCode.UNDEFINED_IDENTIFIER,
        StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void fail_undefinedSetter() {
    Source source = addSource(EngineTestCase.createSource(["class C {}", "f(var p) {", "  C.m = 0;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_SETTER]);
    verify([source]);
  }
  void fail_undefinedStaticMethodOrGetter_getter() {
    Source source = addSource(EngineTestCase.createSource(["class C {}", "f(var p) {", "  f(C.m);", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER]);
    verify([source]);
  }
  void fail_undefinedStaticMethodOrGetter_method() {
    Source source = addSource(EngineTestCase.createSource(["class C {}", "f(var p) {", "  f(C.m());", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER]);
    verify([source]);
  }
  void test_ambiguousImport_as() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f(p) {p as N;}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_extends() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A extends N {}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [
        StaticWarningCode.AMBIGUOUS_IMPORT,
        CompileTimeErrorCode.EXTENDS_NON_CLASS]);
  }
  void test_ambiguousImport_implements() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A implements N {}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [
        StaticWarningCode.AMBIGUOUS_IMPORT,
        CompileTimeErrorCode.IMPLEMENTS_NON_CLASS]);
  }
  void test_ambiguousImport_inPart() {
    Source source = addSource(EngineTestCase.createSource([
        "library lib;",
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "part 'part.dart';"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    Source partSource = addSource2("/part.dart", EngineTestCase.createSource(["part of lib;", "class A extends N {}"]));
    resolve(source);
    assertErrors(partSource, [
        StaticWarningCode.AMBIGUOUS_IMPORT,
        CompileTimeErrorCode.EXTENDS_NON_CLASS]);
  }
  void test_ambiguousImport_instanceCreation() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f() {new N();}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_is() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f(p) {p is N;}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_qualifier() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "g() { N.FOO; }"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_typeAnnotation() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "typedef N FT(N p);",
        "N f(N p) {",
        "  N v;",
        "}",
        "class A {",
        "  N m() {}",
        "}",
        "class B<T extends N> {}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_typeArgument_annotation() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A<T> {}",
        "A<N> f() {}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_typeArgument_instanceCreation() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A<T> {}",
        "f() {new A<N>();}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class N {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class N {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_varRead() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f() { g(v); }",
        "g(p) {}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "var v;"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "var v;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_ambiguousImport_varWrite() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f() { v = 0; }"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "var v;"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "var v;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.AMBIGUOUS_IMPORT]);
  }
  void test_argumentTypeNotAssignable_annotation_namedConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A.fromInt(int p);",
        "}",
        "@A.fromInt('0')",
        "main() {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_annotation_unnamedConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(int p);",
        "}",
        "@A('0')",
        "main() {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_binary() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  operator +(int p) {}",
        "}",
        "f(A a) {",
        "  a + '0';",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_cascadeSEcond() {
    Source source = addSource(EngineTestCase.createSource([
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  B ma() {}",
        "}",
        "class B {",
        "  mb(String p) {}",
        "}",
        "",
        "main() {",
        "  A a = new A();",
        "  a..  ma().mb(0);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_const() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(String p);",
        "}",
        "main() {",
        "  const A(42);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_const_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  const A(String p);",
        "}",
        "class B extends A {",
        "  const B() : super(42);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_functionExpressionInvocation_required() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  (int x) {} ('');", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_index() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  operator [](int index) {}",
        "}",
        "f(A a) {",
        "  a['0'];",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_callParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  call(int p) {}",
        "}",
        "f(A a) {",
        "  a('0');",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_callVariable() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  call(int p) {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a('0');",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_functionParameter() {
    Source source = addSource(EngineTestCase.createSource(["a(b(int p)) {", "  b('0');", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_functionParameter_generic() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<K, V> {",
        "  m(f(K k), V v) {",
        "    f(v);",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_functionTypes_optional() {
    Source source = addSource(EngineTestCase.createSource([
        "void acceptFunNumOptBool(void funNumOptBool([bool b])) {}",
        "void funNumBool(bool b) {}",
        "main() {",
        "  acceptFunNumOptBool(funNumBool);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_generic() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> {",
        "  m(T t) {}",
        "}",
        "f(A<String> a) {",
        "  a.m(1);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_named() {
    Source source = addSource(EngineTestCase.createSource(["f({String p}) {}", "main() {", "  f(p: 42);", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_optional() {
    Source source = addSource(EngineTestCase.createSource(["f([String p]) {}", "main() {", "  f(42);", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_required() {
    Source source = addSource(EngineTestCase.createSource(["f(String p) {}", "main() {", "  f(42);", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_typedef_generic() {
    Source source = addSource(EngineTestCase.createSource(["typedef A<T>(T p);", "f(A<int> a) {", "  a('1');", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_typedef_local() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef A(int p);",
        "A getA() => null;",
        "main() {",
        "  A a = getA();",
        "  a('1');",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_invocation_typedef_parameter() {
    Source source = addSource(EngineTestCase.createSource(["typedef A(int p);", "f(A a) {", "  a('1');", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_new_generic() {
    Source source = addSource(EngineTestCase.createSource([
        "class A<T> {",
        "  A(T p) {}",
        "}",
        "main() {",
        "  new A<String>(42);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_new_optional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A([String p]) {}",
        "}",
        "main() {",
        "  new A(42);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_argumentTypeNotAssignable_new_required() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A(String p) {}",
        "}",
        "main() {",
        "  new A(42);",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_assignmentToConst_instanceVariable() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static const v = 0;",
        "}",
        "f() {",
        "  A.v = 1;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_CONST]);
    verify([source]);
  }
  void test_assignmentToConst_localVariable() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  const x = 0;", "  x = 1;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_CONST]);
    verify([source]);
  }
  void test_assignmentToFinal_instanceVariable() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final v = 0;",
        "}",
        "f() {",
        "  A a = new A();",
        "  a.v = 1;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToFinal_localVariable() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  final x = 0;", "  x = 1;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToFinal_prefixMinusMinus() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  final x = 0;", "  --x;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToFinal_prefixPlusPlus() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  final x = 0;", "  ++x;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToFinal_propertyAccess() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get x => 0;",
        "}",
        "class B {",
        "  static A a;",
        "}",
        "main() {",
        "  B.a.x = 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToFinal_suffixMinusMinus() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  final x = 0;", "  x--;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToFinal_suffixPlusPlus() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  final x = 0;", "  x++;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToFinal_topLevelVariable() {
    Source source = addSource(EngineTestCase.createSource(["final x = 0;", "f() { x = 1; }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_FINAL]);
    verify([source]);
  }
  void test_assignmentToMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m() {}",
        "}",
        "f(A a) {",
        "  a.m = () {};",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ASSIGNMENT_TO_METHOD]);
    verify([source]);
  }
  void test_caseBlockNotTerminated() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int p) {",
        "  switch (p) {",
        "    case 0:",
        "      f(p);",
        "    case 1:",
        "      break;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CASE_BLOCK_NOT_TERMINATED]);
    verify([source]);
  }
  void test_castToNonType() {
    Source source = addSource(EngineTestCase.createSource(["var A = 0;", "f(String s) { var x = s as A; }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CAST_TO_NON_TYPE]);
    verify([source]);
  }
  void test_concreteClassWithAbstractMember() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  m();", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER]);
    verify([source]);
  }
  void test_conflictingDartImport() {
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart';",
        "import 'dart:async';",
        "Future f = null;",
        "Stream s;"]));
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class Future {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_DART_IMPORT]);
  }
  void test_conflictingInstanceGetterAndSuperclassMember_direct_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int v;",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER]);
    verify([source]);
  }
  void test_conflictingInstanceGetterAndSuperclassMember_direct_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static get v => 0;",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER]);
    verify([source]);
  }
  void test_conflictingInstanceGetterAndSuperclassMember_direct_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static v() {}",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER]);
    verify([source]);
  }
  void test_conflictingInstanceGetterAndSuperclassMember_direct_setter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static set v(x) {}",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER]);
    verify([source]);
  }
  void test_conflictingInstanceGetterAndSuperclassMember_indirect() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int v;",
        "}",
        "class B extends A {}",
        "class C extends B {",
        "  get v => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER]);
    verify([source]);
  }
  void test_conflictingInstanceGetterAndSuperclassMember_mixin() {
    Source source = addSource(EngineTestCase.createSource([
        "class M {",
        "  static int v;",
        "}",
        "class B extends Object with M {",
        "  get v => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER]);
    verify([source]);
  }
  void test_conflictingInstanceSetterAndSuperclassMember() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int v;",
        "}",
        "class B extends A {",
        "  set v(x) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER]);
    verify([source]);
  }
  void test_conflictingStaticGetterAndInstanceSetter_mixin() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  set x(int p) {}",
        "}",
        "class B extends Object with A {",
        "  static get x => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER]);
    verify([source]);
  }
  void test_conflictingStaticGetterAndInstanceSetter_superClass() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  set x(int p) {}",
        "}",
        "class B extends A {",
        "  static get x => 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER]);
    verify([source]);
  }
  void test_conflictingStaticGetterAndInstanceSetter_thisClass() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static get x => 0;",
        "  set x(int p) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER]);
    verify([source]);
  }
  void test_conflictingStaticSetterAndInstanceMember_thisClass_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  get x => 0;",
        "  static set x(int p) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER]);
    verify([source]);
  }
  void test_conflictingStaticSetterAndInstanceMember_thisClass_method() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  x() {}", "  static set x(int p) {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER]);
    verify([source]);
  }
  void test_constWithAbstractClass() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  const A();",
        "}",
        "void f() {",
        "  A a = const A();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.CONST_WITH_ABSTRACT_CLASS]);
    verify([source]);
  }
  void test_equalKeysInMap() {
    Source source = addSource(EngineTestCase.createSource(["var m = {'a' : 0, 'b' : 1, 'a' : 2};"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.EQUAL_KEYS_IN_MAP]);
    verify([source]);
  }
  void test_exportDuplicatedLibraryName() {
    Source source = addSource(EngineTestCase.createSource([
        "library test;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"]));
    addSource2("/lib1.dart", "library lib;");
    addSource2("/lib2.dart", "library lib;");
    resolve(source);
    assertErrors(source, [StaticWarningCode.EXPORT_DUPLICATED_LIBRARY_NAME]);
    verify([source]);
  }
  void test_extraPositionalArguments() {
    Source source = addSource(EngineTestCase.createSource(["f() {}", "main() {", "  f(0, 1, '2');", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS]);
    verify([source]);
  }
  void test_extraPositionalArguments_functionExpression() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  (int x) {} (0, 1);", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS]);
    verify([source]);
  }
  void test_fieldInitializedInInitializerAndDeclaration_final() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  final int x = 0;",
        "  A() : x = 1 {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION]);
    verify([source]);
  }
  void test_fieldInitializerNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A() : x = '';", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FIELD_INITIALIZER_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_fieldInitializingFormalNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A(String this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FIELD_INITIALIZING_FORMAL_NOT_ASSIGNABLE]);
    verify([source]);
  }

  /**
   * This test doesn't test the FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR code, but tests the
   * FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION code instead. It is provided here to show
   * coverage over all of the permutations of initializers in constructor declarations.
   *
   * Note: FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION covers a subset of
   * FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR, since it more specific, we use it instead of
   * the broader code
   */
  void test_finalInitializedInDeclarationAndConstructor_initializers() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x = 0;", "  A() : x = 0 {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION]);
    verify([source]);
  }
  void test_finalInitializedInDeclarationAndConstructor_initializingFormal() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final x = 0;", "  A(this.x) {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR]);
    verify([source]);
  }
  void test_finalNotInitialized_inConstructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final int x;", "  A() {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FINAL_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_finalNotInitialized_instanceField_final() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  final F;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FINAL_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_finalNotInitialized_instanceField_final_static() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  static final F;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FINAL_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_finalNotInitialized_library_final() {
    Source source = addSource(EngineTestCase.createSource(["final F;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FINAL_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_finalNotInitialized_local_final() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  final int x;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FINAL_NOT_INITIALIZED]);
    verify([source]);
  }
  void test_functionWithoutCall_direct() {
    Source source = addSource(EngineTestCase.createSource(["class A implements Function {", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FUNCTION_WITHOUT_CALL]);
    verify([source]);
  }
  void test_functionWithoutCall_indirect_extends() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A implements Function {",
        "}",
        "class B extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FUNCTION_WITHOUT_CALL]);
    verify([source]);
  }
  void test_functionWithoutCall_indirect_implements() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A implements Function {",
        "}",
        "class B implements A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.FUNCTION_WITHOUT_CALL]);
    verify([source]);
  }
  void test_importDuplicatedLibraryName() {
    Source source = addSource(EngineTestCase.createSource([
        "library test;",
        "import 'lib1.dart';",
        "import 'lib2.dart';"]));
    addSource2("/lib1.dart", "library lib;");
    addSource2("/lib2.dart", "library lib;");
    resolve(source);
    assertErrors(source, [
        StaticWarningCode.IMPORT_DUPLICATED_LIBRARY_NAME,
        HintCode.UNUSED_IMPORT,
        HintCode.UNUSED_IMPORT]);
    verify([source]);
  }
  void test_inconsistentMethodInheritanceGetterAndMethod() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int x();",
        "}",
        "abstract class B {",
        "  int get x;",
        "}",
        "class C implements A, B {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_field() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static var n;",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_field2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static var n;",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static get n {return 0;}",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_getter2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static get n {return 0;}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static n () {}",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_method2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static n () {}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_setter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static set n(int x) {}",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_instanceMethodNameCollidesWithSuperclassStatic_setter2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static set n(int x) {}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC]);
    verify([source]);
  }
  void test_invalidGetterOverrideReturnType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get g { return 0; }",
        "}",
        "class B extends A {",
        "  String get g { return 'a'; }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE]);
    verify([source]);
  }
  void test_invalidGetterOverrideReturnType_implicit() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  String f;",
        "}",
        "class B extends A {",
        "  int f;",
        "}"]));
    resolve(source);
    assertErrors(source, [
        StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE,
        StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideNamedParamType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m({int a}) {}",
        "}",
        "class B implements A {",
        "  m({String a}) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideNormalParamType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m(int a) {}",
        "}",
        "class B implements A {",
        "  m(String a) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideOptionalParamType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m([int a]) {}",
        "}",
        "class B implements A {",
        "  m([String a]) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideReturnType_interface() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B implements A {",
        "  String m() { return 'a'; }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideReturnType_interface2() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int m();",
        "}",
        "abstract class B implements A {",
        "}",
        "class C implements B {",
        "  String m() { return 'a'; }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideReturnType_mixin() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends Object with A {",
        "  String m() { return 'a'; }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideReturnType_superclass() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "  String m() { return 'a'; }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideReturnType_superclass2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  String m() { return 'a'; }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE]);
    verify([source]);
  }
  void test_invalidMethodOverrideReturnType_void() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int m() {}",
        "}",
        "class B extends A {",
        "  void m() {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE]);
    verify([source]);
  }
  void test_invalidOverrideDifferentDefaultValues_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m({int p : 0}) {}",
        "}",
        "class B extends A {",
        "  m({int p : 1}) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_NAMED]);
    verify([source]);
  }
  void test_invalidOverrideDifferentDefaultValues_positional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m([int p = 0]) {}",
        "}",
        "class B extends A {",
        "  m([int p = 1]) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_POSITIONAL]);
    verify([source]);
  }
  void test_invalidOverrideNamed_fewerNamedParameters() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m({a, b}) {}",
        "}",
        "class B extends A {",
        "  m({a}) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_NAMED]);
    verify([source]);
  }
  void test_invalidOverrideNamed_missingNamedParameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m({a, b}) {}",
        "}",
        "class B extends A {",
        "  m({a, c}) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_NAMED]);
    verify([source]);
  }
  void test_invalidOverridePositional_optional() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m([a, b]) {}",
        "}",
        "class B extends A {",
        "  m([a]) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_POSITIONAL]);
    verify([source]);
  }
  void test_invalidOverridePositional_optionalAndRequired() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m(a, b, [c, d]) {}",
        "}",
        "class B extends A {",
        "  m(a, b, [c]) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_POSITIONAL]);
    verify([source]);
  }
  void test_invalidOverridePositional_optionalAndRequired2() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m(a, b, [c, d]) {}",
        "}",
        "class B extends A {",
        "  m(a, [c, d]) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_POSITIONAL]);
    verify([source]);
  }
  void test_invalidOverrideRequired() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  m(a) {}",
        "}",
        "class B extends A {",
        "  m(a, b) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_OVERRIDE_REQUIRED]);
    verify([source]);
  }
  void test_invalidSetterOverrideNormalParamType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void set s(int v) {}",
        "}",
        "class B extends A {",
        "  void set s(String v) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE]);
    verify([source]);
  }
  void test_listElementTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v = <String> [42];"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.LIST_ELEMENT_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_mapKeyTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v = <String, int > {1 : 2};"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MAP_KEY_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_mapValueTypeNotAssignable() {
    Source source = addSource(EngineTestCase.createSource(["var v = <String, String> {'a' : 2};"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MAP_VALUE_TYPE_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_mismatchedAccessorTypes_class() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get g { return 0; }",
        "  set g(String v) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES]);
    verify([source]);
  }
  void test_mismatchedAccessorTypes_getterAndSuperSetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get g { return 0; }",
        "}",
        "class B extends A {",
        "  set g(String v) {}",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES_FROM_SUPERTYPE]);
    verify([source]);
  }
  void test_mismatchedAccessorTypes_setterAndSuperGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  set g(int v) {}",
        "}",
        "class B extends A {",
        "  String get g { return ''; }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES_FROM_SUPERTYPE]);
    verify([source]);
  }
  void test_mismatchedAccessorTypes_topLevel() {
    Source source = addSource(EngineTestCase.createSource(["int get g { return 0; }", "set g(String v) {}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES]);
    verify([source]);
  }
  void test_mixedReturnTypes_localFunction() {
    Source source = addSource(EngineTestCase.createSource([
        "class C {",
        "  m(int x) {",
        "    return (int y) {",
        "      if (y < 0) {",
        "        return;",
        "      }",
        "      return 0;",
        "    };",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MIXED_RETURN_TYPES]);
    verify([source]);
  }
  void test_mixedReturnTypes_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class C {",
        "  m(int x) {",
        "    if (x < 0) {",
        "      return;",
        "    }",
        "    return 0;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MIXED_RETURN_TYPES]);
    verify([source]);
  }
  void test_mixedReturnTypes_topLevelFunction() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int x) {",
        "  if (x < 0) {",
        "    return;",
        "  }",
        "  return 0;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MIXED_RETURN_TYPES]);
    verify([source]);
  }
  void test_newWithAbstractClass() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {}",
        "void f() {",
        "  A a = new A();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NEW_WITH_ABSTRACT_CLASS]);
    verify([source]);
  }
  void test_newWithInvalidTypeParameters() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "f() { return new A<A>(); }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NEW_WITH_INVALID_TYPE_PARAMETERS]);
    verify([source]);
  }
  void test_newWithInvalidTypeParameters_tooFew() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class C<K, V> {}",
        "f(p) {",
        "  return new C<A>();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NEW_WITH_INVALID_TYPE_PARAMETERS]);
    verify([source]);
  }
  void test_newWithInvalidTypeParameters_tooMany() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class C<E> {}",
        "f(p) {",
        "  return new C<A, A>();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NEW_WITH_INVALID_TYPE_PARAMETERS]);
    verify([source]);
  }
  void test_newWithNonType() {
    Source source = addSource(EngineTestCase.createSource(["var A = 0;", "void f() {", "  var a = new A();", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NEW_WITH_NON_TYPE]);
    verify([source]);
  }
  void test_newWithNonType_fromLibrary() {
    Source source1 = addSource2("lib.dart", "class B {}");
    Source source2 = addSource2("lib2.dart", EngineTestCase.createSource([
        "import 'lib.dart' as lib;",
        "void f() {",
        "  var a = new lib.A();",
        "}",
        "lib.B b;"]));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, [StaticWarningCode.NEW_WITH_NON_TYPE]);
    verify([source1]);
  }
  void test_newWithUndefinedConstructor() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "f() {",
        "  new A.name();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR]);
  }
  void test_newWithUndefinedConstructorDefault() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A.name() {}",
        "}",
        "f() {",
        "  new A();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberFivePlus() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  m();",
        "  n();",
        "  o();",
        "  p();",
        "  q();",
        "}",
        "class C extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberFour() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  m();",
        "  n();",
        "  o();",
        "  p();",
        "}",
        "class C extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_getter_fromInterface() {
    Source source = addSource(EngineTestCase.createSource([
        "class I {",
        "  int get g {return 1;}",
        "}",
        "class C implements I {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_getter_fromSuperclass() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int get g;",
        "}",
        "class C extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_method_fromInterface() {
    Source source = addSource(EngineTestCase.createSource([
        "class I {",
        "  m(p) {}",
        "}",
        "class C implements I {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_method_fromSuperclass() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  m(p);",
        "}",
        "class C extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_method_optionalParamCount() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  int x(int a);",
        "}",
        "abstract class B {",
        "  int x(int a, [int b]);",
        "}",
        "class C implements A, B {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_setter_fromInterface() {
    Source source = addSource(EngineTestCase.createSource([
        "class I {",
        "  set s(int i) {}",
        "}",
        "class C implements I {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_setter_fromSuperclass() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  set s(int i);",
        "}",
        "class C extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberOne_superclasses_interface() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  get a => 'a';",
        "}",
        "abstract class B implements A {",
        "  get b => 'b';",
        "}",
        "class C extends B {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberThree() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  m();",
        "  n();",
        "  o();",
        "}",
        "class C extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE]);
    verify([source]);
  }
  void test_nonAbstractClassInheritsAbstractMemberTwo() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class A {",
        "  m();",
        "  n();",
        "}",
        "class C extends A {",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO]);
    verify([source]);
  }
  void test_nonTypeInCatchClause_noElement() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  try {", "  } on T catch (e) {", "  }", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE]);
    verify([source]);
  }
  void test_nonTypeInCatchClause_notType() {
    Source source = addSource(EngineTestCase.createSource([
        "var T = 0;",
        "f() {",
        "  try {",
        "  } on T catch (e) {",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE]);
    verify([source]);
  }
  void test_nonVoidReturnForOperator() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int operator []=(a, b) {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_VOID_RETURN_FOR_OPERATOR]);
    verify([source]);
  }
  void test_nonVoidReturnForSetter_function() {
    Source source = addSource(EngineTestCase.createSource(["int set x(int v) {", "  return 42;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_VOID_RETURN_FOR_SETTER]);
    verify([source]);
  }
  void test_nonVoidReturnForSetter_method() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int set x(int v) {",
        "    return 42;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NON_VOID_RETURN_FOR_SETTER]);
    verify([source]);
  }
  void test_notAType() {
    Source source = addSource(EngineTestCase.createSource(["f() {}", "main() {", "  f v = null;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NOT_A_TYPE]);
    verify([source]);
  }
  void test_notEnoughRequiredArguments() {
    Source source = addSource(EngineTestCase.createSource(["f(int a, String b) {}", "main() {", "  f();", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NOT_ENOUGH_REQUIRED_ARGUMENTS]);
    verify([source]);
  }
  void test_notEnoughRequiredArguments_functionExpression() {
    Source source = addSource(EngineTestCase.createSource(["main() {", "  (int x) {} ();", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.NOT_ENOUGH_REQUIRED_ARGUMENTS]);
    verify([source]);
  }
  void test_partOfDifferentLibrary() {
    Source source = addSource(EngineTestCase.createSource(["library lib;", "part 'part.dart';"]));
    addSource2("/part.dart", EngineTestCase.createSource(["part of lub;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.PART_OF_DIFFERENT_LIBRARY]);
    verify([source]);
  }
  void test_redirectToInvalidFunctionType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B {",
        "  A(int p) {}",
        "}",
        "class B {",
        "  factory B() = A;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.REDIRECT_TO_INVALID_FUNCTION_TYPE]);
    verify([source]);
  }
  void test_redirectToInvalidReturnType() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "}",
        "class B {",
        "  factory B() = A;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.REDIRECT_TO_INVALID_RETURN_TYPE]);
    verify([source]);
  }
  void test_redirectToMissingConstructor_named() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B{",
        "  A() {}",
        "}",
        "class B {",
        "  factory B() = A.name;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR]);
  }
  void test_redirectToMissingConstructor_unnamed() {
    Source source = addSource(EngineTestCase.createSource([
        "class A implements B{",
        "  A.name() {}",
        "}",
        "class B {",
        "  factory B() = A;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR]);
  }
  void test_redirectToNonClass_notAType() {
    Source source = addSource(EngineTestCase.createSource(["class B {", "  int A;", "  factory B() = A;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.REDIRECT_TO_NON_CLASS]);
    verify([source]);
  }
  void test_redirectToNonClass_undefinedIdentifier() {
    Source source = addSource(EngineTestCase.createSource(["class B {", "  factory B() = A;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.REDIRECT_TO_NON_CLASS]);
    verify([source]);
  }
  void test_returnWithoutValue() {
    Source source = addSource(EngineTestCase.createSource(["int f() { return; }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.RETURN_WITHOUT_VALUE]);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_method_invocation() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  m() {}", "}", "main() {", "  A.m();", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER]);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_method_reference() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  m() {}", "}", "main() {", "  A.m;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER]);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_propertyAccess_field() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  var f;", "}", "main() {", "  A.f;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER]);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_propertyAccess_getter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  get f => 42;",
        "}",
        "main() {",
        "  A.f;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER]);
    verify([source]);
  }
  void test_staticAccessToInstanceMember_propertyAccess_setter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  set f(x) {}",
        "}",
        "main() {",
        "  A.f = 42;",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER]);
    verify([source]);
  }
  void test_switchExpressionNotAssignable() {
    Source source = addSource(EngineTestCase.createSource([
        "f(int p) {",
        "  switch (p) {",
        "    case 'a': break;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.SWITCH_EXPRESSION_NOT_ASSIGNABLE]);
    verify([source]);
  }
  void test_typeParameterReferencedByStatic_field() {
    Source source = addSource(EngineTestCase.createSource(["class A<K> {", "  static K k;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_typeParameterReferencedByStatic_getter() {
    Source source = addSource(EngineTestCase.createSource(["class A<K> {", "  static K get k => 0;", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_typeParameterReferencedByStatic_methodBodyReference() {
    Source source = addSource(EngineTestCase.createSource(["class A<K> {", "  static m() {", "    K k;", "  }", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_typeParameterReferencedByStatic_methodParameter() {
    Source source = addSource(EngineTestCase.createSource(["class A<K> {", "  static m(K k) {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_typeParameterReferencedByStatic_methodReturn() {
    Source source = addSource(EngineTestCase.createSource(["class A<K> {", "  static K m() {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_typeParameterReferencedByStatic_setter() {
    Source source = addSource(EngineTestCase.createSource(["class A<K> {", "  static set s(K k) {}", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC]);
    verify([source]);
  }
  void test_typePromotion_functionType_arg_InterToDyn() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef FuncDyn(x);",
        "typedef FuncA(A a);",
        "class A {}",
        "class B {}",
        "main(FuncA f) {",
        "  if (f is FuncDyn) {",
        "    f(new B());",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE]);
  }
  void test_typeTestNonType() {
    Source source = addSource(EngineTestCase.createSource(["var A = 0;", "f(var p) {", "  if (p is A) {", "  }", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.TYPE_TEST_NON_TYPE]);
    verify([source]);
  }
  void test_undefinedClass_instanceCreation() {
    Source source = addSource(EngineTestCase.createSource(["f() { new C(); }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_CLASS]);
  }
  void test_undefinedClass_variableDeclaration() {
    Source source = addSource(EngineTestCase.createSource(["f() { C c; }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_CLASS]);
  }
  void test_undefinedClassBoolean_variableDeclaration() {
    Source source = addSource(EngineTestCase.createSource(["f() { boolean v; }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_CLASS_BOOLEAN]);
  }
  void test_undefinedGetter_fromLibrary() {
    Source source1 = addSource2("lib.dart", "");
    Source source2 = addSource2("lib2.dart", EngineTestCase.createSource([
        "import 'lib.dart' as lib;",
        "void f() {",
        "  var g = lib.gg;",
        "}"]));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, [StaticWarningCode.UNDEFINED_GETTER]);
    verify([source1]);
  }
  void test_undefinedIdentifier_for() {
    Source source = addSource(EngineTestCase.createSource(["f(var l) {", "  for (e in l) {", "  }", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_undefinedIdentifier_function() {
    Source source = addSource(EngineTestCase.createSource(["int a() => b;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_undefinedIdentifier_function_prefix() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class C {}"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as b;", "", "int a() => b;", "b.C c;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
    verify([source]);
  }
  void test_undefinedIdentifier_initializer() {
    Source source = addSource(EngineTestCase.createSource(["var a = b;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_undefinedIdentifier_initializer_prefix() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class C {}"]));
    Source source = addSource(EngineTestCase.createSource(["import 'lib.dart' as b;", "", "var a = b;", "b.C c;"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_undefinedIdentifier_methodInvocation() {
    Source source = addSource(EngineTestCase.createSource(["f() { C.m(); }"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_undefinedIdentifier_private_getter() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A {", "  var _foo;", "}"]));
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart';",
        "class B extends A {",
        "  test() {",
        "    var v = _foo;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_undefinedIdentifier_private_setter() {
    addSource2("/lib.dart", EngineTestCase.createSource(["library lib;", "class A {", "  var _foo;", "}"]));
    Source source = addSource(EngineTestCase.createSource([
        "import 'lib.dart';",
        "class B extends A {",
        "  test() {",
        "    _foo = 42;",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_IDENTIFIER]);
  }
  void test_undefinedNamedParameter() {
    Source source = addSource(EngineTestCase.createSource(["f({a, b}) {}", "main() {", "  f(c: 1);", "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.UNDEFINED_NAMED_PARAMETER]);
  }
  void test_undefinedSetter() {
    Source source1 = addSource2("lib.dart", "");
    Source source2 = addSource2("lib2.dart", EngineTestCase.createSource([
        "import 'lib.dart' as lib;",
        "void f() {",
        "  lib.gg = null;",
        "}"]));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, [StaticWarningCode.UNDEFINED_SETTER]);
  }
  static dartSuite() {
    _ut.group('StaticWarningCodeTest', () {
      _ut.test('test_ambiguousImport_as', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_as);
      });
      _ut.test('test_ambiguousImport_extends', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_extends);
      });
      _ut.test('test_ambiguousImport_implements', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_implements);
      });
      _ut.test('test_ambiguousImport_inPart', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_inPart);
      });
      _ut.test('test_ambiguousImport_instanceCreation', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_instanceCreation);
      });
      _ut.test('test_ambiguousImport_is', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_is);
      });
      _ut.test('test_ambiguousImport_qualifier', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_qualifier);
      });
      _ut.test('test_ambiguousImport_typeAnnotation', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_typeAnnotation);
      });
      _ut.test('test_ambiguousImport_typeArgument_annotation', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_typeArgument_annotation);
      });
      _ut.test('test_ambiguousImport_typeArgument_instanceCreation', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_typeArgument_instanceCreation);
      });
      _ut.test('test_ambiguousImport_varRead', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_varRead);
      });
      _ut.test('test_ambiguousImport_varWrite', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_ambiguousImport_varWrite);
      });
      _ut.test('test_argumentTypeNotAssignable_annotation_namedConstructor', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_annotation_namedConstructor);
      });
      _ut.test('test_argumentTypeNotAssignable_annotation_unnamedConstructor', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_annotation_unnamedConstructor);
      });
      _ut.test('test_argumentTypeNotAssignable_binary', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_binary);
      });
      _ut.test('test_argumentTypeNotAssignable_cascadeSEcond', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_cascadeSEcond);
      });
      _ut.test('test_argumentTypeNotAssignable_const', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_const);
      });
      _ut.test('test_argumentTypeNotAssignable_const_super', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_const_super);
      });
      _ut.test('test_argumentTypeNotAssignable_functionExpressionInvocation_required', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_functionExpressionInvocation_required);
      });
      _ut.test('test_argumentTypeNotAssignable_index', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_index);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_callParameter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_callParameter);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_callVariable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_callVariable);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_functionParameter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_functionParameter);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_functionParameter_generic', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_functionParameter_generic);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_functionTypes_optional', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_functionTypes_optional);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_generic', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_generic);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_named', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_named);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_optional', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_optional);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_required', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_required);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_typedef_generic', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_typedef_generic);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_typedef_local', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_typedef_local);
      });
      _ut.test('test_argumentTypeNotAssignable_invocation_typedef_parameter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_invocation_typedef_parameter);
      });
      _ut.test('test_argumentTypeNotAssignable_new_generic', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_new_generic);
      });
      _ut.test('test_argumentTypeNotAssignable_new_optional', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_new_optional);
      });
      _ut.test('test_argumentTypeNotAssignable_new_required', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_argumentTypeNotAssignable_new_required);
      });
      _ut.test('test_assignmentToConst_instanceVariable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToConst_instanceVariable);
      });
      _ut.test('test_assignmentToConst_localVariable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToConst_localVariable);
      });
      _ut.test('test_assignmentToFinal_instanceVariable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_instanceVariable);
      });
      _ut.test('test_assignmentToFinal_localVariable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_localVariable);
      });
      _ut.test('test_assignmentToFinal_prefixMinusMinus', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_prefixMinusMinus);
      });
      _ut.test('test_assignmentToFinal_prefixPlusPlus', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_prefixPlusPlus);
      });
      _ut.test('test_assignmentToFinal_propertyAccess', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_propertyAccess);
      });
      _ut.test('test_assignmentToFinal_suffixMinusMinus', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_suffixMinusMinus);
      });
      _ut.test('test_assignmentToFinal_suffixPlusPlus', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_suffixPlusPlus);
      });
      _ut.test('test_assignmentToFinal_topLevelVariable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToFinal_topLevelVariable);
      });
      _ut.test('test_assignmentToMethod', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_assignmentToMethod);
      });
      _ut.test('test_caseBlockNotTerminated', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_caseBlockNotTerminated);
      });
      _ut.test('test_castToNonType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_castToNonType);
      });
      _ut.test('test_concreteClassWithAbstractMember', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_concreteClassWithAbstractMember);
      });
      _ut.test('test_conflictingDartImport', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingDartImport);
      });
      _ut.test('test_conflictingInstanceGetterAndSuperclassMember_direct_field', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingInstanceGetterAndSuperclassMember_direct_field);
      });
      _ut.test('test_conflictingInstanceGetterAndSuperclassMember_direct_getter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingInstanceGetterAndSuperclassMember_direct_getter);
      });
      _ut.test('test_conflictingInstanceGetterAndSuperclassMember_direct_method', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingInstanceGetterAndSuperclassMember_direct_method);
      });
      _ut.test('test_conflictingInstanceGetterAndSuperclassMember_direct_setter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingInstanceGetterAndSuperclassMember_direct_setter);
      });
      _ut.test('test_conflictingInstanceGetterAndSuperclassMember_indirect', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingInstanceGetterAndSuperclassMember_indirect);
      });
      _ut.test('test_conflictingInstanceGetterAndSuperclassMember_mixin', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingInstanceGetterAndSuperclassMember_mixin);
      });
      _ut.test('test_conflictingInstanceSetterAndSuperclassMember', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingInstanceSetterAndSuperclassMember);
      });
      _ut.test('test_conflictingStaticGetterAndInstanceSetter_mixin', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingStaticGetterAndInstanceSetter_mixin);
      });
      _ut.test('test_conflictingStaticGetterAndInstanceSetter_superClass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingStaticGetterAndInstanceSetter_superClass);
      });
      _ut.test('test_conflictingStaticGetterAndInstanceSetter_thisClass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingStaticGetterAndInstanceSetter_thisClass);
      });
      _ut.test('test_conflictingStaticSetterAndInstanceMember_thisClass_getter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingStaticSetterAndInstanceMember_thisClass_getter);
      });
      _ut.test('test_conflictingStaticSetterAndInstanceMember_thisClass_method', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_conflictingStaticSetterAndInstanceMember_thisClass_method);
      });
      _ut.test('test_constWithAbstractClass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_constWithAbstractClass);
      });
      _ut.test('test_equalKeysInMap', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_equalKeysInMap);
      });
      _ut.test('test_exportDuplicatedLibraryName', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_exportDuplicatedLibraryName);
      });
      _ut.test('test_extraPositionalArguments', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments);
      });
      _ut.test('test_extraPositionalArguments_functionExpression', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_extraPositionalArguments_functionExpression);
      });
      _ut.test('test_fieldInitializedInInitializerAndDeclaration_final', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializedInInitializerAndDeclaration_final);
      });
      _ut.test('test_fieldInitializerNotAssignable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializerNotAssignable);
      });
      _ut.test('test_fieldInitializingFormalNotAssignable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_fieldInitializingFormalNotAssignable);
      });
      _ut.test('test_finalInitializedInDeclarationAndConstructor_initializers', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_finalInitializedInDeclarationAndConstructor_initializers);
      });
      _ut.test('test_finalInitializedInDeclarationAndConstructor_initializingFormal', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_finalInitializedInDeclarationAndConstructor_initializingFormal);
      });
      _ut.test('test_finalNotInitialized_inConstructor', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_inConstructor);
      });
      _ut.test('test_finalNotInitialized_instanceField_final', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_instanceField_final);
      });
      _ut.test('test_finalNotInitialized_instanceField_final_static', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_instanceField_final_static);
      });
      _ut.test('test_finalNotInitialized_library_final', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_library_final);
      });
      _ut.test('test_finalNotInitialized_local_final', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_finalNotInitialized_local_final);
      });
      _ut.test('test_functionWithoutCall_direct', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_functionWithoutCall_direct);
      });
      _ut.test('test_functionWithoutCall_indirect_extends', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_functionWithoutCall_indirect_extends);
      });
      _ut.test('test_functionWithoutCall_indirect_implements', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_functionWithoutCall_indirect_implements);
      });
      _ut.test('test_importDuplicatedLibraryName', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_importDuplicatedLibraryName);
      });
      _ut.test('test_inconsistentMethodInheritanceGetterAndMethod', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_inconsistentMethodInheritanceGetterAndMethod);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_field', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_field);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_field2', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_field2);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_getter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_getter);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_getter2', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_getter2);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_method', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_method);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_method2', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_method2);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_setter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_setter);
      });
      _ut.test('test_instanceMethodNameCollidesWithSuperclassStatic_setter2', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_instanceMethodNameCollidesWithSuperclassStatic_setter2);
      });
      _ut.test('test_invalidGetterOverrideReturnType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidGetterOverrideReturnType);
      });
      _ut.test('test_invalidGetterOverrideReturnType_implicit', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidGetterOverrideReturnType_implicit);
      });
      _ut.test('test_invalidMethodOverrideNamedParamType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideNamedParamType);
      });
      _ut.test('test_invalidMethodOverrideNormalParamType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideNormalParamType);
      });
      _ut.test('test_invalidMethodOverrideOptionalParamType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideOptionalParamType);
      });
      _ut.test('test_invalidMethodOverrideReturnType_interface', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideReturnType_interface);
      });
      _ut.test('test_invalidMethodOverrideReturnType_interface2', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideReturnType_interface2);
      });
      _ut.test('test_invalidMethodOverrideReturnType_mixin', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideReturnType_mixin);
      });
      _ut.test('test_invalidMethodOverrideReturnType_superclass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideReturnType_superclass);
      });
      _ut.test('test_invalidMethodOverrideReturnType_superclass2', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideReturnType_superclass2);
      });
      _ut.test('test_invalidMethodOverrideReturnType_void', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidMethodOverrideReturnType_void);
      });
      _ut.test('test_invalidOverrideDifferentDefaultValues_named', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverrideDifferentDefaultValues_named);
      });
      _ut.test('test_invalidOverrideDifferentDefaultValues_positional', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverrideDifferentDefaultValues_positional);
      });
      _ut.test('test_invalidOverrideNamed_fewerNamedParameters', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverrideNamed_fewerNamedParameters);
      });
      _ut.test('test_invalidOverrideNamed_missingNamedParameter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverrideNamed_missingNamedParameter);
      });
      _ut.test('test_invalidOverridePositional_optional', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverridePositional_optional);
      });
      _ut.test('test_invalidOverridePositional_optionalAndRequired', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverridePositional_optionalAndRequired);
      });
      _ut.test('test_invalidOverridePositional_optionalAndRequired2', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverridePositional_optionalAndRequired2);
      });
      _ut.test('test_invalidOverrideRequired', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidOverrideRequired);
      });
      _ut.test('test_invalidSetterOverrideNormalParamType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_invalidSetterOverrideNormalParamType);
      });
      _ut.test('test_listElementTypeNotAssignable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_listElementTypeNotAssignable);
      });
      _ut.test('test_mapKeyTypeNotAssignable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mapKeyTypeNotAssignable);
      });
      _ut.test('test_mapValueTypeNotAssignable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mapValueTypeNotAssignable);
      });
      _ut.test('test_mismatchedAccessorTypes_class', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mismatchedAccessorTypes_class);
      });
      _ut.test('test_mismatchedAccessorTypes_getterAndSuperSetter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mismatchedAccessorTypes_getterAndSuperSetter);
      });
      _ut.test('test_mismatchedAccessorTypes_setterAndSuperGetter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mismatchedAccessorTypes_setterAndSuperGetter);
      });
      _ut.test('test_mismatchedAccessorTypes_topLevel', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mismatchedAccessorTypes_topLevel);
      });
      _ut.test('test_mixedReturnTypes_localFunction', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mixedReturnTypes_localFunction);
      });
      _ut.test('test_mixedReturnTypes_method', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mixedReturnTypes_method);
      });
      _ut.test('test_mixedReturnTypes_topLevelFunction', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_mixedReturnTypes_topLevelFunction);
      });
      _ut.test('test_newWithAbstractClass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithAbstractClass);
      });
      _ut.test('test_newWithInvalidTypeParameters', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithInvalidTypeParameters);
      });
      _ut.test('test_newWithInvalidTypeParameters_tooFew', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithInvalidTypeParameters_tooFew);
      });
      _ut.test('test_newWithInvalidTypeParameters_tooMany', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithInvalidTypeParameters_tooMany);
      });
      _ut.test('test_newWithNonType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithNonType);
      });
      _ut.test('test_newWithNonType_fromLibrary', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithNonType_fromLibrary);
      });
      _ut.test('test_newWithUndefinedConstructor', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithUndefinedConstructor);
      });
      _ut.test('test_newWithUndefinedConstructorDefault', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_newWithUndefinedConstructorDefault);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberFivePlus', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberFivePlus);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberFour', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberFour);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_getter_fromInterface', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_getter_fromInterface);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_getter_fromSuperclass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_getter_fromSuperclass);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_method_fromInterface', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_method_fromInterface);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_method_fromSuperclass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_method_fromSuperclass);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_method_optionalParamCount', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_method_optionalParamCount);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_setter_fromInterface', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_setter_fromInterface);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_setter_fromSuperclass', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_setter_fromSuperclass);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberOne_superclasses_interface', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberOne_superclasses_interface);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberThree', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberThree);
      });
      _ut.test('test_nonAbstractClassInheritsAbstractMemberTwo', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonAbstractClassInheritsAbstractMemberTwo);
      });
      _ut.test('test_nonTypeInCatchClause_noElement', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonTypeInCatchClause_noElement);
      });
      _ut.test('test_nonTypeInCatchClause_notType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonTypeInCatchClause_notType);
      });
      _ut.test('test_nonVoidReturnForOperator', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForOperator);
      });
      _ut.test('test_nonVoidReturnForSetter_function', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForSetter_function);
      });
      _ut.test('test_nonVoidReturnForSetter_method', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_nonVoidReturnForSetter_method);
      });
      _ut.test('test_notAType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_notAType);
      });
      _ut.test('test_notEnoughRequiredArguments', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_notEnoughRequiredArguments);
      });
      _ut.test('test_notEnoughRequiredArguments_functionExpression', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_notEnoughRequiredArguments_functionExpression);
      });
      _ut.test('test_partOfDifferentLibrary', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_partOfDifferentLibrary);
      });
      _ut.test('test_redirectToInvalidFunctionType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_redirectToInvalidFunctionType);
      });
      _ut.test('test_redirectToInvalidReturnType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_redirectToInvalidReturnType);
      });
      _ut.test('test_redirectToMissingConstructor_named', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_redirectToMissingConstructor_named);
      });
      _ut.test('test_redirectToMissingConstructor_unnamed', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_redirectToMissingConstructor_unnamed);
      });
      _ut.test('test_redirectToNonClass_notAType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_redirectToNonClass_notAType);
      });
      _ut.test('test_redirectToNonClass_undefinedIdentifier', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_redirectToNonClass_undefinedIdentifier);
      });
      _ut.test('test_returnWithoutValue', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_returnWithoutValue);
      });
      _ut.test('test_staticAccessToInstanceMember_method_invocation', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_method_invocation);
      });
      _ut.test('test_staticAccessToInstanceMember_method_reference', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_method_reference);
      });
      _ut.test('test_staticAccessToInstanceMember_propertyAccess_field', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_propertyAccess_field);
      });
      _ut.test('test_staticAccessToInstanceMember_propertyAccess_getter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_propertyAccess_getter);
      });
      _ut.test('test_staticAccessToInstanceMember_propertyAccess_setter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_staticAccessToInstanceMember_propertyAccess_setter);
      });
      _ut.test('test_switchExpressionNotAssignable', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_switchExpressionNotAssignable);
      });
      _ut.test('test_typeParameterReferencedByStatic_field', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typeParameterReferencedByStatic_field);
      });
      _ut.test('test_typeParameterReferencedByStatic_getter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typeParameterReferencedByStatic_getter);
      });
      _ut.test('test_typeParameterReferencedByStatic_methodBodyReference', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typeParameterReferencedByStatic_methodBodyReference);
      });
      _ut.test('test_typeParameterReferencedByStatic_methodParameter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typeParameterReferencedByStatic_methodParameter);
      });
      _ut.test('test_typeParameterReferencedByStatic_methodReturn', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typeParameterReferencedByStatic_methodReturn);
      });
      _ut.test('test_typeParameterReferencedByStatic_setter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typeParameterReferencedByStatic_setter);
      });
      _ut.test('test_typePromotion_functionType_arg_InterToDyn', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typePromotion_functionType_arg_InterToDyn);
      });
      _ut.test('test_typeTestNonType', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_typeTestNonType);
      });
      _ut.test('test_undefinedClassBoolean_variableDeclaration', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedClassBoolean_variableDeclaration);
      });
      _ut.test('test_undefinedClass_instanceCreation', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedClass_instanceCreation);
      });
      _ut.test('test_undefinedClass_variableDeclaration', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedClass_variableDeclaration);
      });
      _ut.test('test_undefinedGetter_fromLibrary', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedGetter_fromLibrary);
      });
      _ut.test('test_undefinedIdentifier_for', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_for);
      });
      _ut.test('test_undefinedIdentifier_function', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_function);
      });
      _ut.test('test_undefinedIdentifier_function_prefix', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_function_prefix);
      });
      _ut.test('test_undefinedIdentifier_initializer', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_initializer);
      });
      _ut.test('test_undefinedIdentifier_initializer_prefix', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_initializer_prefix);
      });
      _ut.test('test_undefinedIdentifier_methodInvocation', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_methodInvocation);
      });
      _ut.test('test_undefinedIdentifier_private_getter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_private_getter);
      });
      _ut.test('test_undefinedIdentifier_private_setter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedIdentifier_private_setter);
      });
      _ut.test('test_undefinedNamedParameter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedNamedParameter);
      });
      _ut.test('test_undefinedSetter', () {
        final __test = new StaticWarningCodeTest();
        runJUnitTest(__test, __test.test_undefinedSetter);
      });
    });
  }
}
class ErrorResolverTest extends ResolverTestCase {
  void test_breakLabelOnSwitchMember() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m(int i) {",
        "    switch (i) {",
        "      l: case 0:",
        "        break;",
        "      case 1:",
        "        break l;",
        "    }",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [ResolverErrorCode.BREAK_LABEL_ON_SWITCH_MEMBER]);
    verify([source]);
  }
  void test_continueLabelOnSwitch() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m(int i) {",
        "    l: switch (i) {",
        "      case 0:",
        "        continue l;",
        "    }",
        "  }",
        "}"]));
    resolve(source);
    assertErrors(source, [ResolverErrorCode.CONTINUE_LABEL_ON_SWITCH]);
    verify([source]);
  }
  static dartSuite() {
    _ut.group('ErrorResolverTest', () {
      _ut.test('test_breakLabelOnSwitchMember', () {
        final __test = new ErrorResolverTest();
        runJUnitTest(__test, __test.test_breakLabelOnSwitchMember);
      });
      _ut.test('test_continueLabelOnSwitch', () {
        final __test = new ErrorResolverTest();
        runJUnitTest(__test, __test.test_continueLabelOnSwitch);
      });
    });
  }
}
/**
 * Instances of the class `TestTypeProvider` implement a type provider that can be used by
 * tests without creating the element model for the core library.
 */
class TestTypeProvider implements TypeProvider {

  /**
   * The type representing the built-in type 'bool'.
   */
  InterfaceType _boolType;

  /**
   * The type representing the type 'bottom'.
   */
  Type2 _bottomType;

  /**
   * The type representing the built-in type 'double'.
   */
  InterfaceType _doubleType;

  /**
   * The type representing the built-in type 'dynamic'.
   */
  Type2 _dynamicType;

  /**
   * The type representing the built-in type 'Function'.
   */
  InterfaceType _functionType;

  /**
   * The type representing the built-in type 'int'.
   */
  InterfaceType _intType;

  /**
   * The type representing the built-in type 'Iterable'.
   */
  InterfaceType _iterableType;

  /**
   * The type representing the built-in type 'Iterator'.
   */
  InterfaceType _iteratorType;

  /**
   * The type representing the built-in type 'List'.
   */
  InterfaceType _listType;

  /**
   * The type representing the built-in type 'Map'.
   */
  InterfaceType _mapType;

  /**
   * The type representing the built-in type 'Null'.
   */
  InterfaceType _nullType;

  /**
   * The type representing the built-in type 'num'.
   */
  InterfaceType _numType;

  /**
   * The type representing the built-in type 'Object'.
   */
  InterfaceType _objectType;

  /**
   * The type representing the built-in type 'StackTrace'.
   */
  InterfaceType _stackTraceType;

  /**
   * The type representing the built-in type 'String'.
   */
  InterfaceType _stringType;

  /**
   * The type representing the built-in type 'Symbol'.
   */
  InterfaceType _symbolType;

  /**
   * The type representing the built-in type 'Type'.
   */
  InterfaceType _typeType;
  InterfaceType get boolType {
    if (_boolType == null) {
      _boolType = ElementFactory.classElement2("bool", []).type;
    }
    return _boolType;
  }
  Type2 get bottomType {
    if (_bottomType == null) {
      _bottomType = BottomTypeImpl.instance;
    }
    return _bottomType;
  }
  InterfaceType get doubleType {
    if (_doubleType == null) {
      initializeNumericTypes();
    }
    return _doubleType;
  }
  Type2 get dynamicType {
    if (_dynamicType == null) {
      _dynamicType = DynamicTypeImpl.instance;
    }
    return _dynamicType;
  }
  InterfaceType get functionType {
    if (_functionType == null) {
      _functionType = ElementFactory.classElement2("Function", []).type;
    }
    return _functionType;
  }
  InterfaceType get intType {
    if (_intType == null) {
      initializeNumericTypes();
    }
    return _intType;
  }
  InterfaceType get iterableType {
    if (_iterableType == null) {
      ClassElementImpl iterableElement = ElementFactory.classElement2("Iterable", ["E"]);
      _iterableType = iterableElement.type;
      Type2 eType = iterableElement.typeParameters[0].type;
      iterableElement.accessors = <PropertyAccessorElement> [
          ElementFactory.getterElement("iterator", false, iteratorType.substitute4(<Type2> [eType])),
          ElementFactory.getterElement("last", false, eType)];
      propagateTypeArguments(iterableElement);
    }
    return _iterableType;
  }
  InterfaceType get iteratorType {
    if (_iteratorType == null) {
      ClassElementImpl iteratorElement = ElementFactory.classElement2("Iterator", ["E"]);
      _iteratorType = iteratorElement.type;
      Type2 eType = iteratorElement.typeParameters[0].type;
      iteratorElement.accessors = <PropertyAccessorElement> [ElementFactory.getterElement("current", false, eType)];
      propagateTypeArguments(iteratorElement);
    }
    return _iteratorType;
  }
  InterfaceType get listType {
    if (_listType == null) {
      ClassElementImpl listElement = ElementFactory.classElement2("List", ["E"]);
      listElement.constructors = <ConstructorElement> [ElementFactory.constructorElement(listElement, null)];
      _listType = listElement.type;
      Type2 eType = listElement.typeParameters[0].type;
      InterfaceType iterableType = this.iterableType.substitute4(<Type2> [eType]);
      listElement.interfaces = <InterfaceType> [iterableType];
      listElement.accessors = <PropertyAccessorElement> [ElementFactory.getterElement("length", false, intType)];
      listElement.methods = <MethodElement> [
          ElementFactory.methodElement("[]", eType, [intType]),
          ElementFactory.methodElement("[]=", VoidTypeImpl.instance, [intType, eType]),
          ElementFactory.methodElement("add", VoidTypeImpl.instance, [eType])];
      propagateTypeArguments(listElement);
    }
    return _listType;
  }
  InterfaceType get mapType {
    if (_mapType == null) {
      ClassElementImpl mapElement = ElementFactory.classElement2("Map", ["K", "V"]);
      _mapType = mapElement.type;
      mapElement.accessors = <PropertyAccessorElement> [ElementFactory.getterElement("length", false, intType)];
      propagateTypeArguments(mapElement);
    }
    return _mapType;
  }
  InterfaceType get nullType {
    if (_nullType == null) {
      _nullType = ElementFactory.classElement2("Null", []).type;
    }
    return _nullType;
  }
  InterfaceType get numType {
    if (_numType == null) {
      initializeNumericTypes();
    }
    return _numType;
  }
  InterfaceType get objectType {
    if (_objectType == null) {
      ClassElementImpl objectElement = ElementFactory.object;
      _objectType = objectElement.type;
      objectElement.constructors = <ConstructorElement> [ElementFactory.constructorElement(objectElement, null)];
      objectElement.methods = <MethodElement> [
          ElementFactory.methodElement("toString", stringType, []),
          ElementFactory.methodElement("==", boolType, [_objectType]),
          ElementFactory.methodElement("noSuchMethod", dynamicType, [dynamicType])];
      objectElement.accessors = <PropertyAccessorElement> [
          ElementFactory.getterElement("hashCode", false, intType),
          ElementFactory.getterElement("runtimeType", false, typeType)];
    }
    return _objectType;
  }
  InterfaceType get stackTraceType {
    if (_stackTraceType == null) {
      _stackTraceType = ElementFactory.classElement2("StackTrace", []).type;
    }
    return _stackTraceType;
  }
  InterfaceType get stringType {
    if (_stringType == null) {
      _stringType = ElementFactory.classElement2("String", []).type;
      ClassElementImpl stringElement = _stringType.element as ClassElementImpl;
      stringElement.accessors = <PropertyAccessorElement> [
          ElementFactory.getterElement("isEmpty", false, boolType),
          ElementFactory.getterElement("length", false, intType),
          ElementFactory.getterElement("codeUnits", false, listType.substitute4(<Type2> [intType]))];
      stringElement.methods = <MethodElement> [
          ElementFactory.methodElement("+", _stringType, [_stringType]),
          ElementFactory.methodElement("toLowerCase", _stringType, []),
          ElementFactory.methodElement("toUpperCase", _stringType, [])];
    }
    return _stringType;
  }
  InterfaceType get symbolType {
    if (_symbolType == null) {
      _symbolType = ElementFactory.classElement2("Symbol", []).type;
    }
    return _symbolType;
  }
  InterfaceType get typeType {
    if (_typeType == null) {
      _typeType = ElementFactory.classElement2("Type", []).type;
    }
    return _typeType;
  }

  /**
   * Initialize the numeric types. They are created as a group so that we can (a) create the right
   * hierarchy and (b) add members to them.
   */
  void initializeNumericTypes() {
    ClassElementImpl numElement = ElementFactory.classElement2("num", []);
    _numType = numElement.type;
    ClassElementImpl intElement = ElementFactory.classElement("int", _numType, []);
    _intType = intElement.type;
    ClassElementImpl doubleElement = ElementFactory.classElement("double", _numType, []);
    _doubleType = doubleElement.type;
    boolType;
    stringType;
    numElement.methods = <MethodElement> [
        ElementFactory.methodElement("+", _numType, [_numType]),
        ElementFactory.methodElement("-", _numType, [_numType]),
        ElementFactory.methodElement("*", _numType, [_numType]),
        ElementFactory.methodElement("%", _numType, [_numType]),
        ElementFactory.methodElement("/", _doubleType, [_numType]),
        ElementFactory.methodElement("~/", _numType, [_numType]),
        ElementFactory.methodElement("-", _numType, []),
        ElementFactory.methodElement("remainder", _numType, [_numType]),
        ElementFactory.methodElement("<", _boolType, [_numType]),
        ElementFactory.methodElement("<=", _boolType, [_numType]),
        ElementFactory.methodElement(">", _boolType, [_numType]),
        ElementFactory.methodElement(">=", _boolType, [_numType]),
        ElementFactory.methodElement("isNaN", _boolType, []),
        ElementFactory.methodElement("isNegative", _boolType, []),
        ElementFactory.methodElement("isInfinite", _boolType, []),
        ElementFactory.methodElement("abs", _numType, []),
        ElementFactory.methodElement("floor", _numType, []),
        ElementFactory.methodElement("ceil", _numType, []),
        ElementFactory.methodElement("round", _numType, []),
        ElementFactory.methodElement("truncate", _numType, []),
        ElementFactory.methodElement("toInt", _intType, []),
        ElementFactory.methodElement("toDouble", _doubleType, []),
        ElementFactory.methodElement("toStringAsFixed", _stringType, [_intType]),
        ElementFactory.methodElement("toStringAsExponential", _stringType, [_intType]),
        ElementFactory.methodElement("toStringAsPrecision", _stringType, [_intType]),
        ElementFactory.methodElement("toRadixString", _stringType, [_intType])];
    intElement.methods = <MethodElement> [
        ElementFactory.methodElement("&", _intType, [_intType]),
        ElementFactory.methodElement("|", _intType, [_intType]),
        ElementFactory.methodElement("^", _intType, [_intType]),
        ElementFactory.methodElement("~", _intType, []),
        ElementFactory.methodElement("<<", _intType, [_intType]),
        ElementFactory.methodElement(">>", _intType, [_intType]),
        ElementFactory.methodElement("-", _intType, []),
        ElementFactory.methodElement("abs", _intType, []),
        ElementFactory.methodElement("round", _intType, []),
        ElementFactory.methodElement("floor", _intType, []),
        ElementFactory.methodElement("ceil", _intType, []),
        ElementFactory.methodElement("truncate", _intType, []),
        ElementFactory.methodElement("toString", _stringType, [])];
    List<FieldElement> fields = <FieldElement> [
        ElementFactory.fieldElement("NAN", true, false, true, _doubleType),
        ElementFactory.fieldElement("INFINITY", true, false, true, _doubleType),
        ElementFactory.fieldElement("NEGATIVE_INFINITY", true, false, true, _doubleType),
        ElementFactory.fieldElement("MIN_POSITIVE", true, false, true, _doubleType),
        ElementFactory.fieldElement("MAX_FINITE", true, false, true, _doubleType)];
    doubleElement.fields = fields;
    int fieldCount = fields.length;
    List<PropertyAccessorElement> accessors = new List<PropertyAccessorElement>(fieldCount);
    for (int i = 0; i < fieldCount; i++) {
      accessors[i] = fields[i].getter;
    }
    doubleElement.accessors = accessors;
    doubleElement.methods = <MethodElement> [
        ElementFactory.methodElement("remainder", _doubleType, [_numType]),
        ElementFactory.methodElement("+", _doubleType, [_numType]),
        ElementFactory.methodElement("-", _doubleType, [_numType]),
        ElementFactory.methodElement("*", _doubleType, [_numType]),
        ElementFactory.methodElement("%", _doubleType, [_numType]),
        ElementFactory.methodElement("/", _doubleType, [_numType]),
        ElementFactory.methodElement("~/", _doubleType, [_numType]),
        ElementFactory.methodElement("-", _doubleType, []),
        ElementFactory.methodElement("abs", _doubleType, []),
        ElementFactory.methodElement("round", _doubleType, []),
        ElementFactory.methodElement("floor", _doubleType, []),
        ElementFactory.methodElement("ceil", _doubleType, []),
        ElementFactory.methodElement("truncate", _doubleType, []),
        ElementFactory.methodElement("toString", _stringType, [])];
  }

  /**
   * Given a class element representing a class with type parameters, propagate those type
   * parameters to all of the accessors, methods and constructors defined for the class.
   *
   * @param classElement the element representing the class with type parameters
   */
  void propagateTypeArguments(ClassElementImpl classElement) {
    List<Type2> typeArguments = TypeParameterTypeImpl.getTypes(classElement.typeParameters);
    for (PropertyAccessorElement accessor in classElement.accessors) {
      FunctionTypeImpl functionType = accessor.type as FunctionTypeImpl;
      functionType.typeArguments = typeArguments;
    }
    for (MethodElement method in classElement.methods) {
      FunctionTypeImpl functionType = method.type as FunctionTypeImpl;
      functionType.typeArguments = typeArguments;
    }
    for (ConstructorElement constructor in classElement.constructors) {
      FunctionTypeImpl functionType = constructor.type as FunctionTypeImpl;
      functionType.typeArguments = typeArguments;
    }
  }
}
/**
 * The class `AnalysisContextFactory` defines utility methods used to create analysis contexts
 * for testing purposes.
 */
class AnalysisContextFactory {

  /**
   * Create an analysis context that has a fake core library already resolved.
   *
   * @return the analysis context that was created
   */
  static AnalysisContextImpl contextWithCore() {
    AnalysisContext sdkContext = DirectoryBasedDartSdk.defaultSdk.context;
    SourceFactory sourceFactory = sdkContext.sourceFactory;
    TestTypeProvider provider = new TestTypeProvider();
    CompilationUnitElementImpl coreUnit = new CompilationUnitElementImpl("core.dart");
    Source coreSource = sourceFactory.forUri(DartSdk.DART_CORE);
    sdkContext.setContents(coreSource, "");
    coreUnit.source = coreSource;
    coreUnit.types = <ClassElement> [
        provider.boolType.element,
        provider.doubleType.element,
        provider.functionType.element,
        provider.intType.element,
        provider.listType.element,
        provider.mapType.element,
        provider.nullType.element,
        provider.numType.element,
        provider.objectType.element,
        provider.stackTraceType.element,
        provider.stringType.element,
        provider.symbolType.element,
        provider.typeType.element];
    coreUnit.functions = <FunctionElement> [ElementFactory.functionElement3("identical", provider.boolType.element, <ClassElement> [provider.objectType.element, provider.objectType.element], null)];
    LibraryElementImpl coreLibrary = new LibraryElementImpl(sdkContext, ASTFactory.libraryIdentifier2(["dart", "core"]));
    coreLibrary.definingCompilationUnit = coreUnit;
    CompilationUnitElementImpl htmlUnit = new CompilationUnitElementImpl("html_dartium.dart");
    Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
    sdkContext.setContents(htmlSource, "");
    htmlUnit.source = htmlSource;
    ClassElementImpl elementElement = ElementFactory.classElement2("Element", []);
    InterfaceType elementType = elementElement.type;
    ClassElementImpl documentElement = ElementFactory.classElement("Document", elementType, []);
    ClassElementImpl htmlDocumentElement = ElementFactory.classElement("HtmlDocument", documentElement.type, []);
    htmlDocumentElement.methods = <MethodElement> [ElementFactory.methodElement("query", elementType, <Type2> [provider.stringType])];
    htmlUnit.types = <ClassElement> [
        ElementFactory.classElement("AnchorElement", elementType, []),
        ElementFactory.classElement("BodyElement", elementType, []),
        ElementFactory.classElement("ButtonElement", elementType, []),
        ElementFactory.classElement("DivElement", elementType, []),
        documentElement,
        elementElement,
        htmlDocumentElement,
        ElementFactory.classElement("InputElement", elementType, []),
        ElementFactory.classElement("SelectElement", elementType, [])];
    htmlUnit.functions = <FunctionElement> [ElementFactory.functionElement3("query", elementElement, <ClassElement> [provider.stringType.element], ClassElementImpl.EMPTY_ARRAY)];
    TopLevelVariableElementImpl document = ElementFactory.topLevelVariableElement3("document", true, htmlDocumentElement.type);
    htmlUnit.topLevelVariables = <TopLevelVariableElement> [document];
    htmlUnit.accessors = <PropertyAccessorElement> [document.getter];
    LibraryElementImpl htmlLibrary = new LibraryElementImpl(sdkContext, ASTFactory.libraryIdentifier2(["dart", "dom", "html"]));
    htmlLibrary.definingCompilationUnit = htmlUnit;
    Map<Source, LibraryElement> elementMap = new Map<Source, LibraryElement>();
    elementMap[coreSource] = coreLibrary;
    elementMap[htmlSource] = htmlLibrary;
    ((sdkContext as AnalysisContextImpl)).recordLibraryElements(elementMap);
    AnalysisContextImpl context = new DelegatingAnalysisContextImpl();
    sourceFactory = new SourceFactory.con2([
        new DartUriResolver(sdkContext.sourceFactory.dartSdk),
        new FileUriResolver()]);
    context.sourceFactory = sourceFactory;
    return context;
  }
}
class LibraryImportScopeTest extends ResolverTestCase {
  void test_conflictingImports() {
    AnalysisContext context = new AnalysisContextImpl();
    String typeNameA = "A";
    String typeNameB = "B";
    String typeNameC = "C";
    ClassElement typeA = ElementFactory.classElement2(typeNameA, []);
    ClassElement typeB1 = ElementFactory.classElement2(typeNameB, []);
    ClassElement typeB2 = ElementFactory.classElement2(typeNameB, []);
    ClassElement typeC = ElementFactory.classElement2(typeNameC, []);
    LibraryElement importedLibrary1 = createTestLibrary2(context, "imported1", []);
    ((importedLibrary1.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [typeA, typeB1];
    ImportElementImpl import1 = ElementFactory.importFor(importedLibrary1, null, []);
    LibraryElement importedLibrary2 = createTestLibrary2(context, "imported2", []);
    ((importedLibrary2.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [typeB2, typeC];
    ImportElementImpl import2 = ElementFactory.importFor(importedLibrary2, null, []);
    LibraryElementImpl importingLibrary = createTestLibrary2(context, "importing", []);
    importingLibrary.imports = <ImportElement> [import1, import2];
    {
      GatheringErrorListener errorListener = new GatheringErrorListener();
      Scope scope = new LibraryImportScope(importingLibrary, errorListener);
      JUnitTestCase.assertEquals(typeA, scope.lookup(ASTFactory.identifier3(typeNameA), importingLibrary));
      errorListener.assertNoErrors();
      JUnitTestCase.assertEquals(typeC, scope.lookup(ASTFactory.identifier3(typeNameC), importingLibrary));
      errorListener.assertNoErrors();
      Element element = scope.lookup(ASTFactory.identifier3(typeNameB), importingLibrary);
      errorListener.assertErrors2([StaticWarningCode.AMBIGUOUS_IMPORT]);
      EngineTestCase.assertInstanceOf(MultiplyDefinedElement, element);
      List<Element> conflictingElements = ((element as MultiplyDefinedElement)).conflictingElements;
      EngineTestCase.assertLength(2, conflictingElements);
      if (identical(conflictingElements[0], typeB1)) {
        JUnitTestCase.assertSame(typeB2, conflictingElements[1]);
      } else if (identical(conflictingElements[0], typeB2)) {
        JUnitTestCase.assertSame(typeB1, conflictingElements[1]);
      } else {
        JUnitTestCase.assertSame(typeB1, conflictingElements[0]);
      }
    }
    {
      GatheringErrorListener errorListener = new GatheringErrorListener();
      Scope scope = new LibraryImportScope(importingLibrary, errorListener);
      Identifier identifier = ASTFactory.identifier3(typeNameB);
      ASTFactory.methodDeclaration(null, ASTFactory.typeName3(identifier, []), null, null, ASTFactory.identifier3("foo"), null);
      Element element = scope.lookup(identifier, importingLibrary);
      errorListener.assertErrors2([StaticWarningCode.AMBIGUOUS_IMPORT]);
      EngineTestCase.assertInstanceOf(MultiplyDefinedElement, element);
    }
  }
  void test_creation_empty() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    new LibraryImportScope(definingLibrary, errorListener);
  }
  void test_creation_nonEmpty() {
    AnalysisContext context = new AnalysisContextImpl();
    String importedTypeName = "A";
    ClassElement importedType = new ClassElementImpl(ASTFactory.identifier3(importedTypeName));
    LibraryElement importedLibrary = createTestLibrary2(context, "imported", []);
    ((importedLibrary.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [importedType];
    LibraryElementImpl definingLibrary = createTestLibrary2(context, "importing", []);
    ImportElementImpl importElement = new ImportElementImpl();
    importElement.importedLibrary = importedLibrary;
    definingLibrary.imports = <ImportElement> [importElement];
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(definingLibrary, errorListener);
    JUnitTestCase.assertEquals(importedType, scope.lookup(ASTFactory.identifier3(importedTypeName), definingLibrary));
  }
  void test_getErrorListener() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    LibraryImportScope scope = new LibraryImportScope(definingLibrary, errorListener);
    JUnitTestCase.assertEquals(errorListener, scope.errorListener);
  }
  void test_nonConflictingImports_fromSdk() {
    AnalysisContext context = AnalysisContextFactory.contextWithCore();
    String typeName = "List";
    ClassElement type = ElementFactory.classElement2(typeName, []);
    LibraryElement importedLibrary = createTestLibrary2(context, "lib", []);
    ((importedLibrary.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [type];
    ImportElementImpl importCore = ElementFactory.importFor(context.getLibraryElement(context.sourceFactory.forUri("dart:core")), null, []);
    ImportElementImpl importLib = ElementFactory.importFor(importedLibrary, null, []);
    LibraryElementImpl importingLibrary = createTestLibrary2(context, "importing", []);
    importingLibrary.imports = <ImportElement> [importCore, importLib];
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(importingLibrary, errorListener);
    JUnitTestCase.assertEquals(type, scope.lookup(ASTFactory.identifier3(typeName), importingLibrary));
    errorListener.assertErrors2([StaticWarningCode.CONFLICTING_DART_IMPORT]);
  }
  void test_nonConflictingImports_sameElement() {
    AnalysisContext context = new AnalysisContextImpl();
    String typeNameA = "A";
    String typeNameB = "B";
    ClassElement typeA = ElementFactory.classElement2(typeNameA, []);
    ClassElement typeB = ElementFactory.classElement2(typeNameB, []);
    LibraryElement importedLibrary = createTestLibrary2(context, "imported", []);
    ((importedLibrary.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [typeA, typeB];
    ImportElementImpl import1 = ElementFactory.importFor(importedLibrary, null, []);
    ImportElementImpl import2 = ElementFactory.importFor(importedLibrary, null, []);
    LibraryElementImpl importingLibrary = createTestLibrary2(context, "importing", []);
    importingLibrary.imports = <ImportElement> [import1, import2];
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(importingLibrary, errorListener);
    JUnitTestCase.assertEquals(typeA, scope.lookup(ASTFactory.identifier3(typeNameA), importingLibrary));
    errorListener.assertNoErrors();
    JUnitTestCase.assertEquals(typeB, scope.lookup(ASTFactory.identifier3(typeNameB), importingLibrary));
    errorListener.assertNoErrors();
  }
  void test_prefixedAndNonPrefixed() {
    AnalysisContext context = new AnalysisContextImpl();
    String typeName = "C";
    String prefixName = "p";
    ClassElement prefixedType = ElementFactory.classElement2(typeName, []);
    ClassElement nonPrefixedType = ElementFactory.classElement2(typeName, []);
    LibraryElement prefixedLibrary = createTestLibrary2(context, "import.prefixed", []);
    ((prefixedLibrary.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [prefixedType];
    ImportElementImpl prefixedImport = ElementFactory.importFor(prefixedLibrary, ElementFactory.prefix(prefixName), []);
    LibraryElement nonPrefixedLibrary = createTestLibrary2(context, "import.nonPrefixed", []);
    ((nonPrefixedLibrary.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [nonPrefixedType];
    ImportElementImpl nonPrefixedImport = ElementFactory.importFor(nonPrefixedLibrary, null, []);
    LibraryElementImpl importingLibrary = createTestLibrary2(context, "importing", []);
    importingLibrary.imports = <ImportElement> [prefixedImport, nonPrefixedImport];
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(importingLibrary, errorListener);
    Element prefixedElement = scope.lookup(ASTFactory.identifier5(prefixName, typeName), importingLibrary);
    errorListener.assertNoErrors();
    JUnitTestCase.assertSame(prefixedType, prefixedElement);
    Element nonPrefixedElement = scope.lookup(ASTFactory.identifier3(typeName), importingLibrary);
    errorListener.assertNoErrors();
    JUnitTestCase.assertSame(nonPrefixedType, nonPrefixedElement);
  }
  static dartSuite() {
    _ut.group('LibraryImportScopeTest', () {
      _ut.test('test_conflictingImports', () {
        final __test = new LibraryImportScopeTest();
        runJUnitTest(__test, __test.test_conflictingImports);
      });
      _ut.test('test_creation_empty', () {
        final __test = new LibraryImportScopeTest();
        runJUnitTest(__test, __test.test_creation_empty);
      });
      _ut.test('test_creation_nonEmpty', () {
        final __test = new LibraryImportScopeTest();
        runJUnitTest(__test, __test.test_creation_nonEmpty);
      });
      _ut.test('test_getErrorListener', () {
        final __test = new LibraryImportScopeTest();
        runJUnitTest(__test, __test.test_getErrorListener);
      });
      _ut.test('test_nonConflictingImports_fromSdk', () {
        final __test = new LibraryImportScopeTest();
        runJUnitTest(__test, __test.test_nonConflictingImports_fromSdk);
      });
      _ut.test('test_nonConflictingImports_sameElement', () {
        final __test = new LibraryImportScopeTest();
        runJUnitTest(__test, __test.test_nonConflictingImports_sameElement);
      });
      _ut.test('test_prefixedAndNonPrefixed', () {
        final __test = new LibraryImportScopeTest();
        runJUnitTest(__test, __test.test_prefixedAndNonPrefixed);
      });
    });
  }
}
/**
 * Instances of the class `ResolutionVerifier` verify that all of the nodes in an AST
 * structure that should have been resolved were resolved.
 */
class ResolutionVerifier extends RecursiveASTVisitor<Object> {

  /**
   * A set containing nodes that are known to not be resolvable and should therefore not cause the
   * test to fail.
   */
  Set<ASTNode> _knownExceptions;

  /**
   * A list containing all of the AST nodes that were not resolved.
   */
  List<ASTNode> _unresolvedNodes = new List<ASTNode>();

  /**
   * A list containing all of the AST nodes that were resolved to an element of the wrong type.
   */
  List<ASTNode> _wrongTypedNodes = new List<ASTNode>();

  /**
   * Initialize a newly created verifier to verify that all of the nodes in the visited AST
   * structures that are expected to have been resolved have an element associated with them.
   */
  ResolutionVerifier() : this.con1(null);

  /**
   * Initialize a newly created verifier to verify that all of the identifiers in the visited AST
   * structures that are expected to have been resolved have an element associated with them. Nodes
   * in the set of known exceptions are not expected to have been resolved, even if they normally
   * would have been expected to have been resolved.
   *
   * @param knownExceptions a set containing nodes that are known to not be resolvable and should
   *          therefore not cause the test to fail
   **/
  ResolutionVerifier.con1(Set<ASTNode> knownExceptions) {
    this._knownExceptions = knownExceptions;
  }

  /**
   * Assert that all of the visited identifiers were resolved.
   */
  void assertResolved() {
    if (!_unresolvedNodes.isEmpty || !_wrongTypedNodes.isEmpty) {
      PrintStringWriter writer = new PrintStringWriter();
      if (!_unresolvedNodes.isEmpty) {
        writer.print("Failed to resolve ");
        writer.print(_unresolvedNodes.length);
        writer.println(" nodes:");
        printNodes(writer, _unresolvedNodes);
      }
      if (!_wrongTypedNodes.isEmpty) {
        writer.print("Resolved ");
        writer.print(_wrongTypedNodes.length);
        writer.println(" to the wrong type of element:");
        printNodes(writer, _wrongTypedNodes);
      }
      JUnitTestCase.fail(writer.toString());
    }
  }
  Object visitBinaryExpression(BinaryExpression node) {
    node.visitChildren(this);
    if (!node.operator.isUserDefinableOperator) {
      return null;
    }
    Type2 operandType = node.leftOperand.staticType;
    if (operandType == null || operandType.isDynamic) {
      return null;
    }
    return checkResolved2(node, node.staticElement, MethodElement);
  }
  Object visitCommentReference(CommentReference node) => null;
  Object visitCompilationUnit(CompilationUnit node) {
    node.visitChildren(this);
    return checkResolved2(node, node.element, CompilationUnitElement);
  }
  Object visitExportDirective(ExportDirective node) => checkResolved2(node, node.element, ExportElement);
  Object visitFunctionDeclaration(FunctionDeclaration node) {
    node.visitChildren(this);
    if (node.element is LibraryElement) {
      _wrongTypedNodes.add(node);
    }
    return null;
  }
  Object visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    node.visitChildren(this);
    return null;
  }
  Object visitImportDirective(ImportDirective node) {
    checkResolved2(node, node.element, ImportElement);
    SimpleIdentifier prefix = node.prefix;
    if (prefix == null) {
      return null;
    }
    return checkResolved2(prefix, prefix.staticElement, PrefixElement);
  }
  Object visitIndexExpression(IndexExpression node) {
    node.visitChildren(this);
    Type2 targetType = node.realTarget.staticType;
    if (targetType == null || targetType.isDynamic) {
      return null;
    }
    return checkResolved2(node, node.staticElement, MethodElement);
  }
  Object visitLibraryDirective(LibraryDirective node) => checkResolved2(node, node.element, LibraryElement);
  Object visitNamedExpression(NamedExpression node) => node.expression.accept(this);
  Object visitPartDirective(PartDirective node) => checkResolved2(node, node.element, CompilationUnitElement);
  Object visitPartOfDirective(PartOfDirective node) => checkResolved2(node, node.element, LibraryElement);
  Object visitPostfixExpression(PostfixExpression node) {
    node.visitChildren(this);
    if (!node.operator.isUserDefinableOperator) {
      return null;
    }
    Type2 operandType = node.operand.staticType;
    if (operandType == null || operandType.isDynamic) {
      return null;
    }
    return checkResolved2(node, node.staticElement, MethodElement);
  }
  Object visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefix = node.prefix;
    prefix.accept(this);
    Type2 prefixType = prefix.staticType;
    if (prefixType == null || prefixType.isDynamic) {
      return null;
    }
    return checkResolved(node, node.staticElement);
  }
  Object visitPrefixExpression(PrefixExpression node) {
    node.visitChildren(this);
    if (!node.operator.isUserDefinableOperator) {
      return null;
    }
    Type2 operandType = node.operand.staticType;
    if (operandType == null || operandType.isDynamic) {
      return null;
    }
    return checkResolved2(node, node.staticElement, MethodElement);
  }
  Object visitPropertyAccess(PropertyAccess node) {
    Expression target = node.realTarget;
    target.accept(this);
    Type2 targetType = target.staticType;
    if (targetType == null || targetType.isDynamic) {
      return null;
    }
    return node.propertyName.accept(this);
  }
  Object visitSimpleIdentifier(SimpleIdentifier node) {
    if (node.name == "void") {
      return null;
    }
    ASTNode parent = node.parent;
    if (parent is MethodInvocation) {
      MethodInvocation invocation = parent as MethodInvocation;
      if (identical(invocation.methodName, node)) {
        Expression target = invocation.realTarget;
        Type2 targetType = target == null ? null : target.staticType;
        if (targetType == null || targetType.isDynamic) {
          return null;
        }
      }
    }
    return checkResolved(node, node.staticElement);
  }
  Object checkResolved(ASTNode node, Element element) => checkResolved2(node, element, null);
  Object checkResolved2(ASTNode node, Element element, Type expectedClass) {
    if (element == null) {
      if (_knownExceptions == null || !_knownExceptions.contains(node)) {
        _unresolvedNodes.add(node);
      }
    } else if (expectedClass != null) {
      if (!isInstanceOf(element, expectedClass)) {
        _wrongTypedNodes.add(node);
      }
    }
    return null;
  }
  String getFileName(ASTNode node) {
    if (node != null) {
      ASTNode root = node.root;
      if (root is CompilationUnit) {
        CompilationUnit rootCU = root as CompilationUnit;
        if (rootCU.element != null) {
          return rootCU.element.source.fullName;
        } else {
          return "<unknown file- CompilationUnit.getElement() returned null>";
        }
      } else {
        return "<unknown file- CompilationUnit.getRoot() is not a CompilationUnit>";
      }
    }
    return "<unknown file- ASTNode is null>";
  }
  void printNodes(PrintStringWriter writer, List<ASTNode> nodes) {
    for (ASTNode identifier in nodes) {
      writer.print("  ");
      writer.print(identifier.toString());
      writer.print(" (");
      writer.print(getFileName(identifier));
      writer.print(" : ");
      writer.print(identifier.offset);
      writer.println(")");
    }
  }
}
class MemberMapTest extends JUnitTestCase {

  /**
   * The null type.
   */
  InterfaceType _nullType;
  void setUp() {
    _nullType = new TestTypeProvider().nullType;
  }
  void test_MemberMap_copyConstructor() {
    MethodElement m1 = ElementFactory.methodElement("m1", _nullType, []);
    MethodElement m2 = ElementFactory.methodElement("m2", _nullType, []);
    MethodElement m3 = ElementFactory.methodElement("m3", _nullType, []);
    MemberMap map = new MemberMap();
    map.put(m1.name, m1);
    map.put(m2.name, m2);
    map.put(m3.name, m3);
    MemberMap copy = new MemberMap.con2(map);
    JUnitTestCase.assertEquals(map.size, copy.size);
    JUnitTestCase.assertEquals(m1, copy.get(m1.name));
    JUnitTestCase.assertEquals(m2, copy.get(m2.name));
    JUnitTestCase.assertEquals(m3, copy.get(m3.name));
  }
  void test_MemberMap_override() {
    MethodElement m1 = ElementFactory.methodElement("m", _nullType, []);
    MethodElement m2 = ElementFactory.methodElement("m", _nullType, []);
    MemberMap map = new MemberMap();
    map.put(m1.name, m1);
    map.put(m2.name, m2);
    JUnitTestCase.assertEquals(1, map.size);
    JUnitTestCase.assertEquals(m2, map.get("m"));
  }
  void test_MemberMap_put() {
    MethodElement m1 = ElementFactory.methodElement("m1", _nullType, []);
    MemberMap map = new MemberMap();
    JUnitTestCase.assertEquals(0, map.size);
    map.put(m1.name, m1);
    JUnitTestCase.assertEquals(1, map.size);
    JUnitTestCase.assertEquals(m1, map.get("m1"));
  }
  static dartSuite() {
    _ut.group('MemberMapTest', () {
      _ut.test('test_MemberMap_copyConstructor', () {
        final __test = new MemberMapTest();
        runJUnitTest(__test, __test.test_MemberMap_copyConstructor);
      });
      _ut.test('test_MemberMap_override', () {
        final __test = new MemberMapTest();
        runJUnitTest(__test, __test.test_MemberMap_override);
      });
      _ut.test('test_MemberMap_put', () {
        final __test = new MemberMapTest();
        runJUnitTest(__test, __test.test_MemberMap_put);
      });
    });
  }
}
class LibraryScopeTest extends ResolverTestCase {
  void test_creation_empty() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    new LibraryScope(definingLibrary, errorListener);
  }
  void test_creation_nonEmpty() {
    AnalysisContext context = new AnalysisContextImpl();
    String importedTypeName = "A";
    ClassElement importedType = new ClassElementImpl(ASTFactory.identifier3(importedTypeName));
    LibraryElement importedLibrary = createTestLibrary2(context, "imported", []);
    ((importedLibrary.definingCompilationUnit as CompilationUnitElementImpl)).types = <ClassElement> [importedType];
    LibraryElementImpl definingLibrary = createTestLibrary2(context, "importing", []);
    ImportElementImpl importElement = new ImportElementImpl();
    importElement.importedLibrary = importedLibrary;
    definingLibrary.imports = <ImportElement> [importElement];
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryScope(definingLibrary, errorListener);
    JUnitTestCase.assertEquals(importedType, scope.lookup(ASTFactory.identifier3(importedTypeName), definingLibrary));
  }
  void test_getErrorListener() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    LibraryScope scope = new LibraryScope(definingLibrary, errorListener);
    JUnitTestCase.assertEquals(errorListener, scope.errorListener);
  }
  static dartSuite() {
    _ut.group('LibraryScopeTest', () {
      _ut.test('test_creation_empty', () {
        final __test = new LibraryScopeTest();
        runJUnitTest(__test, __test.test_creation_empty);
      });
      _ut.test('test_creation_nonEmpty', () {
        final __test = new LibraryScopeTest();
        runJUnitTest(__test, __test.test_creation_nonEmpty);
      });
      _ut.test('test_getErrorListener', () {
        final __test = new LibraryScopeTest();
        runJUnitTest(__test, __test.test_getErrorListener);
      });
    });
  }
}
class StaticTypeAnalyzerTest extends EngineTestCase {

  /**
   * The error listener to which errors will be reported.
   */
  GatheringErrorListener _listener;

  /**
   * The resolver visitor used to create the analyzer.
   */
  ResolverVisitor _visitor;

  /**
   * The analyzer being used to analyze the test cases.
   */
  StaticTypeAnalyzer _analyzer;

  /**
   * The type provider used to access the types.
   */
  TestTypeProvider _typeProvider;
  void fail_visitFunctionExpressionInvocation() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitMethodInvocation() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void fail_visitSimpleIdentifier() {
    JUnitTestCase.fail("Not yet tested");
    _listener.assertNoErrors();
  }
  void setUp() {
    _listener = new GatheringErrorListener();
    _typeProvider = new TestTypeProvider();
    _analyzer = createAnalyzer();
  }
  void test_visitAdjacentStrings() {
    Expression node = ASTFactory.adjacentStrings([resolvedString("a"), resolvedString("b")]);
    JUnitTestCase.assertSame(_typeProvider.stringType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitArgumentDefinitionTest() {
    Expression node = ASTFactory.argumentDefinitionTest("p");
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitAsExpression() {
    ClassElement superclass = ElementFactory.classElement2("A", []);
    InterfaceType superclassType = superclass.type;
    ClassElement subclass = ElementFactory.classElement("B", superclassType, []);
    Expression node = ASTFactory.asExpression(ASTFactory.thisExpression(), ASTFactory.typeName(subclass, []));
    JUnitTestCase.assertSame(subclass.type, analyze2(node, superclassType));
    _listener.assertNoErrors();
  }
  void test_visitAssignmentExpression_compound() {
    InterfaceType numType = _typeProvider.numType;
    SimpleIdentifier identifier = resolvedVariable(_typeProvider.intType, "i");
    AssignmentExpression node = ASTFactory.assignmentExpression(identifier, TokenType.PLUS_EQ, resolvedInteger(1));
    MethodElement plusMethod = getMethod(numType, "+");
    node.staticElement = plusMethod;
    JUnitTestCase.assertSame(numType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitAssignmentExpression_simple() {
    InterfaceType intType = _typeProvider.intType;
    Expression node = ASTFactory.assignmentExpression(resolvedVariable(intType, "i"), TokenType.EQ, resolvedInteger(0));
    JUnitTestCase.assertSame(intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_equals() {
    Expression node = ASTFactory.binaryExpression(resolvedInteger(2), TokenType.EQ_EQ, resolvedInteger(3));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_logicalAnd() {
    Expression node = ASTFactory.binaryExpression(ASTFactory.booleanLiteral(false), TokenType.AMPERSAND_AMPERSAND, ASTFactory.booleanLiteral(true));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_logicalOr() {
    Expression node = ASTFactory.binaryExpression(ASTFactory.booleanLiteral(false), TokenType.BAR_BAR, ASTFactory.booleanLiteral(true));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_notEquals() {
    Expression node = ASTFactory.binaryExpression(resolvedInteger(2), TokenType.BANG_EQ, resolvedInteger(3));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_plusID() {
    BinaryExpression node = ASTFactory.binaryExpression(resolvedInteger(1), TokenType.PLUS, resolvedDouble(2.0));
    node.staticElement = getMethod(_typeProvider.numType, "+");
    JUnitTestCase.assertSame(_typeProvider.doubleType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_plusII() {
    BinaryExpression node = ASTFactory.binaryExpression(resolvedInteger(1), TokenType.PLUS, resolvedInteger(2));
    node.staticElement = getMethod(_typeProvider.numType, "+");
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_slash() {
    BinaryExpression node = ASTFactory.binaryExpression(resolvedInteger(2), TokenType.SLASH, resolvedInteger(2));
    node.staticElement = getMethod(_typeProvider.numType, "/");
    JUnitTestCase.assertSame(_typeProvider.doubleType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_star_notSpecial() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    InterfaceType typeA = classA.type;
    MethodElement operator = ElementFactory.methodElement("*", typeA, [_typeProvider.doubleType]);
    classA.methods = <MethodElement> [operator];
    BinaryExpression node = ASTFactory.binaryExpression(ASTFactory.asExpression(ASTFactory.identifier3("a"), ASTFactory.typeName(classA, [])), TokenType.PLUS, resolvedDouble(2.0));
    node.staticElement = operator;
    JUnitTestCase.assertSame(typeA, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBinaryExpression_starID() {
    BinaryExpression node = ASTFactory.binaryExpression(resolvedInteger(1), TokenType.PLUS, resolvedDouble(2.0));
    node.staticElement = getMethod(_typeProvider.numType, "*");
    JUnitTestCase.assertSame(_typeProvider.doubleType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBooleanLiteral_false() {
    Expression node = ASTFactory.booleanLiteral(false);
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitBooleanLiteral_true() {
    Expression node = ASTFactory.booleanLiteral(true);
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitCascadeExpression() {
    Expression node = ASTFactory.cascadeExpression(resolvedString("a"), [ASTFactory.propertyAccess2(null, "length")]);
    JUnitTestCase.assertSame(_typeProvider.stringType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitConditionalExpression_differentTypes() {
    Expression node = ASTFactory.conditionalExpression(ASTFactory.booleanLiteral(true), resolvedDouble(1.0), resolvedInteger(0));
    JUnitTestCase.assertSame(_typeProvider.numType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitConditionalExpression_sameTypes() {
    Expression node = ASTFactory.conditionalExpression(ASTFactory.booleanLiteral(true), resolvedInteger(1), resolvedInteger(0));
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitDoubleLiteral() {
    Expression node = ASTFactory.doubleLiteral(4.33);
    JUnitTestCase.assertSame(_typeProvider.doubleType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_named_block() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p1 = ASTFactory.namedFormalParameter(ASTFactory.simpleFormalParameter3("p1"), resolvedInteger(0));
    setType(p1, dynamicType);
    FormalParameter p2 = ASTFactory.namedFormalParameter(ASTFactory.simpleFormalParameter3("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p1, p2]), ASTFactory.blockFunctionBody2([]));
    analyze3(p1);
    analyze3(p2);
    Type2 resultType = analyze(node);
    Map<String, Type2> expectedNamedTypes = new Map<String, Type2>();
    expectedNamedTypes["p1"] = dynamicType;
    expectedNamedTypes["p2"] = dynamicType;
    assertFunctionType(dynamicType, null, null, expectedNamedTypes, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_named_expression() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p = ASTFactory.namedFormalParameter(ASTFactory.simpleFormalParameter3("p"), resolvedInteger(0));
    setType(p, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p]), ASTFactory.expressionFunctionBody(resolvedInteger(0)));
    analyze3(p);
    Type2 resultType = analyze(node);
    Map<String, Type2> expectedNamedTypes = new Map<String, Type2>();
    expectedNamedTypes["p"] = dynamicType;
    assertFunctionType(_typeProvider.intType, null, null, expectedNamedTypes, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_normal_block() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p1 = ASTFactory.simpleFormalParameter3("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = ASTFactory.simpleFormalParameter3("p2");
    setType(p2, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p1, p2]), ASTFactory.blockFunctionBody2([]));
    analyze3(p1);
    analyze3(p2);
    Type2 resultType = analyze(node);
    assertFunctionType(dynamicType, <Type2> [dynamicType, dynamicType], null, null, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_normal_expression() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p = ASTFactory.simpleFormalParameter3("p");
    setType(p, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p]), ASTFactory.expressionFunctionBody(resolvedInteger(0)));
    analyze3(p);
    Type2 resultType = analyze(node);
    assertFunctionType(_typeProvider.intType, <Type2> [dynamicType], null, null, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_normalAndNamed_block() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p1 = ASTFactory.simpleFormalParameter3("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = ASTFactory.namedFormalParameter(ASTFactory.simpleFormalParameter3("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p1, p2]), ASTFactory.blockFunctionBody2([]));
    analyze3(p2);
    Type2 resultType = analyze(node);
    Map<String, Type2> expectedNamedTypes = new Map<String, Type2>();
    expectedNamedTypes["p2"] = dynamicType;
    assertFunctionType(dynamicType, <Type2> [dynamicType], null, expectedNamedTypes, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_normalAndNamed_expression() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p1 = ASTFactory.simpleFormalParameter3("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = ASTFactory.namedFormalParameter(ASTFactory.simpleFormalParameter3("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p1, p2]), ASTFactory.expressionFunctionBody(resolvedInteger(0)));
    analyze3(p2);
    Type2 resultType = analyze(node);
    Map<String, Type2> expectedNamedTypes = new Map<String, Type2>();
    expectedNamedTypes["p2"] = dynamicType;
    assertFunctionType(_typeProvider.intType, <Type2> [dynamicType], null, expectedNamedTypes, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_normalAndPositional_block() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p1 = ASTFactory.simpleFormalParameter3("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = ASTFactory.positionalFormalParameter(ASTFactory.simpleFormalParameter3("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p1, p2]), ASTFactory.blockFunctionBody2([]));
    analyze3(p1);
    analyze3(p2);
    Type2 resultType = analyze(node);
    assertFunctionType(dynamicType, <Type2> [dynamicType], <Type2> [dynamicType], null, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_normalAndPositional_expression() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p1 = ASTFactory.simpleFormalParameter3("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = ASTFactory.positionalFormalParameter(ASTFactory.simpleFormalParameter3("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p1, p2]), ASTFactory.expressionFunctionBody(resolvedInteger(0)));
    analyze3(p1);
    analyze3(p2);
    Type2 resultType = analyze(node);
    assertFunctionType(_typeProvider.intType, <Type2> [dynamicType], <Type2> [dynamicType], null, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_positional_block() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p1 = ASTFactory.positionalFormalParameter(ASTFactory.simpleFormalParameter3("p1"), resolvedInteger(0));
    setType(p1, dynamicType);
    FormalParameter p2 = ASTFactory.positionalFormalParameter(ASTFactory.simpleFormalParameter3("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p1, p2]), ASTFactory.blockFunctionBody2([]));
    analyze3(p1);
    analyze3(p2);
    Type2 resultType = analyze(node);
    assertFunctionType(dynamicType, null, <Type2> [dynamicType, dynamicType], null, resultType);
    _listener.assertNoErrors();
  }
  void test_visitFunctionExpression_positional_expression() {
    Type2 dynamicType = _typeProvider.dynamicType;
    FormalParameter p = ASTFactory.positionalFormalParameter(ASTFactory.simpleFormalParameter3("p"), resolvedInteger(0));
    setType(p, dynamicType);
    FunctionExpression node = resolvedFunctionExpression(ASTFactory.formalParameterList([p]), ASTFactory.expressionFunctionBody(resolvedInteger(0)));
    analyze3(p);
    Type2 resultType = analyze(node);
    assertFunctionType(_typeProvider.intType, null, <Type2> [dynamicType], null, resultType);
    _listener.assertNoErrors();
  }
  void test_visitIndexExpression_getter() {
    InterfaceType listType = _typeProvider.listType;
    SimpleIdentifier identifier = resolvedVariable(listType, "a");
    IndexExpression node = ASTFactory.indexExpression(identifier, resolvedInteger(2));
    MethodElement indexMethod = listType.element.methods[0];
    node.staticElement = indexMethod;
    JUnitTestCase.assertSame(listType.typeArguments[0], analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitIndexExpression_setter() {
    InterfaceType listType = _typeProvider.listType;
    SimpleIdentifier identifier = resolvedVariable(listType, "a");
    IndexExpression node = ASTFactory.indexExpression(identifier, resolvedInteger(2));
    MethodElement indexMethod = listType.element.methods[1];
    node.staticElement = indexMethod;
    ASTFactory.assignmentExpression(node, TokenType.EQ, ASTFactory.integer(0));
    JUnitTestCase.assertSame(listType.typeArguments[0], analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitIndexExpression_typeParameters() {
    InterfaceType intType = _typeProvider.intType;
    InterfaceType listType = _typeProvider.listType;
    MethodElement methodElement = getMethod(listType, "[]");
    SimpleIdentifier identifier = ASTFactory.identifier3("list");
    InterfaceType listOfIntType = listType.substitute4(<Type2> [intType]);
    identifier.staticType = listOfIntType;
    IndexExpression indexExpression = ASTFactory.indexExpression(identifier, ASTFactory.integer(0));
    MethodElement indexMethod = MethodMember.from(methodElement, listOfIntType);
    indexExpression.staticElement = indexMethod;
    JUnitTestCase.assertSame(intType, analyze(indexExpression));
    _listener.assertNoErrors();
  }
  void test_visitIndexExpression_typeParameters_inSetterContext() {
    InterfaceType intType = _typeProvider.intType;
    InterfaceType listType = _typeProvider.listType;
    MethodElement methodElement = getMethod(listType, "[]=");
    SimpleIdentifier identifier = ASTFactory.identifier3("list");
    InterfaceType listOfIntType = listType.substitute4(<Type2> [intType]);
    identifier.staticType = listOfIntType;
    IndexExpression indexExpression = ASTFactory.indexExpression(identifier, ASTFactory.integer(0));
    MethodElement indexMethod = MethodMember.from(methodElement, listOfIntType);
    indexExpression.staticElement = indexMethod;
    ASTFactory.assignmentExpression(indexExpression, TokenType.EQ, ASTFactory.integer(0));
    JUnitTestCase.assertSame(intType, analyze(indexExpression));
    _listener.assertNoErrors();
  }
  void test_visitInstanceCreationExpression_named() {
    ClassElementImpl classElement = ElementFactory.classElement2("C", []);
    String constructorName = "m";
    ConstructorElementImpl constructor = ElementFactory.constructorElement(classElement, constructorName);
    constructor.returnType = classElement.type;
    FunctionTypeImpl constructorType = new FunctionTypeImpl.con1(constructor);
    constructor.type = constructorType;
    classElement.constructors = <ConstructorElement> [constructor];
    InstanceCreationExpression node = ASTFactory.instanceCreationExpression2(null, ASTFactory.typeName(classElement, []), [ASTFactory.identifier3(constructorName)]);
    node.staticElement = constructor;
    JUnitTestCase.assertSame(classElement.type, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitInstanceCreationExpression_typeParameters() {
    ClassElementImpl elementC = ElementFactory.classElement2("C", ["E"]);
    ClassElementImpl elementI = ElementFactory.classElement2("I", []);
    ConstructorElementImpl constructor = ElementFactory.constructorElement(elementC, null);
    elementC.constructors = <ConstructorElement> [constructor];
    constructor.returnType = elementC.type;
    FunctionTypeImpl constructorType = new FunctionTypeImpl.con1(constructor);
    constructor.type = constructorType;
    TypeName typeName = ASTFactory.typeName(elementC, [ASTFactory.typeName(elementI, [])]);
    typeName.type = elementC.type.substitute4(<Type2> [elementI.type]);
    InstanceCreationExpression node = ASTFactory.instanceCreationExpression2(null, typeName, []);
    node.staticElement = constructor;
    InterfaceType interfaceType = analyze(node) as InterfaceType;
    List<Type2> typeArgs = interfaceType.typeArguments;
    JUnitTestCase.assertEquals(1, typeArgs.length);
    JUnitTestCase.assertEquals(elementI.type, typeArgs[0]);
    _listener.assertNoErrors();
  }
  void test_visitInstanceCreationExpression_unnamed() {
    ClassElementImpl classElement = ElementFactory.classElement2("C", []);
    ConstructorElementImpl constructor = ElementFactory.constructorElement(classElement, null);
    constructor.returnType = classElement.type;
    FunctionTypeImpl constructorType = new FunctionTypeImpl.con1(constructor);
    constructor.type = constructorType;
    classElement.constructors = <ConstructorElement> [constructor];
    InstanceCreationExpression node = ASTFactory.instanceCreationExpression2(null, ASTFactory.typeName(classElement, []), []);
    node.staticElement = constructor;
    JUnitTestCase.assertSame(classElement.type, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitIntegerLiteral() {
    Expression node = resolvedInteger(42);
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitIsExpression_negated() {
    Expression node = ASTFactory.isExpression(resolvedString("a"), true, ASTFactory.typeName4("String", []));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitIsExpression_notNegated() {
    Expression node = ASTFactory.isExpression(resolvedString("a"), false, ASTFactory.typeName4("String", []));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitListLiteral_empty() {
    Expression node = ASTFactory.listLiteral([]);
    Type2 resultType = analyze(node);
    assertType2(_typeProvider.listType.substitute4(<Type2> [_typeProvider.dynamicType]), resultType);
    _listener.assertNoErrors();
  }
  void test_visitListLiteral_nonEmpty() {
    Expression node = ASTFactory.listLiteral([resolvedInteger(0)]);
    Type2 resultType = analyze(node);
    assertType2(_typeProvider.listType.substitute4(<Type2> [_typeProvider.dynamicType]), resultType);
    _listener.assertNoErrors();
  }
  void test_visitMapLiteral_empty() {
    Expression node = ASTFactory.mapLiteral2([]);
    Type2 resultType = analyze(node);
    assertType2(_typeProvider.mapType.substitute4(<Type2> [_typeProvider.dynamicType, _typeProvider.dynamicType]), resultType);
    _listener.assertNoErrors();
  }
  void test_visitMapLiteral_nonEmpty() {
    Expression node = ASTFactory.mapLiteral2([ASTFactory.mapLiteralEntry("k", resolvedInteger(0))]);
    Type2 resultType = analyze(node);
    assertType2(_typeProvider.mapType.substitute4(<Type2> [_typeProvider.dynamicType, _typeProvider.dynamicType]), resultType);
    _listener.assertNoErrors();
  }
  void test_visitMethodInvocation_then() {
    Expression node = ASTFactory.methodInvocation(null, "then", []);
    analyze(node);
    _listener.assertNoErrors();
  }
  void test_visitNamedExpression() {
    Expression node = ASTFactory.namedExpression2("n", resolvedString("a"));
    JUnitTestCase.assertSame(_typeProvider.stringType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitNullLiteral() {
    Expression node = ASTFactory.nullLiteral();
    JUnitTestCase.assertSame(_typeProvider.bottomType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitParenthesizedExpression() {
    Expression node = ASTFactory.parenthesizedExpression(resolvedInteger(0));
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPostfixExpression_minusMinus() {
    PostfixExpression node = ASTFactory.postfixExpression(resolvedInteger(0), TokenType.MINUS_MINUS);
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPostfixExpression_plusPlus() {
    PostfixExpression node = ASTFactory.postfixExpression(resolvedInteger(0), TokenType.PLUS_PLUS);
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixedIdentifier_getter() {
    Type2 boolType = _typeProvider.boolType;
    PropertyAccessorElementImpl getter = ElementFactory.getterElement("b", false, boolType);
    PrefixedIdentifier node = ASTFactory.identifier5("a", "b");
    node.identifier.staticElement = getter;
    JUnitTestCase.assertSame(boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixedIdentifier_setter() {
    Type2 boolType = _typeProvider.boolType;
    FieldElementImpl field = ElementFactory.fieldElement("b", false, false, false, boolType);
    PropertyAccessorElement setter = field.setter;
    PrefixedIdentifier node = ASTFactory.identifier5("a", "b");
    node.identifier.staticElement = setter;
    JUnitTestCase.assertSame(boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixedIdentifier_variable() {
    VariableElementImpl variable = ElementFactory.localVariableElement2("b");
    variable.type = _typeProvider.boolType;
    PrefixedIdentifier node = ASTFactory.identifier5("a", "b");
    node.identifier.staticElement = variable;
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixExpression_bang() {
    PrefixExpression node = ASTFactory.prefixExpression(TokenType.BANG, resolvedInteger(0));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixExpression_minus() {
    PrefixExpression node = ASTFactory.prefixExpression(TokenType.MINUS, resolvedInteger(0));
    MethodElement minusMethod = getMethod(_typeProvider.numType, "-");
    node.staticElement = minusMethod;
    JUnitTestCase.assertSame(_typeProvider.numType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixExpression_minusMinus() {
    PrefixExpression node = ASTFactory.prefixExpression(TokenType.MINUS_MINUS, resolvedInteger(0));
    MethodElement minusMethod = getMethod(_typeProvider.numType, "-");
    node.staticElement = minusMethod;
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixExpression_not() {
    Expression node = ASTFactory.prefixExpression(TokenType.BANG, ASTFactory.booleanLiteral(true));
    JUnitTestCase.assertSame(_typeProvider.boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixExpression_plusPlus() {
    PrefixExpression node = ASTFactory.prefixExpression(TokenType.PLUS_PLUS, resolvedInteger(0));
    MethodElement plusMethod = getMethod(_typeProvider.numType, "+");
    node.staticElement = plusMethod;
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPrefixExpression_tilde() {
    PrefixExpression node = ASTFactory.prefixExpression(TokenType.TILDE, resolvedInteger(0));
    MethodElement tildeMethod = getMethod(_typeProvider.intType, "~");
    node.staticElement = tildeMethod;
    JUnitTestCase.assertSame(_typeProvider.intType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPropertyAccess_getter() {
    Type2 boolType = _typeProvider.boolType;
    PropertyAccessorElementImpl getter = ElementFactory.getterElement("b", false, boolType);
    PropertyAccess node = ASTFactory.propertyAccess2(ASTFactory.identifier3("a"), "b");
    node.propertyName.staticElement = getter;
    JUnitTestCase.assertSame(boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitPropertyAccess_setter() {
    Type2 boolType = _typeProvider.boolType;
    FieldElementImpl field = ElementFactory.fieldElement("b", false, false, false, boolType);
    PropertyAccessorElement setter = field.setter;
    PropertyAccess node = ASTFactory.propertyAccess2(ASTFactory.identifier3("a"), "b");
    node.propertyName.staticElement = setter;
    JUnitTestCase.assertSame(boolType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitSimpleStringLiteral() {
    Expression node = resolvedString("a");
    JUnitTestCase.assertSame(_typeProvider.stringType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitStringInterpolation() {
    Expression node = ASTFactory.string([
        ASTFactory.interpolationString("a", "a"),
        ASTFactory.interpolationExpression(resolvedString("b")),
        ASTFactory.interpolationString("c", "c")]);
    JUnitTestCase.assertSame(_typeProvider.stringType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitSuperExpression() {
    InterfaceType superType = ElementFactory.classElement2("A", []).type;
    InterfaceType thisType = ElementFactory.classElement("B", superType, []).type;
    Expression node = ASTFactory.superExpression();
    JUnitTestCase.assertSame(thisType, analyze2(node, thisType));
    _listener.assertNoErrors();
  }
  void test_visitSymbolLiteral() {
    JUnitTestCase.assertSame(_typeProvider.symbolType, analyze(ASTFactory.symbolLiteral(["a"])));
  }
  void test_visitThisExpression() {
    InterfaceType thisType = ElementFactory.classElement("B", ElementFactory.classElement2("A", []).type, []).type;
    Expression node = ASTFactory.thisExpression();
    JUnitTestCase.assertSame(thisType, analyze2(node, thisType));
    _listener.assertNoErrors();
  }
  void test_visitThrowExpression_withoutValue() {
    Expression node = ASTFactory.throwExpression();
    JUnitTestCase.assertSame(_typeProvider.bottomType, analyze(node));
    _listener.assertNoErrors();
  }
  void test_visitThrowExpression_withValue() {
    Expression node = ASTFactory.throwExpression2(resolvedInteger(0));
    JUnitTestCase.assertSame(_typeProvider.bottomType, analyze(node));
    _listener.assertNoErrors();
  }

  /**
   * Return the type associated with the given expression after the static type analyzer has
   * computed a type for it.
   *
   * @param node the expression with which the type is associated
   * @return the type associated with the expression
   */
  Type2 analyze(Expression node) => analyze2(node, null);

  /**
   * Return the type associated with the given expression after the static type analyzer has
   * computed a type for it.
   *
   * @param node the expression with which the type is associated
   * @param thisType the type of 'this'
   * @return the type associated with the expression
   */
  Type2 analyze2(Expression node, InterfaceType thisType) {
    try {
      _analyzer.thisType_J2DAccessor = thisType;
    } on JavaException catch (exception) {
      throw new IllegalArgumentException("Could not set type of 'this'", exception);
    }
    node.accept(_analyzer);
    return node.staticType;
  }

  /**
   * Return the type associated with the given parameter after the static type analyzer has computed
   * a type for it.
   *
   * @param node the parameter with which the type is associated
   * @return the type associated with the parameter
   */
  Type2 analyze3(FormalParameter node) {
    node.accept(_analyzer);
    return ((node.identifier.staticElement as ParameterElement)).type;
  }

  /**
   * Assert that the actual type is a function type with the expected characteristics.
   *
   * @param expectedReturnType the expected return type of the function
   * @param expectedNormalTypes the expected types of the normal parameters
   * @param expectedOptionalTypes the expected types of the optional parameters
   * @param expectedNamedTypes the expected types of the named parameters
   * @param actualType the type being tested
   */
  void assertFunctionType(Type2 expectedReturnType, List<Type2> expectedNormalTypes, List<Type2> expectedOptionalTypes, Map<String, Type2> expectedNamedTypes, Type2 actualType) {
    EngineTestCase.assertInstanceOf(FunctionType, actualType);
    FunctionType functionType = actualType as FunctionType;
    List<Type2> normalTypes = functionType.normalParameterTypes;
    if (expectedNormalTypes == null) {
      EngineTestCase.assertLength(0, normalTypes);
    } else {
      int expectedCount = expectedNormalTypes.length;
      EngineTestCase.assertLength(expectedCount, normalTypes);
      for (int i = 0; i < expectedCount; i++) {
        JUnitTestCase.assertSame(expectedNormalTypes[i], normalTypes[i]);
      }
    }
    List<Type2> optionalTypes = functionType.optionalParameterTypes;
    if (expectedOptionalTypes == null) {
      EngineTestCase.assertLength(0, optionalTypes);
    } else {
      int expectedCount = expectedOptionalTypes.length;
      EngineTestCase.assertLength(expectedCount, optionalTypes);
      for (int i = 0; i < expectedCount; i++) {
        JUnitTestCase.assertSame(expectedOptionalTypes[i], optionalTypes[i]);
      }
    }
    Map<String, Type2> namedTypes = functionType.namedParameterTypes;
    if (expectedNamedTypes == null) {
      EngineTestCase.assertSize2(0, namedTypes);
    } else {
      EngineTestCase.assertSize2(expectedNamedTypes.length, namedTypes);
      for (MapEntry<String, Type2> entry in getMapEntrySet(expectedNamedTypes)) {
        JUnitTestCase.assertSame(entry.getValue(), namedTypes[entry.getKey()]);
      }
    }
    JUnitTestCase.assertSame(expectedReturnType, functionType.returnType);
  }
  void assertType(InterfaceTypeImpl expectedType, InterfaceTypeImpl actualType) {
    JUnitTestCase.assertEquals(expectedType.displayName, actualType.displayName);
    JUnitTestCase.assertEquals(expectedType.element, actualType.element);
    List<Type2> expectedArguments = expectedType.typeArguments;
    int length = expectedArguments.length;
    List<Type2> actualArguments = actualType.typeArguments;
    EngineTestCase.assertLength(length, actualArguments);
    for (int i = 0; i < length; i++) {
      assertType2(expectedArguments[i], actualArguments[i]);
    }
  }
  void assertType2(Type2 expectedType, Type2 actualType) {
    if (expectedType is InterfaceTypeImpl) {
      EngineTestCase.assertInstanceOf(InterfaceTypeImpl, actualType);
      assertType(expectedType as InterfaceTypeImpl, actualType as InterfaceTypeImpl);
    }
  }

  /**
   * Create the analyzer used by the tests.
   *
   * @return the analyzer to be used by the tests
   */
  StaticTypeAnalyzer createAnalyzer() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory.con2([new DartUriResolver(DirectoryBasedDartSdk.defaultSdk)]);
    context.sourceFactory = sourceFactory;
    FileBasedSource source = new FileBasedSource.con1(sourceFactory.contentCache, FileUtilities2.createFile("/lib.dart"));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("lib.dart");
    definingCompilationUnit.source = source;
    LibraryElementImpl definingLibrary = new LibraryElementImpl(context, null);
    definingLibrary.definingCompilationUnit = definingCompilationUnit;
    Library library = new Library(context, _listener, source);
    library.libraryElement = definingLibrary;
    _visitor = new ResolverVisitor.con1(library, source, _typeProvider);
    _visitor.overrideManager.enterScope();
    try {
      return _visitor.typeAnalyzer_J2DAccessor as StaticTypeAnalyzer;
    } on JavaException catch (exception) {
      throw new IllegalArgumentException("Could not create analyzer", exception);
    }
  }

  /**
   * Return an integer literal that has been resolved to the correct type.
   *
   * @param value the value of the literal
   * @return an integer literal that has been resolved to the correct type
   */
  DoubleLiteral resolvedDouble(double value) {
    DoubleLiteral literal = ASTFactory.doubleLiteral(value);
    literal.staticType = _typeProvider.doubleType;
    return literal;
  }

  /**
   * Create a function expression that has an element associated with it, where the element has an
   * incomplete type associated with it (just like the one
   * [ElementBuilder#visitFunctionExpression] would have built if we had
   * run it).
   *
   * @param parameters the parameters to the function
   * @param body the body of the function
   * @return a resolved function expression
   */
  FunctionExpression resolvedFunctionExpression(FormalParameterList parameters, FunctionBody body) {
    List<ParameterElement> parameterElements = new List<ParameterElement>();
    for (FormalParameter parameter in parameters.parameters) {
      ParameterElementImpl element = new ParameterElementImpl.con1(parameter.identifier);
      element.parameterKind = parameter.kind;
      element.type = _typeProvider.dynamicType;
      parameter.identifier.staticElement = element;
      parameterElements.add(element);
    }
    FunctionExpression node = ASTFactory.functionExpression2(parameters, body);
    FunctionElementImpl element = new FunctionElementImpl.con1(null);
    element.parameters = new List.from(parameterElements);
    element.type = new FunctionTypeImpl.con1(element);
    node.element = element;
    return node;
  }

  /**
   * Return an integer literal that has been resolved to the correct type.
   *
   * @param value the value of the literal
   * @return an integer literal that has been resolved to the correct type
   */
  IntegerLiteral resolvedInteger(int value) {
    IntegerLiteral literal = ASTFactory.integer(value);
    literal.staticType = _typeProvider.intType;
    return literal;
  }

  /**
   * Return a string literal that has been resolved to the correct type.
   *
   * @param value the value of the literal
   * @return a string literal that has been resolved to the correct type
   */
  SimpleStringLiteral resolvedString(String value) {
    SimpleStringLiteral string = ASTFactory.string2(value);
    string.staticType = _typeProvider.stringType;
    return string;
  }

  /**
   * Return a simple identifier that has been resolved to a variable element with the given type.
   *
   * @param type the type of the variable being represented
   * @param variableName the name of the variable
   * @return a simple identifier that has been resolved to a variable element with the given type
   */
  SimpleIdentifier resolvedVariable(InterfaceType type, String variableName) {
    SimpleIdentifier identifier = ASTFactory.identifier3(variableName);
    VariableElementImpl element = ElementFactory.localVariableElement(identifier);
    element.type = type;
    identifier.staticElement = element;
    identifier.staticType = type;
    return identifier;
  }

  /**
   * Set the type of the given parameter to the given type.
   *
   * @param parameter the parameter whose type is to be set
   * @param type the new type of the given parameter
   */
  void setType(FormalParameter parameter, Type2 type) {
    SimpleIdentifier identifier = parameter.identifier;
    Element element = identifier.staticElement;
    if (element is! ParameterElement) {
      element = new ParameterElementImpl.con1(identifier);
      identifier.staticElement = element;
    }
    ((element as ParameterElementImpl)).type = type;
  }
  static dartSuite() {
    _ut.group('StaticTypeAnalyzerTest', () {
      _ut.test('test_visitAdjacentStrings', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitAdjacentStrings);
      });
      _ut.test('test_visitArgumentDefinitionTest', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitArgumentDefinitionTest);
      });
      _ut.test('test_visitAsExpression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitAsExpression);
      });
      _ut.test('test_visitAssignmentExpression_compound', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitAssignmentExpression_compound);
      });
      _ut.test('test_visitAssignmentExpression_simple', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitAssignmentExpression_simple);
      });
      _ut.test('test_visitBinaryExpression_equals', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_equals);
      });
      _ut.test('test_visitBinaryExpression_logicalAnd', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_logicalAnd);
      });
      _ut.test('test_visitBinaryExpression_logicalOr', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_logicalOr);
      });
      _ut.test('test_visitBinaryExpression_notEquals', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_notEquals);
      });
      _ut.test('test_visitBinaryExpression_plusID', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_plusID);
      });
      _ut.test('test_visitBinaryExpression_plusII', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_plusII);
      });
      _ut.test('test_visitBinaryExpression_slash', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_slash);
      });
      _ut.test('test_visitBinaryExpression_starID', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_starID);
      });
      _ut.test('test_visitBinaryExpression_star_notSpecial', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBinaryExpression_star_notSpecial);
      });
      _ut.test('test_visitBooleanLiteral_false', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBooleanLiteral_false);
      });
      _ut.test('test_visitBooleanLiteral_true', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitBooleanLiteral_true);
      });
      _ut.test('test_visitCascadeExpression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitCascadeExpression);
      });
      _ut.test('test_visitConditionalExpression_differentTypes', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitConditionalExpression_differentTypes);
      });
      _ut.test('test_visitConditionalExpression_sameTypes', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitConditionalExpression_sameTypes);
      });
      _ut.test('test_visitDoubleLiteral', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitDoubleLiteral);
      });
      _ut.test('test_visitFunctionExpression_named_block', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_named_block);
      });
      _ut.test('test_visitFunctionExpression_named_expression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_named_expression);
      });
      _ut.test('test_visitFunctionExpression_normalAndNamed_block', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_normalAndNamed_block);
      });
      _ut.test('test_visitFunctionExpression_normalAndNamed_expression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_normalAndNamed_expression);
      });
      _ut.test('test_visitFunctionExpression_normalAndPositional_block', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_normalAndPositional_block);
      });
      _ut.test('test_visitFunctionExpression_normalAndPositional_expression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_normalAndPositional_expression);
      });
      _ut.test('test_visitFunctionExpression_normal_block', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_normal_block);
      });
      _ut.test('test_visitFunctionExpression_normal_expression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_normal_expression);
      });
      _ut.test('test_visitFunctionExpression_positional_block', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_positional_block);
      });
      _ut.test('test_visitFunctionExpression_positional_expression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitFunctionExpression_positional_expression);
      });
      _ut.test('test_visitIndexExpression_getter', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitIndexExpression_getter);
      });
      _ut.test('test_visitIndexExpression_setter', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitIndexExpression_setter);
      });
      _ut.test('test_visitIndexExpression_typeParameters', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitIndexExpression_typeParameters);
      });
      _ut.test('test_visitIndexExpression_typeParameters_inSetterContext', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitIndexExpression_typeParameters_inSetterContext);
      });
      _ut.test('test_visitInstanceCreationExpression_named', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitInstanceCreationExpression_named);
      });
      _ut.test('test_visitInstanceCreationExpression_typeParameters', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitInstanceCreationExpression_typeParameters);
      });
      _ut.test('test_visitInstanceCreationExpression_unnamed', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitInstanceCreationExpression_unnamed);
      });
      _ut.test('test_visitIntegerLiteral', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitIntegerLiteral);
      });
      _ut.test('test_visitIsExpression_negated', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitIsExpression_negated);
      });
      _ut.test('test_visitIsExpression_notNegated', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitIsExpression_notNegated);
      });
      _ut.test('test_visitListLiteral_empty', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitListLiteral_empty);
      });
      _ut.test('test_visitListLiteral_nonEmpty', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitListLiteral_nonEmpty);
      });
      _ut.test('test_visitMapLiteral_empty', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitMapLiteral_empty);
      });
      _ut.test('test_visitMapLiteral_nonEmpty', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitMapLiteral_nonEmpty);
      });
      _ut.test('test_visitMethodInvocation_then', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitMethodInvocation_then);
      });
      _ut.test('test_visitNamedExpression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitNamedExpression);
      });
      _ut.test('test_visitNullLiteral', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitNullLiteral);
      });
      _ut.test('test_visitParenthesizedExpression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitParenthesizedExpression);
      });
      _ut.test('test_visitPostfixExpression_minusMinus', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPostfixExpression_minusMinus);
      });
      _ut.test('test_visitPostfixExpression_plusPlus', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPostfixExpression_plusPlus);
      });
      _ut.test('test_visitPrefixExpression_bang', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixExpression_bang);
      });
      _ut.test('test_visitPrefixExpression_minus', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixExpression_minus);
      });
      _ut.test('test_visitPrefixExpression_minusMinus', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixExpression_minusMinus);
      });
      _ut.test('test_visitPrefixExpression_not', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixExpression_not);
      });
      _ut.test('test_visitPrefixExpression_plusPlus', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixExpression_plusPlus);
      });
      _ut.test('test_visitPrefixExpression_tilde', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixExpression_tilde);
      });
      _ut.test('test_visitPrefixedIdentifier_getter', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixedIdentifier_getter);
      });
      _ut.test('test_visitPrefixedIdentifier_setter', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixedIdentifier_setter);
      });
      _ut.test('test_visitPrefixedIdentifier_variable', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPrefixedIdentifier_variable);
      });
      _ut.test('test_visitPropertyAccess_getter', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPropertyAccess_getter);
      });
      _ut.test('test_visitPropertyAccess_setter', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitPropertyAccess_setter);
      });
      _ut.test('test_visitSimpleStringLiteral', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitSimpleStringLiteral);
      });
      _ut.test('test_visitStringInterpolation', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitStringInterpolation);
      });
      _ut.test('test_visitSuperExpression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitSuperExpression);
      });
      _ut.test('test_visitSymbolLiteral', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitSymbolLiteral);
      });
      _ut.test('test_visitThisExpression', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitThisExpression);
      });
      _ut.test('test_visitThrowExpression_withValue', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitThrowExpression_withValue);
      });
      _ut.test('test_visitThrowExpression_withoutValue', () {
        final __test = new StaticTypeAnalyzerTest();
        runJUnitTest(__test, __test.test_visitThrowExpression_withoutValue);
      });
    });
  }
}
class NonHintCodeTest extends ResolverTestCase {
  void test_deadCode_deadBlock_conditionalElse_debugConst() {
    Source source = addSource(EngineTestCase.createSource([
        "const bool DEBUG = true;",
        "f() {",
        "  DEBUG ? 1 : 2;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadBlock_conditionalIf_debugConst() {
    Source source = addSource(EngineTestCase.createSource([
        "const bool DEBUG = false;",
        "f() {",
        "  DEBUG ? 1 : 2;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadBlock_else() {
    Source source = addSource(EngineTestCase.createSource([
        "const bool DEBUG = true;",
        "f() {",
        "  if(DEBUG) {} else {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadBlock_if_debugConst_prefixedIdentifier() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static const bool DEBUG = false;",
        "}",
        "f() {",
        "  if(A.DEBUG) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadBlock_if_debugConst_prefixedIdentifier2() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib2.dart';",
        "f() {",
        "  if(A.DEBUG) {}",
        "}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource([
        "library lib2;",
        "class A {",
        "  static const bool DEBUG = false;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadBlock_if_debugConst_propertyAccessor() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib2.dart' as LIB;",
        "f() {",
        "  if(LIB.A.DEBUG) {}",
        "}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource([
        "library lib2;",
        "class A {",
        "  static const bool DEBUG = false;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadBlock_if_debugConst_simpleIdentifier() {
    Source source = addSource(EngineTestCase.createSource([
        "const bool DEBUG = false;",
        "f() {",
        "  if(DEBUG) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadBlock_while_debugConst() {
    Source source = addSource(EngineTestCase.createSource([
        "const bool DEBUG = false;",
        "f() {",
        "  while(DEBUG) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadCatch_onCatchSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {}",
        "f() {",
        "  try {} on B catch (e) {} on A catch (e) {} catch (e) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadOperandLHS_and_debugConst() {
    Source source = addSource(EngineTestCase.createSource([
        "const bool DEBUG = false;",
        "f() {",
        "  bool b = DEBUG && false;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_deadCode_deadOperandLHS_or_debugConst() {
    Source source = addSource(EngineTestCase.createSource([
        "const bool DEBUG = true;",
        "f() {",
        "  bool b = DEBUG || true;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_divisionOptimization() {
    Source source = addSource(EngineTestCase.createSource(["f(int x, int y) {", "  var v = x / y.toInt();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_divisionOptimization_supressIfDivisionNotDefinedInCore() {
    Source source = addSource(EngineTestCase.createSource(["f(x, y) {", "  var v = (x / y).toInt();", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_divisionOptimization_supressIfDivisionOverridden() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  num operator /(x) {}",
        "}",
        "f(A x, A y) {",
        "  var v = (x / y).toInt();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_duplicateImport_as() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart' as one;",
        "A a;",
        "one.A a2;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_duplicateImport_hide() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart' hide A;",
        "A a;",
        "B b;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}", "class B {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_duplicateImport_show() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart' show A;",
        "A a;",
        "B b;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {}", "class B {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_overriddingPrivateMember_sameLibrary() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  _m(int x) => 0;",
        "}",
        "class B extends A {",
        "  _m(int x) => 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_overrideEqualsButNotHashCode() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  bool operator ==(x) {}",
        "  get hashCode => 0;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_proxy_annotation_prefixed() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'meta.dart';",
        "@proxy",
        "class A {}",
        "f(var a) {",
        "  a = new A();",
        "  a.m();",
        "  var x = a.g;",
        "  a.s = 1;",
        "  var y = a + a;",
        "  a++;",
        "  ++a;",
        "}"]));
    addSource2("/meta.dart", EngineTestCase.createSource([
        "library meta;",
        "const proxy = const _Proxy();",
        "class _Proxy { const _Proxy(); }"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_proxy_annotation_prefixed2() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'meta.dart';",
        "@proxy",
        "class A {}",
        "class B {",
        "  f(var a) {",
        "    a = new A();",
        "    a.m();",
        "    var x = a.g;",
        "    a.s = 1;",
        "    var y = a + a;",
        "    a++;",
        "    ++a;",
        "  }",
        "}"]));
    addSource2("/meta.dart", EngineTestCase.createSource([
        "library meta;",
        "const proxy = const _Proxy();",
        "class _Proxy { const _Proxy(); }"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_proxy_annotation_prefixed3() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'meta.dart';",
        "class B {",
        "  f(var a) {",
        "    a = new A();",
        "    a.m();",
        "    var x = a.g;",
        "    a.s = 1;",
        "    var y = a + a;",
        "    a++;",
        "    ++a;",
        "  }",
        "}",
        "@proxy",
        "class A {}"]));
    addSource2("/meta.dart", EngineTestCase.createSource([
        "library meta;",
        "const proxy = const _Proxy();",
        "class _Proxy { const _Proxy(); }"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedGetter_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  get b => 0;",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    return a.b;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedMethod_assignmentExpression_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  operator +(B b) {return new B();}",
        "}",
        "f(var a, var a2) {",
        "  a = new A();",
        "  a2 = new A();",
        "  a += a2;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedMethod_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  b() {}",
        "}",
        "f() {",
        "  var a = new A();",
        "  a.b();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedOperator_binaryExpression_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  operator +(B b) {}",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    a + 1;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedOperator_indexBoth_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  operator [](int index) {}",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0]++;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedOperator_indexGetter_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  operator [](int index) {}",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0];",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedOperator_indexSetter_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  operator []=(i, v) {}",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0] = 1;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedOperator_postfixExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  operator +(B b) {return new B();}",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    a++;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedOperator_prefixExpression() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  operator +(B b) {return new B();}",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    ++a;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_undefinedSetter_inSubtype() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {}",
        "class B extends A {",
        "  set b(x) {}",
        "}",
        "f(var a) {",
        "  if(a is A) {",
        "    a.b = 0;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_unnecessaryCast_13855_parameter_A() {
    Source source = addSource(EngineTestCase.createSource([
        "class A{",
        "  a() {}",
        "}",
        "class B<E> {",
        "  E e;",
        "  m() {",
        "    (e as A).a();",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unnecessaryCast_dynamic_type() {
    Source source = addSource(EngineTestCase.createSource(["m(v) {", "  var b = v as Object;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unnecessaryCast_type_dynamic() {
    Source source = addSource(EngineTestCase.createSource(["m(v) {", "  var b = Object as dynamic;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unusedImport_annotationOnDirective() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "@A()", "import 'lib1.dart';"]));
    Source source2 = addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class A {", "  const A() {}", "}"]));
    resolve(source);
    assertErrors(source, []);
    verify([source, source2]);
  }
  void test_unusedImport_core_library() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "import 'dart:core';"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unusedImport_export() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "import 'lib1.dart';", "Two two;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "export 'lib2.dart';", "class One {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "class Two {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unusedImport_export_infiniteLoop() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "import 'lib1.dart';", "Two two;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "export 'lib2.dart';", "class One {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "export 'lib3.dart';", "class Two {}"]));
    addSource2("/lib3.dart", EngineTestCase.createSource(["library lib3;", "export 'lib2.dart';", "class Three {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unusedImport_export2() {
    Source source = addSource(EngineTestCase.createSource(["library L;", "import 'lib1.dart';", "Three three;"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "export 'lib2.dart';", "class One {}"]));
    addSource2("/lib2.dart", EngineTestCase.createSource(["library lib2;", "export 'lib3.dart';", "class Two {}"]));
    addSource2("/lib3.dart", EngineTestCase.createSource(["library lib3;", "class Three {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_unusedImport_prefix_topLevelFunction() {
    Source source = addSource(EngineTestCase.createSource([
        "library L;",
        "import 'lib1.dart' hide topLevelFunction;",
        "import 'lib1.dart' as one show topLevelFunction;",
        "class A {",
        "  static void x() {",
        "    One o;",
        "    one.topLevelFunction();",
        "  }",
        "}"]));
    addSource2("/lib1.dart", EngineTestCase.createSource(["library lib1;", "class One {}", "topLevelFunction() {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  static dartSuite() {
    _ut.group('NonHintCodeTest', () {
      _ut.test('test_deadCode_deadBlock_conditionalElse_debugConst', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_conditionalElse_debugConst);
      });
      _ut.test('test_deadCode_deadBlock_conditionalIf_debugConst', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_conditionalIf_debugConst);
      });
      _ut.test('test_deadCode_deadBlock_else', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_else);
      });
      _ut.test('test_deadCode_deadBlock_if_debugConst_prefixedIdentifier', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_if_debugConst_prefixedIdentifier);
      });
      _ut.test('test_deadCode_deadBlock_if_debugConst_prefixedIdentifier2', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_if_debugConst_prefixedIdentifier2);
      });
      _ut.test('test_deadCode_deadBlock_if_debugConst_propertyAccessor', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_if_debugConst_propertyAccessor);
      });
      _ut.test('test_deadCode_deadBlock_if_debugConst_simpleIdentifier', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_if_debugConst_simpleIdentifier);
      });
      _ut.test('test_deadCode_deadBlock_while_debugConst', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadBlock_while_debugConst);
      });
      _ut.test('test_deadCode_deadCatch_onCatchSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadCatch_onCatchSubtype);
      });
      _ut.test('test_deadCode_deadOperandLHS_and_debugConst', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadOperandLHS_and_debugConst);
      });
      _ut.test('test_deadCode_deadOperandLHS_or_debugConst', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_deadCode_deadOperandLHS_or_debugConst);
      });
      _ut.test('test_divisionOptimization', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_divisionOptimization);
      });
      _ut.test('test_divisionOptimization_supressIfDivisionNotDefinedInCore', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_divisionOptimization_supressIfDivisionNotDefinedInCore);
      });
      _ut.test('test_divisionOptimization_supressIfDivisionOverridden', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_divisionOptimization_supressIfDivisionOverridden);
      });
      _ut.test('test_duplicateImport_as', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_duplicateImport_as);
      });
      _ut.test('test_duplicateImport_hide', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_duplicateImport_hide);
      });
      _ut.test('test_duplicateImport_show', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_duplicateImport_show);
      });
      _ut.test('test_overriddingPrivateMember_sameLibrary', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_overriddingPrivateMember_sameLibrary);
      });
      _ut.test('test_overrideEqualsButNotHashCode', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_overrideEqualsButNotHashCode);
      });
      _ut.test('test_proxy_annotation_prefixed', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_proxy_annotation_prefixed);
      });
      _ut.test('test_proxy_annotation_prefixed2', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_proxy_annotation_prefixed2);
      });
      _ut.test('test_proxy_annotation_prefixed3', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_proxy_annotation_prefixed3);
      });
      _ut.test('test_undefinedGetter_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedGetter_inSubtype);
      });
      _ut.test('test_undefinedMethod_assignmentExpression_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod_assignmentExpression_inSubtype);
      });
      _ut.test('test_undefinedMethod_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedMethod_inSubtype);
      });
      _ut.test('test_undefinedOperator_binaryExpression_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_binaryExpression_inSubtype);
      });
      _ut.test('test_undefinedOperator_indexBoth_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexBoth_inSubtype);
      });
      _ut.test('test_undefinedOperator_indexGetter_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexGetter_inSubtype);
      });
      _ut.test('test_undefinedOperator_indexSetter_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_indexSetter_inSubtype);
      });
      _ut.test('test_undefinedOperator_postfixExpression', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_postfixExpression);
      });
      _ut.test('test_undefinedOperator_prefixExpression', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedOperator_prefixExpression);
      });
      _ut.test('test_undefinedSetter_inSubtype', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_undefinedSetter_inSubtype);
      });
      _ut.test('test_unnecessaryCast_13855_parameter_A', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryCast_13855_parameter_A);
      });
      _ut.test('test_unnecessaryCast_dynamic_type', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryCast_dynamic_type);
      });
      _ut.test('test_unnecessaryCast_type_dynamic', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unnecessaryCast_type_dynamic);
      });
      _ut.test('test_unusedImport_annotationOnDirective', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_annotationOnDirective);
      });
      _ut.test('test_unusedImport_core_library', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_core_library);
      });
      _ut.test('test_unusedImport_export', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_export);
      });
      _ut.test('test_unusedImport_export2', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_export2);
      });
      _ut.test('test_unusedImport_export_infiniteLoop', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_export_infiniteLoop);
      });
      _ut.test('test_unusedImport_prefix_topLevelFunction', () {
        final __test = new NonHintCodeTest();
        runJUnitTest(__test, __test.test_unusedImport_prefix_topLevelFunction);
      });
    });
  }
}
class EnclosedScopeTest extends ResolverTestCase {
  void test_define_duplicate() {
    GatheringErrorListener errorListener2 = new GatheringErrorListener();
    Scope rootScope = new Scope_21(errorListener2);
    EnclosedScope scope = new EnclosedScope(rootScope);
    VariableElement element1 = ElementFactory.localVariableElement(ASTFactory.identifier3("v1"));
    VariableElement element2 = ElementFactory.localVariableElement(ASTFactory.identifier3("v1"));
    scope.define(element1);
    scope.define(element2);
    errorListener2.assertErrors3([ErrorSeverity.ERROR]);
  }
  void test_define_normal() {
    GatheringErrorListener errorListener3 = new GatheringErrorListener();
    Scope rootScope = new Scope_22(errorListener3);
    EnclosedScope outerScope = new EnclosedScope(rootScope);
    EnclosedScope innerScope = new EnclosedScope(outerScope);
    VariableElement element1 = ElementFactory.localVariableElement(ASTFactory.identifier3("v1"));
    VariableElement element2 = ElementFactory.localVariableElement(ASTFactory.identifier3("v2"));
    outerScope.define(element1);
    innerScope.define(element2);
    errorListener3.assertNoErrors();
  }
  static dartSuite() {
    _ut.group('EnclosedScopeTest', () {
      _ut.test('test_define_duplicate', () {
        final __test = new EnclosedScopeTest();
        runJUnitTest(__test, __test.test_define_duplicate);
      });
      _ut.test('test_define_normal', () {
        final __test = new EnclosedScopeTest();
        runJUnitTest(__test, __test.test_define_normal);
      });
    });
  }
}
class Scope_21 extends Scope {
  GatheringErrorListener errorListener2;
  Scope_21(this.errorListener2) : super();
  AnalysisErrorListener get errorListener => errorListener2;
  Element lookup3(Identifier identifier, String name, LibraryElement referencingLibrary) => null;
}
class Scope_22 extends Scope {
  GatheringErrorListener errorListener3;
  Scope_22(this.errorListener3) : super();
  AnalysisErrorListener get errorListener => errorListener3;
  Element lookup3(Identifier identifier, String name, LibraryElement referencingLibrary) => null;
}
class LibraryElementBuilderTest extends EngineTestCase {

  /**
   * The source factory used to create [Source].
   */
  SourceFactory _sourceFactory;
  void setUp() {
    _sourceFactory = new SourceFactory.con2([new FileUriResolver()]);
  }
  void test_accessorsAcrossFiles() {
    Source librarySource = addSource("/lib.dart", EngineTestCase.createSource([
        "library lib;",
        "part 'first.dart';",
        "part 'second.dart';"]));
    addSource("/first.dart", EngineTestCase.createSource(["part of lib;", "int get V => 0;"]));
    addSource("/second.dart", EngineTestCase.createSource(["part of lib;", "void set V(int v) {}"]));
    LibraryElement element = buildLibrary(librarySource, []);
    JUnitTestCase.assertNotNull(element);
    List<CompilationUnitElement> sourcedUnits = element.parts;
    EngineTestCase.assertLength(2, sourcedUnits);
    List<PropertyAccessorElement> firstAccessors = sourcedUnits[0].accessors;
    EngineTestCase.assertLength(1, firstAccessors);
    List<PropertyAccessorElement> secondAccessors = sourcedUnits[1].accessors;
    EngineTestCase.assertLength(1, secondAccessors);
    JUnitTestCase.assertSame(firstAccessors[0].variable, secondAccessors[0].variable);
  }
  void test_empty() {
    Source librarySource = addSource("/lib.dart", "library lib;");
    LibraryElement element = buildLibrary(librarySource, []);
    JUnitTestCase.assertNotNull(element);
    JUnitTestCase.assertEquals("lib", element.name);
    JUnitTestCase.assertNull(element.entryPoint);
    EngineTestCase.assertLength(0, element.importedLibraries);
    EngineTestCase.assertLength(0, element.imports);
    JUnitTestCase.assertSame(element, element.library);
    EngineTestCase.assertLength(0, element.prefixes);
    EngineTestCase.assertLength(0, element.parts);
    CompilationUnitElement unit = element.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    JUnitTestCase.assertEquals("lib.dart", unit.name);
    JUnitTestCase.assertEquals(element, unit.library);
    EngineTestCase.assertLength(0, unit.accessors);
    EngineTestCase.assertLength(0, unit.functions);
    EngineTestCase.assertLength(0, unit.functionTypeAliases);
    EngineTestCase.assertLength(0, unit.types);
    EngineTestCase.assertLength(0, unit.topLevelVariables);
  }
  void test_invalidUri_part() {
    Source librarySource = addSource("/lib.dart", EngineTestCase.createSource(["library lib;", "", "part '\${'a'}.dart';"]));
    LibraryElement element = buildLibrary(librarySource, [CompileTimeErrorCode.URI_WITH_INTERPOLATION]);
    JUnitTestCase.assertNotNull(element);
  }
  void test_missingLibraryDirectiveWithPart() {
    addSource("/a.dart", EngineTestCase.createSource(["part of lib;"]));
    Source librarySource = addSource("/lib.dart", EngineTestCase.createSource(["part 'a.dart';"]));
    LibraryElement element = buildLibrary(librarySource, [ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART]);
    JUnitTestCase.assertNotNull(element);
  }
  void test_missingPartOfDirective() {
    addSource("/a.dart", "class A {}");
    Source librarySource = addSource("/lib.dart", EngineTestCase.createSource(["library lib;", "", "part 'a.dart';"]));
    LibraryElement element = buildLibrary(librarySource, [CompileTimeErrorCode.PART_OF_NON_PART]);
    JUnitTestCase.assertNotNull(element);
  }
  void test_multipleFiles() {
    Source librarySource = addSource("/lib.dart", EngineTestCase.createSource([
        "library lib;",
        "part 'first.dart';",
        "part 'second.dart';",
        "",
        "class A {}"]));
    addSource("/first.dart", EngineTestCase.createSource(["part of lib;", "class B {}"]));
    addSource("/second.dart", EngineTestCase.createSource(["part of lib;", "class C {}"]));
    LibraryElement element = buildLibrary(librarySource, []);
    JUnitTestCase.assertNotNull(element);
    List<CompilationUnitElement> sourcedUnits = element.parts;
    EngineTestCase.assertLength(2, sourcedUnits);
    assertTypes(element.definingCompilationUnit, ["A"]);
    if (sourcedUnits[0].name == "first.dart") {
      assertTypes(sourcedUnits[0], ["B"]);
      assertTypes(sourcedUnits[1], ["C"]);
    } else {
      assertTypes(sourcedUnits[0], ["C"]);
      assertTypes(sourcedUnits[1], ["B"]);
    }
  }
  void test_singleFile() {
    Source librarySource = addSource("/lib.dart", EngineTestCase.createSource(["library lib;", "", "class A {}"]));
    LibraryElement element = buildLibrary(librarySource, []);
    JUnitTestCase.assertNotNull(element);
    assertTypes(element.definingCompilationUnit, ["A"]);
  }

  /**
   * Add a source file to the content provider. The file path should be absolute.
   *
   * @param filePath the path of the file being added
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  Source addSource(String filePath, String contents) {
    Source source = new FileBasedSource.con1(_sourceFactory.contentCache, FileUtilities2.createFile(filePath));
    _sourceFactory.setContents(source, contents);
    return source;
  }

  /**
   * Ensure that there are elements representing all of the types in the given array of type names.
   *
   * @param unit the compilation unit containing the types
   * @param typeNames the names of the types that should be found
   */
  void assertTypes(CompilationUnitElement unit, List<String> typeNames) {
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> types = unit.types;
    EngineTestCase.assertLength(typeNames.length, types);
    for (ClassElement type in types) {
      JUnitTestCase.assertNotNull(type);
      String actualTypeName = type.displayName;
      bool wasExpected = false;
      for (String expectedTypeName in typeNames) {
        if (expectedTypeName == actualTypeName) {
          wasExpected = true;
        }
      }
      if (!wasExpected) {
        JUnitTestCase.fail("Found unexpected type ${actualTypeName}");
      }
    }
  }

  /**
   * Build the element model for the library whose defining compilation unit has the given source.
   *
   * @param librarySource the source of the defining compilation unit for the library
   * @param expectedErrorCodes the errors that are expected to be found while building the element
   *          model
   * @return the element model that was built for the library
   * @throws Exception if the element model could not be built
   */
  LibraryElement buildLibrary(Source librarySource, List<ErrorCode> expectedErrorCodes) {
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.sourceFactory = new SourceFactory.con2([
        new DartUriResolver(DirectoryBasedDartSdk.defaultSdk),
        new FileUriResolver()]);
    LibraryResolver resolver = new LibraryResolver(context);
    LibraryElementBuilder builder = new LibraryElementBuilder(resolver);
    Library library = resolver.createLibrary(librarySource) as Library;
    LibraryElement element = builder.buildLibrary(library);
    GatheringErrorListener listener = new GatheringErrorListener();
    listener.addAll(resolver.errorListener);
    listener.assertErrors2(expectedErrorCodes);
    return element;
  }
  static dartSuite() {
    _ut.group('LibraryElementBuilderTest', () {
      _ut.test('test_accessorsAcrossFiles', () {
        final __test = new LibraryElementBuilderTest();
        runJUnitTest(__test, __test.test_accessorsAcrossFiles);
      });
      _ut.test('test_empty', () {
        final __test = new LibraryElementBuilderTest();
        runJUnitTest(__test, __test.test_empty);
      });
      _ut.test('test_invalidUri_part', () {
        final __test = new LibraryElementBuilderTest();
        runJUnitTest(__test, __test.test_invalidUri_part);
      });
      _ut.test('test_missingLibraryDirectiveWithPart', () {
        final __test = new LibraryElementBuilderTest();
        runJUnitTest(__test, __test.test_missingLibraryDirectiveWithPart);
      });
      _ut.test('test_missingPartOfDirective', () {
        final __test = new LibraryElementBuilderTest();
        runJUnitTest(__test, __test.test_missingPartOfDirective);
      });
      _ut.test('test_multipleFiles', () {
        final __test = new LibraryElementBuilderTest();
        runJUnitTest(__test, __test.test_multipleFiles);
      });
      _ut.test('test_singleFile', () {
        final __test = new LibraryElementBuilderTest();
        runJUnitTest(__test, __test.test_singleFile);
      });
    });
  }
}
class ScopeTest extends ResolverTestCase {
  void test_define_duplicate() {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ScopeTest_TestScope scope = new ScopeTest_TestScope(errorListener);
    VariableElement element1 = ElementFactory.localVariableElement(ASTFactory.identifier3("v1"));
    VariableElement element2 = ElementFactory.localVariableElement(ASTFactory.identifier3("v1"));
    scope.define(element1);
    scope.define(element2);
    errorListener.assertErrors3([ErrorSeverity.ERROR]);
  }
  void test_define_normal() {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ScopeTest_TestScope scope = new ScopeTest_TestScope(errorListener);
    VariableElement element1 = ElementFactory.localVariableElement(ASTFactory.identifier3("v1"));
    VariableElement element2 = ElementFactory.localVariableElement(ASTFactory.identifier3("v2"));
    scope.define(element1);
    scope.define(element2);
    errorListener.assertNoErrors();
  }
  void test_getErrorListener() {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ScopeTest_TestScope scope = new ScopeTest_TestScope(errorListener);
    JUnitTestCase.assertEquals(errorListener, scope.errorListener);
  }
  void test_isPrivateName_nonPrivate() {
    JUnitTestCase.assertFalse(Scope.isPrivateName("Public"));
  }
  void test_isPrivateName_private() {
    JUnitTestCase.assertTrue(Scope.isPrivateName("_Private"));
  }
  static dartSuite() {
    _ut.group('ScopeTest', () {
      _ut.test('test_define_duplicate', () {
        final __test = new ScopeTest();
        runJUnitTest(__test, __test.test_define_duplicate);
      });
      _ut.test('test_define_normal', () {
        final __test = new ScopeTest();
        runJUnitTest(__test, __test.test_define_normal);
      });
      _ut.test('test_getErrorListener', () {
        final __test = new ScopeTest();
        runJUnitTest(__test, __test.test_getErrorListener);
      });
      _ut.test('test_isPrivateName_nonPrivate', () {
        final __test = new ScopeTest();
        runJUnitTest(__test, __test.test_isPrivateName_nonPrivate);
      });
      _ut.test('test_isPrivateName_private', () {
        final __test = new ScopeTest();
        runJUnitTest(__test, __test.test_isPrivateName_private);
      });
    });
  }
}
/**
 * A non-abstract subclass that can be used for testing purposes.
 */
class ScopeTest_TestScope extends Scope {

  /**
   * The listener that is to be informed when an error is encountered.
   */
  AnalysisErrorListener _errorListener;
  ScopeTest_TestScope(AnalysisErrorListener errorListener) {
    this._errorListener = errorListener;
  }
  AnalysisErrorListener get errorListener => _errorListener;
  Element lookup3(Identifier identifier, String name, LibraryElement referencingLibrary) => localLookup(name, referencingLibrary);
}
class SimpleResolverTest extends ResolverTestCase {
  void fail_staticInvocation() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  static int get g => (a,b) => 0;",
        "}",
        "class B {",
        "  f() {",
        "    A.g(1,0);",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_argumentResolution_required_matching() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, 3);",
        "  }",
        "  void g(a, b, c) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, 2]);
  }
  void test_argumentResolution_required_tooFew() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2);",
        "  }",
        "  void g(a, b, c) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1]);
  }
  void test_argumentResolution_required_tooMany() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, 3);",
        "  }",
        "  void g(a, b) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, -1]);
  }
  void test_argumentResolution_requiredAndNamed_extra() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, c: 3, d: 4);",
        "  }",
        "  void g(a, b, {c}) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, 2, -1]);
  }
  void test_argumentResolution_requiredAndNamed_matching() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, c: 3);",
        "  }",
        "  void g(a, b, {c}) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, 2]);
  }
  void test_argumentResolution_requiredAndNamed_missing() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, d: 3);",
        "  }",
        "  void g(a, b, {c, d}) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, 3]);
  }
  void test_argumentResolution_requiredAndPositional_fewer() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, 3);",
        "  }",
        "  void g(a, b, [c, d]) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, 2]);
  }
  void test_argumentResolution_requiredAndPositional_matching() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, 3, 4);",
        "  }",
        "  void g(a, b, [c, d]) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, 2, 3]);
  }
  void test_argumentResolution_requiredAndPositional_more() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void f() {",
        "    g(1, 2, 3, 4);",
        "  }",
        "  void g(a, b, [c]) {}",
        "}"]));
    validateArgumentResolution(source, [0, 1, 2, -1]);
  }
  void test_class_definesCall() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int call(int x) { return x; }",
        "}",
        "int f(A a) {",
        "  return a(0);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_class_extends_implements() {
    Source source = addSource(EngineTestCase.createSource([
        "class A extends B implements C {}",
        "class B {}",
        "class C {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_commentReference_class() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {}",
        "/** [A] [new A] [A.n] [new A.n] [m] [f] */",
        "class A {",
        "  A() {}",
        "  A.n() {}",
        "  m() {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_commentReference_parameter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  A() {}",
        "  A.n() {}",
        "  /** [e] [f] */",
        "  m(e, f()) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_commentReference_singleLine() {
    Source source = addSource(EngineTestCase.createSource(["/// [A]", "class A {}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_empty() {
    Source source = addSource("");
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_extractedMethodAsConstant() {
    Source source = addSource(EngineTestCase.createSource([
        "abstract class Comparable<T> {",
        "  int compareTo(T other);",
        "  static int compare(Comparable a, Comparable b) => a.compareTo(b);",
        "}",
        "class A {",
        "  void sort([compare = Comparable.compare]) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_fieldFormalParameter() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  int x;", "  A(this.x) {}", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_forEachLoops_nonConflicting() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  List list = [1,2,3];",
        "  for (int x in list) {}",
        "  for (int x in list) {}",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_forLoops_nonConflicting() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  for (int i = 0; i < 3; i++) {",
        "  }",
        "  for (int i = 0; i < 3; i++) {",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_functionTypeAlias() {
    Source source = addSource(EngineTestCase.createSource([
        "typedef bool P(e);",
        "class A {",
        "  P p;",
        "  m(e) {",
        "    if (p(e)) {}",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_getterAndSetterWithDifferentTypes() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get f => 0;",
        "  void set f(String s) {}",
        "}",
        "g (A a) {",
        "  a.f = a.f.toString();",
        "}"]));
    resolve(source);
    assertErrors(source, [StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES]);
    verify([source]);
  }
  void test_hasReferenceToSuper() {
    Source source = addSource(EngineTestCase.createSource(["class A {}", "class B {toString() => super.toString();}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(2, classes);
    JUnitTestCase.assertFalse(classes[0].hasReferenceToSuper());
    JUnitTestCase.assertTrue(classes[1].hasReferenceToSuper());
    assertNoErrors(source);
    verify([source]);
  }
  void test_import_hide() {
    addSource2("lib1.dart", EngineTestCase.createSource(["library lib1;", "set foo(value) {}", "class A {}"]));
    addSource2("lib2.dart", EngineTestCase.createSource(["library lib2;", "set foo(value) {}"]));
    Source source = addSource2("lib3.dart", EngineTestCase.createSource([
        "import 'lib1.dart' hide foo;",
        "import 'lib2.dart';",
        "",
        "main() {",
        "  foo = 0;",
        "}",
        "A a;"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_import_prefix() {
    addSource2("/two.dart", EngineTestCase.createSource(["library two;", "f(int x) {", "  return x * x;", "}"]));
    Source source = addSource2("/one.dart", EngineTestCase.createSource([
        "library one;",
        "import 'two.dart' as _two;",
        "main() {",
        "  _two.f(0);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_import_spaceInUri() {
    addSource2("sub folder/lib.dart", EngineTestCase.createSource(["library lib;", "foo() {}"]));
    Source source = addSource2("app.dart", EngineTestCase.createSource([
        "import 'sub folder/lib.dart';",
        "",
        "main() {",
        "  foo();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_indexExpression_typeParameters() {
    Source source = addSource(EngineTestCase.createSource([
        "f() {",
        "  List<int> a;",
        "  a[0];",
        "  List<List<int>> b;",
        "  b[0][0];",
        "  List<List<List<int>>> c;",
        "  c[0][0][0];",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_indexExpression_typeParameters_invalidAssignmentWarning() {
    Source source = addSource(EngineTestCase.createSource(["f() {", "  List<List<int>> b;", "  b[0][0] = 'hi';", "}"]));
    resolve(source);
    assertErrors(source, [StaticTypeWarningCode.INVALID_ASSIGNMENT]);
    verify([source]);
  }
  void test_indirectOperatorThroughCall() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  B call() { return new B(); }",
        "}",
        "",
        "class B {",
        "  int operator [](int i) { return i; }",
        "}",
        "",
        "A f = new A();",
        "",
        "g(int x) {}",
        "",
        "main() {",
        "  g(f()[0]);",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_invoke_dynamicThroughGetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  List get X => [() => 0];",
        "  m(A a) {",
        "    X.last;",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_isValidMixin_badSuperclass() {
    Source source = addSource(EngineTestCase.createSource(["class A extends B {}", "class B {}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(2, classes);
    JUnitTestCase.assertFalse(classes[0].isValidMixin);
    assertNoErrors(source);
    verify([source]);
  }
  void test_isValidMixin_constructor() {
    Source source = addSource(EngineTestCase.createSource(["class A {", "  A() {}", "}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(1, classes);
    JUnitTestCase.assertFalse(classes[0].isValidMixin);
    assertNoErrors(source);
    verify([source]);
  }
  void test_isValidMixin_super() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  toString() {",
        "    return super.toString();",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(1, classes);
    JUnitTestCase.assertFalse(classes[0].isValidMixin);
    assertNoErrors(source);
    verify([source]);
  }
  void test_isValidMixin_valid() {
    Source source = addSource(EngineTestCase.createSource(["class A {}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(1, classes);
    JUnitTestCase.assertTrue(classes[0].isValidMixin);
    assertNoErrors(source);
    verify([source]);
  }
  void test_labels_switch() {
    Source source = addSource(EngineTestCase.createSource([
        "void doSwitch(int target) {",
        "  switch (target) {",
        "    l0: case 0:",
        "      continue l1;",
        "    l1: case 1:",
        "      continue l0;",
        "    default:",
        "      continue l1;",
        "  }",
        "}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    assertNoErrors(source);
    verify([source]);
  }
  void test_metadata_class() {
    Source source = addSource(EngineTestCase.createSource(["const A = null;", "@A class C {}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(1, classes);
    List<ElementAnnotation> annotations = classes[0].metadata;
    EngineTestCase.assertLength(1, annotations);
    assertNoErrors(source);
    verify([source]);
  }
  void test_metadata_field() {
    Source source = addSource(EngineTestCase.createSource(["const A = null;", "class C {", "  @A int f;", "}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(1, classes);
    FieldElement field = classes[0].fields[0];
    List<ElementAnnotation> annotations = field.metadata;
    EngineTestCase.assertLength(1, annotations);
    assertNoErrors(source);
    verify([source]);
  }
  void test_metadata_libraryDirective() {
    Source source = addSource(EngineTestCase.createSource(["@A library lib;", "const A = null;"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    List<ElementAnnotation> annotations = library.metadata;
    EngineTestCase.assertLength(1, annotations);
    assertNoErrors(source);
    verify([source]);
  }
  void test_metadata_method() {
    Source source = addSource(EngineTestCase.createSource(["const A = null;", "class C {", "  @A void m() {}", "}"]));
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    CompilationUnitElement unit = library.definingCompilationUnit;
    JUnitTestCase.assertNotNull(unit);
    List<ClassElement> classes = unit.types;
    EngineTestCase.assertLength(1, classes);
    MethodElement method = classes[0].methods[0];
    List<ElementAnnotation> annotations = method.metadata;
    EngineTestCase.assertLength(1, annotations);
    assertNoErrors(source);
    verify([source]);
  }
  void test_method_fromMixin() {
    Source source = addSource(EngineTestCase.createSource([
        "class B {",
        "  bar() => 1;",
        "}",
        "class A {",
        "  foo() => 2;",
        "}",
        "",
        "class C extends B with A {",
        "  bar() => super.bar();",
        "  foo() => super.foo();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_method_fromSuperclassMixin() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m1() {}",
        "}",
        "class B extends Object with A {",
        "}",
        "class C extends B {",
        "}",
        "f(C c) {",
        "  c.m1();",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_methodCascades() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  void m1() {}",
        "  void m2() {}",
        "  void m() {",
        "    A a = new A();",
        "    a..m1()",
        "     ..m2();",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_methodCascades_withSetter() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  String name;",
        "  void m1() {}",
        "  void m2() {}",
        "  void m() {",
        "    A a = new A();",
        "    a..m1()",
        "     ..name = 'name'",
        "     ..m2();",
        "  }",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_resolveAgainstNull() {
    Source source = addSource(EngineTestCase.createSource(["f(var p) {", "  return null == p;", "}"]));
    resolve(source);
    assertNoErrors(source);
  }
  void test_setter_inherited() {
    Source source = addSource(EngineTestCase.createSource([
        "class A {",
        "  int get x => 0;",
        "  set x(int p) {}",
        "}",
        "class B extends A {",
        "  int get x => super.x == null ? 0 : super.x;",
        "  int f() => x = 1;",
        "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }
  void test_setter_static() {
    Source source = addSource(EngineTestCase.createSource(["set s(x) {", "}", "", "main() {", "  s = 123;", "}"]));
    resolve(source);
    assertNoErrors(source);
    verify([source]);
  }

  /**
   * Resolve the given source and verify that the arguments in a specific method invocation were
   * correctly resolved.
   *
   * The source is expected to be source for a compilation unit, the first declaration is expected
   * to be a class, the first member of which is expected to be a method with a block body, and the
   * first statement in the body is expected to be an expression statement whose expression is a
   * method invocation. It is the arguments to that method invocation that are tested. The method
   * invocation can contain errors.
   *
   * The arguments were resolved correctly if the number of expressions in the list matches the
   * length of the array of indices and if, for each index in the array of indices, the parameter to
   * which the argument expression was resolved is the parameter in the invoked method's list of
   * parameters at that index. Arguments that should not be resolved to a parameter because of an
   * error can be denoted by including a negative index in the array of indices.
   *
   * @param source the source to be resolved
   * @param indices the array of indices used to associate arguments with parameters
   * @throws Exception if the source could not be resolved or if the structure of the source is not
   *           valid
   */
  void validateArgumentResolution(Source source, List<int> indices) {
    LibraryElement library = resolve(source);
    JUnitTestCase.assertNotNull(library);
    ClassElement classElement = library.definingCompilationUnit.types[0];
    List<ParameterElement> parameters = classElement.methods[1].parameters;
    CompilationUnit unit = resolveCompilationUnit(source, library);
    JUnitTestCase.assertNotNull(unit);
    ClassDeclaration classDeclaration = unit.declarations[0] as ClassDeclaration;
    MethodDeclaration methodDeclaration = classDeclaration.members[0] as MethodDeclaration;
    Block block = ((methodDeclaration.body as BlockFunctionBody)).block;
    ExpressionStatement statement = block.statements[0] as ExpressionStatement;
    MethodInvocation invocation = statement.expression as MethodInvocation;
    NodeList<Expression> arguments = invocation.argumentList.arguments;
    int argumentCount = arguments.length;
    JUnitTestCase.assertEquals(indices.length, argumentCount);
    for (int i = 0; i < argumentCount; i++) {
      Expression argument = arguments[i];
      ParameterElement element = argument.staticParameterElement;
      int index = indices[i];
      if (index < 0) {
        JUnitTestCase.assertNull(element);
      } else {
        JUnitTestCase.assertSame(parameters[index], element);
      }
    }
  }
  static dartSuite() {
    _ut.group('SimpleResolverTest', () {
      _ut.test('test_argumentResolution_requiredAndNamed_extra', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_requiredAndNamed_extra);
      });
      _ut.test('test_argumentResolution_requiredAndNamed_matching', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_requiredAndNamed_matching);
      });
      _ut.test('test_argumentResolution_requiredAndNamed_missing', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_requiredAndNamed_missing);
      });
      _ut.test('test_argumentResolution_requiredAndPositional_fewer', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_requiredAndPositional_fewer);
      });
      _ut.test('test_argumentResolution_requiredAndPositional_matching', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_requiredAndPositional_matching);
      });
      _ut.test('test_argumentResolution_requiredAndPositional_more', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_requiredAndPositional_more);
      });
      _ut.test('test_argumentResolution_required_matching', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_required_matching);
      });
      _ut.test('test_argumentResolution_required_tooFew', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_required_tooFew);
      });
      _ut.test('test_argumentResolution_required_tooMany', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_argumentResolution_required_tooMany);
      });
      _ut.test('test_class_definesCall', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_class_definesCall);
      });
      _ut.test('test_class_extends_implements', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_class_extends_implements);
      });
      _ut.test('test_commentReference_class', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_commentReference_class);
      });
      _ut.test('test_commentReference_parameter', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_commentReference_parameter);
      });
      _ut.test('test_commentReference_singleLine', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_commentReference_singleLine);
      });
      _ut.test('test_empty', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_empty);
      });
      _ut.test('test_extractedMethodAsConstant', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_extractedMethodAsConstant);
      });
      _ut.test('test_fieldFormalParameter', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_fieldFormalParameter);
      });
      _ut.test('test_forEachLoops_nonConflicting', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_forEachLoops_nonConflicting);
      });
      _ut.test('test_forLoops_nonConflicting', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_forLoops_nonConflicting);
      });
      _ut.test('test_functionTypeAlias', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_functionTypeAlias);
      });
      _ut.test('test_getterAndSetterWithDifferentTypes', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_getterAndSetterWithDifferentTypes);
      });
      _ut.test('test_hasReferenceToSuper', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_hasReferenceToSuper);
      });
      _ut.test('test_import_hide', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_import_hide);
      });
      _ut.test('test_import_prefix', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_import_prefix);
      });
      _ut.test('test_import_spaceInUri', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_import_spaceInUri);
      });
      _ut.test('test_indexExpression_typeParameters', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_indexExpression_typeParameters);
      });
      _ut.test('test_indexExpression_typeParameters_invalidAssignmentWarning', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_indexExpression_typeParameters_invalidAssignmentWarning);
      });
      _ut.test('test_indirectOperatorThroughCall', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_indirectOperatorThroughCall);
      });
      _ut.test('test_invoke_dynamicThroughGetter', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_invoke_dynamicThroughGetter);
      });
      _ut.test('test_isValidMixin_badSuperclass', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_isValidMixin_badSuperclass);
      });
      _ut.test('test_isValidMixin_constructor', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_isValidMixin_constructor);
      });
      _ut.test('test_isValidMixin_super', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_isValidMixin_super);
      });
      _ut.test('test_isValidMixin_valid', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_isValidMixin_valid);
      });
      _ut.test('test_labels_switch', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_labels_switch);
      });
      _ut.test('test_metadata_class', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_metadata_class);
      });
      _ut.test('test_metadata_field', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_metadata_field);
      });
      _ut.test('test_metadata_libraryDirective', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_metadata_libraryDirective);
      });
      _ut.test('test_metadata_method', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_metadata_method);
      });
      _ut.test('test_methodCascades', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_methodCascades);
      });
      _ut.test('test_methodCascades_withSetter', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_methodCascades_withSetter);
      });
      _ut.test('test_method_fromMixin', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_method_fromMixin);
      });
      _ut.test('test_method_fromSuperclassMixin', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_method_fromSuperclassMixin);
      });
      _ut.test('test_resolveAgainstNull', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_resolveAgainstNull);
      });
      _ut.test('test_setter_inherited', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_setter_inherited);
      });
      _ut.test('test_setter_static', () {
        final __test = new SimpleResolverTest();
        runJUnitTest(__test, __test.test_setter_static);
      });
    });
  }
}
class SubtypeManagerTest extends EngineTestCase {

  /**
   * The inheritance manager being tested.
   */
  SubtypeManager _subtypeManager;

  /**
   * The compilation unit element containing all of the types setup in each test.
   */
  CompilationUnitElementImpl _definingCompilationUnit;
  void test_computeAllSubtypes_infiniteLoop() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    classA.supertype = classB.type;
    _definingCompilationUnit.types = <ClassElement> [classA, classB];
    Set<ClassElement> subtypesOfA = _subtypeManager.computeAllSubtypes(classA);
    List<ClassElement> arraySubtypesOfA = new List.from(subtypesOfA);
    EngineTestCase.assertSize3(2, subtypesOfA);
    EngineTestCase.assertContains(arraySubtypesOfA, [classA, classB]);
  }
  void test_computeAllSubtypes_manyRecursiveSubtypes() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    ClassElementImpl classC = ElementFactory.classElement("C", classB.type, []);
    ClassElementImpl classD = ElementFactory.classElement("D", classB.type, []);
    ClassElementImpl classE = ElementFactory.classElement("E", classB.type, []);
    _definingCompilationUnit.types = <ClassElement> [classA, classB, classC, classD, classE];
    Set<ClassElement> subtypesOfA = _subtypeManager.computeAllSubtypes(classA);
    List<ClassElement> arraySubtypesOfA = new List.from(subtypesOfA);
    Set<ClassElement> subtypesOfB = _subtypeManager.computeAllSubtypes(classB);
    List<ClassElement> arraySubtypesOfB = new List.from(subtypesOfB);
    EngineTestCase.assertSize3(4, subtypesOfA);
    EngineTestCase.assertContains(arraySubtypesOfA, [classB, classC, classD, classE]);
    EngineTestCase.assertSize3(3, subtypesOfB);
    EngineTestCase.assertContains(arraySubtypesOfB, [classC, classD, classE]);
  }
  void test_computeAllSubtypes_noSubtypes() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    _definingCompilationUnit.types = <ClassElement> [classA];
    Set<ClassElement> subtypesOfA = _subtypeManager.computeAllSubtypes(classA);
    EngineTestCase.assertSize3(0, subtypesOfA);
  }
  void test_computeAllSubtypes_oneSubtype() {
    ClassElementImpl classA = ElementFactory.classElement2("A", []);
    ClassElementImpl classB = ElementFactory.classElement("B", classA.type, []);
    _definingCompilationUnit.types = <ClassElement> [classA, classB];
    Set<ClassElement> subtypesOfA = _subtypeManager.computeAllSubtypes(classA);
    List<ClassElement> arraySubtypesOfA = new List.from(subtypesOfA);
    EngineTestCase.assertSize3(1, subtypesOfA);
    EngineTestCase.assertContains(arraySubtypesOfA, [classB]);
  }
  void setUp() {
    super.setUp();
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    FileBasedSource source = new FileBasedSource.con1(new ContentCache(), FileUtilities2.createFile("/test.dart"));
    _definingCompilationUnit = new CompilationUnitElementImpl("test.dart");
    _definingCompilationUnit.source = source;
    LibraryElementImpl definingLibrary = ElementFactory.library(context, "test");
    definingLibrary.definingCompilationUnit = _definingCompilationUnit;
    _subtypeManager = new SubtypeManager();
  }
  static dartSuite() {
    _ut.group('SubtypeManagerTest', () {
      _ut.test('test_computeAllSubtypes_infiniteLoop', () {
        final __test = new SubtypeManagerTest();
        runJUnitTest(__test, __test.test_computeAllSubtypes_infiniteLoop);
      });
      _ut.test('test_computeAllSubtypes_manyRecursiveSubtypes', () {
        final __test = new SubtypeManagerTest();
        runJUnitTest(__test, __test.test_computeAllSubtypes_manyRecursiveSubtypes);
      });
      _ut.test('test_computeAllSubtypes_noSubtypes', () {
        final __test = new SubtypeManagerTest();
        runJUnitTest(__test, __test.test_computeAllSubtypes_noSubtypes);
      });
      _ut.test('test_computeAllSubtypes_oneSubtype', () {
        final __test = new SubtypeManagerTest();
        runJUnitTest(__test, __test.test_computeAllSubtypes_oneSubtype);
      });
    });
  }
}
main() {
//  ElementResolverTest.dartSuite();
//  InheritanceManagerTest.dartSuite();
//  LibraryElementBuilderTest.dartSuite();
//  LibraryTest.dartSuite();
//  StaticTypeAnalyzerTest.dartSuite();
//  SubtypeManagerTest.dartSuite();
//  TypeOverrideManagerTest.dartSuite();
//  TypeProviderImplTest.dartSuite();
//  TypeResolverVisitorTest.dartSuite();
//  EnclosedScopeTest.dartSuite();
//  LibraryImportScopeTest.dartSuite();
//  LibraryScopeTest.dartSuite();
//  ScopeTest.dartSuite();
//  CompileTimeErrorCodeTest.dartSuite();
//  ErrorResolverTest.dartSuite();
//  HintCodeTest.dartSuite();
//  MemberMapTest.dartSuite();
//  NonHintCodeTest.dartSuite();
//  NonErrorResolverTest.dartSuite();
//  SimpleResolverTest.dartSuite();
//  StaticTypeWarningCodeTest.dartSuite();
//  StaticWarningCodeTest.dartSuite();
//  StrictModeTest.dartSuite();
//  TypePropagationTest.dartSuite();
}
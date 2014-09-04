/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.resolver;

import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import junit.framework.AssertionFailedError;

public class TypePropagationTest extends ResolverTestCase {
  public void fail_mergePropagatedTypesAtJoinPoint_1() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    assertTypeOfMarkedExpression(
        createSource(
            "f1(x) {",
            "  var y = [];",
            "  if (x) {",
            "    y = 0;",
            "  } else {",
            "    y = '';",
            "  }",
            "  // Propagated type is [List] here: incorrect.",
            "  // Best we can do is [Object]?",
            "  return y; // marker",
            "}"),
        // Don't care about the static type.
        null,
        // TODO(collinsn):
        // In general, it might be more useful to compute a (structural)
        // intersection of interfaces here;
        // the best nominal type may be much less precise.
        //
        // The same concern applies in the other [fail_merge*] tests expecting [dynamic] below.
        getTypeProvider().getDynamicType());
  }

  public void fail_mergePropagatedTypesAtJoinPoint_2() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    assertTypeOfMarkedExpression(
        createSource(
            "f2(x) {",
            "  var y = [];",
            "  if (x) {",
            "    y = 0;",
            "  } else {",
            "  }",
            "  // Propagated type is [List] here: incorrect.",
            "  // Best we can do is [Object]?",
            "  return y; // marker",
            "}"),
        // Don't care about the static type.
        null,
        getTypeProvider().getDynamicType());
  }

  public void fail_mergePropagatedTypesAtJoinPoint_3() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    assertTypeOfMarkedExpression(
        createSource(
            "f4(x) {",
            "  var y = [];",
            "  if (x) {",
            "    y = 0;",
            "  } else {",
            "    y = 1.5;",
            "  }",
            "  // Propagated type is [List] here: incorrect.",
            "  // A correct answer is the least upper bound of [int] and [double],",
            "  // i.e. [num].",
            "  return y; // marker",
            "}"),
        // Don't care about the static type.
        null,
        getTypeProvider().getNumType());
  }

  public void fail_mergePropagatedTypesAtJoinPoint_5() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    assertTypeOfMarkedExpression(
        createSource(
            "f6(x,y) {",
            "  var z = [];",
            "  if (x || (z = y) < 0) {",
            "  } else {",
            "    z = 0;",
            "  }",
            "  // Propagated type is [List] here: incorrect.",
            "  // Best we can do is [Object]?",
            "  return z; // marker",
            "}"),
        // Don't care about the static type.
        null,
        getTypeProvider().getDynamicType());
  }

  public void fail_mergePropagatedTypesAtJoinPoint_7() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    //
    // In general [continue]s are unsafe for the purposes of [isAbruptTerminationStatement].
    //
    // This is like example 6, but less tricky: the code in the branch that
    // [continue]s is in effect after the [if].
    String code = createSource(
        "f() {",
        "  var x = 0;",
        "  var c = false;",
        "  var d = true;",
        "  while (d) {",
        "    if (c) {",
        "      d = false;",
        "    } else {",
        "      x = '';",
        "      c = true;",
        "      continue;",
        "    }",
        "    x; // marker",
        "  }",
        "}");
    Type t = findMarkedIdentifier(code, "; // marker").getPropagatedType();
    assertTrue(getTypeProvider().getIntType().isSubtypeOf(t));
    assertTrue(getTypeProvider().getStringType().isSubtypeOf(t));
  }

  public void fail_mergePropagatedTypesAtJoinPoint_8() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    //
    // In nested loops [breaks]s are unsafe for the purposes of [isAbruptTerminationStatement].
    //
    // This is a combination of 6 and 7: we use an unlabeled [break]
    // like a continue for the outer loop / like a labeled [break] to
    // jump just above the [if].
    String code = createSource(
        "f() {",
        "  var x = 0;",
        "  var c = false;",
        "  var d = true;",
        "  while (d) {",
        "    while (d) {",
        "      if (c) {",
        "        d = false;",
        "      } else {",
        "        x = '';",
        "        c = true;",
        "        break;",
        "      }",
        "      x; // marker",
        "    }",
        "  }",
        "}");
    Type t = findMarkedIdentifier(code, "; // marker").getPropagatedType();
    assertTrue(getTypeProvider().getIntType().isSubtypeOf(t));
    assertTrue(getTypeProvider().getStringType().isSubtypeOf(t));
  }

  public void fail_propagatedReturnType_functionExpression() throws Exception {
    // TODO(scheglov) disabled because we don't resolve function expression
    String code = createSource(//
        "main() {",
        "  var v = (() {return 42;})();",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_as() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  bool get g => true;",
        "}",
        "A f(var p) {",
        "  if ((p as A).g) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getThenStatement()).getStatements().get(
        0);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_assert() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  assert (p is A);",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_assignment() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v;",
        "  v = 0;",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(2);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(getTypeProvider().getIntType(), variableName.getPropagatedType());
  }

  public void test_assignment_afterInitializer() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v = 0;",
        "  v = 1.0;",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(2);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(getTypeProvider().getDoubleType(), variableName.getPropagatedType());
  }

  public void test_assignment_null() throws Exception {
    String code = createSource(//
        "main() {",
        "  int v; // declare",
        "  v = null;",
        "  return v; // return",
        "}");
    CompilationUnit unit;
    {
      Source source = addSource(code);
      LibraryElement library = resolve(source);
      assertNoErrors(source);
      verify(source);
      unit = resolveCompilationUnit(source, library);
    }
    {
      SimpleIdentifier identifier = findNode(unit, code, "v; // declare", SimpleIdentifier.class);
      assertSame(getTypeProvider().getIntType(), identifier.getStaticType());
      assertSame(null, identifier.getPropagatedType());
    }
    {
      SimpleIdentifier identifier = findNode(unit, code, "v = null;", SimpleIdentifier.class);
      assertSame(getTypeProvider().getIntType(), identifier.getStaticType());
      assertSame(null, identifier.getPropagatedType());
    }
    {
      SimpleIdentifier identifier = findNode(unit, code, "v; // return", SimpleIdentifier.class);
      assertSame(getTypeProvider().getIntType(), identifier.getStaticType());
      assertSame(null, identifier.getPropagatedType());
    }
  }

  public void test_CanvasElement_getContext() throws Exception {
    String code = createSource(//
        "import 'dart:html';",
        "main(CanvasElement canvas) {",
        "  var context = canvas.getContext('2d');",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);

    SimpleIdentifier identifier = findNode(unit, code, "context", SimpleIdentifier.class);
    assertEquals("CanvasRenderingContext2D", identifier.getPropagatedType().getName());
  }

  public void test_finalPropertyInducingVariable_classMember_instance() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "class A {",
        "  final v = 0;",
        "}"));
    String code = createSource(//
        "import 'lib.dart';",
        "f(A a) {",
        "  return a.v; // marker",
        "}");
    assertTypeOfMarkedExpression(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_finalPropertyInducingVariable_classMember_instance_inherited() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "class A {",
        "  final v = 0;",
        "}"));
    String code = createSource(//
        "import 'lib.dart';",
        "class B extends A {",
        "  m() {",
        "    return v; // marker",
        "  }",
        "}");
    assertTypeOfMarkedExpression(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_finalPropertyInducingVariable_classMember_instance_propagatedTarget()
      throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "class A {",
        "  final v = 0;",
        "}"));
    String code = createSource(//
        "import 'lib.dart';",
        "f(p) {",
        "  if (p is A) {",
        "    return p.v; // marker",
        "  }",
        "}");
    assertTypeOfMarkedExpression(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_finalPropertyInducingVariable_classMember_static() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "class A {",
        "  static final V = 0;",
        "}"));
    String code = createSource(//
        "import 'lib.dart';",
        "f() {",
        "  return A.V; // marker",
        "}");
    assertTypeOfMarkedExpression(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_finalPropertyInducingVariable_topLevelVaraible_prefixed() throws Exception {
    addNamedSource("/lib.dart", "final V = 0;");
    String code = createSource(//
        "import 'lib.dart' as p;",
        "f() {",
        "  var v2 = p.V; // marker prefixed",
        "}");
    assertTypeOfMarkedExpression(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_finalPropertyInducingVariable_topLevelVaraible_simple() throws Exception {
    addNamedSource("/lib.dart", "final V = 0;");
    String code = createSource(//
        "import 'lib.dart';",
        "f() {",
        "  return V; // marker simple",
        "}");
    assertTypeOfMarkedExpression(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_forEach() throws Exception {
    String code = createSource(//
        "main() {",
        "  var list = <String> [];",
        "  for (var e in list) {",
        "    e;",
        "  }",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    InterfaceType stringType = getTypeProvider().getStringType();
    // in the declaration
    {
      SimpleIdentifier identifier = findNode(unit, code, "e in", SimpleIdentifier.class);
      assertSame(stringType, identifier.getPropagatedType());
    }
    // in the loop body
    {
      SimpleIdentifier identifier = findNode(unit, code, "e;", SimpleIdentifier.class);
      assertSame(stringType, identifier.getPropagatedType());
    }
  }

  public void test_functionExpression_asInvocationArgument() throws Exception {
    String code = createSource(//
        "class MyMap<K, V> {",
        "  forEach(f(K key, V value)) {}",
        "}",
        "f(MyMap<int, String> m) {",
        "  m.forEach((k, v) {",
        "    k;",
        "    v;",
        "  });",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    // k
    Type intType = getTypeProvider().getIntType();
    FormalParameter kParameter = findNode(unit, code, "k, ", SimpleFormalParameter.class);
    assertSame(intType, kParameter.getIdentifier().getPropagatedType());
    SimpleIdentifier kIdentifier = findNode(unit, code, "k;", SimpleIdentifier.class);
    assertSame(intType, kIdentifier.getPropagatedType());
    assertSame(getTypeProvider().getDynamicType(), kIdentifier.getStaticType());
    // v
    Type stringType = getTypeProvider().getStringType();
    FormalParameter vParameter = findNode(unit, code, "v)", SimpleFormalParameter.class);
    assertSame(stringType, vParameter.getIdentifier().getPropagatedType());
    SimpleIdentifier vIdentifier = findNode(unit, code, "v;", SimpleIdentifier.class);
    assertSame(stringType, vIdentifier.getPropagatedType());
    assertSame(getTypeProvider().getDynamicType(), vIdentifier.getStaticType());
  }

  public void test_functionExpression_asInvocationArgument_fromInferredInvocation()
      throws Exception {
    String code = createSource(//
        "class MyMap<K, V> {",
        "  forEach(f(K key, V value)) {}",
        "}",
        "f(MyMap<int, String> m) {",
        "  var m2 = m;",
        "  m2.forEach((k, v) {});",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    // k
    Type intType = getTypeProvider().getIntType();
    FormalParameter kParameter = findNode(unit, code, "k, ", SimpleFormalParameter.class);
    assertSame(intType, kParameter.getIdentifier().getPropagatedType());
    // v
    Type stringType = getTypeProvider().getStringType();
    FormalParameter vParameter = findNode(unit, code, "v)", SimpleFormalParameter.class);
    assertSame(stringType, vParameter.getIdentifier().getPropagatedType());
  }

  public void test_functionExpression_asInvocationArgument_functionExpressionInvocation()
      throws Exception {
    String code = createSource(//
        "main() {",
        "  (f(String value)) {} ((v) {",
        "    v;",
        "  });",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    // v
    Type dynamicType = getTypeProvider().getDynamicType();
    Type stringType = getTypeProvider().getStringType();
    FormalParameter vParameter = findNode(unit, code, "v)", FormalParameter.class);
    assertSame(stringType, vParameter.getIdentifier().getPropagatedType());
    assertSame(dynamicType, vParameter.getIdentifier().getStaticType());
    SimpleIdentifier vIdentifier = findNode(unit, code, "v;", SimpleIdentifier.class);
    assertSame(stringType, vIdentifier.getPropagatedType());
    assertSame(dynamicType, vIdentifier.getStaticType());
  }

  public void test_functionExpression_asInvocationArgument_keepIfLessSpecific() throws Exception {
    String code = createSource(//
        "class MyList {",
        "  forEach(f(Object value)) {}",
        "}",
        "f(MyList list) {",
        "  list.forEach((int v) {",
        "    v;",
        "  });",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    // v
    Type intType = getTypeProvider().getIntType();
    FormalParameter vParameter = findNode(unit, code, "v)", SimpleFormalParameter.class);
    assertSame(null, vParameter.getIdentifier().getPropagatedType());
    assertSame(intType, vParameter.getIdentifier().getStaticType());
    SimpleIdentifier vIdentifier = findNode(unit, code, "v;", SimpleIdentifier.class);
    assertSame(intType, vIdentifier.getStaticType());
    assertSame(null, vIdentifier.getPropagatedType());
  }

  public void test_functionExpression_asInvocationArgument_notSubtypeOfStaticType()
      throws Exception {
    String code = createSource(//
        "class A {",
        "  m(void f(int i)) {}",
        "}",
        "x() {",
        "  A a = new A();",
        "  a.m(() => 0);",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    // () => 0
    FunctionExpression functionExpression = findNode(
        unit,
        code,
        "() => 0)",
        FunctionExpression.class);
    assertSame(0, ((FunctionType) functionExpression.getStaticType()).getParameters().length);
    assertSame(null, functionExpression.getPropagatedType());
  }

  public void test_functionExpression_asInvocationArgument_replaceIfMoreSpecific() throws Exception {
    String code = createSource(//
        "class MyList<E> {",
        "  forEach(f(E value)) {}",
        "}",
        "f(MyList<String> list) {",
        "  list.forEach((Object v) {",
        "    v;",
        "  });",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    // v
    Type stringType = getTypeProvider().getStringType();
    FormalParameter vParameter = findNode(unit, code, "v)", SimpleFormalParameter.class);
    assertSame(stringType, vParameter.getIdentifier().getPropagatedType());
    assertSame(getTypeProvider().getObjectType(), vParameter.getIdentifier().getStaticType());
    SimpleIdentifier vIdentifier = findNode(unit, code, "v;", SimpleIdentifier.class);
    assertSame(stringType, vIdentifier.getPropagatedType());
  }

  public void test_Future_then() throws Exception {
    String code = createSource(//
        "import 'dart:async';",
        "main(Future<int> firstFuture) {",
        "  firstFuture.then((p1) {",
        "    return 1.0;",
        "  }).then((p2) {",
        "    return new Future<String>.value('str');",
        "  }).then((p3) {",
        "  });",
        "}");
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    // p1
    FormalParameter p1 = findNode(unit, code, "p1) {", SimpleFormalParameter.class);
    assertSame(getTypeProvider().getIntType(), p1.getIdentifier().getPropagatedType());
    // p2
    FormalParameter p2 = findNode(unit, code, "p2) {", SimpleFormalParameter.class);
    assertSame(getTypeProvider().getDoubleType(), p2.getIdentifier().getPropagatedType());
    // p3
    FormalParameter p3 = findNode(unit, code, "p3) {", SimpleFormalParameter.class);
    assertSame(getTypeProvider().getStringType(), p3.getIdentifier().getPropagatedType());
  }

  public void test_initializer() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v = 0;",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    NodeList<Statement> statements = body.getBlock().getStatements();
    // Type of 'v' in declaration.
    {
      VariableDeclarationStatement statement = (VariableDeclarationStatement) statements.get(0);
      SimpleIdentifier variableName = statement.getVariables().getVariables().get(0).getName();
      assertSame(getTypeProvider().getDynamicType(), variableName.getStaticType());
      assertSame(getTypeProvider().getIntType(), variableName.getPropagatedType());
    }
    // Type of 'v' in reference.
    {
      ReturnStatement statement = (ReturnStatement) statements.get(1);
      SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
      assertSame(getTypeProvider().getIntType(), variableName.getPropagatedType());
    }
  }

  public void test_initializer_dereference() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v = 'String';",
        "  v.",
        "}"));
    LibraryElement library = resolve(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ExpressionStatement statement = (ExpressionStatement) body.getBlock().getStatements().get(1);
    PrefixedIdentifier invocation = (PrefixedIdentifier) statement.getExpression();
    SimpleIdentifier variableName = invocation.getPrefix();
    assertSame(getTypeProvider().getStringType(), variableName.getPropagatedType());
  }

  public void test_initializer_null() throws Exception {
    String code = createSource(//
        "main() {",
        "  int v = null;",
        "  return v; // marker",
        "}");
    CompilationUnit unit;
    {
      Source source = addSource(code);
      LibraryElement library = resolve(source);
      assertNoErrors(source);
      verify(source);
      unit = resolveCompilationUnit(source, library);
    }
    {
      SimpleIdentifier identifier = findNode(unit, code, "v = null;", SimpleIdentifier.class);
      assertSame(getTypeProvider().getIntType(), identifier.getStaticType());
      assertSame(null, identifier.getPropagatedType());
    }
    {
      SimpleIdentifier identifier = findNode(unit, code, "v; // marker", SimpleIdentifier.class);
      assertSame(getTypeProvider().getIntType(), identifier.getStaticType());
      assertSame(null, identifier.getPropagatedType());
    }
  }

  public void test_is_conditional() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  return (p is A) ? p : null;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(0);
    ConditionalExpression conditional = (ConditionalExpression) statement.getExpression();
    SimpleIdentifier variableName = (SimpleIdentifier) conditional.getThenExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_is_if() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is A) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getThenStatement()).getStatements().get(
        0);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_is_if_lessSpecific() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(A p) {",
        "  if (p is String) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
//    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
//    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getThenStatement()).getStatements().get(
        0);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(null, variableName.getPropagatedType());
  }

  public void test_is_if_logicalAnd() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is A && p != null) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getThenStatement()).getStatements().get(
        0);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_is_postConditional() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  A a = (p is A) ? p : throw null;",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_is_postIf() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is A) {",
        "    A a = p;",
        "  } else {",
        "    return null;",
        "  }",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_is_subclass() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  B m() => this;",
        "}",
        "A f(A p) {",
        "  if (p is B) {",
        "    return p.m();",
        "  }",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(2);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getThenStatement()).getStatements().get(
        0);
    MethodInvocation invocation = (MethodInvocation) statement.getExpression();
    assertNotNull(invocation.getMethodName().getPropagatedElement());
  }

  public void test_is_while() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  while (p is A) {",
        "    return p;",
        "  }",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    WhileStatement whileStatement = (WhileStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) whileStatement.getBody()).getStatements().get(
        0);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_isNot_conditional() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  return (p is! A) ? null : p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(0);
    ConditionalExpression conditional = (ConditionalExpression) statement.getExpression();
    SimpleIdentifier variableName = (SimpleIdentifier) conditional.getElseExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_isNot_if() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is! A) {",
        "    return null;",
        "  } else {",
        "    return p;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getElseStatement()).getStatements().get(
        0);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_isNot_if_logicalOr() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is! A || null == p) {",
        "    return null;",
        "  } else {",
        "    return p;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getElseStatement()).getStatements().get(
        0);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_isNot_postConditional() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  A a = (p is! A) ? throw null : p;",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_isNot_postIf() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is! A) {",
        "    return null;",
        "  }",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getPropagatedType());
  }

  public void test_listLiteral_different() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v = [0, '1', 2];",
        "  return v[2];",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    IndexExpression indexExpression = (IndexExpression) statement.getExpression();
    assertNull(indexExpression.getPropagatedType());
  }

  public void test_listLiteral_same() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v = [0, 1, 2];",
        "  return v[2];",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    IndexExpression indexExpression = (IndexExpression) statement.getExpression();
    assertNull(indexExpression.getPropagatedType());
    Expression v = indexExpression.getTarget();
    InterfaceType propagatedType = (InterfaceType) v.getPropagatedType();
    assertSame(getTypeProvider().getListType().getElement(), propagatedType.getElement());
    Type[] typeArguments = propagatedType.getTypeArguments();
    assertLength(1, typeArguments);
    assertSame(getTypeProvider().getDynamicType(), typeArguments[0]);
  }

  public void test_mapLiteral_different() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v = {'0' : 0, 1 : '1', '2' : 2};",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier identifier = (SimpleIdentifier) statement.getExpression();
    InterfaceType propagatedType = (InterfaceType) identifier.getPropagatedType();
    assertSame(getTypeProvider().getMapType().getElement(), propagatedType.getElement());
    Type[] typeArguments = propagatedType.getTypeArguments();
    assertLength(2, typeArguments);
    assertSame(getTypeProvider().getDynamicType(), typeArguments[0]);
    assertSame(getTypeProvider().getDynamicType(), typeArguments[1]);
  }

  public void test_mapLiteral_same() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var v = {'a' : 0, 'b' : 1, 'c' : 2};",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier identifier = (SimpleIdentifier) statement.getExpression();
    InterfaceType propagatedType = (InterfaceType) identifier.getPropagatedType();
    assertSame(getTypeProvider().getMapType().getElement(), propagatedType.getElement());
    Type[] typeArguments = propagatedType.getTypeArguments();
    assertLength(2, typeArguments);
    assertSame(getTypeProvider().getDynamicType(), typeArguments[0]);
    assertSame(getTypeProvider().getDynamicType(), typeArguments[1]);
  }

  public void test_mergePropagatedTypesAtJoinPoint_4() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    assertTypeOfMarkedExpression(
        createSource(
            "f5(x) {",
            "  var y = [];",
            "  if (x) {",
            "    y = 0;",
            "  } else {",
            "    return y;",
            "  }",
            "  // Propagated type is [int] here: correct.",
            "  return y; // marker",
            "}"),
        // Don't care about the static type.
        null,
        getTypeProvider().getIntType());
  }

  public void test_mergePropagatedTypesAtJoinPoint_6() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=19929
    //
    // Labeled [break]s are unsafe for the purposes of [isAbruptTerminationStatement].
    //
    // This is tricky: the [break] jumps back above the [if], making
    // it into a loop of sorts. The [if] type-propagation code assumes
    // that [break] does not introduce a loop.
    String code = createSource(
        "f() {",
        "  var x = 0;",
        "  var c = false;",
        "  L: ",
        "  if (c) {",
        "  } else {",
        "    x = '';",
        "    c = true;",
        "    break L;",
        "  }",
        "  x; // marker",
        "}");
    Type t = findMarkedIdentifier(code, "; // marker").getPropagatedType();
    assertTrue(getTypeProvider().getIntType().isSubtypeOf(t));
    assertTrue(getTypeProvider().getStringType().isSubtypeOf(t));
  }

  public void test_objectMethodOnDynamicExpression_doubleEquals() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=20342
    //
    // This was not actually part of Issue 20342, since the spec specifies a
    // static type of [bool] for [==] comparison and the implementation
    // was already consistent with the spec there. But, it's another
    // [Object] method, so it's included here.
    assertTypeOfMarkedExpression(createSource(//
        "f1(x) {",
        "  var v = (x == x);",
        "  return v; // marker",
        "}"), null, getTypeProvider().getBoolType());
  }

  public void test_objectMethodOnDynamicExpression_hashCode() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=20342
    assertTypeOfMarkedExpression(createSource(//
        "f1(x) {",
        "  var v = x.hashCode;",
        "  return v; // marker",
        "}"), null, getTypeProvider().getIntType());
  }

  public void test_objectMethodOnDynamicExpression_runtimeType() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=20342
    assertTypeOfMarkedExpression(createSource(//
        "f1(x) {",
        "  var v = x.runtimeType;",
        "  return v; // marker",
        "}"), null, getTypeProvider().getTypeType());
  }

  public void test_objectMethodOnDynamicExpression_toString() throws Exception {
    // https://code.google.com/p/dart/issues/detail?id=20342
    assertTypeOfMarkedExpression(createSource(//
        "f1(x) {",
        "  var v = x.toString();",
        "  return v; // marker",
        "}"), null, getTypeProvider().getStringType());
  }

  public void test_propagatedReturnType_function_hasReturnType_returnsNull() throws Exception {
    String code = createSource(//
        "String f() => null;",
        "main() {",
        "  var v = f();",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getStringType());
  }

  public void test_propagatedReturnType_function_lessSpecificStaticReturnType() throws Exception {
    String code = createSource(//
        "Object f() => 42;",
        "main() {",
        "  var v = f();",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_propagatedReturnType_function_moreSpecificStaticReturnType() throws Exception {
    String code = createSource(//
        "int f(v) => (v as num);",
        "main() {",
        "  var v = f(3);",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_propagatedReturnType_function_noReturnTypeName_blockBody_multipleReturns()
      throws Exception {
    String code = createSource(//
        "f() {",
        "  if (true) return 0;",
        "  return 1.0;",
        "}",
        "main() {",
        "  var v = f();",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getNumType());
  }

  public void test_propagatedReturnType_function_noReturnTypeName_blockBody_oneReturn()
      throws Exception {
    String code = createSource(//
        "f() {",
        "  var z = 42;",
        "  return z;",
        "}",
        "main() {",
        "  var v = f();",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_propagatedReturnType_function_noReturnTypeName_expressionBody() throws Exception {
    String code = createSource(//
        "f() => 42;",
        "main() {",
        "  var v = f();",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_propagatedReturnType_localFunction() throws Exception {
    String code = createSource(//
        "main() {",
        "  f() => 42;",
        "  var v = f();",
        "}");
    assertPropagatedReturnType(
        code,
        getTypeProvider().getDynamicType(),
        getTypeProvider().getIntType());
  }

  public void test_query() throws Exception {
    Source source = addSource(createSource(//
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
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration main = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) main.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(11);
    NodeList<Expression> elements = ((ListLiteral) statement.getExpression()).getElements();
    assertEquals("AnchorElement", elements.get(0).getPropagatedType().getName());
    assertEquals("AnchorElement", elements.get(1).getPropagatedType().getName());
    assertEquals("BodyElement", elements.get(2).getPropagatedType().getName());
    assertEquals("ButtonElement", elements.get(3).getPropagatedType().getName());
    assertEquals("DivElement", elements.get(4).getPropagatedType().getName());
    assertEquals("InputElement", elements.get(5).getPropagatedType().getName());
    assertEquals("SelectElement", elements.get(6).getPropagatedType().getName());
    assertEquals("DivElement", elements.get(7).getPropagatedType().getName());
    assertEquals("Element", elements.get(8).getPropagatedType().getName());
    assertEquals("Element", elements.get(9).getPropagatedType().getName());
    assertEquals("Element", elements.get(10).getPropagatedType().getName());
  }

  /**
   * @param code the code that assigns the value to the variable "v", no matter how. We check that
   *          "v" has expected static and propagated type.
   */
  private void assertPropagatedReturnType(String code, Type expectedStaticType,
      Type expectedPropagatedType) throws Exception {

    SimpleIdentifier identifier = findMarkedIdentifier(code, "v = ");
    assertSame(expectedStaticType, identifier.getStaticType());
    assertSame(expectedPropagatedType, identifier.getPropagatedType());
  }

  /**
   * Check the static and propagated types of the expression marked with "; // marker" comment.
   * 
   * @param code source code to analyze, with the expression to check marked with "// marker".
   * @param expectedStaticType if non-null, check actual static type is equal to this.
   * @param expectedPropagatedType if non-null, check actual static type is equal to this.
   * @throws Exception
   */
  private void assertTypeOfMarkedExpression(String code, Type expectedStaticType,
      Type expectedPropagatedType) throws Exception {
    SimpleIdentifier identifier = findMarkedIdentifier(code, "; // marker");
    if (expectedStaticType != null) {
      assertSame(expectedStaticType, identifier.getStaticType());
    }
    if (expectedPropagatedType != null) {
      assertSame(expectedPropagatedType, identifier.getPropagatedType());
    }
  }

  /**
   * Return the {@code SimpleIdentifier} marked by {@code marker}. The source code must have no
   * errors and be verifiable.
   * 
   * @param code source code to analyze.
   * @param marker marker identifying sought after expression in source code.
   * @return expression marked by the marker.
   * @throws Exception
   */
  private SimpleIdentifier findMarkedIdentifier(String code, String marker) throws Exception {
    try {
      Source source = addSource(code);
      LibraryElement library = resolve(source);
      assertNoErrors(source);
      verify(source);
      CompilationUnit unit = resolveCompilationUnit(source, library);
      // Could generalize this further by making [SimpleIdentifier.class] a parameter.
      return findNode(unit, code, marker, SimpleIdentifier.class);
    } catch (AssertionFailedError exception) {
      // Is there a better exception to throw here? The point is that an assertion failure
      // here should be a failure, in both "test_*" and "fail_*" tests.
      // However, an assertion failure is success for the purpose of "fail_*" tests, so
      // without catching them here "fail_*" tests can succeed by failing for the wrong reason.
      throw new Exception("Unexexpected assertion failure: " + exception);
    }
  }
}

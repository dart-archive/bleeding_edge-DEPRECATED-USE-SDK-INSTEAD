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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

public class TypePropagationTest extends ResolverTestCase {
  public void fail_propagatedReturnType_functionExpression() throws Exception {
    // TODO(scheglov) disabled because we don't resolve function expression
    String code = createSource(//
        "main() {",
        "  var v = (() {return 42;})();",
        "}");
    check_propagatedReturnType(
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

  public void test_forEach() throws Exception {
    String code = createSource(//
        "f(List<String> p) {",
        "  for (var e in p) {",
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
    assertSame(getTypeProvider().getIntType(), indexExpression.getPropagatedType());
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
    assertSame(getTypeProvider().getStringType(), typeArguments[0]);
    assertSame(getTypeProvider().getIntType(), typeArguments[1]);
  }

  public void test_propagatedReturnType_function_hasReturnType_returnsNull() throws Exception {
    String code = createSource(//
        "String f() => null;",
        "main() {",
        "  var v = f();",
        "}");
    check_propagatedReturnType(
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
    check_propagatedReturnType(
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
    check_propagatedReturnType(
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
    check_propagatedReturnType(
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
    check_propagatedReturnType(
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
    check_propagatedReturnType(
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
    check_propagatedReturnType(
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
  private void check_propagatedReturnType(String code, Type expectedStaticType,
      Type expectedPropagatedType) throws Exception {
    Source source = addSource(code);
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    //
    SimpleIdentifier identifier = findNode(unit, code, "v = ", SimpleIdentifier.class);
    assertSame(expectedStaticType, identifier.getStaticType());
    assertSame(expectedPropagatedType, identifier.getPropagatedType());
  }
}

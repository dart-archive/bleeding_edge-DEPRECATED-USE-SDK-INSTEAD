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
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;

public class TypePropagationTest extends ResolverTestCase {
  public void test_as() throws Exception {
    Source source = addSource("/test.dart", createSource(//
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
    assertNoErrors();
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
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_assert() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  assert (p is A);",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_assignment() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var v;",
        "  v = 0;",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(2);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(getTypeProvider().getIntType(), variableName.getStaticType());
  }

  public void test_assignment_afterInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var v = 0;",
        "  v = 1.0;",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(2);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(getTypeProvider().getDoubleType(), variableName.getStaticType());
  }

  public void test_initializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var v = 0;",
        "  return v;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(0);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(getTypeProvider().getIntType(), variableName.getStaticType());
  }

  public void test_is_conditional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  return (p is A) ? p : null;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(0);
    ConditionalExpression conditional = (ConditionalExpression) statement.getExpression();
    SimpleIdentifier variableName = (SimpleIdentifier) conditional.getThenExpression();
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_is_if() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is A) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
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
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_is_if_logicalAnd() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is A && p != null) {",
        "    return p;",
        "  } else {",
        "    return null;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
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
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_is_postConditional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  A a = (p is A) ? p : throw null;",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_is_postIf() throws Exception {
    Source source = addSource("/test.dart", createSource(//
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
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_is_subclass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {",
        "  B m() => this;",
        "}",
        "A f(A p) {",
        "  if (p is B) {",
        "    return p.m();",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(2);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(0);
    ReturnStatement statement = (ReturnStatement) ((Block) ifStatement.getThenStatement()).getStatements().get(
        0);
    MethodInvocation invocation = (MethodInvocation) statement.getExpression();
    assertNotNull(invocation.getMethodName().getElement());
  }

  public void test_isNot_conditional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  return (p is! A) ? null : p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(0);
    ConditionalExpression conditional = (ConditionalExpression) statement.getExpression();
    SimpleIdentifier variableName = (SimpleIdentifier) conditional.getElseExpression();
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_isNot_if() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is! A) {",
        "    return null;",
        "  } else {",
        "    return p;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
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
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_isNot_if_logicalOr() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is! A || null == p) {",
        "    return null;",
        "  } else {",
        "    return p;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
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
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_isNot_postConditional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  A a = (p is! A) ? throw null : p;",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getStaticType());
  }

  public void test_isNot_postIf() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "A f(var p) {",
        "  if (p is! A) {",
        "    return null;",
        "  }",
        "  return p;",
        "}"));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    ClassDeclaration classA = (ClassDeclaration) unit.getDeclarations().get(0);
    InterfaceType typeA = classA.getElement().getType();
    FunctionDeclaration function = (FunctionDeclaration) unit.getDeclarations().get(1);
    BlockFunctionBody body = (BlockFunctionBody) function.getFunctionExpression().getBody();
    ReturnStatement statement = (ReturnStatement) body.getBlock().getStatements().get(1);
    SimpleIdentifier variableName = (SimpleIdentifier) statement.getExpression();
    assertSame(typeA, variableName.getStaticType());
  }
}

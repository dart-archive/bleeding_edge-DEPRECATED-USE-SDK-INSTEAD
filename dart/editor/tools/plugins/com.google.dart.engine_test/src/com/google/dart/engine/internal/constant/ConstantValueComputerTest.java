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
package com.google.dart.engine.internal.constant;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.logging.TestLogger;

import java.util.HashMap;
import java.util.HashSet;

public class ConstantValueComputerTest extends ResolverTestCase {
  class ValidatingConstantVisitor extends ConstantVisitor {
    private HashMap<AstNode, HashSet<AstNode>> capturedDependencies;
    private AstNode nodeBeingEvaluated;

    public ValidatingConstantVisitor(TypeProvider typeProvider,
        HashMap<AstNode, HashSet<AstNode>> capturedDependencies, AstNode nodeBeingEvaluated) {
      super(typeProvider);
      this.capturedDependencies = capturedDependencies;
      this.nodeBeingEvaluated = nodeBeingEvaluated;
    }

    @Override
    protected void beforeGetEvaluationResult(AstNode node) {
      super.beforeGetEvaluationResult(node);

      // If we are getting the evaluation result for a node in the graph, make sure we properly
      // recorded the dependency.
      if (capturedDependencies.containsKey(node)) {
        assertTrue(capturedDependencies.get(nodeBeingEvaluated).contains(node));
      }
    }
  }

  private class ValidatingConstantValueComputer extends ConstantValueComputer {
    private HashMap<AstNode, HashSet<AstNode>> capturedDependencies;
    private AstNode nodeBeingEvaluated;

    public ValidatingConstantValueComputer(TypeProvider typeProvider) {
      super(typeProvider);
    }

    @Override
    protected void beforeComputeValue(AstNode constNode) {
      super.beforeComputeValue(constNode);
      nodeBeingEvaluated = constNode;
    }

    @Override
    protected void beforeGraphWalk() {
      super.beforeGraphWalk();

      // Capture the dependency info in referenceGraph so that we can check it later.  (We need
      // to capture it now, before nodes get removed from the graph).
      capturedDependencies = new HashMap<AstNode, HashSet<AstNode>>();
      for (AstNode head : referenceGraph.getNodes()) {
        HashSet<AstNode> tails = new HashSet<AstNode>();
        for (AstNode tail : referenceGraph.getTails(head)) {
          tails.add(tail);
        }
        capturedDependencies.put(head, tails);
      }
    }

    @Override
    protected ConstantVisitor createConstantVisitor() {
      return new ValidatingConstantVisitor(typeProvider, capturedDependencies, nodeBeingEvaluated);
    }

  }

  public void test_computeValues_cycle() throws Exception {
    TestLogger logger = new TestLogger();
    AnalysisEngine.getInstance().setLogger(logger);

    Source librarySource = addSource(createSource(//
        "const int a = c;",
        "const int b = a;",
        "const int c = b;"));
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    getAnalysisContext().computeErrors(librarySource);
    assertNotNull(unit);

    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSizeOfList(3, members);
    validate(false, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
    validate(false, ((TopLevelVariableDeclaration) members.get(1)).getVariables());
    validate(false, ((TopLevelVariableDeclaration) members.get(2)).getVariables());
  }

  public void test_computeValues_dependentVariables() throws Exception {
    Source librarySource = addSource(createSource(//
        "const int b = a;",
        "const int a = 0;"));
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(unit);

    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSizeOfList(2, members);
    validate(true, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) members.get(1)).getVariables());
  }

  public void test_computeValues_empty() {
    ConstantValueComputer computer = makeConstantValueComputer();
    computer.computeValues();
  }

  public void test_computeValues_multipleSources() throws Exception {
    Source librarySource = addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "const int c = b;",
        "const int a = 0;"));
    Source partSource = addNamedSource("/part.dart", createSource(//
        "part of lib;",
        "const int b = a;",
        "const int d = c;"));
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit libraryUnit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(libraryUnit);
    CompilationUnit partUnit = getAnalysisContext().resolveCompilationUnit(
        partSource,
        libraryElement);
    assertNotNull(partUnit);

    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(libraryUnit);
    computer.add(partUnit);
    computer.computeValues();

    NodeList<CompilationUnitMember> libraryMembers = libraryUnit.getDeclarations();
    assertSizeOfList(2, libraryMembers);
    validate(true, ((TopLevelVariableDeclaration) libraryMembers.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) libraryMembers.get(1)).getVariables());

    NodeList<CompilationUnitMember> partMembers = libraryUnit.getDeclarations();
    assertSizeOfList(2, partMembers);
    validate(true, ((TopLevelVariableDeclaration) partMembers.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) partMembers.get(1)).getVariables());
  }

  public void test_computeValues_singleVariable() throws Exception {
    Source librarySource = addSource("const int a = 0;");
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(unit);

    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSizeOfList(1, members);
    validate(true, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
  }

  public void test_dependencyOnConstructor() throws Exception {
    // x depends on "const A()"
    assertProperDependencies(createSource(//
        "class A {",
        "  const A();",
        "}",
        "const x = const A();"));
  }

  public void test_dependencyOnConstructorArgument() throws Exception {
    // "const A(x)" depends on x
    assertProperDependencies(createSource(//
        "class A {",
        "  const A(this.next);",
        "  final A next;",
        "}",
        "const A x = const A(null);",
        "const A y = const A(x);"));
  }

  public void test_dependencyOnConstructorArgument_unresolvedConstructor() throws Exception {
    // "const A.a(x)" depends on x even if the constructor A.a can't be found.
    // TODO(paulberry): the error CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE is redundant and
    // probably shouldn't be issued.
    assertProperDependencies(
        createSource(//
            "class A {",
            "}",
            "const int x = 1;",
            "const A y = const A.a(x);"),
        CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE,
        CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR);
  }

  public void test_dependencyOnConstructorInitializer() throws Exception {
    // "const A()" depends on x
    assertProperDependencies(createSource(//
        "const int x = 1;",
        "class A {",
        "  const A() : v = x;",
        "  final int v;",
        "}"));
  }

  public void test_dependencyOnVariable() throws Exception {
    // x depends on y
    assertProperDependencies(createSource(//
        "const x = y + 1;",
        "const y = 2;"));
  }

  public void test_instanceCreationExpression_redirect() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  const factory A() = B;",
        "}",
        "class B implements A {",
        "  const B();",
        "}"));
    assertType(evaluateInstanceCreationExpression(compilationUnit, "foo"), "B");
  }

  public void test_instanceCreationExpression_redirect_cycle() throws Exception {
    // It is an error to have a cycle in factory redirects; however, we need
    // to make sure that even if the error occurs, attempting to evaluate the
    // constant will terminate.
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  const factory A() = A.b;",
        "  const factory A.b() = A;",
        "}"));
    assertValidUnknown(evaluateInstanceCreationExpression(compilationUnit, "foo"));
  }

  public void test_instanceCreationExpression_redirect_extern() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  external const factory A();",
        "}"));
    assertValidUnknown(evaluateInstanceCreationExpression(compilationUnit, "foo"));
  }

  public void test_instanceCreationExpression_redirect_nonConst() throws Exception {
    // It is an error for a const factory constructor redirect to a non-const
    // constructor; however, we need to make sure that even if the error
    // attempting to evaluate the constant won't cause a crash.
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  const factory A() = A.b;",
        "  A.b();",
        "}"));
    assertValidUnknown(evaluateInstanceCreationExpression(compilationUnit, "foo"));
  }

  public void test_instanceCreationExpression_symbol() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource("const foo = const Symbol('a');"));
    EvaluationResultImpl evaluationResult = evaluateInstanceCreationExpression(
        compilationUnit,
        "foo");
    assertInstanceOf(ValidResult.class, evaluationResult);
    DartObjectImpl value = ((ValidResult) evaluationResult).getValue();
    assertEquals(getTypeProvider().getSymbolType(), value.getType());
    assertEquals("a", value.getValue());
  }

  private void assertProperDependencies(String sourceText, ErrorCode... expectedErrorCodes)
      throws AnalysisException {
    Source source = addSource(sourceText);
    LibraryElement element = resolve(source);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(source, element);
    assertNotNull(unit);
    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    assertErrors(source, expectedErrorCodes);
  }

  private void assertType(EvaluationResultImpl result, String typeName) {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertEquals(typeName, value.getType().getName());
  }

  private void assertValidUnknown(EvaluationResultImpl result) {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertTrue(value.isUnknown());
  }

  private EvaluationResultImpl evaluateInstanceCreationExpression(CompilationUnit compilationUnit,
      String name) throws AnalysisException {
    Expression expression = findTopLevelConstantExpression(compilationUnit, name);
    return ((InstanceCreationExpression) expression).getEvaluationResult();
  }

  private ConstantValueComputer makeConstantValueComputer() {
    return new ValidatingConstantValueComputer(new TestTypeProvider());
  }

  private void validate(boolean shouldBeValid, VariableDeclarationList declarationList) {
    for (VariableDeclaration declaration : declarationList.getVariables()) {
      VariableElementImpl element = (VariableElementImpl) declaration.getElement();
      assertNotNull(element);
      EvaluationResultImpl result = element.getEvaluationResult();
      if (shouldBeValid) {
        assertInstanceOf(ValidResult.class, result);
        Object value = ((ValidResult) result).getValue();
        assertNotNull(value);
      } else {
        assertInstanceOf(ErrorResult.class, result);
      }
    }
  }
}

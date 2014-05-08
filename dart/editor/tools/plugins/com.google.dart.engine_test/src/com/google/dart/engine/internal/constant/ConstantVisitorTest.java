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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.ast.AstFactory.booleanLiteral;
import static com.google.dart.engine.ast.AstFactory.conditionalExpression;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.AstFactory.integer;
import static com.google.dart.engine.ast.AstFactory.nullLiteral;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.library;

public class ConstantVisitorTest extends ResolverTestCase {
  public void test_visitConditionalExpression_false() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(false),
        thenExpression,
        elseExpression);
    assertValue(0L, expression.accept(new ConstantVisitor(new TestTypeProvider())));
  }

  public void test_visitConditionalExpression_instanceCreation_invalidFieldInitializer() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    LibraryElementImpl libraryElement = library(null, "lib");
    String className = "C";
    ClassElementImpl classElement = classElement(className);
    ((CompilationUnitElementImpl) libraryElement.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classElement});
    ConstructorElementImpl constructorElement = constructorElement(
        classElement,
        null,
        true,
        typeProvider.getIntType());
    constructorElement.getParameters()[0] = new FieldFormalParameterElementImpl(identifier("x"));
    InstanceCreationExpression expression = instanceCreationExpression(
        Keyword.CONST,
        typeName(className),
        integer(0L));
    expression.setStaticElement(constructorElement);
    expression.accept(new ConstantVisitor(typeProvider));
  }

  public void test_visitConditionalExpression_nonBooleanCondition() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        nullLiteral(),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor(new TestTypeProvider()));
    assertInstanceOf(ErrorResult.class, result);
  }

  public void test_visitConditionalExpression_nonConstantElse() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = identifier("x");
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor(new TestTypeProvider()));
    assertInstanceOf(ErrorResult.class, result);
  }

  public void test_visitConditionalExpression_nonConstantThen() {
    Expression thenExpression = identifier("x");
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor(new TestTypeProvider()));
    assertInstanceOf(ErrorResult.class, result);
  }

  public void test_visitConditionalExpression_true() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    assertValue(1L, expression.accept(new ConstantVisitor(new TestTypeProvider())));
  }

  public void test_visitInstanceCreationExpression_redirect() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  const factory A() = B;",
        "}",
        "class B implements A {",
        "  const B();",
        "}"));
    assertType(evaluateConstant(compilationUnit, "foo"), "B");
  }

  public void test_visitInstanceCreationExpression_redirect_cycle() throws Exception {
    // It is an error to have a cycle in factory redirects; however, we need
    // to make sure that even if the error occurs, attempting to evaluate the
    // constant will terminate.
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  const factory A() = A.b;",
        "  const factory A.b() = A;",
        "}"));
    assertValidUnknown(evaluateConstant(compilationUnit, "foo"));
  }

  public void test_visitInstanceCreationExpression_redirect_extern() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  external const factory A();",
        "}"));
    assertValidUnknown(evaluateConstant(compilationUnit, "foo"));
  }

  public void test_visitInstanceCreationExpression_redirect_nonConst() throws Exception {
    // It is an error for a const factory constructor redirect to a non-const
    // constructor; however, we need to make sure that even if the error
    // attempting to evaluate the constant won't cause a crash.
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A();",
        "class A {",
        "  const factory A() = A.b;",
        "  A.b();",
        "}"));
    assertValidUnknown(evaluateConstant(compilationUnit, "foo"));
  }

  public void test_visitInstanceCreationExpression_symbol() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource("const foo = const Symbol('a');"));
    EvaluationResultImpl evaluationResult = evaluateConstant(compilationUnit, "foo");
    assertInstanceOf(ValidResult.class, evaluationResult);
    DartObjectImpl value = ((ValidResult) evaluationResult).getValue();
    assertEquals(getTypeProvider().getSymbolType(), value.getType());
    assertEquals("a", value.getValue());
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

  private void assertValue(long expectedValue, EvaluationResultImpl result) {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertEquals("int", value.getType().getName());
    assertEquals(expectedValue, value.getIntValue().longValue());
  }

  private EvaluationResultImpl evaluateConstant(CompilationUnit compilationUnit, String name)
      throws AnalysisException {
    Expression expression = findTopLevelConstantExpression(compilationUnit, name);
    return expression.accept(new ConstantVisitor(getTypeProvider()));
  }

  private Expression findTopLevelConstantExpression(CompilationUnit compilationUnit, String name) {
    for (CompilationUnitMember member : compilationUnit.getDeclarations()) {
      if (member instanceof TopLevelVariableDeclaration) {
        for (VariableDeclaration variable : ((TopLevelVariableDeclaration) member).getVariables().getVariables()) {
          if (variable.getName().getName().equals(name)) {
            return variable.getInitializer();
          }
        }
      }
    }
    return null; // Not found
  }

  private CompilationUnit resolveSource(String sourceText) throws AnalysisException {
    Source source = addSource(sourceText);
    LibraryElement library = getAnalysisContext().computeLibraryElement(source);
    return getAnalysisContext().resolveCompilationUnit(source, library);
  }
}

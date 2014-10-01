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
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.IntState;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.source.NonExistingSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;

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

import java.math.BigInteger;
import java.util.HashMap;

public class ConstantVisitorTest extends ResolverTestCase {
  public void test_visitConditionalExpression_false() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(false),
        thenExpression,
        elseExpression);
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ErrorReporter errorReporter = new ErrorReporter(errorListener, dummySource());
    assertValue(0L, expression.accept(new ConstantVisitor(new TestTypeProvider(), errorReporter)));
    errorListener.assertNoErrors();
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
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ErrorReporter errorReporter = new ErrorReporter(errorListener, dummySource());
    expression.accept(new ConstantVisitor(typeProvider, errorReporter));
    errorListener.assertErrorsWithCodes(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  public void test_visitConditionalExpression_nonBooleanCondition() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    NullLiteral conditionExpression = nullLiteral();
    ConditionalExpression expression = conditionalExpression(
        conditionExpression,
        thenExpression,
        elseExpression);
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ErrorReporter errorReporter = new ErrorReporter(errorListener, dummySource());
    DartObjectImpl result = expression.accept(new ConstantVisitor(
        new TestTypeProvider(),
        errorReporter));
    assertNull(result);
    errorListener.assertErrorsWithCodes(CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL);
  }

  public void test_visitConditionalExpression_nonConstantElse() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = identifier("x");
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ErrorReporter errorReporter = new ErrorReporter(errorListener, dummySource());
    DartObjectImpl result = expression.accept(new ConstantVisitor(
        new TestTypeProvider(),
        errorReporter));
    assertNull(result);
    errorListener.assertErrorsWithCodes(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  public void test_visitConditionalExpression_nonConstantThen() {
    Expression thenExpression = identifier("x");
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ErrorReporter errorReporter = new ErrorReporter(errorListener, dummySource());
    DartObjectImpl result = expression.accept(new ConstantVisitor(
        new TestTypeProvider(),
        errorReporter));
    assertNull(result);
    errorListener.assertErrorsWithCodes(CompileTimeErrorCode.INVALID_CONSTANT);
  }

  public void test_visitConditionalExpression_true() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ErrorReporter errorReporter = new ErrorReporter(errorListener, dummySource());
    assertValue(1L, expression.accept(new ConstantVisitor(new TestTypeProvider(), errorReporter)));
    errorListener.assertNoErrors();
  }

  private NonExistingSource dummySource() {
    return new NonExistingSource(
        "foo.dart",
        UriKind.FILE_URI);
  }

  public void test_visitSimpleIdentifier_inEnvironment() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const a = b;",
        "const b = 3;"));
    HashMap<String, DartObjectImpl> environment = new HashMap<String, DartObjectImpl>();
    DartObjectImpl six = new DartObjectImpl(getTypeProvider().getIntType(), new IntState(
        BigInteger.valueOf(6L)));
    environment.put("b", six);
    assertValue(6, evaluateConstant(compilationUnit, "a", environment));
  }

  public void test_visitSimpleIdentifier_notInEnvironment() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const a = b;",
        "const b = 3;"));
    HashMap<String, DartObjectImpl> environment = new HashMap<String, DartObjectImpl>();
    DartObjectImpl six = new DartObjectImpl(getTypeProvider().getIntType(), new IntState(
        BigInteger.valueOf(6L)));
    environment.put("c", six);
    assertValue(3, evaluateConstant(compilationUnit, "a", environment));
  }

  public void test_visitSimpleIdentifier_withoutEnvironment() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const a = b;",
        "const b = 3;"));
    assertValue(3, evaluateConstant(compilationUnit, "a", null));
  }

  private void assertValue(long expectedValue, DartObjectImpl result) {
    assertNotNull(result);
    assertEquals("int", result.getType().getName());
    assertEquals(expectedValue, result.getIntValue().longValue());
  }

  private DartObjectImpl evaluateConstant(CompilationUnit compilationUnit, String name,
      HashMap<String, DartObjectImpl> lexicalEnvironment) throws AnalysisException {
    Source source = compilationUnit.getElement().getSource();
    Expression expression = findTopLevelConstantExpression(compilationUnit, name);
    GatheringErrorListener errorListener = new GatheringErrorListener();
    ErrorReporter errorReporter = new ErrorReporter(errorListener, source);
    DartObjectImpl result = expression.accept(new ConstantVisitor(
        getTypeProvider(),
        lexicalEnvironment,
        errorReporter));
    errorListener.assertNoErrors();
    return result;
  }
}

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
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.constant.DeclaredVariables;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.GenericState;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.collection.DirectedGraph;
import com.google.dart.engine.utilities.logging.TestLogger;

import java.util.HashMap;

public class ConstantValueComputerTest extends ResolverTestCase {
  class ValidatingConstantVisitor extends ConstantVisitor {
    private DirectedGraph<AstNode> referenceGraph;
    private AstNode nodeBeingEvaluated;

    public ValidatingConstantVisitor(TypeProvider typeProvider,
        DirectedGraph<AstNode> referenceGraph, AstNode nodeBeingEvaluated) {
      super(typeProvider);
      this.referenceGraph = referenceGraph;
      this.nodeBeingEvaluated = nodeBeingEvaluated;
    }

    @Override
    protected void beforeGetEvaluationResult(AstNode node) {
      super.beforeGetEvaluationResult(node);

      // If we are getting the evaluation result for a node in the graph, make sure we properly
      // recorded the dependency.
      if (referenceGraph.getNodes().contains(node)) {
        assertTrue(referenceGraph.containsPath(nodeBeingEvaluated, node));
      }
    }
  }

  private class ValidatingConstantValueComputer extends ConstantValueComputer {
    private AstNode nodeBeingEvaluated;

    public ValidatingConstantValueComputer(TypeProvider typeProvider,
        DeclaredVariables declaredVariables) {
      super(typeProvider, declaredVariables);
    }

    @Override
    protected void beforeComputeValue(AstNode constNode) {
      super.beforeComputeValue(constNode);
      nodeBeingEvaluated = constNode;
    }

    @Override
    protected void beforeGetConstantInitializers(ConstructorElement constructor) {
      super.beforeGetConstantInitializers(constructor);

      // If we are getting the constant initializers for a node in the graph, make sure we properly
      // recorded the dependency.
      ConstructorDeclaration node = findConstructorDeclaration(constructor);
      if (node != null && referenceGraph.getNodes().contains(node)) {
        assertTrue(referenceGraph.containsPath(nodeBeingEvaluated, node));
      }
    }

    @Override
    protected void beforeGetParameterDefault(ParameterElement parameter) {
      super.beforeGetParameterDefault(parameter);

      // Find the ConstructorElement and figure out which parameter we're talking about.
      ConstructorElement constructor = parameter.getAncestor(ConstructorElement.class);
      int parameterIndex;
      ParameterElement[] parameters = constructor.getParameters();
      int numParameters = parameters.length;
      for (parameterIndex = 0; parameterIndex < numParameters; parameterIndex++) {
        if (parameters[parameterIndex] == parameter) {
          break;
        }
      }
      assertTrue(parameterIndex < numParameters);

      // If we are getting the default parameter for a constructor in the graph, make sure we properly
      // recorded the dependency on the parameter.
      ConstructorDeclaration constructorNode = constructorDeclarationMap.get(constructor);
      if (constructorNode != null) {
        FormalParameter parameterNode = constructorNode.getParameters().getParameters().get(
            parameterIndex);
        assertTrue(referenceGraph.getNodes().contains(parameterNode));
        assertTrue(referenceGraph.containsPath(nodeBeingEvaluated, parameterNode));
      }
    }

    @Override
    protected ConstantVisitor createConstantVisitor() {
      return new ValidatingConstantVisitor(typeProvider, referenceGraph, nodeBeingEvaluated);
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

  public void test_dependencyOnExplicitSuperConstructor() throws Exception {
    // b depends on B() depends on A()
    assertProperDependencies(createSource(//
        "class A {",
        "  const A(this.x);",
        "  final int x;",
        "}",
        "class B extends A {",
        "  const B() : super(5);",
        "}",
        "const B b = const B();"));
  }

  public void test_dependencyOnExplicitSuperConstructorParameters() throws Exception {
    // b depends on B() depends on i
    assertProperDependencies(createSource(//
        "class A {",
        "  const A(this.x);",
        "  final int x;",
        "}",
        "class B extends A {",
        "  const B() : super(i);",
        "}",
        "const B b = const B();",
        "const int i = 5;"));
  }

  public void test_dependencyOnFactoryRedirect() throws Exception {
    // a depends on A.foo() depends on A.bar()
    assertProperDependencies(createSource(//
        "const A a = const A.foo();",
        "class A {",
        "  factory const A.foo() = A.bar;",
        "  const A.bar();",
        "}"));
  }

  public void test_dependencyOnFactoryRedirectWithTypeParams() throws Exception {
    assertProperDependencies(createSource(//
        "class A {",
        "  const factory A(var a) = B<int>;",
        "}",
        "",
        "class B<T> implements A {",
        "  final T x;",
        "  const B(this.x);",
        "}",
        "",
        "const A a = const A(10);"));
  }

  public void test_dependencyOnImplicitSuperConstructor() throws Exception {
    // b depends on B() depends on A()
    assertProperDependencies(createSource(//
        "class A {",
        "  const A() : x = 5;",
        "  final int x;",
        "}",
        "class B extends A {",
        "  const B();",
        "}",
        "const B b = const B();"));
  }

  public void test_dependencyOnOptionalParameterDefault() throws Exception {
    // a depends on A() depends on B()
    assertProperDependencies(createSource(//
        "class A {",
        "  const A([x = const B()]) : b = x;",
        "  final B b;",
        "}",
        "class B {",
        "  const B();",
        "}",
        "const A a = const A();"));
  }

  public void test_dependencyOnVariable() throws Exception {
    // x depends on y
    assertProperDependencies(createSource(//
        "const x = y + 1;",
        "const y = 2;"));
  }

  public void test_fromEnvironment_bool_default_false() throws Exception {
    assertEquals(false, assertValidBool(check_fromEnvironment_bool(null, "false")));
  }

  public void test_fromEnvironment_bool_default_overridden() throws Exception {
    assertEquals(false, assertValidBool(check_fromEnvironment_bool("false", "true")));
  }

  public void test_fromEnvironment_bool_default_parseError() throws Exception {
    assertEquals(true, assertValidBool(check_fromEnvironment_bool("parseError", "true")));
  }

  public void test_fromEnvironment_bool_default_true() throws Exception {
    assertEquals(true, assertValidBool(check_fromEnvironment_bool(null, "true")));
  }

  public void test_fromEnvironment_bool_false() throws Exception {
    assertEquals(false, assertValidBool(check_fromEnvironment_bool("false", null)));
  }

  public void test_fromEnvironment_bool_parseError() throws Exception {
    assertEquals(false, assertValidBool(check_fromEnvironment_bool("parseError", null)));
  }

  public void test_fromEnvironment_bool_true() throws Exception {
    assertEquals(true, assertValidBool(check_fromEnvironment_bool("true", null)));
  }

  public void test_fromEnvironment_bool_undeclared() throws Exception {
    assertValidUnknown(check_fromEnvironment_bool(null, null));
  }

  public void test_fromEnvironment_int_default_overridden() throws Exception {
    assertEquals(234, assertValidInt(check_fromEnvironment_int("234", "123")));
  }

  public void test_fromEnvironment_int_default_parseError() throws Exception {
    assertEquals(123, assertValidInt(check_fromEnvironment_int("parseError", "123")));
  }

  public void test_fromEnvironment_int_default_undeclared() throws Exception {
    assertEquals(123, assertValidInt(check_fromEnvironment_int(null, "123")));
  }

  public void test_fromEnvironment_int_ok() throws Exception {
    assertEquals(234, assertValidInt(check_fromEnvironment_int("234", null)));
  }

  public void test_fromEnvironment_int_parseError() throws Exception {
    assertValidNull(check_fromEnvironment_int("parseError", null));
  }

  public void test_fromEnvironment_int_parseError_nullDefault() throws Exception {
    assertValidNull(check_fromEnvironment_int("parseError", "null"));
  }

  public void test_fromEnvironment_int_undeclared() throws Exception {
    assertValidUnknown(check_fromEnvironment_int(null, null));
  }

  public void test_fromEnvironment_int_undeclared_nullDefault() throws Exception {
    assertValidNull(check_fromEnvironment_int(null, "null"));
  }

  public void test_fromEnvironment_string_default_overridden() throws Exception {
    assertEquals("abc", assertValidString(check_fromEnvironment_string("abc", "'def'")));
  }

  public void test_fromEnvironment_string_default_undeclared() throws Exception {
    assertEquals("def", assertValidString(check_fromEnvironment_string(null, "'def'")));
  }

  public void test_fromEnvironment_string_empty() throws Exception {
    assertEquals("", assertValidString(check_fromEnvironment_string("", null)));
  }

  public void test_fromEnvironment_string_ok() throws Exception {
    assertEquals("abc", assertValidString(check_fromEnvironment_string("abc", null)));
  }

  public void test_fromEnvironment_string_undeclared() throws Exception {
    assertValidUnknown(check_fromEnvironment_string(null, null));
  }

  public void test_fromEnvironment_string_undeclared_nullDefault() throws Exception {
    assertValidNull(check_fromEnvironment_string(null, "null"));
  }

  public void test_instanceCreationExpression_computedField() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A(4, 5);",
        "class A {",
        "  const A(int i, int j) : k = 2 * i + j;",
        "  final int k;",
        "}"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "foo");
    HashMap<String, DartObjectImpl> fields = assertType(result, "A");
    assertSizeOfMap(1, fields);
    assertIntField(fields, "k", 13L);
  }

  public void test_instanceCreationExpression_computedField_namedOptionalWithDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(false, true, true);
  }

  public void test_instanceCreationExpression_computedField_namedOptionalWithoutDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(false, true, false);
  }

  public void test_instanceCreationExpression_computedField_unnamedOptionalWithDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(false, false, true);
  }

  public void test_instanceCreationExpression_computedField_unnamedOptionalWithoutDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(false, false, false);
  }

  public void test_instanceCreationExpression_computedField_usesConstConstructor() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A(3);",
        "class A {",
        "  const A(int i) : b = const B(4);",
        "  final int b;",
        "}",
        "class B {",
        "  const B(this.k);",
        "  final int k;",
        "}"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "foo");
    HashMap<String, DartObjectImpl> fieldsOfA = assertType(result, "A");
    assertSizeOfMap(1, fieldsOfA);
    HashMap<String, DartObjectImpl> fieldsOfB = assertFieldType(fieldsOfA, "b", "B");
    assertSizeOfMap(1, fieldsOfB);
    assertIntField(fieldsOfB, "k", 4L);
  }

  public void test_instanceCreationExpression_computedField_usesStaticConst() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A(3);",
        "class A {",
        "  const A(int i) : k = i + B.bar;",
        "  final int k;",
        "}",
        "class B {",
        "  static const bar = 4;",
        "}"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "foo");
    HashMap<String, DartObjectImpl> fields = assertType(result, "A");
    assertSizeOfMap(1, fields);
    assertIntField(fields, "k", 7L);
  }

  public void test_instanceCreationExpression_computedField_usesToplevelConst() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A(3);",
        "const bar = 4;",
        "class A {",
        "  const A(int i) : k = i + bar;",
        "  final int k;",
        "}"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "foo");
    HashMap<String, DartObjectImpl> fields = assertType(result, "A");
    assertSizeOfMap(1, fields);
    assertIntField(fields, "k", 7L);
  }

  public void test_instanceCreationExpression_explicitSuper() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const B(4, 5);",
        "class A {",
        "  const A(this.x);",
        "  final int x;",
        "}",
        "class B extends A {",
        "  const B(int x, this.y) : super(x * 2);",
        "  final int y;",
        "}"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "foo");
    HashMap<String, DartObjectImpl> fields = assertType(result, "B");
    assertSizeOfMap(2, fields);
    assertIntField(fields, "y", 5L);
    HashMap<String, DartObjectImpl> superclassFields = assertFieldType(
        fields,
        GenericState.SUPERCLASS_FIELD,
        "A");
    assertSizeOfMap(1, superclassFields);
    assertIntField(superclassFields, "x", 8L);
  }

  public void test_instanceCreationExpression_fieldFormalParameter() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const A(42);",
        "class A {",
        "  int x;",
        "  const A(this.x)",
        "}"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "foo");
    HashMap<String, DartObjectImpl> fields = assertType(result, "A");
    assertSizeOfMap(1, fields);
    assertIntField(fields, "x", 42L);
  }

  public void test_instanceCreationExpression_fieldFormalParameter_namedOptionalWithDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(true, true, true);
  }

  public void test_instanceCreationExpression_fieldFormalParameter_namedOptionalWithoutDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(true, true, false);
  }

  public void test_instanceCreationExpression_fieldFormalParameter_unnamedOptionalWithDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(true, false, true);
  }

  public void test_instanceCreationExpression_fieldFormalParameter_unnamedOptionalWithoutDefault()
      throws Exception {
    checkInstanceCreationOptionalParams(true, false, false);
  }

  public void test_instanceCreationExpression_implicitSuper() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const foo = const B(4);",
        "class A {",
        "  const A() : x(3);",
        "  final int x;",
        "}",
        "class B extends A {",
        "  const B(this.y);",
        "  final int y;",
        "}"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "foo");
    HashMap<String, DartObjectImpl> fields = assertType(result, "B");
    assertSizeOfMap(2, fields);
    assertIntField(fields, "y", 4L);
    HashMap<String, DartObjectImpl> superclassFields = assertFieldType(
        fields,
        GenericState.SUPERCLASS_FIELD,
        "A");
    assertSizeOfMap(1, superclassFields);
    assertIntField(superclassFields, "x", 3L);
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

  public void test_instanceCreationExpression_redirectWithTypeParams() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "class A {",
        "  const factory A(var a) = B<int>;",
        "}",
        "",
        "class B<T> implements A {",
        "  final T x;",
        "  const B(this.x);",
        "}",
        "",
        "const A a = const A(10);"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "a");
    HashMap<String, DartObjectImpl> fields = assertType(result, "B<int>");
    assertSizeOfMap(1, fields);
    assertIntField(fields, "x", 10L);
  }

  public void test_instanceCreationExpression_redirectWithTypeSubstitution() throws Exception {
    // To evaluate the redirection of A<int>, A's template argument (T=int) must be substituted
    // into B's template argument (B<U> where U=T) to get B<int>. 
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "class A<T> {",
        "  const factory A(var a) = B<T>;",
        "}",
        "",
        "class B<U> implements A {",
        "  final U x;",
        "  const B(this.x);",
        "}",
        "",
        "const A<int> a = const A<int>(10);"));
    EvaluationResultImpl result = evaluateInstanceCreationExpression(compilationUnit, "a");
    HashMap<String, DartObjectImpl> fields = assertType(result, "B<int>");
    assertSizeOfMap(1, fields);
    assertIntField(fields, "x", 10L);
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

  public void test_instanceCreationExpression_withSupertypeParams_explicit() throws Exception {
    checkInstanceCreation_withSupertypeParams(true);
  }

  public void test_instanceCreationExpression_withSupertypeParams_implicit() throws Exception {
    checkInstanceCreation_withSupertypeParams(false);
  }

  public void test_instanceCreationExpression_withTypeParams() throws Exception {
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "class C<E> {",
        "  const C();",
        "}",
        "const c_int = const C<int>();",
        "const c_num = const C<num>();"));
    EvaluationResultImpl c_int = evaluateInstanceCreationExpression(compilationUnit, "c_int");
    assertType(c_int, "C<int>");
    DartObjectImpl c_int_value = ((ValidResult) c_int).getValue();
    EvaluationResultImpl c_num = evaluateInstanceCreationExpression(compilationUnit, "c_num");
    assertType(c_num, "C<num>");
    DartObjectImpl c_num_value = ((ValidResult) c_num).getValue();
    assertFalse(c_int_value.equals(c_num_value));
  }

  private HashMap<String, DartObjectImpl> assertFieldType(HashMap<String, DartObjectImpl> fields,
      String fieldName, String expectedType) {
    DartObjectImpl field = fields.get(fieldName);
    assertEquals(expectedType, field.getType().getDisplayName());
    return field.getFields();
  }

  private void assertIntField(HashMap<String, DartObjectImpl> fields, String fieldName,
      long expectedValue) {
    DartObjectImpl field = fields.get(fieldName);
    assertEquals("int", field.getType().getName());
    assertEquals(expectedValue, field.getIntValue().longValue());
  }

  private void assertNullField(HashMap<String, DartObjectImpl> fields, String fieldName) {
    DartObjectImpl field = fields.get(fieldName);
    assertTrue(field.isNull());
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

  private HashMap<String, DartObjectImpl> assertType(EvaluationResultImpl result, String typeName) {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertEquals(typeName, value.getType().getDisplayName());
    return value.getFields();
  }

  private boolean assertValidBool(EvaluationResultImpl result) throws AnalysisException {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertEquals(getTypeProvider().getBoolType(), value.getType());
    Boolean boolValue = value.getBoolValue();
    assertNotNull(boolValue);
    return boolValue;
  }

  private int assertValidInt(EvaluationResultImpl result) throws AnalysisException {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertEquals(getTypeProvider().getIntType(), value.getType());
    return value.getIntValue().intValue();
  }

  private void assertValidNull(EvaluationResultImpl result) throws AnalysisException {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertEquals(getTypeProvider().getNullType(), value.getType());
  }

  private String assertValidString(EvaluationResultImpl result) throws AnalysisException {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertEquals(getTypeProvider().getStringType(), value.getType());
    return value.getStringValue();
  }

  private void assertValidUnknown(EvaluationResultImpl result) {
    assertInstanceOf(ValidResult.class, result);
    DartObjectImpl value = ((ValidResult) result).getValue();
    assertTrue(value.isUnknown());
  }

  private EvaluationResultImpl check_fromEnvironment_bool(String valueInEnvironment,
      String defaultExpr) throws AnalysisException {
    String envVarName = "x";
    String varName = "foo";
    if (valueInEnvironment != null) {
      analysisContext.getDeclaredVariables().define(envVarName, valueInEnvironment);
    }
    String defaultArg = defaultExpr == null ? "" : ", defaultValue: " + defaultExpr;
    CompilationUnit compilationUnit = resolveSource(createSource(//
    "const " + varName + " = const bool.fromEnvironment('" + envVarName + "'" + defaultArg + ");"));
    return evaluateInstanceCreationExpression(compilationUnit, varName);
  }

  private EvaluationResultImpl check_fromEnvironment_int(String valueInEnvironment,
      String defaultExpr) throws AnalysisException {
    String envVarName = "x";
    String varName = "foo";
    if (valueInEnvironment != null) {
      analysisContext.getDeclaredVariables().define(envVarName, valueInEnvironment);
    }
    String defaultArg = defaultExpr == null ? "" : ", defaultValue: " + defaultExpr;
    CompilationUnit compilationUnit = resolveSource(createSource(//
    "const " + varName + " = const int.fromEnvironment('" + envVarName + "'" + defaultArg + ");"));
    return evaluateInstanceCreationExpression(compilationUnit, varName);
  }

  private EvaluationResultImpl check_fromEnvironment_string(String valueInEnvironment,
      String defaultExpr) throws AnalysisException {
    String envVarName = "x";
    String varName = "foo";
    if (valueInEnvironment != null) {
      analysisContext.getDeclaredVariables().define(envVarName, valueInEnvironment);
    }
    String defaultArg = defaultExpr == null ? "" : ", defaultValue: " + defaultExpr;
    CompilationUnit compilationUnit = resolveSource(createSource(//
    "const " + varName + " = const String.fromEnvironment('" + envVarName + "'" + defaultArg + ");"));
    return evaluateInstanceCreationExpression(compilationUnit, varName);
  }

  private void checkInstanceCreation_withSupertypeParams(boolean isExplicit)
      throws AnalysisException {
    String superCall = isExplicit ? " : super()" : "";
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "class A<T> {",
        "  const A();",
        "}",
        "class B<T, U> extends A<T> {",
        "  const B()" + superCall + ";",
        "}",
        "class C<T, U> extends A<U> {",
        "  const C()" + superCall + ";",
        "}",
        "const b_int_num = const B<int, num>();",
        "const c_int_num = const C<int, num>();"));
    EvaluationResultImpl b_int_num = evaluateInstanceCreationExpression(
        compilationUnit,
        "b_int_num");
    HashMap<String, DartObjectImpl> b_int_num_fields = assertType(b_int_num, "B<int, num>");
    assertFieldType(b_int_num_fields, GenericState.SUPERCLASS_FIELD, "A<int>");
    EvaluationResultImpl c_int_num = evaluateInstanceCreationExpression(
        compilationUnit,
        "c_int_num");
    HashMap<String, DartObjectImpl> c_int_num_fields = assertType(c_int_num, "C<int, num>");
    assertFieldType(c_int_num_fields, GenericState.SUPERCLASS_FIELD, "A<num>");
  }

  private void checkInstanceCreationOptionalParams(boolean isFieldFormal, boolean isNamed,
      boolean hasDefault) throws AnalysisException {
    String fieldName = "j";
    String paramName = isFieldFormal ? fieldName : "i";
    String formalParam = (isFieldFormal ? "this." : "int ") + paramName
        + (hasDefault ? " = 3" : "");
    CompilationUnit compilationUnit = resolveSource(createSource(//
        "const x = const A();",
        "const y = const A(" + (isNamed ? paramName + ": " : "") + "10);",
        "class A {",
        "  const A(" + (isNamed ? "{" + formalParam + "}" : "[" + formalParam + "]") + ")"
            + (isFieldFormal ? "" : " : " + fieldName + " = " + paramName) + ";",
        "  final int " + fieldName + ";",
        "}"));
    EvaluationResultImpl x = evaluateInstanceCreationExpression(compilationUnit, "x");
    HashMap<String, DartObjectImpl> fieldsOfX = assertType(x, "A");
    assertSizeOfMap(1, fieldsOfX);
    if (hasDefault) {
      assertIntField(fieldsOfX, fieldName, 3L);
    } else {
      assertNullField(fieldsOfX, fieldName);
    }
    EvaluationResultImpl y = evaluateInstanceCreationExpression(compilationUnit, "y");
    HashMap<String, DartObjectImpl> fieldsOfY = assertType(y, "A");
    assertSizeOfMap(1, fieldsOfY);
    assertIntField(fieldsOfY, fieldName, 10L);
  }

  private EvaluationResultImpl evaluateInstanceCreationExpression(CompilationUnit compilationUnit,
      String name) throws AnalysisException {
    Expression expression = findTopLevelConstantExpression(compilationUnit, name);
    return ((InstanceCreationExpression) expression).getEvaluationResult();
  }

  private ConstantValueComputer makeConstantValueComputer() {
    return new ValidatingConstantValueComputer(
        new TestTypeProvider(),
        analysisContext.getDeclaredVariables());
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

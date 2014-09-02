/*
 * Copyright (c) 2012, the Dart project authors.
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

import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;

public class SimpleResolverTest extends ResolverTestCase {
  public void fail_staticInvocation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int get g => (a,b) => 0;",
        "}",
        "class B {",
        "  f() {",
        "    A.g(1,0);",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_argumentResolution_required_matching() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, 3);",
        "  }",
        "  void g(a, b, c) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, 2);
  }

  public void test_argumentResolution_required_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2);",
        "  }",
        "  void g(a, b, c) {}",
        "}"));
    validateArgumentResolution(source, 0, 1);
  }

  public void test_argumentResolution_required_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, 3);",
        "  }",
        "  void g(a, b) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, -1);
  }

  public void test_argumentResolution_requiredAndNamed_extra() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, c: 3, d: 4);",
        "  }",
        "  void g(a, b, {c}) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, 2, -1);
  }

  public void test_argumentResolution_requiredAndNamed_matching() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, c: 3);",
        "  }",
        "  void g(a, b, {c}) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, 2);
  }

  public void test_argumentResolution_requiredAndNamed_missing() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, d: 3);",
        "  }",
        "  void g(a, b, {c, d}) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, 3);
  }

  public void test_argumentResolution_requiredAndPositional_fewer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, 3);",
        "  }",
        "  void g(a, b, [c, d]) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, 2);
  }

  public void test_argumentResolution_requiredAndPositional_matching() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, 3, 4);",
        "  }",
        "  void g(a, b, [c, d]) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, 2, 3);
  }

  public void test_argumentResolution_requiredAndPositional_more() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void f() {",
        "    g(1, 2, 3, 4);",
        "  }",
        "  void g(a, b, [c]) {}",
        "}"));
    validateArgumentResolution(source, 0, 1, 2, -1);
  }

  public void test_argumentResolution_setter_propagated() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  var a = new A();",
        "  a.sss = 0;",
        "}",
        "class A {",
        "  set sss(x) {}",
        "}"));
    LibraryElement library = resolve(source);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    // find "a.sss = 0"
    AssignmentExpression assignment;
    {
      FunctionElement mainElement = unit.getFunctions()[0];
      FunctionBody mainBody = mainElement.getNode().getFunctionExpression().getBody();
      Statement statement = ((BlockFunctionBody) mainBody).getBlock().getStatements().get(1);
      ExpressionStatement expressionStatement = (ExpressionStatement) statement;
      assignment = (AssignmentExpression) expressionStatement.getExpression();
    }
    // get parameter
    Expression rhs = assignment.getRightHandSide();
    assertNull(rhs.getStaticParameterElement());
    ParameterElement parameter = rhs.getPropagatedParameterElement();
    assertNotNull(parameter);
    assertEquals("x", parameter.getDisplayName());
    // validate
    ClassElement classA = unit.getTypes()[0];
    PropertyAccessorElement setter = classA.getAccessors()[0];
    assertSame(parameter, setter.getParameters()[0]);
  }

  public void test_argumentResolution_setter_propagated_propertyAccess() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  var a = new A();",
        "  a.b.sss = 0;",
        "}",
        "class A {",
        "  B b = new B();",
        "}",
        "class B {",
        "  set sss(x) {}",
        "}"));
    LibraryElement library = resolve(source);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    // find "a.b.sss = 0"
    AssignmentExpression assignment;
    {
      FunctionElement mainElement = unit.getFunctions()[0];
      FunctionBody mainBody = mainElement.getNode().getFunctionExpression().getBody();
      Statement statement = ((BlockFunctionBody) mainBody).getBlock().getStatements().get(1);
      ExpressionStatement expressionStatement = (ExpressionStatement) statement;
      assignment = (AssignmentExpression) expressionStatement.getExpression();
    }
    // get parameter
    Expression rhs = assignment.getRightHandSide();
    assertNull(rhs.getStaticParameterElement());
    ParameterElement parameter = rhs.getPropagatedParameterElement();
    assertNotNull(parameter);
    assertEquals("x", parameter.getDisplayName());
    // validate
    ClassElement classB = unit.getTypes()[1];
    PropertyAccessorElement setter = classB.getAccessors()[0];
    assertSame(parameter, setter.getParameters()[0]);
  }

  public void test_argumentResolution_setter_static() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  A a = new A();",
        "  a.sss = 0;",
        "}",
        "class A {",
        "  set sss(x) {}",
        "}"));
    LibraryElement library = resolve(source);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    // find "a.sss = 0"
    AssignmentExpression assignment;
    {
      FunctionElement mainElement = unit.getFunctions()[0];
      FunctionBody mainBody = mainElement.getNode().getFunctionExpression().getBody();
      Statement statement = ((BlockFunctionBody) mainBody).getBlock().getStatements().get(1);
      ExpressionStatement expressionStatement = (ExpressionStatement) statement;
      assignment = (AssignmentExpression) expressionStatement.getExpression();
    }
    // get parameter
    Expression rhs = assignment.getRightHandSide();
    assertNull(rhs.getPropagatedParameterElement());
    ParameterElement parameter = rhs.getStaticParameterElement();
    assertNotNull(parameter);
    assertEquals("x", parameter.getDisplayName());
    // validate
    ClassElement classA = unit.getTypes()[0];
    PropertyAccessorElement setter = classA.getAccessors()[0];
    assertSame(parameter, setter.getParameters()[0]);
  }

  public void test_argumentResolution_setter_static_propertyAccess() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  A a = new A();",
        "  a.b.sss = 0;",
        "}",
        "class A {",
        "  B b = new B();",
        "}",
        "class B {",
        "  set sss(x) {}",
        "}"));
    LibraryElement library = resolve(source);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    // find "a.b.sss = 0"
    AssignmentExpression assignment;
    {
      FunctionElement mainElement = unit.getFunctions()[0];
      FunctionBody mainBody = mainElement.getNode().getFunctionExpression().getBody();
      Statement statement = ((BlockFunctionBody) mainBody).getBlock().getStatements().get(1);
      ExpressionStatement expressionStatement = (ExpressionStatement) statement;
      assignment = (AssignmentExpression) expressionStatement.getExpression();
    }
    // get parameter
    Expression rhs = assignment.getRightHandSide();
    assertNull(rhs.getPropagatedParameterElement());
    ParameterElement parameter = rhs.getStaticParameterElement();
    assertNotNull(parameter);
    assertEquals("x", parameter.getDisplayName());
    // validate
    ClassElement classB = unit.getTypes()[1];
    PropertyAccessorElement setter = classB.getAccessors()[0];
    assertSame(parameter, setter.getParameters()[0]);
  }

  public void test_class_definesCall() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int call(int x) { return x; }",
        "}",
        "int f(A a) {",
        "  return a(0);",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_class_extends_implements() throws Exception {
    Source source = addSource(createSource(//
        "class A extends B implements C {}",
        "class B {}",
        "class C {}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_commentReference_class() throws Exception {
    Source source = addSource(createSource(//
        "f() {}",
        "/** [A] [new A] [A.n] [new A.n] [m] [f] */",
        "class A {",
        "  A() {}",
        "  A.n() {}",
        "  m() {}",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_commentReference_parameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "  A.n() {}",
        "  /** [e] [f] */",
        "  m(e, f()) {}",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_commentReference_singleLine() throws Exception {
    Source source = addSource(createSource(//
        "/// [A]",
        "class A {}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_empty() throws Exception {
    Source source = addSource("");
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_entryPoint_exported() throws Exception {
    addNamedSource("/two.dart", createSource(//
        "library two;",
        "main() {}"));
    Source source = addNamedSource("/one.dart", createSource(//
        "library one;",
        "export 'two.dart';"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    FunctionElement main = library.getEntryPoint();
    assertNotNull(main);
    assertNotSame(library, main.getLibrary());
    assertNoErrors(source);
    verify(source);
  }

  public void test_entryPoint_local() throws Exception {
    Source source = addNamedSource("/one.dart", createSource(//
        "library one;",
        "main() {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    FunctionElement main = library.getEntryPoint();
    assertNotNull(main);
    assertSame(library, main.getLibrary());
    assertNoErrors(source);
    verify(source);
  }

  public void test_entryPoint_none() throws Exception {
    Source source = addNamedSource("/one.dart", createSource(//
        "library one;"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    assertNull(library.getEntryPoint());
    assertNoErrors(source);
    verify(source);
  }

  public void test_extractedMethodAsConstant() throws Exception {
    Source source = addSource(createSource(//
        "abstract class Comparable<T> {",
        "  int compareTo(T other);",
        "  static int compare(Comparable a, Comparable b) => a.compareTo(b);",
        "}",
        "class A {",
        "  void sort([compare = Comparable.compare]) {}",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_fieldFormalParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_forEachLoops_nonConflicting() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  List list = [1,2,3];",
        "  for (int x in list) {}",
        "  for (int x in list) {}",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_forLoops_nonConflicting() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  for (int i = 0; i < 3; i++) {",
        "  }",
        "  for (int i = 0; i < 3; i++) {",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_functionTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "typedef bool P(e);",
        "class A {",
        "  P p;",
        "  m(e) {",
        "    if (p(e)) {}",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_getterAndSetterWithDifferentTypes() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get f => 0;",
        "  void set f(String s) {}",
        "}",
        "g (A a) {",
        "  a.f = a.f.toString();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void test_hasReferenceToSuper() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {toString() => super.toString();}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(2, classes);
    assertFalse(classes[0].hasReferenceToSuper());
    assertTrue(classes[1].hasReferenceToSuper());
    assertNoErrors(source);
    verify(source);
  }

  public void test_import_hide() throws Exception {
    addNamedSource("lib1.dart", createSource(//
        "library lib1;",
        "set foo(value) {}",
        "class A {}"));
    addNamedSource("lib2.dart", createSource(//
        "library lib2;",
        "set foo(value) {}"));
    Source source = addNamedSource("lib3.dart", createSource(//
        "import 'lib1.dart' hide foo;",
        "import 'lib2.dart';",
        "",
        "main() {",
        "  foo = 0;",
        "}",
        "A a;"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_import_prefix() throws Exception {
    addNamedSource("/two.dart", createSource(//
        "library two;",
        "f(int x) {",
        "  return x * x;",
        "}"));
    Source source = addNamedSource("/one.dart", createSource(//
        "library one;",
        "import 'two.dart' as _two;",
        "main() {",
        "  _two.f(0);",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_import_spaceInUri() throws Exception {
    addNamedSource("sub folder/lib.dart", createSource(//
        "library lib;",
        "foo() {}"));
    Source source = addNamedSource("app.dart", createSource(//
        "import 'sub folder/lib.dart';",
        "",
        "main() {",
        "  foo();",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_indexExpression_typeParameters() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  List<int> a;",
        "  a[0];",
        "  List<List<int>> b;",
        "  b[0][0];",
        "  List<List<List<int>>> c;",
        "  c[0][0][0];",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_indexExpression_typeParameters_invalidAssignmentWarning() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  List<List<int>> b;",
        "  b[0][0] = 'hi';",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_indirectOperatorThroughCall() throws Exception {
    Source source = addSource(createSource(//
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
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_invoke_dynamicThroughGetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  List get X => [() => 0];",
        "  m(A a) {",
        "    X.last;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_isValidMixin_badSuperclass() throws Exception {
    Source source = addSource(createSource(//
        "class A extends B {}",
        "class B {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(2, classes);
    assertFalse(classes[0].isValidMixin());
    assertNoErrors(source);
    verify(source);
  }

  public void test_isValidMixin_constructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    assertFalse(classes[0].isValidMixin());
    assertNoErrors(source);
    verify(source);
  }

  public void test_isValidMixin_super() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  toString() {",
        "    return super.toString();",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    assertFalse(classes[0].isValidMixin());
    assertNoErrors(source);
    verify(source);
  }

  public void test_isValidMixin_valid() throws Exception {
    Source source = addSource(createSource(//
    "class A {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    assertTrue(classes[0].isValidMixin());
    assertNoErrors(source);
    verify(source);
  }

  public void test_labels_switch() throws Exception {
    Source source = addSource(createSource(//
        "void doSwitch(int target) {",
        "  switch (target) {",
        "    l0: case 0:",
        "      continue l1;",
        "    l1: case 1:",
        "      continue l0;",
        "    default:",
        "      continue l1;",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    assertNoErrors(source);
    verify(source);
  }

  public void test_localVariable_types_invoked() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "main() {",
        "  var myVar = (int p) => 'foo';",
        "  myVar(42);",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnit unit = getAnalysisContext().getResolvedCompilationUnit(source, library);
    assertNotNull(unit);
    final boolean[] found = {false};
    final AnalysisException[] thrownException = new AnalysisException[1];
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        if (node.getName().equals("myVar") && node.getParent() instanceof MethodInvocation) {
          try {
            found[0] = true;
            // check static type
            Type staticType = node.getStaticType();
            assertSame(getTypeProvider().getDynamicType(), staticType);
            // check propagated type
            FunctionType propagatedType = (FunctionType) node.getPropagatedType();
            assertEquals(getTypeProvider().getStringType(), propagatedType.getReturnType());
          } catch (AnalysisException e) {
            thrownException[0] = e;
          }
        }
        return null;
      }
    });
    if (thrownException[0] != null) {
      throw new AnalysisException("Exception", thrownException[0]);
    }
    assertTrue(found[0]);
  }

  public void test_metadata_class() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "@A class C<A> {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unitElement = library.getDefiningCompilationUnit();
    assertNotNull(unitElement);
    ClassElement[] classes = unitElement.getTypes();
    assertLength(1, classes);
    ElementAnnotation[] annotations = classes[0].getMetadata();
    assertLength(1, annotations);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(2, declarations);
    Element expectedElement = ((TopLevelVariableDeclaration) declarations.get(0)).getVariables().getVariables().get(
        0).getName().getStaticElement();
    assertInstanceOf(PropertyInducingElement.class, expectedElement);
    expectedElement = ((PropertyInducingElement) expectedElement).getGetter();
    Element actualElement = ((ClassDeclaration) declarations.get(1)).getMetadata().get(0).getName().getStaticElement();
    assertSame(expectedElement, actualElement);
  }

  public void test_metadata_field() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "class C {",
        "  @A int f;",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    FieldElement field = classes[0].getFields()[0];
    ElementAnnotation[] annotations = field.getMetadata();
    assertLength(1, annotations);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_fieldFormalParameter() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "class C {",
        "  int f;",
        "  C(@A this.f);",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    ConstructorElement[] constructors = classes[0].getConstructors();
    assertLength(1, constructors);
    ParameterElement[] parameters = constructors[0].getParameters();
    assertLength(1, parameters);
    ElementAnnotation[] annotations = parameters[0].getMetadata();
    assertLength(1, annotations);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_function() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "@A f() {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    FunctionElement[] functions = unit.getFunctions();
    assertLength(1, functions);
    ElementAnnotation[] annotations = functions[0].getMetadata();
    assertLength(1, annotations);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_functionTypedParameter() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "f(@A int p(int x)) {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    FunctionElement[] functions = unit.getFunctions();
    assertLength(1, functions);
    ParameterElement[] parameters = functions[0].getParameters();
    assertLength(1, parameters);
    ElementAnnotation[] annotations1 = parameters[0].getMetadata();
    assertLength(1, annotations1);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_libraryDirective() throws Exception {
    Source source = addSource(createSource(//
        "@A library lib;",
        "const A = null;"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    ElementAnnotation[] annotations = library.getMetadata();
    assertLength(1, annotations);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_method() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "class C {",
        "  @A void m() {}",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    MethodElement method = classes[0].getMethods()[0];
    ElementAnnotation[] annotations = method.getMetadata();
    assertLength(1, annotations);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_namedParameter() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "f({@A int p : 0}) {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    FunctionElement[] functions = unit.getFunctions();
    assertLength(1, functions);
    ParameterElement[] parameters = functions[0].getParameters();
    assertLength(1, parameters);
    ElementAnnotation[] annotations1 = parameters[0].getMetadata();
    assertLength(1, annotations1);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_positionalParameter() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "f([@A int p = 0]) {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    FunctionElement[] functions = unit.getFunctions();
    assertLength(1, functions);
    ParameterElement[] parameters = functions[0].getParameters();
    assertLength(1, parameters);
    ElementAnnotation[] annotations1 = parameters[0].getMetadata();
    assertLength(1, annotations1);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_simpleParameter() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "f(@A p1, @A int p2) {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    FunctionElement[] functions = unit.getFunctions();
    assertLength(1, functions);
    ParameterElement[] parameters = functions[0].getParameters();
    assertLength(2, parameters);
    ElementAnnotation[] annotations1 = parameters[0].getMetadata();
    assertLength(1, annotations1);
    ElementAnnotation[] annotations2 = parameters[1].getMetadata();
    assertLength(1, annotations2);
    assertNoErrors(source);
    verify(source);
  }

  public void test_metadata_typedef() throws Exception {
    Source source = addSource(createSource(//
        "const A = null;",
        "@A typedef F<A>();"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unitElement = library.getDefiningCompilationUnit();
    assertNotNull(unitElement);
    FunctionTypeAliasElement[] aliases = unitElement.getFunctionTypeAliases();
    assertLength(1, aliases);
    ElementAnnotation[] annotations = aliases[0].getMetadata();
    assertLength(1, annotations);
    assertNoErrors(source);
    verify(source);
    CompilationUnit unit = resolveCompilationUnit(source, library);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(2, declarations);
    Element expectedElement = ((TopLevelVariableDeclaration) declarations.get(0)).getVariables().getVariables().get(
        0).getName().getStaticElement();
    assertInstanceOf(PropertyInducingElement.class, expectedElement);
    expectedElement = ((PropertyInducingElement) expectedElement).getGetter();
    Element actualElement = ((FunctionTypeAlias) declarations.get(1)).getMetadata().get(0).getName().getStaticElement();
    assertSame(expectedElement, actualElement);
  }

  public void test_method_fromMixin() throws Exception {
    Source source = addSource(createSource(//
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
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_method_fromSuperclassMixin() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void m1() {}",
        "}",
        "class B extends Object with A {",
        "}",
        "class C extends B {",
        "}",
        "f(C c) {",
        "  c.m1();",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_methodCascades() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void m1() {}",
        "  void m2() {}",
        "  void m() {",
        "    A a = new A();",
        "    a..m1()",
        "     ..m2();",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_methodCascades_withSetter() throws Exception {
    Source source = addSource(createSource(//
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
        "}"));
    resolve(source);
    // failing with error code: INVOCATION_OF_NON_FUNCTION
    assertNoErrors(source);
    verify(source);
  }

  public void test_resolveAgainstNull() throws Exception {
    Source source = addSource(createSource(//
        "f(var p) {",
        "  return null == p;",
        "}"));
    resolve(source);
    assertNoErrors(source);
  }

  public void test_setter_inherited() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get x => 0;",
        "  set x(int p) {}",
        "}",
        "class B extends A {",
        "  int get x => super.x == null ? 0 : super.x;",
        "  int f() => x = 1;",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_setter_static() throws Exception {
    Source source = addSource(createSource(//
        "set s(x) {",
        "}",
        "",
        "main() {",
        "  s = 123;",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  /**
   * Resolve the given source and verify that the arguments in a specific method invocation were
   * correctly resolved.
   * <p>
   * The source is expected to be source for a compilation unit, the first declaration is expected
   * to be a class, the first member of which is expected to be a method with a block body, and the
   * first statement in the body is expected to be an expression statement whose expression is a
   * method invocation. It is the arguments to that method invocation that are tested. The method
   * invocation can contain errors.
   * <p>
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
  private void validateArgumentResolution(Source source, int... indices) throws Exception {
    LibraryElement library = resolve(source);
    assertNotNull(library);
    ClassElement classElement = library.getDefiningCompilationUnit().getTypes()[0];
    ParameterElement[] parameters = classElement.getMethods()[1].getParameters();

    CompilationUnit unit = resolveCompilationUnit(source, library);
    assertNotNull(unit);
    ClassDeclaration classDeclaration = (ClassDeclaration) unit.getDeclarations().get(0);
    MethodDeclaration methodDeclaration = ((MethodDeclaration) classDeclaration.getMembers().get(0));
    Block block = ((BlockFunctionBody) methodDeclaration.getBody()).getBlock();
    ExpressionStatement statement = (ExpressionStatement) block.getStatements().get(0);
    MethodInvocation invocation = (MethodInvocation) statement.getExpression();
    NodeList<Expression> arguments = invocation.getArgumentList().getArguments();

    int argumentCount = arguments.size();
    assertEquals(indices.length, argumentCount);
    for (int i = 0; i < argumentCount; i++) {
      Expression argument = arguments.get(i);
      ParameterElement element = argument.getStaticParameterElement();
      int index = indices[i];
      if (index < 0) {
        assertNull(element);
      } else {
        assertSame(parameters[index], element);
      }
    }
  }
}

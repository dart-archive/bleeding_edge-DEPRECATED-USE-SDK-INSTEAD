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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.LocationWithData;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.index.IndexContributorHelper.ExpectedLocation;
import com.google.dart.engine.internal.index.IndexContributorHelper.RecordedRelation;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.internal.index.IndexContributorHelper.assertLocation;
import static com.google.dart.engine.internal.index.IndexContributorHelper.assertNoRecordedRelation;
import static com.google.dart.engine.internal.index.IndexContributorHelper.assertRecordedRelation;
import static com.google.dart.engine.internal.index.IndexContributorHelper.assertRecordedRelations;
import static com.google.dart.engine.internal.index.IndexContributorHelper.captureRelations;
import static com.google.dart.engine.internal.index.IndexContributorHelper.mockElement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

public class IndexContributorTest extends AbstractDartTest {
  private static void assertDefinesTopLevelElement(List<RecordedRelation> recordedRelations,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    assertRecordedRelation(
        recordedRelations,
        expectedElement,
        expectedRelationship,
        expectedLocation);
    assertRecordedRelation(
        recordedRelations,
        IndexConstants.UNIVERSE,
        expectedRelationship,
        expectedLocation);
  }

  private IndexStore store = mock(IndexStore.class);
  private IndexContributor index = new IndexContributor(store);

  public void test_createElementLocation() throws Exception {
    ElementLocation elementLocation = mock(ElementLocation.class);
    Element element = mockElement(Element.class, elementLocation, 42, "myName");
    Location location = IndexContributor.createLocation(element);
    assertLocation(location, element, 42, "myName");
  }

  public void test_createElementLocation_null() throws Exception {
    assertSame(null, IndexContributor.createLocation(null));
  }

  public void test_definesClass() throws Exception {
    parseTestUnit("class A {}");
    // prepare elements
    ClassElement classElementA = findElement("A {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        testLibraryElement,
        IndexConstants.DEFINES_CLASS,
        new ExpectedLocation(classElementA, findOffset("A {}"), "A"));
  }

  public void test_definesClassAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Mix {}",
        "class MyClass = Object with Mix;");
    Element classElement = findElement("MyClass =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        testLibraryElement,
        IndexConstants.DEFINES_CLASS_ALIAS,
        new ExpectedLocation(classElement, findOffset("MyClass ="), "MyClass"));
    assertNoRecordedRelation(relations, classElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_definesFunction() throws Exception {
    parseTestUnit("myFunction() {}");
    FunctionElement functionElement = findElement("myFunction() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        testLibraryElement,
        IndexConstants.DEFINES_FUNCTION,
        new ExpectedLocation(functionElement, findOffset("myFunction() {}"), "myFunction"));
    assertNoRecordedRelation(relations, functionElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_definesFunctionType() throws Exception {
    parseTestUnit("typedef MyFunction(int p);");
    FunctionTypeAliasElement typeAliasElement = findElement("MyFunction");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        testLibraryElement,
        IndexConstants.DEFINES_FUNCTION_TYPE,
        new ExpectedLocation(typeAliasElement, findOffset("MyFunction"), "MyFunction"));
    assertNoRecordedRelation(relations, typeAliasElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_definesVariable() throws Exception {
    parseTestUnit("var myVar;");
    VariableElement varElement = findElement("myVar");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        testLibraryElement,
        IndexConstants.DEFINES_VARIABLE,
        new ExpectedLocation(varElement, findOffset("myVar"), "myVar"));
    assertNoRecordedRelation(relations, varElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  @SuppressWarnings("unchecked")
  public void test_FieldElement_assignedTypes_assignment_qualifed() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "}",
        "main() {",
        "  new A().myField = 1;",
        "  A a = new A();",
        "  a.myField = 2.0;",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    FieldElement fieldElement = findElement("myField;");
    PropertyAccessorElement setterElement = fieldElement.getSetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    {
      Location location = assertRecordedRelation(
          relations,
          setterElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          new ExpectedLocation(mainElement, findOffset("myField = 1"), "myField"));
      Type type = ((LocationWithData<Type>) location).getData();
      assertEquals("int", type.toString());
    }
    {
      Location location = assertRecordedRelation(
          relations,
          setterElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          new ExpectedLocation(mainElement, findOffset("myField = 2.0"), "myField"));
      Type type = ((LocationWithData<Type>) location).getData();
      assertEquals("double", type.toString());
    }
  }

  @SuppressWarnings("unchecked")
  public void test_FieldElement_assignedTypes_assignment_unqualifed() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "  main() {",
        "    myField = 1;",
        "    myField = 2.0;",
        "    myField = '3';",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    FieldElement fieldElement = findElement("myField;");
    PropertyAccessorElement setterElement = fieldElement.getSetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    {
      Location location = assertRecordedRelation(
          relations,
          setterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          new ExpectedLocation(mainElement, findOffset("myField = 1"), "myField"));
      Type type = ((LocationWithData<Type>) location).getData();
      assertEquals("int", type.toString());
    }
    {
      Location location = assertRecordedRelation(
          relations,
          setterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          new ExpectedLocation(mainElement, findOffset("myField = 2.0"), "myField"));
      Type type = ((LocationWithData<Type>) location).getData();
      assertEquals("double", type.toString());
    }
    {
      Location location = assertRecordedRelation(
          relations,
          setterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          new ExpectedLocation(mainElement, findOffset("myField = '3'"), "myField"));
      Type type = ((LocationWithData<Type>) location).getData();
      assertEquals("String", type.toString());
    }
  }

  @SuppressWarnings("unchecked")
  public void test_FieldElement_assignedTypes_initializer() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField = 1;",
        "  A() : myField = 2.0;",
        "}");
    // set elements
    Element classElement = findElement("A {");
    Element constructorElement = findNode("A() :", ConstructorDeclaration.class).getElement();
    FieldElement fieldElement = findElement("myField = 1;");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    {
      Location location = assertRecordedRelation(
          relations,
          fieldElement,
          IndexConstants.IS_DEFINED_BY,
          new ExpectedLocation(classElement, findOffset("myField = 1"), "myField"));
      Type type = ((LocationWithData<Type>) location).getData();
      assertEquals("int", type.toString());
    }
    {
      Location location = assertRecordedRelation(
          relations,
          fieldElement,
          IndexConstants.IS_REFERENCED_BY,
          new ExpectedLocation(constructorElement, findOffset("myField = 2.0"), "myField"));
      Type type = ((LocationWithData<Type>) location).getData();
      assertEquals("double", type.toString());
    }
  }

  public void test_FieldElement_noAssignedType_notLHS() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "  main() {",
        "    print(myField);",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    FieldElement fieldElement = findElement("myField;");
    PropertyAccessorElement getterElement = fieldElement.getGetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    {
      Location location = assertRecordedRelation(
          relations,
          getterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          new ExpectedLocation(mainElement, findOffset("myField);"), "myField"));
      assertSame(Location.class, location.getClass());
    }
  }

  public void test_forIn() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for (var v in []) {",
        "  }",
        "}");
    // prepare elements
    Element mainElement = findElement("main(");
    VariableElement variableElement = findElement("v in []");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertNoRecordedRelation(
        relations,
        variableElement,
        IndexConstants.IS_READ_BY,
        new ExpectedLocation(mainElement, findOffset("v in []"), "v"));
  }

  public void test_isDefinedBy_ConstructorElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {}",
        "  A.foo() {} ",
        "}",
        "");
    // set elements
    ClassElement classA = findElement("A {");
    ConstructorElement consA = findNode("A()", ConstructorDeclaration.class).getElement();
    ConstructorElement consA_foo = findNode("A.foo()", ConstructorDeclaration.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(relations, consA, IndexConstants.IS_DEFINED_BY, new ExpectedLocation(
        classA,
        findOffset("() {}"),
        ""));
    assertRecordedRelation(
        relations,
        consA_foo,
        IndexConstants.IS_DEFINED_BY,
        new ExpectedLocation(classA, findOffset(".foo() {}"), ".foo"));
  }

  public void test_isDefinedBy_NameElement_method() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  m() {}",
        "}");
    // prepare elements
    Element methodElement = findElement("m() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("m");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_DEFINED_BY,
        new ExpectedLocation(methodElement, findOffset("m() {}"), "m"));
  }

  public void test_isDefinedBy_NameElement_operator() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator +(o) {}",
        "}");
    // prepare elements
    Element methodElement = findElement("+(o) {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("+");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_DEFINED_BY,
        new ExpectedLocation(methodElement, findOffset("+(o) {}"), "+"));
  }

  public void test_isExtendedBy_ClassDeclaration() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B extends A {} // 2",
        "");
    // prepare elements
    ClassElement classElementA = findElement("A {} // 1");
    ClassElement classElementB = findElement("B extends");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_EXTENDED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementB, findOffset("A {} // 2"), "A"));
  }

  public void test_isExtendedBy_ClassDeclaration_Object() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "");
    // prepare elements
    ClassElement classElementA = findElement("A {} // 1");
    ClassElement classElementObject = classElementA.getSupertype().getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementObject,
        IndexConstants.IS_EXTENDED_BY,
        new ExpectedLocation(classElementA, findOffset("A {} // 1"), ""));
  }

  public void test_isExtendedBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B {} // 2",
        "class C = A with B; // 3",
        "");
    // prepare elements
    ClassElement classElementA = findElement("A {} // 1");
    ClassElement classElementC = findElement("C =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_EXTENDED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementC, findOffset("A with"), "A"));
  }

  public void test_isImplementedBy_ClassDeclaration() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B implements A {} // 2",
        "");
    // prepare elements
    ClassElement classElementA = findElement("A {} // 1");
    ClassElement classElementB = findElement("B implements");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_IMPLEMENTED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementB, findOffset("A {} // 2"), "A"));
  }

  public void test_isImplementedBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B {} // 2",
        "class C = Object with A implements B; // 3",
        "");
    // prepare elements
    ClassElement classElementB = findElement("B {} // 2");
    ClassElement classElementC = findElement("C =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementB,
        IndexConstants.IS_IMPLEMENTED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementC, findOffset("B; // 3"), "B"));
  }

  public void test_isInvokedBy_FunctionElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() {}",
        "main() {",
        "  foo();",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    FunctionElement referencedElement = findElement("foo() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_INVOKED_BY,
        new ExpectedLocation(mainElement, findOffset("foo();"), "foo"));
  }

  public void test_isInvokedBy_LocalVariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v();",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    LocalVariableElement referencedElement = findElement("v;");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_INVOKED_BY,
        new ExpectedLocation(mainElement, findOffset("v();"), "v"));
  }

  public void test_isInvokedBy_ParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main(p()) {",
        "  p();",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    ParameterElement referencedElement = findElement("p()) {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_INVOKED_BY,
        new ExpectedLocation(mainElement, findOffset("p();"), "p"));
  }

  public void test_isInvokedByQualified_FieldElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var field;",
        "  main() {",
        "    this.field();",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    FieldElement fieldElement = findElement("field;");
    PropertyAccessorElement getterElement = fieldElement.getGetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        getterElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("field();"), "field"));
    assertNoRecordedRelation(
        relations,
        getterElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        null);
  }

  public void test_isInvokedByQualified_MethodElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  main() {",
        "    this.foo();",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    MethodElement fooElement = findElement("foo() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("foo();"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY_QUALIFIED, null);
  }

  public void test_isInvokedByQualified_MethodElement_propagatedType() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "}",
        "main() {",
        "  var a = new A();",
        "  a.foo();",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    MethodElement fooElement = findElement("foo() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("foo();"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY_QUALIFIED, null);
  }

  public void test_isInvokedByUnqualified_MethodElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  main() {",
        "    foo();",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    MethodElement fooElement = findElement("foo() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, findOffset("foo();"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isMixedInBy_ClassDeclaration() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B extends Object with A {} // 2",
        "");
    // prepare elements
    ClassElement classElementA = findElement("A {} // 1");
    ClassElement classElementB = findElement("B extends");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_MIXED_IN_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementB, findOffset("A {} // 2"), "A"));
  }

  public void test_isMixedInBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class C = Object with A; // 2",
        "");
    // prepare elements
    ClassElement classElementA = findElement("A {} // 1");
    ClassElement classElementC = findElement("C =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_MIXED_IN_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementC, findOffset("A; // 2"), "A"));
  }

  public void test_isReadBy_ParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main2(var p) {",
        "  print(p);",
        "}");
    // prepare elements
    Element mainElement = findElement("main2(");
    ParameterElement parameterElement = findElement("p) {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        parameterElement,
        IndexConstants.IS_READ_BY,
        new ExpectedLocation(mainElement, findOffset("p);"), "p"));
  }

  public void test_isReadBy_VariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 0;",
        "  print(v);",
        "}");
    // prepare elements
    Element mainElement = findElement("main(");
    VariableElement variableElement = findElement("v = 0");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        variableElement,
        IndexConstants.IS_READ_BY,
        new ExpectedLocation(mainElement, findOffset("v);"), "v"));
  }

  public void test_isReadWrittenBy_ParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main2(var p) {",
        "  p += 1;",
        "}");
    // prepare elements
    Element mainElement = findElement("main2(");
    ParameterElement parameterElement = findElement("p) {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        parameterElement,
        IndexConstants.IS_READ_WRITTEN_BY,
        new ExpectedLocation(mainElement, findOffset("p += 1"), "p"));
  }

  public void test_isReadWrittenBy_VariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 0;",
        "  v += 1;",
        "}");
    // prepare elements
    Element mainElement = findElement("main(");
    VariableElement variableElement = findElement("v = 0");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        variableElement,
        IndexConstants.IS_READ_WRITTEN_BY,
        new ExpectedLocation(mainElement, findOffset("v += 1"), "v"));
  }

  public void test_isReferencedBy_ClassElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var field;",
        "}",
        "topLevelFunction(A p) {",
        "  A v;",
        "  new A(); // 2",
        "  A.field = 1;",
        "  print(A.field); // 3",
        "}");
    // prepare elements
    Element functionElement = findElement("topLevelFunction(");
    VariableElement vElement = findElement("v;");
    ParameterElement pElement = findElement("p) {");
    ClassElement classElementA = findElement("A {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(pElement, findOffset("A p)"), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(vElement, findOffset("A v"), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, findOffset("A(); // 2"), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, findOffset("A.field ="), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, findOffset("A.field); // 3"), "A"));
  }

  public void test_isReferencedBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "class B = Object with A;",
        "topLevelFunction(B p) {",
        "  B v;",
        "}");
    // prepare elements
    VariableElement vElement = findElement("v;");
    ClassElement classElementB = findElement("B =");
    ParameterElement pElement = findElement("p) {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(pElement, findOffset("B p)"), "B"));
    assertRecordedRelation(
        relations,
        classElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(vElement, findOffset("B v"), "B"));
  }

  public void test_isReferencedBy_CompilationUnitElement() throws Exception {
    setFileContent("SomeUnit.dart", "part of myLib;");
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library myLib;",
        "part 'SomeUnit.dart';",
        "");
    // set elements
    Element mainElement = testUnitElement;
    CompilationUnitElement referencedElement = (CompilationUnitElement) findNode(
        "part 'Some",
        PartDirective.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("'SomeUnit.dart'"), "'SomeUnit.dart'"));
  }

  public void test_isReferencedBy_ConstructorFieldInitializer() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int field;",
        "  A() : field = 5;",
        "}",
        "");
    // prepare elements
    Element constructorElement = findNode("A() :", ConstructorDeclaration.class).getElement();
    FieldElement fieldElement = findElement("field;");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(constructorElement, findOffset("field = 5"), "field"));
  }

  public void test_isReferencedBy_FieldFormalParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int field;",
        "  A(this.field) {}",
        "}",
        "");
    // prepare elements
    FieldElement fieldElement = findElement("field;");
    ParameterElement fieldParameterElement = findElement("field) {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(fieldParameterElement, findOffset("field) {}"), "field"));
  }

  public void test_isReferencedBy_FunctionElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() {}",
        "main() {",
        "  print(foo);",
        "  print(foo());",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    FunctionElement referencedElement = findElement("foo() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    // "referenced" here
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("foo);"), "foo"));
    // only "invoked", but not "referenced"
    {
      assertRecordedRelation(
          relations,
          referencedElement,
          IndexConstants.IS_INVOKED_BY,
          new ExpectedLocation(mainElement, findOffset("foo());"), "foo"));
      assertNoRecordedRelation(
          relations,
          referencedElement,
          IndexConstants.IS_REFERENCED_BY,
          new ExpectedLocation(mainElement, findOffset("foo());"), "foo"));
    }
  }

  public void test_isReferencedBy_FunctionTypeAliasElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef A();",
        "main2(A p) {",
        "}");
    // prepare elements
    ParameterElement pElement = findElement("p) {");
    FunctionTypeAliasElement classElementA = findElement("A();");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(pElement, findOffset("A p)"), "A"));
  }

  /**
   * There was bug in the AST structure, when single {@link Comment} was cloned and assigned to both
   * {@link FieldDeclaration} and {@link VariableDeclaration}. This caused duplicate indexing. Here
   * we test that this problem is fixed one way or another.
   */
  public void test_isReferencedBy_identifierInComment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "/// [A] text",
        "var myVariable = null;",
        "");
    // prepare elements
    Element classElementA = findElement("A {}");
    Element variableElement = findElement("myVariable =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(testUnitElement, findOffset("A] text"), "A"));
    assertNoRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(variableElement, findOffset("A] text"), "A"));
  }

  public void test_isReferencedBy_ImportElement_noPrefix() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "var myVar;",
            "myFunction() {}",
            "myToHide() {}"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' show myVar, myFunction hide myToHide;",
        "main() {",
        "  myVar = 1;",
        "  myFunction();",
        "  print(0);",
        "}",
        "");
    // set elements
    ImportElement importElement = findNode("import 'Lib.dart", ImportDirective.class).getElement();
    Element mainElement = findElement("main(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("myVar = 1"), ""));
    assertRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("myFunction();"), ""));
    assertNoRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("print(0);"), ""));
    // no references from import combinators
    assertNoRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(testUnitElement, findOffset("myVar, "), ""));
    assertNoRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(testUnitElement, findOffset("myFunction hide"), ""));
    assertNoRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(testUnitElement, findOffset("myToHide;"), ""));
  }

  public void test_isReferencedBy_ImportElement_withPrefix() throws Exception {
    setFileContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "var myVar;"));
    setFileContent(
        "LibB.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libB;",
            "class MyClass {}"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as pref;",
        "import 'LibB.dart' as pref;",
        "main() {",
        "  pref.myVar = 1;",
        "  new pref.MyClass();",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    ImportElement importElementA = findNode("import 'LibA.dart", ImportDirective.class).getElement();
    ImportElement importElementB = findNode("import 'LibB.dart", ImportDirective.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        importElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("pref.myVar"), "pref."));
    assertRecordedRelation(
        relations,
        importElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("pref.MyClass"), "pref."));
  }

  public void test_isReferencedBy_ImportElement_withPrefix_combinators() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "class A {}",
            "class B {}"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as pref show A;",
        "import 'Lib.dart' as pref show B;",
        "import 'Lib.dart';",
        "import 'Lib.dart' as otherPrefix;",
        "main() {",
        "  new pref.A();",
        "  new pref.B();",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    ImportElement importElementA = findNode(
        "import 'Lib.dart' as pref show A",
        ImportDirective.class).getElement();
    ImportElement importElementB = findNode(
        "import 'Lib.dart' as pref show B",
        ImportDirective.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        importElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("pref.A"), "pref."));
    assertRecordedRelation(
        relations,
        importElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("pref.B"), "pref."));
  }

  public void test_isReferencedBy_ImportElement_withPrefix_invocation() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "myFunc() {}"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as pref;",
        "main() {",
        "  pref.myFunc();",
        "}",
        "");
    // prepare elements
    Element mainElement = findElement("main(");
    ImportElement importElement = findNode("import 'Lib.dart", ImportDirective.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("pref.myFunc()"), "pref."));
  }

  public void test_isReferencedBy_ImportElement_withPrefix_oneCandidate() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "class A {}",
            "class B {}"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as pref show A;",
        "main() {",
        "  new pref.A();",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    ImportElement importElementA = findNode(
        "import 'Lib.dart' as pref show A",
        ImportDirective.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        importElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("pref.A"), "pref."));
  }

  public void test_isReferencedBy_ImportElement_withPrefix_unresolvedElement() throws Exception {
    verifyNoTestUnitErrors = false;
    setFileContent("Lib.dart", "library lib;");
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as pref;",
        "main() {",
        "  pref.myVar = 1;",
        "}",
        "");
    // index
    index.visitCompilationUnit(testUnit);
    // no exception
  }

  public void test_isReferencedBy_ImportElement_withPrefix_wrongInvocation() throws Exception {
    verifyNoTestUnitErrors = false;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as m;",
        "main() {",
        "  m();",
        "}",
        "");
    // index
    index.visitCompilationUnit(testUnit);
    // should be no exceptions
  }

  public void test_isReferencedBy_ImportElement_withPrefix_wrongPrefixedIdentifier()
      throws Exception {
    verifyNoTestUnitErrors = false;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as m;",
        "main() {",
        "  x.m;",
        "}",
        "");
    // index
    index.visitCompilationUnit(testUnit);
    // should be no exceptions
  }

  public void test_isReferencedBy_LabelElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  L: while(true) {",
        "    break L;",
        "  }",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    LabelElement referencedElement = findElement("L:");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("L;"), "L"));
  }

  public void test_isReferencedBy_LibraryElement_export() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "export 'Lib.dart';",
        "");
    // set elements
    Element mainElement = testUnitElement;
    LibraryElement referencedElement = findNode("export 'Lib.dart", ExportDirective.class).getElement().getExportedLibrary();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement.getDefiningCompilationUnit(),
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("'Lib.dart'"), "'Lib.dart'"));
  }

  public void test_isReferencedBy_LibraryElement_import() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart';",
        "");
    // set elements
    Element mainElement = testUnitElement;
    LibraryElement referencedElement = findNode("import 'Lib.dart", ImportDirective.class).getElement().getImportedLibrary();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement.getDefiningCompilationUnit(),
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("'Lib.dart'"), "'Lib.dart'"));
  }

  public void test_isReferencedBy_libraryName() throws Exception {
    String partCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of lib; // marker",
        "");
    Source partSource = addSource("/part.dart", partCode);
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library lib;",
        "part 'part.dart';",
        "");
    CompilationUnit partUnit = analysisContext.getResolvedCompilationUnit(
        partSource,
        testLibraryElement);
    // index
    index.visitCompilationUnit(partUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        testLibraryElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(partUnit.getElement(), partCode.indexOf("lib; // marker"), "lib"));
  }

  public void test_isReferencedBy_ParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo({var p}) {}",
        "main() {",
        "  foo(p: 1);",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    ParameterElement parameterElement = findElement("p}) {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        parameterElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("p: 1"), "p"));
  }

  public void test_isReferencedBy_typeInVariableList() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "A myVariable = null;",
        "");
    // prepare elements
    Element classElementA = findElement("A {}");
    Element variableElement = findElement("myVariable =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(variableElement, findOffset("A myVa"), "A"));
  }

  public void test_isReferencedBy_TypeParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<T> {",
        "  T f;",
        "  foo(T p) {",
        "    T v;",
        "  }",
        "}");
    // prepare elements
    VariableElement fieldElement = findElement("f;");
    TypeParameterElement typeParameterElement = findElement("T>");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        typeParameterElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(fieldElement, findOffset("T f"), "T"));
  }

  public void test_isReferencedByQualified_ConstructorElement() throws Exception {
    // Turn off verify of no errors since A implements B extends A
    verifyNoTestUnitErrors = false;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A implements B {",
        "  A() {}",
        "  A.foo() {}",
        "}",
        "class B extends A {",
        "  B() : super(); // marker-1",
        "  B.foo() : super.foo(); // marker-2",
        "  factory B.bar() = A.foo; // marker-3",
        "}",
        "main() {",
        "  new A(); // marker-main-1",
        "  new A.foo(); // marker-main-2",
        "}",
        "");
    verifyNoTestUnitErrors = true;
    // set elements
    Element mainElement = findElement("main() {");
    ConstructorElement consB = findNode("B()", ConstructorDeclaration.class).getElement();
    ConstructorElement consB_foo = findNode("B.foo()", ConstructorDeclaration.class).getElement();
    ConstructorElement consB_bar = findNode("B.bar()", ConstructorDeclaration.class).getElement();
    ConstructorElement consA = findNode("A()", ConstructorDeclaration.class).getElement();
    ConstructorElement consA_foo = findNode("A.foo()", ConstructorDeclaration.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    // A()
    assertRecordedRelation(relations, consA, IndexConstants.IS_REFERENCED_BY, new ExpectedLocation(
        consB,
        findOffset("(); // marker-1"),
        ""));
    assertRecordedRelation(relations, consA, IndexConstants.IS_REFERENCED_BY, new ExpectedLocation(
        mainElement,
        findOffset("(); // marker-main-1"),
        ""));
    // A.foo()
    assertRecordedRelation(
        relations,
        consA_foo,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(consB_foo, findOffset(".foo(); // marker-2"), ".foo"));
    assertRecordedRelation(
        relations,
        consA_foo,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(consB_bar, findOffset(".foo; // marker-3"), ".foo"));
    assertRecordedRelation(
        relations,
        consA_foo,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset(".foo(); // marker-main-2"), ".foo"));
  }

  public void test_isReferencedByQualified_ConstructorElement_classTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "class A implements B {",
        "  A() {}",
        "  A.named() {}",
        "}",
        "class B = A with M;",
        "main() {",
        "  new B(); // marker-main-1",
        "  new B.named(); // marker-main-2",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main() {");
    ConstructorElement consA = findNode("A()", ConstructorDeclaration.class).getElement();
    ConstructorElement consA_named = findNode("A.named()", ConstructorDeclaration.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(relations, consA, IndexConstants.IS_REFERENCED_BY, new ExpectedLocation(
        mainElement,
        findOffset("(); // marker-main-1"),
        ""));
    assertRecordedRelation(
        relations,
        consA_named,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset(".named(); // marker-main-2"), ".named"));
  }

  public void test_isReferencedByQualified_FieldElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var myField;",
        "}",
        "main() {",
        "  A.myField = 1;",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    FieldElement fieldElement = findElement("myField;");
    PropertyAccessorElement accessorElement = fieldElement.getSetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        accessorElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("myField = 1"), "myField"));
  }

  public void test_isReferencedByQualified_MethodElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  main() {",
        "    print(this.foo);",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    MethodElement fooElement = findElement("foo() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("foo);"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isReferencedByQualified_MethodElement_operator_binary() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator +(other) => this;",
        "}",
        "main(A a) {",
        "  print(a + 42);",
        "  a += 42;",
        "  ++a;",
        "  a++;",
        "}");
    // set elements
    Element mainElement = findElement("main(A a");
    MethodElement operatorElement = findElement("+(other");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        operatorElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("+ 42);"), "+"));
    assertRecordedRelation(
        relations,
        operatorElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("+= 42;"), "+="));
    assertRecordedRelation(
        relations,
        operatorElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("++a;"), "++"));
    assertRecordedRelation(
        relations,
        operatorElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("++;"), "++"));
  }

  public void test_isReferencedByQualified_MethodElement_operator_index() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator [](i) => null;",
        "  operator []=(i, v) {}",
        "}",
        "main(A a) {",
        "  print(a[0]);",
        "  a[1] = 42;",
        "}");
    // set elements
    Element mainElement = findElement("main(A a");
    MethodElement readElement = findElement("[]");
    MethodElement writeElement = findElement("[]=");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        readElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("[0]);"), "["));
    assertRecordedRelation(
        relations,
        writeElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("[1] = 42"), "["));
  }

  public void test_isReferencedByQualified_MethodElement_operator_prefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A operator ~() => this;",
        "}",
        "main(A a) {",
        "  print(~a);",
        "}");
    // set elements
    Element mainElement = findElement("main(A a");
    MethodElement operatorElement = findElement("~(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        operatorElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("~a);"), "~"));
    assertNoRecordedRelation(relations, operatorElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isReferencedByQualified_PropertyAccessorElement_method_getter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get foo => 42;",
        "  main() {",
        "    print(this.foo);",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    PropertyAccessorElement fooElement = findElement("foo => ");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("foo);"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isReferencedByQualified_PropertyAccessorElement_method_setter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  set foo(x) {}",
        "  main() {",
        "    this.foo = 42;",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    PropertyAccessorElement fooElement = findElement("foo(x) {}");
    findSimpleIdentifier("foo = 42").setStaticElement(fooElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("foo = 42"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isReferencedByQualified_PropertyAccessorElement_topLevelField() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "var myVar;"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as pref;",
        "main() {",
        "  pref.myVar = 1;",
        "  print(pref.myVar);",
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    ImportElement importElement = findNode("import 'Lib.dart", ImportDirective.class).getElement();
    CompilationUnitElement impUnit = importElement.getImportedLibrary().getDefiningCompilationUnit();
    TopLevelVariableElement myVar = impUnit.getTopLevelVariables()[0];
    PropertyAccessorElement getter = myVar.getGetter();
    PropertyAccessorElement setter = myVar.getSetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        setter,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("myVar ="), "myVar"));
    assertRecordedRelation(
        relations,
        getter,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, findOffset("myVar);"), "myVar"));
  }

  public void test_isReferencedByQualifiedResolved_NameElement_field() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int myField;",
        "}",
        "main2(A a) {",
        "  print(a.myField);",
        "}");
    // prepare elements
    Element mainElement = findElement("main2(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("myField");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("myField);"), "myField"));
  }

  public void test_isReferencedByQualifiedResolved_NameElement_method() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {}",
        "}",
        "main2(A a) {",
        "  a.myMethod();",
        "}");
    // prepare elements
    Element mainElement = findElement("main2(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("myMethod");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("myMethod();"), "myMethod"));
  }

  public void test_isReferencedByQualifiedResolved_NameElement_operator() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator +(o) {}",
        "  operator -(o) {}",
        "  operator ~() {}",
        "  operator ==(o) {}",
        "}",
        "main(A a) {",
        "  a + 5;",
        "  a += 5;",
        "  a == 5;",
        "  ++a;",
        "  --a;",
        "  ~a;",
        "  a++;",
        "  a--;",
        "}");
    // prepare elements
    Element mainElement = findElement("main(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    // binary
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("+ 5;"), "+"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("+= 5;"), "+="));
    assertRecordedRelation(
        relations,
        new NameElementImpl("=="),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("== 5;"), "=="));
    // prefix
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("++a;"), "++"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("-"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("--a;"), "--"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("~"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("~a;"), "~"));
    // postfix
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("++;"), "++"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("-"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("--;"), "--"));
  }

  public void test_isReferencedByQualifiedUnresolved_NameElement_field() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int myField;",
        "}",
        "main2(var a) {",
        "  print(a.myField);",
        "}");
    // prepare elements
    Element mainElement = findElement("main2(");
    FieldElement fieldElement = findElement("myField;");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("myField");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_DEFINED_BY,
        new ExpectedLocation(fieldElement, findOffset("myField;"), "myField"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("myField);"), "myField"));
  }

  public void test_isReferencedByQualifiedUnresolved_NameElement_method() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {}",
        "}",
        "main2(var a) {",
        "  a.myMethod();",
        "}");
    // prepare elements
    Element mainElement = findElement("main2(");
    MethodElement fieldElement = findElement("myMethod() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("myMethod");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_DEFINED_BY,
        new ExpectedLocation(fieldElement, findOffset("myMethod() {}"), "myMethod"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("myMethod();"), "myMethod"));
  }

  public void test_isReferencedByQualifiedUnresolved_NameElement_operator() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main(a) {",
        "  a + 5;",
        "  a += 5;",
        "  a == 5;",
        "  ++a;",
        "  --a;",
        "  ~a;",
        "  a++;",
        "  a--;",
        "}");
    // prepare elements
    Element mainElement = findElement("main(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    // binary
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("+ 5;"), "+"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("+= 5;"), "+="));
    assertRecordedRelation(
        relations,
        new NameElementImpl("=="),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("== 5;"), "=="));
    // prefix
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("++a;"), "++"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("-"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("--a;"), "--"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("~"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("~a;"), "~"));
    // postfix
    assertRecordedRelation(
        relations,
        new NameElementImpl("+"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("++;"), "++"));
    assertRecordedRelation(
        relations,
        new NameElementImpl("-"),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("--;"), "--"));
  }

  public void test_isReferencedByUnqualified_FieldElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "  main() {",
        "    myField = 5;",
        "    print(myField);",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    FieldElement fieldElement = findElement("myField;");
    PropertyAccessorElement getterElement = fieldElement.getGetter();
    PropertyAccessorElement setterElement = fieldElement.getSetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        getterElement,
        IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, findOffset("myField);"), "myField"));
    assertRecordedRelation(
        relations,
        setterElement,
        IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, findOffset("myField = 5"), "myField"));
  }

  public void test_isReferencedByUnqualified_MethodElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  main() {",
        "    print(foo);",
        "  }",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    MethodElement fooElement = findElement("foo() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, findOffset("foo);"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isReferencedByUnqualified_TopLevelVariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var myTopLevelVariable;",
        "main() {",
        "  myTopLevelVariable = 5;",
        "  print(myTopLevelVariable);",
        "}");
    // set elements
    Element mainElement = findElement("main() {");
    TopLevelVariableElement topVarElement = findElement("myTopLevelVariable;");
    PropertyAccessorElement getterElement = topVarElement.getGetter();
    PropertyAccessorElement setterElement = topVarElement.getSetter();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        getterElement,
        IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, findOffset("myTopLevelVariable);"), "myTopLevelVariable"));
    assertRecordedRelation(
        relations,
        setterElement,
        IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
        new ExpectedLocation(
            mainElement,
            findOffset("myTopLevelVariable = 5"),
            "myTopLevelVariable"));
  }

  public void test_isWrittenBy_ParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main2(var p) {",
        "  p = 1;",
        "}");
    // set elements
    Element mainElement = findElement("main2(");
    ParameterElement parameterElement = findElement("p) {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        parameterElement,
        IndexConstants.IS_WRITTEN_BY,
        new ExpectedLocation(mainElement, findOffset("p = 1"), "p"));
  }

  public void test_isWrittenBy_VariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 0;",
        "  v = 1;",
        "}");
    // prepare elements
    Element mainElement = findElement("main(");
    VariableElement varElement = findElement("v = 0");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        varElement,
        IndexConstants.IS_WRITTEN_BY,
        new ExpectedLocation(mainElement, findOffset("v = 1;"), "v"));
  }

  public void test_nameIsInvokedBy() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test(x) {}",
        "}",
        "main(A a, p) {",
        "  a.test(1);",
        "  p.test(2);",
        "}");
    // set elements
    Element mainElement = findElement("main(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    NameElementImpl nameElement = new NameElementImpl("test");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_INVOKED_BY_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("test(1);"), "test"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_INVOKED_BY_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("test(2);"), "test"));
    assertNoRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_READ_BY_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("test(2);"), "test"));
  }

  public void test_nameIsReadBy() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test;",
        "}",
        "main(A a, p) {",
        "  print(a.test); // a",
        "  print(p.test); // p",
        "}");
    // set elements
    Element mainElement = findElement("main(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    NameElementImpl nameElement = new NameElementImpl("test");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_READ_BY_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("test); // a"), "test"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_READ_BY_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("test); // p"), "test"));
  }

  public void test_nameIsReadWrittenBy() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test;",
        "}",
        "main(A a, p) {",
        "  a.test += 1;",
        "  p.test += 2;",
        "}");
    // set elements
    Element mainElement = findElement("main(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    NameElementImpl nameElement = new NameElementImpl("test");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_READ_WRITTEN_BY_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("test += 1;"), "test"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_READ_WRITTEN_BY_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("test += 2;"), "test"));
  }

  public void test_nameIsWrittenByUnresolved() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test;",
        "}",
        "main(A a, p) {",
        "  a.test = 1;",
        "  p.test = 2;",
        "}");
    // set elements
    Element mainElement = findElement("main(");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    NameElementImpl nameElement = new NameElementImpl("test");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_WRITTEN_BY_RESOLVED,
        new ExpectedLocation(mainElement, findOffset("test = 1;"), "test"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.NAME_IS_WRITTEN_BY_UNRESOLVED,
        new ExpectedLocation(mainElement, findOffset("test = 2;"), "test"));
  }

  public void test_unresolvedUnit() throws Exception {
    index = new IndexContributor(store);
    // no CompilationUnitElement, but no NPE
    testUnit = mock(CompilationUnit.class);
    index.visitCompilationUnit(testUnit);
    verify(testUnit).getElement();
    verifyNoMoreInteractions(testUnit);
    // no enclosing element
    assertSame(null, index.peekElement());
  }

  private List<RecordedRelation> captureRecordedRelations() {
    return captureRelations(store);
  }
}

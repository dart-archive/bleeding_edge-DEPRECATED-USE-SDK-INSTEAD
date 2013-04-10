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

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;

import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

public class IndexContributorTest extends AbstractDartTest {
  private static class ExpectedLocation {
    Element element;
    int offset;
    String name;
    String prefix;

    ExpectedLocation(Element element, int offset, String name) {
      this(element, offset, name, null);
    }

    ExpectedLocation(Element element, int offset, String name, String prefix) {
      this.element = element;
      this.offset = offset;
      this.name = name;
      this.prefix = prefix;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).addValue(element).addValue(offset).addValue(name.length()).addValue(
          prefix).toString();
    }
  }

  /**
   * Information about single relation recorded into {@link IndexStore}.
   */
  private static class RecordedRelation {
    final Element element;
    final Relationship relation;
    final Location location;

    public RecordedRelation(Element element, Relationship relation, Location location) {
      this.element = element;
      this.relation = relation;
      this.location = location;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).addValue(element).addValue(relation).addValue(location).toString();
    }
  }

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

  /**
   * Asserts that actual {@link Location} has given properties.
   */
  private static void assertLocation(Location actual, Element expectedElement, int expectedOffset,
      String expectedNameForLength, String expectedPrefix) {
    assertEquals(expectedElement, actual.getElement());
    assertEquals(expectedOffset, actual.getOffset());
    assertEquals(expectedNameForLength.length(), actual.getLength());
    assertEquals(expectedPrefix, actual.getImportPrefix());
  }

  /**
   * Asserts that given list of {@link RecordedRelation} has no item with specified properties.
   */
  private static void assertNoRecordedRelation(List<RecordedRelation> recordedRelations,
      Element element, Relationship relationship, ExpectedLocation location) {
    for (RecordedRelation recordedRelation : recordedRelations) {
      if (equalsRecordedRelation(recordedRelation, element, relationship, location)) {
        fail("not expected: " + recordedRelation);
      }
    }
  }

  /**
   * Asserts that given list of {@link RecordedRelation} has item with expected properties.
   */
  private static void assertRecordedRelation(List<RecordedRelation> recordedRelations,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    for (RecordedRelation recordedRelation : recordedRelations) {
      if (equalsRecordedRelation(
          recordedRelation,
          expectedElement,
          expectedRelationship,
          expectedLocation)) {
        return;
      }
    }
    fail("not found " + expectedElement + " " + expectedRelationship + " in " + expectedLocation
        + " in\n" + Joiner.on("\n").join(recordedRelations));
  }

  /**
   * Asserts that there are two relations with same location.
   */
  private static void assertRecordedRelations(List<RecordedRelation> relations, Element element,
      Relationship r1, Relationship r2, ExpectedLocation expectedLocation) {
    assertRecordedRelation(relations, element, r1, expectedLocation);
    assertRecordedRelation(relations, element, r2, expectedLocation);
  }

  /**
   * @return {@code true} if given {@link Location} has specified expected properties.
   */
  private static boolean equalsLocation(Location actual, Element expectedElement,
      int expectedOffset, String expectedNameForLength, String expectedPrefix) {
    return Objects.equal(expectedElement, actual.getElement())
        && Objects.equal(expectedOffset, actual.getOffset())
        && Objects.equal(expectedNameForLength.length(), actual.getLength())
        && Objects.equal(expectedPrefix, actual.getImportPrefix());
  }

  /**
   * @return {@code true} if given {@link Location} has specified expected properties.
   */
  private static boolean equalsLocation(Location actual, ExpectedLocation expected) {
    return equalsLocation(actual, expected.element, expected.offset, expected.name, expected.prefix);
  }

  private static boolean equalsRecordedRelation(RecordedRelation recordedRelation,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    return Objects.equal(expectedElement, recordedRelation.element)
        && expectedRelationship == recordedRelation.relation
        && (expectedLocation == null || equalsLocation(recordedRelation.location, expectedLocation));
  }

  private static <T extends Element> T mockElement(Class<T> clazz, ElementLocation location,
      int offset, String name) {
    T element = mock(clazz);
    when(element.getLocation()).thenReturn(location);
    when(element.getNameOffset()).thenReturn(offset);
    when(element.getName()).thenReturn(name);
    return element;
  }

  private IndexStore store = mock(IndexStore.class);
  private IndexContributor index = new IndexContributor(store);

  public void test_createElementLocation() throws Exception {
    ElementLocation elementLocation = mock(ElementLocation.class);
    Element element = mockElement(Element.class, elementLocation, 42, "myName");
    Location location = IndexContributor.createElementLocation(element);
    assertLocation(location, element, 42, "myName", null);
  }

  public void test_createElementLocation_null() throws Exception {
    assertSame(null, IndexContributor.createElementLocation(null));
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
        "typedef MyClass = Object with Mix;");
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

  public void test_isExtendedBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B {} // 2",
        "typedef C = A with B; // 3",
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
        "typedef C = Object with A implements B; // 3",
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
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
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
        "typedef C = Object with A; // 2",
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
        "  var field;",
        "}",
        "topLevelFunction(A p) {",
        "  A v;",
        "  new A(); // 2",
        "  A.field = 1;",
        "  print(A.field); // 3",
        "}");
    // prepare elements
    Element functionElement = findElement("topLevelFunction(");
    ClassElement classElementA = findElement("A {");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, findOffset("A p)"), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, findOffset("A v"), "A"));
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

  public void test_isReferencedBy_ClassElement_withPrefix() throws Exception {
    // Turn of verify of no errors since "pref.MyClass" is an undefined class.
    verifyNoTestUnitErrors = false;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  pref.MyClass v;",
        "}");
    verifyNoTestUnitErrors = true;
    // prepare elements
    Element mainElement = findElement("main(");
    LibraryElement libraryElement = mock(LibraryElement.class);
    ClassElement classElement = mock(ClassElement.class);
    findSimpleIdentifier("MyClass v;").setElement(classElement);
    findSimpleIdentifier("pref.MyClass").setElement(libraryElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("MyClass v;"), "MyClass", "pref"));
  }

  public void test_isReferencedBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef B = Object with A;",
        "topLevelFunction(B p) {",
        "  B v;",
        "}");
    // prepare elements
    Element functionElement = findElement("topLevelFunction(");
    ClassElement classElementB = findElement("B =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, findOffset("B p)"), "B"));
    assertRecordedRelation(
        relations,
        classElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, findOffset("B v"), "B"));
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

  public void test_isReferencedBy_FieldFormalParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int field;",
        "  A(this.field) {}",
        "}",
        "");
    // prepare elements
    Element constructorElement = findNode("A(this.", ConstructorDeclaration.class).getElement();
    FieldElement fieldElement = findElement("field;");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(constructorElement, findOffset("field) {}"), "field"));
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

  public void test_isReferencedBy_ImportElement_noPrefix() throws Exception {
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "var myVar;"));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart';",
        "main() {",
        "  myVar = 1;",
        "}",
        "");
    // set elements
    ImportElement importElement = (ImportElement) findNode(
        "import 'Lib.dart",
        ImportDirective.class).getElement();
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
  }

  public void test_isReferencedBy_ImportElement_withPrefix() throws Exception {
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
        "}",
        "");
    // set elements
    Element mainElement = findElement("main(");
    ImportElement importElement = (ImportElement) findNode(
        "import 'Lib.dart",
        ImportDirective.class).getElement();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    // TODO(scheglov) we have problem - PrefixElement recorded for "pref" and we don't know
    // which ImportElement it is.
//    List<RecordedRelation> relations = captureRecordedRelations();
//    assertRecordedRelation(
//        relations,
//        importElement,
//        IndexConstants.IS_REFERENCED_BY,
//        new ExpectedLocation(mainElement, findOffset("pref.myVar"), "pref"));
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
    LibraryElement referencedElement = ((ExportElement) findNode(
        "export 'Lib.dart",
        ExportDirective.class).getElement()).getExportedLibrary();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("'Lib.dart'"), "'Lib.dart'"));
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
    LibraryElement referencedElement = ((ImportElement) findNode(
        "import 'Lib.dart",
        ImportDirective.class).getElement()).getImportedLibrary();
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("'Lib.dart'"), "'Lib.dart'"));
    assertRecordedRelation(
        relations,
        referencedElement.getDefiningCompilationUnit(),
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("'Lib.dart'"), "'Lib.dart'"));
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

  public void test_isReferencedBy_TypeAliasElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef A();",
        "main2(A p) {",
        "}");
    // prepare elements
    Element mainElement = findElement("main2(");
    FunctionTypeAliasElement classElementA = findElement("A();");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, findOffset("A p)"), "A"));
  }

  public void test_isReferencedBy_TypeVariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<T> {",
        "  T f;",
        "  foo(T p) {",
        "    T v;",
        "  }",
        "}");
    // prepare elements
    ClassElement classElementA = findElement("A<T>");
    TypeVariableElement typeVariableElement = findElement("T>");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        typeVariableElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementA, findOffset("T f"), "T"));
  }

  public void test_isReferencedByQualified_ConstructorElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
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

  public void test_isReferencedByQualified_FieldElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static myField;",
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
    findSimpleIdentifier("foo = 42").setElement(fooElement);
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
    ArgumentCaptor<Element> argElement = ArgumentCaptor.forClass(Element.class);
    ArgumentCaptor<Relationship> argRel = ArgumentCaptor.forClass(Relationship.class);
    ArgumentCaptor<Location> argLocation = ArgumentCaptor.forClass(Location.class);
    verify(store, atLeast(0)).recordRelationship(
        argElement.capture(),
        argRel.capture(),
        argLocation.capture());
    List<RecordedRelation> relations = Lists.newArrayList();
    int count = argElement.getAllValues().size();
    for (int i = 0; i < count; i++) {
      relations.add(new RecordedRelation(
          argElement.getAllValues().get(i),
          argRel.getAllValues().get(i),
          argLocation.getAllValues().get(i)));
    }
    return relations;
  }
}

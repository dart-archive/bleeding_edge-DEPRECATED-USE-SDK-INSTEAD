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
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.builder.CompilationUnitBuilder;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.io.FileUtilities2;

import org.mockito.ArgumentCaptor;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class IndexContributorTest extends EngineTestCase {
  private static class ExpectedLocation {
    Element element;
    int offset;
    String name;

    ExpectedLocation(Element element, int offset, String name) {
      this.element = element;
      this.offset = offset;
      this.name = name;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).addValue(element).addValue(offset).addValue(name.length()).toString();
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

  /**
   * Asserts that actual {@link Location} has given properties.
   */
  private static void assertLocation(Location actual, Element expectedElement, int expectedOffset,
      String expectedNameForLength) {
    assertEquals(expectedElement, actual.getElement());
    assertEquals(expectedOffset, actual.getOffset());
    assertEquals(expectedNameForLength.length(), actual.getLength());
    assertSame(null, actual.getImportPrefix());
  }

  /**
   * Asserts that actual {@link Location} has given properties.
   */
  private static void assertLocation(Location actual, ExpectedLocation expected) {
    assertLocation(actual, expected.element, expected.offset, expected.name);
  }

  private static void assertRecordedRelation(List<RecordedRelation> recordedRelations,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    for (RecordedRelation recordedRelation : recordedRelations) {
      try {
        assertRecordedRelation(
            recordedRelation,
            expectedElement,
            expectedRelationship,
            expectedLocation);
        return;
      } catch (Throwable e) {
      }
    }
    fail("not found " + expectedElement + " " + expectedRelationship + " in " + expectedLocation
        + " in\n" + Joiner.on("\n").join(recordedRelations));
  }

  private static void assertRecordedRelation(RecordedRelation recordedRelation,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    assertEquals(expectedElement, recordedRelation.element);
    assertSame(expectedRelationship, recordedRelation.relation);
    assertLocation(recordedRelation.location, expectedLocation);
  }

  /**
   * Asserts that there are two relations with same location.
   */
  private static void assertRecordedRelations(List<RecordedRelation> relations, Element element,
      Relationship r1, Relationship r2, ExpectedLocation expectedLocation) {
    assertRecordedRelation(relations, element, r1, expectedLocation);
    assertRecordedRelation(relations, element, r2, expectedLocation);
  }

//  private static <T extends Element> T mockElement(Class<T> clazz, Element enclosingElement,
//      ElementLocation location, int offset, String name) {
//    T element = mockElement(clazz, location, offset, name);
//    when(element.getEnclosingElement()).thenReturn(enclosingElement);
//    return element;
//  }

  private static <T extends Element> T mockElement(Class<T> clazz, ElementLocation location,
      int offset, String name) {
    T element = mock(clazz);
    when(element.getLocation()).thenReturn(location);
    when(element.getNameOffset()).thenReturn(offset);
    when(element.getName()).thenReturn(name);
    return element;
  }

//  private static SimpleIdentifier mockSimpleIdentifier(Element element, int offset, String name) {
//    SimpleIdentifier identifier = mock(SimpleIdentifier.class);
//    when(identifier.getElement()).thenReturn(element);
//    when(identifier.getOffset()).thenReturn(offset);
//    when(identifier.getLength()).thenReturn(name.length());
//    return identifier;
//  }

  private IndexStore store = mock(IndexStore.class);
  private IndexContributor index = new IndexContributor(store);
  private Source unitSource = mock(Source.class);
  private CompilationUnit unitNode = mock(CompilationUnit.class);
  private LibraryElement libraryElement = mock(LibraryElement.class);
  private ElementLocation libraryLocation = mock(ElementLocation.class);
  private CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
  private String testCode;
  private CompilationUnit testUnit;

  public void test_accessByQualified_field() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static myField;",
        "}",
        "main() {",
        "  print(A.myField);",
        "}");
    // set elements
    Element mainElement = getElement("main() {");
    FieldElement fieldElement = getElement("myField;");
    findSimpleIdentifier("myField);").setElement(fieldElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldElement,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("myField);"), "myField"));
  }

  public void test_accessByUnqualified_field() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "  main() {",
        "    print(myField);",
        "  }",
        "}");
    // set elements
    Element mainElement = getElement("main() {");
    Element fieldElement = getElement("myField;");
    findSimpleIdentifier("myField);").setElement(fieldElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldElement,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("myField);"), "myField"));
  }

  public void test_accessByUnqualified_parameter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main(var p) {",
        "    print(p);",
        "  }",
        "}");
    // set elements
    MethodElement enclosingElement = getElement("main(");
    ParameterElement parameterElement = getElement("p) {");
    findSimpleIdentifier("p);").setElement(parameterElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        parameterElement,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new ExpectedLocation(enclosingElement, getOffset("p);"), "p"));
  }

  public void test_createElementLocation() throws Exception {
    ElementLocation elementLocation = mock(ElementLocation.class);
    Element element = mockElement(Element.class, elementLocation, 42, "myName");
    Location location = IndexContributor.createElementLocation(element);
    assertLocation(location, element, 42, "myName");
  }

  public void test_createElementLocation_null() throws Exception {
    assertSame(null, IndexContributor.createElementLocation(null));
  }

  public void test_definesClass() throws Exception {
    parseTestUnit("class A {}");
    // prepare elements
    ClassElement classElementA = getElement("A {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        libraryElement,
        IndexConstants.DEFINES_CLASS,
        new ExpectedLocation(classElementA, getOffset("A {}"), "A"));
  }

  public void test_definesClassAlias() throws Exception {
    parseTestUnit("typedef MyClass = Object with Mix;");
    Element classElement = getElement("MyClass =");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        libraryElement,
        IndexConstants.DEFINES_CLASS_ALIAS,
        new ExpectedLocation(classElement, getOffset("MyClass ="), "MyClass"));
  }

  public void test_definesFunction() throws Exception {
    parseTestUnit("myFunction() {}");
    FunctionElement functionElement = getElement("myFunction() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        libraryElement,
        IndexConstants.DEFINES_FUNCTION,
        new ExpectedLocation(functionElement, getOffset("myFunction() {}"), "myFunction"));
  }

  public void test_definesFunctionType() throws Exception {
    parseTestUnit("typedef MyFunction(int p);");
    TypeAliasElement typeAliasElement = getElement("MyFunction");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        libraryElement,
        IndexConstants.DEFINES_FUNCTION_TYPE,
        new ExpectedLocation(typeAliasElement, getOffset("MyFunction"), "MyFunction"));
  }

  public void test_definesVariable() throws Exception {
    parseTestUnit("var myVar;");
    VariableElement varElement = getElement("myVar");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        libraryElement,
        IndexConstants.DEFINES_VARIABLE,
        new ExpectedLocation(varElement, getOffset("myVar"), "myVar"));
  }

  public void test_isExtendedBy_ClassDeclaration() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B extends A {} // 2",
        "");
    // prepare elements
    ClassElement classElementA = getElement("A {} // 1");
    ClassElement classElementB = getElement("B extends");
    findSimpleIdentifier("A {} // 2").setElement(classElementA);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_EXTENDED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementB, getOffset("A {} // 2"), "A"));
  }

  public void test_isExtendedBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B {} // 2",
        "typedef C = A with B; // 3",
        "");
    // prepare elements
    ClassElement classElementA = getElement("A {} // 1");
    ClassElement classElementB = getElement("B {} // 2");
    ClassElement classElementC = getElement("C =");
    findSimpleIdentifier("A with B").setElement(classElementA);
    findSimpleIdentifier("B; // 3").setElement(classElementB);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_EXTENDED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementC, getOffset("A with"), "A"));
  }

  public void test_isImplementedBy_ClassDeclaration() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B implements A {} // 2",
        "");
    // prepare elements
    ClassElement classElementA = getElement("A {} // 1");
    ClassElement classElementB = getElement("B implements");
    findSimpleIdentifier("A {} // 2").setElement(classElementA);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_IMPLEMENTED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementB, getOffset("A {} // 2"), "A"));
  }

  public void test_isImplementedBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B {} // 2",
        "typedef C = Object with A implements B; // 3",
        "");
    // prepare elements
    ClassElement classElementA = getElement("A {} // 1");
    ClassElement classElementB = getElement("B {} // 2");
    ClassElement classElementC = getElement("C =");
    findSimpleIdentifier("A implements B").setElement(classElementA);
    findSimpleIdentifier("B; // 3").setElement(classElementB);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementB,
        IndexConstants.IS_IMPLEMENTED_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementC, getOffset("B; // 3"), "B"));
  }

  public void test_isInvokedByX_constructor() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {}",
        "  A.foo() {}",
        "}",
        "main() {",
        "  new A();",
        "  new A.foo();",
        "}",
        "");
    // set elements
    FunctionElement mainElement = getElement("main() {");
    ConstructorElement unnamedElement = findNode(ConstructorDeclaration.class, "A()").getElement();
    ConstructorElement namedElement = findNode(ConstructorDeclaration.class, "A.foo()").getElement();
    findNode(ConstructorName.class, "A();").setElement(unnamedElement);
    findNode(ConstructorName.class, "A.foo();").setElement(namedElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        unnamedElement,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("A();"), "A"));
    assertRecordedRelation(
        relations,
        namedElement,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("A.foo();"), "A.foo"));
  }

  public void test_isInvokedByX_function() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() {}",
        "main() {",
        "  foo(); // 1",
        "}",
        "");
    // set elements
    FunctionElement mainElement = getElement("main() {");
    FunctionElement fooElement = getElement("foo() {}");
    findSimpleIdentifier("foo();").setElement(fooElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo();"), "foo"));
  }

  public void test_isInvokedByX_method() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  main() {",
        "    foo(); // 1",
        "    this.foo(); // 2",
        "  }",
        "}");
    // set elements
    MethodElement mainElement = getElement("main() {");
    MethodElement fooElement = getElement("foo() {}");
    findSimpleIdentifier("foo(); // 1").setElement(fooElement);
    findSimpleIdentifier("foo(); // 2").setElement(fooElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo(); // 1"), "foo"));
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo(); // 2"), "foo"));
  }

  public void test_isMixedInBy_ClassDeclaration() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "class B extends Object with A {} // 2",
        "");
    // prepare elements
    ClassElement classElementA = getElement("A {} // 1");
    ClassElement classElementB = getElement("B extends");
    findSimpleIdentifier("A {} // 2").setElement(classElementA);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_MIXED_IN_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementB, getOffset("A {} // 2"), "A"));
  }

  public void test_isMixedInBy_ClassTypeAlias() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {} // 1",
        "typedef C = Object with A; // 2",
        "");
    // prepare elements
    ClassElement classElementA = getElement("A {} // 1");
    ClassElement classElementC = getElement("C =");
    findSimpleIdentifier("A; // 2").setElement(classElementA);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelations(
        relations,
        classElementA,
        IndexConstants.IS_MIXED_IN_BY,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementC, getOffset("A; // 2"), "A"));
  }

  public void test_isReferencedBy_ClassElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "topLevelFunction(A p) {",
        "  A v;",
        "  new A(); // 2",
        "  A.field = 1;",
        "  print(A.field); // 3",
        "}");
    // prepare elements
    Element functionElement = getElement("topLevelFunction(");
    ClassElement classElementA = getElement("A {}");
    findSimpleIdentifier("A p").setElement(classElementA);
    findSimpleIdentifier("A v").setElement(classElementA);
    findSimpleIdentifier("A(); // 2").setElement(classElementA);
    findSimpleIdentifier("A.field =").setElement(classElementA);
    findSimpleIdentifier("A.field); // 3").setElement(classElementA);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, getOffset("A p)"), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, getOffset("A v"), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, getOffset("A(); // 2"), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, getOffset("A.field ="), "A"));
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, getOffset("A.field); // 3"), "A"));
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
    Element functionElement = getElement("topLevelFunction(");
    ClassElement classElementB = getElement("B =");
    findSimpleIdentifier("B p").setElement(classElementB);
    findSimpleIdentifier("B v").setElement(classElementB);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, getOffset("B p)"), "B"));
    assertRecordedRelation(
        relations,
        classElementB,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(functionElement, getOffset("B v"), "B"));
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
    FunctionElement enclosingElement = getElement("main(");
    ParameterElement parameterElement = getElement("p}) {");
    findSimpleIdentifier("p: 1").setElement(parameterElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        parameterElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(enclosingElement, getOffset("p: 1"), "p"));
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
    ClassElement classElementA = getElement("A<T>");
    TypeVariableElement typeVariableElement = getElement("T>");
    findSimpleIdentifier("T f").setElement(typeVariableElement);
    findSimpleIdentifier("T v").setElement(typeVariableElement);
    findSimpleIdentifier("T v").setElement(typeVariableElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        typeVariableElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(classElementA, getOffset("T f"), "T"));
  }

  public void test_modifiedByQualified_field() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static myField;",
        "}",
        "main() {",
        "  A.myField = 1;",
        "}");
    // set elements
    Element mainElement = getElement("main() {");
    FieldElement fieldElement = getElement("myField;");
    findSimpleIdentifier("myField = 1").setElement(fieldElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldElement,
        IndexConstants.IS_MODIFIED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("myField = 1"), "myField"));
  }

  public void test_modifiedByUnqualified_field() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "  main() {",
        "    myField = 1;",
        "  }",
        "}");
    // set elements
    Element mainElement = getElement("main() {");
    Element fieldElement = getElement("myField;");
    findSimpleIdentifier("myField = 1").setElement(fieldElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldElement,
        IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("myField = 1"), "myField"));
  }

  public void test_modifiedByUnqualified_parameter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main(var p) {",
        "    p = 1;",
        "  }",
        "}");
    // set elements
    MethodElement mainElement = getElement("main(");
    ParameterElement parameterElement = getElement("p) {");
    findSimpleIdentifier("p = 1").setElement(parameterElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        parameterElement,
        IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("p = 1"), "p"));
  }

  public void test_unresolvedUnit() throws Exception {
    index = new IndexContributor(store);
    // no CompilationUnitElement, but no NPE
    unitNode = mock(CompilationUnit.class);
    index.visitCompilationUnit(unitNode);
    verify(unitNode).getElement();
    verifyNoMoreInteractions(unitNode);
    // no enclosing element
    assertSame(null, index.peekElement());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(libraryElement.getLocation()).thenReturn(libraryLocation);
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(unitElement);
    when(unitNode.getElement()).thenReturn(unitElement);
    when(unitElement.getSource()).thenReturn(unitSource);
    when(unitElement.getEnclosingElement()).thenReturn(libraryElement);
    index.visitCompilationUnit(unitNode);
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

  /**
   * Find node in {@link #testUnit} parsed form {@link #testCode}.
   */
  private <T extends ASTNode> T findNode(final Class<T> clazz, String pattern) {
    final int index = getOffset(pattern);
    final AtomicReference<T> result = new AtomicReference<T>();
    testUnit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void visitNode(ASTNode node) {
        if (node.getOffset() <= index && index < node.getOffset() + node.getLength()
            && clazz.isInstance(node)) {
          result.set((T) node);
        }
        return super.visitNode(node);
      }
    });
    return result.get();
  }

  private SimpleIdentifier findSimpleIdentifier(String pattern) {
    return findNode(SimpleIdentifier.class, pattern);
  }

  /**
   * @return the {@link Element} if {@link SimpleIdentifier} at position of "pattern", not
   *         <code>null</code> or fails.
   */
  @SuppressWarnings("unchecked")
  private <T extends Element> T getElement(String pattern) {
    Element element = findSimpleIdentifier(pattern).getElement();
    assertNotNull(element);
    return (T) element;
  }

  /**
   * @return the existing offset of the given "pattern" in {@link #testCode}.
   */
  private int getOffset(String pattern) {
    int offset = testCode.indexOf(pattern);
    assertThat(offset).describedAs(testCode).isNotEqualTo(-1);
    return offset;
  }

  private void parseTestUnit(String... lines) throws AnalysisException {
    testCode = createSource(lines);
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    TestSource source = new TestSource(null, FileUtilities2.createFile("Test.dart"), testCode);
    testUnit = context.parse(source, null);
    // TODO(scheglov) replace parse() with requesting resolved unit
    testUnit.setElement(unitElement);
    //
    new CompilationUnitBuilder(null, null).buildCompilationUnit(source, testUnit);
  }
}

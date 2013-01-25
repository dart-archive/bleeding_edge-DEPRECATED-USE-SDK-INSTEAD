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
import com.google.dart.engine.ast.Directive;
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
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
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
import com.google.dart.engine.internal.context.AnalysisContextImpl;
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
   * @return <code>true</code> if given {@link Location} has specified expected properties.
   */
  private static boolean equalsLocation(Location actual, Element expectedElement,
      int expectedOffset, String expectedNameForLength, String expectedPrefix) {
    return Objects.equal(expectedElement, actual.getElement())
        && Objects.equal(expectedOffset, actual.getOffset())
        && Objects.equal(expectedNameForLength.length(), actual.getLength())
        && Objects.equal(expectedPrefix, actual.getImportPrefix());
  }

  /**
   * @return <code>true</code> if given {@link Location} has specified expected properties.
   */
  private static boolean equalsLocation(Location actual, ExpectedLocation expected) {
    return equalsLocation(actual, expected.element, expected.offset, expected.name, expected.prefix);
  }

//  private static <T extends Element> T mockElement(Class<T> clazz, Element enclosingElement,
//      ElementLocation location, int offset, String name) {
//    T element = mockElement(clazz, location, offset, name);
//    when(element.getEnclosingElement()).thenReturn(enclosingElement);
//    return element;
//  }

  private static boolean equalsRecordedRelation(RecordedRelation recordedRelation,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    return Objects.equal(expectedElement, recordedRelation.element)
        && expectedRelationship == recordedRelation.relation
        && (expectedLocation == null || equalsLocation(recordedRelation.location, expectedLocation));
  }

//  private static SimpleIdentifier mockSimpleIdentifier(Element element, int offset, String name) {
//    SimpleIdentifier identifier = mock(SimpleIdentifier.class);
//    when(identifier.getElement()).thenReturn(element);
//    when(identifier.getOffset()).thenReturn(offset);
//    when(identifier.getLength()).thenReturn(name.length());
//    return identifier;
//  }

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
  private Source unitSource = mock(Source.class);
  private CompilationUnit unitNode = mock(CompilationUnit.class);
  private LibraryElement libraryElement = mock(LibraryElement.class);
  private ElementLocation libraryLocation = mock(ElementLocation.class);
  private CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
  private String testCode;

  private CompilationUnit testUnit;

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
    ClassElement classElementA = getElement("A {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
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
    assertDefinesTopLevelElement(
        relations,
        libraryElement,
        IndexConstants.DEFINES_CLASS_ALIAS,
        new ExpectedLocation(classElement, getOffset("MyClass ="), "MyClass"));
    assertNoRecordedRelation(relations, classElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_definesFunction() throws Exception {
    parseTestUnit("myFunction() {}");
    FunctionElement functionElement = getElement("myFunction() {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        libraryElement,
        IndexConstants.DEFINES_FUNCTION,
        new ExpectedLocation(functionElement, getOffset("myFunction() {}"), "myFunction"));
    assertNoRecordedRelation(relations, functionElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_definesFunctionType() throws Exception {
    parseTestUnit("typedef MyFunction(int p);");
    TypeAliasElement typeAliasElement = getElement("MyFunction");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        libraryElement,
        IndexConstants.DEFINES_FUNCTION_TYPE,
        new ExpectedLocation(typeAliasElement, getOffset("MyFunction"), "MyFunction"));
    assertNoRecordedRelation(relations, typeAliasElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_definesVariable() throws Exception {
    parseTestUnit("var myVar;");
    VariableElement varElement = getElement("myVar");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertDefinesTopLevelElement(
        relations,
        libraryElement,
        IndexConstants.DEFINES_VARIABLE,
        new ExpectedLocation(varElement, getOffset("myVar"), "myVar"));
    assertNoRecordedRelation(relations, varElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isAccessedByQualified_FieldElement() throws Exception {
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

  public void test_isAccessedByQualified_FunctionElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() {}",
        "main() {",
        "  print(this.foo);",
        "}",
        "");
    // set elements
    Element mainElement = getElement("main(");
    FunctionElement referencedElement = getElement("foo() {}");
    findSimpleIdentifier("foo);").setElement(referencedElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo);"), "foo"));
  }

  public void test_isAccessedByQualified_MethodElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  main() {",
        "    print(this.foo);",
        "  }",
        "}");
    // set elements
    Element mainElement = getElement("main() {");
    MethodElement fooElement = getElement("foo() {}");
    findSimpleIdentifier("foo);").setElement(fooElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo);"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isAccessedByUnqualified_FieldElement() throws Exception {
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

  public void test_isAccessedByUnqualified_FunctionElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() {}",
        "main() {",
        "  print(foo);",
        "}",
        "");
    // set elements
    Element mainElement = getElement("main(");
    FunctionElement referencedElement = getElement("foo() {}");
    findSimpleIdentifier("foo);").setElement(referencedElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo);"), "foo"));
  }

  public void test_isAccessedByUnqualified_MethodElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  main() {",
        "    print(foo);",
        "  }",
        "}");
    // set elements
    Element mainElement = getElement("main() {");
    MethodElement fooElement = getElement("foo() {}");
    findSimpleIdentifier("foo);").setElement(fooElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo);"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isAccessedByUnqualified_ParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main2(var p) {",
        "  print(p);",
        "}");
    // set elements
    Element mainElement = getElement("main2(");
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
        new ExpectedLocation(mainElement, getOffset("p);"), "p"));
  }

  public void test_isAccessedByUnqualified_VariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(v);",
        "}");
    // set elements
    Element mainElement = getElement("main(");
    VariableElement variableElement = mock(VariableElement.class);
    findSimpleIdentifier("v);").setElement(variableElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        variableElement,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("v);"), "v"));
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

  public void test_isInvokedByQualified_function() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  pref.myFunction();",
        "}");
    // prepare elements
    Element mainElement = getElement("main(");
    LibraryElement libraryElement = mock(LibraryElement.class);
    FunctionElement functionElement = mock(FunctionElement.class);
    findSimpleIdentifier("myFunction();").setElement(functionElement);
    findSimpleIdentifier("pref.myFunction").setElement(libraryElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        functionElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("myFunction();"), "myFunction", "pref"));
  }

  public void test_isInvokedByQualified_FunctionElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  pref.foo();",
        "}",
        "");
    // set elements
    Element mainElement = getElement("main() {");
    FunctionElement fooElement = mock(FunctionElement.class);
    findSimpleIdentifier("foo();").setElement(fooElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo();"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
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
    Element mainElement = getElement("main() {");
    MethodElement fooElement = getElement("foo() {}");
    findSimpleIdentifier("foo();").setElement(fooElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fooElement,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("foo();"), "foo"));
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
  }

  public void test_isInvokedByUnqualified_ConstructorElement() throws Exception {
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
    Element mainElement = getElement("main() {");
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

  public void test_isInvokedByUnqualified_FunctionElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() {}",
        "main() {",
        "  foo(); // 1",
        "}",
        "");
    // set elements
    Element mainElement = getElement("main() {");
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
    Element mainElement = getElement("main() {");
    MethodElement fooElement = getElement("foo() {}");
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
    assertNoRecordedRelation(relations, fooElement, IndexConstants.IS_REFERENCED_BY, null);
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

  public void test_isModifiedByQualified_FieldElement() throws Exception {
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

  public void test_isModifiedByQualified_VariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  pref.myTopLevelVar = 0;",
        "}");
    // prepare elements
    Element mainElement = getElement("main(");
    LibraryElement libraryElement = mock(LibraryElement.class);
    VariableElement varElement = mock(VariableElement.class);
    findSimpleIdentifier("myTopLevelVar = 0;").setElement(varElement);
    findSimpleIdentifier("pref.myTopLevelVar").setElement(libraryElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        varElement,
        IndexConstants.IS_MODIFIED_BY_QUALIFIED,
        new ExpectedLocation(mainElement, getOffset("myTopLevelVar = 0;"), "myTopLevelVar", "pref"));
  }

  public void test_isModifiedByUnqualified_FieldElement() throws Exception {
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

  public void test_isModifiedByUnqualified_ParameterElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main2(var p) {",
        "  p = 1;",
        "}");
    // set elements
    Element mainElement = getElement("main2(");
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

  public void test_isModifiedByUnqualified_VariableElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  myTopLevelVar = 0;",
        "}");
    // prepare elements
    Element mainElement = getElement("main(");
    VariableElement varElement = mock(VariableElement.class);
    findSimpleIdentifier("myTopLevelVar = 0;").setElement(varElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        varElement,
        IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
        new ExpectedLocation(mainElement, getOffset("myTopLevelVar = 0;"), "myTopLevelVar"));
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

  public void test_isReferencedBy_ClassElement_withPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  pref.MyClass v;",
        "}");
    // prepare elements
    Element mainElement = getElement("main(");
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
        new ExpectedLocation(mainElement, getOffset("MyClass v;"), "MyClass", "pref"));
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

  public void test_isReferencedBy_CompilationUnitElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library myLib;",
        "part 'SomeUnit.dart';",
        "");
    // set elements
    Element mainElement = unitElement;
    CompilationUnitElement referencedElement = mock(CompilationUnitElement.class);
    findNode(Directive.class, "part 'SomeUnit.dart'").setElement(referencedElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("'SomeUnit.dart'"), "'SomeUnit.dart'"));
  }

  public void test_isReferencedBy_ImportElement_noPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  myVar = 1;",
        "}",
        "");
    // set elements
    ImportElement importElement = mock(ImportElement.class);
    LibraryElement importedlibrary = mock(LibraryElement.class);
    when(importElement.getImportedLibrary()).thenReturn(importedlibrary);
    when(libraryElement.getImports()).thenReturn(new ImportElement[] {importElement});
    Element mainElement = getElement("main(");
    {
      VariableElement variableElement = mock(VariableElement.class);
      when(variableElement.getEnclosingElement()).thenReturn(importedlibrary);
      findSimpleIdentifier("myVar").setElement(variableElement);
    }
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("myVar = 1"), ""));
  }

  public void test_isReferencedBy_ImportElement_withPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  pref.myVar = 1;",
        "  pref.MyClass m;",
        "}",
        "");
    // set elements
    Element mainElement = getElement("main(");
    LibraryElement importedlibrary = mock(LibraryElement.class);
    ImportElement importElement = mock(ImportElement.class);
    when(importElement.getImportedLibrary()).thenReturn(importedlibrary);
    findSimpleIdentifier("pref.myVar").setElement(importElement);
    {
      VariableElement variableElement = mock(VariableElement.class);
      when(variableElement.getEnclosingElement()).thenReturn(importedlibrary);
      findSimpleIdentifier("myVar").setElement(variableElement);
    }
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        importElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("pref.myVar"), "pref"));
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
    Element mainElement = getElement("main(");
    LabelElement referencedElement = getElement("L:");
    findSimpleIdentifier("L;").setElement(referencedElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("L;"), "L"));
  }

  public void test_isReferencedBy_LibraryElement_export() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "export 'SomeLib.dart';",
        "");
    // set elements
    Element mainElement = unitElement;
    LibraryElement referencedElement = mock(LibraryElement.class);
    findNode(Directive.class, "export 'SomeLib.dart'").setElement(referencedElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("'SomeLib.dart'"), "'SomeLib.dart'"));
  }

  public void test_isReferencedBy_LibraryElement_import() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'SomeLib.dart';",
        "");
    // set elements
    Element mainElement = unitElement;
    LibraryElement referencedElement = mock(LibraryElement.class);
    findNode(Directive.class, "import 'SomeLib.dart'").setElement(referencedElement);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        referencedElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("'SomeLib.dart'"), "'SomeLib.dart'"));
  }

  public void test_isReferencedBy_NameElement_class() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "main() {",
        "  A a = new A();",
        "}");
    // prepare elements
    Element mainElement = getElement("main(");
    Element varElement = getElement("a =");
    ClassElement classElementA = getElement("A {}");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("A");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_DEFINED_BY,
        new ExpectedLocation(classElementA, getOffset("A {}"), "A"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("A a = "), "A"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(varElement, getOffset("A();"), "A"));
  }

  public void test_isReferencedBy_NameElement_field() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int myField;",
        "}",
        "main2(var a) {",
        "  print(a.myField);",
        "}");
    // prepare elements
    Element mainElement = getElement("main2(");
    FieldElement fieldElement = getElement("myField;");
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    Element nameElement = new NameElementImpl("myField");
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_DEFINED_BY,
        new ExpectedLocation(fieldElement, getOffset("myField;"), "myField"));
    assertRecordedRelation(
        relations,
        nameElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("myField);"), "myField"));
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
    Element mainElement = getElement("main(");
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
        new ExpectedLocation(mainElement, getOffset("p: 1"), "p"));
  }

  public void test_isReferencedBy_TypeAliasElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef A();",
        "main2(A p) {",
        "}");
    // prepare elements
    Element mainElement = getElement("main2(");
    TypeAliasElement classElementA = getElement("A();");
    findSimpleIdentifier("A p").setElement(classElementA);
    // index
    index.visitCompilationUnit(testUnit);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        classElementA,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(mainElement, getOffset("A p)"), "A"));
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
    testUnit = ((AnalysisContextImpl) context).parse(source, null);
    // TODO(scheglov) replace parse() with requesting resolved unit
    testUnit.setElement(unitElement);
    //
    new CompilationUnitBuilder(null, null).buildCompilationUnit(source, testUnit);
  }
}

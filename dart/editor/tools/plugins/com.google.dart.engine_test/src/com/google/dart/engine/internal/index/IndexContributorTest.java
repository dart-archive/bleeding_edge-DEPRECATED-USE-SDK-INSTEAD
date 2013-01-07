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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ASTFactory;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ElementProxy;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

import org.mockito.ArgumentCaptor;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

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
  }

  /**
   * Information about single relation recorded into {@link IndexStore}.
   */
  private static class RecordedRelation {
    final ElementProxy element;
    final Relationship relation;
    final Location location;

    public RecordedRelation(ElementProxy element, Relationship relation, Location location) {
      this.element = element;
      this.relation = relation;
      this.location = location;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).add("relation", relation).toString();
    }
  }

  /**
   * Asserts that given {@link ElementProxy} represents given {@link Element}.
   */
  private static void assertElementProxy(Element expected, ElementProxy actual) {
    assertEquals(new ElementProxy(expected), actual);
  }

  /**
   * Asserts that actual {@link Location} has given properties.
   */
  private static void assertLocation(Location actual, Element expectedElement, int expectedOffset,
      String expectedNameForLength) {
    assertElementProxy(expectedElement, actual.getElement());
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

  private static void assertRecordedRelation(RecordedRelation recordedRelation,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    assertElementProxy(expectedElement, recordedRelation.element);
    assertSame(expectedRelationship, recordedRelation.relation);
    assertLocation(recordedRelation.location, expectedLocation);
  }

  private static <T extends ASTNode> NodeList<T> createNodeList(ASTNode owner, T... nodes) {
    NodeList<T> nodeList = new NodeList<T>(owner);
    for (int i = 0; i < nodes.length; i++) {
      nodeList.add(nodes[i]);
    }
    return nodeList;
  }

  private static <T extends Element> T mockElement(Class<T> clazz, Element enclosingElement,
      ElementLocation location, int offset, String name) {
    T element = mockElement(clazz, location, offset, name);
    when(element.getEnclosingElement()).thenReturn(enclosingElement);
    return element;
  }

  private static <T extends Element> T mockElement(Class<T> clazz, ElementLocation location,
      int offset, String name) {
    T element = mock(clazz);
    when(element.getLocation()).thenReturn(location);
    when(element.getNameOffset()).thenReturn(offset);
    when(element.getName()).thenReturn(name);
    return element;
  }

  private static SimpleIdentifier mockSimpleIdentifier(Element element, int offset, String name) {
    SimpleIdentifier identifier = mock(SimpleIdentifier.class);
    when(identifier.getElement()).thenReturn(element);
    when(identifier.getOffset()).thenReturn(offset);
    when(identifier.getLength()).thenReturn(name.length());
    return identifier;
  }

  private static TypeName mockTypeName(Type type, int offset, String name) {
    TypeName typeName = mock(TypeName.class);
    when(typeName.getType()).thenReturn(type);
    when(typeName.getOffset()).thenReturn(offset);
    when(typeName.getLength()).thenReturn(name.length());
    return typeName;
  }

  private static VariableDeclarationList mockVariableDeclaration(VariableDeclaration var) {
    VariableDeclarationList variableList = mock(VariableDeclarationList.class);
    NodeList<VariableDeclaration> variableNodeList = createNodeList(variableList, var);
    when(variableList.getVariables()).thenReturn(variableNodeList);
    return variableList;
  }

  private IndexStore store = mock(IndexStore.class);
  private IndexContributor index = new IndexContributor(store);
  private Source unitSource = mock(Source.class);
  private CompilationUnit unitNode = mock(CompilationUnit.class);
  private LibraryElement libraryElement = mock(LibraryElement.class);
  private ElementLocation libraryLocation = mock(ElementLocation.class);

  private CompilationUnitElement unitElement = mock(CompilationUnitElement.class);

  public void test_accessByQualified_field() throws Exception {
    FunctionElement enclosingFunction = mock(FunctionElement.class);
    FieldElement fieldElement = mockElement(FieldElement.class, null, 10, "myField");
    SimpleIdentifier field = mockSimpleIdentifier(fieldElement, 50, "myField");
    // wrap into PrefixedIdentifier
    PrefixedIdentifier prefixedIdentifier = mock(PrefixedIdentifier.class);
    when(field.inGetterContext()).thenReturn(true);
    when(prefixedIdentifier.getIdentifier()).thenReturn(field);
    when(field.getParent()).thenReturn(prefixedIdentifier);
    // index
    index.enterScope(enclosingFunction);
    index.visitSimpleIdentifier(field);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(1);
    assertRecordedRelation(
        relations.get(0),
        fieldElement,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        new ExpectedLocation(enclosingFunction, 50, "myField"));
  }

  public void test_accessByUnqualified_field() throws Exception {
    FunctionElement enclosingFunction = mock(FunctionElement.class);
    FieldElement fieldElement = mockElement(FieldElement.class, null, 10, "myField");
    SimpleIdentifier field = mockSimpleIdentifier(fieldElement, 50, "myField");
    when(field.inGetterContext()).thenReturn(true);
    // index
    index.enterScope(enclosingFunction);
    index.visitSimpleIdentifier(field);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(1);
    assertRecordedRelation(
        relations.get(0),
        fieldElement,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new ExpectedLocation(enclosingFunction, 50, "myField"));
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
    ClassDeclaration classNodeA = mock(ClassDeclaration.class);
    ElementLocation classLocationA = mock(ElementLocation.class);
    ClassElement classElementA = mockElement(ClassElement.class, classLocationA, 42, "ABCDE");
    when(classNodeA.getElement()).thenReturn(classElementA);
    when(classElementA.getEnclosingElement()).thenReturn(unitElement);
    // index
    index.visitClassDeclaration(classNodeA);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(1);
    assertRecordedRelation(
        relations.get(0),
        libraryElement,
        IndexConstants.DEFINES_CLASS,
        new ExpectedLocation(classElementA, 42, "ABCDE"));
  }

  public void test_definesFunction() throws Exception {
    FunctionElement functionElement = mockElement(FunctionElement.class, null, 42, "myFunction");
    FunctionDeclaration function = mock(FunctionDeclaration.class);
    when(function.getElement()).thenReturn(functionElement);
    // index
    index.visitFunctionDeclaration(function);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(1);
    assertRecordedRelation(
        relations.get(0),
        libraryElement,
        IndexConstants.DEFINES_FUNCTION,
        new ExpectedLocation(functionElement, 42, "myFunction"));
  }

  public void test_definesFunctionType() throws Exception {
    TypeAliasElement functionElement = mockElement(TypeAliasElement.class, null, 42, "MyFunction");
    FunctionTypeAlias function = mock(FunctionTypeAlias.class);
    when(function.getElement()).thenReturn(functionElement);
    // index
    index.visitFunctionTypeAlias(function);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(1);
    assertRecordedRelation(
        relations.get(0),
        libraryElement,
        IndexConstants.DEFINES_FUNCTION_TYPE,
        new ExpectedLocation(functionElement, 42, "MyFunction"));
  }

  public void test_definesVariable() throws Exception {
    // VariableDeclaration
    ElementLocation varLocation = mock(ElementLocation.class);
    VariableElement varElement = mockElement(VariableElement.class, varLocation, 42, "myVar");
    VariableDeclaration var = mock(VariableDeclaration.class);
    when(var.getElement()).thenReturn(varElement);
    // TopLevelVariableDeclaration
    TopLevelVariableDeclaration topDeclaration = mock(TopLevelVariableDeclaration.class);
    VariableDeclarationList variableList = mockVariableDeclaration(var);
    when(topDeclaration.getVariables()).thenReturn(variableList);
    index.visitTopLevelVariableDeclaration(topDeclaration);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(1);
    assertRecordedRelation(
        relations.get(0),
        libraryElement,
        IndexConstants.DEFINES_VARIABLE,
        new ExpectedLocation(varElement, 42, "myVar"));
  }

  public void test_isExtendedBy() throws Exception {
    // class B {}
    ElementLocation classLocationB = mock(ElementLocation.class);
    ClassElement classElementB = mockElement(
        ClassElement.class,
        libraryElement,
        classLocationB,
        1042,
        "B");
    Type typeB = mock(Type.class);
    when(typeB.getElement()).thenReturn(classElementB);
    // class A extends B {}
    ClassDeclaration classNodeA = mock(ClassDeclaration.class);
    ElementLocation classLocationA = mock(ElementLocation.class);
    ClassElement classElementA = mockElement(ClassElement.class, classLocationA, 42, "ABCDE");
    when(classNodeA.getElement()).thenReturn(classElementA);
    when(classElementA.getEnclosingElement()).thenReturn(unitElement);
    {
      TypeName extendsTypeNameA = mockTypeName(typeB, 142, "B");
      ExtendsClause extendsClauseA = ASTFactory.extendsClause(extendsTypeNameA);
      when(classNodeA.getExtendsClause()).thenReturn(extendsClauseA);
    }
    // index
    reset(store);
    index.visitClassDeclaration(classNodeA);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(2);
    assertRecordedRelation(
        relations.get(1),
        classElementB,
        IndexConstants.IS_EXTENDED_BY,
        new ExpectedLocation(classElementA, 142, "B"));
  }

  public void test_isImplementedBy() throws Exception {
    // class B {}
    ElementLocation classLocationB = mock(ElementLocation.class);
    ClassElement classElementB = mockElement(
        ClassElement.class,
        libraryElement,
        classLocationB,
        2042,
        "B");
    Type typeB = mock(Type.class);
    when(typeB.getElement()).thenReturn(classElementB);
    ClassDeclaration classNodeA = mock(ClassDeclaration.class);
    // class A implements MyInterface {}
    ElementLocation classLocationA = mock(ElementLocation.class);
    ClassElement classElementA = mockElement(ClassElement.class, classLocationA, 42, "ABCDE");
    when(classNodeA.getElement()).thenReturn(classElementA);
    when(classElementA.getEnclosingElement()).thenReturn(unitElement);
    {
      TypeName implementsTypeNameA = mockTypeName(typeB, 242, "B");
      ImplementsClause implementsClauseA = ASTFactory.implementsClause(implementsTypeNameA);
      when(classNodeA.getImplementsClause()).thenReturn(implementsClauseA);
    }
    // index
    reset(store);
    index.visitClassDeclaration(classNodeA);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertThat(relations).hasSize(2);
    assertRecordedRelation(
        relations.get(1),
        classElementB,
        IndexConstants.IS_IMPLEMENTED_BY,
        new ExpectedLocation(classElementA, 242, "B"));
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
    ArgumentCaptor<ElementProxy> argElement = ArgumentCaptor.forClass(ElementProxy.class);
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

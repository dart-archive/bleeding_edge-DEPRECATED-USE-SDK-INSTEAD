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
package com.google.dart.engine.internal.search;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularFormatterElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.LocationWithData;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.internal.element.member.MethodMember;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.IndexImpl;
import com.google.dart.engine.internal.index.NameElementImpl;
import com.google.dart.engine.internal.index.file.MemoryNodeManager;
import com.google.dart.engine.internal.index.operation.IndexOperation;
import com.google.dart.engine.internal.index.operation.OperationProcessor;
import com.google.dart.engine.internal.index.operation.OperationQueue;
import com.google.dart.engine.internal.search.scope.LibrarySearchScope;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.MatchQuality;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.search.SearchPattern;
import com.google.dart.engine.search.SearchPatternFactory;
import com.google.dart.engine.search.SearchScope;
import com.google.dart.engine.search.SearchScopeFactory;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SearchEngineImplTest extends EngineTestCase {
  private static class ExpectedMatch {
    Element element;
    MatchKind kind;
    MatchQuality quality;
    SourceRange range;
    boolean qualified;

    public ExpectedMatch(Element element, MatchKind kind, int offset, int length) {
      this(element, kind, MatchQuality.EXACT, offset, length);
    }

    public ExpectedMatch(Element element, MatchKind kind, int offset, int length, boolean qualified) {
      this(element, kind, MatchQuality.EXACT, offset, length, qualified);
    }

    public ExpectedMatch(Element element, MatchKind kind, MatchQuality quality, int offset,
        int length) {
      this(element, kind, quality, offset, length, false);
    }

    public ExpectedMatch(Element element, MatchKind kind, MatchQuality quality, int offset,
        int length, boolean qualified) {
      this.element = element;
      this.kind = kind;
      this.quality = quality;
      this.range = new SourceRange(offset, length);
      this.qualified = qualified;
    }
  }

  private static interface SearchRunner<T> {
    T run(OperationQueue queue, OperationProcessor processor, Index index, SearchEngine engine)
        throws Exception;
  }

  private static void assertMatches(List<SearchMatch> matches, ExpectedMatch... expectedMatches) {
    assertThat(matches).hasSize(expectedMatches.length);
    for (SearchMatch match : matches) {
      boolean found = false;
      String msg = match.toString();
      for (ExpectedMatch expectedMatch : expectedMatches) {
        if (Objects.equal(match.getElement(), expectedMatch.element)
            && match.getKind() == expectedMatch.kind && match.getQuality() == expectedMatch.quality
            && Objects.equal(match.getSourceRange(), expectedMatch.range)
            && match.isQualified() == expectedMatch.qualified) {
          found = true;
          break;
        }
      }
      if (!found) {
        fail("Not found: " + msg);
      }
    }
  }

  private IndexStore indexStore = IndexFactory.newSplitIndexStore(new MemoryNodeManager());

  private static final AnalysisContext CONTEXT = mock(AnalysisContext.class);
  private int nextLocationId = 0;
  private SearchScope scope;
  private SearchPattern pattern = null;
  private SearchFilter filter = null;

  private final Source source = mock(Source.class);
  private final CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
  private final LibraryElement libraryElement = mock(LibraryElement.class);
  private final Element elementA = mockElement(Element.class, ElementKind.CLASS);
  private final Element elementB = mockElement(Element.class, ElementKind.CLASS);
  private final Element elementC = mockElement(Element.class, ElementKind.CLASS);
  private final Element elementD = mockElement(Element.class, ElementKind.CLASS);
  private final Element elementE = mockElement(Element.class, ElementKind.CLASS);

  public void fail_searchAssignedTypes_assignments() throws Exception {
    // TODO(scheglov) does not work - new split index store cannot store types (yet?)
    final PropertyAccessorElement setterElement = mockElement(
        PropertyAccessorElement.class,
        ElementKind.SETTER);
    final FieldElement fieldElement = mockElement(FieldElement.class, ElementKind.FIELD);
    when(fieldElement.getSetter()).thenReturn(setterElement);
    final Type typeA = mock(Type.class);
    final Type typeB = mock(Type.class);
    final Type typeC = mock(Type.class);
    indexStore.aboutToIndexDart(CONTEXT, unitElement);
    {
      Location location = new Location(elementA, 1, 10);
      location = new LocationWithData<Type>(location, typeA);
      indexStore.recordRelationship(
          setterElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      location = new LocationWithData<Type>(location, typeB);
      indexStore.recordRelationship(
          setterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          location);
    }
    // will be filtered by scope
    {
      Location location = new Location(elementC, 3, 30);
      location = new LocationWithData<Type>(location, typeC);
      indexStore.recordRelationship(
          setterElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    // not LocationWithData
    {
      Location location = new Location(elementD, 4, 40);
      indexStore.recordRelationship(
          setterElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    indexStore.doneIndex();
    // ask types
    Set<Type> types = runSearch(new SearchRunner<Set<Type>>() {
      @Override
      public Set<Type> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        return engine.searchAssignedTypes(fieldElement, new SearchScope() {
          @Override
          public boolean encloses(Element element) {
            return element != elementC;
          }
        });
      }
    });
    assertThat(types).containsOnly(typeA, typeB);
  }

  public void fail_searchAssignedTypes_initializers() throws Exception {
    // TODO(scheglov) does not work - new split index store cannot store types (yet?)
    final FieldElement fieldElement = mockElement(FieldElement.class, ElementKind.FIELD);
    final Type typeA = mock(Type.class);
    final Type typeB = mock(Type.class);
    {
      Location location = new Location(elementA, 10, 1);
      location = new LocationWithData<Type>(location, typeA);
      indexStore.recordRelationship(fieldElement, IndexConstants.IS_DEFINED_BY, location);
    }
    {
      Location location = new Location(elementB, 20, 1);
      location = new LocationWithData<Type>(location, typeB);
      indexStore.recordRelationship(fieldElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    indexStore.doneIndex();
    // ask types
    Set<Type> types = runSearch(new SearchRunner<Set<Type>>() {
      @Override
      public Set<Type> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        return engine.searchAssignedTypes(fieldElement, null);
      }
    });
    assertThat(types).containsOnly(typeA, typeB);
  }

  public void test_searchDeclarations_String() throws Exception {
    Element referencedElement = new NameElementImpl("test");
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_DEFINED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_DEFINED_BY, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = runSearch(new SearchRunner<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        return engine.searchDeclarations("test", scope, filter);
      }
    });
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NAME_DECLARATION, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NAME_DECLARATION, 10, 20));
  }

  public void test_searchFunctionDeclarations() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    defineFunctionsAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchFunctionDeclarationsSync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FUNCTION_DECLARATION, 1, 2),
        new ExpectedMatch(elementB, MatchKind.FUNCTION_DECLARATION, 10, 20));
  }

  public void test_searchFunctionDeclarations_async() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    defineFunctionsAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchFunctionDeclarationsAsync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FUNCTION_DECLARATION, 1, 2),
        new ExpectedMatch(elementB, MatchKind.FUNCTION_DECLARATION, 10, 20));
  }

  public void test_searchFunctionDeclarations_inUniverse() throws Exception {
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(
          IndexConstants.UNIVERSE,
          IndexConstants.DEFINES_FUNCTION,
          locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(
          IndexConstants.UNIVERSE,
          IndexConstants.DEFINES_FUNCTION,
          locationB);
    }
    indexStore.doneIndex();
    scope = SearchScopeFactory.createUniverseScope();
    // search matches
    List<SearchMatch> matches = searchFunctionDeclarationsSync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FUNCTION_DECLARATION, 1, 2),
        new ExpectedMatch(elementB, MatchKind.FUNCTION_DECLARATION, 10, 20));
  }

  public void test_searchFunctionDeclarations_useFilter() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    defineFunctionsAB(library);
    scope = new LibrarySearchScope(library);
    // search "elementA"
    {
      filter = new SearchFilter() {
        @Override
        public boolean passes(SearchMatch match) {
          return match.getElement() == elementA;
        }
      };
      List<SearchMatch> matches = searchFunctionDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementA, MatchKind.FUNCTION_DECLARATION, 1, 2));
    }
    // search "elementB"
    {
      filter = new SearchFilter() {
        @Override
        public boolean passes(SearchMatch match) {
          return match.getElement() == elementB;
        }
      };
      List<SearchMatch> matches = searchFunctionDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementB, MatchKind.FUNCTION_DECLARATION, 10, 20));
    }
  }

  public void test_searchFunctionDeclarations_usePattern() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    defineFunctionsAB(library);
    scope = new LibrarySearchScope(library);
    // search "A"
    {
      pattern = SearchPatternFactory.createExactPattern("A", true);
      List<SearchMatch> matches = searchFunctionDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementA, MatchKind.FUNCTION_DECLARATION, 1, 2));
    }
    // search "B"
    {
      pattern = SearchPatternFactory.createExactPattern("B", true);
      List<SearchMatch> matches = searchFunctionDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementB, MatchKind.FUNCTION_DECLARATION, 10, 20));
    }
  }

  public void test_searchReferences_AngularComponentElement() throws Exception {
    AngularComponentElement referencedElement = mockElement(
        AngularComponentElement.class,
        ElementKind.ANGULAR_COMPONENT);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.ANGULAR_CLOSING_TAG_REFERENCE,
          locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.ANGULAR_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.ANGULAR_CLOSING_TAG_REFERENCE, 10, 20));
  }

  public void test_searchReferences_AngularControllerElement() throws Exception {
    AngularControllerElement referencedElement = mockElement(
        AngularControllerElement.class,
        ElementKind.ANGULAR_CONTROLLER);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.ANGULAR_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.ANGULAR_REFERENCE, 10, 20));
  }

  public void test_searchReferences_AngularFilterElement() throws Exception {
    AngularFormatterElement referencedElement = mockElement(
        AngularFormatterElement.class,
        ElementKind.ANGULAR_FORMATTER);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.ANGULAR_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.ANGULAR_REFERENCE, 10, 20));
  }

  public void test_searchReferences_AngularPropertyElement() throws Exception {
    AngularPropertyElement referencedElement = mockElement(
        AngularPropertyElement.class,
        ElementKind.ANGULAR_PROPERTY);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.ANGULAR_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.ANGULAR_REFERENCE, 10, 20));
  }

  public void test_searchReferences_AngularScopePropertyElement() throws Exception {
    AngularScopePropertyElement referencedElement = mockElement(
        AngularScopePropertyElement.class,
        ElementKind.ANGULAR_SCOPE_PROPERTY);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.ANGULAR_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.ANGULAR_REFERENCE, 10, 20));
  }

  public void test_searchReferences_AngularSelectorElement() throws Exception {
    AngularSelectorElement referencedElement = mockElement(
        AngularSelectorElement.class,
        ElementKind.ANGULAR_SELECTOR);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.ANGULAR_REFERENCE, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.ANGULAR_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.ANGULAR_REFERENCE, 10, 20));
  }

  public void test_searchReferences_ClassElement() throws Exception {
    ClassElement referencedElement = mockElement(ClassElement.class, ElementKind.CLASS);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.TYPE_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.TYPE_REFERENCE, 10, 20));
  }

  public void test_searchReferences_ClassElement_useScope() throws Exception {
    LibraryElement libraryA = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    LibraryElement libraryB = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    ClassElement referencedElement = mockElement(ClassElement.class, ElementKind.CLASS);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(libraryA);
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      when(elementB.getAncestor(LibraryElement.class)).thenReturn(libraryB);
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    indexStore.doneIndex();
    // search matches, in "libraryA"
    scope = SearchScopeFactory.createLibraryScope(libraryA);
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.TYPE_REFERENCE, 1, 2));
  }

  public void test_searchReferences_CompilationUnitElement() throws Exception {
    CompilationUnitElement referencedElement = mockElement(
        CompilationUnitElement.class,
        ElementKind.COMPILATION_UNIT);
    {
      Location location = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.UNIT_REFERENCE, 1, 2));
  }

  public void test_searchReferences_ConstructorElement() throws Exception {
    ConstructorElement referencedElement = mockElement(
        ConstructorElement.class,
        ElementKind.CONSTRUCTOR);
    {
      Location location = new Location(elementA, 10, 1);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_DEFINED_BY, location);
    }
    {
      Location location = new Location(elementB, 20, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    {
      Location location = new Location(elementC, 30, 3);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.CONSTRUCTOR_DECLARATION, 10, 1),
        new ExpectedMatch(elementB, MatchKind.CONSTRUCTOR_REFERENCE, 20, 2),
        new ExpectedMatch(elementC, MatchKind.CONSTRUCTOR_REFERENCE, 30, 3));
  }

  public void test_searchReferences_Element_unknown() throws Exception {
    List<SearchMatch> matches = searchReferencesSync(Element.class, null);
    assertThat(matches).isEmpty();
  }

  public void test_searchReferences_FieldElement() throws Exception {
    PropertyAccessorElement getterElement = mockElement(
        PropertyAccessorElement.class,
        ElementKind.GETTER);
    PropertyAccessorElement setterElement = mockElement(
        PropertyAccessorElement.class,
        ElementKind.SETTER);
    FieldElement fieldElement = mockElement(FieldElement.class, ElementKind.FIELD);
    when(fieldElement.getGetter()).thenReturn(getterElement);
    when(fieldElement.getSetter()).thenReturn(setterElement);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(
          getterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(
          getterElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 3, 30);
      indexStore.recordRelationship(
          setterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementD, 4, 40);
      indexStore.recordRelationship(
          setterElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, fieldElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FIELD_READ, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.FIELD_READ, 2, 20, true),
        new ExpectedMatch(elementC, MatchKind.FIELD_WRITE, 3, 30, false),
        new ExpectedMatch(elementD, MatchKind.FIELD_WRITE, 4, 40, true));
  }

  public void test_searchReferences_FieldElement_invocation() throws Exception {
    PropertyAccessorElement getterElement = mockElement(
        PropertyAccessorElement.class,
        ElementKind.GETTER);
    FieldElement fieldElement = mockElement(FieldElement.class, ElementKind.FIELD);
    when(fieldElement.getGetter()).thenReturn(getterElement);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(getterElement, IndexConstants.IS_INVOKED_BY_QUALIFIED, location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(
          getterElement,
          IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
          location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, fieldElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FIELD_INVOCATION, 1, 10, true),
        new ExpectedMatch(elementB, MatchKind.FIELD_INVOCATION, 2, 20, false));
  }

  public void test_searchReferences_FieldElement2() throws Exception {
    FieldElement fieldElement = mockElement(FieldElement.class, ElementKind.FIELD);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(fieldElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(
          fieldElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, fieldElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FIELD_REFERENCE, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.FIELD_REFERENCE, 2, 20, true));
  }

  public void test_searchReferences_FunctionElement() throws Exception {
    FunctionElement referencedElement = mockElement(FunctionElement.class, ElementKind.FUNCTION);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_INVOKED_BY, location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FUNCTION_EXECUTION, 1, 10),
        new ExpectedMatch(elementB, MatchKind.FUNCTION_REFERENCE, 2, 20));
  }

  public void test_searchReferences_ImportElement() throws Exception {
    ImportElement referencedElement = mockElement(ImportElement.class, ElementKind.IMPORT);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 0);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.IMPORT_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.IMPORT_REFERENCE, 10, 0));
  }

  public void test_searchReferences_LibraryElement() throws Exception {
    LibraryElement referencedElement = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    {
      Location location = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.LIBRARY_REFERENCE, 1, 2));
  }

  public void test_searchReferences_MethodElement() throws Exception {
    MethodElement referencedElement = mockElement(MethodElement.class, ElementKind.METHOD);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 3, 30);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementD, 4, 40);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.METHOD_INVOCATION, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.METHOD_INVOCATION, 2, 20, true),
        new ExpectedMatch(elementC, MatchKind.METHOD_REFERENCE, 3, 30, false),
        new ExpectedMatch(elementD, MatchKind.METHOD_REFERENCE, 4, 40, true));
  }

  public void test_searchReferences_MethodMember() throws Exception {
    MethodElement referencedElement = mockElement(MethodElement.class, ElementKind.METHOD);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 3, 30);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementD, 4, 40);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          location);
    }
    indexStore.doneIndex();
    // search matches
    MethodMember referencedMember = new MethodMember(referencedElement, null);
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedMember);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.METHOD_INVOCATION, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.METHOD_INVOCATION, 2, 20, true),
        new ExpectedMatch(elementC, MatchKind.METHOD_REFERENCE, 3, 30, false),
        new ExpectedMatch(elementD, MatchKind.METHOD_REFERENCE, 4, 40, true));
  }

  public void test_searchReferences_notSupported() throws Exception {
    Element referencedElement = mockElement(Element.class, ElementKind.UNIVERSE);
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    assertThat(matches).isEmpty();
  }

  public void test_searchReferences_ParameterElement() throws Exception {
    ParameterElement referencedElement = mockElement(ParameterElement.class, ElementKind.PARAMETER);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_READ_BY, location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_WRITTEN_BY, location);
    }
    {
      Location location = new Location(elementC, 3, 30);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_READ_WRITTEN_BY, location);
    }
    {
      Location location = new Location(elementD, 4, 40);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    {
      Location location = new Location(elementD, 5, 50);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_INVOKED_BY, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    // TODO(scheglov) why no MatchKind.FIELD_READ_WRITE ?
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.VARIABLE_READ, 1, 10),
        new ExpectedMatch(elementB, MatchKind.VARIABLE_WRITE, 2, 20),
        new ExpectedMatch(elementC, MatchKind.VARIABLE_READ_WRITE, 3, 30),
        new ExpectedMatch(elementD, MatchKind.NAMED_PARAMETER_REFERENCE, 4, 40),
        new ExpectedMatch(elementD, MatchKind.FUNCTION_EXECUTION, 5, 50));
  }

  public void test_searchReferences_PropertyAccessorElement_getter() throws Exception {
    PropertyAccessorElement accessor = mockElement(
        PropertyAccessorElement.class,
        ElementKind.GETTER);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(accessor, IndexConstants.IS_REFERENCED_BY_UNQUALIFIED, location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(accessor, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, accessor);
    // verify
    assertMatches(matches, new ExpectedMatch(
        elementA,
        MatchKind.PROPERTY_ACCESSOR_REFERENCE,
        1,
        10,
        false), new ExpectedMatch(elementB, MatchKind.PROPERTY_ACCESSOR_REFERENCE, 2, 20, true));
  }

  public void test_searchReferences_PropertyAccessorElement_setter() throws Exception {
    PropertyAccessorElement accessor = mockElement(
        PropertyAccessorElement.class,
        ElementKind.SETTER);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(accessor, IndexConstants.IS_REFERENCED_BY_UNQUALIFIED, location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(accessor, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, accessor);
    // verify
    assertMatches(matches, new ExpectedMatch(
        elementA,
        MatchKind.PROPERTY_ACCESSOR_REFERENCE,
        1,
        10,
        false), new ExpectedMatch(elementB, MatchKind.PROPERTY_ACCESSOR_REFERENCE, 2, 20, true));
  }

  public void test_searchReferences_TopLevelVariableElement() throws Exception {
    PropertyAccessorElement getterElement = mockElement(
        PropertyAccessorElement.class,
        ElementKind.GETTER);
    PropertyAccessorElement setterElement = mockElement(
        PropertyAccessorElement.class,
        ElementKind.SETTER);
    TopLevelVariableElement topVariableElement = mockElement(
        TopLevelVariableElement.class,
        ElementKind.TOP_LEVEL_VARIABLE);
    when(topVariableElement.getGetter()).thenReturn(getterElement);
    when(topVariableElement.getSetter()).thenReturn(setterElement);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(
          getterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 2, 20);
      indexStore.recordRelationship(
          setterElement,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, topVariableElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FIELD_READ, 1, 10, false),
        new ExpectedMatch(elementC, MatchKind.FIELD_WRITE, 2, 20, false));
  }

  public void test_searchReferences_TypeAliasElement() throws Exception {
    FunctionTypeAliasElement referencedElement = mockElement(
        FunctionTypeAliasElement.class,
        ElementKind.FUNCTION_TYPE_ALIAS);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FUNCTION_TYPE_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.FUNCTION_TYPE_REFERENCE, 10, 20));
  }

  public void test_searchReferences_TypeParameterElement() throws Exception {
    TypeParameterElement referencedElement = mockElement(
        TypeParameterElement.class,
        ElementKind.TYPE_PARAMETER);
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.TYPE_PARAMETER_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.TYPE_PARAMETER_REFERENCE, 10, 20));
  }

  public void test_searchReferences_VariableElement() throws Exception {
    LocalVariableElement referencedElement = mockElement(
        LocalVariableElement.class,
        ElementKind.LOCAL_VARIABLE);
    {
      Location location = new Location(elementA, 1, 10);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_READ_BY, location);
    }
    {
      Location location = new Location(elementB, 2, 20);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_WRITTEN_BY, location);
    }
    {
      Location location = new Location(elementC, 3, 30);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_READ_WRITTEN_BY, location);
    }
    {
      Location location = new Location(elementD, 4, 40);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_INVOKED_BY, location);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(Element.class, referencedElement);
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.VARIABLE_READ, 1, 10),
        new ExpectedMatch(elementB, MatchKind.VARIABLE_WRITE, 2, 20),
        new ExpectedMatch(elementC, MatchKind.VARIABLE_READ_WRITE, 3, 30),
        new ExpectedMatch(elementD, MatchKind.FUNCTION_EXECUTION, 4, 40));
  }

  public void test_searchSubtypes() throws Exception {
    final ClassElement referencedElement = mockElement(ClassElement.class, ElementKind.CLASS);
    {
      Location locationA = new Location(elementA, 10, 1);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_EXTENDED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 20, 2);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_MIXED_IN_BY, locationB);
    }
    {
      Location locationC = new Location(elementC, 30, 3);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_IMPLEMENTED_BY, locationC);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = runSearch(new SearchRunner<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        return engine.searchSubtypes(referencedElement, scope, filter);
      }
    });
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.EXTENDS_REFERENCE, 10, 1),
        new ExpectedMatch(elementB, MatchKind.WITH_REFERENCE, 20, 2),
        new ExpectedMatch(elementC, MatchKind.IMPLEMENTS_REFERENCE, 30, 3));
  }

  public void test_searchTypeDeclarations_async() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_CLASS, locationA);
    }
    indexStore.doneIndex();
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsAsync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.CLASS_DECLARATION, 1, 2));
  }

  public void test_searchTypeDeclarations_class() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_CLASS, locationA);
    }
    indexStore.doneIndex();
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsSync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.CLASS_DECLARATION, 1, 2));
  }

  public void test_searchTypeDeclarations_classAlias() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_CLASS_ALIAS, locationA);
    }
    indexStore.doneIndex();
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsSync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.CLASS_ALIAS_DECLARATION, 1, 2));
  }

  public void test_searchTypeDeclarations_functionType() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION_TYPE, locationA);
    }
    indexStore.doneIndex();
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsSync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.FUNCTION_TYPE_DECLARATION, 1, 2));
  }

  public void test_searchUnresolvedQualifiedReferences() throws Exception {
    Element referencedElement = new NameElementImpl("test");
    {
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
          locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
          locationB);
    }
    indexStore.doneIndex();
    // search matches
    List<SearchMatch> matches = searchReferencesSync(
        "searchQualifiedMemberReferences",
        String.class,
        "test");
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NAME_REFERENCE_RESOLVED, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NAME_REFERENCE_UNRESOLVED, 10, 20));
  }

  public void test_searchVariableDeclarations() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    defineVariablesAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchVariableDeclarationsSync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.VARIABLE_DECLARATION, 1, 2),
        new ExpectedMatch(elementB, MatchKind.VARIABLE_DECLARATION, 10, 20));
  }

  public void test_searchVariableDeclarations_async() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    defineVariablesAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchVariableDeclarationsAsync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.VARIABLE_DECLARATION, 1, 2),
        new ExpectedMatch(elementB, MatchKind.VARIABLE_DECLARATION, 10, 20));
  }

  public void test_searchVariableDeclarations_usePattern() throws Exception {
    LibraryElement library = mockElement(LibraryElement.class, ElementKind.LIBRARY);
    defineVariablesAB(library);
    scope = new LibrarySearchScope(library);
    // search "A"
    {
      pattern = SearchPatternFactory.createExactPattern("A", true);
      List<SearchMatch> matches = searchVariableDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementA, MatchKind.VARIABLE_DECLARATION, 1, 2));
    }
    // search "B"
    {
      pattern = SearchPatternFactory.createExactPattern("B", true);
      List<SearchMatch> matches = searchVariableDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementB, MatchKind.VARIABLE_DECLARATION, 10, 20));
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // library
    when(unitElement.getLibrary()).thenReturn(libraryElement);
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(unitElement);
    when(unitElement.getSource()).thenReturn(source);
    when(libraryElement.getSource()).thenReturn(source);
    when(libraryElement.getParts()).thenReturn(new CompilationUnitElement[0]);
    // elements
    when(elementA.toString()).thenReturn("A");
    when(elementB.toString()).thenReturn("B");
    when(elementC.toString()).thenReturn("C");
    when(elementD.toString()).thenReturn("D");
    when(elementE.toString()).thenReturn("E");
    when(elementA.getDisplayName()).thenReturn("A");
    when(elementB.getDisplayName()).thenReturn("B");
    when(elementC.getDisplayName()).thenReturn("C");
    when(elementD.getDisplayName()).thenReturn("D");
    when(elementE.getDisplayName()).thenReturn("E");
    when(elementA.getSource()).thenReturn(source);
    when(elementB.getSource()).thenReturn(source);
    when(elementC.getSource()).thenReturn(source);
    when(elementD.getSource()).thenReturn(source);
    when(elementE.getSource()).thenReturn(source);
    when(elementA.getContext()).thenReturn(CONTEXT);
    when(elementB.getContext()).thenReturn(CONTEXT);
    when(elementC.getContext()).thenReturn(CONTEXT);
    when(elementD.getContext()).thenReturn(CONTEXT);
    when(elementE.getContext()).thenReturn(CONTEXT);
    when(CONTEXT.getElement(elementA.getLocation())).thenReturn(elementA);
    when(CONTEXT.getElement(elementB.getLocation())).thenReturn(elementB);
    when(CONTEXT.getElement(elementC.getLocation())).thenReturn(elementC);
    when(CONTEXT.getElement(elementD.getLocation())).thenReturn(elementD);
    when(CONTEXT.getElement(elementE.getLocation())).thenReturn(elementE);
    // start indexing
    assertTrue(indexStore.aboutToIndexDart(CONTEXT, unitElement));
  }

  @Override
  protected void tearDown() throws Exception {
    indexStore = null;
    super.tearDown();
  }

  private void defineFunctionsAB(LibraryElement library) {
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION, locationA);
    }
    {
      when(elementB.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION, locationB);
    }
    indexStore.doneIndex();
  }

  private void defineVariablesAB(LibraryElement library) {
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_VARIABLE, locationA);
    }
    {
      when(elementB.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationB = new Location(elementB, 10, 20);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_VARIABLE, locationB);
    }
    indexStore.doneIndex();
  }

  private <T extends Element> T mockElement(Class<T> clazz, ElementKind kind) {
    T element = mock(clazz);
    when(element.getContext()).thenReturn(CONTEXT);
    when(element.getSource()).thenReturn(source);
    when(element.getKind()).thenReturn(kind);
    ElementLocation elementLocation = new ElementLocationImpl("mockLocation" + nextLocationId++);
    when(element.getLocation()).thenReturn(elementLocation);
    when(CONTEXT.getElement(element.getLocation())).thenReturn(element);
    return element;
  }

  private <T> T runSearch(SearchRunner<T> runner) throws Exception {
    final OperationQueue queue = new OperationQueue();
    final OperationProcessor processor = new OperationProcessor(queue);
    final Index index = new IndexImpl(indexStore, queue, processor);
    final SearchEngine engine = SearchEngineFactory.createSearchEngine(index);
    try {
      new Thread() {
        @Override
        public void run() {
          processor.run();
        }
      }.start();
      processor.waitForRunning();
      return runner.run(queue, processor, index, engine);
    } finally {
      processor.stop(false);
    }
  }

  private List<SearchMatch> searchDeclarationsAsync(final String methodName) throws Exception {
    return runSearch(new SearchRunner<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<SearchMatch> matches = Lists.newArrayList();
        engine.getClass().getMethod(
            methodName,
            SearchScope.class,
            SearchPattern.class,
            SearchFilter.class,
            SearchListener.class).invoke(engine, scope, pattern, filter, new SearchListener() {
          @Override
          public void matchFound(SearchMatch match) {
            matches.add(match);
          }

          @Override
          public void searchComplete() {
            latch.countDown();
          }
        });
        latch.await(30, TimeUnit.SECONDS);
        return matches;
      }
    });
  }

  @SuppressWarnings("unchecked")
  private List<SearchMatch> searchDeclarationsSync(final String methodName) throws Exception {
    return runSearch(new SearchRunner<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        return (List<SearchMatch>) engine.getClass().getMethod(
            methodName,
            SearchScope.class,
            SearchPattern.class,
            SearchFilter.class).invoke(engine, scope, pattern, filter);
      }
    });
  }

  private List<SearchMatch> searchFunctionDeclarationsAsync() throws Exception {
    return searchDeclarationsAsync("searchFunctionDeclarations");
  }

  private List<SearchMatch> searchFunctionDeclarationsSync() throws Exception {
    return searchDeclarationsSync("searchFunctionDeclarations");
  }

  private List<SearchMatch> searchReferencesSync(Class<?> clazz, Object element) throws Exception {
    return searchReferencesSync("searchReferences", clazz, element);
  }

  @SuppressWarnings("unchecked")
  private List<SearchMatch> searchReferencesSync(final String methodName, final Class<?> clazz,
      final Object element) throws Exception {
    return runSearch(new SearchRunner<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        // pass some operation to wait if search will not call processor
        queue.enqueue(mock(IndexOperation.class));
        // run actual search
        return (List<SearchMatch>) engine.getClass().getMethod(
            methodName,
            clazz,
            SearchScope.class,
            SearchFilter.class).invoke(engine, element, scope, filter);
      }
    });
  }

  private List<SearchMatch> searchTypeDeclarationsAsync() throws Exception {
    return searchDeclarationsAsync("searchTypeDeclarations");
  }

  private List<SearchMatch> searchTypeDeclarationsSync() throws Exception {
    return searchDeclarationsSync("searchTypeDeclarations");
  }

  private List<SearchMatch> searchVariableDeclarationsAsync() throws Exception {
    return searchDeclarationsAsync("searchVariableDeclarations");
  }

  private List<SearchMatch> searchVariableDeclarationsSync() throws Exception {
    return searchDeclarationsSync("searchVariableDeclarations");
  }
}

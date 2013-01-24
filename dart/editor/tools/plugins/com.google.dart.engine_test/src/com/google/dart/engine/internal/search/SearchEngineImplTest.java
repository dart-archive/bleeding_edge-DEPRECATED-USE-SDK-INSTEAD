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

import com.google.common.collect.Lists;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.IndexImpl;
import com.google.dart.engine.internal.index.NameElementImpl;
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
import com.google.dart.engine.utilities.source.SourceRange;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SearchEngineImplTest extends EngineTestCase {
  private static class ExpectedMatch {
    Element element;
    MatchKind kind;
    MatchQuality quality;
    SourceRange range;
    boolean qualified;
    String prefix;

    public ExpectedMatch(Element element, MatchKind kind, int offset, int length) {
      this(element, kind, MatchQuality.EXACT, offset, length, null);
    }

    public ExpectedMatch(Element element, MatchKind kind, int offset, int length, boolean qualified) {
      this(element, kind, MatchQuality.EXACT, offset, length, null, qualified);
    }

    public ExpectedMatch(Element element, MatchKind kind, int offset, int length, String prefix) {
      this(element, kind, MatchQuality.EXACT, offset, length, prefix);
    }

    public ExpectedMatch(Element element, MatchKind kind, MatchQuality quality, int offset,
        int length, String prefix) {
      this(element, kind, quality, offset, length, prefix, false);
    }

    public ExpectedMatch(Element element, MatchKind kind, MatchQuality quality, int offset,
        int length, String prefix, boolean qualified) {
      this.element = element;
      this.kind = kind;
      this.quality = quality;
      this.range = new SourceRange(offset, length);
      this.prefix = prefix;
      this.qualified = qualified;
    }
  }

  private static interface SearchRunner<T> {
    T run(OperationQueue queue, OperationProcessor processor, Index index, SearchEngine engine)
        throws Exception;
  }

  private static void assertMatches(List<SearchMatch> matches, ExpectedMatch... expectedMatches) {
    assertThat(matches).hasSize(expectedMatches.length);
    for (int i = 0; i < expectedMatches.length; i++) {
      ExpectedMatch expectedMatch = expectedMatches[i];
      SearchMatch match = matches.get(i);
      String msg = match.toString();
      assertEquals(msg, expectedMatch.element, match.getElement());
      assertSame(msg, expectedMatch.kind, match.getKind());
      assertSame(msg, expectedMatch.quality, match.getQuality());
      assertEquals(msg, expectedMatch.range, match.getSourceRange());
      assertEquals(msg, expectedMatch.prefix, match.getImportPrefix());
      assertEquals(msg, expectedMatch.qualified, match.isQualified());
    }
  }

  private final IndexStore indexStore = IndexFactory.newMemoryIndexStore();
  private SearchScope scope;
  private SearchPattern pattern = null;
  private SearchFilter filter = null;
  private final Element elementA = mock(Element.class);
  private final Element elementB = mock(Element.class);
  private final Element elementC = mock(Element.class);
  private final Element elementD = mock(Element.class);

  public void test_searchDeclarations_String() throws Exception {
    Element referencedElement = new NameElementImpl("test");
    {
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_DEFINED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_DEFINED_BY, locationB);
    }
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
    LibraryElement library = mock(LibraryElement.class);
    defineFunctionsAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchFunctionDeclarationsSync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
  }

  public void test_searchFunctionDeclarations_async() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    defineFunctionsAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchFunctionDeclarationsAsync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
  }

  public void test_searchFunctionDeclarations_inUniverse() throws Exception {
    {
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(
          IndexConstants.UNIVERSE,
          IndexConstants.DEFINES_FUNCTION,
          locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(
          IndexConstants.UNIVERSE,
          IndexConstants.DEFINES_FUNCTION,
          locationB);
    }
    scope = SearchScopeFactory.createUniverseScope();
    // search matches
    List<SearchMatch> matches = searchFunctionDeclarationsSync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
  }

  public void test_searchFunctionDeclarations_useFilter() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
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
      assertMatches(matches, new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2));
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
      assertMatches(matches, new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
    }
  }

  public void test_searchFunctionDeclarations_usePattern() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    defineFunctionsAB(library);
    scope = new LibrarySearchScope(library);
    // search "A"
    {
      pattern = SearchPatternFactory.createExactPattern("A", true);
      List<SearchMatch> matches = searchFunctionDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2));
    }
    // search "B"
    {
      pattern = SearchPatternFactory.createExactPattern("B", true);
      List<SearchMatch> matches = searchFunctionDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
    }
  }

  public void test_searchReferences_ClassElement() throws Exception {
    ClassElement referencedElement = mock(ClassElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.CLASS);
    {
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(ClassElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.TYPE_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.TYPE_REFERENCE, 10, 20));
  }

  public void test_searchReferences_ClassElement_useScope() throws Exception {
    LibraryElement libraryA = mock(LibraryElement.class);
    LibraryElement libraryB = mock(LibraryElement.class);
    ClassElement referencedElement = mock(ClassElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.CLASS);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(libraryA);
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      when(elementB.getAncestor(LibraryElement.class)).thenReturn(libraryB);
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    // search matches, in "libraryA"
    scope = SearchScopeFactory.createLibraryScope(libraryA);
    List<SearchMatch> matches = searchReferencesSync(ClassElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.TYPE_REFERENCE, 1, 2));
  }

  public void test_searchReferences_ClassElement_withPrefix() throws Exception {
    ClassElement referencedElement = mock(ClassElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.CLASS);
    {
      Location locationA = new Location(elementA, 1, 2, "prefA");
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20, "prefB");
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(ClassElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.TYPE_REFERENCE, 1, 2, "prefA"),
        new ExpectedMatch(elementB, MatchKind.TYPE_REFERENCE, 10, 20, "prefB"));
  }

  public void test_searchReferences_CompilationUnitElement() throws Exception {
    CompilationUnitElement referencedElement = mock(CompilationUnitElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.COMPILATION_UNIT);
    {
      Location location = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(
        CompilationUnitElement.class,
        referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.UNIT_REFERENCE, 1, 2));
  }

  public void test_searchReferences_Element_unknown() throws Exception {
    List<SearchMatch> matches = searchReferencesSync(Element.class, null);
    assertThat(matches).isEmpty();
  }

  public void test_searchReferences_FieldElement() throws Exception {
    FieldElement referencedElement = mock(FieldElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.FIELD);
    {
      Location location = new Location(elementA, 1, 10, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 3, 30, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementD, 4, 40, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_MODIFIED_BY_QUALIFIED,
          location);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(FieldElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FIELD_READ, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.FIELD_READ, 2, 20, true),
        new ExpectedMatch(elementC, MatchKind.FIELD_WRITE, 3, 30, false),
        new ExpectedMatch(elementD, MatchKind.FIELD_WRITE, 4, 40, true));
  }

  public void test_searchReferences_FunctionElement() throws Exception {
    FunctionElement referencedElement = mock(FunctionElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.FUNCTION);
    {
      Location location = new Location(elementA, 1, 10, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 3, 30, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementD, 4, 40, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_QUALIFIED,
          location);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(FunctionElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FUNCTION_EXECUTION, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.FUNCTION_EXECUTION, 2, 20, true),
        new ExpectedMatch(elementC, MatchKind.FUNCTION_REFERENCE, 3, 30, false),
        new ExpectedMatch(elementD, MatchKind.FUNCTION_REFERENCE, 4, 40, true));
  }

  public void test_searchReferences_ImportElement() throws Exception {
    ImportElement referencedElement = mock(ImportElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.IMPORT);
    {
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 0, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(ImportElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.IMPORT_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.IMPORT_REFERENCE, 10, 0));
  }

  public void test_searchReferences_LibraryElement() throws Exception {
    LibraryElement referencedElement = mock(LibraryElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.LIBRARY);
    {
      Location location = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, location);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(LibraryElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.LIBRARY_REFERENCE, 1, 2));
  }

  public void test_searchReferences_MethodElement() throws Exception {
    MethodElement referencedElement = mock(MethodElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.METHOD);
    {
      Location location = new Location(elementA, 1, 10, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_INVOKED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 3, 30, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementD, 4, 40, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_QUALIFIED,
          location);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(MethodElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.METHOD_INVOCATION, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.METHOD_INVOCATION, 2, 20, true),
        new ExpectedMatch(elementC, MatchKind.METHOD_REFERENCE, 3, 30, false),
        new ExpectedMatch(elementD, MatchKind.METHOD_REFERENCE, 4, 40, true));
  }

  public void test_searchReferences_ParameterElement() throws Exception {
    ParameterElement referencedElement = mock(ParameterElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.PARAMETER);
    {
      Location location = new Location(elementA, 1, 10, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 2, 20, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
          location);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(ParameterElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.VARIABLE_READ, 1, 10, false),
        new ExpectedMatch(elementC, MatchKind.VARIABLE_WRITE, 2, 20, false));
  }

  public void test_searchReferences_String() throws Exception {
    Element referencedElement = new NameElementImpl("test");
    {
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(String.class, "test");
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NAME_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NAME_REFERENCE, 10, 20));
  }

  public void test_searchReferences_TypeAliasElement() throws Exception {
    TypeAliasElement referencedElement = mock(TypeAliasElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.TYPE_ALIAS);
    {
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_REFERENCED_BY, locationB);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(TypeAliasElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.FUNCTION_TYPE_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.FUNCTION_TYPE_REFERENCE, 10, 20));
  }

  public void test_searchReferences_VariableElement() throws Exception {
    VariableElement referencedElement = mock(VariableElement.class);
    when(referencedElement.getKind()).thenReturn(ElementKind.VARIABLE);
    {
      Location location = new Location(elementA, 1, 10, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementB, 2, 20, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_ACCESSED_BY_QUALIFIED,
          location);
    }
    {
      Location location = new Location(elementC, 3, 30, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
          location);
    }
    {
      Location location = new Location(elementD, 4, 40, null);
      indexStore.recordRelationship(
          referencedElement,
          IndexConstants.IS_MODIFIED_BY_QUALIFIED,
          location);
    }
    // search matches
    List<SearchMatch> matches = searchReferencesSync(VariableElement.class, referencedElement);
    assertEquals(matches, searchReferencesSync(Element.class, referencedElement));
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.VARIABLE_READ, 1, 10, false),
        new ExpectedMatch(elementB, MatchKind.VARIABLE_READ, 2, 20, true),
        new ExpectedMatch(elementC, MatchKind.VARIABLE_WRITE, 3, 30, false),
        new ExpectedMatch(elementD, MatchKind.VARIABLE_WRITE, 4, 40, true));
  }

  public void test_searchSubtypes() throws Exception {
    final ClassElement referencedElement = mock(ClassElement.class);
    {
      Location locationA = new Location(elementA, 10, 1, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_EXTENDED_BY, locationA);
    }
    {
      Location locationB = new Location(elementB, 20, 2, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_MIXED_IN_BY, locationB);
    }
    {
      Location locationC = new Location(elementC, 30, 3, null);
      indexStore.recordRelationship(referencedElement, IndexConstants.IS_IMPLEMENTED_BY, locationC);
    }
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
    LibraryElement library = mock(LibraryElement.class);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_CLASS, locationA);
    }
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsAsync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2));
  }

  public void test_searchTypeDeclarations_class() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_CLASS, locationA);
    }
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsSync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2));
  }

  public void test_searchTypeDeclarations_classAlias() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_CLASS_ALIAS, locationA);
    }
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsSync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2));
  }

  public void test_searchTypeDeclarations_functionType() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION_TYPE, locationA);
    }
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchTypeDeclarationsSync();
    // verify
    assertMatches(matches, new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2));
  }

  public void test_searchVariableDeclarations() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    defineVariablesAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchVariableDeclarationsSync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
  }

  public void test_searchVariableDeclarations_async() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    defineVariablesAB(library);
    scope = new LibrarySearchScope(library);
    // search matches
    List<SearchMatch> matches = searchVariableDeclarationsAsync();
    // verify
    assertMatches(
        matches,
        new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2),
        new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
  }

  public void test_searchVariableDeclarations_usePattern() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    defineVariablesAB(library);
    scope = new LibrarySearchScope(library);
    // search "A"
    {
      pattern = SearchPatternFactory.createExactPattern("A", true);
      List<SearchMatch> matches = searchVariableDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementA, MatchKind.NOT_A_REFERENCE, 1, 2));
    }
    // search "B"
    {
      pattern = SearchPatternFactory.createExactPattern("B", true);
      List<SearchMatch> matches = searchVariableDeclarationsSync();
      assertMatches(matches, new ExpectedMatch(elementB, MatchKind.NOT_A_REFERENCE, 10, 20));
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(elementA.getName()).thenReturn("A");
    when(elementB.getName()).thenReturn("B");
    when(elementC.getName()).thenReturn("C");
    when(elementD.getName()).thenReturn("D");
  }

  private void defineFunctionsAB(LibraryElement library) {
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION, locationA);
    }
    {
      when(elementB.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION, locationB);
    }
  }

  private void defineVariablesAB(LibraryElement library) {
    {
      when(elementA.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_VARIABLE, locationA);
    }
    {
      when(elementB.getAncestor(LibraryElement.class)).thenReturn(library);
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_VARIABLE, locationB);
    }
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
        latch.await(1, TimeUnit.SECONDS);
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

  @SuppressWarnings("unchecked")
  private List<SearchMatch> searchReferencesSync(final Class<?> clazz, final Object element)
      throws Exception {
    return runSearch(new SearchRunner<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> run(OperationQueue queue, OperationProcessor processor, Index index,
          SearchEngine engine) throws Exception {
        // pass some operation to wait if search will not call processor
        queue.enqueue(mock(IndexOperation.class));
        // run actual search
        return (List<SearchMatch>) engine.getClass().getMethod(
            "searchReferences",
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

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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.IndexImpl;
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
  private class ExpectedMatch {
    Element element;
    MatchKind kind;
    MatchQuality quality;
    SourceRange range;
    boolean qualified;
    String prefix;

    public ExpectedMatch(Element element, MatchKind kind, int offset, int length) {
      this(element, kind, MatchQuality.EXACT, new SourceRange(offset, length));
    }

    public ExpectedMatch(Element element, MatchKind kind, MatchQuality quality, SourceRange range) {
      this.element = element;
      this.kind = kind;
      this.quality = quality;
      this.range = range;
    }
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

  public void test_searchFunctionDeclarations_inWorkspace() throws Exception {
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
    scope = SearchScopeFactory.createWorkspaceScope();
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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(elementA.getName()).thenReturn("A");
    when(elementB.getName()).thenReturn("B");
  }

  private void defineFunctionsAB(LibraryElement library) {
    {
      Location locationA = new Location(elementA, 1, 2, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION, locationA);
    }
    {
      Location locationB = new Location(elementB, 10, 20, null);
      indexStore.recordRelationship(library, IndexConstants.DEFINES_FUNCTION, locationB);
    }
  }

  private List<SearchMatch> doSearchAsync(String methodName) throws Exception {
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
    } finally {
      processor.stop(false);
    }
  }

  @SuppressWarnings("unchecked")
  private List<SearchMatch> doSearchSync(String methodName) throws Exception {
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
      return (List<SearchMatch>) engine.getClass().getMethod(
          methodName,
          SearchScope.class,
          SearchPattern.class,
          SearchFilter.class).invoke(engine, scope, pattern, filter);
    } finally {
      processor.stop(false);
    }
  }

  private List<SearchMatch> searchFunctionDeclarationsAsync() throws Exception {
    return doSearchAsync("searchFunctionDeclarations");
  }

  private List<SearchMatch> searchFunctionDeclarationsSync() throws Exception {
    return doSearchSync("searchFunctionDeclarations");
  }
}

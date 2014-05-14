/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import org.apache.commons.lang3.StringUtils;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TopLevelDeclarationsComputerTest extends AbstractLocalServerTest {
  public static final String RIGHT_ARROW = " => "; //$NON-NLS-1$

  private String contextId;
  private String code;
  private Source source;
  private List<SearchResult> searchResults = Lists.newArrayList();

  public void test_class() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class ClassA {",
        "}",
        "class ClassB {",
        "}",
        "main() {",
        "}"));
    doSearch("Class[A-Z]");
    assertThat(searchResults).hasSize(2);
    assertHasResult("ClassA {", SearchResultKind.CLASS_DECLARATION);
    assertHasResult("ClassB {", SearchResultKind.CLASS_DECLARATION);
  }

  public void test_class_exactName() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "}",
        "class B {",
        "}",
        "main() {",
        "}"));
    doSearch("A");
    assertThat(searchResults).hasSize(1);
    assertHasResult("A {", SearchResultKind.CLASS_DECLARATION);
  }

  public void test_class_inUniverse() throws Exception {
    String contextA = createContext("contextA");
    String contextB = createContext("contextB");
    String codeA = makeSource(//
        "class MyTestA {",
        "}");
    String codeB = makeSource(//
        "class MyTestB {",
        "}");
    Source sourceA = addSource(contextA, "/testA.dart", codeA);
    Source sourceB = addSource(contextB, "/testB.dart", codeB);
    // search the universe
    contextId = null;
    doSearch("MyTest[A,B]");
    // validate
    assertThat(searchResults).hasSize(2);
    assertHasResult(
        sourceA,
        codeA,
        "MyTestA {",
        "MyTestA".length(),
        SearchResultKind.CLASS_DECLARATION);
    assertHasResult(
        sourceB,
        codeB,
        "MyTestB {",
        "MyTestB".length(),
        SearchResultKind.CLASS_DECLARATION);
  }

  public void test_function_nameRange() throws Exception {
    createContextWithSingleSource(makeSource(//
        "fa() {}",
        "fb() {}",
        "fc() {}",
        ""));
    doSearch("f[a-b]");
    assertThat(searchResults).hasSize(2);
    assertHasResult("fa() {}", SearchResultKind.FUNCTION_DECLARATION);
    assertHasResult("fb() {}", SearchResultKind.FUNCTION_DECLARATION);
  }

  public void test_functionTypeAlias_exactName() throws Exception {
    createContextWithSingleSource(makeSource(//
        "typedef FA();",
        "typedef FB();",
        "typedef FC();",
        ""));
    doSearch("FB");
    assertThat(searchResults).hasSize(1);
    assertHasResult("FB()", SearchResultKind.FUNCTION_TYPE_DECLARATION);
  }

  public void test_variable() throws Exception {
    createContextWithSingleSource(makeSource(//
        "var va = 1;",
        "var vb = 2;",
        "var vc = 3;",
        ""));
    doSearch("v[a,c]");
    assertThat(searchResults).hasSize(2);
    assertHasResult("va = 1;", SearchResultKind.VARIABLE_DECLARATION);
    assertHasResult("vc = 3;", SearchResultKind.VARIABLE_DECLARATION);
  }

  public void test_variable_noLocal() throws Exception {
    createContextWithSingleSource(makeSource(//
        "main() {",
        "  var v = 42;",
        "}"));
    doSearch("v");
    assertThat(searchResults).isEmpty();
  }

  private void addTestSource(String code) {
    this.code = code;
    this.source = addSource(contextId, "/test.dart", code);
  }

  private SearchResult assertHasResult(Source source, String code, String search, int length,
      SearchResultKind kind) {
    int offset = code.indexOf(search);
    SearchResult result = findSearchResult(source, kind, offset, length);
    assertNotNull("Not found\n\"" + search + "\" [offset=" + offset + ", length=" + length
        + ", kind=" + kind + "]\n" + "in\n" + StringUtils.join(searchResults, "\n"), result);
    return result;
  }

  private SearchResult assertHasResult(String search, int length, SearchResultKind kind) {
    return assertHasResult(source, code, search, length, kind);
  }

  private SearchResult assertHasResult(String search, SearchResultKind kind) {
    int length = CharMatcher.JAVA_LETTER.negate().indexIn(search);
    return assertHasResult(search, length, kind);
  }

  private void createContextWithSingleSource(String code) {
    createTestContext();
    addTestSource(code);
  }

  private void createTestContext() {
    this.contextId = createContext("test");
  }

  /**
   * Requests references and waits for results.
   */
  private void doSearch(String search) throws Exception {
    searchResults.clear();
    final CountDownLatch latch = new CountDownLatch(1);
    server.searchTopLevelDeclarations(contextId, search, new SearchResultsConsumer() {
      @Override
      public void computed(SearchResult[] _searchResults, boolean isLastResult) {
        Collections.addAll(searchResults, _searchResults);
        if (isLastResult) {
          latch.countDown();
        }
      }
    });
    latch.await(600, TimeUnit.SECONDS);
  }

  private SearchResult findSearchResult(Source source, SearchResultKind kind, int offset, int length) {
    for (SearchResult searchResult : searchResults) {
      if (ObjectUtilities.equals(searchResult.getSource(), source)
          && searchResult.getKind() == kind && searchResult.getOffset() == offset
          && searchResult.getLength() == length) {
        return searchResult;
      }
    }
    return null;
  }
}

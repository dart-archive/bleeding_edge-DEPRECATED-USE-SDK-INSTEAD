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

import com.google.common.collect.Lists;
import com.google.dart.engine.source.Source;
import com.google.dart.server.SearchResult;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import java.util.List;

// TODO(scheglov) restore or remove for the new API
public class ClassMemberDeclarationsComputerTest extends AbstractLocalServerTest {
  private String contextId;
  private String code;
  private Source source;
  private List<SearchResult> searchResults = Lists.newArrayList();

  public void test_field() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  var test; // in A",
//        "}",
//        "class A {",
//        "  int test = 42; // in B",
//        "}"));
//    doSearch("test");
//    assertHasResult("test; // in A", SearchResultKind.CLASS_MEMBER_DECLARATION);
//    assertHasResult("test = 42; // in B", SearchResultKind.CLASS_MEMBER_DECLARATION);
  }

//  public void test_method() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  test() {} // in A",
//        "}",
//        "class B {",
//        "  test(int b) {} // in B",
//        "}"));
//    doSearch("test");
//    assertHasResult("test() {} // in A", SearchResultKind.CLASS_MEMBER_DECLARATION);
//    assertHasResult("test(int b) {} // in B", SearchResultKind.CLASS_MEMBER_DECLARATION);
//  }
//
//  private void addTestSource(String code) {
//    this.code = code;
//    this.source = addSource(contextId, "/test.dart", code);
//  }
//
//  private SearchResult assertHasResult(Source source, String code, String search, int length,
//      SearchResultKind kind) {
//    int offset = code.indexOf(search);
//    SearchResult result = findSearchResult(source, kind, offset, length);
//    assertNotNull("Not found\n\"" + search + "\" [offset=" + offset + ", length=" + length
//        + ", kind=" + kind + "]\n" + "in\n" + StringUtils.join(searchResults, "\n"), result);
//    return result;
//  }
//
//  private SearchResult assertHasResult(String search, int length, SearchResultKind kind) {
//    return assertHasResult(source, code, search, length, kind);
//  }
//
//  private SearchResult assertHasResult(String search, SearchResultKind kind) {
//    int length = CharMatcher.JAVA_LETTER.negate().indexIn(search);
//    return assertHasResult(search, length, kind);
//  }
//
//  private void createContextWithSingleSource(String code) {
//    createTestContext();
//    addTestSource(code);
//  }
//
//  private void createTestContext() {
//    this.contextId = createContext("test");
//  }
//
//  /**
//   * Requests references and waits for results.
//   */
//  private void doSearch(String name) throws Exception {
//    searchResults.clear();
//    final CountDownLatch latch = new CountDownLatch(1);
//    server.searchClassMemberDeclarations(name, new SearchResultsConsumer() {
//      @Override
//      public void computed(SearchResult[] _searchResults, boolean isLastResult) {
//        Collections.addAll(searchResults, _searchResults);
//        if (isLastResult) {
//          latch.countDown();
//        }
//      }
//    });
//    latch.await(600, TimeUnit.SECONDS);
//  }
//
//  private SearchResult findSearchResult(Source source, SearchResultKind kind, int offset, int length) {
//    for (SearchResult searchResult : searchResults) {
//      if (ObjectUtilities.equals(searchResult.getSource(), source)
//          && searchResult.getKind() == kind && searchResult.getOffset() == offset
//          && searchResult.getLength() == length) {
//        return searchResult;
//      }
//    }
//    return null;
//  }
}

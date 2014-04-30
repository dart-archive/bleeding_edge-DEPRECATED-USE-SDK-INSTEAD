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
import com.google.dart.server.Outline;
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

public class DartUnitReferencesComputerTest extends AbstractLocalServerTest {
  private String contextId;
  private String code;
  private Source source;
  private List<SearchResult> searchResults = Lists.newArrayList();

  public void test_constructor_named() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  A.named(p);",
        "}",
        "main() {",
        "  new A.named(1);",
        "  new A.named(2);",
        "}"));
    doSearch("named(p);");
    assertThat(searchResults).hasSize(2);
    assertHasResult(".named(1);", ".named".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
    assertHasResult(".named(2);", ".named".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
  }

  public void test_constructor_unnamed() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  A(p);",
        "}",
        "main() {",
        "  new A(1);",
        "  new A(2);",
        "}"));
    doSearch("A(p);");
    assertThat(searchResults).hasSize(2);
    assertHasResult("(1);", "".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
    assertHasResult("(2);", "".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
  }

  public void test_field() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  int fff; // declaration",
        "  A(this.fff); // in constructor",
        "  m() {",
        "    fff = 2;",
        "    fff += 3;",
        "    print(fff); // in m()",
        "  }",
        "}",
        "main(A a) {",
        "  a.fff = 20;",
        "  a.fff += 30;",
        "  print(a.fff); // in main()",
        "}"));
    doSearch("fff; // declaration");
    assertThat(searchResults).hasSize(7);
    assertHasResult("fff); // in constructor", SearchResultKind.FIELD_REFERENCE);
    assertHasResult("fff = 2", SearchResultKind.FIELD_WRITE);
    assertHasResult("fff += 3", SearchResultKind.FIELD_WRITE);
    assertHasResult("fff); // in m()", SearchResultKind.FIELD_READ);
    assertHasResult("fff = 20", SearchResultKind.FIELD_WRITE);
    assertHasResult("fff += 30", SearchResultKind.FIELD_WRITE);
    assertHasResult("fff); // in main()", SearchResultKind.FIELD_READ);
  }

  public void test_function() throws Exception {
    createContextWithSingleSource(makeSource(//
        "fff() {};",
        "main() {",
        "  fff(1);",
        "  print(fff);",
        "}"));
    doSearch("fff() {}");
    assertThat(searchResults).hasSize(2);
    assertHasResult("fff(1);", SearchResultKind.FUNCTION_INVOCATION);
    assertHasResult("fff);", SearchResultKind.FUNCTION_REFERENCE);
  }

  public void test_getter() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  int get ggg => 0;",
        "  m() {",
        "    print(ggg); // in m()",
        "  }",
        "}",
        "main(A a) {",
        "  print(a.ggg); // in main()",
        "}"));
    doSearch("ggg => 0;");
    assertThat(searchResults).hasSize(2);
    assertHasResult("ggg); // in m()", SearchResultKind.PROPERTY_ACCESSOR_REFERENCE);
    assertHasResult("ggg); // in main()", SearchResultKind.PROPERTY_ACCESSOR_REFERENCE);
  }

  public void test_localVariable() throws Exception {
    createContextWithSingleSource(makeSource(//
        "main() {",
        "  int vvv = 1;",
        "  vvv = 2;",
        "  vvv += 3;",
        "  print(vvv);",
        "}"));
    doSearch("vvv = 1;");
    assertThat(searchResults).hasSize(3);
    assertHasResult("vvv = 2", SearchResultKind.VARIABLE_WRITE);
    assertHasResult("vvv += 3", SearchResultKind.VARIABLE_READ_WRITE);
    assertHasResult("vvv);", SearchResultKind.VARIABLE_READ);
  }

  public void test_method() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  mmm(p) {}",
        "  method() {",
        "    mmm(1);",
        "  }",
        "}",
        "main(A a) {",
        "  a.mmm(10);",
        "  print(a.mmm);",
        "}"));
    doSearch("mmm(p) {}");
    assertThat(searchResults).hasSize(3);
    assertHasResult("mmm(1);", SearchResultKind.METHOD_INVOCATION);
    assertHasResult("mmm(10);", SearchResultKind.METHOD_INVOCATION);
    assertHasResult("mmm);", SearchResultKind.METHOD_REFERENCE);
  }

  public void test_method_propagatedType() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  mmm(p) {}",
        "}",
        "main() {",
        "  var a = new A();",
        "  a.mmm(1);",
        "  print(a.mmm);",
        "}"));
    doSearch("mmm(p) {}");
    // TODO(scheglov) there is an extra METHOD_REFERENCE for mmm(1)
    // Only for propagated type?
//    assertThat(searchResults).hasSize(2);
    assertHasResult("mmm(1);", SearchResultKind.METHOD_INVOCATION);
    assertHasResult("mmm);", SearchResultKind.METHOD_REFERENCE);
  }

  public void test_parameter() throws Exception {
    createContextWithSingleSource(makeSource(//
        "main(int ppp) {",
        "  ppp = 2;",
        "  ppp += 3;",
        "  print(ppp);",
        "}"));
    doSearch("ppp) {");
    assertThat(searchResults).hasSize(3);
    assertHasResult("ppp = 2", SearchResultKind.VARIABLE_WRITE);
    assertHasResult("ppp += 3", SearchResultKind.VARIABLE_READ_WRITE);
    assertHasResult("ppp);", SearchResultKind.VARIABLE_READ);
  }

  public void test_path() throws Exception {
    createContextWithSingleSource(makeSource(//
        "library my_lib;",
        "class A {}",
        "class B {",
        "  m() {",
        "    A a = null; // 1",
        "  }",
        "}",
        "typedef String F(A a); // 2",
        "main(int p1, double p2) {",
        "  A a = null; // 3",
        "}"));
    doSearch("A {}");
    assertThat(searchResults).hasSize(3);
    {
      SearchResult result = findSearchResult(
          source,
          SearchResultKind.TYPE_REFERENCE,
          code.indexOf("A a = null; // 1"),
          "A".length());
      assertNotNull(result);
      Outline path = result.getPath();
      assertEquals(makeSource(//
          "LIBRARY my_lib",
          "COMPILATION_UNIT /test.dart",
          "CLASS B",
          "METHOD B.m() → dynamic"), getPathString(path));
    }
    {
      SearchResult result = findSearchResult(
          source,
          SearchResultKind.TYPE_REFERENCE,
          code.indexOf("A a); // 2"),
          "A".length());
      assertNotNull(result);
      Outline path = result.getPath();
      assertEquals(makeSource(//
          "LIBRARY my_lib",
          "COMPILATION_UNIT /test.dart",
          "FUNCTION_TYPE_ALIAS typedef F(A a) → String"), getPathString(path));
    }
    {
      SearchResult result = findSearchResult(
          source,
          SearchResultKind.TYPE_REFERENCE,
          code.indexOf("A a = null; // 3"),
          "A".length());
      assertNotNull(result);
      Outline path = result.getPath();
      assertEquals(makeSource(//
          "LIBRARY my_lib",
          "COMPILATION_UNIT /test.dart",
          "FUNCTION main(int p1, double p2) → dynamic"), getPathString(path));
    }
  }

  public void test_setter() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  void set sss(x) {}",
        "  m() {",
        "    sss = 1;",
        "  }",
        "}",
        "main(A a) {",
        "  a.sss = 10;",
        "}"));
    doSearch("sss(x) {}");
    assertThat(searchResults).hasSize(2);
    assertHasResult("sss = 1", SearchResultKind.PROPERTY_ACCESSOR_REFERENCE);
    assertHasResult("sss = 10", SearchResultKind.PROPERTY_ACCESSOR_REFERENCE);
  }

  public void test_topLevelVariable() throws Exception {
    createContextWithSingleSource(makeSource(//
        "int vvv = 1;",
        "main() {",
        "  vvv = 2;",
        "  vvv += 3;",
        "  print(vvv);",
        "}"));
    doSearch("vvv = 1;");
    assertThat(searchResults).hasSize(3);
    assertHasResult("vvv = 2", SearchResultKind.FIELD_WRITE);
    assertHasResult("vvv += 3", SearchResultKind.FIELD_WRITE);
    assertHasResult("vvv);", SearchResultKind.FIELD_READ);
  }

  public void test_typeReference_class() throws Exception {
    createContextWithSingleSource(makeSource(//
        "main() {",
        "  int va = 1;",
        "  int vb = 1;",
        "}"));
    doSearch("int va = 1");
    assertThat(searchResults).hasSize(2);
    assertHasResult("int va", SearchResultKind.TYPE_REFERENCE);
    assertHasResult("int vb", SearchResultKind.TYPE_REFERENCE);
  }

  public void test_typeReference_functionType() throws Exception {
    createContextWithSingleSource(makeSource(//
        "typedef F();",
        "main(F f) {",
        "}"));
    doSearch("F();");
    assertThat(searchResults).hasSize(1);
    assertHasResult("F f) {", SearchResultKind.TYPE_REFERENCE);
  }

  public void test_typeReference_typeVariable() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A<T> {",
        "  T f;",
        "  T m() => null;",
        "}"));
    doSearch("T>");
    assertThat(searchResults).hasSize(2);
    assertHasResult("T f;", SearchResultKind.TYPE_REFERENCE);
    assertHasResult("T m()", SearchResultKind.TYPE_REFERENCE);
  }

  public void test_unknownMatchKind() throws Exception {
    createContextWithSingleSource(makeSource(//
        "main() {",
        "  int vvv = 1;",
        "  print(vvv);",
        "}"));
    // do simulation
    DartUnitReferencesComputer.test_simulateUknownMatchKind = true;
    try {
      doSearch("vvv = 1;");
    } finally {
      DartUnitReferencesComputer.test_simulateUknownMatchKind = false;
    }
    // no errors
    serverListener.assertNoServerErrors();
    // ...and no results too
    assertThat(searchResults).isEmpty();
  }

  private void assertHasResult(String search, int length, SearchResultKind kind) {
    int offset = code.indexOf(search);
    SearchResult result = findSearchResult(source, kind, offset, length);
    assertNotNull("Not found\n\"" + search + "\" [offset=" + offset + ", length=" + length
        + ", kind=" + kind + "]\n" + "in\n" + StringUtils.join(searchResults, "\n"), result);
  }

  private void assertHasResult(String search, SearchResultKind kind) {
    int length = CharMatcher.JAVA_LETTER.negate().indexIn(search);
    assertHasResult(search, length, kind);
  }

  private void createContextWithSingleSource(String code) {
    contextId = createContext("test");
    this.code = code;
    source = addSource(contextId, "/test.dart", code);
  }

  /**
   * Requests references and waits for results.
   */
  private void doSearch(String search) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    server.searchReferences(contextId, source, code.indexOf(search), new SearchResultsConsumer() {
      @Override
      public void computedReferences(String contextId, Source source, int offset,
          SearchResult[] searchResults, boolean isLastResult) {
        Collections.addAll(DartUnitReferencesComputerTest.this.searchResults, searchResults);
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

  private String getPathString(Outline outline) {
    String str = outline.getKind() + " " + outline.getName();
    Outline[] children = outline.getChildren();
    if (children.length != 0) {
      str += "\n" + getPathString(children[0]);
    }
    return str;
  }
}

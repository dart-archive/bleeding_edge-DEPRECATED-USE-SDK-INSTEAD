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
public class ElementReferencesComputerTest extends AbstractLocalServerTest {
  public static final String RIGHT_ARROW = " => "; //$NON-NLS-1$

  private String contextId;
  private String code;
  private Source source;
  private boolean withPotential = true;
  private List<SearchResult> searchResults = Lists.newArrayList();

  public void test_constructor_named() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  A.named(p);",
//        "}",
//        "main() {",
//        "  new A.named(1);",
//        "  new A.named(2);",
//        "}"));
//    doSearch("named(p);");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult(".named(1);", ".named".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
//    assertHasResult(".named(2);", ".named".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
  }

//  public void test_constructor_unnamed() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  A(p);",
//        "}",
//        "main() {",
//        "  new A(1);",
//        "  new A(2);",
//        "}"));
//    doSearch("A(p);");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult("(1);", "".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
//    assertHasResult("(2);", "".length(), SearchResultKind.CONSTRUCTOR_REFERENCE);
//  }
//
//  public void test_field_explicit() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  int fff; // declaration",
//        "  A(this.fff); // in constructor",
//        "  m() {",
//        "    fff = 2;",
//        "    fff += 3;",
//        "    print(fff); // in m()",
//        "  }",
//        "}",
//        "main(A a) {",
//        "  a.fff = 20;",
//        "  a.fff += 30;",
//        "  print(a.fff); // in main()",
//        "}"));
//    doSearch("fff; // declaration");
//    assertThat(searchResults).hasSize(8);
//    assertHasResult("fff; // declaration", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("fff); // in constructor", SearchResultKind.FIELD_REFERENCE);
//    assertHasResult("fff = 2", SearchResultKind.FIELD_WRITE);
//    assertHasResult("fff += 3", SearchResultKind.FIELD_WRITE);
//    assertHasResult("fff); // in m()", SearchResultKind.FIELD_READ);
//    assertHasResult("fff = 20", SearchResultKind.FIELD_WRITE);
//    assertHasResult("fff += 30", SearchResultKind.FIELD_WRITE);
//    assertHasResult("fff); // in main()", SearchResultKind.FIELD_READ);
//  }
//
//  public void test_field_implicit() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  int get fff => 0;",
//        "  int set fff(x) {}",
//        "  m() {",
//        "    print(fff); // in m()",
//        "    fff = 1; // in m()",
//        "  }",
//        "}",
//        "main(A a) {",
//        "  print(a.fff); // in main()",
//        "  a.fff = 10; // in main()",
//        "}"));
//    {
//      doSearch("fff => 0;");
//      assertThat(searchResults).hasSize(4);
//      assertHasResult("fff); // in m()", SearchResultKind.FIELD_READ);
//      assertHasResult("fff = 1;", SearchResultKind.FIELD_WRITE);
//      assertHasResult("fff); // in m()", SearchResultKind.FIELD_READ);
//      assertHasResult("fff = 10;", SearchResultKind.FIELD_WRITE);
//    }
//    {
//      doSearch("fff(x) {}");
//      assertThat(searchResults).hasSize(4);
//      assertHasResult("fff); // in m()", SearchResultKind.FIELD_READ);
//      assertHasResult("fff = 1;", SearchResultKind.FIELD_WRITE);
//      assertHasResult("fff); // in m()", SearchResultKind.FIELD_READ);
//      assertHasResult("fff = 10;", SearchResultKind.FIELD_WRITE);
//    }
//  }
//
//  public void test_field_inFieldFormalParameter() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  int fff; // declaration",
//        "  A(this.fff); // in constructor",
//        "  m() {",
//        "    fff = 2;",
//        "    print(fff); // in m()",
//        "  }",
//        "}"));
//    doSearch("fff); // in constructor");
//    assertThat(searchResults).hasSize(4);
//    assertHasResult("fff; // declaration", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("fff); // in constructor", SearchResultKind.FIELD_REFERENCE);
//    assertHasResult("fff = 2", SearchResultKind.FIELD_WRITE);
//    assertHasResult("fff); // in m()", SearchResultKind.FIELD_READ);
//  }
//
//  public void test_function() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "fff(p) {};",
//        "main() {",
//        "  fff(1);",
//        "  print(fff);",
//        "}"));
//    doSearch("fff(p) {}");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult("fff(1);", SearchResultKind.FUNCTION_INVOCATION);
//    assertHasResult("fff);", SearchResultKind.FUNCTION_REFERENCE);
//  }
//
//  public void test_getSource() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  var ttt = 0;",
//        "  print(ttt);",
//        "}"));
//    doSearch("ttt = 0");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult("ttt = 0;", SearchResultKind.VARIABLE_DECLARATION);
//    SearchResult searchResult = assertHasResult("ttt);", SearchResultKind.VARIABLE_READ);
//    assertEquals(source, searchResult.getSource());
//  }
//
//  public void test_hierarchyMembers_field_explicit() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  int fff; // in A",
//        "}",
//        "class B extends A {",
//        "  int fff; // in B",
//        "}",
//        "class C extends B {",
//        "  int fff; // in C",
//        "}",
//        "main(A a, B b, C c) {",
//        "  a.fff = 10;",
//        "  b.fff = 20;",
//        "  c.fff = 30;",
//        "}"));
//    doSearch("fff; // in B");
//    assertThat(searchResults).hasSize(6);
//    assertHasResult("fff; // in A", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("fff; // in B", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("fff; // in C", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("fff = 10;", SearchResultKind.FIELD_WRITE);
//    assertHasResult("fff = 20;", SearchResultKind.FIELD_WRITE);
//    assertHasResult("fff = 30;", SearchResultKind.FIELD_WRITE);
//  }
//
//  public void test_hierarchyMembers_method() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  mmm() {} // in A",
//        "}",
//        "class B extends A {",
//        "  mmm() {} // in B",
//        "}",
//        "class C extends B {",
//        "  mmm() {} // in C",
//        "}",
//        "main(A a, B b, C c) {",
//        "  a.mmm(10);",
//        "  b.mmm(20);",
//        "  c.mmm(30);",
//        "}"));
//    doSearch("mmm() {} // in B");
//    assertThat(searchResults).hasSize(3);
//    assertHasResult("mmm(10);", SearchResultKind.METHOD_INVOCATION);
//    assertHasResult("mmm(20);", SearchResultKind.METHOD_INVOCATION);
//    assertHasResult("mmm(30);", SearchResultKind.METHOD_INVOCATION);
//  }
//
//  public void test_localVariable() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  int vvv = 1;",
//        "  vvv = 2;",
//        "  vvv += 3;",
//        "  print(vvv);",
//        "}"));
//    doSearch("vvv = 1;");
//    assertThat(searchResults).hasSize(4);
//    assertHasResult("vvv = 1;", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("vvv = 2", SearchResultKind.VARIABLE_WRITE);
//    assertHasResult("vvv += 3", SearchResultKind.VARIABLE_READ_WRITE);
//    assertHasResult("vvv);", SearchResultKind.VARIABLE_READ);
//  }
//
//  public void test_method() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  mmm(p) {}",
//        "  method() {",
//        "    mmm(1);",
//        "  }",
//        "}",
//        "main(A a) {",
//        "  a.mmm(10);",
//        "  print(a.mmm);",
//        "}"));
//    doSearch("mmm(p) {}");
//    assertThat(searchResults).hasSize(3);
//    assertHasResult("mmm(1);", SearchResultKind.METHOD_INVOCATION);
//    assertHasResult("mmm(10);", SearchResultKind.METHOD_INVOCATION);
//    assertHasResult("mmm);", SearchResultKind.METHOD_REFERENCE);
//  }
//
//  public void test_method_propagatedType() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  mmm(p) {}",
//        "}",
//        "main() {",
//        "  var a = new A();",
//        "  a.mmm(1);",
//        "  print(a.mmm);",
//        "}"));
//    doSearch("mmm(p) {}");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult("mmm(1);", SearchResultKind.METHOD_INVOCATION);
//    assertHasResult("mmm);", SearchResultKind.METHOD_REFERENCE);
//  }
//
//  public void test_oneUnit_twoLibraries() throws Exception {
//    createTestContext();
//    String libA_code = makeSource(//
//        "library my_lib;",
//        "part 'test.dart';",
//        "main() {",
//        "  fff(10);",
//        "}");
//    Source libA_source = addSource(contextId, "/libA.dart", libA_code);
//    String libB_code = makeSource(//
//        "library my_lib;",
//        "part 'test.dart';",
//        "main() {",
//        "  // inserted to ensure a different offset in libB.dart",
//        "  fff(20);",
//        "}");
//    Source libB_source = addSource(contextId, "/libB.dart", libB_code);
//    addTestSource(makeSource(//
//        "part of my_lib;",
//        "fff(p) {};",
//        ""));
//    doSearch("fff(p) {}");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult(
//        libA_source,
//        libA_code,
//        "fff(10);",
//        "fff".length(),
//        SearchResultKind.FUNCTION_INVOCATION);
//    assertHasResult(
//        libB_source,
//        libB_code,
//        "fff(20);",
//        "fff".length(),
//        SearchResultKind.FUNCTION_INVOCATION);
//  }
//
//  public void test_oneUnit_zeroLibraries() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "part of my_lib;",
//        "fff(p) {};",
//        ""));
//    doSearch("fff(p) {}");
//    assertThat(searchResults).isEmpty();
//  }
//
//  public void test_parameter() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main(int ppp) {",
//        "  ppp = 2;",
//        "  ppp += 3;",
//        "  print(ppp);",
//        "}"));
//    doSearch("ppp) {");
//    assertThat(searchResults).hasSize(4);
//    assertHasResult("ppp) {", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("ppp = 2", SearchResultKind.VARIABLE_WRITE);
//    assertHasResult("ppp += 3", SearchResultKind.VARIABLE_READ_WRITE);
//    assertHasResult("ppp);", SearchResultKind.VARIABLE_READ);
//  }
//
//  public void test_path_inConstructor_named() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "library my_lib;",
//        "class A {}",
//        "class B {",
//        "  B.named() {",
//        "    A a = null; // 2",
//        "  }",
//        "}"));
//    doSearch("A {}");
//    assertThat(searchResults).hasSize(1);
//    {
//      SearchResult result = findSearchResult(
//          source,
//          SearchResultKind.TYPE_REFERENCE,
//          code.indexOf("A a = null; // 2"),
//          "A".length());
//      assertNotNull(result);
//      Element[] path = result.getPath();
//      assertEquals(makeSource(//
//          "CONSTRUCTOR B.named()" + RIGHT_ARROW + "B",
//          "CLASS B",
//          "COMPILATION_UNIT test.dart",
//          "LIBRARY my_lib"), getPathString(path));
//    }
//  }
//
//  public void test_path_inConstructor_unnamed() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "library my_lib;",
//        "class A {}",
//        "class B {",
//        "  B() {",
//        "    A a = null; // 1",
//        "  }",
//        "}"));
//    doSearch("A {}");
//    assertThat(searchResults).hasSize(1);
//    {
//      SearchResult result = findSearchResult(
//          source,
//          SearchResultKind.TYPE_REFERENCE,
//          code.indexOf("A a = null; // 1"),
//          "A".length());
//      assertNotNull(result);
//      Element[] path = result.getPath();
//      assertEquals(makeSource(//
//          "CONSTRUCTOR B()" + RIGHT_ARROW + "B",
//          "CLASS B",
//          "COMPILATION_UNIT test.dart",
//          "LIBRARY my_lib"), getPathString(path));
//    }
//  }
//
//  public void test_path_inFunction() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "library my_lib;",
//        "class A {}",
//        "main(int p1, double p2) {",
//        "  A a = null; // 5",
//        "}"));
//    doSearch("A {}");
//    assertThat(searchResults).hasSize(1);
//    {
//      SearchResult result = findSearchResult(
//          source,
//          SearchResultKind.TYPE_REFERENCE,
//          code.indexOf("A a = null; // 5"),
//          "A".length());
//      assertNotNull(result);
//      Element[] path = result.getPath();
//      assertEquals(makeSource(//
//          "FUNCTION main(int p1, double p2)" + RIGHT_ARROW + "dynamic",
//          "COMPILATION_UNIT test.dart",
//          "LIBRARY my_lib"), getPathString(path));
//    }
//  }
//
//  public void test_path_inFunctionTypeAlias() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "library my_lib;",
//        "class A {}",
//        "typedef String F(A a); // 4",
//        ""));
//    doSearch("A {}");
//    assertThat(searchResults).hasSize(1);
//    {
//      SearchResult result = findSearchResult(
//          source,
//          SearchResultKind.TYPE_REFERENCE,
//          code.indexOf("A a); // 4"),
//          "A".length());
//      assertNotNull(result);
//      Element[] path = result.getPath();
//      assertEquals(makeSource(//
//          "FUNCTION_TYPE_ALIAS F(A a)" + RIGHT_ARROW + "String",
//          "COMPILATION_UNIT test.dart",
//          "LIBRARY my_lib"), getPathString(path));
//    }
//  }
//
//  public void test_path_inMethod() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "library my_lib;",
//        "class A {}",
//        "class B {",
//        "  m() {",
//        "    A a = null; // 3",
//        "  }",
//        "}"));
//    doSearch("A {}");
//    assertThat(searchResults).hasSize(1);
//    {
//      SearchResult result = findSearchResult(
//          source,
//          SearchResultKind.TYPE_REFERENCE,
//          code.indexOf("A a = null; // 3"),
//          "A".length());
//      assertNotNull(result);
//      Element[] path = result.getPath();
//      assertEquals(makeSource(//
//          "METHOD m()" + RIGHT_ARROW + "dynamic",
//          "CLASS B",
//          "COMPILATION_UNIT test.dart",
//          "LIBRARY my_lib"), getPathString(path));
//    }
//  }
//
//  public void test_potential_disabled() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  test(p) {}",
//        "}",
//        "main(A a, p) {",
//        "  a.test(1);",
//        "  p.test(2);",
//        "}"));
//    withPotential = false;
//    doSearch("test(p) {}");
//    assertThat(searchResults).hasSize(1);
//    assertHasResult("test(1);", "test".length(), SearchResultKind.METHOD_INVOCATION);
//  }
//
//  public void test_potential_field() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  var test; // declaration",
//        "}",
//        "main(p) {",
//        "  print(p.test); // get",
//        "  p.test = 1;",
//        "  p.test += 2;",
//        "}"));
//    doSearch("test; // declaration");
//    {
//      SearchResult searchResult = assertHasResult(
//          "test); // get",
//          "test".length(),
//          SearchResultKind.FIELD_READ);
//      assertTrue(searchResult.isPotential());
//    }
//    {
//      SearchResult searchResult = assertHasResult(
//          "test = 1;",
//          "test".length(),
//          SearchResultKind.FIELD_WRITE);
//      assertTrue(searchResult.isPotential());
//    }
//    {
//      SearchResult searchResult = assertHasResult(
//          "test += 2;",
//          "test".length(),
//          SearchResultKind.FIELD_READ_WRITE);
//      assertTrue(searchResult.isPotential());
//    }
//  }
//
//  public void test_potential_method() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  test(p) {}",
//        "}",
//        "main(A a, p) {",
//        "  a.test(1);",
//        "  p.test(2);",
//        "}"));
//    doSearch("test(p) {}");
//    assertThat(searchResults).hasSize(2);
//    {
//      SearchResult searchResult = assertHasResult(
//          "test(1);",
//          "test".length(),
//          SearchResultKind.METHOD_INVOCATION);
//      assertFalse(searchResult.isPotential());
//    }
//    {
//      SearchResult searchResult = assertHasResult(
//          "test(2);",
//          "test".length(),
//          SearchResultKind.METHOD_INVOCATION);
//      assertTrue(searchResult.isPotential());
//    }
//  }
//
//  public void test_topLevelVariable_explicit() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "int vvv = 1;",
//        "main() {",
//        "  vvv = 2;",
//        "  vvv += 3;",
//        "  print(vvv);",
//        "}"));
//    doSearch("vvv = 1;");
//    assertThat(searchResults).hasSize(4);
//    assertHasResult("vvv = 1;", SearchResultKind.VARIABLE_DECLARATION);
//    assertHasResult("vvv = 2", SearchResultKind.FIELD_WRITE);
//    assertHasResult("vvv += 3", SearchResultKind.FIELD_WRITE);
//    assertHasResult("vvv);", SearchResultKind.FIELD_READ);
//  }
//
//  public void test_topLevelVariable_implicit() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "int get vvv => 1;",
//        "void set vvv(x) {}",
//        "main() {",
//        "  vvv = 2;",
//        "  vvv += 3;",
//        "  print(vvv);",
//        "}"));
//    {
//      doSearch("vvv => 1;");
//      assertThat(searchResults).hasSize(3);
//      assertHasResult("vvv = 2", SearchResultKind.FIELD_WRITE);
//      assertHasResult("vvv += 3", SearchResultKind.FIELD_WRITE);
//      assertHasResult("vvv);", SearchResultKind.FIELD_READ);
//    }
//    {
//      doSearch("vvv(x) {}");
//      assertThat(searchResults).hasSize(3);
//      assertHasResult("vvv = 2", SearchResultKind.FIELD_WRITE);
//      assertHasResult("vvv += 3", SearchResultKind.FIELD_WRITE);
//      assertHasResult("vvv);", SearchResultKind.FIELD_READ);
//    }
//  }
//
//  public void test_typeReference_class() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  int va = 1;",
//        "  int vb = 1;",
//        "}"));
//    doSearch("int va = 1");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult("int va", SearchResultKind.TYPE_REFERENCE);
//    assertHasResult("int vb", SearchResultKind.TYPE_REFERENCE);
//  }
//
//  public void test_typeReference_functionType() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "typedef F();",
//        "main(F f) {",
//        "}"));
//    doSearch("F();");
//    assertThat(searchResults).hasSize(1);
//    assertHasResult("F f) {", SearchResultKind.TYPE_REFERENCE);
//  }
//
//  public void test_typeReference_typeVariable() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A<T> {",
//        "  T f;",
//        "  T m() => null;",
//        "}"));
//    doSearch("T>");
//    assertThat(searchResults).hasSize(2);
//    assertHasResult("T f;", SearchResultKind.TYPE_REFERENCE);
//    assertHasResult("T m()", SearchResultKind.TYPE_REFERENCE);
//  }
//
//  public void test_unknownMatchKind() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "fff() {}",
//        "main() {",
//        "  fff();",
//        "}"));
//    // do simulation
//    SearchResultConverter.test_simulateUnknownMatchKind = true;
//    try {
//      doSearch("fff();");
//    } finally {
//      SearchResultConverter.test_simulateUnknownMatchKind = false;
//    }
//    // no errors
//    serverListener.assertNoServerErrors();
//    // ...and no results too
//    assertThat(searchResults).isEmpty();
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
//  private void doSearch(String search) throws Exception {
//    searchResults.clear();
//    // prepare Element
//    int offset = code.indexOf(search);
//    Element element = findElement(offset);
//    // do request
//    if (element != null) {
//      final CountDownLatch latch = new CountDownLatch(1);
//      server.searchElementReferences(element, withPotential, new SearchResultsConsumer() {
//        @Override
//        public void computed(SearchResult[] _searchResults, boolean isLastResult) {
//          Collections.addAll(searchResults, _searchResults);
//          if (isLastResult) {
//            latch.countDown();
//          }
//        }
//      });
//      latch.await(600, TimeUnit.SECONDS);
//    }
//  }
//
//  private Element findElement(int offset) {
//    // ensure navigation data
//    server.subscribe(
//        contextId,
//        ImmutableMap.of(NotificationKind.NAVIGATION, SourceSet.EXPLICITLY_ADDED));
//    server.test_waitForWorkerComplete();
//    // find navigation region with Element
//    return serverListener.findNavigationElement(contextId, source, offset);
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
//
//  private String getPathString(Element[] path) {
//    String str = "";
//    for (Element element : path) {
//      if (!str.isEmpty()) {
//        str += "\n";
//      }
//      str += element.getKind() + " " + element.getName();
//      {
//        String parameters = element.getParameters();
//        if (!StringUtils.isEmpty(parameters)) {
//          str += parameters;
//        }
//      }
//      {
//        String parameters = element.getReturnType();
//        if (!StringUtils.isEmpty(parameters)) {
//          str += RIGHT_ARROW;
//          str += parameters;
//        }
//      }
//    }
//    return str;
//  }
}

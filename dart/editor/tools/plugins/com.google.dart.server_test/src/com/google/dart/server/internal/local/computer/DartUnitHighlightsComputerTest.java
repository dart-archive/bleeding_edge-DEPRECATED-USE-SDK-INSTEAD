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

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.source.Source;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.HighlightType;
import com.google.dart.server.ListSourceSet;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import org.apache.commons.lang3.StringUtils;

public class DartUnitHighlightsComputerTest extends AbstractLocalServerTest {
  private String contextId;
  private String testCode;
  private HighlightRegion[] regions;

  public void fail_BUILT_IN_deferred() throws Exception {
    prepareRegions(//
        "import 'dart:math' deferred as math;",
        "main() {",
        "  int deferred = 42;",
        "}");
    assertHasRegion("deferred as math", HighlightType.BUILT_IN);
    assertNoRegion("deferred = 42", HighlightType.BUILT_IN);
  }

  public void test_ANNOTATION_hasArguments() throws Exception {
    prepareRegions(//
        "class AAA {",
        "  const AAA(a, b, c);",
        "}",
        "@AAA(1, 2, 3) main() {}");
    assertHasStringRegion("@AAA(", HighlightType.ANNOTATION);
    assertHasRegion(") main", ")".length(), HighlightType.ANNOTATION);
  }

  public void test_ANNOTATION_noArguments() throws Exception {
    prepareRegions(//
        "const AAA = 42;",
        "@AAA main() {}");
    assertHasRegion("@AAA main()", HighlightType.ANNOTATION);
  }

  public void test_BUILT_IN_abstract() throws Exception {
    prepareRegions(//
        "abstract class A {}",
        "main() {",
        "  int abstract = 42;",
        "}");
    assertHasRegion("abstract class A {}", HighlightType.BUILT_IN);
    assertNoRegion("abstract = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_as() throws Exception {
    prepareRegions(//
        "import 'dart:math' as math;",
        "main() {",
        "  p as int;",
        "  int as = 42;",
        "}");
    assertHasRegion("as math", HighlightType.BUILT_IN);
    assertHasRegion("as int", HighlightType.BUILT_IN);
    assertNoRegion("as = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_export() throws Exception {
    prepareRegions(//
        "export 'foo.dart';",
        "main() {",
        "  var export = 42;",
        "}");
    assertHasRegion("export 'foo.dart'", HighlightType.BUILT_IN);
    assertNoRegion("export = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_external() throws Exception {
    prepareRegions(//
        "class A {",
        "  external A();",
        "  external aaa();",
        "}",
        "external main() {",
        "  int external = 42;",
        "}");
    assertHasRegion("external A()", HighlightType.BUILT_IN);
    assertHasRegion("external aaa()", HighlightType.BUILT_IN);
    assertHasRegion("external main()", HighlightType.BUILT_IN);
    assertNoRegion("external = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_factory() throws Exception {
    prepareRegions(//
        "class A {",
        "  factory A() {}",
        "}",
        "main() {",
        "  int factory = 42;",
        "}");
    assertHasRegion("factory A()", HighlightType.BUILT_IN);
    assertNoRegion("factory = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_get() throws Exception {
    prepareRegions(//
        "get aaa => 1;",
        "class A {",
        "  get bbb => 2;",
        "}",
        "main() {",
        "  int get = 42;",
        "}");
    assertHasRegion("get aaa =>", HighlightType.BUILT_IN);
    assertHasRegion("get bbb =>", HighlightType.BUILT_IN);
    assertNoRegion("get = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_hide() throws Exception {
    prepareRegions(//
        "import 'foo.dart' hide Foo;",
        "main() {",
        "  var hide = 42;",
        "}");
    assertHasRegion("hide Foo", HighlightType.BUILT_IN);
    assertNoRegion("hide = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_implements() throws Exception {
    prepareRegions(//
        "class A {}",
        "class B implements A {}",
        "main() {",
        "  int implements = 42;",
        "}");
    assertHasRegion("implements A {}", HighlightType.BUILT_IN);
    assertNoRegion("implements = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_import() throws Exception {
    prepareRegions(//
        "import 'foo.dart';",
        "main() {",
        "  var import = 42;",
        "}");
    assertHasRegion("import 'foo.dart'", HighlightType.BUILT_IN);
    assertNoRegion("import = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_library() throws Exception {
    prepareRegions(//
        "library a;",
        "",
        "main() {",
        "  var library = 42;",
        "}");
    assertHasRegion("library a", HighlightType.BUILT_IN);
    assertNoRegion("library = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_native() throws Exception {
    prepareRegions(//
        "class A native 'A_native' {}",
        "class B {",
        "  bbb() native 'bbb_native';",
        "}",
        "main() {",
        "  int native = 42;",
        "}");
    assertHasRegion("native 'A_", HighlightType.BUILT_IN);
    assertHasRegion("native 'bbb_", HighlightType.BUILT_IN);
    assertNoRegion("native = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_on() throws Exception {
    prepareRegions(//
        "main() {",
        "  try {",
        "  } on int catch (e) {",
        "  }",
        "  int on = 42;",
        "}");
    assertHasRegion("on int", HighlightType.BUILT_IN);
    assertNoRegion("on = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_operator() throws Exception {
    prepareRegions(//
        "class A {",
        "  operator +(x) => null;",
        "}",
        "main() {",
        "  int operator = 42;",
        "}");
    assertHasRegion("operator +(", HighlightType.BUILT_IN);
    assertNoRegion("operator = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_part() throws Exception {
    prepareRegions(//
        "part 'd';",
        "main() {",
        "  var part = 42;",
        "}");
    assertHasRegion("part 'd'", HighlightType.BUILT_IN);
    assertNoRegion("part = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_partOf() throws Exception {
    contextId = createContext("test");
    addSource(contextId, "/test_lib.dart", makeSource(//
        "library a;",
        "part 'test.dart';"));
    prepareRegions_forExistingContext(//
        "part of a;",
        "main() {",
        "  var part = 1;",
        "  var of = 2;",
        "}");
    assertHasStringRegion("part of", HighlightType.BUILT_IN);
    assertNoRegion("part = 1", HighlightType.BUILT_IN);
    assertNoRegion("of = 2", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_set() throws Exception {
    prepareRegions(//
        "set aaa(x) {}",
        "class A {",
        "  set bbb(x) {}",
        "}",
        "main() {",
        "  int set = 42;",
        "}");
    assertHasRegion("set aaa(", HighlightType.BUILT_IN);
    assertHasRegion("set bbb(", HighlightType.BUILT_IN);
    assertNoRegion("set = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_show() throws Exception {
    prepareRegions(//
        "import 'foo.dart' show Foo;",
        "main() {",
        "  var show = 42;",
        "}");
    assertHasRegion("show Foo", HighlightType.BUILT_IN);
    assertNoRegion("show = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_static() throws Exception {
    prepareRegions(//
        "class A {",
        "  static aaa;",
        "  static bbb() {}",
        "}",
        "main() {",
        "  int static = 42;",
        "}");
    assertHasRegion("static aaa;", HighlightType.BUILT_IN);
    assertHasRegion("static bbb()", HighlightType.BUILT_IN);
    assertNoRegion("static = 42", HighlightType.BUILT_IN);
  }

  public void test_BUILT_IN_typedef() throws Exception {
    prepareRegions(//
        "typedef A();",
        "",
        "main() {",
        "  int typedef = 42;",
        "}");
    assertHasRegion("typedef A()", HighlightType.BUILT_IN);
    assertNoRegion("typedef = 42", HighlightType.BUILT_IN);
  }

  public void test_CLASS() throws Exception {
    prepareRegions(//
        "class AAA {}",
        "AAA aaa;");
    assertHasRegion("AAA {}", HighlightType.CLASS);
    assertHasRegion("AAA aaa", HighlightType.CLASS);
  }

  public void test_CLASS_not_dynamic() throws Exception {
    prepareRegions("dynamic f() {}");
    assertNoRegion("dynamic f()", HighlightType.CLASS);
  }

  public void test_CLASS_not_void() throws Exception {
    prepareRegions("void f() {}");
    assertNoRegion("void f()", HighlightType.CLASS);
  }

  public void test_CONSTRUCTOR() throws Exception {
    prepareRegions(//
        "class AAA {",
        "  AAA() {}",
        "  AAA.name(p) {}",
        "}",
        "main() {",
        "  new AAA();",
        "  new AAA.name(42);",
        "}");
    assertHasRegion("name(p) {}", HighlightType.CONSTRUCTOR);
    assertHasRegion("name(42);", HighlightType.CONSTRUCTOR);
    assertNoRegion("AAA() {}", HighlightType.CONSTRUCTOR);
    assertNoRegion("AAA();", HighlightType.CONSTRUCTOR);
  }

  public void test_DYNAMIC_TYPE() throws Exception {
    prepareRegions(//
        "f() {}",
        "main(p) {",
        "  print(p);",
        "  var v1 = f();",
        "  int v2;",
        "  var v3 = 42;",
        "}");
    assertHasRegion("p);", HighlightType.DYNAMIC_TYPE);
    assertHasRegion("v1 =", HighlightType.DYNAMIC_TYPE);
    assertNoRegion("v2;", HighlightType.DYNAMIC_TYPE);
    assertNoRegion("v3 =", HighlightType.DYNAMIC_TYPE);
  }

  public void test_FIELD() throws Exception {
    prepareRegions(//
        "class A {",
        "  int aaa = 1;",
        "  int bbb = 2;",
        "  A([this.bbb = 3]);",
        "}",
        "main(A a) {",
        "  a.aaa = 4;",
        "  a.bbb = 5;",
        "}");
    assertHasRegion("aaa = 1", HighlightType.FIELD);
    assertHasRegion("bbb = 2", HighlightType.FIELD);
    assertHasRegion("bbb = 3", HighlightType.FIELD);
    assertHasRegion("aaa = 4", HighlightType.FIELD);
    assertHasRegion("bbb = 5", HighlightType.FIELD);
  }

  public void test_FIELD_STATIC() throws Exception {
    prepareRegions(//
        "class A {",
        "  static int aaa = 1;",
        "  static get bbb => null;",
        "  static set ccc(x) {}",
        "}",
        "main() {",
        "  A.aaa = 2;",
        "  A.bbb;",
        "  A.ccc = 3;",
        "}");
    assertHasRegion("aaa = 1", HighlightType.FIELD_STATIC);
    assertHasRegion("aaa = 2", HighlightType.FIELD_STATIC);
    assertHasRegion("bbb;", HighlightType.FIELD_STATIC);
    assertHasRegion("ccc = 3", HighlightType.FIELD_STATIC);
  }

  public void test_FUNCTION() throws Exception {
    prepareRegions(//
        "fff(p) {}",
        "main() {",
        "  fff(42);",
        "}");
    assertHasRegion("fff(p) {}", HighlightType.FUNCTION_DECLARATION);
    assertHasRegion("fff(42)", HighlightType.FUNCTION);
  }

  public void test_FUNCTION_TYPE_ALIAS() throws Exception {
    prepareRegions(//
        "typedef F(p);",
        "main(F f) {",
        "}");
    assertHasRegion("F(p)", HighlightType.FUNCTION_TYPE_ALIAS);
    assertHasRegion("F f)", HighlightType.FUNCTION_TYPE_ALIAS);
  }

  public void test_GETTER_DECLARATION() throws Exception {
    prepareRegions(//
        "get aaa => null;",
        "class A {",
        "  get bbb => null;",
        "}",
        "main(A a) {",
        "  aaa;",
        "  a.bbb;",
        "}");
    assertHasRegion("aaa => null", HighlightType.GETTER_DECLARATION);
    assertHasRegion("bbb => null", HighlightType.GETTER_DECLARATION);
    assertHasRegion("aaa;", HighlightType.FIELD_STATIC);
    assertHasRegion("bbb;", HighlightType.FIELD);
  }

  public void test_IDENTIFIER_DEFAULT() throws Exception {
    prepareRegions(//
        "main() {",
        "  aaa = 42;",
        "  bbb(84);",
        "  CCC ccc;",
        "}");
    assertHasRegion("aaa = 42", HighlightType.IDENTIFIER_DEFAULT);
    assertHasRegion("bbb(84)", HighlightType.IDENTIFIER_DEFAULT);
    assertHasRegion("CCC", HighlightType.IDENTIFIER_DEFAULT);
  }

  public void test_IMPORT_PREFIX() throws Exception {
    prepareRegions(//
        "import 'dart:math' as ma;",
        "main(F f) {",
        "  ma.max(1, 2);",
        "}");
    assertHasRegion("ma;", HighlightType.IMPORT_PREFIX);
    assertHasRegion("ma.max", HighlightType.IMPORT_PREFIX);
  }

  public void test_KEYWORD_void() throws Exception {
    prepareRegions("void main() {}");
    assertHasRegion("void ", HighlightType.KEYWORD);
  }

  public void test_LITERAL_BOOLEAN() throws Exception {
    prepareRegions("var V = true;");
    assertHasRegion("true;", HighlightType.LITERAL_BOOLEAN);
  }

  public void test_LITERAL_DOUBLE() throws Exception {
    prepareRegions("var V = 4.2;");
    assertHasStringRegion("4.2", HighlightType.LITERAL_DOUBLE);
  }

  public void test_LITERAL_INTEGER() throws Exception {
    prepareRegions("var V = 42;");
    assertHasStringRegion("42", HighlightType.LITERAL_INTEGER);
  }

  public void test_LITERAL_STRING() throws Exception {
    prepareRegions("var V = 'abc';");
    assertHasStringRegion("'abc'", HighlightType.LITERAL_STRING);
  }

  public void test_LOCAL_VARIABLE() throws Exception {
    prepareRegions(//
        "main() {",
        "  int vvv = 0;",
        "  vvv;",
        "  vvv = 1;",
        "}");
    assertHasRegion("vvv = 0;", HighlightType.LOCAL_VARIABLE_DECLARATION);
    assertHasRegion("vvv;", HighlightType.LOCAL_VARIABLE);
    assertHasRegion("vvv = 1;", HighlightType.LOCAL_VARIABLE);
  }

  public void test_METHOD() throws Exception {
    prepareRegions(//
        "class A {",
        "  aaa() {}",
        "  static bbb() {}",
        "}",
        "main(A a) {",
        "  a.aaa();",
        "  a.aaa;",
        "  A.bbb();",
        "  A.bbb;",
        "}");
    assertHasRegion("aaa() {}", HighlightType.METHOD_DECLARATION);
    assertHasRegion("bbb() {}", HighlightType.METHOD_DECLARATION_STATIC);
    assertHasRegion("aaa();", HighlightType.METHOD);
    assertHasRegion("aaa;", HighlightType.METHOD);
    assertHasRegion("bbb();", HighlightType.METHOD_STATIC);
    assertHasRegion("bbb;", HighlightType.METHOD_STATIC);
  }

  public void test_METHOD_useBestType() throws Exception {
    prepareRegions(//
        "main(p) {",
        "  if (p is List) {",
        "    p.forEach(null);",
        "  }",
        "}");
    assertHasRegion("forEach(", HighlightType.METHOD);
  }

  public void test_PARAMETER() throws Exception {
    prepareRegions(//
        "main(int p) {",
        "  p;",
        "  p = 42;",
        "}");
    assertHasRegion("p) {", HighlightType.PARAMETER);
    assertHasRegion("p;", HighlightType.PARAMETER);
    assertHasRegion("p = 42;", HighlightType.PARAMETER);
  }

  public void test_SETTER_DECLARATION() throws Exception {
    prepareRegions(//
        "set aaa(x) {}",
        "class A {",
        "  set bbb(x) {}",
        "}",
        "main(A a) {",
        "  aaa = 1;",
        "  a.bbb = 2;",
        "}");
    assertHasRegion("aaa(x)", HighlightType.SETTER_DECLARATION);
    assertHasRegion("bbb(x)", HighlightType.SETTER_DECLARATION);
    assertHasRegion("aaa = 1;", HighlightType.FIELD_STATIC);
    assertHasRegion("bbb = 2;", HighlightType.FIELD);
  }

  public void test_TOP_LEVEL_VARIABLE() throws Exception {
    prepareRegions(//
        "var VVV = 0;",
        "main() {",
        "  print(VVV);",
        "  VVV = 1;",
        "}");
    assertHasRegion("VVV = 0", HighlightType.TOP_LEVEL_VARIABLE);
    assertHasRegion("VVV);", HighlightType.FIELD_STATIC);
    assertHasRegion("VVV = 1", HighlightType.FIELD_STATIC);
  }

  public void test_TYPE_NAME_DYNAMIC() throws Exception {
    prepareRegions(//
        "dynamic main() {",
        "  var dynamic = 42;",
        "}");
    assertHasRegion("dynamic main()", HighlightType.TYPE_NAME_DYNAMIC);
    assertNoRegion("dynamic main()", HighlightType.IDENTIFIER_DEFAULT);
    assertNoRegion("dynamic = 42", HighlightType.TYPE_NAME_DYNAMIC);
  }

  public void test_TYPE_PARAMETER() throws Exception {
    prepareRegions(//
        "class A<T> {",
        "  T fff;",
        "  T mmm(T p) => null;",
        "}");
    assertHasRegion("T> {", HighlightType.TYPE_PARAMETER);
    assertHasRegion("T fff;", HighlightType.TYPE_PARAMETER);
    assertHasRegion("T mmm(", HighlightType.TYPE_PARAMETER);
    assertHasRegion("T p)", HighlightType.TYPE_PARAMETER);
  }

  private void assertHasRegion(HighlightRegion expected) {
    if (hasRegion(expected)) {
      return;
    }
    fail("Expected to find highlight region:\n" + expected + "\n\nin\n\n"
        + StringUtils.join(regions, "\n"));
  }

  private void assertHasRegion(String search, HighlightType type) {
    HighlightRegion expected = createRegion(search, type);
    assertHasRegion(expected);
  }

  private void assertHasRegion(String search, int length, HighlightType type) {
    HighlightRegion expected = createRegion(search, length, type);
    assertHasRegion(expected);
  }

  private void assertHasStringRegion(String search, HighlightType type) {
    HighlightRegion expected = createRegion(search, search.length(), type);
    assertHasRegion(expected);
  }

  private void assertNoRegion(HighlightRegion expected) {
    if (!hasRegion(expected)) {
      return;
    }
    fail("Unexpected highlight region found:\n" + expected + "\n\nin\n\n"
        + StringUtils.join(regions, "\n"));
  }

  private void assertNoRegion(String search, HighlightType type) {
    HighlightRegion expected = createRegion(search, type);
    assertNoRegion(expected);
  }

  private HighlightRegion createRegion(String search, HighlightType type) {
    int offset = testCode.indexOf(search);
    assertTrue(search, offset >= 0);
    int identifierLength = 0;
    while (identifierLength < search.length()) {
      char c = search.charAt(identifierLength);
      if (identifierLength == 0 && c == '@') {
        identifierLength++;
        continue;
      }
      if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')) {
        break;
      }
      identifierLength++;
    }
    return new HighlightRegionImpl(offset, identifierLength, type);
  }

  private HighlightRegion createRegion(String search, int length, HighlightType type) {
    int offset = testCode.indexOf(search);
    assertTrue(search, offset >= 0);
    return new HighlightRegionImpl(offset, length, type);
  }

  private boolean hasRegion(HighlightRegion region) {
    return hasRegion(region.getOffset(), region.getLength(), region.getType());
  }

  private boolean hasRegion(int offset, int length, HighlightType type) {
    assertNotNull(regions);
    for (HighlightRegion region : regions) {
      if (region.getOffset() == offset && region.getLength() == length && region.getType() == type) {
        return true;
      }
    }
    return false;
  }

  private void prepareRegions(String... lines) {
    contextId = createContext("test");
    prepareRegions_forExistingContext(lines);
  }

  private void prepareRegions_forExistingContext(String... lines) {
    testCode = makeSource(lines);
    Source testSource = addSource(contextId, "/test.dart", testCode);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.HIGHLIGHTS, ListSourceSet.create(testSource)));
    server.test_waitForWorkerComplete();
    regions = serverListener.getHighlightRegions(contextId, testSource);
  }
}

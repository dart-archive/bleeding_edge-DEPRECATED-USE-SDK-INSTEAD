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
package com.google.dart.engine.services.completion;

import com.google.dart.engine.source.Source;

import java.util.ArrayList;

public class CompletionLibraryTests extends CompletionTestCase {

  public void test_export_ignoreIfThisLibraryExports() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    test(//
        src(//
            "export 'dart:math';",
            "libFunction() {};",
            "main() {",
            "  !1",
            "}"),
        sources,
        "1-cos",
        "1+libFunction");
  }

  public void test_export_showIfImportLibraryWithExport() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(addSource(//
        "/lib.dart",
        src(//
            "library lib;",
            "export 'dart:math' hide sin;",
            "libFunction() {};",
            "")));
    test(//
        src(//
            "import 'lib.dart' as p;",
            "main() {",
            "  p.!1",
            "}"),
        sources,
        "1+cos",
        "1-sin",
        "1+libFunction");
  }

  public void test_importPrefix_hideCombinator() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    test(//
        src(//
            "import 'dart:math' as math hide PI;",
            "main() {",
            "  math.!1",
            "}"),
        sources,
        "1-PI",
        "1+LN10");
  }

  public void test_importPrefix_showCombinator() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    test(//
        src(//
            "import 'dart:math' as math show PI;",
            "main() {",
            "  math.!1",
            "}"),
        sources,
        "1+PI",
        "1-LN10");
  }

  public void test_noPrivateElement_otherLibrary_constructor() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(addSource(//
        "/lib.dart",
        src(//
            "library lib;",
            "class A {",
            "  A.c();",
            "  A._c();",
            "}",
            "")));
    test(//
        src(//
            "import 'lib.dart';",
            "main() {",
            "  new A.!1",
            "}"),
        sources,
        "1-_c",
        "1+c");
  }

  public void test_noPrivateElement_otherLibrary_member() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(addSource(//
        "/lib.dart",
        src(//
            "library lib;",
            "class A {",
            "  var f;",
            "  var _f;",
            "}",
            "")));
    test(//
        src(//
            "import 'lib.dart';",
            "main(A a) {",
            "  a.!1",
            "}"),
        sources,
        "1-_f",
        "1+f");
  }

  public void test001() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(addSource(//
        "/firth.dart",
        src(//
            "library firth;",
            "class SerializationException {",
            "  const SerializationException();",
            "}")));
    test(//
        src(//
            "import 'firth.dart';",
            "main() {",
            "throw new Seria!1lizationException();}"),
        sources,
        "1+SerializationException");
  }

  public void test002() throws Exception {
    // Type propagation.
    // TODO Include corelib analysis (this works in the editor)
//    test("t2() {var q=[0],z=q.!1length;q.!2clear();}", "1+length", "1+isEmpty", "2+clear");
  }

  public void test003() throws Exception {
    // TODO Include corelib analysis
//    test("class X{var q; f() {q.!1a!2}}", "1+end", "2+abs", "2-end");
  }

  public void test004() throws Exception {
    // TODO Include corelib analysis
    // TODO Use another library, dart:json is gone.
    // Resolving dart:html takes between 2.5s and 30s; json, about 0.12s
//    test(
//        src(
//            "library foo;",
//            "import 'dart:json' as json;",
//            "class JsonParserX{}",
//            "f1() {var x=new json.!1}",
//            "f2() {var x=new json.JsonPa!2}",
//            "f3() {var x=new json.JsonParser!3}"),
//        "1+JsonParser",
//        "1-JsonParserX",
//        "2+JsonParser",
//        "2-JsonParserX",
//        "3+JsonParser",
//        "3-JsonParserX");
  }

  public void test005() throws Exception {
    // TODO Enable after type propagation is implemented. Not yet.
    // TODO Include corelib analysis
//    test("var PHI;main(){PHI=5.3;PHI.abs().!1 Object x;}", "1+abs");
  }

  public void test006() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    // Exercise import and export handling.
    // Libraries are defined in partial order of increasing dependency.
    sources.add(addSource(//
        "/exp2a.dart",
        src(//
            "library exp2a;",
            "e2a() {}",
            "")));
    sources.add(addSource(//
        "/exp1b.dart",
        src(//
            "library exp1b;",
            "e1b() {}",
            "")));
    sources.add(addSource(//
        "/exp1a.dart",
        src(//
            "library exp1a;",
            "export 'exp1b.dart';",
            "e1a() {}",
            "")));
    sources.add(addSource(//
        "/imp1.dart",
        src(//
            "library imp1;",
            "export 'exp1a.dart';",
            "i1() {}",
            "")));
    sources.add(addSource(//
        "/imp2.dart",
        src(//
            "library imp2;",
            "export 'exp2a.dart';",
            "i2() {}",
            "")));
    test(//
        src(//
            "import 'imp1.dart';",
            "import 'imp2.dart';",
            "main() {!1",
            "  i1();",
            "  i2();",
            "  e1a();",
            "  e1b();",
            "  e2a();",
            "}"),
        sources,
        "1+i1",
        "1+i2",
        "1+e1a",
        "1+e2a",
        "1+e1b");
  }

  public void test007() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    // Exercise import and export handling.
    // Libraries are defined in partial order of increasing dependency.
    sources.add(addSource(//
        "/l1.dart",
        src(//
            "library l1;",
            "var _l1t; var l1t;",
            "")));
    test(//
        src(//
            "import 'l1.dart';",
            "main() {",
            "  var x = l!1",
            "  var y = _!2",
            "}"),
        sources,
        "1+l1t",
        "1-_l1t",
        "2-_l1t");
  }

  public void test008() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    // Check private library exclusion
    sources.add(addSource(//
        "/public.dart",
        src(//
            "library public;",
            "class NonPrivate {",
            "  void publicMethod() {",
            "  }",
            "}")));
    sources.add(addSource(//
        "/private.dart",
        src(//
            "library _private;",
            "import 'public.dart';",
            "class Private extends NonPrivate {",
            "  void privateMethod() {",
            "  }",
            "}")));
    test(//
        src(//
            "import 'private.dart';",
            "import 'public.dart';",
            "class Test {",
            "  void test() {",
            "    NonPrivate x = new NonPrivate();",
            "    x.!1 //publicMethod but not privateMethod should appear",
            "  }",
            "}"),
        sources,
        "1-privateMethod",
        "1+publicMethod");
  }

  public void test009() throws Exception {
    ArrayList<Source> sources = new ArrayList<Source>();
    // Exercise library prefixes.
    sources.add(addSource(//
        "/lib.dart",
        src(//
            "library lib;",
            "int X = 1;",
            "void m(){}",
            "class Y {}",
            "")));
    test(//
        src(//
            "import 'lib.dart' as Q;",
            "void a() {",
            "  var x = Q.!1",
            "}",
            "void b() {",
            "  var x = [Q.!2]",
            "}",
            "void c() {",
            "  var x = new List([Q.!3])",
            "}",
            "void d() {",
            "  new Q.!4",
            "}"),
        sources,
        "1+X",
        "1+m",
        "1+Y",
        "2+X",
        "2+m",
        "2+Y",
        "3+X",
        "3+m",
        "3+Y",
        "4+Y",
        "4-m",
        "4-X");
  }

}

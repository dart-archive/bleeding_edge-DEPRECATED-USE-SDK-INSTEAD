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

public class CompletionLibraryTests extends CompletionTestCase {

  public void test001() throws Exception {
    addLib(//
        "/firth.dart",
        src(//
            "library firth;",
            "class SerializationException {",
            "  const SerializationException();",
            "}"));
    test(//
        src(//
            "import 'firth.dart';",
            "main() {",
            "throw new Seria!1lizationException();}"),
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
    // Exercise import and export handling.
    // Libraries are define in partial order of increasing dependency.
    addLib(//
        "/exp2a.dart",
        src(//
            "library exp2a;",
            "e2a() {}",
            ""));
    addLib(//
        "/exp1b.dart",
        src(//
            "library exp1b;",
            "e1b() {}",
            ""));
    addLib(//
        "/exp1a.dart",
        src(//
            "library exp1a;",
            "export 'exp1b.dart';",
            "e1a() {}",
            ""));
    addLib(//
        "/imp1.dart",
        src(//
            "library imp1;",
            "export 'exp1a.dart';",
            "i1() {}",
            ""));
    addLib(//
        "/imp2.dart",
        src(//
            "library imp2;",
            "export 'exp2a.dart';",
            "i2() {}",
            ""));
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
        "1+i1",
        "1+i2",
        "1+e1a",
        "1+e2a",
        "1+e1b");
  }
}

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

/**
 * Short, specific code completion tests.
 */
public class CompletionTests extends CompletionTestCase {

  public void test000() throws Exception {
    String source = src(//
        "class A {",
        "  int x;",
        "  A() : this.x = 1;",
        "  A.b() : this();",
        "  A.c() : this.b();",
        "  g() => new A.c();",
        "  h() { A q; q.!1x; }",
        "}");
    test(source, "1+x");
  }

  public void test001() throws Exception {
    String source = src(//
        "void r(var v) {",
        "  v.!1to!2",
        "}");
    test(source, "1+toString", "2+toString");
  }

  public void test002() throws Exception {
    String source = src(//
        "void r(var vim) {",
        "  v!1.toStri",
        "}");
    test(source, "1+vim");
  }

  public void test003() throws Exception {
    String source = src(//
        "class A {",
        "  int a() => 3;",
        "  int b() => this.1!a();",
        "}");
    test(source, "1+a");
  }

  public void test004() throws Exception {
    String source = src(//
        "class A {",
        "  int x;",
        "  A() : this.!1x = 1;",
        "  A.b() : this();",
        "  A.c() : this.!2b();",
        "  g() => new A.!3c();",
        "}");
    test(source, "1+x", "2+b", "3+c");
  }
}

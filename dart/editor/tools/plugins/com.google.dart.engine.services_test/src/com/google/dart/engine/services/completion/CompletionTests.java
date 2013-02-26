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
 * Short, specific code completion tests. 29 ATM.
 */
public class CompletionTests extends CompletionTestCase {

  public void test001() throws Exception {
    String source = src(//
        "void r1(var v) {",
        "  v.!1toString!2().!3hash!4Code",
        "}");
    test(source, "1+toString", "2+toString", "3+hashCode", "3+toString", "4+hashCode", "4-toString");
  }

  public void test002() throws Exception {
    String source = src(//
        "void r2(var vim) {",
        "  v!1.toString()",
        "}");
    test(source, "1+vim");
  }

  public void test003() throws Exception {
    String source = src(//
        "class A {",
        "  int a() => 3;",
        "  int b() => this.!1a();",
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

  public void test005() throws Exception {
    String source = src(//
        "class A {}",
        "void rr(var vim) {",
        "  var !1vq = v!2.toString();",
        "  var vf;",
        "  v!3.toString();",
        "}");
    test(
        source,
        "1+A",
        "1+vim",
        "1-vq",
        "1-vf",
        "2-A",
        "2+vim",
        "2-vf",
        "2-vq",
        "3+vf",
        "3+vq",
        "3+vim",
        "3-A");
  }

  public void testCommentSnippets001() throws Exception {
    test(
        "class X {static final num MAX = 0;num yc,xc;mth() {xc = yc = MA!1X;x!2c.abs();num f = M!3AX;}}",
        "1+MAX",
        "2+xc",
        "3+MAX");
  }

  public void testCommentSnippets002() throws Exception {
    test(
        "class Y {String x='hi';mth() {x.l!1ength;int n = 0;x!2.codeUnitAt(n!3);}}",
        "1+length",
        "2+x",
        "3+n");
  }

  public void testCommentSnippets004() throws Exception {
    test(
        "class A {!1int x; !2mth() {!3int y = this.!5x!6;}}class B{}",
        "1+A",
        "2+B",
        "3+x",
        "3-y",
        "5+mth",
        "6+x");
  }
}

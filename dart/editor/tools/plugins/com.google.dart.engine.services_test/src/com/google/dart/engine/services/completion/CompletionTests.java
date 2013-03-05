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

  public void test006() throws Exception {
    String source = src(//
        "void r2(var vim, {va: 2, b: 3}) {",
        "  v!1.toString()",
        "}");
    test(source, "1+va", "1-b");
  }

  public void test007() throws Exception {
    String source = src(//
        "void r2(var vim, [va: 2, b: 3]) {",
        "  v!1.toString()",
        "}");
    test(source, "1+va", "1-b");
  }

  public void test008() throws Exception {
    String source = src(//
        "!1class Aclass {}",
        "class Bclass !2extends!3 !4Aclass {}",
        "!5typedef Ctype = !6Bclass with !7Aclass;",
        "class Dclass extends !8Ctype {}",
        "!9abstract class Eclass implements Dclass,!C Ctype, Bclass {}",
        "class Fclass extends Bclass !Awith !B Eclass {}");
    test(
        source,
        "1+class",
        "1-implements",
        "1-extends",
        "1-with",
        "2+extends",
        "3+extends",
        "4+Aclass",
        "4-Bclass",
        "5+typedef",
        "6+Bclass",
        "6-Ctype",
        "7+Aclass",
        "7-Bclass",
        "8+Ctype",
        "9+abstract",
        "A+with",
        "B+Eclass",
        "B-Dclass",
        "B-Ctype",
        "C+Bclass",
        "C-Eclass");
  }

  public void test009() throws Exception {
    String source = src(//
        "class num{}",
        "typedef !1dy!2namic TestFn1();",
        "typedef !3vo!4id TestFn2();",
        "typ!7edef !5n!6");
    test(
        source,
        "1+void",
        "1+TestFn2",
        "2+dynamic",
        "2-void",
        "3+dynamic",
        "4+void",
        "4-dynamic",
        "5+TestFn2",
        "6+num",
        "7+typedef");
  }

  public void test010() throws Exception {
    String source = src(//
        "class String{}class List{}",
        "class test !8<!1t !2 !3extends String,!4 List,!5 !6>!7 {}",
        "class tezetst !9<!BString,!C !DList>!A {}");
    test(
        source,
        "1+String",
        "1+List",
        "1-test",
        "2-String",
        "2-test",
        "3+extends",
        "4+tezetst",
        "4-test",
        "5+String",
        "6+List",
        "7-List",
        "8-List",
        "9-String",
        "A-String",
        "B+String",
        "C+List",
        "C-tezetst",
        "D+List",
        "D+test");
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

  public void testCommentSnippets006() throws Exception {
    test("class B1 {B1();x(){}}class B2 extends B1 {B2() { super.!2x();}}", "2+x");
  }

  public void testCommentSnippets007() throws Exception {
    test(
        "class C {mth(Map x, !1) {}mtf(!2, Map x) {}m() {for (in!3t i=0; i<5; i++); A!4 x;}}class int{}class Arrays{}class bool{}",
        "1+bool",
        "2+bool",
        "3+int",
        "4+Arrays");
  }

  public void testCommentSnippets008() throws Exception {
    test("class Date{}final num M = Dat!1", "1+Date");
  }

  public void testCommentSnippets009() throws Exception {
    // space, char, eol are important
    test(
        "class Map{}class Maps{}class x extends!5 !2M!3 !4implements!6 !1\n{}",
        "1+Map",
        "2+Maps",
        "3+Maps",
        "4-Maps",
        "4+implements",
        "5-Maps",
        "6-Map",
        "6+implements");
  }

  public void testCommentSnippets014() throws Exception {
    // trailing space is important
    test("class num{}typedef n!1 ;", "1+num");
  }

  public void testCommentSnippets027() throws Exception {
    test(
        "class String{}class List{}class test <X extends !1String!2> {}",
        "1+List",
        "2+String",
        "2-List");
  }
}

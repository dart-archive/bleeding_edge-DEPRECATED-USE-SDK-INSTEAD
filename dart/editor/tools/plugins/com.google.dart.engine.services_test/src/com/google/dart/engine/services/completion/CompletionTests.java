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

  public void fail_test034() throws Exception {
    // TODO(scheglov) decide what to do with Type for untyped field (not supported by the new store)
    // test analysis of untyped fields and top-level vars
    test(
        src(
            "var topvar;",
            "class Top {top(){}}",
            "class Left extends Top {left(){}}",
            "class Right extends Top {right(){}}",
            "t1() {",
            "  topvar = new Left();",
            "}",
            "t2() {",
            "  topvar = new Right();",
            "}",
            "class A {",
            "  var field;",
            "  a() {",
            "    field = new Left();",
            "  }",
            "  b() {",
            "    field = new Right();",
            "  }",
            "  test() {",
            "    topvar.!1top();",
            "    field.!2top();",
            "  }",
            "}"),
        "1+top",
        "2+top");
  }

  public void fail_test036() throws Exception {
    // TODO(scheglov) decide what to do with Type for untyped field (not supported by the new store)
    // test analysis of untyped fields and top-level vars
    test(
        src(
            "class A1 {",
            "  var field;",
            "  A1() : field = 0;",
            "  q() {",
            "    A1 a = new A1();",
            "    a.field.!1",
            "  }",
            "}",
            "main() {",
            "  A1 a = new A1();",
            "  a.field.!2",
            "}"),
        "1+round",
        "2+round");
  }

  public void test_classMembers_inGetter() throws Exception {
    test("class A { var fff; get z {ff!1}}", "1+fff");
  }

  public void test001() throws Exception {
    String source = src(//
        "void r1(var v) {",
        "  v.!1toString!2().!3hash!4Code",
        "}");
    test(
        source,
        "1+toString",
        "1-==",
        "2+toString",
        "3+hashCode",
        "3+toString",
        "4+hashCode",
        "4-toString");
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
        "1-A",
        "1-vim",
        "1+vq",
        "1-vf",
        "1-this",
        "1-void",
        "1-null",
        "1-false",
        "2-A",
        "2+vim",
        "2-vf",
        "2-vq",
        "2-this",
        "2-void",
        "2-null",
        "2-false",
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
    // keywords
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
    // keywords
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

  public void test011() throws Exception {
    // name generation with conflicts
    test("r2(var object, Object object1, Object !1);", "1+object2");
  }

  public void test012() throws Exception {
    // reserved words
    test(src(//
        "class X {",
        "f() {",
        "  g(!1var!2 z) {!3true.!4toString();};",
        " }",
        "}"), "1+var", "1+dynamic", "1-f", "2+var", "2-dynamic", "3+false", "3+true", "4+toString");
  }

  public void test013() throws Exception {
    // conditions & operators
    test(src(//
        "class Q {",
        "  bool x;",
        "  List zs;",
        "  int k;",
        "  var a;",
        "  mth() {",
        "    while (!1x !9); ",
        "    do{} while(!2x !8);",
        "    for(z in !3zs) {}",
        "    switch(!4k) {case 1:{!0}}",
        "    try {",
        "    } on !5Object catch(a){}",
        "    if (!7x !6) {} else {};",
        "  }",
        "}"), "1+x", "2+x", "3+zs", "4+k", "5+Q", "5-a", "6+==", "7+x", "8+==", "9+==", "0+k");
  }

  public void test014() throws Exception {
    // keywords
    test(
        src(//
            "class Q {",
            "  bool x;",
            "  List zs;",
            "  int k;",
            "  !Dvar a;",
            "  !Evoid mth() {",
            "    !1while (z) { !Gcontinue; }; ",
            "    !2do{ !Hbreak; } !3while(x);",
            "    !4for(z !5in zs) {}",
            "    !6for (int i; i < 3; i++);",
            "    !7switch(k) {!8case 1:{} !9default:{}}",
            "    !Atry {",
            "    } !Bon Object !Ccatch(a){}",
            "    !Fassert true;",
            "    !Jif (x) {} !Kelse {};",
            "    !Lreturn;",
            "  }",
            "}"),
        "1+while",
        "2+do",
        "3+while",
        "4+for",
        "5+in",
        "6+for",
        "7+switch",
        "8+case",
        "9+default",
        "A+try",
        "B+on",
        "C+catch",
        "D+var",
        "E+void",
        "F+assert",
        "G+continue",
        "H+break",
        "J+if",
        "K+else",
        "L+return");
  }

  public void test015() throws Exception {
    // operators in function
    test("f(a,b,c) => a + b * c !1;", "1+==");
  }

  public void test016() throws Exception {
    // operators in return
    test("class X {dynamic f(a,b,c) {return a + b * c !1;}}", "1+==");
  }

  public void test017() throws Exception {
    // keywords
    test(
        src(//
            "!1library foo;",
            "!2import 'x' !5as r;",
            "!3export '!8uri' !6hide Q !7show X;",
            "!4part 'x';"),
        "1+library",
        "2+import",
        "3+export",
        "4+part",
        "5+as",
        "6+hide",
        "7+show",
        "8-null");
  }

  public void test018() throws Exception {
    // keywords
    test("!1part !2of foo;", "1+part", "2+of");
  }

  public void test019() throws Exception {
    test(src(//
        "var truefalse = 0;",
        "var falsetrue = 1;",
        "main() {",
        "  var foo = true!1",
        "}"), "1+true", "1+truefalse", "1-falsetrue");
  }

  public void test020() throws Exception {
    test("var x = null.!1", "1+toString");
  }

  public void test021() throws Exception {
    test("var x = .!1", "1-toString");
  }

  public void test022() throws Exception {
    test("var x = .!1;", "1-toString");
  }

  public void test023() throws Exception {
    test(//
        src(//
            "class Map{getKeys(){}}",
            "class X {",
            "  static x1(Map m) {",
            "    m.!1getKeys;",
            "  }",
            "  x2(Map m) {",
            "    m.!2getKeys;",
            "  }",
            "}"),
        "1+getKeys",
        "2+getKeys");
  }

  public void test024() throws Exception {
    test(//
        src(// Note lack of semicolon following completion location
            "class List{factory List.from(Iterable other) {}}",
            "class F {",
            "  f() {",
            "    new List.!1",
            "  }",
            "}"),
        "1+from");
  }

  public void test025() throws Exception {
    test(
        src(
            "class R {",
            "  static R _m;",
            "  static R m;",
            "  f() {",
            "    var a = !1m;",
            "    var b = _!2m;",
            "    var c = !3g();",
            "  }",
            "  static g() {",
            "    var a = !4m;",
            "    var b = _!5m;",
            "    var c = !6g();",
            "  }",
            "}",
            "class T {",
            "  f() {",
            "    R x;",
            "    x.!7g();",
            "    x.!8m;",
            "    x._!9m;",
            "  }",
            "  static g() {",
            "    var q = R._!Am;",
            "    var g = R.!Bm;",
            "    var h = R.!Cg();",
            "  }",
            "  h() {",
            "    var q = R._!Dm;",
            "    var g = R.!Em;",
            "    var h = R.!Fg();",
            "  }",
            "}"),
        "1+m",
        "2+_m",
        "3+g",
        "4+m",
        "5+_m",
        "6+g",
        "7-g",
        "8-m",
        "9-_m",
        "A+_m",
        "B+m",
        "C+g",
        "D+_m",
        "E+m",
        "F+g");
  }

  public void test026() throws Exception {
    test("var aBcD; var x=ab!1", "1+aBcD");
  }

  public void test027() throws Exception {
    test("m(){try{}catch(eeee,ssss){s!1}", "1+ssss");
  }

  public void test028() throws Exception {
    test("m(){var isX=3;if(is!1)", "1+isX");
  }

  public void test029() throws Exception {
    test("m(){[1].forEach((x)=>!1x);}", "1+x");
  }

  public void test030() throws Exception {
    test("n(){[1].forEach((x){!1});}", "1+x");
  }

  public void test031() throws Exception {
    test(
        "class Caster {} m() {try {} on Cas!1ter catch (CastBlock) {!2}}",
        "1+Caster",
        "1-CastBlock",
        "2+Caster",
        "2+CastBlock");
  }

  public void test032() throws Exception {
    test(
        src(
            "const ONE = 1;",
            "const ICHI = 10;",
            "const UKSI = 100;",
            "const EIN = 1000;",
            "m() {",
            "  int x;",
            "  switch (x) {",
            "    case !3ICHI:",
            "    case UKSI:",
            "    case EIN!2:",
            "    case ONE!1: return;",
            "    default: return;",
            "  }",
            "}"),
        "1+ONE",
        "1-UKSI",
        "2+EIN",
        "2-ICHI",
        "3+ICHI",
        "3+UKSI",
        "3+EIN",
        "3+ONE");
  }

  public void test033() throws Exception {
    test(
        "class A{}class B extends A{b(){}}class C implements A {c(){}}class X{x(){A f;f.!1}}",
        "1+b",
        "1-c");
  }

  public void test035() throws Exception {
    // test analysis of untyped fields and top-level vars
    test("class Y {final x='hi';mth() {x.!1length;}}", "1+length");
  }

  public void test037() throws Exception {
    test(src(//
        "class HttpServer{}",
        "class HttpClient{}",
        "main() {",
        "  new HtS!1",
        "}"), "1+HttpServer", "1-HttpClient");
  }

  public void test038() throws Exception {
    test(src(//
        "class X {",
        "  x(){}",
        "}",
        "class Y {",
        "  y(){}",
        "}",
        "class A<Z extends X> {",
        "  Y ay;",
        "  Z az;",
        "  A(this.ay, this.az) {",
        "    ay.!1y;",
        "    az.!2x;",
        "  }",
        "}"), "1+y", "1-x", "2+x", "2-y");
  }

  public void test039() throws Exception {
    // test analysis of untyped fields and top-level vars
    test("class X{}var x = null as !1X;", "1+X", "1-void");
  }

  public void test040() throws Exception {
    // test arg lists with named params
    test("m(){f(a, b, {x1, x2, y}) {};f(1, 2, !1)!2;}", "1+x1", "2-x2");
  }

  public void test041() throws Exception {
    // test arg lists with named params
    test("m(){f(a, b, {x1, x2, y}) {};f(1, 2, !1", "1+x1", "1+x2", "1+y");
  }

  public void test042() throws Exception {
    // test arg lists with named params
    test("m(){f(a, b, {x1, x2, y}) {};f(1, 2, !1;!2", "1+x1", "1+x2", "2-y");
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

  public void testCommentSnippets005() throws Exception {
    test(
        "class Date { static Date JUN, JUL;}class X { m() { return Da!1te.JU!2L; }}",
        "1+Date",
        "2+JUN",
        "2+JUL");
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

  public void testCommentSnippets010() throws Exception {
    // space, char, eol are important
    test("class Map{}class x implements !1{}", "1+Map");
  }

  public void testCommentSnippets011() throws Exception {
    // space, char, eol are important
    test("class Map{}class x implements M!1{}", "1+Map");
  }

  public void testCommentSnippets012() throws Exception {
    // space, char, eol are important
    test("class Map{}class x implements M!1\n{}", "1+Map");
  }

  public void testCommentSnippets013() throws Exception {
    test("class num{}class x !2{!1}!3", "1+num", "2-num", "3-num");
  }

  public void testCommentSnippets014() throws Exception {
    // trailing space is important
    test("class num{}typedef n!1 ;", "1+num");
  }

  public void testCommentSnippets015() throws Exception {
    test("class D {f(){} g(){f!1(f!2);}}", "1+f", "2+f");
  }

  public void testCommentSnippets016() throws Exception {
    test("class F {m() { m(); !1}}", "1+m");
  }

  public void testCommentSnippets017() throws Exception {
    test("class F {var x = !1false;}", "1+true");
  }

  public void testCommentSnippets018() throws Exception {
    test(
        "class Map{}class Arrays{}class C{ m(!1){} n(!2 x, q)",
        "1+Map",
        "1-void",
        "1-null",
        "2+Arrays",
        "2-void",
        "2-null");
  }

  public void testCommentSnippets019() throws Exception {
    test("class A{m(){Object x;x.!1/**/clear()", "1+toString");
  }

  public void testCommentSnippets020() throws Exception {
    test(
        "classMap{}class tst {var newt;void newf(){}test() {var newz;new!1/**/;}}",
        "1+newt",
        "1+newf",
        "1+newz",
        "1-Map");
  }

  public void testCommentSnippets021() throws Exception {
    test(
        "class Map{}class tst {var newt;void newf(){}test() {var newz;new !1/**/;}}",
        "1+Map",
        "1-newt");
  }

  public void testCommentSnippets022() throws Exception {
    test("class Map{}class F{m(){new !1;}}", "1+Map");
  }

  public void testCommentSnippets022a() throws Exception {
    test("class Map{}class F{m(){new !1", "1+Map");
  }

  public void testCommentSnippets022b() throws Exception {
    test("class Map{factory Map.qq(){return null;}}class F{m(){new Map.!1qq();}}", "1+qq");
  }

  public void testCommentSnippets023() throws Exception {
    test("class X {X c; X(this.!1c!3) : super() {c.!2}}", "1+c", "2+c", "3+c");
  }

  public void testCommentSnippets024() throws Exception {
    test("class q {m(Map q){var x;m(!1)}n(){var x;n(!2)}}", "1+x", "2+x");
  }

  public void testCommentSnippets025() throws Exception {
    test("class q {num m() {var q; num x=!1 q!3 + !2/**/;}}", "1+q", "2+q", "3+q");
  }

  public void testCommentSnippets026() throws Exception {
    test("class List{}class a implements !1{}", "1+List");
  }

  public void testCommentSnippets027() throws Exception {
    test(
        "class String{}class List{}class test <X extends !1String!2> {}",
        "1+List",
        "2+String",
        "2-List");
  }

  public void testCommentSnippets028() throws Exception {
    test(
        "class String{}class List{}class DateTime{}typedef T Y<T extends !1>(List input);",
        "1+DateTime",
        "1+String");
  }

  public void testCommentSnippets029() throws Exception {
//    test("interface A<X> default B<X extends !1List!2> {}", "1+DateTime", "2+List"); bad test
  }

  public void testCommentSnippets030() throws Exception {
    test(
        "class Bar<T extends Foo> {const Bar(!1T!2 k);T!3 m(T!4 a, T!5 b){}final T!6 f = null;}",
        "1+T",
        "2+T",
        "3+T",
        "4+T",
        "5+T",
        "6+T");
  }

  public void testCommentSnippets031() throws Exception {
    test(
        "class Bar<T extends Foo> {m(x){if (x is !1) return;if (x is!!!2)}}",
        "1+Bar",
        "1+T",
        "2+T",
        "2+Bar");
  }

  public void testCommentSnippets032() throws Exception {
    test("class Fit{}class Bar<T extends Fooa> {const F!1ara();}", "1+Fit", "1+Fara", "1-Bar");
  }

  public void testCommentSnippets033() throws Exception {
    // Type propagation
    test(
        "class List{add(){}length(){}}t1() {var x;if (x is List) {x.!1add(3);}}",
        "1+add",
        "1+length");
  }

  public void testCommentSnippets034() throws Exception {
    // Moved to CompletionLibraryTests
  }

  public void testCommentSnippets035() throws Exception {
    // Type propagation
    test(
        "class List{clear(){}length(){}}t3() {var x=new List(), y=x.!1length();x.!2clear();}",
        "1+length",
        "2+clear");
  }

  public void testCommentSnippets036() throws Exception {
    test("class List{}t3() {var x=new List!1}", "1+List");
  }

  public void testCommentSnippets037() throws Exception {
    test("class List{factory List.from(){}}t3() {var x=new List.!1}", "1+from");
  }

  public void testCommentSnippets038() throws Exception {
    test("f(){int xa; String s = '$x!1';}", "1+xa");
  }

  public void testCommentSnippets038a() throws Exception {
    test("int xa; String s = '$x!1'", "1+xa");
  }

  public void testCommentSnippets039() throws Exception {
    test("f(){int xa; String s = '$!1';}", "1+xa");
  }

  public void testCommentSnippets039a() throws Exception {
    test("int xa; String s = '$!1'", "1+xa");
  }

  public void testCommentSnippets040() throws Exception {
    test("class List{add(){}}class Map{}class X{m(){List list; list.!1 Map map;}}", "1+add");
  }

  public void testCommentSnippets041() throws Exception {
    test("class List{add(){}length(){}}class X{m(){List list; list.!1 zox();}}", "1+add");
  }

  public void testCommentSnippets042() throws Exception {
    test(
        "class DateTime{static const int WED=3;int get day;}fd(){DateTime d=new DateTime.now();d.!1WED!2;}",
        "1+day",
        "2-WED");
  }

  public void testCommentSnippets043() throws Exception {
    test("class L{var k;void.!1}", "1-k");
  }

  public void testCommentSnippets044() throws Exception {
    test("class List{}class XXX {XXX.fisk();}main() {main(); new !1}}", "1+List", "1+XXX.fisk");
  }

  public void testCommentSnippets045() throws Exception {
    // Moved to CompletionLibraryTests
  }

  public void testCommentSnippets046() throws Exception {
    // Moved to CompletionLibraryTests
  }

  public void testCommentSnippets047() throws Exception {
    test("f(){int x;int y=!1;}", "1+x");
  }

  public void testCommentSnippets048() throws Exception {
    // TODO: Use another library, dart:json is gone.
    // test("import 'dart:json' as json;f() {var x=new js!1}", "1+json");
  }

  public void testCommentSnippets049() throws Exception {
    // TODO: Use another library, dart:json is gone.
    // test(//
    //     src(//
    //         "import 'dart:json' as json;",
    //         "import 'dart:json' as jxx;",
    //         "class JsonParserX{}",
    //         "f1() {var x=new !2j!1s!3}"),
    //     "1+json",
    //     "1+jxx",
    //     "2+json",
    //     "2+jxx",
    //     "2-JsonParser",
    //     "3+json",
    //     "3-jxx");
  }

  public void testCommentSnippets050() throws Exception {
    test(//
        src(//
            "class xdr {",
            "  xdr();",
            "  const xdr.a(a,b,c);",
            "  xdr.b();",
            "  f() => 3;",
            "}",
            "class xa{}",
            "k() {",
            "  new x!1dr().f();",
            "  const x!2dr.!3a(1, 2, 3);",
            "}"),
        "1+xdr",
        "1+xa",
        "1+xdr.a",
        "1+xdr.b",
        "2-xa",
        "2-xdr",
        "2+xdr.a",
        "2-xdr.b",
        "3-b",
        "3+a");
  }

  public void testCommentSnippets051() throws Exception {
    // Type propagation.
    String source = src(
        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
        "void r() {",
        "  var v;",
        "  if (v is String) {",
        "    v.!1length;",
        "    v.!2getKeys;",
        "  }",
        "}");
    test(source, "1+length", "2-getKeys");
  }

  public void testCommentSnippets052() throws Exception {
    // Type propagation.
    String source = src(
        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
        "void r() {",
        "  List<String> values = ['a','b','c'];",
        "  for (var v in values) {",
        "    v.!1toUpperCase;",
        "    v.!2getKeys;",
        "  }",
        "}");
    test(source, "1+toUpperCase", "2-getKeys");
  }

  public void testCommentSnippets053() throws Exception {
    // Type propagation.
    String source = src(
        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
        "void r() {",
        "  var v;",
        "  while (v is String) {",
        "    v.!1toUpperCase;",
        "    v.!2getKeys;",
        "  }",
        "}");
    test(source, "1+toUpperCase", "2-getKeys");
  }

  public void testCommentSnippets054() throws Exception {
    // TODO Enable after type propagation is fully implemented.
//    String source = src(
//        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
//        "void r() {",
//        "  var v;",
//        "  for (; v is String; v.!1isEmpty) {",
//        "    v.!2toUpperCase;",
//        "    v.!3getKeys;",
//        "  }",
//        "}");
//    test(source, "1+isEmpty", "2+toUpperCase", "3-getKeys");
  }

  public void testCommentSnippets055() throws Exception {
    String source = src(
        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
        "void r() {",
        "  String v;",
        "  if (v is Object) {",
        "    v.!1toUpperCase;",
        "  }",
        "}");
    test(source, "1+toUpperCase");
  }

  public void testCommentSnippets056() throws Exception {
    // Type propagation.
    String source = src(
        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
        "void f(var v) {",
        "  if (v is!! String) {",
        "    return;",
        "  }",
        "  v.!1toUpperCase;",
        "}");
    test(source, "1+toUpperCase");
  }

  public void testCommentSnippets057() throws Exception {
    // Type propagation.
    String source = src(
        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
        "void f(var v) {",
        "  if ((v as String).length == 0) {",
        "    v.!1toUpperCase;",
        "  }",
        "}");
    test(source, "1+toUpperCase");
  }

  public void testCommentSnippets058() throws Exception {
    String source = src(
        "typedef vo!2id callback(int k);",
        "void x(callback q){}",
        "void r() {",
        "  callback v;",
        "  x(!1);",
        "}");
    test(source, "1+v", "2+void");
  }

  public void testCommentSnippets059() throws Exception {
    test("f(){((int x) => x+4).!1call(1);}", "1-call");
  }

  public void testCommentSnippets060() throws Exception {
    String source = src(
        "class Map{}",
        "abstract class MM extends Map{factory MM() => new Map();}",
        "class Z {",
        "  MM x;",
        "  f() {",
        "    x!1",
        "  }",
        "}");
    test(source, "1+x", "1-x[]");
  }

  public void testCommentSnippets061() throws Exception {
    test(
        "class A{m(){!1f(3);!2}}n(){!3f(3);!4}f(x)=>x*3;",
        "1+f",
        "1+n",
        "2+f",
        "2+n",
        "3+f",
        "3+n",
        "4+f",
        "4+n");
  }

  public void testCommentSnippets062() throws Exception {
    // Moved to CompletionLibraryTests
  }

  public void testCommentSnippets063() throws Exception {
    // Type propagation.
    String source = src(
        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
        "void r(var v) {",
        "  v.!1toUpperCase;",
        "  assert(v is String);",
        "  v.!2toUpperCase;",
        "}");
    test(source, "1-toUpperCase", "2+toUpperCase");
  }

  public void testCommentSnippets064() throws Exception {
    String source = src(
        "class Spline {",
        "  Line c;",
        "  Spline a() {",
        "    return this;",
        "  }",
        "  Line b() {",
        "    return null;",
        "  }",
        "  Spline f() {",
        "    Line x = new Line();",
        "    x.!9h()..!1a()..!2b().!7g();",
        "    x.!8j..!3b()..!4c..!6c..!5a();",
        "  }",
        "}",
        "class Line {",
        "  Spline j;",
        "  Line g() {",
        "    return this;",
        "  }",
        "  Spline h() {",
        "    return null;",
        "  }",
        "}");
    test(source, "1+a", "2+b", "1-g", "2-h", "3+b", "4+c", "5+a", "6+c", "7+g", "8+j", "9+h");
  }

  public void testCommentSnippets065() throws Exception {
    String source = src(
        "class Spline {",
        "  Line c;",
        "  Spline a() {",
        "    return this;",
        "  }",
        "  Line b() {",
        "    return null;",
        "  }",
        "  Spline f() {",
        "    Line x = new Line();",
        "    x.h()..!1;",
        "  }",
        "}",
        "class Line {",
        "  Spline j;",
        "  Line g() {",
        "    return this;",
        "  }",
        "  Spline h() {",
        "    return null;",
        "  }",
        "}");
    test(source, "1+a");
  }

  public void testCommentSnippets066() throws Exception {
    String source = src(
        "class Spline {",
        "  Line c;",
        "  Spline a() {",
        "    return this;",
        "  }",
        "  Line b() {",
        "    return null;",
        "  }",
        "  Spline f() {",
        "    Line x = new Line();",
        "    x.h()..a()..!1;",
        "  }",
        "}",
        "class Line {",
        "  Spline j;",
        "  Line g() {",
        "    return this;",
        "  }",
        "  Spline h() {",
        "    return null;",
        "  }",
        "}");
    test(source, "1+b");
  }

  public void testCommentSnippets067() throws Exception {
    String source = src(
        "class Spline {",
        "  Line c;",
        "  Spline a() {",
        "    return this;",
        "  }",
        "  Line b() {",
        "    return null;",
        "  }",
        "  Spline f() {",
        "    Line x = new Line();",
        "    x.h()..a()..c..!1;",
        "  }",
        "}",
        "class Line {",
        "  Spline j;",
        "  Line g() {",
        "    return this;",
        "  }",
        "  Spline h() {",
        "    return null;",
        "  }",
        "}");
    test(source, "1+b");
  }

  public void testCommentSnippets068() throws Exception {
    String source = src(
        "class Spline {",
        "  Line c;",
        "  Spline a() {",
        "    return this;",
        "  }",
        "  Line b() {",
        "    return null;",
        "  }",
        "  Spline f() {",
        "    Line x = new Line();",
        "    x.j..b()..c..!1;",
        "  }",
        "}",
        "class Line {",
        "  Spline j;",
        "  Line g() {",
        "    return this;",
        "  }",
        "  Spline h() {",
        "    return null;",
        "  }",
        "}");
    test(source, "1+c");
  }

  public void testCommentSnippets069() throws Exception {
    String source = src(
        "class Spline {",
        "  Line c;",
        "  Spline a() {",
        "    return this;",
        "  }",
        "  Line b() {",
        "    return null;",
        "  }",
        "  Spline f() {",
        "    Line x = new Line();",
        "    x.j..b()..!1;",
        "  }",
        "}",
        "class Line {",
        "  Spline j;",
        "  Line g() {",
        "    return this;",
        "  }",
        "  Spline h() {",
        "    return null;",
        "  }",
        "}");
    test(source, "1+c");
  }

  public void testCommentSnippets070() throws Exception {
    String source = src(
        "class Spline {",
        "  Line c;",
        "  Spline a() {",
        "    return this;",
        "  }",
        "  Line b() {",
        "    return null;",
        "  }",
        "  Spline f() {",
        "    Line x = new Line();",
        "    x.j..!1;",
        "  }",
        "}",
        "class Line {",
        "  Spline j;",
        "  Line g() {",
        "    return this;",
        "  }",
        "  Spline h() {",
        "    return null;",
        "  }",
        "}");
    test(source, "1+b");
  }

  public void testCommentSnippets072() throws Exception {
    String source = src(
        "class X {",
        "  int _p;",
        "  set p(int x) => _p = x;",
        "}",
        "f() {",
        "  X x = new X();",
        "  x.!1p = 3;",
        "}");
    test(source, "1+p");
  }

  public void testCommentSnippets073() throws Exception {
    String source = src(
        "class X {",
        "  m() {",
        "    JSON.stri!1;",
        "    X f = null;",
        "  }",
        "}",
        "class JSON {",
        "  static stringify() {}",
        "}");
    test(source, "1+stringify");
  }

  public void testCommentSnippets074() throws Exception {
    String source = src(//
        "class X {",
        "  m() {",
        "    _x!1",
        "  }",
        "  _x1(){}",
        "}");
    test(source, "1+_x1");
  }

  public void testCommentSnippets075() throws Exception {
    test("p(x)=>0;var E;f(q)=>!1p(!2E);", "1+p", "2+E");
  }

  public void testCommentSnippets076() throws Exception {
    test(
        "class Map<K,V>{}class List<E>{}class int{}main() {var m=new Map<Lis!1t<Map<int,in!2t>>,List<!3int>>();}",
        "1+List",
        "2+int",
        "3+int");
  }

  public void testCommentSnippets076a() throws Exception {
    test(
        "class Map<K,V>{}class List<E>{}class int{}main() {var m=new Map<Lis!1t<Map<int,in!2t>>,List<!3>>();}",
        "1+List",
        "2+int",
        "3+int");
  }

  public void testCommentSnippets077() throws Exception {
    test(
        src(
            "class FileMode {",
            "  static const READ = const FileMode._internal(0);",
            "  static const WRITE = const FileMode._internal(1);",
            "  static const APPEND = const FileMode._internal(2);",
            "  const FileMode._internal(int this._mode);",
            "  factory FileMode._internal1(int this._mode);",
            "  factory FileMode(_mode);",
            "  final int _mode;",
            "}",
            "class File {",
            "  factory File(String path) => null;",
            "  factory File.fromPath(Path path) => null;",
            "}",
            "f() => new Fil!1"),
        "1+File",
        "1+File.fromPath",
        "1+FileMode",
        "1+FileMode._internal1",
        "1+FileMode._internal");
  }

  public void testCommentSnippets078() throws Exception {
    test("class Map{static from()=>null;clear(){}}void main() { Map.!1 }", "1+from", "1-clear"); // static method, instance method
  }

  public void testCommentSnippets079() throws Exception {
    test("class Map{static from()=>null;clear(){}}void main() { Map s; s.!1 }", "1-from", "1+clear"); // static method, instance method
  }

  public void testCommentSnippets080() throws Exception {
    test("class RuntimeError{var message;}void main() { RuntimeError.!1 }", "1-message"); // field
  }

  public void testCommentSnippets081() throws Exception {
    test("class Foo {this.!1}", "1-Object");
  }

  public void testCommentSnippets082() throws Exception {
    String source = src(
        "class HttpRequest {}",
        "class HttpResponse {}",
        "main() {",
        "  var v = (HttpRequest req, HttpResp!1)",
        "}");
    test(source, "1+HttpResponse");
  }

  public void testCommentSnippets083() throws Exception {
    test("main() {(.!1)}", "1-toString");
  }

  public void testCommentSnippets083a() throws Exception {
    test("main() { .!1 }", "1-toString");
  }

  public void testCommentSnippets083b() throws Exception {
    test("main() { null.!1 }", "1+toString");
  }

  public void testCommentSnippets084() throws Exception {
    test(
        "class List{}class Map{}typedef X = !1Lis!2t with !3Ma!4p;",
        "1+Map",
        "2+List",
        "2-Map",
        "3+List",
        "4+Map",
        "4-List");
  }

  public void testCommentSnippets085() throws Exception {
    test(
        "class List{}class Map{}class Z extends List with !1Ma!2p {}",
        "1+List",
        "1+Map",
        "2+Map",
        "2-List");
  }

  public void testCommentSnippets086() throws Exception {
    test("class Q{f(){xy() {!2};x!1y();}}", "1+xy", "2+f", "2-xy");
  }

  public void testCommentSnippets087() throws Exception {
    test("class Map{}class Q extends Object with !1Map {}", "1+Map", "1-HashMap");
  }

  public void testCommentSnippets088() throws Exception {
    String source = src(
        "class A {",
        "  int f;",
        "  B m(){}",
        "}",
        "class B extends A {",
        "  num f;",
        "  A m(){}",
        "}",
        "class Z {",
        "  B q;",
        "  f() {q.!1}",
        "}");
    test(source, "1+f", "1+m"); // f->num, m()->A
  }

  public void testCommentSnippets089() throws Exception {
    String source = src(
        "class Q {",
        "  fqe() {",
        "    xya() {",
        "      xyb() {",
        "        !1",
        "      }",
        "      !3 xyb();",
        "    };",
        "    xza() {",
        "      !2",
        "    }",
        "    xya();",
        "    !4 xza();",
        "  }",
        "  fqi() {",
        "    !5",
        "  }",
        "}");
    test(
        source,
        "1+fqe",
        "1+fqi",
        "1+Q",
        "1-xya",
        "1-xyb",
        "1-xza",
        "2+fqe",
        "2+fqi",
        "2+Q",
        "2-xya",
        "2-xyb",
        "2-xza",
        "3+fqe",
        "3+fqi",
        "3+Q",
        "3-xya",
        "3+xyb",
        "3-xza",
        "4+fqe",
        "4+fqi",
        "4+Q",
        "4+xya",
        "4-xyb",
        "4+xza",
        "5+fqe",
        "5+fqi",
        "5+Q",
        "5-xya",
        "5-xyb",
        "5-xza");
  }

  public void testCommentSnippets090() throws Exception {
    test("class X { f() { var a = 'x'; a.!1 }}", "1+length");
  }

  public void testCompletion_alias_field() throws Exception {
    test("typedef int fnint(int k); fn!1int x;", "1+fnint");
  }

  public void testCompletion_annotation_argumentList() throws Exception {
    test(src(//
        "class AAA {",
        "  const AAA({int aaa, int bbb});",
        "}",
        "",
        "@AAA(!1)",
        "main() {",
        "}"), "1+AAA:" + ProposalKind.ARGUMENT_LIST, "1+aaa", "1+bbb");
  }

  public void testCompletion_annotation_topLevelVar() throws Exception {
    test(src(//
        "const fooConst = null;",
        "final fooNotConst = null;",
        "const bar = null;",
        "",
        "@foo!1",
        "main() {",
        "}"), "1+fooConst", "1-fooNotConst", "1-bar");
  }

  public void testCompletion_annotation_type() throws Exception {
    test(src(//
        "class AAA {",
        "  const AAA({int a, int b});",
        "  const AAA.nnn(int c, int d);",
        "}",
        "",
        "",
        "@AAA!1",
        "main() {",
        "}"), "1+AAA:" + ProposalKind.CONSTRUCTOR, "1+AAA.nnn:" + ProposalKind.CONSTRUCTOR);
  }

  public void testCompletion_annotation_type_inClass_withoutMember() throws Exception {
    test(src(//
        "class AAA {",
        "  const AAA();",
        "}",
        "",
        "class C {",
        "  @A!1",
        "}"), "1+AAA:" + ProposalKind.CONSTRUCTOR);
  }

  public void testCompletion_argument_typeName() throws Exception {
    test(src(//
        "class Enum {",
        "  static Enum FOO = new Enum();",
        "}",
        "f(Enum e) {}",
        "main() {",
        "  f(En!1);",
        "}"), "1+Enum");
  }

  public void testCompletion_arguments_ignoreEmpty() throws Exception {
    test(src(//
        "class A {",
        "  test() {}",
        "}",
        "main(A a) {",
        "  a.test(!1);",
        "}"), "1-test");
  }

  public void testCompletion_as_asIdentifierPrefix() throws Exception {
    test(src(//
        "main(p) {",
        "  var asVisible;",
        "  var v = as!1;",
        "}"), "1+asVisible");
  }

  public void testCompletion_as_asPrefixedIdentifierStart() throws Exception {
    test(src(//
        "class A {",
        "  var asVisible;",
        "}",
        "",
        "main(A p) {",
        "  var v = p.as!1;",
        "}"), "1+asVisible");
  }

  public void testCompletion_as_incompleteStatement() throws Exception {
    test(src(//
        "class MyClass {}",
        "main(p) {",
        "  var justSomeVar;",
        "  var v = p as !1",
        "}"), "1+MyClass", "1-justSomeVar");
  }

  public void testCompletion_cascade() throws Exception {
    test(src(//
        "class A {",
        "  aaa() {}",
        "}",
        "",
        "",
        "main(A a) {",
        "  a..!1 aaa();",
        "}"), "1+aaa", "1-main");
  }

  public void testCompletion_combinator_afterComma() throws Exception {
    test("import 'dart:math' show cos, !1;", "1+PI", "1+sin", "1+Random", "1-String");
  }

  public void testCompletion_combinator_ended() throws Exception {
    test("import 'dart:math' show !1;", "1+PI", "1+sin", "1+Random", "1-String");
  }

  public void testCompletion_combinator_export() throws Exception {
    test("export 'dart:math' show !1;", "1+PI", "1+sin", "1+Random", "1-String");
  }

  public void testCompletion_combinator_hide() throws Exception {
    test("import 'dart:math' hide !1;", "1+PI", "1+sin", "1+Random", "1-String");
  }

  public void testCompletion_combinator_notEnded() throws Exception {
    test("import 'dart:math' show !1", "1+PI", "1+sin", "1+Random", "1-String");
  }

  public void testCompletion_combinator_usePrefix() throws Exception {
    test("import 'dart:math' show s!1", "1+sin", "1+sqrt", "1-cos", "1-String");
  }

  public void testCompletion_constructor_field() throws Exception {
    test("class X { X(this.field); int f!1ield;}", "1+field");
  }

  public void testCompletion_constructorArguments_showOnlyCurrent() throws Exception {
    test(src(//
        "class A {",
        "  A.first(int p);",
        "  A.second(double p);",
        "}",
        "main() {",
        "  new A.first(!1);",
        "}"), "1+A.first", "1-A.second");
  }

  public void testCompletion_constructorArguments_whenPrefixedType() throws Exception {
    test(src(//
        "import 'dart:math' as m;",
        "main() {",
        "  new m.Random(!1);",
        "}"), "1+Random:ARGUMENT_LIST");
  }

  public void testCompletion_dartDoc_reference_forClass() throws Exception {
    test(src(//
        "/**",
        " * [int!1]",
        " * [method!2]",
        " */",
        "class AAA {",
        "  methodA() {}",
        "}",
        ""), "1+int", "1-method", "2+methodA", "2-int");
  }

  public void testCompletion_dartDoc_reference_forConstructor() throws Exception {
    test(src(//
        "class A {",
        "  /**",
        "   * [aa!1]",
        "   * [int!2]",
        "   * [method!3]",
        "   */",
        "  A.named(aaa, bbb) {}",
        "  methodA() {}",
        "}"), "1+aaa", "1-bbb", "2+int", "2-double", "3+methodA");
  }

  public void testCompletion_dartDoc_reference_forFunction() throws Exception {
    test(src(//
        "/**",
        " * [aa!1]",
        " * [int!2]",
        " * [function!3]",
        " */",
        "functionA(aaa, bbb) {}",
        "functionB() {}",
        ""), "1+aaa", "1-bbb", "2+int", "2-double", "3+functionA", "3+functionB", "3-int");
  }

  public void testCompletion_dartDoc_reference_forFunctionTypeAlias() throws Exception {
    test(src(//
        "/**",
        " * [aa!1]",
        " * [int!2]",
        " * [Function!3]",
        " */",
        "typedef FunctionA(aaa, bbb) {}",
        "typedef FunctionB() {}",
        ""), "1+aaa", "1-bbb", "2+int", "2-double", "3+FunctionA", "3+FunctionB", "3-int");
  }

  public void testCompletion_dartDoc_reference_forMethod() throws Exception {
    test(src(//
        "class A {",
        "  /**",
        "   * [aa!1]",
        "   * [int!2]",
        "   * [method!3]",
        "   */",
        "  methodA(aaa, bbb) {}",
        "  methodB() {}",
        "}"), "1+aaa", "1-bbb", "2+int", "2-double", "3+methodA", "3+methodB", "3-int");
  }

  public void testCompletion_dartDoc_reference_incomplete() throws Exception {
    test(src(//
        "/**",
        " * [doubl!1 some text",
        " * other text",
        " */",
        "class A {}",
        "/**",
        " * [!2 some text",
        " * other text",
        " */",
        "class B {}",
        "/**",
        " * [!3] some text",
        " */",
        "class C {}",
        ""), "1+double", "1-int", "2+int", "2+String", "3+int", "3+String");
  }

  public void testCompletion_double_inFractionPart() throws Exception {
    test(src(//
        "main() {",
        "  1.0!1",
        "}"), "1-abs", "1-main");
  }

  public void testCompletion_exactPrefix_hasHigherRelevance() throws Exception {
    test(src(//
        "var STR;",
        "main(p) {",
        "  var str;",
        "  str!1;",
        "  STR!2;",
        "  Str!3;",
        "}"),//
        "1+str,rel=" + (CompletionProposal.RELEVANCE_DEFAULT + 1),
        "1+STR,rel=" + (CompletionProposal.RELEVANCE_DEFAULT + 0),
        "2+STR,rel=" + (CompletionProposal.RELEVANCE_DEFAULT + 1),
        "2+str,rel=" + (CompletionProposal.RELEVANCE_DEFAULT + 0),
        "3+String,rel=" + (CompletionProposal.RELEVANCE_DEFAULT + 1),
        "3+STR,rel=" + (CompletionProposal.RELEVANCE_DEFAULT + 0),
        "3+str,rel=" + (CompletionProposal.RELEVANCE_DEFAULT + 0));
  }

  public void testCompletion_export_dart() throws Exception {
    test(
        src(//
            "import 'dart:math",
            "import 'dart:_chrome",
            "import 'dart:_collection.dev",
            "export 'dart:!1"),
        "1+dart:core",
        "1+dart:math",
        "1-dart:_chrome",
        "1-dart:_collection.dev");
  }

  public void testCompletion_export_noStringLiteral_noSemicolon() throws Exception {
    test(src(//
        "import !1",
        "",
        "class A {}"), resultWithCursor("1+'dart:!';"), resultWithCursor("1+'package:!';"));
  }

  public void testCompletion_forStmt_vars() throws Exception {
    test(
        "class int{}class Foo { mth() { for (in!1t i = 0; i!2 < 5; i!3++); }}",
        "1+int",
        "2+i",
        "3+i");
  }

  public void testCompletion_function() throws Exception {
    test(
        "class String{}class Foo { int boo = 7; mth() { PNGS.sort((String a, Str!1) => a.compareTo(b)); }}",
        "1+String");
  }

  public void testCompletion_function_partial() throws Exception {
    test(
        "class String{}class Foo { int boo = 7; mth() { PNGS.sort((String a, Str!1)); }}",
        "1+String");
  }

  public void testCompletion_functionTypeParameter_namedArgument() throws Exception {
    test(src(//
        "typedef FFF(a, b, {x1, x2, y});",
        "main(FFF fff) {",
        "  fff(1, 2, !1)!2;",
        "}"), "1+x1", "2-x2");
  }

  public void testCompletion_ifStmt_field1() throws Exception {
    test("class Foo { int myField = 7; mth() { if (!1) {}}}", "1+myField");
  }

  public void testCompletion_ifStmt_field1a() throws Exception {
    test("class Foo { int myField = 7; mth() { if (!1) }}", "1+myField");
  }

  public void testCompletion_ifStmt_field2() throws Exception {
    test("class Foo { int myField = 7; mth() { if (m!1) {}}}", "1+myField");
  }

  public void testCompletion_ifStmt_field2a() throws Exception {
    test("class Foo { int myField = 7; mth() { if (m!1) }}", "1+myField");
  }

  public void testCompletion_ifStmt_field2b() throws Exception {
    test("class Foo { myField = 7; mth() { if (m!1) {}}}", "1+myField");
  }

  public void testCompletion_ifStmt_localVar() throws Exception {
    test("class Foo { mth() { int value = 7; if (v!1) {}}}", "1+value");
  }

  public void testCompletion_ifStmt_localVara() throws Exception {
    test("class Foo { mth() { value = 7; if (v!1) {}}}", "1-value");
  }

  public void testCompletion_ifStmt_topLevelVar() throws Exception {
    test("int topValue = 7; class Foo { mth() { if (t!1) {}}}", "1+topValue");
  }

  public void testCompletion_ifStmt_topLevelVara() throws Exception {
    test("topValue = 7; class Foo { mth() { if (t!1) {}}}", "1+topValue");
  }

  public void testCompletion_ifStmt_unionType() throws Exception {
    enableUnionTypes(false);
    test(src(//
        "class A { a() => null; }",
        "class B { b() => null; }",
        "void main() {",
        "  var x;",
        "  var c;",
        "  if(c) {",
        "    x = new A();",
        "  } else {",
        "    x = new B();",
        "  }",
        "  x.!1;",
        "}"), "1+a", "1+b");
  }

  public void testCompletion_import() throws Exception {
    test(src(//
        "import '!1';"),
        resultWithCursor("1+dart:!"),
        resultWithCursor("1+package:!"));
  }

  public void testCompletion_import_dart() throws Exception {
    test(
        src(//
            "import 'dart:math",
            "import 'dart:_chrome",
            "import 'dart:_collection.dev",
            "import 'dart:!1"),
        "1+dart:core",
        "1+dart:math",
        "1-dart:_chrome",
        "1-dart:_collection.dev");
  }

  public void testCompletion_import_hasStringLiteral_noSemicolon() throws Exception {
    test(src(//
        "import '!1'",
        "",
        "class A {}"), resultWithCursor("1+dart:!"), resultWithCursor("1+package:!"));
  }

  public void testCompletion_import_noSpace() throws Exception {
    test(src(//
        "import!1",
        ""), resultWithCursor("1+ 'dart:!';"), resultWithCursor("1+ 'package:!';"));
  }

  public void testCompletion_import_noStringLiteral() throws Exception {
    test(src(//
        "import !1;"),
        resultWithCursor("1+'dart:!'"),
        resultWithCursor("1+'package:!'"));
  }

  public void testCompletion_import_noStringLiteral_noSemicolon() throws Exception {
    test(src(//
        "import !1",
        "",
        "class A {}"), resultWithCursor("1+'dart:!';"), resultWithCursor("1+'package:!';"));
  }

  public void testCompletion_incompleteClassMember() throws Exception {
    test(src(//
        "class A {",
        "  Str!1",
        "  final f = null;",
        "}"), "1+String", "1-bool");
  }

  public void testCompletion_incompleteClosure_parameterType() throws Exception {
    test(src(//
        "f1(cb(String s)) {}",
        "f2(String s) {}",
        "main() {",
        "  f1((Str!1));",
        "  f2((Str!2));",
        "}"), "1+String", "1-bool", "2+String", "2-bool");
  }

  public void testCompletion_inPeriodPeriod() throws Exception {
    test(src(//
        "main(String str) {",
        "  1 < str.!1.length;",
        "  1 + str.!2.length;",
        "  1 + 2 * str.!3.length;",
        "}"), "1+codeUnits", "2+codeUnits", "3+codeUnits");
  }

  public void testCompletion_instanceCreation_unresolved() throws Exception {
    test(src(//
        "class A {",
        "}",
        "main() {",
        "  new NoSuchClass(!1);",
        "  new A.noSuchConstructor(!2);",
        "}"), "1+int", "2+int");
    // no checks, but no exceptions
  }

  // TODO(scheglov)
//  public void testCompletion_import_lib() throws Exception {
//    addSource("/my_lib.dart", "");
//    test("import '!1", "1+my_lib.dart");
//  }
  public void testCompletion_is() throws Exception {
    test(src(//
        "class MyClass {}",
        "main(p) {",
        "  var isVariable;",
        "  if (p is MyCla!1) {}",
        "  var v1 = p is MyCla!2;",
        "  var v2 = p is !3;",
        "  var v2 = p is!4;",
        "}"), "1+MyClass", "2+MyClass", "3+MyClass", "3-v1", "4+is", "4-isVariable");
  }

  public void testCompletion_is_asIdentifierStart() throws Exception {
    test(src(//
        "main(p) {",
        "  var isVisible;",
        "  var v1 = is!1;",
        "  var v2 = is!2",
        "}"), "1+isVisible", "2+isVisible");
  }

  public void testCompletion_is_asPrefixedIdentifierStart() throws Exception {
    test(src(//
        "class A {",
        "  var isVisible;",
        "}",
        "",
        "main(A p) {",
        "  var v1 = p.is!1;",
        "  var v2 = p.is!2",
        "}"), "1+isVisible", "2+isVisible");
  }

  public void testCompletion_is_incompleteStatement1() throws Exception {
    test(src(//
        "class MyClass {}",
        "main(p) {",
        "  var justSomeVar;",
        "  var v = p is !1",
        "}"), "1+MyClass", "1-justSomeVar");
  }

  public void testCompletion_is_incompleteStatement2() throws Exception {
    test(src(//
        "class MyClass {}",
        "main(p) {",
        "  var isVariable;",
        "  var v = p is!1",
        "}"), "1+is", "1-isVariable");
  }

  public void testCompletion_keyword_in() throws Exception {
    test("class Foo { int input = 7; mth() { if (in!1) {}}}", "1+input");
  }

  public void testCompletion_keyword_syntheticIdentifier() throws Exception {
    test(src(//
        "main() {",
        "  var caseVar;",
        "  var otherVar;",
        "  var v = case!1",
        "}"), "1+caseVar", "1-otherVar");
  }

  public void testCompletion_libraryIdentifier_atEOF() throws Exception {
    test("library int.!1", "1-parse", "1-bool");
  }

  public void testCompletion_libraryIdentifier_notEOF() throws Exception {
    test(src(//
        "library int.!1",
        ""), "1-parse", "1-bool");
  }

  public void testCompletion_methodRef_asArg_incompatibleFunctionType() throws Exception {
    test(src(//
        "foo( f(int p) ) {}",
        "class Functions {",
        "  static myFuncInt(int p) {}",
        "  static myFuncDouble(double p) {}",
        "}",
        "bar(p) {}",
        "main(p) {",
        "  foo( Functions.!1; );",
        "}"), "1+myFuncInt:" + ProposalKind.METHOD_NAME, "1-myFuncDouble:"
        + ProposalKind.METHOD_NAME);
  }

  public void testCompletion_methodRef_asArg_notFunctionType() throws Exception {
    test(src(//
        "foo( f(int p) ) {}",
        "class Functions {",
        "  static myFunc(int p) {}",
        "}",
        "bar(p) {}",
        "main(p) {",
        "  foo( (int p) => Functions.!1; );",
        "}"), "1+myFunc:" + ProposalKind.METHOD, "1-myFunc:" + ProposalKind.METHOD_NAME);
  }

  public void testCompletion_methodRef_asArg_ofFunctionType() throws Exception {
    test(src(//
        "foo( f(int p) ) {}",
        "class Functions {",
        "  static int myFunc(int p) {}",
        "}",
        "main(p) {",
        "  foo(Functions.!1);",
        "}"), "1+myFunc:" + ProposalKind.METHOD, "1+myFunc:" + ProposalKind.METHOD_NAME);
  }

  public void testCompletion_namedArgument_alreadyUsed() throws Exception {
    test("func({foo}) {} main() { func(foo: 0, fo!1); }", "1-foo");
  }

  public void testCompletion_namedArgument_constructor() throws Exception {
    test("class A {A({foo, bar}) {}} main() { new A(fo!1); }", "1+foo", "1-bar");
  }

  public void testCompletion_namedArgument_empty() throws Exception {
    test(
        "func({foo, bar}) {} main() { func(!1); }",
        "1+foo:" + ProposalKind.NAMED_ARGUMENT,
        "1-foo:" + ProposalKind.OPTIONAL_ARGUMENT);
  }

  public void testCompletion_namedArgument_function() throws Exception {
    test("func({foo, bar}) {} main() { func(fo!1); }", "1+foo", "1-bar");
  }

  public void testCompletion_namedArgument_notNamed() throws Exception {
    test("func([foo]) {} main() { func(fo!1); }", "1-foo");
  }

  public void testCompletion_namedArgument_unresolvedFunction() throws Exception {
    test("main() { func(fo!1); }", "1-foo");
  }

  public void testCompletion_newMemberType1() throws Exception {
    test(
        "class Collection{}class List extends Collection{}class Foo { !1 }",
        "1+Collection",
        "1+List");
  }

  public void testCompletion_newMemberType2() throws Exception {
    test(
        "class Collection{}class List extends Collection{}class Foo {!1}",
        "1+Collection",
        "1+List");
  }

  public void testCompletion_newMemberType3() throws Exception {
    test(
        "class Collection{}class List extends Collection{}class Foo {L!1}",
        "1-Collection",
        "1+List");
  }

  public void testCompletion_newMemberType4() throws Exception {
    test(
        "class Collection{}class List extends Collection{}class Foo {C!1}",
        "1+Collection",
        "1-List");
  }

  public void testCompletion_positionalArgument_constructor() throws Exception {
    test(src(//
        "class A {",
        "  A([foo, bar]);",
        "}",
        "main() {",
        "  new A(!1);",
        "  new A(0, !2);",
        "}"), "1+foo:" + ProposalKind.OPTIONAL_ARGUMENT, "1-bar", "2-foo", "2+bar:"
        + ProposalKind.OPTIONAL_ARGUMENT);
  }

  public void testCompletion_positionalArgument_function() throws Exception {
    test(src(//
        "func([foo, bar]) {}",
        "main() {",
        "  func(!1);",
        "  func(0, !2);",
        "}"), "1+foo:" + ProposalKind.OPTIONAL_ARGUMENT, "1-bar", "2-foo", "2+bar:"
        + ProposalKind.OPTIONAL_ARGUMENT);
  }

  public void testCompletion_preferStaticType() throws Exception {
    test(
        src(//
            "class A {",
            "  foo() {}",
            "}",
            "class B extends A {",
            "  bar() {}",
            "}",
            "main() {",
            "  A v = new B();",
            "  v.!1",
            "}"),
        "1+foo",
        "1-bar,potential=false,declaringType=B",
        "1+bar,potential=true,declaringType=B");
  }

  public void testCompletion_privateElement_sameLibrary_constructor() throws Exception {
    test(src(//
        "class A {",
        "  A._c();",
        "  A.c();",
        "}",
        "main() {",
        "  new A.!1",
        "}"), "1+_c", "1+c");
  }

  public void testCompletion_privateElement_sameLibrary_member() throws Exception {
    test(src(//
        "class A {",
        "  _m() {}",
        "  m() {}",
        "}",
        "main(A a) {",
        "  a.!1",
        "}"), "1+_m", "1+m");
  }

  public void testCompletion_propertyAccess_whenClassTarget() throws Exception {
    test(src(//
        "class A {",
        "  static int FIELD;",
        "  int field;",
        "}",
        "main() {",
        "  A.!1",
        "}"), "1+FIELD", "1-field");
  }

  public void testCompletion_propertyAccess_whenClassTarget_excludeSuper() throws Exception {
    test(src(//
        "class A {",
        "  static int FIELD_A;",
        "  static int methodA() {}",
        "}",
        "class B extends A {",
        "  static int FIELD_B;",
        "  static int methodB() {}",
        "}",
        "main() {",
        "  B.!1;",
        "}"), "1+FIELD_B", "1-FIELD_A", "1+methodB", "1-methodA");
  }

  public void testCompletion_propertyAccess_whenInstanceTarget() throws Exception {
    test(src(//
        "class A {",
        "  static int FIELD;",
        "  int fieldA;",
        "}",
        "class B {",
        "  A a;",
        "}",
        "class C extends A {",
        "  int fieldC;",
        "}",
        "main(B b, C c) {",
        "  b.a.!1;",
        "  c.!2;",
        "}"), "1-FIELD", "1+fieldA", "2+fieldC", "2+fieldA");
  }

  public void testCompletion_return_withIdentifierPrefix() throws Exception {
    test("f() { var vvv = 42; return v!1 }", "1+vvv");
  }

  public void testCompletion_return_withoutExpression() throws Exception {
    test("f() { var vvv = 42; return !1 }", "1+vvv");
  }

  public void testCompletion_staticField1() throws Exception {
    test(
        "class num{}class Sunflower {static final n!2um MAX_D = 300;nu!3m xc, yc;Sun!4flower() {x!Xc = y!Yc = MA!1 }}",
        "1+MAX_D",
        "X+xc",
        "Y+yc",
        "2+num",
        "3+num",
        "4+Sunflower");
  }

  public void testCompletion_super_superType() throws Exception {
    test(src(//
        "class A {",
        "  var fa;",
        "  ma() {}",
        "}",
        "class B extends A {",
        "  var fb;",
        "  mb() {}",
        "  main() {",
        "    super.!1",
        "  }",
        "}"), "1+fa", "1-fb", "1+ma", "1-mb");
  }

  public void testCompletion_superConstructorInvocation_noNamePrefix() throws Exception {
    test(src(//
        "class A {",
        "  A.fooA();",
        "  A.fooB();",
        "  A.bar();",
        "}",
        "class B extends A {",
        "  B() : super.!1",
        "}"), "1+fooA", "1+fooB", "1+bar");
  }

  public void testCompletion_superConstructorInvocation_withNamePrefix() throws Exception {
    test(src(//
        "class A {",
        "  A.fooA();",
        "  A.fooB();",
        "  A.bar();",
        "}",
        "class B extends A {",
        "  B() : super.f!1",
        "}"), "1+fooA", "1+fooB", "1-bar");
  }

  public void testCompletion_this_bad_inConstructorInitializer() throws Exception {
    test(src(//
        "class A {",
        "  var f;",
        "  A() : f = this.!1;",
        "}"), "1-toString");
  }

  public void testCompletion_this_bad_inFieldDeclaration() throws Exception {
    test(src(//
        "class A {",
        "  var f = this.!1;",
        "}"), "1-toString");
  }

  public void testCompletion_this_bad_inStaticMethod() throws Exception {
    test(src(//
        "class A {",
        "  static m() {",
        "    this.!1;",
        "  }",
        "}"), "1-toString");
  }

  public void testCompletion_this_bad_inTopLevelFunction() throws Exception {
    test(src(//
        "main() {",
        "  this.!1;",
        "}"), "1-toString");
  }

  public void testCompletion_this_bad_inTopLevelVariableDeclaration() throws Exception {
    test(src(//
        "var v = this.!1;"),
        "1-toString");
  }

  public void testCompletion_this_OK_inConstructorBody() throws Exception {
    test(src(//
        "class A {",
        "  var f;",
        "  m() {}",
        "  A() {",
        "    this.!1;",
        "  }",
        "}"), "1+f", "1+m");
  }

  public void testCompletion_this_OK_localAndSuper() throws Exception {
    test(src(//
        "class A {",
        "  var fa;",
        "  ma() {}",
        "}",
        "class B extends A {",
        "  var fb;",
        "  mb() {}",
        "  main() {",
        "    this.!1",
        "  }",
        "}"), "1+fa", "1+fb", "1+ma", "1+mb");
  }

  public void testCompletion_topLevelField_init2() throws Exception {
    test("class DateTime{static var JUN;}final num M = Dat!1eTime.JUN;", "1+DateTime", "1-void");
  }

  public void testCompletion_while() throws Exception {
    test("class Foo { int boo = 7; mth() { while (b!1) {} }}", "1+boo");
  }

  // TODO Improve proposals for optional params.
  public void testSingle() throws Exception {
    test("class A {int x; !2mth() {int y = this.x;}}class B{}", "2+B");
  }

}

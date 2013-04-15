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
    test(
        src(//
            "class Q {",
            "  bool x;",
            "  List zs;",
            "  int k;",
            "  var a;",
            "  mth() {",
            "    while (!1x !9); ",
            "    do{} while(!2x !8);",
            "    for(z in !3zs) {}",
            "    switch(!4k) {case 1:{}}",
            "    try {",
            "    } on !5Object catch(a){}",
            "    if (!7x !6) {} else {};",
            "  }",
            "}"),
        "1+x",
        "1-Q",
        "2+x",
        "2-Q",
        "3+zs",
        "3-Q",
        "4+k",
        "4-Q",
        "5+Q",
        "5-a",
        "6+==",
        "7+x",
        "8+==",
        "9+==");
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
    // TODO Enable after type propagation is implemented.
    test(
        "class List{add(){}length(){}}t1() {var x;if (x is List) {x.!1add(3);}}",
        "1+add",
        "1+length");
  }

  public void testCommentSnippets034() throws Exception {
    // TODO Enable after type propagation is implemented.
    // TODO Move to new TestCase that includes corelib analysis
//    test("t2() {var q=[0],z=q.!1length;q.!2clear();}", "1+length", "1+isEmpty", "2+clear");
  }

  public void testCommentSnippets035() throws Exception {
    // TODO Enable after type propagation is implemented.
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
    // TODO Move to new TestCase that includes corelib analysis
//    test("class X{var q; f() {q.!1a!2}}", "1+end", "2+abs", "2-end");
  }

  public void testCommentSnippets046() throws Exception {
    // TODO Enable once the indexer is fixed to properly handle library prefixes
    // Resolving dart:html takes between 2.5s and 30s; json, about 0.12s
//    test(
//        src(
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

  public void testCommentSnippets047() throws Exception {
    test("f(){int x;int y=!1;}", "1+x");
  }

  public void testCommentSnippets048() throws Exception {
    test("import 'dart:json' as json;f() {var x=new js!1}", "1+json");
  }

  public void testCommentSnippets049() throws Exception {
    test(//
        src(//
            "import 'dart:json' as json;",
            "import 'dart:json' as jxx;",
            "class JsonParserX{}",
            "f1() {var x=new !2j!1s!3}"),
        "1+json",
        "1+jxx",
        "2+json",
        "2+jxx",
        "2-JsonParser",
        "3+json",
        "3-jxx");
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
        "1-xdr.a",
        "1+xdr.b",
        "2-xa",
        "2-xdr",
        "2+xdr.a",
        "2-xdr.b",
        "3-b",
        "3+a");
  }

  public void testCommentSnippets051() throws Exception {
    // TODO Enable after type propagation is implemented.
//    String source = src(
//        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
//        "void r() {",
//        "  var v;",
//        "  if (v is String) {",
//        "    v.!1length;",
//        "    v.!2getKeys;",
//        "  }",
//        "}");
//    test(source, "1+length", "2-getKeys");
  }

  public void testCommentSnippets052() throws Exception {
    // TODO Enable after type propagation is implemented.
//    String source = src(
//        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
//        "void r() {",
//        "  List<String> values = ['a','b','c'];",
//        "  for (var v in values) {",
//        "    v.!1toUpperCase;",
//        "    v.!2getKeys;",
//        "  }",
//        "}");
//    test(source, "1+toUpperCase", "2-getKeys");
  }

  public void testCommentSnippets053() throws Exception {
    // TODO Enable after type propagation is implemented.
//    String source = src(
//        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
//        "void r() {",
//        "  var v;",
//        "  while (v is String) {",
//        "    v.!1toUpperCase;",
//        "    v.!2getKeys;",
//        "  }",
//        "}");
//    test(source, "1+toUpperCase", "2-getKeys");
  }

  public void testCommentSnippets054() throws Exception {
    // TODO Enable after type propagation is implemented.
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
    // TODO Enable after type propagation is implemented.
//    String source = src(
//        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
//        "void f(var v) {",
//        "  if (v is!! String) {",
//        "    return;",
//        "  }",
//        "  v.!1toUpperCase;",
//        "}");
//    test(source, "1+toUpperCase");
  }

  public void testCommentSnippets057() throws Exception {
    // TODO Enable after type propagation is implemented.
//    String source = src(
//        "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
//        "void f(var v) {",
//        "  if ((v as String).length == 0) {",
//        "    v.!1toUpperCase;",
//        "  }",
//        "}");
//    test(source, "1+toUpperCase");
  }

  public void testCommentSnippets058() throws Exception {
    String source = src(
        "typedef void callback(int k);",
        "void x(callback q){}",
        "void r() {",
        "  callback v;",
        "  x(!1);",
        "}");
    test(source, "1+v");
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
    // TODO Move to new TestCase that includes corelib analysis
//    test("var PHI;main(){PHI=5.3;PHI.abs().!1 Object x;}", "1+abs");
  }

  public void testCommentSnippets063() throws Exception {
    // TODO Enable after type propagation is implemented.
//  String source = src(
//      "class String{int length(){} String toUpperCase(){} bool isEmpty(){}}class Map{getKeys(){}}",
//        "void r(var v) {",
//        "  v.!1toUpperCase;",
//        "  assert(v is String);",
//        "  v.!2toUpperCase;",
//        "}");
//    test(source, "1-toUpperCase", "2+toUpperCase");
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

  public void testCommentSnippets071() throws Exception {
    String source = src(
        "f(int x, [int a,int b, int c]) {",
        "  int q;",
        "  bool f = !!?!1a??b:!!?!2c == ?a?!!?c:?!3b;",
        "}");
    test(source, "1+a", "2+c", "2-q", "3+b");
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
    test("p(x)=>0;var E;f()=>!1p(!2E);", "1+p", "2+E");
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
        "1-FileMode._internal1",
        "1-FileMode._internal");
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
    test("class Q{f(){xy() {};x!1y();}}", "1+xy");
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

  public void testCompletion_alias_field() throws Exception {
    test("typedef int fnint(int k); fn!1int x;", "1+fnint");
  }

  public void testCompletion_constructor_field() throws Exception {
    test("class X { X(this.field); int f!1ield;}", "1+field");
  }

  public void testCompletion_forStmt_vars() throws Exception {
    test(
        "class int{}class Foo { mth() { for (in!1t i = 0; i!2 < 5; i!3++); }}",
        "1+int",
        "2+i",
        "3+i",
        "2-int",
        "3-int");
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

  public void testCompletion_keyword_in() throws Exception {
    test("class Foo { int input = 7; mth() { if (in!1) {}}}", "1+input");
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

  public void testCompletion_topLevelField_init2() throws Exception {
    test("class DateTime{static var JUN;}final num M = Dat!1eTime.JUN;", "1+DateTime", "1-void");
  }

  public void testCompletion_while() throws Exception {
    test("class Foo { int boo = 7; mth() { while (b!1) {} }}", "1+boo");
  }

  // TODO Test for disallowed instance refs from within static methods. Improve proposals for optional params.
  public void testSingle() throws Exception {
    test("class A {int x; !2mth() {int y = this.x;}}class B{}", "2+B");
  }

}

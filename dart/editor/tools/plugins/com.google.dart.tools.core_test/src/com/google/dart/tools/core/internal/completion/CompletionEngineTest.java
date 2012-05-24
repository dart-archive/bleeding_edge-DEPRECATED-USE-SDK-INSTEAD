/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

import com.google.common.base.Joiner;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisTestUtilities;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.model.DartModelException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Short specific code completion tests. Do not remove comments or whitespace. The parser produces
 * different ASTs depending the input characters. This seems like a mis-feature, at best, but it is
 * what we have to deal with today.
 */
public class CompletionEngineTest extends TestCase {

  private static boolean analysisCleared = false;

  public void testCommentSnippets001() throws Exception {
    test(
        "class X {static final num M!4AX = 0;num yc,xc;mth() {xc = yc = MA!1X;x!2c.abs();num f = M!3AX;}}",
        "1+MAX",
        "2+xc",
        "3+MAX",
        "3+Map",
        "3+Math",
        "4+Math"); // not sure 4+Math is correct
  }

  public void testCommentSnippets002() throws Exception {
    test(
        "class Y {String x='hi';mth() {x.l!1ength;int n = 0;x!2.charCodeAt(n!3);}}",
        "1+length",
        "2+x",
        "3+n");
  }

  public void testCommentSnippets003() throws Exception {
    test(
        "class Z {!1Ex!2ception m = const !3Exception!4();mth() {var x = new Li!5st.!6fr!7om(['a']);}}",
        "1+void",
        "2+Exception",
        "3+Object",
        "4+Exception",
        "5+List",
        "6+from",
        "7+from",
        "6-forEach",
        "7-filter");
  }

  public void testCommentSnippets004() throws Exception {
    test(
        "class A {!1int x;!2mth() {!3in!4t y = this.!5x!6;}}",
        "1+void",
        "1+int",
        "2+List",
        "3+x",
        "3-y",
        "4+int",
        "5+mth",
        "6+x");
  }

  public void testCommentSnippets005() throws Exception {
    test("class X { m() { return Ma!1th.P!2I; }}", "1+Math", "2+PI");
  }

  public void testCommentSnippets006() throws Exception {
    test("class B1 {B1();x(){}}class B2 extends B1 {B2() { super.!2x();}}", "2+x");
  }

  public void testCommentSnippets007() throws Exception {
    test(
        "class C {mth(Map x, !1) {}mtf(!2, Map x) {}m() {for (in!3t i=0; i<5; i++); A!4 x;}}",
        "1+bool",
        "2+bool",
        "3+int",
        "4+Arrays");
  }

  public void testCommentSnippets008() throws Exception {
    test("final num PI2 = Mat!1", "1+Math");
  }

  public void testCommentSnippets009() throws Exception {
    // space, char, eol are important
    test(
        "class x extends!5 !2M!3 !4implements!6 !1\n{}",
        "1+Map",
        "1-Math",
        "2+Maps",
        "3+Maps",
        "4-Maps",
        "5-Maps",
        "6-Map");
  }

  public void testCommentSnippets010() throws Exception {
    // space, char, eol are important
    test("class x implements !1{}", "1+Map", "1-Math");
  }

  public void testCommentSnippets011() throws Exception {
    // space, char, eol are important
    test("class x implements M!1{}", "1+Map", "1-Math");
  }

  public void testCommentSnippets012() throws Exception {
    // space, char, eol are important
    test("class x implements M!1\n{}", "1+Map", "1-Math");
  }

  public void testCommentSnippets013() throws Exception {
    test("class x {!1}", "1+num");
  }

  public void testCommentSnippets014() throws Exception {
    // trailing space is important
    test("typedef n!1 ", "1+num");
  }

  public void testCommentSnippets015() throws Exception {
    test("class D {f(){} g(){f!1(f!2);}}", "1+f", "2+f");
  }

  public void testCommentSnippets016() throws Exception {
    test("class F {m() { m(); !1}}", "1+m");
  }

  public void testCommentSnippets017() throws Exception {
    test("class F {var x = fa!1lse;}", "1+true");
  }

  public void testCommentSnippets018() throws Exception {
    test("class C{ m(!1){} n(!2 x, q)", "1+Map", "2+Arrays");
  }

  public void testCommentSnippets019() throws Exception {
    test("class A{m(){Map x;x.!1/**/clear()", "1+toString");
  }

  public void testCommentSnippets020() throws Exception {
    test(
        "class tst {var newt;void newf(){}test() {var newz;new!1/**/;}}",
        "1+newt",
        "1+newf",
        "1+newz",
        "1-Map");
  }

  public void testCommentSnippets021() throws Exception {
    test("class tst {var newt;void newf(){}test() {var newz;new !1/**/;}}", "1+Map", "1-newt");
  }

  public void testCommentSnippets022() throws Exception {
    test("class F{m(){new !1;}}", "1+Map");
  }

  public void testCommentSnippets023() throws Exception {
    test("class X {X c; X(this.!1c) : super() {c.!2}}", "1+c", "2+c");
  }

  public void testCommentSnippets024() throws Exception {
    test("class q {m(Map q){var x;m(!1)}n(){var x;n(!2)}}", "1+x", "2+x");
  }

  public void testCommentSnippets025() throws Exception {
    test("class q {num m() {var q; num x=!1 q + !2/**/;}}", "1+q", "2+q");
  }

  public void testCommentSnippets026() throws Exception {
    test("interface a implements !1{}", "1+List");
  }

  public void testCommentSnippets027() throws Exception {
    test("class test <X extends !1String!2> {}", "1+List", "2+String", "2-List");
  }

  public void testCommentSnippets028() throws Exception {
    test("typedef T Y<T extends !1>(List input);", "1+TimeZone", "1+String");
  }

  public void testCommentSnippets029() throws Exception {
    test("interface A<X> default B<X extends !1List!2> {}", "1+TimeZone", "2+List");
  }

  public void testCommentSnippets030() throws Exception {
    test(
        "class Bar<T extends Foo> {const Bar(!1T!2 k);T!3 m(T!4 a, T!5 b){}T!6 f = null;}",
        "1+T",
        "2+T",
        "3+T",
        "4+T",
        "5+T",
        "6+T");
  }

  public void testCommentSnippets031() throws Exception {
    test(
        "class Bar<T extends Foo> {m(x){if (x is !1) return;if (x is !!!2)}}",
        "1+Map",
        "1+T",
        "2+Map");
  }

  public void testCommentSnippets032() throws Exception {
    test(
        "class Bar<T extends Fooa> {const B!1ara();}",
        "1+BadNumberFormatException",
        "1+Bara",
        "1-Map");
  }

  public void testCommentSnippets033() throws Exception {
    test("t1() {var x;if (x is List) {x.!1add(3);}}", "1+add", "1+length");
  }

  public void testCommentSnippets034() throws Exception {
    test("t2() {var q=[0],z=q.!1length;q.!2isEmpty();}", "1+length", "2+isEmpty");
  }

  public void testCommentSnippets035() throws Exception {
    test("t3() {var x=new List(), y=x.!1length();x.!2isEmpty();}", "1+length", "2+isEmpty");
  }

  public void testCommentSnippets036() throws Exception {
    test("t3() {var x=new List!1}", "1+List");
  }

  public void testCommentSnippets037() throws Exception {
    test("t3() {var x=new List.!1}", "1+from");
  }

  public void testCommentSnippets038() throws Exception {
    test("int xa; String s = '$x!1'", "1+xa");
  }

  public void testCommentSnippets039() throws Exception {
    test("int xa; String s = '$!1'", "1+xa");
  }

  public void testCommentSnippets040() throws Exception {
    test("class X{m(){List list; list.!1 Map map;}}", "1+add");
  }

  public void testCommentSnippets041() throws Exception {
    test("class X{m(){List list; list.!1 zox();}}", "1+add");
  }

  public void testCommentSnippets042() throws Exception {
    test("fd(){Date d=new Date.now();d.!1WED!2;}", "1+day", "2-WED");
  }

  public void testCommentSnippets043() throws Exception {
    test("class L{var k;void.!1}", "1-k");
  }

  public void testCommentSnippets044() throws Exception {
    // see testCompletion_alias_field for reason to not check for XXX in completion list
    test("class XXX {XXX.fisk();}main() {main(); new !1}}", "1+List");
  }

  public void testCommentSnippets045() throws Exception {
    test("class X{var q; f() {q.!1a!2}}", "1+future", "1+end", "2+arguments", "2+abs", "2-end");
  }

  public void testCommentSnippets046() throws Exception {
    // fails because test framework does not set compilation unit
//    test("#import('dart:html', prefix: 'html');f() {var x=new html.Element!1}", "1+Element");
  }

  public void testCommentSnippets047() throws Exception {
    test("f(){int x;int y=!1;}", "1+x");
  }

  public void testCommentSnippets048() throws Exception {
    // fails because test framework does not set compilation unit
//    test("#import('dart:html', prefix: 'html');f() {var x=new ht!1}", "1+html");
  }

  public void testCommentSnippets049() throws Exception {
    // fails because test framework does not set compilation unit
//    test(
//        "#import('dart:html', prefix: 'html');\n#import('dart:json', prefix: 'hxx');f() {var x=new !2h!1;}",
//        "1+html", "1+hxx", "2+html", "2+hxx");
  }

  public void testCommentSnippets050() throws Exception {
    // fails because test framework does not set compilation unit
//    test(
//        "class xdr {xdr();const xdr.a(a,b,c);xdr.b();f() => 3;}class xa{}k() {new x!1dr().f();const xdr.!2a(1, 2, 3);}",
//        "1+xdr", "1+xa", "2+a", "2-b");
  }

  public void testCommentSnippets051() throws Exception {
    String source = Joiner.on("\n").join(
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
    String source = Joiner.on("\n").join(
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
    String source = Joiner.on("\n").join(
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
    String source = Joiner.on("\n").join(
        "void r() {",
        "  var v;",
        "  for (; v is String; v.!1isEmpty) {",
        "    v.!2toUpperCase;",
        "    v.!3getKeys;",
        "  }",
        "}");
    test(source, "1+isEmpty", "2+toUpperCase", "3-getKeys");
  }

  public void testCommentSnippets055() throws Exception {
    String source = Joiner.on("\n").join(
        "void r() {",
        "  String v;",
        "  if (v is Object) {",
        "    v.!1toUpperCase;",
        "  }",
        "}");
    test(source, "1+toUpperCase");
  }

  public void testCompletion_alias_field() throws Exception {
    // fails because test framework does not set compilation unit
    // tests cannot check completion of any type defined in the test
//    test("typedef int fnint(int k); fn!1int x;", "1+fnint");
  }

  public void testCompletion_constructor_field() throws Exception {
    test("class X { X(this.field); int f!1ield;}", "1+field");
  }

  public void testCompletion_forStmt_vars() throws Exception {
    test("class Foo { mth() { for (in!1t i = 0; i!2 < 5; i!3++); }}", "1+int", "2+i", "3+i");
//        "2-int", "3-int"); // not clear these negative results should be eliminated
  }

  public void testCompletion_function() throws Exception {
    test(
        "class Foo { int boo = 7; mth() { PNGS.sort((String a, Str!1) => a.compareTo(b)); }}",
        "1+String");
  }

  public void testCompletion_function_partial() throws Exception {
    // TODO waiting on fix to issue 884
//    test("class Foo { int boo = 7; mth() { PNGS.sort((String a, Str!1)); }}", "1+String");
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
    test("class Foo { myField = 7; mth() { if (m!1) {}}}", "1-myField");
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
    test("topValue = 7; class Foo { mth() { if (t!1) {}}}", "1-topValue");
  }

  public void testCompletion_keyword_in() throws Exception {
    test("class Foo { int input = 7; mth() { if (in!1) {}}}", "1+input");
  }

  public void testCompletion_newMemberType1() throws Exception {
    test("class Foo { !1 }", "1+Collection", "1+List");
  }

  public void testCompletion_newMemberType2() throws Exception {
    test("class Foo {!1}", "1+Collection", "1+List");
  }

  public void testCompletion_newMemberType3() throws Exception {
    test("class Foo {L!1}", "1-Collection", "1+List");
  }

  public void testCompletion_newMemberType4() throws Exception {
    test("class Foo {C!1}", "1+Collection", "1-List");
  }

  public void testCompletion_staticField1() throws Exception {
    test(
    // cannot complete Sunflower due to bug in test framework
        "class Sunflower {static final n!2um MAX_D = 300;nu!3m xc, yc;Sunflower() {x!Xc = y!Yc = MA!1 }}",
        "1+MAX_D",
        "X+xc",
        "Y+yc",
        "2+num",
        "3+num");
  }

  public void testCompletion_topLevelField_init1() throws Exception {
    test("final num PI2 = Mat!1", "1+Math");
  }

  public void testCompletion_topLevelField_init2() throws Exception {
    test("final num PI2 = Mat!1h.PI;", "1+Math", "1+Match", "1-void");
  }

  public void testCompletion_while() throws Exception {
    test("class Foo { int boo = 7; mth() { while (b!1) {} }}", "1+boo");
  }

  /**
   * Run a set of completion tests on the given <code>originalSource</code>. The source string has
   * completion points embedded in it, which are identified by '!X' where X is a single character.
   * Each X is matched to positive or negative results in the array of
   * <code>validationStrings</code>. Validation strings contain the name of a prediction with a two
   * character prefix. The first character of the prefix corresponds to an X in the
   * <code>originalSource</code>. The second character is either a '+' or a '-' indicating whether
   * the string is a positive or negative result.
   * 
   * @param originalSource The source for a completion test that contains completion points
   * @param validationStrings The positive and negative predictions
   */
  private void test(String originalSource, String... results) throws URISyntaxException,
      DartModelException {
    Collection<CompletionSpec> completionTests = CompletionSpec.from(originalSource, results);
    assertTrue(
        "Expected exclamation point ('!') within the source"
            + " denoting the position at which code completion should occur",
        !completionTests.isEmpty());
    if (DartCoreDebug.ANALYSIS_SERVER && !analysisCleared) {
      analysisCleared = true;
      SystemLibraryManagerProvider.getDefaultAnalysisServer().reanalyzeLibraries();
    }
    InMemoryIndex.getInstance().initializeIndex();
    if (DartCoreDebug.ANALYSIS_SERVER) {
      AnalysisTestUtilities.waitForIdle(60000);
    }
    IProgressMonitor monitor = new NullProgressMonitor();
    MockLibrarySource library = new MockLibrarySource("FooLib");
    MockDartSource sourceFile = new MockDartSource(library, "Foo.dart", "");
    for (CompletionSpec test : completionTests) {
      MockCompletionRequestor requestor = new MockCompletionRequestor();
      CompletionEngine engine = new CompletionEngine(null, requestor, null, null, null, monitor);
      engine.complete(library, sourceFile, test.source, test.completionPoint, 0);
      if (test.positiveResults.size() > 0) {
        assertTrue("Expected code completion suggestions", requestor.validate());
      }
      for (String result : test.positiveResults) {
        requestor.assertSuggested(result);
      }
      for (String result : test.negativeResults) {
        requestor.assertNotSuggested(result);
      }
    }
  }
}

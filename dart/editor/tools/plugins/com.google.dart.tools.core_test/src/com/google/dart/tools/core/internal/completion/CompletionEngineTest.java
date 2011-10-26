/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.model.DartModelException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Short specific code completion tests
 */
public class CompletionEngineTest extends TestCase {

//  public void testCompletion_alias_field() throws Exception {
//    // fails because test framework does not set compilation unit
//    // tests cannot check completion of any type defined in the test
//    MockCompletionRequestor requestor = testCompletion("typedef int fnint(int k); fn!int x;");
//    requestor.assertSuggested("fnint");
//  }

  public void testCompletion_constructor_field() throws Exception {
    test("class X { X(this.field); int f!1ield;}", "1+field");
  }

  public void testCompletion_forStmt_vars() throws Exception {
    test("class Foo { mth() { for (in!1t i = 0; i!2 < 5; i!3++); }}", "1+int", "2+i", "3+i");
//        "2-int", "3-int"); // not clear these negative results should be eliminated
  }

  public void testCompletion_ifStmt_field1() throws Exception {
    test("class Foo { int myField = 7; mth() { if (!1) {}}}", "1-myField");
  }

  public void testCompletion_ifStmt_field1a() throws Exception {
    test("class Foo { int myField = 7; mth() { if (!1) }}", "1-myField");
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

  public void testCompletion_return() throws Exception {
    test("class X { m() { return Ma!1th.P!2I; }}", "1+Math", "2+PI");
  }

  public void testCompletion_staticField1() throws Exception {
    test(
        // cannot complete Sunflower due to bug in test framework
        "class Sunflower {static final n!2um MAX_D = 300;nu!3m xc, yc;Sunflower() {x!Xc = y!Yc = MA!1 }}",
        "1+MAX_D", "X+xc", "Y+yc", "2+num", "3+num");
  }

  public void testCompletion_topLevelField_init1() throws Exception {
    test("final num PI2 = Mat!1", "1+Math");
  }

  public void testCompletion_topLevelField_init2() throws Exception {
    test("final num PI2 = Mat!1h.PI;", "1+Math", "1+Match", "1-void");
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
    assertTrue("Expected exclamation point ('!') within the source"
        + " denoting the position at which code completion should occur",
        !completionTests.isEmpty());
    IProgressMonitor monitor = new NullProgressMonitor();
    MockLibrarySource library = new MockLibrarySource("FooLib");
    MockDartSource sourceFile = new MockDartSource(library, "Foo.dart", "");
    for (CompletionSpec test : completionTests) {
      MockCompletionRequestor requestor = new MockCompletionRequestor();
      CompletionEngine engine = new CompletionEngine(null, requestor, null, null, null, monitor);
      engine.complete(library, sourceFile, test.source, test.completionPoint, 0);
      if (test.positiveResults.size() > 0) {
        assertTrue("Expected code completion suggestions", requestor.validate());
      } else {
        assertFalse("Expected code completion suggestions", requestor.validate());
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

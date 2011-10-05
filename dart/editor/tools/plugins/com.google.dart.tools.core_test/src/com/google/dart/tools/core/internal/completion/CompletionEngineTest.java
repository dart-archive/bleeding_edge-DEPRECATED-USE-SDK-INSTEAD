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
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.net.URISyntaxException;
import java.util.Hashtable;

public class CompletionEngineTest extends TestCase {

//  public void testCompletion_ifStmt_field1() throws Exception {
//    // failure expected due to resolver NPE
//    MockCompletionRequestor requestor = testCompletion("class Foo { int myField = 7; mth() { if (!) {}}}");
//    requestor.assertSuggested("myField");
//  }
//
//  public void testCompletion_ifStmt_field1a() throws Exception {
//    // failure expected because parser can't handle if with no test or block
//    MockCompletionRequestor requestor = testCompletion("class Foo { int myField = 7; mth() { if (!) }}");
//    requestor.assertSuggested("myField");
//  }
//
//  public void testCompletion_ifStmt_field2() throws Exception {
//    MockCompletionRequestor requestor = testCompletion("class Foo { int myField = 7; mth() { if (m!) {}}}");
//    requestor.assertSuggested("myField");
//  }
//
//  public void testCompletion_ifStmt_field2a() throws Exception {
//    // failure expected because parser can't handle if with no block
//    MockCompletionRequestor requestor = testCompletion("class Foo { int myField = 7; mth() { if (m!) }}");
//    requestor.assertSuggested("myField");
//  }
//
//  public void testCompletion_ifStmt_field2b() throws Exception {
//    // failure expected because compiler claims myField is not resolvable
//    MockCompletionRequestor requestor = testCompletion("class Foo { myField = 7; mth() { if (m!) {}}}");
//    requestor.assertSuggested("myField");
//  }
//
//  public void testCompletion_ifStmt_localVar() throws Exception {
//    MockCompletionRequestor requestor = testCompletion("class Foo { mth() { int value = 7; if (v!) {}}}");
//    requestor.assertSuggested("value");
//  }
//
//  public void testCompletion_ifStmt_localVara() throws Exception {
//    // failure expected because compiler claims value is not resolvable
//    MockCompletionRequestor requestor = testCompletion("class Foo { mth() { value = 7; if (v!) {}}}");
//    requestor.assertSuggested("value");
//  }
//
//  public void testCompletion_ifStmt_topLevelVar() throws Exception {
//    MockCompletionRequestor requestor = testCompletion("int topValue = 7; class Foo { mth() { if (t!) {}}}");
//    requestor.assertSuggested("topValue");
//  }
//
//  public void testCompletion_ifStmt_topLevelVara() throws Exception {
//    // failure expected because topValue not recognized w/o type declaration
//    MockCompletionRequestor requestor = testCompletion("topValue = 7; class Foo { mth() { if (t!) {}}}");
//    requestor.assertSuggested("topValue");
//  }

  public void testCompletion_newMemberType1() throws Exception {
    MockCompletionRequestor requestor = testCompletion("class Foo { ! }");
    requestor.assertSuggested("List");
    requestor.assertSuggested("Collection");
  }

  public void testCompletion_newMemberType2() throws Exception {
    MockCompletionRequestor requestor = testCompletion("class Foo {!}");
    requestor.assertSuggested("List");
    requestor.assertSuggested("Collection");
  }

  public void testCompletion_newMemberType3() throws Exception {
    MockCompletionRequestor requestor = testCompletion("class Foo {L!}");
    requestor.assertSuggested("List");
    requestor.assertNotSuggested("Collection");
  }

  public void testCompletion_newMemberType4() throws Exception {
    MockCompletionRequestor requestor = testCompletion("class Foo {C!}");
    requestor.assertNotSuggested("List");
    requestor.assertSuggested("Collection");
  }

//  public void testCompletion_staticField1() throws Exception {
//    MockCompletionRequestor requestor = testCompletion("class Sunflower {static final num MAX_D = 300;num xc, yc;Sunflower() {xc = yc = MA! }}");
//    requestor.assertSuggested("MAX_D");
//  }

  /**
   * Generate a series of code completion suggestions for the specified source at the location
   * within the source denoted by an exclaimation point ("!"). An exception is thrown if the source
   * does not contain "!".
   */
  private MockCompletionRequestor testCompletion(String originalSource) throws URISyntaxException,
      DartModelException {

    int index = originalSource.indexOf('!');
    assertTrue("Expected exclamation point ('!') within the source"
        + " denoting the position at which code completion should occur", index != -1);
    String modifiedSource = originalSource.substring(0, index)
        + originalSource.substring(index + 1);

    CompletionEngine engine;
    CompletionEnvironment environment = null;
    MockCompletionRequestor requestor = new MockCompletionRequestor();
    Hashtable<String, String> options = null;
    DartProject project = null;
    WorkingCopyOwner owner = null;
    IProgressMonitor monitor = new NullProgressMonitor();
    engine = new CompletionEngine(environment, requestor, options, project, owner, monitor);

    MockLibrarySource library = new MockLibrarySource("FooLib");
    MockDartSource sourceFile = new MockDartSource(library, "Foo.dart", "");
    engine.complete(library, sourceFile, modifiedSource, index, 0);

    requestor.validate();
    return requestor;
  }
}

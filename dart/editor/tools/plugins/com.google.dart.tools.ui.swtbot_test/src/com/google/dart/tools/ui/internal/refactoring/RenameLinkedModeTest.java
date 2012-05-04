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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.ui.actions.JdtActionConstants;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;

/**
 * Test for {@link RenameLinkedMode}.
 */
public final class RenameLinkedModeTest extends AbstractDartEditorTest {
  /**
   * There was bug in {@link CompilationUnitChange} which did not allow to rename two times. Reason
   * was probably because changes where not saved, so no build was done.
   */
  public void test_renameField_twoTimes() throws Exception {
    // Defs.dart should be separate library to reproduce problem.
    // If we include it using #source, problem will not appear.
    CompilationUnit defsUnit = testProject.setUnitContent(
        "Defs.dart",
        makeSource(
            "#library('Defs');",
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  var test;",
            "}"));
    openEditor(defsUnit);
    // open Test.dart editor
    openTestEditor(
        "#library('Test');",
        "#import('Defs.dart');",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  A a = new A();",
        "  a.test = 1;",
        "}",
        "");
    // rename #1
    {
      selectAndStartRename(" = 1;");
      // animate linked mode rename
      EventSender eventSender = new EventSender(textWidget);
      eventSender.keyDown('2');
      eventSender.keyDown(SWT.CR);
      waitEventLoop(0);
      // assert source
      assertUnitContent(
          defsUnit,
          makeSource(
              "#library('Defs');",
              "// filler filler filler filler filler filler filler filler filler filler",
              "class A {",
              "  var test2;",
              "}"));
      assertTestUnitContent(
          "#library('Test');",
          "#import('Defs.dart');",
          "// filler filler filler filler filler filler filler filler filler filler",
          "f() {",
          "  A a = new A();",
          "  a.test2 = 1;",
          "}",
          "");
    }
    // rename #1
    {
      selectAndStartRename(" = 1;");
      // animate linked mode rename
      EventSender eventSender = new EventSender(textWidget);
      eventSender.keyDown(SWT.BS);
      eventSender.keyDown('3');
      eventSender.keyDown(SWT.CR);
      waitEventLoop(0);
      // assert source
      assertUnitContent(
          defsUnit,
          makeSource(
              "#library('Defs');",
              "// filler filler filler filler filler filler filler filler filler filler",
              "class A {",
              "  var test3;",
              "}"));
      assertTestUnitContent(
          "#library('Test');",
          "#import('Defs.dart');",
          "// filler filler filler filler filler filler filler filler filler filler",
          "f() {",
          "  A a = new A();",
          "  a.test3 = 1;",
          "}",
          "");
    }
  }

  public void test_renameLocalVariable_onInvocationExpression() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "}",
        "f() {",
        "  A test = new A();",
        "",
        "",
        "  test.foo();",
        "}",
        "");
    waitEventLoop(3000);
    selectAndReveal("est.foo()");
    // initiate rename
    IAction renameAction = getEditorAction(JdtActionConstants.RENAME);
    renameAction.run();
    // animate linked mode rename
    EventSender eventSender = new EventSender(textWidget);
    eventSender.keyDown(SWT.BS);
    eventSender.keyDown('r');
    eventSender.keyDown(SWT.CR);
    // assert source
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "}",
        "f() {",
        "  A rest = new A();",
        "",
        "",
        "  rest.foo();",
        "}",
        "");
  }

  private void selectAndStartRename(String pattern) throws Exception {
    selectAndReveal(pattern);
    // initiate rename
    IAction renameAction = getEditorAction(JdtActionConstants.RENAME);
    renameAction.run();
  }
}

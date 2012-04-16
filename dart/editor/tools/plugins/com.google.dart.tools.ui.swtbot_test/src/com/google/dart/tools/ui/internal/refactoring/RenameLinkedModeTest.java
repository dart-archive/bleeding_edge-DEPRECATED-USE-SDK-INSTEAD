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

import com.google.dart.tools.ui.actions.JdtActionConstants;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;

/**
 * Test for {@link RenameLinkedMode}.
 */
public final class RenameLinkedModeTest extends AbstractDartEditorTest {
  public void test_renameLocalVariable_onInvocationExpression() throws Exception {
    openEditor(
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
}

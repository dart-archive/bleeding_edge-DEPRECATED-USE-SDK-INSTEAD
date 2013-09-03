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
package editor.refactoring;

import com.google.dart.tools.ui.actions.DartActionConstants;
import com.google.dart.ui.test.helpers2.WizardDialogHelper;

import static com.google.dart.ui.test.util.UiContext2.runAction;
import static com.google.dart.ui.test.util.UiContext2.setTextByLabel;
import static com.google.dart.ui.test.util.UiContext2.waitForActionEnabled;
import static com.google.dart.ui.test.util.UiContext2.waitForShell;
import static com.google.dart.ui.test.util.UiContext2.waitForShellClosed;

import editor.AbstractDartEditorTabTest2;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;

/**
 * Test for the "Rename" refactoring.
 */
public final class RenameRefactoringTest2 extends AbstractDartEditorTabTest2 {
  private static class RenameDialogHelper extends WizardDialogHelper {
    public RenameDialogHelper(Shell shell) {
      super(shell);
    }

    public void setName(String name) {
      setTextByLabel(shell, "New name:", name);
    }
  }

  public void test_renameClass() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "main() {",
        "  A v = new A();",
        "}");
    findAndStartRename("A v");
    // animate wizard dialog
    Shell shell = waitForShell("Rename Class");
    RenameDialogHelper helper = new RenameDialogHelper(shell);
    {
      // invalid name
      helper.setName("-A");
      helper.assertMessage("Class name must not start with '-'.");
      // set new name
      helper.setName("A2");
      helper.assertNoMessage();
      // done
      helper.closeOK();
    }
    waitForShellClosed(shell);
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A2 {}",
        "main() {",
        "  A2 v = new A2();",
        "}");
  }

  public void test_renameField() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int f;",
        "}",
        "foo(A a) {",
        "  a.f = 42;",
        "}");
    findAndStartRename("f;");
    // animate wizard dialog
    Shell shell = waitForShell("Rename Field");
    RenameDialogHelper helper = new RenameDialogHelper(shell);
    {
      helper.setName("newName");
      helper.closeOK();
    }
    waitForShellClosed(shell);
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName;",
        "}",
        "foo(A a) {",
        "  a.newName = 42;",
        "}");
  }

  public void test_renameTopLevelVariable() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var myVar;",
        "main() {",
        "  myVar = 42;",
        "}");
    findAndStartRename("myVar;");
    // animate wizard dialog
    Shell shell = waitForShell("Rename Top-Level Variable");
    RenameDialogHelper helper = new RenameDialogHelper(shell);
    {
      helper.setName("newName");
      helper.closeOK();
    }
    waitForShellClosed(shell);
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "main() {",
        "  newName = 42;",
        "}");
  }

  private void findAndStartRename(String pattern) throws Exception {
    selectOffset(pattern);
    // run "Rename" action
    IAction renameAction = getEditorAction(DartActionConstants.RENAME);
    waitForActionEnabled(renameAction);
    runAction(renameAction);
  }
}

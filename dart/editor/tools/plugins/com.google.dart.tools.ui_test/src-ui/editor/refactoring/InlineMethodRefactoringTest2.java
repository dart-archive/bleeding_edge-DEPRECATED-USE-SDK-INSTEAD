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

import static com.google.dart.ui.test.util.UiContext2.findButton;
import static com.google.dart.ui.test.util.UiContext2.getSelection;
import static com.google.dart.ui.test.util.UiContext2.isEnabled;
import static com.google.dart.ui.test.util.UiContext2.runAction;
import static com.google.dart.ui.test.util.UiContext2.setSelection;
import static com.google.dart.ui.test.util.UiContext2.waitForActionEnabled;
import static com.google.dart.ui.test.util.UiContext2.waitForShell;
import static com.google.dart.ui.test.util.UiContext2.waitForShellClosed;

import editor.AbstractDartEditorTabTest2;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * Test for the "Inline Method" refactoring.
 */
public final class InlineMethodRefactoringTest2 extends AbstractDartEditorTabTest2 {
  private static class WizardHelper extends WizardDialogHelper {
    public WizardHelper(Shell shell) {
      super(shell);
    }

    public Button getAllButton() {
      return findButton(shell, "All invocations");
    }

    public Button getDeleteButton() {
      return findButton(shell, "Delete method declaration");
    }

    public Button getSelectedButton() {
      return findButton(shell, "Only the selected invocation");
    }
  }

  private abstract static class WizardOperation {
    abstract void run(WizardHelper helper);
  }

  public void test_function_declaration() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 123;",
        "main() {",
        "  print(test()); // 1",
        "  print(test()); // 2",
        "}");
    selectAndInline("test() =>", new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // check buttons
        assertTrue(getSelection(helper.getAllButton()));
        {
          Button deleteButton = helper.getDeleteButton();
          assertTrue(isEnabled(deleteButton));
          assertTrue(getSelection(deleteButton));
        }
        assertFalse(getSelection(helper.getSelectedButton()));
        // done
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123); // 1",
        "  print(123); // 2",
        "}");
  }

  public void test_function_selectedInvocation_inlineAll() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 123;",
        "main() {",
        "  print(test()); // 1",
        "  print(test()); // 2",
        "}");
    selectAndInline("test()); // 1", new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // inline all
        setSelection(helper.getAllButton(), true);
        {
          Button deleteButton = helper.getDeleteButton();
          assertTrue(isEnabled(deleteButton));
          assertTrue(getSelection(deleteButton));
        }
        // done
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123); // 1",
        "  print(123); // 2",
        "}");
  }

  public void test_function_selectedInvocation_inlineSelected() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 123;",
        "main() {",
        "  print(test()); // 1",
        "  print(test()); // 2",
        "}");
    selectAndInline("test()); // 1", new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // check default settings
        assertFalse(getSelection(helper.getAllButton()));
        {
          Button deleteButton = helper.getDeleteButton();
          assertFalse(isEnabled(deleteButton));
          assertTrue(getSelection(deleteButton));
        }
        assertTrue(getSelection(helper.getSelectedButton()));
        // done
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 123;",
        "main() {",
        "  print(123); // 1",
        "  print(test()); // 2",
        "}");
  }

  public void test_method_declaration() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() => 123;",
        "  main() {",
        "    print(test()); // 1",
        "    print(test()); // 2",
        "  }",
        "}");
    selectAndInline("test() =>", new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        assertTrue(getSelection(helper.getAllButton()));
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main() {",
        "    print(123); // 1",
        "    print(123); // 2",
        "  }",
        "}");
  }

  private void selectAndInline(String pattern, WizardOperation operation) throws Exception {
    selectOffset(pattern);
    // run action
    IAction action = getEditorAction(DartActionConstants.INLINE);
    waitForActionEnabled(action);
    runAction(action);
    // animate wizard dialog
    Shell shell = waitForShell("Inline Method");
    WizardHelper helper = new WizardHelper(shell);
    operation.run(helper);
    waitForShellClosed(shell);
  }
}

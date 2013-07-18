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

import static com.google.dart.ui.test.util.UiContext2.findLabel;
import static com.google.dart.ui.test.util.UiContext2.runAction;
import static com.google.dart.ui.test.util.UiContext2.waitForActionEnabled;
import static com.google.dart.ui.test.util.UiContext2.waitForShell;
import static com.google.dart.ui.test.util.UiContext2.waitForShellClosed;

import editor.AbstractDartEditorTabTest2;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Test for the "Inline Local" refactoring.
 */
public final class InlineLocalRefactoringTest2 extends AbstractDartEditorTabTest2 {

  private static abstract class WizardOperation {
    abstract void run(WizardDialogHelper helper);
  }

  public void test_oneOccurrence() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var test = 123;",
        "  print(test);",
        "}");
    selectAndInline("test", new WizardOperation() {
      @Override
      void run(WizardDialogHelper helper) {
        // check message
        Label msg = findLabel(helper.shell, "Inline 1 occurrence of local variable 'test' ?");
        assertNotNull(msg);
        // done
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "}");
  }

  public void test_twoOccurrences() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var test = 123;",
        "  print(test);",
        "  print(test);",
        "}");
    selectAndInline("test", new WizardOperation() {
      @Override
      void run(WizardDialogHelper helper) {
        // check message
        Label msg = findLabel(helper.shell, "Inline 2 occurrences of local variable 'test' ?");
        assertNotNull(msg);
        // done
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "  print(123);",
        "}");
  }

  private void selectAndInline(String pattern, WizardOperation operation) throws Exception {
    selectRange(pattern);
    // run action
    IAction action = getEditorAction(DartActionConstants.INLINE);
    waitForActionEnabled(action);
    runAction(action);
    // animate wizard dialog
    Shell shell = waitForShell("Inline Local Variable");
    WizardDialogHelper helper = new WizardDialogHelper(shell);
    operation.run(helper);
    waitForShellClosed(shell);
  }
}

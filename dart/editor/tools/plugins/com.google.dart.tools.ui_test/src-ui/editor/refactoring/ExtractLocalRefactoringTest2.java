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
import static com.google.dart.ui.test.util.UiContext2.runAction;
import static com.google.dart.ui.test.util.UiContext2.setSelection;
import static com.google.dart.ui.test.util.UiContext2.setTextByLabel;
import static com.google.dart.ui.test.util.UiContext2.waitForActionEnabled;
import static com.google.dart.ui.test.util.UiContext2.waitForShell;
import static com.google.dart.ui.test.util.UiContext2.waitForShellClosed;

import editor.AbstractDartEditorTabTest2;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * Test for the "Rename" refactoring.
 */
public final class ExtractLocalRefactoringTest2 extends AbstractDartEditorTabTest2 {
  private static class WizardHelper extends WizardDialogHelper {
    public WizardHelper(Shell shell) {
      super(shell);
    }

    public boolean getReplaceAll() {
      Button button = findButton(shell, "Replace all.*");
      return getSelection(button);
    }

    public void setName(String name) {
      setTextByLabel(shell, "Variable name:", name);
    }

    public void setReplaceAll(boolean replaceAll) {
      Button button = findButton(shell, "Replace all.*");
      setSelection(button, replaceAll);
    }
  }

  private static abstract class WizardOperation {
    abstract void run(WizardHelper helper);
  }

  public void test_replaceAll_extractOnlyOne() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "  print(123);",
        "}");
    selectAndExtract("123", new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // don't extract all
        helper.setReplaceAll(false);
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 123;",
        "  print(res);",
        "  print(123);",
        "}");
  }

  public void test_replaceAll_trueByDefault() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "  print(123);",
        "}");
    selectAndExtract("123", new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // "Replace all..." should checked by default
        assertTrue(helper.getReplaceAll());
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 123;",
        "  print(res);",
        "  print(res);",
        "}");
  }

  public void test_singleExpression() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "}");
    selectAndExtract("123", new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // invalid name
        helper.setName("-name");
        helper.assertMessage("Variable name must not start with '-'.");
        // set new name
        helper.setName("res");
        helper.assertNoMessage();
        // done
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 123;",
        "  print(res);",
        "}");
  }

  private void selectAndExtract(String pattern, WizardOperation operation) throws Exception {
    selectRange(pattern);
    // run action
    IAction action = getEditorAction(DartActionConstants.EXTRACT_LOCAL);
    waitForActionEnabled(action);
    runAction(action);
    // animate wizard dialog
    Shell shell = waitForShell("Extract Local Variable");
    WizardHelper helper = new WizardHelper(shell);
    operation.run(helper);
    waitForShellClosed(shell);
  }
}

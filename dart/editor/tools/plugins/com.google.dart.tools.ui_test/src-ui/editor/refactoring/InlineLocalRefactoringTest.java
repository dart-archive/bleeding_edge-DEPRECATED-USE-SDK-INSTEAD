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

import com.google.dart.tools.ui.actions.JdtActionConstants;
import com.google.dart.ui.test.driver.Operation;
import com.google.dart.ui.test.driver.ShellOperation;
import com.google.dart.ui.test.helpers.WizardDialogHelper;
import com.google.dart.ui.test.util.UiContext;

import editor.AbstractDartEditorTabTest;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Label;

import java.util.concurrent.TimeUnit;

/**
 * Test for the "Inline Local" refactoring.
 */
public final class InlineLocalRefactoringTest extends AbstractDartEditorTabTest {

  public void test_oneOccurrence() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var test = 123;",
        "  print(test);",
        "}");
    // run action
    selectAndInline("test");
    // animate wizard dialog
    addOperation(new ShellOperation("Inline Local Variable") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardDialogHelper helper = new WizardDialogHelper(context);
        // check message
        Label msg = context.findLabel("Inline 1 occurrence of local variable 'test' ?");
        assertNotNull(msg);
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    selectAndInline("test");
    // animate wizard dialog
    addOperation(new ShellOperation("Inline Local Variable") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardDialogHelper helper = new WizardDialogHelper(context);
        // check message
        Label msg = context.findLabel("Inline 2 occurrences of local variable 'test' ?");
        assertNotNull(msg);
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "  print(123);",
        "}");
  }

  private void selectAndInline(String pattern) throws Exception {
    selectOffset(pattern);
    final IAction action = getEditorAction(JdtActionConstants.INLINE);
    addOperation(new Operation() {
      @Override
      public boolean isReady(UiContext context) throws Exception {
        return action.isEnabled();
      }

      @Override
      public void run(UiContext context) throws Exception {
        action.run();
      }
    });
  }
}

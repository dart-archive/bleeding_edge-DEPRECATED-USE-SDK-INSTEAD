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
import com.google.dart.ui.test.driver.Operation;
import com.google.dart.ui.test.driver.ShellOperation;
import com.google.dart.ui.test.helpers.WizardDialogHelper;
import com.google.dart.ui.test.util.UiContext;

import editor.AbstractDartEditorTabTest;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Button;

import java.util.concurrent.TimeUnit;

/**
 * Test for the "Inline Method" refactoring.
 */
public final class InlineMethodRefactoringTest extends AbstractDartEditorTabTest {
  private static class WizardHelper extends WizardDialogHelper {
    public WizardHelper(UiContext context) {
      super(context);
    }

    public Button getAllButton() {
      return context.findButton("All invocations");
    }

    public Button getDeleteButton() {
      return context.findButton("Delete method declaration");
    }

    public Button getSelectedButton() {
      return context.findButton("Only the selected invocation");
    }
  }
  private abstract static class WizardOperation {
    abstract void run(UiContext context, WizardHelper helper);
  }

  public void test_function_declaration() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 123;",
        "main() {",
        "  print(test()); // 1",
        "  print(test()); // 2",
        "}");
    // run action
    selectAndInline("test() =>", new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        // check buttons
        assertTrue(helper.getAllButton().getSelection());
        {
          Button deleteButton = helper.getDeleteButton();
          assertTrue(deleteButton.isEnabled());
          assertTrue(deleteButton.getSelection());
        }
        assertFalse(helper.getSelectedButton().getSelection());
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    selectAndInline("test()); // 1", new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        // inline all
        context.setButtonSelection(helper.getAllButton(), true);
        {
          Button deleteButton = helper.getDeleteButton();
          assertTrue(deleteButton.isEnabled());
          assertTrue(deleteButton.getSelection());
        }
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    selectAndInline("test()); // 1", new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        // check default settings
        assertFalse(helper.getAllButton().getSelection());
        {
          Button deleteButton = helper.getDeleteButton();
          assertFalse(deleteButton.isEnabled());
          assertTrue(deleteButton.getSelection());
        }
        assertTrue(helper.getSelectedButton().getSelection());
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    selectAndInline("test() =>", new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        assertTrue(helper.getAllButton().getSelection());
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main() {",
        "    print(123); // 1",
        "    print(123); // 2",
        "  }",
        "}");
  }

  private void selectAndInline(String pattern, final WizardOperation operation) throws Exception {
    selectOffset(pattern);
    final IAction action = getEditorAction(DartActionConstants.INLINE);
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
    addOperation(new ShellOperation("Inline Method") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardHelper helper = new WizardHelper(context);
        operation.run(context, helper);
      }
    });
  }
}

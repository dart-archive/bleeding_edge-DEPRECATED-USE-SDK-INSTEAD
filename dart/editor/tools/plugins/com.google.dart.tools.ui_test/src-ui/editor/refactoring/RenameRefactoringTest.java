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

import java.util.concurrent.TimeUnit;

/**
 * Test for the "Rename" refactoring.
 */
public final class RenameRefactoringTest extends AbstractDartEditorTabTest {
  private static class WizardHelper extends WizardDialogHelper {
    public WizardHelper(UiContext context) {
      super(context);
    }

    public void setName(String name) {
      context.findTextByLabel("New name:").setText(name);
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
    addOperation(new ShellOperation("Rename Class") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardHelper helper = new WizardHelper(context);
        // invalid name
        helper.setName("-A");
        helper.assertMessage("Class name must not start with '-'.");
        // set new name
        helper.setName("A2");
        helper.assertNoMessage();
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    addOperation(new ShellOperation("Rename Field") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardHelper helper = new WizardHelper(context);
        // set new name
        helper.setName("newName");
        helper.assertNoMessage();
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    addOperation(new ShellOperation("Rename Top-Level Variable") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardHelper helper = new WizardHelper(context);
        // set new name
        helper.setName("newName");
        helper.assertNoMessage();
        // done
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    final IAction renameAction = getEditorAction(DartActionConstants.RENAME);
    addOperation(new Operation() {
      @Override
      public boolean isReady(UiContext context) throws Exception {
        return renameAction.isEnabled();
      }

      @Override
      public void run(UiContext context) throws Exception {
        renameAction.run();
      }
    });
  }
}

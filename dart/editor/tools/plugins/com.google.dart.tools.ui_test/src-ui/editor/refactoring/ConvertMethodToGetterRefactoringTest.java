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
import com.google.dart.ui.test.helpers.DialogHelper;
import com.google.dart.ui.test.util.UiContext;

import editor.AbstractDartEditorTabTest;

import org.eclipse.jface.action.IAction;

import java.util.concurrent.TimeUnit;

/**
 * Test for the "Convert Method to Getter" refactoring.
 */
public final class ConvertMethodToGetterRefactoringTest extends AbstractDartEditorTabTest {

  public void test_ok() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 123;",
        "main() {",
        "  print(test());",
        "}");
    // run action
    selectAndRun("test");
    // animate wizard dialog
    addOperation(new ShellOperation("Convert method to getter") {
      @Override
      public void run(UiContext context) throws Exception {
        DialogHelper helper = new DialogHelper(context);
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get test => 123;",
        "main() {",
        "  print(test);",
        "}");
  }

  private void selectAndRun(String pattern) throws Exception {
    selectOffset(pattern);
    final IAction action = getEditorAction(DartActionConstants.CONVERT_METHOD_TO_GETTER);
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

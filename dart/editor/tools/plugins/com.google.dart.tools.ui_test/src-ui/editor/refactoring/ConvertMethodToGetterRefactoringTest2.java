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
import com.google.dart.ui.test.helpers2.DialogHelper;

import static com.google.dart.ui.test.util.UiContext2.runAction;
import static com.google.dart.ui.test.util.UiContext2.waitForActionEnabled;
import static com.google.dart.ui.test.util.UiContext2.waitForShell;
import static com.google.dart.ui.test.util.UiContext2.waitForShellClosed;

import editor.AbstractDartEditorTabTest2;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;

/**
 * Test for the "Convert Method to Getter" refactoring.
 */
public final class ConvertMethodToGetterRefactoringTest2 extends AbstractDartEditorTabTest2 {
  public void test_ok() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 123;",
        "main() {",
        "  print(test());",
        "}");
    selectAndRun("test");
    // animate wizard dialog
    Shell shell = waitForShell("Convert method to getter");
    {
      DialogHelper helper = new DialogHelper(shell);
      helper.closeOK();
    }
    waitForShellClosed(shell);
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get test => 123;",
        "main() {",
        "  print(test);",
        "}");
  }

  private void selectAndRun(String pattern) throws Exception {
    selectRange(pattern);
    // run action
    IAction action = getEditorAction(DartActionConstants.CONVERT_METHOD_TO_GETTER);
    waitForActionEnabled(action);
    runAction(action);
  }
}

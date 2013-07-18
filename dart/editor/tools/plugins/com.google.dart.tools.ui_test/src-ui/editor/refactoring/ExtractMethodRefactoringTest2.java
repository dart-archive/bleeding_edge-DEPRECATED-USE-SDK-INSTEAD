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
import com.google.dart.ui.test.helpers2.WizardDialogHelper;
import com.google.dart.ui.test.runnable.UIThreadRunnable;
import com.google.dart.ui.test.runnable.VoidResult;

import static com.google.dart.ui.test.util.UiContext2.clickButton;
import static com.google.dart.ui.test.util.UiContext2.clickButtonAsync;
import static com.google.dart.ui.test.util.UiContext2.findButton;
import static com.google.dart.ui.test.util.UiContext2.findTable;
import static com.google.dart.ui.test.util.UiContext2.getSelection;
import static com.google.dart.ui.test.util.UiContext2.getTextByLabel;
import static com.google.dart.ui.test.util.UiContext2.isEnabled;
import static com.google.dart.ui.test.util.UiContext2.runAction;
import static com.google.dart.ui.test.util.UiContext2.setButtonSelection;
import static com.google.dart.ui.test.util.UiContext2.setSelection;
import static com.google.dart.ui.test.util.UiContext2.setTextByLabel;
import static com.google.dart.ui.test.util.UiContext2.waitForActionEnabled;
import static com.google.dart.ui.test.util.UiContext2.waitForShell;
import static com.google.dart.ui.test.util.UiContext2.waitForShellClosed;

import editor.AbstractDartEditorTabTest2;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for the "Extract Method" refactoring.
 */
public final class ExtractMethodRefactoringTest2 extends AbstractDartEditorTabTest2 {
  private static class ParameterDialogHelper extends DialogHelper {
    public ParameterDialogHelper(Shell shell) {
      super(shell);
    }

    public String getName() {
      return getTextByLabel(shell, "Name:");
    }

    public String getType() {
      return getTextByLabel(shell, "Type:");
    }

    public void setName(String newName) {
      setTextByLabel(shell, "Name:", newName);
    }

    public void setType(String newType) {
      setTextByLabel(shell, "Type:", newType);
    }
  }

  private static class WizardHelper extends WizardDialogHelper {
    public WizardHelper(Shell shell) {
      super(shell);
    }

    public Button findGetterButton() {
      return findButton(shell, "Extract getter .*");
    }

    public String getSignaturePreview() {
      return getTextByLabel(shell, "Method signature preview:");
    }

    public void setName(String name) {
      setTextByLabel(shell, "Method name:", name);
    }

    public void setReplaceAll(boolean replaceAll) {
      setButtonSelection(shell, "Replace .*", replaceAll);
    }
  }

  private static abstract class WizardOperation {
    abstract void run(WizardHelper helper);
  }

  private static void assertTableItems(final Table table, final String expectedItems[][]) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        assertThat(table.getItemCount()).isEqualTo(expectedItems.length);
        for (int i = 0; i < expectedItems.length; i++) {
          TableItem item = table.getItem(i);
          String[] expectedItem = expectedItems[i];
          for (int j = 0; j < expectedItem.length; j++) {
            assertEquals(expectedItem[j], item.getText(j));
          }
        }
      }
    });
  }

  public void test_occurrences_disable() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(123);",
        "// end",
        "  print(123);",
        "}");
    setSelectionFromStartEndComments();
    runExtractAction(new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // don't replace occurrences
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
        "// start",
        "  res();",
        "// end",
        "  print(123);",
        "}",
        "",
        "void res() {",
        "  print(123);",
        "}");
  }

  public void test_occurrences_one() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(123);",
        "// end",
        "  print(123);",
        "}");
    setSelectionFromStartEndComments();
    runExtractAction(new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        Button replaceButton = findButton(helper.shell, "Replace 1 additional occurrence of .*");
        assertTrue(getSelection(replaceButton));
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  res();",
        "// end",
        "  res();",
        "}",
        "",
        "void res() {",
        "  print(123);",
        "}");
  }

  public void test_occurrences_two() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(123);",
        "// end",
        "  print(123);",
        "  print(123);",
        "}");
    setSelectionFromStartEndComments();
    runExtractAction(new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        Button replaceButton = findButton(helper.shell, "Replace 2 additional occurrences of .*");
        assertTrue(getSelection(replaceButton));
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  res();",
        "// end",
        "  res();",
        "  res();",
        "}",
        "",
        "void res() {",
        "  print(123);",
        "}");
  }

  public void test_parameters() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a = 1;",
        "  var b = 2;",
        "  print(a + b);",
        "}");
    selectRange("a + b");
    runExtractAction();
    // animate wizard dialog
    Shell shell = waitForShell("Extract Method");
    WizardHelper helper = new WizardHelper(shell);
    {
      // check table
      Table table = findTable(shell);
      assertTableItems(table, new String[][] { {"dynamic", "a"}, {"dynamic", "b"}});
      // edit parameter "a"
      clickButtonAsync(shell, "Edit...");
      {
        Shell parameterShell = waitForShell("Method Parameter");
        ParameterDialogHelper parameterHelper = new ParameterDialogHelper(parameterShell);
        {
          assertEquals("dynamic", parameterHelper.getType());
          assertEquals("a", parameterHelper.getName());
          parameterHelper.setType("int");
          parameterHelper.setName("p1");
          parameterHelper.closeOK();
        }
        waitForShellClosed(parameterShell);
      }
      assertTableItems(table, new String[][] { {"int", "p1"}, {"dynamic", "b"}});
      // move "a" down
      clickButton(shell, "Down");
      assertTableItems(table, new String[][] { {"dynamic", "b"}, {"int", "p1"}});
      // done
      helper.setName("res");
      helper.closeOK();
    }
    waitForShellClosed(shell);
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a = 1;",
        "  var b = 2;",
        "  print(res(b, a));",
        "}",
        "",
        "res(b, int p1) => p1 + b;");
  }

  public void test_singleExpression_getterByDefault() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "  print(123);",
        "}");
    selectRange("123");
    runExtractAction(new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        Button getterButton = helper.findGetterButton();
        assertTrue(isEnabled(getterButton));
        assertTrue(getSelection(getterButton));
        // done
        helper.setName("res");
        assertEquals("get res", helper.getSignaturePreview());
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(res);",
        "  print(res);",
        "}",
        "",
        "int get res => 123;");
  }

  public void test_singleExpression_getterDisable() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(123);",
        "  print(123);",
        "}");
    selectRange("123");
    runExtractAction(new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        setSelection(helper.findGetterButton(), false);
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(res());",
        "  print(res());",
        "}",
        "",
        "int res() => 123;");
  }

  public void test_singleStatement() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(123);",
        "// end",
        "}");
    setSelectionFromStartEndComments();
    runExtractAction(new WizardOperation() {
      @Override
      void run(WizardHelper helper) {
        // not getter
        Button getterButton = helper.findGetterButton();
        assertFalse(isEnabled(getterButton));
        assertFalse(getSelection(getterButton));
        // invalid name
        helper.setName("-name");
        helper.assertMessage("Method name must not start with '-'.");
        // set new name
        helper.setName("res");
        assertEquals("res()", helper.getSignaturePreview());
        helper.assertNoMessage();
        // done
        helper.closeOK();
      }
    });
    // validate result
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  res();",
        "// end",
        "}",
        "",
        "void res() {",
        "  print(123);",
        "}");
  }

  private void runExtractAction() {
    IAction action = getEditorAction(DartActionConstants.EXTRACT_METHOD);
    waitForActionEnabled(action);
    runAction(action);
  }

  private void runExtractAction(WizardOperation operation) throws Exception {
    runExtractAction();
    // animate wizard dialog
    Shell shell = waitForShell("Extract Method");
    WizardHelper helper = new WizardHelper(shell);
    operation.run(helper);
    waitForShellClosed(shell);
  }

  private void setSelectionFromStartEndComments() throws Exception {
    int start = findEnd("// start") + EOL.length();
    int end = findOffset("// end");
    selectRange(start, end);
  }
}

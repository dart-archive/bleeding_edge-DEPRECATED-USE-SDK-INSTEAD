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
import com.google.dart.ui.test.helpers.DialogHelper;
import com.google.dart.ui.test.helpers.WizardDialogHelper;
import com.google.dart.ui.test.util.UiContext;

import editor.AbstractDartEditorTabTest;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

/**
 * Test for the "Extract Method" refactoring.
 */
public final class ExtractMethodRefactoringTest extends AbstractDartEditorTabTest {
  private static class ParameterDialogHelper extends DialogHelper {
    public ParameterDialogHelper(UiContext context) {
      super(context);
    }

    public String getName() {
      return context.findTextByLabel("Name:").getText();
    }

    public String getType() {
      return context.findTextByLabel("Type:").getText();
    }

    public void setName(String newName) {
      context.findTextByLabel("Name:").setText(newName);
    }

    public void setType(String newType) {
      context.findTextByLabel("Type:").setText(newType);
    }

  }

  private static class WizardHelper extends WizardDialogHelper {
    public WizardHelper(UiContext context) {
      super(context);
    }

    public Button findGetterButton() {
      return context.findButton("Extract getter .*");
    }

    public String getSignaturePreview() {
      Widget previewWidget = context.findWidgetByLabel("Method signature preview:");
      return UiContext.getText(previewWidget);
    }

    public void setName(String name) {
      context.findTextByLabel("Method name:").setText(name);
    }
  }

  private abstract static class WizardOperation {
    abstract void run(UiContext context, WizardHelper helper);
  }

  private static void assertTableItems(Table table, String expectedItems[][]) {
    assertThat(table.getItemCount()).isEqualTo(expectedItems.length);
    for (int i = 0; i < expectedItems.length; i++) {
      TableItem item = table.getItem(i);
      String[] expectedItem = expectedItems[i];
      for (int j = 0; j < expectedItem.length; j++) {
        assertEquals(expectedItem[j], item.getText(j));
      }
    }
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
    // run action
    setSelectionFromStartEndComments();
    runExtractAction();
    // animate wizard
    animateExtractWizard(new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        // don't replace occurrences
        context.setButtonSelection("Replace .*", false);
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    setSelectionFromStartEndComments();
    runExtractAction();
    // animate wizard
    animateExtractWizard(new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        Button replaceButton = context.findButton("Replace 1 additional occurrence of .*");
        assertTrue(replaceButton.getSelection());
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    setSelectionFromStartEndComments();
    runExtractAction();
    // animate wizard
    animateExtractWizard(new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        Button replaceButton = context.findButton("Replace 2 additional occurrences of .*");
        assertTrue(replaceButton.getSelection());
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    selectRange("a + b");
    runExtractAction();
    // animate wizard
    addOperation(new ShellOperation("Extract Method") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardHelper helper = new WizardHelper(context);
        // check table
        Table table = context.findTable();
        assertTableItems(table, new String[][] { {"dynamic", "a"}, {"dynamic", "b"}});
        // edit parameter "a"
        context.clickButton("Edit...");
        assertTableItems(table, new String[][] { {"int", "p1"}, {"dynamic", "b"}});
        // move "a" down
        context.clickButton("Down");
        assertTableItems(table, new String[][] { {"dynamic", "b"}, {"int", "p1"}});
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // animate "a" dialog
    addOperation(new ShellOperation("Method Parameter") {
      @Override
      public void run(UiContext context) throws Exception {
        ParameterDialogHelper helper = new ParameterDialogHelper(context);
        assertEquals("dynamic", helper.getType());
        assertEquals("a", helper.getName());
        helper.setType("int");
        helper.setName("p1");
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(10, TimeUnit.SECONDS);
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
    // run action
    selectRange("123");
    runExtractAction();
    // animate wizard
    animateExtractWizard(new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        Button getterButton = helper.findGetterButton();
        assertTrue(getterButton.isEnabled());
        assertTrue(getterButton.getSelection());
        // done
        helper.setName("res");
        assertEquals("get res", helper.getSignaturePreview());
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    selectRange("123");
    runExtractAction();
    // animate wizard
    animateExtractWizard(new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        context.setButtonSelection(helper.findGetterButton(), false);
        // done
        helper.setName("res");
        helper.closeOK();
      }
    });
    // validate result
    runUiOperations(60, TimeUnit.SECONDS);
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
    // run action
    setSelectionFromStartEndComments();
    runExtractAction();
    // animate wizard
    animateExtractWizard(new WizardOperation() {
      @Override
      void run(UiContext context, WizardHelper helper) {
        // not getter
        Button getterButton = helper.findGetterButton();
        assertFalse(getterButton.isEnabled());
        assertFalse(getterButton.getSelection());
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
    runUiOperations(60, TimeUnit.SECONDS);
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

  /**
   * Waits for the "Extract Method" wizard opening, runs given {@link WizardOperation} and wait for
   * wizard closing.
   */
  private void animateExtractWizard(final WizardOperation operation) {
    addOperation(new ShellOperation("Extract Method") {
      @Override
      public void run(UiContext context) throws Exception {
        WizardHelper helper = new WizardHelper(context);
        operation.run(context, helper);
      }

      @Override
      public String toString() {
        return "animateExtractWizard";
      }
    });
  }

  private void runExtractAction() throws Exception {
    // run "Extract Method" action
    final IAction action = getEditorAction(JdtActionConstants.EXTRACT_METHOD);
    addOperation(new Operation() {
      @Override
      public boolean isReady(UiContext context) throws Exception {
        return action.isEnabled();
      }

      @Override
      public void run(UiContext context) throws Exception {
        action.run();
      }

      @Override
      public String toString() {
        return "runExtractAction";
      }
    });
  }

  private void setSelectionFromStartEndComments() throws Exception {
    int start = findEnd("// start") + EOL.length();
    int end = findOffset("// end");
    selectRange(start, end);
  }
}

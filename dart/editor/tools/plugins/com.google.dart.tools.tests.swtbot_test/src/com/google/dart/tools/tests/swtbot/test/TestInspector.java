/*
 * Copyright 2014 Dart project authors.
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
package com.google.dart.tools.tests.swtbot.test;

import com.google.dart.tools.tests.swtbot.harness.EditorTestHarness;
import com.google.dart.tools.tests.swtbot.model.DebuggerBotView;
import com.google.dart.tools.tests.swtbot.model.DebuggerContextBotView;
import com.google.dart.tools.tests.swtbot.model.DebuggerStackBotView;
import com.google.dart.tools.tests.swtbot.model.EditorBotWindow;
import com.google.dart.tools.tests.swtbot.model.FilesBotView;
import com.google.dart.tools.tests.swtbot.model.InspectorBotView;
import com.google.dart.tools.tests.swtbot.model.InspectorExpressionBotView;
import com.google.dart.tools.tests.swtbot.model.InspectorObjectBotView;
import com.google.dart.tools.tests.swtbot.model.TextBotEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This is a complex test. A whole lot of machinery is required to be in good working order before
 * we can even open the object inspector.
 */
public class TestInspector extends EditorTestHarness {

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
  }

  @AfterClass
  public static void tearDownTest() {
    try {
      EditorBotWindow main = new EditorBotWindow(bot);
      main.menu("File").menu("Close All").click();
    } catch (TimeoutException ex) {
      // If we get here, we don't care about exceptions.
    }
  }

  @Test
  public void testInspector() throws Exception {
    // 1. Create sample, set breakpoint, run it, and open debugger
    TextBotEditor editor = createSample();
    editor.setBreakPointOnLine(1);
    bot.menu("Run").menu("Run").click();
    DebuggerBotView debugger = new DebuggerBotView(bot);
    DebuggerStackBotView stack = debugger.stackView();
    TableCollection selection = stack.selection();
    assertEquals("[main()]\n", selection.toString());
    DebuggerContextBotView context = debugger.contextView();
    assertEquals(2, context.treeSize());

    // 2. Select var, inspect it, evaluate a getter, and verify expression evaluation
    SWTBotTreeItem selected = context.select("t");
    selected.contextMenu("Inspect Instance...").click();
    InspectorBotView inspector = new InspectorBotView(bot);
    InspectorObjectBotView instView = inspector.instanceView();
    instView.select("Instance of string");
    InspectorExpressionBotView expr = inspector.expressionView();
    expr.focus();
    expr.type("length");
    expr.enter();
    String value = expr.content();
    String expected = "length\n  2\n";
    assertEquals(expected, value);

    // 3. Set a new breakpoint, run, and refresh debugger
    editor.setBreakPointOnLine(4);
    bot.menu("Run").menu("Resume").click();
    debugger = new DebuggerBotView(bot);
    stack = debugger.stackView();
    selection = stack.selection();
    assertEquals("[main()]\n", selection.toString());
    context = debugger.contextView();
    assertEquals(2, context.treeSize());

    // 4. Selected a different var, inspect it, evaluate a method, and verify expression evaluation
    selected = context.select("q");
    selected.contextMenu("Inspect Instance...").click();
    inspector = new InspectorBotView(bot);
    instView = inspector.instanceView();
    instView.select("Instance of string");
    expr = inspector.expressionView();
    expr.focus();
    expr.type("toString()");
    expr.enter();
    value = expr.content();
    expected = "toString()\n  \"there\"\n";
    assertEquals(expected, value);

    // 5. Clean up: close inspector, terminate execution, close debugger, and delete project
    inspector.close();
    bot.menu("Run").menu("Terminate").click();
    debugger.close();
    deleteSample();
  }

  private TextBotEditor createSample() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sample");
    main.createCommandLineProject("sample");
    TextBotEditor editor = new TextBotEditor(bot, "sample.dart");
    SWTBotEclipseEditor text = editor.editor();
    text.pressShortcut(SWT.NONE, SWT.ARROW_DOWN, (char) SWT.NONE);
    text.pressShortcut(SWT.SHIFT, SWT.ARROW_DOWN, (char) SWT.NONE);
    text.pressShortcut(SWT.SHIFT, SWT.ARROW_LEFT, (char) SWT.NONE);
    text.typeText("  ");
    text.typeText("var t = 'hi';");
    text.typeText("\n");
    text.typeText("var q = 'there';");
    text.typeText("\n");
    text.typeText("print('$t $q'");
    text.pressShortcut(SWT.NONE, SWT.ARROW_RIGHT, (char) SWT.NONE);
    text.typeText(";");
    editor.save();
    editor.waitForAnalysis();
    return editor;
  }

  private void deleteSample() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sample");
  }

}

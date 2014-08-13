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
import com.google.dart.tools.tests.swtbot.model.TextBotEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestBreakpoints extends EditorTestHarness {

  private static TextBotEditor editor;

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sample");
    main.createCommandLineProject("sample");
    editor = new TextBotEditor(bot, "sample.dart");
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
  }

  @AfterClass
  public static void tearDownTest() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sample");
//    main.menu("File").menu("Close All").click();
  }

  @Test
  public void test1() throws Exception {
    editor.setBreakPointOnLine(4);
    bot.menu("Run").menu("Run").click();
    DebuggerBotView debugger = new DebuggerBotView(bot);
    DebuggerStackBotView stack = debugger.stackView();
    TableCollection selection = stack.selection();
    assertEquals("[main()]\n", selection.toString());
    DebuggerContextBotView context = debugger.contextView();
    assertEquals(2, context.treeSize());
    debugger.stepInto();
    debugger.stepOver();
    assertEquals(3, context.treeSize());
    debugger.stepReturn();
    assertEquals(2, context.treeSize());
  }
}

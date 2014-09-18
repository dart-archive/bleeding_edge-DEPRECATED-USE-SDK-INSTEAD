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
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestBreakpoints extends EditorTestHarness {

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
  public void testDartiumBreaks() throws Exception {
    if (!Platform.getOS().equals("macosx")) {
      // Disabled on Linux and Windows.
      // The problem on Linux is the debugger isn't brought to the top of the window
      // stack when a breakpoint is triggered. If the editor is occluded SWTBot is dead.
      return;
    }
    if (!isDartiumInstalled()) {
      // The SWTBot test script run on the bots does not build the SDK.
      return;
    }
    // Test dartium breakpoints set both before and after execution starts.
    TextBotEditor editor = createSunflower();
    editor.setBreakPointOnLine(31);
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.tree().setFocus();
    editor.waitMillis(1000);
    files.select("sunflower", "web", "sunflower.dart [sunflower]");
    bot.menu("Run").menu("Run").click();
    editor.waitMillis(1000); // Launching can be really slow on the bots
    DebuggerBotView debugger = new DebuggerBotView(bot);
    DebuggerStackBotView stack = debugger.stackView();
    TableCollection selection = stack.selection();
    assertEquals("[draw()]\n", selection.toString());
    editor.setBreakPointOnLine(35);
    bot.menu("Run").menu("Resume").click();
    debugger = new DebuggerBotView(bot);
    stack = debugger.stackView();
    selection = stack.selection();
    assertEquals("[draw()]\n", selection.toString());
    DebuggerContextBotView context = debugger.contextView();
    assertEquals(2, context.treeSize());
    debugger.close();
    deleteSunflower();
  }

  @Test
  public void testVmBreaks() throws Exception {
    // Test VM breakpoints set both before and after execution starts.
    TextBotEditor editor = createSample();
    editor.setBreakPointOnLine(1);
    bot.menu("Run").menu("Run").click();
    DebuggerBotView debugger = new DebuggerBotView(bot);
    DebuggerStackBotView stack = debugger.stackView();
    TableCollection selection = stack.selection();
    assertEquals("[main()]\n", selection.toString());
    editor.setBreakPointOnLine(4);
    bot.menu("Run").menu("Resume").click();
    debugger = new DebuggerBotView(bot);
    stack = debugger.stackView();
    selection = stack.selection();
    assertEquals("[main()]\n", selection.toString());
    DebuggerContextBotView context = debugger.contextView();
    assertEquals(2, context.treeSize());
    debugger.stepInto();
    debugger.stepOver();
    assertEquals(3, context.treeSize());
    debugger.stepReturn();
    assertEquals(2, context.treeSize());
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

  private TextBotEditor createSunflower() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sunflower");
    WelcomePageEditor page = main.openWelcomePage();
    page.createSunflower();
    TextBotEditor editor = new TextBotEditor(bot, "sunflower.dart");
    return editor;
  }

  private void deleteSample() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sample");
  }

  private void deleteSunflower() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteProject("sunflower");
  }
}

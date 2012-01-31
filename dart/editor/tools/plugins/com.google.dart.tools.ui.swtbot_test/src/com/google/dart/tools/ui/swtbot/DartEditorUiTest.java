/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot;

import com.google.dart.tools.ui.swtbot.conditions.BuildLibCondition;
import com.google.dart.tools.ui.swtbot.conditions.CompilerWarmedUp;
import com.google.dart.tools.ui.swtbot.conditions.ProblemsViewCount;
import com.google.dart.tools.ui.swtbot.dialog.LaunchBrowserHelper;
import com.google.dart.tools.ui.swtbot.dialog.NewApplicationHelper;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;

import static com.google.dart.tools.ui.swtbot.DartLib.SLIDER_SAMPLE;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

@RunWith(SWTBotJunit4ClassRunner.class)
public class DartEditorUiTest {
  private static SWTWorkbenchBot bot;

  @AfterClass
  public static void printResults() {
    Performance.waitForResults(bot);
    Performance.printResults();
  }

  @AfterClass
  public static void saveAllEditors() {
    bot.saveAllEditors();
  }

  @BeforeClass
  public static void setUp() {
    bot = new SWTWorkbenchBot();
    CompilerWarmedUp.waitUntilWarmedUp(bot);
    BuildLibCondition.startListening();
  }

  @Test
  public void testDartEditorUI() throws Exception {
    SLIDER_SAMPLE.openAndLaunch(bot);

    DartLib app = new NewApplicationHelper(bot).create("NewAppTest");
    new LaunchBrowserHelper(bot).launch(app);
    Performance.waitForResults(bot);
    modifySourceInEditor(app);
    new LaunchBrowserHelper(bot).launch(app);

    new NewApplicationHelper(bot).create("NewAppTest2");
    Performance.waitForResults(bot);

    for (DartLib lib : DartLib.getAllSamples()) {
      if (lib != SLIDER_SAMPLE) {
        lib.openAndLaunch(bot);
      }
    }
    new ProblemsViewHelper(bot).assertNoProblems();
  }

  protected void modifySourceInEditor(DartLib lib) throws Exception {
    try {
      SWTBotEclipseEditor editor = lib.editor;
      editor.setFocus();
      navigateToLineContaining(editor, "Hello");
      editor.pressShortcut(Keystrokes.DOWN, Keystrokes.LEFT, Keystrokes.LF);
      long start = System.currentTimeMillis();
      editor.autoCompleteProposal("wri", "write(String message) : void - " + lib.name);
      Performance.CODE_COMPLETION.log(start, "includes time to select and insert completion");
      editor.typeText("\"Goodbye.\"");
      editor.save();
      lib.logIncrementalCompileTime("(with error in src)");
      Performance.waitForResults(bot);
      bot.waitUntil(new ProblemsViewCount(1));

      editor.setFocus();
      editor.pressShortcut(Keystrokes.RIGHT);
      editor.typeText(";");
      editor.save();
      lib.logIncrementalCompileTime();
      Performance.waitForResults(bot);
      bot.waitUntil(new ProblemsViewCount(0));

      editor.pressShortcut(Keystrokes.LF);
      editor.autoCompleteProposal("wri", "write(String message) : void - " + lib.name);
      Performance.CODE_COMPLETION.log(start, "includes time to select and insert completion");
      editor.typeText("\"Again.\"");
      editor.pressShortcut(Keystrokes.RIGHT);
      editor.typeText(";");
      editor.save();
      lib.logIncrementalCompileTime();
      Performance.waitForResults(bot);
      bot.waitUntil(new ProblemsViewCount(0));

      editor.setFocus();

    } catch (Exception e) {
      printActiveEditorText();
      throw e;
    }
  }

  private void navigateToLineContaining(SWTBotEclipseEditor editor, String text) {
    int line = 0;
    while (true) {
      if (editor.getTextOnLine(line).contains(text)) {
        break;
      }
      if (line > 50) {
        fail("Could not find line in editor containing \"" + text + "\"");
      }
      line++;
    }
    editor.navigateTo(line, 0);
  }

  private void printActiveEditorText() {
    System.out.println("====================================================");
    System.out.println(bot.activeEditor().toTextEditor().getText());
    System.out.println("====================================================");
  }
}

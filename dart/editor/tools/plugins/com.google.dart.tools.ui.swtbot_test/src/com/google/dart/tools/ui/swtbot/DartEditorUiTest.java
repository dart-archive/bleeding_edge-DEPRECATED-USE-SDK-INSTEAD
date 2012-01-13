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

import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.utilities.compiler.DartCompilerWarmup;
import com.google.dart.tools.ui.swtbot.Performance.Metric;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

@RunWith(SWTBotJunit4ClassRunner.class)
public class DartEditorUiTest {

  public class ProblemsViewCount implements ICondition {
    private final int expectedProblemsCount;
    private SWTBotTable table;

    public ProblemsViewCount(int expectedProblemsCount) {
      this.expectedProblemsCount = expectedProblemsCount;
    }

    @Override
    public String getFailureMessage() {
      return "Gave up waiting for problems: expected=" + expectedProblemsCount + " actual="
          + table.rowCount();
    }

    @Override
    public void init(SWTBot bot) {
      SWTBotView problemsView = ((SWTWorkbenchBot) bot).viewByTitle("Problems");
      Composite composite = (Composite) problemsView.getWidget();
      Table problemsTable = bot.widget(widgetOfType(Table.class), composite);
      table = new SWTBotTable(problemsTable);
    }

    @Override
    public boolean test() throws Exception {
      return table.rowCount() == expectedProblemsCount;
    }
  }

  private class App {
    String name;
    File dir;
    File dartFile;
    File jsFile;
    SWTBotEclipseEditor editor;
  }

  private SWTWorkbenchBot bot;
  private SWTBotShell workbenchShell;
  private String workbenchTitle;

  @Before
  public void setUp() {
    bot = new SWTWorkbenchBot();
    workbenchShell = bot.activeShell();
    workbenchTitle = workbenchShell.getText();
    Performance.COMPILER_WARMUP.log(bot, new ICondition() {

      @Override
      public String getFailureMessage() {
        return "Gave up waiting for compiler warmup";
      }

      @Override
      public void init(SWTBot bot) {
      }

      @Override
      public boolean test() throws Exception {
        return DartCompilerWarmup.isComplete();
      }
    });
  }

  @After
  public void tearDown() {
    while (true) {
      SWTBotShell shell;
      try {
        shell = bot.activeShell();
      } catch (WidgetNotFoundException e) {
        // ignored
        break;
      }
      if (shell == null || shell.getText().equals(workbenchTitle)) {
        break;
      }
      System.out.println("Closing " + shell);
      shell.pressShortcut(Keystrokes.ESC);
      bot.waitUntil(shellCloses(shell), 20000);
    }
    bot.saveAllEditors();
  }

  @Test
  public void testDartEditorUI() throws Exception {

    App app = createNewApp("NewAppTest");
    launchInBrowser(app);
    modifySourceInEditor(app);
    launchInBrowser(app);

    createNewApp("NewAppTest2");

    Performance.waitForResults(bot);
    Performance.printResults();
  }

  /**
   * Create a new Dart application and wait for the editor to appear
   * 
   * @param appName the name of the application to be created
   * @return a data structure containing information related to the created application
   */
  private App createNewApp(String appName) {
    App app = new App();
    app.name = appName;

    // Open wizard
    bot.menu("File").menu("New Application...").click();
    SWTBotShell shell = bot.shell("New Dart Application");
    shell.activate();

    // Assert content
    SWTBotText appNameField = bot.text();
    SWTBotText appDirField = bot.textWithLabel("Directory: ");
    assertEquals("", appNameField.getText());
    assertTrue(appDirField.getText().length() > 0);

    // Ensure that the directory to be created does not exist
    app.dir = new File(appDirField.getText(), appName);
    app.dartFile = new File(app.dir, appName + ".dart");
    if (app.dartFile.exists()) {
      System.out.println("Deleting directory " + app.dir);
      FileUtilities.delete(app.dir);
    }

    // Enter name of new app
    appNameField.typeText(appName);

    // Press Enter and wait for the operation to complete
    shell.pressShortcut(Keystrokes.LF);
    app.jsFile = new File(app.dir, appName + ".dart.app.js");
    Performance.COMPILE.logInBackground(new FileExists(app.jsFile), appName);
    EditorWithTitle matcher = new EditorWithTitle(app.dartFile.getName());
    Performance.NEW_APP.log(bot, waitForEditor(matcher), appName);
    app.editor = bot.editor(matcher).toTextEditor();
    return app;
  }

  /**
   * Click the toolbar button to launch a browser based application and wait for the operation to
   * complete.
   * 
   * @param title the name of the application being launched (not <code>null</code>)
   */
  private void launchInBrowser(App app) {
    long start = System.currentTimeMillis();
    bot.toolbarButtonWithTooltip("Run in Browser").click();
    Metric metric = Performance.LAUNCH_APP;
    try {
      bot.waitUntil(Conditions.shellIsActive("Launching browser"));
    } catch (TimeoutException e) {
      metric.log(start, app.name, "<<< progress dialog not detected");
      return;
    }
    metric.log(bot, start, shellCloses(bot.shell("Launching browser")), app.name);
  }

  private void modifySourceInEditor(App app) throws Exception {
    try {
      app.editor.setFocus();
      navigateToLineContaining(app.editor, "Hello");
      app.editor.pressShortcut(Keystrokes.DOWN, Keystrokes.LEFT, Keystrokes.LF);
      long start = System.currentTimeMillis();
      app.editor.autoCompleteProposal("wri", "write(String message) : void - " + app.name);
      Performance.CODE_COMPLETION.log(start);
      app.editor.typeText("\"Goodbye.\"");
      app.editor.save();
      Performance.COMPILE.log(bot, new ProblemsViewCount(1), app.name, "(with error in src)");

      app.editor.setFocus();
      app.editor.pressShortcut(Keystrokes.RIGHT);
      app.editor.typeText(";");
      app.editor.save();
      Performance.COMPILE.log(bot, new FileExists(app.jsFile), app.name);
      bot.waitUntil(new ProblemsViewCount(0));

      app.editor.setFocus();

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

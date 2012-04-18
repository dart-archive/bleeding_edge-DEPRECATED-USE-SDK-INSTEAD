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

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.swtbot.action.LaunchBrowserHelper;
import com.google.dart.tools.ui.swtbot.conditions.AnalysisCompleteCondition;
import com.google.dart.tools.ui.swtbot.conditions.BuildLibCondition;
import com.google.dart.tools.ui.swtbot.conditions.CompilerWarmedUp;
import com.google.dart.tools.ui.swtbot.dialog.OpenLibraryHelper;
import com.google.dart.tools.ui.swtbot.performance.Performance;
import com.google.dart.tools.ui.swtbot.views.ConsoleViewHelper;
import com.google.dart.tools.ui.swtbot.views.FilesViewHelper;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * An abstract class used by tests so that the general setup and tear-down functionality doesn't
 * have to be duplicated.
 * 
 * @see TestAll
 */
public abstract class AbstractDartEditorTest {

  protected static SWTWorkbenchBot bot;

  public static String FILE_MENU_NAME = "File";
  public static String EDIT_MENU_NAME = "Edit";
  public static String NAVIGATE_MENU_NAME = "Navigate";
  public static String TOOLS_MENU_NAME = "Tools";
  public static String HELP_MENU_NAME = "Help";

  public static String WELCOME_EDITOR_NAME = "Welcome";
  public static String CALLERS_VIEW_NAME = "Callers";
  public static String CONSOLE_VIEW_NAME = "Console";
  public static String DEBUGGER_VIEW_NAME = "Debugger";
  public static String FILES_VIEW_NAME = "Files";
  public static String OUTLINE_VIEW_NAME = "Outline";
  public static String PROBLEMS_VIEW_NAME = "Problems";

  @AfterClass
  public static void printResults() {
    Performance.waitForResults(bot);
    Performance.printResults();
  }

  @AfterClass
  public static void saveAndCloseAllEditors() {
    bot.saveAllEditors();
    bot.closeAllEditors();
  }

  @BeforeClass
  public static void setUp() throws Exception {
    // TODO (danrubel) hook launching LogTimer for launching performance measurements
    bot = new SWTWorkbenchBot();
    CompilerWarmedUp.waitUntilWarmedUp(bot);
    if (DartCoreDebug.ANALYSIS_SERVER) {
      AnalysisCompleteCondition.startListening();
      AnalysisCompleteCondition.waitUntilWarmedUp(bot);
    } else {
      BuildLibCondition.startListening();
    }
    // Copy samples from DART_TRUNK/samples into ~/Downloads/dart/samples/
    DartLib.buildSamples();
    // Make assertions on the samples
    DartLib.getAllSamples();
  }

  @After
  public void assertNoLibrariesOpen() {
    // After each test, assert that there is only one element (SDK Libraries) in the Files view,
    // this guarantees that any previous examples have been closed out.
    new FilesViewHelper(bot).assertTreeItemsEqual(FilesViewHelper.SDK_TEXT);
  }

  /**
   * Launch and open the passed sample. The passed boolean is used to either assert that there
   * should be nothing in the console (<code>true</code>), or that no assertion should be made on
   * the console output (<code>false</code>).
   * 
   * @param dartLibSample the {@link DartLib} to be tested
   * @param assertNoConsoleOutput if <code>true</code> an assertion is made that the console log is
   *          empty after the sample is launched
   */
  protected void openAndLaunchLibrary(DartLib dartLibSample, boolean isWebApp,
      boolean assertNoConsoleOutput) {
    if (assertNoConsoleOutput) {
      openAndLaunchLibrary(dartLibSample, isWebApp, "");
    } else {
      openAndLaunchLibrary(dartLibSample, isWebApp, null);
    }
  }

  /**
   * Launch and open the passed sample. The passed String has two states, if the passed String is
   * <code>null</code>, then no assertion is made, if the String is not <code>null</code> then an
   * assertion is made that the console output will match the String.
   * 
   * @param dartLibSample the {@link DartLib} to be tested
   * @param assertNoConsoleOutput if non-<code>null</code>, then assert that the console output
   *          matches this String
   */
  private void openAndLaunchLibrary(DartLib dartLibSample, boolean isWebApp, String consoleOutput) {
    assertNotNull(dartLibSample);
    dartLibSample.deleteJsFile();
    try {
      new OpenLibraryHelper(bot).open(dartLibSample);

      // Test the JS Generation of this sample
      // TODO (jwren) Rewrite this once we have the new Console View
      //      if (isWebApp) {
      //        SWTBotMenu menu = bot.menu("Tools").menu("Generate JavaScript");
      //        menu.isEnabled();
      //        menu.isActive();
      //        menu.click();
      //        bot.waitUntil(new ConsoleViewOutput("Generating JavaScript...\\p{Space}Wrote .*"), 10000);
      //      }

      if (!isWebApp) {
        // TODO (jwren) implement a different version of LaunchBrowerHelper for server apps, so that console output can be asserted against
        Performance.waitForResults(bot);
      } else {
        new LaunchBrowserHelper(bot).launch(dartLibSample);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to open and launch " + dartLibSample.name + ", " + e.getMessage());
    }

    Performance.waitForResults(bot);

    // problem assertions
    new ProblemsViewHelper(bot).assertNoProblems();

    // console assertions
    if (consoleOutput != null) {
      if (consoleOutput.length() == 0) {
        new ConsoleViewHelper(bot).assertNoConsoleLog();
      } else {
        new ConsoleViewHelper(bot).assertConsoleEquals(consoleOutput);
      }
    }

    // Files view assertions
    new FilesViewHelper(bot).assertTreeItemsEqual(
        dartLibSample.getNameInFilesView(),
        FilesViewHelper.SDK_TEXT);

    // Finally, close the library
    dartLibSample.close(bot);
  }

}

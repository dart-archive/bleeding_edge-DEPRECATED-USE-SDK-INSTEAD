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
import com.google.dart.tools.core.samples.SamplesTest;
import com.google.dart.tools.ui.swtbot.app.NewSimpleApp;
import com.google.dart.tools.ui.swtbot.conditions.AnalysisCompleteCondition;
import com.google.dart.tools.ui.swtbot.conditions.BuildLibCondition;
import com.google.dart.tools.ui.swtbot.conditions.CompilerWarmedUp;
import com.google.dart.tools.ui.swtbot.dialog.NewApplicationHelper;
import com.google.dart.tools.ui.swtbot.dialog.PreferencesHelper;
import com.google.dart.tools.ui.swtbot.performance.Performance;
import com.google.dart.tools.ui.swtbot.views.ConsoleViewHelper;
import com.google.dart.tools.ui.swtbot.views.FilesViewHelper;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SWTBotJunit4ClassRunner.class)
public class DartEditorUiTest {
  public static SWTWorkbenchBot bot;

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

  public static Shell getShell() {
    return getWorkbenchWindow().getShell();
  }

  public static IWorkbenchWindow getWorkbenchWindow() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      window = PlatformUI.getWorkbench().getWorkbenchWindows()[0];
    }
    return window;
  }

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
    // Before any test starts, assert that there is only one element (SDK Libraries) in the Files view,
    // this guarantees that any previous examples have been closed out.
    new FilesViewHelper(bot).assertTreeItemsEqual(FilesViewHelper.SDK_TEXT);
  }

  @Ignore("Not yet implemented")
  @Test
  public void testInitState_aboutDialog() throws Exception {
    // TODO (jwren) once implemented, this test should assert that:
    // the dialog appears
    // correct widgets are visible and discoverable via SWTBot
    // user can exit the dialog
  }

  @Test
  public void testInitState_editor_welcome() throws Exception {
    SWTBotEditor editor = bot.editorByTitle(WELCOME_EDITOR_NAME);
    assertNotNull(editor);
    IEditorReference editorRef = editor.getReference();
    assertNotNull(editorRef);
    assertFalse(editorRef.isPinned());
    assertFalse(editorRef.isDirty());
    assertEquals(WELCOME_EDITOR_NAME, editorRef.getTitle());
    assertNotNull(editorRef.getTitleImage());
  }

  @Test
  public void testInitState_perspective() throws Exception {
    SWTBotPerspective perspective = bot.activePerspective();
    assertNotNull(perspective);
    assertEquals("Dart", perspective.getLabel());
  }

  @Test
  public void testInitState_preferencesDialog() throws Exception {
    PreferencesHelper prefrencesHelper = new PreferencesHelper(bot);
    prefrencesHelper.open();
    prefrencesHelper.assertDefaultPreferencesSelected();
    prefrencesHelper.close();
  }

  @Test
  public void testInitState_view_callers() throws Exception {
    bot.menu(TOOLS_MENU_NAME).menu(CALLERS_VIEW_NAME).click();
    SWTBotView view = baseViewAssertions(CALLERS_VIEW_NAME);
    view.close();
  }

  @Test
  public void testInitState_view_console() throws Exception {
    baseViewAssertions(CONSOLE_VIEW_NAME);
  }

  @Test
  public void testInitState_view_debugger() throws Exception {
    bot.menu("Tools").menu(DEBUGGER_VIEW_NAME).click();
    SWTBotView view = baseViewAssertions(DEBUGGER_VIEW_NAME);
    view.close();
  }

  @Test
  public void testInitState_view_files() throws Exception {
    baseViewAssertions(FILES_VIEW_NAME);
    FilesViewHelper filesViewHelper = new FilesViewHelper(bot);
    filesViewHelper.assertTreeItemCount(1);
    filesViewHelper.assertTreeItemsEqual(FilesViewHelper.SDK_TEXT);
    SWTBotTreeItem sdkTreeItem = filesViewHelper.getItems()[0];
    assertFalse(sdkTreeItem.isExpanded());
    assertTrue(sdkTreeItem.isVisible());
  }

  @Test
  public void testInitState_view_outline() throws Exception {
    bot.menu("Tools").menu(OUTLINE_VIEW_NAME).click();
    SWTBotView view = baseViewAssertions(OUTLINE_VIEW_NAME);
    view.close();
  }

  @Test
  public void testInitState_view_problems() throws Exception {
    baseViewAssertions(PROBLEMS_VIEW_NAME);
    ProblemsViewHelper helper = new ProblemsViewHelper(bot);
    helper.assertNoProblems();
  }

  @Test
  public void testNewApplicationWizard_server() throws Exception {
    DartLib dartLib = new NewApplicationHelper(bot).create("NewAppServer",
        NewApplicationHelper.ContentType.SERVER);
    Performance.waitForResults(bot);
    // TODO (jwren) once we can launch server apps (see todo in DartLib.openAndLaunch), then this
    // call should be: openAndLaunchLibrary(dartLib, false, "Hello World");
    openAndLaunchLibrary(dartLib, false, false);
  }

  @Test
  public void testNewApplicationWizard_web() throws Exception {
    DartLib dartLib = new NewApplicationHelper(bot).create("NewAppWeb",
        NewApplicationHelper.ContentType.WEB);
    Performance.waitForResults(bot);
    openAndLaunchLibrary(dartLib, true, true);
  }

  @Test
  public void testSample_clock() throws Exception {
    openAndLaunchLibrary(DartLib.CLOCK_SAMPLE, true, true);
  }

  @Test
  public void testSample_slider() throws Exception {
    openAndLaunchLibrary(DartLib.SLIDER_SAMPLE, true, true);
  }

  @Test
  public void testSample_sunflower() throws Exception {
    openAndLaunchLibrary(DartLib.SUNFLOWER_SAMPLE, true, true);
  }

  @Test
  public void testSample_timeServer() throws Exception {
    openAndLaunchLibrary(DartLib.TIME_SERVER_SAMPLE, false, true);
  }

  @Test
  public void testSample_total() throws Exception {
    openAndLaunchLibrary(DartLib.TOTAL_SAMPLE, true, false);
  }

  @Test
  public void testSamples_compileAllSamples() throws Exception {
    new SamplesTest(new SamplesTest.Listener() {
      @Override
      public void logParse(long elapseTime, String... comments) {
        long start = System.currentTimeMillis() - elapseTime;
        Performance.COMPILER_PARSE.log(start, comments);
      }
    }).testSamples(DartLib.getSamplesDir());
  }

  //Not working on Linux yet
  @Test
  public void testSimpleApp() throws Exception {
    NewSimpleApp newSimpleApp = new NewSimpleApp(bot);
    newSimpleApp.create();
    Performance.waitForResults(bot);
    new ProblemsViewHelper(bot).assertNoProblems();
    newSimpleApp.app.close(bot);
  }

  /**
   * A utility method which make a set of base-assertions on the view with the passed title.
   * 
   * @param viewName the name as it appears in the Editor
   * @return the {@link SWTBotView}, handy for make more tests after this method is called
   */
  private SWTBotView baseViewAssertions(String viewName) {
    SWTBotView view = bot.viewByTitle(viewName);
    assertNotNull(view);
    IViewReference viewRef = view.getReference();
    assertNotNull(viewRef);
    assertFalse(viewRef.isFastView());
    assertFalse(viewRef.isDirty());
    assertFalse(viewRef.getTitle().isEmpty());
    assertNotNull(viewRef.getTitleImage());
    return view;
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
  private void openAndLaunchLibrary(DartLib dartLibSample, boolean isWebApp,
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
    try {
      dartLibSample.openAndLaunch(bot, isWebApp);
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
    new FilesViewHelper(bot).assertTreeItemsEqual(dartLibSample.getNameInFilesView(),
        FilesViewHelper.SDK_TEXT);

    // Finally, close the library
    dartLibSample.close(bot);
  }

}

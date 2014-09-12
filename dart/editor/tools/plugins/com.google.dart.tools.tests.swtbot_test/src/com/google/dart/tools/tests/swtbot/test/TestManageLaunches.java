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
import com.google.dart.tools.tests.swtbot.model.EditorBotWindow;
import com.google.dart.tools.tests.swtbot.model.FilesBotView;
import com.google.dart.tools.tests.swtbot.model.LaunchBrowserBotView;
import com.google.dart.tools.tests.swtbot.model.LaunchChromeBotView;
import com.google.dart.tools.tests.swtbot.model.LaunchDartBotView;
import com.google.dart.tools.tests.swtbot.model.LaunchDartiumBotView;
import com.google.dart.tools.tests.swtbot.model.LaunchMobileBotView;
import com.google.dart.tools.tests.swtbot.model.ManageLaunchesBotView;
import com.google.dart.tools.tests.swtbot.model.ProblemsBotView;
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestManageLaunches extends EditorTestHarness {

  private static EditorBotWindow editor;
  private static FilesBotView files;

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
    editor = new EditorBotWindow(bot);
    files = editor.filesView();
    files.deleteExistingProject("Sunflower");
    assertTrue(files.isEmpty());
  }

  @AfterClass
  public static void tearDownTest() {
    try {
      editor.menu("File").menu("Close All").click();
    } catch (Exception ex) {
      // If we get here, we don't care about exceptions.
    }
  }

  @Test
  public void testBrowserBasedLaunches() throws Exception {
    WelcomePageEditor page = editor.openWelcomePage();
    page.createSunflower();
    ProblemsBotView problems = editor.problemsView();
    assertTrue(problems.isEmpty()); // ensure timing is correct
    editor.menu("Run").menu("Manage Launches").click();
    ManageLaunchesBotView launches = new ManageLaunchesBotView(bot);

    LaunchDartiumBotView launchDartium = launches.createDartiumLaunch();
    launchDartium.url("file://sunflower/web/sunflower.html", "sunflower/web");
    launchDartium.htmlFile("sunflower/web/sunflower.html");
    launchDartium.checkedMode(false);
    launchDartium.experimentalFeatures(false);
    launchDartium.showOutput(true);
    launchDartium.usePubServe(false);
    launches.apply();

    LaunchBrowserBotView launchBrowser = launches.createBrowserLaunch();
    launchBrowser.url("file://sunflower/web/sunflower.html", "sunflower/web");
    launchBrowser.htmlFile("sunflower/web/sunflower.html");
    launches.apply();
    int index = launches.launchSelection();

    LaunchMobileBotView launchMobile = launches.createMobileLaunch();
    launchMobile.url("file://sunflower/web/sunflower.html", "sunflower/web");
    launchMobile.htmlFile("sunflower/web/sunflower.html");
    launchMobile.useUSB();
    launchMobile.useWifi();
    // Changes cannot be saved because there is no debug connection.

    launches.close();
    editor.menu("Run").menu("Manage Launches").click();
    launches = new ManageLaunchesBotView(bot);
    launches.selectLaunch(index);
    launchBrowser = launches.selectedBrowserLaunch();
    assertTrue(launchBrowser.isHtml());
    assertEquals("sunflower/web/sunflower.html", launchBrowser.htmlFile());

    launches.close();
    files.deleteProject("sunflower");
  }

  @Test
  public void testChromeAppLaunch() throws Exception {
    createChromeAppSample();
    editor.menu("Run").menu("Manage Launches").click();
    ManageLaunchesBotView launches = new ManageLaunchesBotView(bot);
    LaunchChromeBotView launchChrome = launches.createChromeLaunch();
    launchChrome.target("sample/web/manifest.json");
    launchChrome.checkedMode(false);
    int index = launches.launchSelection();
    launches.apply();
    launches.close();

    editor.menu("Run").menu("Manage Launches").click();
    launches = new ManageLaunchesBotView(bot);
    launches.selectLaunch(index);
    launchChrome = launches.selectedChromeLaunch();
    assertEquals("sample/web/manifest.json", launchChrome.target());
    assertFalse(launchChrome.isCheckedMode());

    launches.close();
    deleteSample();
  }

  @Test
  public void testCommandLineLaunch() throws Exception {
    createCommandLineSample();
    editor.menu("Run").menu("Manage Launches").click();
    ManageLaunchesBotView launches = new ManageLaunchesBotView(bot);
    LaunchDartBotView launchDart = launches.createDartLaunch();
    launchDart.script("sample/bin/sample.dart"); // field is not editable
    launchDart.workingDirectory("/tmp");
    launchDart.checkedMode(false);
    launchDart.pauseIsolateOnExit(true);
    launchDart.pauseIsolateOnStart(true);
    int index = launches.launchSelection();
    launches.apply();
    launches.close();

    editor.menu("Run").menu("Manage Launches").click();
    launches = new ManageLaunchesBotView(bot);
    launches.selectLaunch(index);
    launchDart = launches.selectedDartLaunch();
//    assertEquals("sample/bin/sample.dart", launchDart.script()); // value is not saved
    assertEquals("/tmp", launchDart.workingDirectory());
    assertFalse(launchDart.isCheckedMode());
    assertTrue(launchDart.isPauseIsolateOnExit());
    assertTrue(launchDart.isPauseIsolateOnStart());

    launches.close();
    deleteSample();
  }

  private void createChromeAppSample() {
    deleteSample();
    editor.createChromeProject("sample");
    ProblemsBotView problems = editor.problemsView();
    assertTrue(problems.isEmpty()); // ensure timing is correct
  }

  private void createCommandLineSample() {
    deleteSample();
    editor.createCommandLineProject("sample");
    ProblemsBotView problems = editor.problemsView();
    assertTrue(problems.isEmpty()); // ensure timing is correct
  }

  private void deleteSample() {
    files.deleteExistingProject("sample");
  }

}

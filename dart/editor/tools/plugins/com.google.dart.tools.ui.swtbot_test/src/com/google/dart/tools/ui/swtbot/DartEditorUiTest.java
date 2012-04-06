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
import com.google.dart.tools.ui.swtbot.performance.Performance;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class DartEditorUiTest {
  public static SWTWorkbenchBot bot;

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
    DartLib.buildSamples();
  }

  @Test
  public void testNewApp() throws Exception {
    new NewApplicationHelper(bot).create("NewAppTest");
    Performance.waitForResults(bot);
    new ProblemsViewHelper(bot).assertNoProblems();
  }

  @Test
  public void testOpenSamples() throws Exception {
    for (DartLib lib : DartLib.getAllSamples()) {
      lib.openAndLaunch(bot);
    }
    new ProblemsViewHelper(bot).assertNoProblems();
  }

  @Test
  public void testSamplesStructure() throws Exception {
    new SamplesTest(new SamplesTest.Listener() {
      @Override
      public void logParse(long elapseTime, String... comments) {
        long start = System.currentTimeMillis() - elapseTime;
        Performance.COMPILER_PARSE.log(start, comments);
      }
    }).testSamples(DartLib.getSamplesDir());
  }

  @Test
  public void testSimpleApp() throws Exception {
    new NewSimpleApp(bot).create();
    Performance.waitForResults(bot);
    new ProblemsViewHelper(bot).assertNoProblems();
  }
}

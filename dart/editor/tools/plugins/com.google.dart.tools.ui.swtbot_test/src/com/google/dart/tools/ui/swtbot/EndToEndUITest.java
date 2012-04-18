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

import com.google.dart.tools.ui.swtbot.app.NewSimpleApp;
import com.google.dart.tools.ui.swtbot.performance.Performance;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;

import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * End To End UI tests for the Dart Editor.
 * 
 * @see TestAll
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public final class EndToEndUITest extends AbstractDartEditorTest {

  @Ignore("Currently with the analysis engine turned on, danrubel investigating.")
  //Not working on Linux yet
  @Test
  public void testEndToEnd_simpleApp() throws Exception {
    NewSimpleApp newSimpleApp = new NewSimpleApp(bot);
    newSimpleApp.create();
    Performance.waitForResults(bot);
    new ProblemsViewHelper(bot).assertNoProblems();
    newSimpleApp.app.close(bot);
  }

}

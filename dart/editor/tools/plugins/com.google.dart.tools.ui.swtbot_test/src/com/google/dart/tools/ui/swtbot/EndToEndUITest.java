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

import com.google.dart.tools.ui.swtbot.endtoend.AbstractEndToEndTest;
import com.google.dart.tools.ui.swtbot.endtoend.EndToEnd001;
import com.google.dart.tools.ui.swtbot.endtoend.EndToEnd002;
import com.google.dart.tools.ui.swtbot.performance.Performance;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;

import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

/**
 * End To End UI tests for the Dart Editor.
 * 
 * @see TestAll
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public final class EndToEndUITest extends AbstractDartEditorTest {

  @Test
  public void testEndToEnd_001() throws Exception {
    EndToEnd001 endToEnd001 = new EndToEnd001(bot);
    runEndToEndTest(endToEnd001);
  }

  // Not working on Linux yet- code completion testing bug.
  @Test
  public void testEndToEnd_002() throws Exception {
    EndToEnd002 endToEnd002 = new EndToEnd002(bot);
    runEndToEndTest(endToEnd002);
  }

  private void runEndToEndTest(AbstractEndToEndTest endToEndTest) throws Exception {
    assertNotNull(endToEndTest);
    endToEndTest.runTest();
    Performance.waitForResults(bot);
    new ProblemsViewHelper(bot).assertNoProblems();
    endToEndTest.afterTest();
  }

}

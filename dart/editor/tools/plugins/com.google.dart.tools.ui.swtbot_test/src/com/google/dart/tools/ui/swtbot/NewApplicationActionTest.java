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

import com.google.dart.tools.ui.swtbot.dialog.NewApplicationHelper;
import com.google.dart.tools.ui.swtbot.dialog.NewApplicationHelper.ContentType;
import com.google.dart.tools.ui.swtbot.performance.SwtBotPerformance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * New Application tests for the Dart Editor.
 * 
 * @see TestAll
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public final class NewApplicationActionTest extends AbstractDartEditorTest {

  private DartLib lib;

  @After
  public void cleanup() throws CoreException {
    if (lib != null) {
      lib.getProject().delete(true, null);
    }
  }

  @Test
  public void testNewApplicationWizard_server() throws Exception {
    createApp("NewAppServer", ContentType.SERVER);
    //TODO (pquitslund): verify content and launch
  }

  @Test
  public void testNewApplicationWizard_web() throws Exception {
    createApp("NewAppWeb", ContentType.WEB);
    //TODO (pquitslund): verify content and launch
  }

  private void createApp(String name, ContentType type) throws CoreException {
    lib = new NewApplicationHelper(bot).create(name, type);
    SwtBotPerformance.waitForResults(bot);
  }

}

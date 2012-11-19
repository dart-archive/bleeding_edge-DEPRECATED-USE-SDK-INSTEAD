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
import com.google.dart.tools.ui.swtbot.performance.SwtBotPerformance;

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

  private DartLib serverLib;
  private DartLib webLib;

  @After
  public void cleanup() {
    if (serverLib != null) {
      serverLib.deleteDir();
    }
    if (webLib != null) {
      webLib.deleteDir();
    }
  }

  @Test
  public void testNewApplicationWizard_server() throws Exception {
    serverLib = new NewApplicationHelper(bot).create(
        "NewAppServer",
        NewApplicationHelper.ContentType.SERVER);
    SwtBotPerformance.waitForResults(bot);
    // TODO (jwren) once we can launch server apps (see todo in DartLib.openAndLaunch), then this
    // call should be: openAndLaunchLibrary(dartLib, false, "Hello World");
    openAndLaunchLibrary(serverLib, false, false);
  }

  @Test
  public void testNewApplicationWizard_web() throws Exception {
    webLib = new NewApplicationHelper(bot).create("NewAppWeb", NewApplicationHelper.ContentType.WEB);
    SwtBotPerformance.waitForResults(bot);
    openAndLaunchLibrary(webLib, true, true);
  }

}

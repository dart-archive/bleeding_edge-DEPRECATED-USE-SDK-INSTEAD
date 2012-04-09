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
package com.google.dart.tools.ui.swtbot.app;

import com.google.dart.tools.ui.swtbot.DartEditorHelper;
import com.google.dart.tools.ui.swtbot.DartEditorUiTest;
import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.action.LaunchBrowserHelper;
import com.google.dart.tools.ui.swtbot.conditions.ProblemsViewCount;
import com.google.dart.tools.ui.swtbot.dialog.NewApplicationHelper;
import com.google.dart.tools.ui.swtbot.performance.Performance;

import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.printActiveEditorText;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Helper for creating a simple application named "NewTestApp"
 */
public class NewSimpleApp {

  private final SWTWorkbenchBot bot;
  public DartLib app;

  public NewSimpleApp(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  public void create() throws Exception {
    app = new NewApplicationHelper(bot).create("SimpleApp", NewApplicationHelper.ContentType.WEB);
    new LaunchBrowserHelper(DartEditorUiTest.bot).launch(app);
    Performance.waitForResults(DartEditorUiTest.bot);
    try {
      app.editor.setFocus();
      modifySource(new DartEditorHelper(bot, app));
    } catch (Exception e) {
      printActiveEditorText(bot);
      throw e;
    }
    new LaunchBrowserHelper(DartEditorUiTest.bot).launch(app);
  }

  public void modifySource(DartEditorHelper helper) {
    helper.moveToEndOfLineContaining("Hello");

    helper.typeLine("wri!te(\"Hello Again!!\")");
    helper.save("error in src");
    Performance.waitForResults(bot);
    bot.waitUntil(new ProblemsViewCount(1));

    helper.editor().typeText(";");
    helper.save();
    Performance.waitForResults(bot);
    bot.waitUntil(new ProblemsViewCount(0));

    helper.typeLine("wr!ite(\"Goodbye.\");");
    helper.save();
  }

}

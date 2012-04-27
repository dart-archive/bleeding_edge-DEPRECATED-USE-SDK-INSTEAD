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
package com.google.dart.tools.ui.swtbot.endtoend;

import com.google.dart.tools.ui.swtbot.DartEditorHelper;
import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.EndToEndUITest;
import com.google.dart.tools.ui.swtbot.action.LaunchBrowserHelper;
import com.google.dart.tools.ui.swtbot.conditions.ProblemsViewCount;
import com.google.dart.tools.ui.swtbot.dialog.NewApplicationHelper;
import com.google.dart.tools.ui.swtbot.performance.Performance;

import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.printActiveEditorText;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Helper class for {@link EndToEndUITest}, this test creates a new web application using
 * {@link NewApplicationHelper}, adds some text introducing an error (missing semicolon), fixes the
 * error, saves and exits.
 */
public class EndToEnd002 extends AbstractEndToEndTest {

  private DartLib app;

  public EndToEnd002(SWTWorkbenchBot bot) {
    super(bot);
  }

  @Override
  public void afterTest() {
    app.close(bot);
  }

  @Override
  public void runTest() throws Exception {
    app = new NewApplicationHelper(bot).create("EndToEnd002", NewApplicationHelper.ContentType.WEB);
    new LaunchBrowserHelper(bot).launch(app);
    Performance.waitForResults(bot);
    try {
      app.editor.setFocus();
      modifySource(new DartEditorHelper(bot, app));
    } catch (Exception e) {
      printActiveEditorText(bot);
      throw e;
    }
    new LaunchBrowserHelper(bot).launch(app);
  }

  private void modifySource(DartEditorHelper helper) {
    helper.moveToEndOfLineContaining("Hello");

    helper.typeLine("wri!te(\"Hello Again\")");
    helper.save("error in src");
    Performance.waitForResults(bot);
    bot.waitUntil(new ProblemsViewCount(1));

    helper.moveToEndOfLineContaining("write(\"Hello Again\")");
    helper.editor().typeText(";");
    helper.save();
    Performance.waitForResults(bot);
    bot.waitUntil(new ProblemsViewCount(0));

    helper.typeLine("write(\"Goodbye.");
    helper.moveToEndOfLineContaining("write(\"Goodbye.\")");
    helper.editor().typeText(";");
    Performance.waitForResults(bot);
    bot.waitUntil(new ProblemsViewCount(0));

    helper.save();
  }

}

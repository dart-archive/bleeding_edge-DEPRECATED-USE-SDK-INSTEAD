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
package com.google.dart.tools.tests.swtbot.model;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;

public class EditorBotWindow extends AbstractBotView {

  public EditorBotWindow(SWTWorkbenchBot bot) {
    super(bot);
  }

  public TextBotEditor editorNamed(String title) {
    waitForAnalysis();
    return new TextBotEditor(bot, title);
  }

  public FilesBotView filesView() {
    waitForAnalysis();
    return new FilesBotView(bot);
  }

  public SWTBotMenu menu(final String title) {
    final SWTBotMenu[] menu = new SWTBotMenu[1];
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        menu[0] = bot.menu(title);
      }
    });
    return menu[0];
  }

  public WelcomePageEditor openWelcomePage() {
    waitForAnalysis();
    // TODO re-open welcome page if needed, and bring to top
    return new WelcomePageEditor(bot);
  }

  public OutlineBotView outlineView() {
    waitForAnalysis();
    return new OutlineBotView(bot);
  }

  public ProblemsBotView problemsView() {
    waitForAnalysis();
    return new ProblemsBotView(bot);
  }

  @Override
  protected String viewName() {
    return "Dart Editor";
  }
}

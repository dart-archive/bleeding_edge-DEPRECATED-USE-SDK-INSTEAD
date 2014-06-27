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

  /**
   * Return a TextBotEditor for the editor with the given <code>title</code>.
   * 
   * @param title the editor title
   * @return the SWTBot model of the editor
   */
  public TextBotEditor editorNamed(String title) {
    waitForAnalysis();
    return new TextBotEditor(bot, title);
  }

  /**
   * Return a FilesBotView for the Files view.
   * 
   * @return the SWTBot model of the Files view
   */
  public FilesBotView filesView() {
    waitForAnalysis();
    return new FilesBotView(bot);
  }

  /**
   * Return a SWTBotMenu with the given <code>title</code>
   * 
   * @param title the menu title
   * @return the SWTBotMenu that models the menu
   */
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

  /**
   * Return the WelcomePageEditor for the Welcome page.
   * 
   * @return the SWTBot model for the Welcome page
   */
  public WelcomePageEditor openWelcomePage() {
    waitForAnalysis();
    // TODO re-open welcome page if needed, and bring to top
    return new WelcomePageEditor(bot);
  }

  /**
   * Return the OutlineBotView for the Outline view.
   * 
   * @return the SWTBot model for the Outline view
   */
  public OutlineBotView outlineView() {
    waitForAnalysis();
    return new OutlineBotView(bot);
  }

  /**
   * Return the ProblemsBotView for the Problems view.
   * 
   * @return the SWTBot model for the Problems view
   */
  public ProblemsBotView problemsView() {
    waitForAnalysis();
    return new ProblemsBotView(bot);
  }

  /**
   * Return a SearchBotView for the Search view.
   * 
   * @return the SWTBot model of the Search view
   */
  public SearchBotView searchView() {
    waitForAnalysis();
    return new SearchBotView(bot);
  }

  @Override
  protected String viewName() {
    return "Dart Editor";
  }
}

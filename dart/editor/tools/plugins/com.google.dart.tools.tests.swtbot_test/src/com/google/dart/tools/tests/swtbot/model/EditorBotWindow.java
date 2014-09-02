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

import com.google.dart.tools.tests.swtbot.matchers.EditorWithTitle;
import com.google.dart.tools.ui.internal.projects.ProjectMessages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

@SuppressWarnings("restriction")
public class EditorBotWindow extends AbstractBotView {

  // Project types -- need to keep in sync with New Project wizard (NewApplicationCreationPage)
  private enum Project {
    WEB_APP, CMDLINE_APP, PKG_TEMPLATE, POLYMER_APP, CHROME_APP
  }

  public EditorBotWindow(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * Create a new chrome app with the given name.
   * 
   * @param string the project name
   */
  public void createChromeProject(String string) {
    createProject(string, Project.CHROME_APP, "manifest.json");
  }

  /**
   * Create a new command-line app with the given name.
   * 
   * @param string the project name
   */
  public void createCommandLineProject(String string) {
    createProject(string, Project.CMDLINE_APP, string + ".dart");
  }

  /**
   * Create a new package template with the given name.
   * 
   * @param string the project name
   */
  public void createPackageTemplateProject(String string) {
    // TODO Test this.
    createProject(string, Project.PKG_TEMPLATE, string + ".dart");
  }

  /**
   * Create a new polymer app with the given name.
   * 
   * @param string the project name
   */
  public void createPolymerProject(String string) {
    // TODO Test this.
    createProject(string, Project.POLYMER_APP, string + ".html");
  }

  /**
   * Create a new web app with the given name.
   * 
   * @param string the project name
   */
  public void createWebProject(String string) {
    createProject(string, Project.WEB_APP, string + ".dart");
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
    menu("Tools").menu("Files").click();
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
    menu("Tools").menu("Welcome Page").click();
    waitForAnalysis();
    return new WelcomePageEditor(bot);
  }

  /**
   * Return the OutlineBotView for the Outline view.
   * 
   * @return the SWTBot model for the Outline view
   */
  public OutlineBotView outlineView() {
    waitForAnalysis();
    menu("Tools").menu("Outline").click();
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
    menu("Tools").menu("Problems").click();
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

  private void createProject(String string, Project type, String editorName) {
    waitForAnalysis();
    menu("File").menu("New Project...").click();
    SWTBotText textField = bot.textWithTooltip(ProjectMessages.NewApplicationWizardPage_project_name_tooltip);
    textField.setText("sample");
    SWTBotTable types = bot.tableInGroup("Sample content");
    types.select(type.ordinal());
    bot.button(IDialogConstants.FINISH_LABEL).click();
    bot.waitUntilWidgetAppears(Conditions.waitForEditor(new EditorWithTitle(editorName)));
    filesView().waitForToolsOutput();
    bot.editorByTitle(editorName).setFocus();
  }
}

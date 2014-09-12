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
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.IntResult;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;

public class ManageLaunchesBotView extends AbstractBotView {

  public ManageLaunchesBotView(SWTWorkbenchBot bot) {
    super(bot);
    bot.waitUntilWidgetAppears(Conditions.shellIsActive(viewName()));
  }

  /**
   * Save the changes.
   */
  public void apply() {
    manageLaunchesShell().bot().button("Apply").click();
    waitForAnalysis();
  }

  /**
   * Close the dialog.
   */
  public void close() {
    manageLaunchesShell().bot().button("Close").click();
    waitForAnalysis();
  }

  public LaunchBrowserBotView createBrowserLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    SWTBotToolbarButton button = shellBot.toolbarButtonWithTooltip("Create a new Browser launch");
    button.click();
    return new LaunchBrowserBotView(bot, shellBot);
  }

  public LaunchChromeBotView createChromeLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    SWTBotToolbarButton button = shellBot.toolbarButtonWithTooltip("Create a new Chrome app launch");
    button.click();
    return new LaunchChromeBotView(bot, shellBot);
  }

  public LaunchDartiumBotView createDartiumLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    SWTBotToolbarButton button = shellBot.toolbarButtonWithTooltip("Create a new Dartium launch");
    button.click();
    return new LaunchDartiumBotView(bot, shellBot);
  }

  public LaunchDartBotView createDartLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    SWTBotToolbarButton button = shellBot.toolbarButtonWithTooltip("Create a new Dart command-line launch");
    button.click();
    return new LaunchDartBotView(bot, shellBot);
  }

  public LaunchMobileBotView createMobileLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    SWTBotToolbarButton button = shellBot.toolbarButtonWithTooltip("Create a new Mobile launch");
    button.click();
    return new LaunchMobileBotView(bot, shellBot);
  }

  public int launchSelection() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    final SWTBotTable table = shellBot.table();
    return UIThreadRunnable.syncExec(new IntResult() {
      @Override
      public Integer run() {
        return table.widget.getSelectionIndex();
      }
    });
  }

  public LaunchBrowserBotView selectedBrowserLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    return new LaunchBrowserBotView(bot, shellBot);
  }

  public LaunchChromeBotView selectedChromeLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    return new LaunchChromeBotView(bot, shellBot);
  }

  public LaunchDartBotView selectedDartLaunch() {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    return new LaunchDartBotView(bot, shellBot);
  }

  public void selectLaunch(final int index) {
    SWTBotShell botShell = manageLaunchesShell();
    SWTBot shellBot = botShell.bot();
    final SWTBotTable table = shellBot.table();
    table.select(index);
  }

  @Override
  protected String viewName() {
    return "Manage Launches";
  }

  private SWTBotShell manageLaunchesShell() {
    return bot.shell(viewName());
  }

}

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
package com.google.dart.tools.ui.swtbot.dialog;

import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.Performance;
import com.google.dart.tools.ui.swtbot.Performance.Metric;

import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.toolbarDropDownButton;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;

/**
 * Helper for launching a Dart application in a browser
 */
public class LaunchBrowserHelper {

  private final SWTWorkbenchBot bot;

  public LaunchBrowserHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  /**
   * Click the toolbar button to launch a browser based application and wait for the operation to
   * complete.
   * 
   * @param title the name of the application being launched (not <code>null</code>)
   */
  public void launch(DartLib lib) {
    Performance.waitForResults(bot);
    SWTBotToolbarDropDownButton launchButton = toolbarDropDownButton(bot, "Run.*");
    long start = System.currentTimeMillis();
    launchButton.click();
    Metric metric = Performance.LAUNCH_APP;
    // TODO (danrubel): hook into launch as lister rather than relying on progress dialog
    try {
      bot.waitUntil(shellIsActive("Launching browser"));
    } catch (TimeoutException e) {
      metric.log(start, lib.name, "<<< progress dialog not detected");
      return;
    }
    try {
      metric.log(bot, start, shellCloses(bot.shell("Launching browser")), lib.name);
    } catch (TimeoutException e) {
      metric.log(start, lib.name, "<<< missed progress dialog close");
      return;
    }
  }

}

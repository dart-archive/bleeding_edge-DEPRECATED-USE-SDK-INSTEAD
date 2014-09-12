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

public class LaunchDartBotView extends AbstractBotView {

  private SWTBot shellBot;

  public LaunchDartBotView(SWTWorkbenchBot bot, SWTBot shellBot) {
    super(bot);
    this.shellBot = shellBot;
  }

  /**
   * Set the check mode check box to the given boolean value.
   * 
   * @param b <code>true</code> if the check box should be selected
   */
  public void checkedMode(boolean b) {
    setSelected(shellBot.checkBox("Run in checked mode"), b);
  }

  /**
   * Set the pause on exit check box to the given boolean value.
   * 
   * @param b <code>true</code> if the check box should be selected
   */
  public void pauseIsolateOnExit(boolean b) {
    setSelected(shellBot.checkBox("Pause isolate on exit"), b);
  }

  /**
   * Set the pause on start check box to the given boolean value.
   * 
   * @param b <code>true</code> if the check box should be selected
   */
  public void pauseIsolateOnStart(boolean b) {
    setSelected(shellBot.checkBox("Pause isolate on start"), b);
  }

  /**
   * Set the Dart script to the given path.
   * 
   * @param path the path to the Dart script
   */
  public void script(String path) {
    shellBot.textInGroup("Application", 0).setText(path);
  }

  /**
   * Set the working directory to the given path.
   * 
   * @param path the path to the Dart script
   */
  public void workingDirectory(String path) {
    shellBot.textInGroup("Application", 1).setText(path);
  }

  @Override
  protected String viewName() {
    return "Launch Dart";
  }
}

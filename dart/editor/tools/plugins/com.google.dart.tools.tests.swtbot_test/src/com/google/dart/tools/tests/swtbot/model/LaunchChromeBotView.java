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

public class LaunchChromeBotView extends AbstractBotView {

  private SWTBot shellBot;

  public LaunchChromeBotView(SWTWorkbenchBot bot, SWTBot shellBot) {
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
   * Return true if the checked mode check box is selected.
   */
  public boolean isCheckedMode() {
    return shellBot.checkBox("Run in checked mode").isChecked();
  }

  /**
   * Get the path to the launch target.
   */
  public String target() {
    return shellBot.textInGroup("Launch target").getText();
  }

  /**
   * Set the path to the manifest.json to the given argument.
   * 
   * @param path the path to manifest.json
   */
  public void target(String path) {
    shellBot.textInGroup("Launch target").setText(path);
  }

  @Override
  protected String viewName() {
    return "Launch Chrome";
  }

}

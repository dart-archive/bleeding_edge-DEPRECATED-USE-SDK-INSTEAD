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

public class LaunchDartiumBotView extends LaunchBrowserBasedBotView {

  public LaunchDartiumBotView(SWTWorkbenchBot bot, SWTBot shellBot) {
    super(bot, shellBot);
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
   * Set the experimental features check box to the given boolean value.
   * 
   * @param b <code>true</code> if the check box should be selected
   */
  public void experimentalFeatures(boolean b) {
    setSelected(shellBot.checkBox("Enable experimental browser features (Web Components)"), b);
  }

  /**
   * Set the show output check box to the given boolean value.
   * 
   * @param b <code>true</code> if the check box should be selected
   */
  public void showOutput(boolean b) {
    setSelected(shellBot.checkBox("Show browser stdout and stderr output"), b);
  }

  /**
   * Set the pub serve check box to the given boolean value.
   * 
   * @param b <code>true</code> if the check box should be selected
   */
  public void usePubServe(boolean b) {
    setSelected(shellBot.checkBox("Use pub serve to serve the application"), b);
  }

  @Override
  protected String viewName() {
    return "Launch Dartium";
  }
}

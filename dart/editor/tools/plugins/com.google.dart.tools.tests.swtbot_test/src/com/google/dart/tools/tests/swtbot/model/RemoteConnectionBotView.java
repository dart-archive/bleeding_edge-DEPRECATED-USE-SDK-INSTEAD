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
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class RemoteConnectionBotView extends AbstractBotView {

  public RemoteConnectionBotView(SWTWorkbenchBot bot) {
    super(bot);
    bot.waitUntilWidgetAppears(Conditions.shellIsActive(viewName()));
  }

  /**
   * Close the dialog.
   */
  public void close() {
    remoteConnectionShell().bot().button("OK").click();
    waitForAnalysis();
  }

  /**
   * Get the host.
   */
  public String host() {
    SWTBot shellBot = shellBot();
    return shellBot.textInGroup("Connection parameters", 0).getText();
  }

  /**
   * Set the host name and port number for the remote connection
   * 
   * @param host the host name
   * @param port the port number
   */
  public void host(String host, String port) {
    SWTBot shellBot = shellBot();
    shellBot.textInGroup("Connection parameters", 0).setText(host);
    shellBot.textInGroup("Connection parameters", 1).setText(port);
  }

  /**
   * Get the port.
   */
  public String port() {
    SWTBot shellBot = shellBot();
    return shellBot.textInGroup("Connection parameters", 1).getText();
  }

  /**
   * Connect to Chrome-based browser.
   */
  public void useChrome() {
    selectConnection(0);
  }

  /**
   * Set the pub serve check box to the given boolean value.
   * 
   * @param b <code>true</code> if the check box should be selected
   */
  public void usePubServe(boolean b) {
    setSelected(shellBot().checkBox("Using pub to serve the application"), b);
  }

  /**
   * Connect to command-line VM.
   */
  public void useVM() {
    selectConnection(1);
  }

  /**
   * Get the state of the pub check box.
   */
  public boolean usingPub() {
    SWTBot shellBot = shellBot();
    return shellBot.checkBox("Using pub to serve the application").isChecked();
  }

  @Override
  protected String viewName() {
    return "Open Remote Connection";
  }

  private SWTBotShell remoteConnectionShell() {
    return bot.shell(viewName());
  }

  private void selectConnection(int index) {
    shellBot().comboBox().setSelection(index);
  }

  private SWTBot shellBot() {
    SWTBotShell botShell = remoteConnectionShell();
    SWTBot shellBot = botShell.bot();
    return shellBot;
  }
}

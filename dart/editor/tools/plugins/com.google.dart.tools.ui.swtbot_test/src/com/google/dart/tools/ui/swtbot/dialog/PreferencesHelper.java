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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

/**
 * Drive the "Preferences" dialog
 */
public class PreferencesHelper {

  private final SWTWorkbenchBot bot;

  private SWTBotShell shell;

  public PreferencesHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  public void close() {
    if (shell != null && shell.isOpen()) {
      shell.close();
      shell = null;
    }
  }

  public SWTBotShell open() {
    if (shell == null) {
      // TODO (jwren) Do some research online to figure out how to get the preferences dialog open,
      // this may be different on different OSs
      // Open dialog
//      bot.bot.menu("Dart Editor").click();
//      shell = bot.shell("Preferences");
//      assertNotNull(shell);
//      assertTrue(shell.isOpen());
//      shell.activate();
    }
    return shell;
  }

}

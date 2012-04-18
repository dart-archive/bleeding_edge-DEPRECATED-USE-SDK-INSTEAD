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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.swtbot.performance.Performance;
import com.google.dart.tools.ui.swtbot.util.SWTBotUtil;

import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.activeShell;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Drive the "Preferences" dialog
 */
@SuppressWarnings("restriction")
public class PreferencesHelper {

  @SuppressWarnings("unused")
  private final SWTWorkbenchBot bot;

  private SWTBotShell shell;

  public PreferencesHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  public void assertDefaultPreferencesSelected() {
    SWTBotCheckBox lineNumbersCheckBox = bot.checkBoxInGroup("Show line numbers", "General");
    SWTBotCheckBox printMarginCheckBox = bot.checkBoxInGroup(
        "Show print margin at column:",
        "General");

    assertNotNull(lineNumbersCheckBox);
    assertFalse(lineNumbersCheckBox.isChecked());
    assertTrue(lineNumbersCheckBox.isEnabled());
    assertTrue(lineNumbersCheckBox.isVisible());

    assertNotNull(printMarginCheckBox);
    assertFalse(printMarginCheckBox.isChecked());
    assertTrue(printMarginCheckBox.isEnabled());
    assertTrue(printMarginCheckBox.isVisible());

    // TODO (jwren) make assertions on the rest of the default-preferences
  }

  public void close() {
    if (shell != null && shell.isOpen()) {
      shell.close();
      shell = null;
    }
  }

  @SuppressWarnings("restriction")
  public SWTBotShell open() {
    final SWTBotShell mainShell = activeShell(bot);
    if (shell == null) {
      // Open dialog
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          try {
            WorkbenchPreferenceDialog.createDialogOn(SWTBotUtil.getShell(), null).open();
          } catch (Exception e) {
            DartToolsPlugin.log(e);
          }
        }
      });

      // Wait for the main shell to loose focus
      bot.waitUntil(new ICondition() {

        @Override
        public String getFailureMessage() {
          return "Failed to detect launch of the Preference dialog";
        }

        @Override
        public void init(SWTBot bot) {
        }

        @Override
        public boolean test() throws Exception {
          return !mainShell.isActive();
        }
      }, Performance.DEFAULT_TIMEOUT_MS);

      SWTBotShell activeShell = activeShell(bot);

      // Assert that the active shell is the Preferences dialog
      assertNotNull(activeShell);
      assertTrue(activeShell.getText().equals("Preferences"));
      assertTrue(activeShell.isEnabled());
      assertTrue(activeShell.isOpen());
      assertTrue(activeShell.isVisible());

      shell = activeShell;
    }
    return shell;
  }

}

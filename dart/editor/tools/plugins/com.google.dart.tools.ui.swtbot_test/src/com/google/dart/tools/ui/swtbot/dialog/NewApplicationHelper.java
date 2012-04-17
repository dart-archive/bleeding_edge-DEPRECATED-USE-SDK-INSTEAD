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
import com.google.dart.tools.ui.swtbot.matchers.EditorWithTitle;
import com.google.dart.tools.ui.swtbot.performance.Performance;
import com.google.dart.tools.ui.swtbot.util.SWTBotUtil;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

public class NewApplicationHelper {

  public enum ContentType {
    WEB, SERVER
  }

  private final SWTWorkbenchBot bot;

  public NewApplicationHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  /**
   * Drive the "New Application..." dialog to create a new application with the specified name
   * 
   * @param appName the application name
   * @return the new application
   */
  public DartLib create(String appName, ContentType contentType) {

    // Open wizard
    bot.menu("File").menu("New Application...").click();
    SWTBotShell shell = bot.activeShell();
    shell.activate();

    // Reference widgets and Assert content
    SWTBotText appNameField = bot.textWithLabel("Name: ");
    assertNotNull(appNameField);
    // By calling setFocus on this widget, we ensure that this dialog is made the top-most
    // window before the click action happens.
    appNameField.setFocus();
    SWTBotUtil.waitForMainShellToDisappear(bot);
    SWTBotText appDirField = bot.textWithLabel("Directory: ");
    SWTBotButton browseButton = bot.button("Browse...");
    SWTBotButton finishButton = bot.button("Finish");

    SWTBotRadio webAppRadio = bot.radioInGroup("Web application", "Create sample content");
    SWTBotRadio serverAppRadio = bot.radioInGroup("Server application", "Create sample content");

    assertEquals("", appNameField.getText());
    assertTrue(appDirField.getText().length() > 0);
    assertNotNull(browseButton);
    assertNotNull(finishButton);

    assertTrue(webAppRadio.isSelected());
    assertFalse(serverAppRadio.isSelected());

    // Make either the selection of the web sample, or the server sample
    switch (contentType) {
      case WEB:
        webAppRadio.click();
        assertTrue(webAppRadio.isSelected());
        assertFalse(serverAppRadio.isSelected());
        break;
      case SERVER:
        serverAppRadio.click();
        assertTrue(serverAppRadio.isSelected());
        assertFalse(webAppRadio.isSelected());
        break;
    }

    // Ensure that the directory to be created does not exist
    DartLib lib = new DartLib(new File(appDirField.getText(), appName), appName);
    lib.deleteDir();

    // Enter name of new app
    appNameField.typeText(appName);

    // Click OK button and wait for the operation to complete
    finishButton.click();
    lib.logFullAnalysisTime();
    EditorWithTitle matcher = new EditorWithTitle(lib.dartFile.getName());
    Performance.NEW_APP.log(bot, waitForEditor(matcher), appName);
    lib.editor = bot.editor(matcher).toTextEditor();
    return lib;
  }

}

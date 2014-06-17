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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * Model the Files view of Dart Editor.
 */
public class FilesBotView extends AbstractBotView {

  public FilesBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  public void deleteProject(String name) {
    SWTBotView files = bot.viewByPartName("Files");
    files.setFocus();
    SWTBotTreeItem[] items = files.bot().tree().getAllItems();
    SWTBotTreeItem item = items[0]; // TODO match name
    item.select();
    item.contextMenu("Delete").click();
    bot.waitUntil(Conditions.shellIsActive("Delete Resources"));
    SWTBotShell shell = bot.shell("Delete Resources");
    shell.bot().checkBox().click();
    shell.bot().button("OK").click();
    waitForAnalysis();
  }

  public boolean isEmpty() {
    SWTBotView files = bot.viewByPartName("Files");
    files.setFocus();
    SWTBotTreeItem[] items = files.bot().tree().getAllItems();
    return items.length == 2;
  }
}

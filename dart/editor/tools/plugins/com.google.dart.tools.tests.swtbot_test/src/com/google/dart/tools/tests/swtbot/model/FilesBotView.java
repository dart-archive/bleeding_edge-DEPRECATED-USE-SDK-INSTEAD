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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.ScanCallbackProvider;
import com.google.dart.tools.ui.actions.CreateAndRevealProjectAction;

import org.eclipse.core.resources.IProject;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.Workbench;

import static org.junit.Assert.fail;

import java.io.File;

/**
 * Model the Files view of Dart Editor.
 */
@SuppressWarnings("restriction")
public class FilesBotView extends AbstractTreeBotView {

  public FilesBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * If a project with the given <code>name</code> exists, delete it.
   * 
   * @param name the potentially existing project to delete
   */
  public void deleteExistingProject(String name) {
    try {
      if (openExistingFolder(name)) {
        deleteProject(name);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Could not clean up old project: " + name);
    }
  }

  /**
   * Delete the named project.
   * 
   * @param name the project to delete
   */
  public void deleteProject(String name) {
    SWTBotTreeItem item = select(name);
    item.contextMenu("Delete").click();
    bot.waitUntil(Conditions.shellIsActive("Delete Resources"));
    SWTBotShell shell = bot.shell("Delete Resources");
    shell.bot().checkBox().click();
    shell.bot().button("OK").click();
    waitForAnalysis();
  }

  /**
   * Return true if the Files view has nothing other than the two default items.
   * 
   * @return true if there are no projects
   */
  public boolean isEmpty() {
    SWTBotView files = bot.viewByPartName("Files");
    files.setFocus();
    SWTBotTreeItem[] items = files.bot().tree().getAllItems();
    return items.length == 2;
  }

  /**
   * Open the existing project name <code>name</code> in the user's default dart folder if it
   * exists.
   * 
   * @param name the project name
   * @return true if the existing project was opened
   */
  public boolean openExistingFolder(final String name) {
    final File existingDir = new File(DartCore.getUserDefaultDartFolder(), name);
    if (!existingDir.exists()) {
      return false;
    }
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        IWorkbenchWindow window = Workbench.getInstance().getActiveWorkbenchWindow();
        String dir = existingDir.toString();
        CreateAndRevealProjectAction createAction = new CreateAndRevealProjectAction(window, dir);
        createAction.run();
        IProject project = createAction.getProject();
        if (project != null) {
          // show analysis progress dialog for open folder
          ScanCallbackProvider.setNewProjectName(name);
        }
      }
    });
    waitForAnalysis();
    return true;
  }

  @Override
  protected String viewName() {
    return "Files";
  }
}

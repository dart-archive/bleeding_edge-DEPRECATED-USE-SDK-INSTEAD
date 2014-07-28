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
package com.google.dart.tools.tests.swtbot.test;

import com.google.dart.tools.tests.swtbot.conditions.TreeHasSomeRows;
import com.google.dart.tools.tests.swtbot.harness.EditorTestHarness;
import com.google.dart.tools.tests.swtbot.model.EditorBotWindow;
import com.google.dart.tools.tests.swtbot.model.FilesBotView;
import com.google.dart.tools.tests.swtbot.model.SearchBotView;
import com.google.dart.tools.tests.swtbot.model.TextBotEditor;
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO: This sometimes fails because indexed_db.dart shows up in search results, but not always.
 * When it is first in the list, it will be the first file opened during navigation, but it will not
 * be closed when the project is deleted, since it is in the SDK. Other non-project files may appear
 * in search results, so we need a way to delete all editor tabs except Welcome during
 * tearDownTest().
 */
public class TestSearch extends EditorTestHarness {

  private static TextBotEditor editor;
  private static SearchBotView search;

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("pop_pop_win");
    final WelcomePageEditor page = main.openWelcomePage();
    page.createPopPopWin();
    SWTBotTreeItem item;
    item = files.select("pop_pop_win", "web", "platform_web.dart [pop_pop_win.platform_web]");
    item.doubleClick();
    page.waitForAnalysis();
    editor = new TextBotEditor(bot, "platform_web.dart");
    SWTBotStyledText text = editor.select("StreamController", 3);
    text.contextMenu("Find Uses").click();
    page.waitForAnalysis();
    page.waitForAsyncDrain();
    search = main.searchView();
  }

  @AfterClass
  public static void tearDownTest() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteProject("pop_pop_win");
  }

  @Test
  public void test() throws Exception {
    int originalSize = search.treeSize();
    search.collapseAll();
    int collapsedSize = search.treeSize();
    assertTrue(collapsedSize < originalSize);

    search.expandAll();
    bot.waitUntil(new TreeHasSomeRows(search.tree(), collapsedSize + 1));
    int expandedSize = search.treeSize();
    assertTrue(expandedSize >= originalSize);

    search.toggleFilterOutPotential(); // no potentials so no change
    search.collapseAll();
    search.toggleFilterToProject();
    bot.waitUntil(new TreeHasSomeRows(search.tree(), collapsedSize + 1));
    int projectFilterSize = search.treeSize();
    assertTrue(projectFilterSize < expandedSize);

    search.collapseAll();
    search.toggleFilterOutSdk();
    bot.waitUntil(new TreeHasSomeRows(search.tree(), collapsedSize + 1));
    int sdkFilterSize = search.treeSize();
    assertTrue(sdkFilterSize < expandedSize);

    search.toggleFilterOutPotential();
    search.toggleFilterToProject();
    search.collapseAll();
    search.toggleFilterOutSdk();
    search.expandAll();
    bot.waitUntil(new TreeHasSomeRows(search.tree(), collapsedSize + 1));
//    assertEquals(expandedSize, search.treeSize());

    search.showNext();
    search.showNext();
    search.showPrevious();
    search.removeSelected();
    assertTrue(search.treeSize() < expandedSize);

    search.refreshSearch();
//    assertEquals(originalSize, search.treeSize());
    search.removeAll();
  }
}

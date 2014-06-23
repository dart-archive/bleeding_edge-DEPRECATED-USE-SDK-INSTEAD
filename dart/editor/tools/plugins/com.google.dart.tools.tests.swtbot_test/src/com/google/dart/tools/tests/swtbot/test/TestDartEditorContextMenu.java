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

import com.google.dart.tools.tests.swtbot.harness.EditorTestHarness;
import com.google.dart.tools.tests.swtbot.model.EditorBotWindow;
import com.google.dart.tools.tests.swtbot.model.FilesBotView;
import com.google.dart.tools.tests.swtbot.model.TextBotEditor;
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TestDartEditorContextMenu extends EditorTestHarness {

  private static TextBotEditor editor;

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sunflower");
    WelcomePageEditor page = main.openWelcomePage();
    page.createPopPopWin();
    SWTBotTreeItem item;
    item = files.select("pop_pop_win", "web", "platform_web.dart [pop_pop_win.platform_web]");
    item.doubleClick();
    page.waitForAnalysis();
    editor = new TextBotEditor(bot, "platform_web.dart");
  }

  @AfterClass
  public static void tearDownTest() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteProject("pop_pop_win");
  }

  @Test
  public void testClassDef() throws Exception {
    SWTBotStyledText item = editor.select("class PlatformWeb", 8);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Show Hierarchy"));
    assertNotNull(item.contextMenu("Rename..."));
    checkStandardItems(item);
  }

  @Test
  public void testClassRef() throws Exception {
    SWTBotStyledText item = editor.select("PlatformTarget", 2);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Open Declaration"));
    assertNotNull(item.contextMenu("Show Hierarchy"));
    assertNotNull(item.contextMenu("Rename..."));
    checkStandardItems(item);
  }

  @Test
  public void testFieldDef() throws Exception {
    SWTBotStyledText item = editor.select("bool _sizeAccessed", 6);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Find Declarations"));
    assertNotNull(item.contextMenu("Rename..."));
    checkStandardItems(item);
  }

  @Test
  public void testLocalVarRef() throws Exception {
    SWTBotStyledText item = editor.select("hash.replaceAll", 2);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Open Declaration"));
    assertNotNull(item.contextMenu("Rename..."));
    assertNotNull(item.contextMenu("Inline..."));
    assertNotNull(item.contextMenu("Quick Fix"));
    checkStandardItems(item);
  }

  @Test
  public void testMethodDef() throws Exception {
    SWTBotStyledText item = editor.select("Future clearValues()", 9);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Find Declarations"));
    assertNotNull(item.contextMenu("Rename..."));
    assertNotNull(item.contextMenu("Inline..."));
    assertNotNull(item.contextMenu("Convert Method to Getter..."));
    checkStandardItems(item);
  }

  @Test
  public void testSdkClassRef() throws Exception {
    SWTBotStyledText item = editor.select("String", 2);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Open Declaration"));
    assertNotNull(item.contextMenu("Show Hierarchy"));
    assertNotNull(item.contextMenu("Browse Dart Doc"));
    checkStandardItems(item);
  }

  @Test
  public void testSdkFieldRef() throws Exception {
    SWTBotStyledText item = editor.select("window", 2);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Find Declarations"));
    assertNotNull(item.contextMenu("Open Declaration"));
    assertNotNull(item.contextMenu("Browse Dart Doc"));
    checkStandardItems(item);
  }

  @Test
  public void testSdkGetterRef() throws Exception {
    SWTBotStyledText item = editor.select("window.localStorage", 9);
    assertNotNull(item.contextMenu("Find Uses"));
    assertNotNull(item.contextMenu("Find Declarations"));
    assertNotNull(item.contextMenu("Open Declaration"));
    assertNotNull(item.contextMenu("Browse Dart Doc"));
    checkStandardItems(item);
  }

  private void checkStandardItems(SWTBotStyledText item) {
    assertNotNull(item.contextMenu("Undo"));
    assertNotNull(item.contextMenu("Cut"));
    assertNotNull(item.contextMenu("Copy"));
    assertNotNull(item.contextMenu("Paste"));
    assertNotNull(item.contextMenu("Format"));
    assertNotNull(item.contextMenu("Quick Fix"));
    assertNotNull(item.contextMenu("Outline File"));
    assertNotNull(item.contextMenu("Revert File"));
  }
}

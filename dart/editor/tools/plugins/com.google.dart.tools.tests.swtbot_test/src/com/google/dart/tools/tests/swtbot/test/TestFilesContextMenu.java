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
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TestFilesContextMenu extends EditorTestHarness {

  private static FilesBotView files;

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
    EditorBotWindow main = new EditorBotWindow(bot);
    files = main.filesView();
    files.deleteExistingProject("sunflower");
    WelcomePageEditor page = main.openWelcomePage();
    page.clickSunflower();
    page.waitForAnalysis();
  }

  @AfterClass
  public static void tearDownTest() {
    files.deleteProject("sunflower");
  }

  @Test
  public void testCSS() throws Exception {
    SWTBotTreeItem item = files.select("sunflower", "web", "sunflower.css");
    assertNotNull(item.contextMenu("Open as Text"));
  }

  @Test
  public void testDart() throws Exception {
    SWTBotTreeItem item = files.select("sunflower", "web", "sunflower.dart [sunflower]");
    assertNotNull(item.contextMenu("Format"));
    assertNotNull(item.contextMenu("Don't Analyze"));
    assertNotNull(item.contextMenu("Run in Dartium"));
    assertNotNull(item.contextMenu("Run as JavaScript"));
    assertNotNull(item.contextMenu("Run on Mobile"));
  }

  @Test
  public void testHTML() throws Exception {
    SWTBotTreeItem item = files.select("sunflower", "web", "sunflower.html");
    assertNotNull(item.contextMenu("Open as Text"));
    assertNotNull(item.contextMenu("Don't Analyze"));
    assertNotNull(item.contextMenu("Run in Dartium"));
    assertNotNull(item.contextMenu("Run as JavaScript"));
    assertNotNull(item.contextMenu("Run on Mobile"));
  }

  @Test
  public void testNoSelection() throws Exception {
    SWTBotTree tree = files.unselectAll();
    assertNotNull(tree.contextMenu("Open Existing Folder..."));
  }

  @Test
  public void testPackages() throws Exception {
    SWTBotTreeItem item = files.select("sunflower", "packages");
    assertNotNull(item.contextMenu("Find packages to include")); // bug: caps
  }

  @Test
  public void testProject() throws Exception {
    SWTBotTreeItem item = files.select("sunflower");
    assertNotNull(item.contextMenu("Close Folder"));
    assertNotNull(item.contextMenu("Reanalyze Sources"));
  }

  @Test
  public void testPubspec() throws Exception {
    SWTBotTreeItem item = files.select("sunflower", "pubspec.yaml");
    assertNotNull(item.contextMenu("Open as Text"));
    assertNotNull(item.contextMenu("Pub Get"));
    assertNotNull(item.contextMenu("Pub Get Offline"));
    assertNotNull(item.contextMenu("Pub Upgrade"));
    assertNotNull(item.contextMenu("Pub Build (generates JS)")); // bug: caps
  }
}

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
import com.google.dart.tools.tests.swtbot.model.PackagesBotView;
import com.google.dart.tools.tests.swtbot.model.ProblemsBotView;
import com.google.dart.tools.tests.swtbot.model.PubspecBotEditor;
import com.google.dart.tools.tests.swtbot.model.TextBotEditor;
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestPubspecEditor extends EditorTestHarness {

  private static EditorBotWindow editor;
  private static FilesBotView files;

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
    editor = new EditorBotWindow(bot);
    files = editor.filesView();
    files.deleteExistingProject("sunflower");
    assertTrue(files.isEmpty());
  }

  @AfterClass
  public static void tearDownTest() {
    try {
      editor.menu("File").menu("Close All").click();
    } catch (Exception ex) {
      // If we get here, we don't care about exceptions.
    }
  }

  @Test
  public void testPubspecEditor() throws Exception {
    WelcomePageEditor page = editor.openWelcomePage();
    page.createSunflower();
    ProblemsBotView problems = editor.problemsView();
    assertTrue(problems.isEmpty()); // ensure timing is correct

    SWTBotTreeItem item = files.select("sunflower", "pubspec.yaml");
    item.doubleClick();
    PubspecBotEditor pubspec = new PubspecBotEditor(bot);
    pubspec.pubGet();
    pubspec.pubBuild();
    PackagesBotView packages = pubspec.packagesView();
    int count = packages.tableSize();
    packages.filter("unittest");
    // Verify that filtering does something.
    assertTrue(packages.tableSize() < count);
    TextBotEditor yaml = pubspec.switchToYamlEditor();
    // Fail if the text editor didn't appear or doesn't have expected content.
    yaml.select("name:");

    files.deleteProject("sunflower");
  }
}

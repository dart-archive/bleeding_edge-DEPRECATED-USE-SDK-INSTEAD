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
import com.google.dart.tools.tests.swtbot.model.ExtractMethodBotView;
import com.google.dart.tools.tests.swtbot.model.FilesBotView;
import com.google.dart.tools.tests.swtbot.model.InlineMethodBotView;
import com.google.dart.tools.tests.swtbot.model.TextBotEditor;
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestExtractMethod extends EditorTestHarness {

  private static TextBotEditor editor;

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
  }

  @AfterClass
  public static void tearDownTest() {
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteProject("pop_pop_win");
    main.menu("File").menu("Close All").click();
  }

  @Test
  public void testExtractAndInline() throws Exception {
    editor.editor().selectLine(27);
    ExtractMethodBotView extractor = editor.openExtractMethodWizard();
    extractor.methodNameField().typeText("randomMethod");
    extractor.editParam(0, null, "string");
    extractor.moveDown();
    extractor.editParam(0, "Object", null);
    extractor.close();
    String selected = editor.selection();
    assertEquals("    randomMethod(value, key);", selected);
    SWTBotEclipseEditor typist = editor.editor();
    typist.pressShortcut(SWT.NONE, SWT.ARROW_LEFT, (char) SWT.NONE);
    for (int i = 0; i < 5; i++) {
      typist.pressShortcut(SWT.NONE, SWT.ARROW_RIGHT, (char) SWT.NONE);
    }
    InlineMethodBotView inliner = editor.openInlineMethodWizard();
    inliner.clickAllInvocations();
    inliner.close();
    selected = editor.selection();
    assertEquals("    window.localStorage[key] = value;\n", selected);
  }
}

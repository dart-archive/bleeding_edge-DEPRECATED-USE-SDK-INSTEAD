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

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestDartDocDoubleClick extends EditorTestHarness {
  private static TextBotEditor editor;

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sample");
    main.createCommandLineProject("sample");
    editor = new TextBotEditor(bot, "sample.dart");
    SWTBotEclipseEditor text = editor.editor();
    text.typeText("\n");
    text.pressShortcut(SWT.NONE, SWT.ARROW_UP, (char) SWT.NONE);
    text.typeText("/// This is a @sample app");
    text.pressShortcut(SWT.NONE, SWT.ARROW_RIGHT, (char) SWT.NONE);
    text.pressShortcut(SWT.NONE, SWT.ARROW_UP, (char) SWT.NONE);
    editor.save();
    editor.waitForAnalysis();
  }

  @AfterClass
  public static void tearDownTest() {
    try {
      EditorBotWindow main = new EditorBotWindow(bot);
      FilesBotView files = main.filesView();
      files.deleteExistingProject("sample");
      main.menu("File").menu("Close All").click();
    } catch (TimeoutException ex) {
      // If we get here, we don't care about exceptions.
    }
  }

  @Test
  public void testDartDocDoubleClick() throws Exception {
    editor.doubleClick(1, 9, false);
    assertEquals("This", editor.selection());
    editor.doubleClick(1, 15, true);
    assertEquals("@sample", editor.selection());
    editor.doubleClick(1, 10);
    assertEquals("is", editor.selection());
    editor.doubleClick(1, 22, true);
    assertEquals("@sample", editor.selection());
  }
}

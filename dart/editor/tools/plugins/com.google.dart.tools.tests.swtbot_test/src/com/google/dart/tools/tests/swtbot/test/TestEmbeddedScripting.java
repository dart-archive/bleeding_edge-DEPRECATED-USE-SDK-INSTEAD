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
import com.google.dart.tools.tests.swtbot.model.CompletionProposalsBotView;
import com.google.dart.tools.tests.swtbot.model.EditorBotWindow;
import com.google.dart.tools.tests.swtbot.model.FilesBotView;
import com.google.dart.tools.tests.swtbot.model.FindTextBotView;
import com.google.dart.tools.tests.swtbot.model.TextBotEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestEmbeddedScripting extends EditorTestHarness {

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
  }

  @AfterClass
  public static void tearDownTest() {
    try {
      EditorBotWindow main = new EditorBotWindow(bot);
      FilesBotView files = main.filesView();
      files.deleteProject("sample");
      main.menu("File").menu("Close All").click();
    } catch (TimeoutException ex) {
      // If we get here, we don't care about exceptions.
    }
  }

  private boolean sampleIsValid = false;
  private TextBotEditor htmlEditor;

  @Test
  public void testAngularCompletion() throws Exception {
    buildProject();
    exerciseCompletion(31, 8, "<", 0, "a");
  }

  @Test
  public void testEmbeddedDartCompletion() throws Exception {
    buildProject();
    exerciseCompletion(18, 54, ".", 0, "codeUnits \u2192 List<int>");
  }

  @Test
  public void testHyperlink() throws Exception {
    buildProject();
    htmlEditor.clickHyperlinkAt(14, 31);
    assertEquals("reverseText", htmlEditor.selection());
  }

  /**
   * Create and edit a sample project usable for these tests. Each test is required to leave the
   * sample in its initial state. Whenever the file is modified sampleIsValid should be set false.
   * When that edit is reversed then sampleIsValid should be set true.
   */
  private void buildProject() {
    if (sampleIsValid) {
      return;
    }
    EditorBotWindow main = new EditorBotWindow(bot);
    FilesBotView files = main.filesView();
    files.deleteExistingProject("sample");
    // Begin with a simple web app.
    main.createWebProject("sample");
    TextBotEditor dartEditor = new TextBotEditor(bot, "sample.dart");
    dartEditor.selectAll();
    // Copy the Dart code.
    String script = dartEditor.selection();
    // Open the HTML file to which the Dart code will be added.
    SWTBotTreeItem item = files.select("sample", "web", "sample.html");
    item.doubleClick();
    main.waitForAnalysis();
    htmlEditor = new TextBotEditor(bot, "sample.html");
    // Find the script tag.
    FindTextBotView finder = htmlEditor.findText("application/dart");
    finder.dismiss();
    SWTBotEclipseEditor typist = htmlEditor.editor();
    // Skip over the close quote.
    typist.pressShortcut(SWT.NONE, SWT.ARROW_RIGHT, (char) SWT.NONE);
    typist.pressShortcut(SWT.NONE, SWT.ARROW_RIGHT, (char) SWT.NONE);
    // Delete the src attribute.
    for (int i = 0; i < 18; i++) {
      typist.pressShortcut(SWT.SHIFT, SWT.ARROW_RIGHT, (char) SWT.NONE);
    }
    typist.pressShortcut(SWT.NONE, SWT.DEL, (char) SWT.NONE);
    htmlEditor.waitForAsyncDrain();
    // Skip over the close bracket.
    typist.pressShortcut(SWT.NONE, SWT.ARROW_RIGHT, (char) SWT.NONE);
    // Type a couple newlines.
    typist.typeText("\n");
    htmlEditor.waitForAsyncDrain();
    typist.typeText("\n");
    htmlEditor.waitForAsyncDrain();
    // Go up one line.
    typist.pressShortcut(SWT.NONE, SWT.ARROW_UP, (char) SWT.NONE);
    // And insert the Dart script copied from the sample.dart file.
    typist.insertText(script);
    htmlEditor.waitForAsyncDrain();
    typist.navigateTo(0, 0);
    htmlEditor.save();
    htmlEditor.waitForAnalysis();
    sampleIsValid = true;
  }

  /**
   * Exercise completion by navigating to a location, inserting a trigger character, selecting a
   * proposal and confirming that it matches the expected proposal.
   * 
   * @param line the line number (0-based)
   * @param col the column number (0-based)
   * @param trigger the trigger character in a 1-char long String
   * @param proposal the 0-based index of the proposal to select
   * @param expected the expected text at the proposal-number
   */
  private void exerciseCompletion(int line, int col, String trigger, int proposal, String expected) {
    // Go to a likely insertion point.
    SWTBotEclipseEditor typist = htmlEditor.editor();
    typist.navigateTo(line, col);
    // Trigger completion and wait for the pop-up.
    typist.typeText(trigger);
    sampleIsValid = false;
    // Select the proposal-index item and retrieve it without inserting it.
    CompletionProposalsBotView completions = htmlEditor.completionList();
    TableCollection sel = completions.select(proposal);
    String choice = sel.get(0, 0);
    // Dismiss the completion list.
    completions.traverse(SWT.TRAVERSE_ESCAPE);
    // Validate the selection.
    assertEquals(expected, choice);
    // Delete the completion trigger and signal the code is in good shape.
    htmlEditor.undo();
    htmlEditor.save();
    sampleIsValid = true;
  }
}

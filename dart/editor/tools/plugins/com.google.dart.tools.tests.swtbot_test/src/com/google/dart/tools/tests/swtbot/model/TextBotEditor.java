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

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.junit.Assert.fail;

/**
 * Model a code editor of Dart Editor.
 */
public class TextBotEditor extends AbstractBotView {

  private static KeyStroke keyM1;
  private static KeyStroke keyF;

  static {
    try {
      // Apparently there is no platform-independent method to construct KeyStrokes for modifiers
      // without going through this indirection.
      keyM1 = KeyStroke.getInstance("M1+F");
      keyM1 = KeyStroke.getInstance(keyM1.getModifierKeys(), KeyStroke.NO_KEY);
      keyF = KeyStroke.getInstance("F");
    } catch (ParseException e) {
      // Won't happen
    }
  }

  private final String title;

  public TextBotEditor(SWTWorkbenchBot bot, String title) {
    super(bot);
    this.title = title;
  }

  /**
   * Return the SWTBotEclipseEditor for this editor pane.
   * 
   * @return the SWTBotEclipseEditor
   */
  public SWTBotEclipseEditor editor() {
    return bot.editorByTitle(title).toTextEditor();
  }

  /**
   * Use the Find Text panel to find all occurrences of the given text.
   * 
   * @param text
   * @return the bot that controls the find-text panel
   */
  public FindTextBotView findText(String text) {
    final SWTBotEclipseEditor editor = editor();
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        editor.pressShortcut(keyM1, keyF);
      }
    });
    waitForAnalysis();
    FindTextBotView finder = new FindTextBotView(bot);
    finder.findText(text);
    waitMillis(500);
    return finder;
  }

  /**
   * Set the selection to the given string. If the optional <code>delta</code> is given, rather than
   * setting the selection to a range, set it to the number of characters from the beginning of the
   * <code>selection</code> as given by <code>delta[0]</code>.
   * 
   * @param selection the string to search for an select
   * @param delta an optional single integer that defines a position relative to the beginning of
   *          <code>selection</code> which should become the cursor position
   * @return
   */
  public SWTBotStyledText select(String selection, int... delta) {
    SWTBotEditor editor = bot.editorByTitle(title);
    editor.show();
    SWTBotStyledText text = editor.bot().styledText();
    String content = text.getText();
    IDocument doc = new Document(content);
    FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(doc);
    try {
      IRegion found = finder.find(0, selection, true, true, false, false);
      int offset = found.getOffset();
      int line = doc.getLineOfOffset(offset);
      int column = offset - doc.getLineInformationOfOffset(offset).getOffset();
      if (delta.length > 0) {
        text.selectRange(line, column + delta[0], 0);
      } else {
        text.selectRange(line, column, selection.length());
      }
      return text;
    } catch (BadLocationException ex) {
      fail(ex.getMessage());
      throw new RuntimeException(ex);
    }
  }

  /**
   * Return the currently-selected string.
   * 
   * @return the selection
   */
  public String selection() {
    SWTBotEditor editor = bot.editorByTitle("platform_web.dart");
    SWTBotStyledText text = editor.bot().styledText();
    String selection = text.getSelection();
    return selection;
  }

  @Override
  protected String viewName() {
    return title;
  }

  @SuppressWarnings("unused")
  private IEditorReference editorReference() {
    // TODO for reference only; probably want to use SWTBotView
    return UIThreadRunnable.syncExec(new Result<IEditorReference>() {
      @Override
      public IEditorReference run() {
        IWorkbenchWindow bench = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IEditorReference[] refs = bench.getActivePage().getEditorReferences();
        for (IEditorReference ref : refs) {
          if (title.equals(ref.getTitle())) {
            return ref;
          }
        }
        return null;
      }
    });
  }
}

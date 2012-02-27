/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot;

import com.google.dart.tools.ui.swtbot.matchers.TableItemWithText;
import com.google.dart.tools.ui.swtbot.performance.Performance;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.exceptions.QuickFixNotFoundException;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.Finder;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.waits.WaitForObjectCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;
import org.hamcrest.Matcher;

import static org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable.syncExec;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

/**
 * Helper for manipulating source code in an editor
 */
@SuppressWarnings("restriction")
public class DartEditorHelper {

  private final SWTWorkbenchBot bot;
  private final DartLib app;
  private final SWTBotEclipseEditor editor;
  private final SWTBotStyledText styledText;

  public DartEditorHelper(SWTWorkbenchBot bot, DartLib app) {
    this.bot = bot;
    this.app = app;
    this.editor = app.editor;
    this.styledText = new SWTBotStyledText(widget());
  }

  public void codeComplete(String proposalText) {
    String simpleText = proposalText;
    if (simpleText.endsWith(".*")) {
      simpleText = simpleText.substring(0, simpleText.length() - 2);
    }
    simpleText = simpleText.replace("\\", "");
    long start = System.currentTimeMillis();
    WaitForObjectCondition<SWTBotTable> autoCompleteTable = autoCompleteAppears(tableWithRow(proposalText));
    waitUntil(autoCompleteTable);
    Performance.CODE_COMPLETION.log(start, simpleText);
    selectProposal(autoCompleteTable.get(0), proposalText);
  }

  public void codeComplete(String insertText, String proposalText) {
    editor.typeText(insertText);
    codeComplete(proposalText);
  }

  public SWTBotEclipseEditor editor() {
    return editor;
  }

  public void moveToEndOfLineContaining(String text) {
    moveToStartOfLineContaining(text);
    editor.pressShortcut(Keystrokes.DOWN, Keystrokes.LEFT);
  }

  /**
   * Position the cursor at the beginning of the line containing the specified text
   */
  public void moveToStartOfLineContaining(String text) {
    int line = 0;
    while (true) {
      if (editor.getTextOnLine(line).contains(text)) {
        break;
      }
      if (line > 50) {
        fail("Could not find line in editor containing \"" + text + "\"");
      }
      line++;
    }
    editor.navigateTo(line, 0);
  }

  /**
   * Save the editor and log the time for incremental compilation
   */
  public void save(String... comments) {
    editor.save();
    app.logFullAnalysisTime(comments);
  }

  /**
   * Type the specified text. Interpret '!' as request for code completion.
   */
  public void typeLine(String text) {
    editor.pressShortcut(Keystrokes.LF);
    int start = 0;
    int end = text.indexOf('!');
    while (end != -1) {
      editor.typeText(text.substring(start, end));
      if (text.length() > end + 1 && text.charAt(end + 1) == '!') {
        editor.typeText("!");
        end += 2;
      } else {

        // Trigger code completion at '!'
        start = end - 1;
        while (start > 0) {
          char ch = text.charAt(start);
          if (!Character.isLetter(ch) && !Character.isDigit(ch)) {
            break;
          }
          start--;
        }
        String proposalText = text.substring(start, end);
        end++;
        start = end;
        while (end < text.length()) {
          char ch = text.charAt(end);
          if (!Character.isLetter(ch) && !Character.isDigit(ch)) {
            break;
          }
          end++;
        }
        proposalText += text.substring(start, end);
        if (end < text.length() && text.charAt(end) == '(') {
          proposalText += "\\(";
          end++;
        }
        proposalText += ".*";
        codeComplete(proposalText);

      }
      start = end;
      end = text.indexOf('!', start);
    }
    editor.typeText(text.substring(start));
  }

  /**
   * @param matcher a matcher.
   * @return a widget within the parent widget that matches the specified matcher.
   */
  protected <S extends Widget> List<? extends S> findWidgets(Matcher<S> matcher) {
    Finder finder = bot.getFinder();
    Control control = getControl();
    boolean shouldFindInvisibleControls = finder.shouldFindInvisibleControls();
    finder.setShouldFindInvisibleControls(true);
    try {
      return bot.widgets(matcher, control);
    } catch (Exception e) {
      throw new WidgetNotFoundException(
          "Could not find any control inside the view " + editor.getReference().getPartName(), e); //$NON-NLS-1$
    } finally {
      finder.setShouldFindInvisibleControls(shouldFindInvisibleControls);
    }
  }

  private void activateAutoCompleteShell() {
    invokeAction("ContentAssistProposal");
  }

  /**
   * This activates the popup shell.
   * 
   * @return The shell.
   */
  private SWTBotShell activatePopupShell() {
    try {
      Shell mainWindow = syncExec(new WidgetResult<Shell>() {
        @Override
        public Shell run() {
          return styledText.widget.getShell();
        }
      });
      SWTBotShell shell = bot.shell("", mainWindow); //$NON-NLS-1$
      shell.activate();
      return shell;
    } catch (Exception e) {
      throw new QuickFixNotFoundException("Quickfix popup not found. Giving up.", e); //$NON-NLS-1$
    }
  }

  private WaitForObjectCondition<SWTBotTable> autoCompleteAppears(Matcher<SWTBotTable> tableMatcher) {
    return new WaitForObjectCondition<SWTBotTable>(tableMatcher) {

      @Override
      public String getFailureMessage() {
        return "Could not find auto complete proposal using matcher " + matcher;
      }

      @Override
      protected List<SWTBotTable> findMatches() {
        try {
          activateAutoCompleteShell();
          SWTBotTable autoCompleteTable = getProposalTable();
          if (matcher.matches(autoCompleteTable)) {
            return Arrays.asList(autoCompleteTable);
          }
        } catch (Throwable e) {
          //makeProposalsDisappear();
          editor.setFocus();
        }
        return null;
      }

    };
  }

  /**
   * Returns the workbench pane control.
   * 
   * @return returns the workbench pane control.
   */
  private Control getControl() {
    return ((WorkbenchPartReference) editor.getReference()).getPane().getControl();
  }

  /**
   * Gets the quick fix table.
   * 
   * @param proposalShell the shell containing the quickfixes.
   * @return the table containing the quickfix.
   */
  private SWTBotTable getProposalTable() {
    try {
      Table table = bot.widget(widgetOfType(Table.class), activatePopupShell().widget);
      return new SWTBotTable(table);
    } catch (Exception e) {
      throw new QuickFixNotFoundException("Quickfix options not found. Giving up.", e); //$NON-NLS-1$
    }
  }

  private void invokeAction(final String actionId) {
    final IAction action = ((ITextEditor) editor.getReference().getEditor(false)).getAction(actionId);
    syncExec(new VoidResult() {
      @Override
      public void run() {
        action.run();
      }
    });
  }

  /**
   * Applies the specified quickfix.
   * 
   * @param proposalTable the table containing the quickfix.
   * @param proposalIndex the index of the quickfix.
   */
  private void selectProposal(final SWTBotTable proposalTable, final int proposalIndex) {
    UIThreadRunnable.asyncExec(new VoidResult() {
      @Override
      public void run() {
        Table table = proposalTable.widget;
        table.setSelection(proposalIndex);
        Event event = new Event();
        event.type = SWT.Selection;
        event.widget = table;
        event.item = table.getItem(proposalIndex);
        table.notifyListeners(SWT.Selection, event);
        table.notifyListeners(SWT.DefaultSelection, event);
      }
    });
  }

  /**
   * Attempt to apply the quick fix.
   * <p>
   * FIXME: this needs a lot of optimization.
   * </p>
   * 
   * @param proposalTable the table containing the quickfix.
   * @param proposalText the name of the quickfix to apply.
   */
  private void selectProposal(SWTBotTable proposalTable, String proposalText) {
    int index = TableItemWithText.indexOf(proposalTable, proposalText);
    if (index != -1) {
      selectProposal(proposalTable, index);
      return;
    }
    throw new QuickFixNotFoundException("Quickfix options not found. Giving up."); //$NON-NLS-1$
  }

  private Matcher<SWTBotTable> tableWithRow(final String itemText) {
    return new TableItemWithText(itemText);
  }

  private void waitUntil(WaitForObjectCondition<SWTBotTable> table) {
    bot.waitUntil(table, 10000);
  }

  private StyledText widget() {
    List<? extends Widget> findWidgets = findWidgets(widgetOfType(StyledText.class));
    return (StyledText) findWidgets.get(findWidgets.size() - 1);
  }

}

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

import com.google.dart.tools.tests.swtbot.matchers.ViewWithTitle;

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarPushButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.ui.part.PageBook;

import java.util.List;

/**
 * Model the debugger view of Dart Editor.
 */
public class DebuggerBotView extends AbstractBotView {

  public DebuggerBotView(SWTWorkbenchBot bot) {
    super(bot);
    // while the app is launching the debugger may not yet be available
    bot.waitUntilWidgetAppears(Conditions.waitForView(new ViewWithTitle(viewName())));
  }

  public void close() {
    try {
      bot.menu("Run").menu("Terminate").click();
    } catch (TimeoutException ex) {
      // If the debugger isn't running, that's fine.
    }
    debugger().close();
  }

  /**
   * Get the model for the selected stack context.
   * 
   * @return a DebuggerContextBotView
   */
  public DebuggerContextBotView contextView() {
    Tree tree = findTreeWithParent(SashForm.class);
    return new DebuggerContextBotView(bot, new SWTBotTree(tree));
  }

  /**
   * Get the local bot for the debugger view.
   * 
   * @return a SWTBotView rooted at the Debugger view
   */
  public SWTBotView debugger() {
    return bot.viewByTitle(viewName());
  }

  /**
   * Get the model for the debugger's stack
   * 
   * @return the DebuggerStackBotView
   */
  public DebuggerStackBotView stackView() {
    Tree tree = findTreeWithParent(PageBook.class);
    return new DebuggerStackBotView(bot, new SWTBotTree(tree));
  }

  /**
   * Make the debugger step into the next call. This will possibly open a new editor.
   */
  public void stepInto() {
    stepCommand("Step Into (F5)");
  }

  /**
   * Make the debugger step over the next call.
   */
  public void stepOver() {
    stepCommand("Step Over (F6)");
  }

  /**
   * Make the debugger return from the current context. This may switch editors.
   */
  public void stepReturn() {
    stepCommand("Step Return (F7)");
  }

  /**
   * Get all the widgets that are accessible from the Debugger view.
   * 
   * @return a list of widgets
   */
  public List<? extends Widget> widgets() {
    return super.widgets(debugger().getWidget());
  }

  @Override
  protected String viewName() {
    return "Debugger";
  }

  private Tree findTreeWithParent(final Class<? extends Composite> parentClass) {
    final Widget root = debugger().getWidget();
    return super.findTreeWithParent(root, parentClass);
  }

  private void stepCommand(final String tooltipForButton) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
//        debugger().toolbarPushButton(tooltipForButton).click();
        // No idea why the previous line fails. This loop does what it should do.
        List<? extends Widget> widgets = widgets();
        for (Widget widget : widgets) {
          if (widget instanceof ToolItem) {
            ToolItem item = (ToolItem) widget;
            if (tooltipForButton.equals(item.getToolTipText())) {
              new SWTBotToolbarPushButton(item).click();
              break;
            }
          }
        }
      }
    });
    waitForAnalysis();
    waitForAsyncDrain();
    waitMillis(500); // TODO None of the heuristics are working when switching editors
  }
}

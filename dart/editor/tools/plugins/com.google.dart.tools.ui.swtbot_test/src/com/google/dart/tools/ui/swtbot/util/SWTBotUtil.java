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
package com.google.dart.tools.ui.swtbot.util;

import com.google.dart.tools.ui.swtbot.matchers.EditorWithTitle;
import com.google.dart.tools.ui.swtbot.matchers.WithToolTip;
import com.google.dart.tools.ui.swtbot.performance.Performance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.WaitForEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarPushButton;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.hamcrest.Matcher;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withStyle;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SWTBotUtil {

  /**
   * Answer the currently active shell. This can be called from a non-UI thread and handles the case
   * where the SWT main event loop gets blocked by SWT dialog event loop
   */
  public static SWTBotShell activeShell(SWTBot bot) {
    final Display display = bot.getDisplay();
    final CountDownLatch latch = new CountDownLatch(1);
    // Try for up to 5 seconds
    for (int i = 0; i < 10; i++) {
      final Shell[] result = new Shell[1];
      // Queue a new runnable each time
      // because a new event processing loop is created when a dialog opens
      // thus the old runnable may never get served until after the dialog closes
      display.asyncExec(new Runnable() {

        @Override
        public void run() {
          result[0] = display.getActiveShell();
          latch.countDown();
        }
      });
      try {
        if (latch.await(500, TimeUnit.MILLISECONDS)) {
          return result[0] != null ? new SWTBotShell(result[0]) : null;
        }
      } catch (InterruptedException e) {
        //$FALL-THROUGH$
      }
    }
    throw new TimeoutException("Failed to determine active shell");
  }

  /**
   * Debugging: Echo all tool items to System.out
   */
  public static void echoToolbarButtons(SWTWorkbenchBot bot) {
    for (Widget w : toolbarPushButtons(bot)) {
      String toolTipText = new SWTBotToolbarPushButton((ToolItem) w).getToolTipText();
      System.out.println("ToolItem (push) : " + toolTipText);
    }
    for (Widget w : toolbarDropDownButtons(bot)) {
      String toolTipText = new SWTBotToolbarDropDownButton((ToolItem) w).getToolTipText();
      System.out.println("ToolItem (dropdown) : " + toolTipText);
    }
  }

  /**
   * Answer the editor with the specified title
   */
  public static SWTBotEclipseEditor editorWithTitle(SWTWorkbenchBot bot, String title) {
    // TODO (danrubel) find and editor that has title starting with the specified text
    return bot.editor(new EditorWithTitle(title)).toTextEditor();
  }

  /**
   * Print the content of the active editor
   */
  public static void printActiveEditorText(SWTWorkbenchBot bot) {
    System.out.println("====================================================");
    System.out.println(bot.activeEditor().toTextEditor().getText());
    System.out.println("====================================================");
  }

  /**
   * Answer a toolbar dropdown button that has a tooltip matching the specified regex. This differs
   * from {@link SWTBot#toolbarButtonWithTooltip(String)} which returns a toolbar dropdown button
   * that has a tooltip equal to the specified text.
   */
  @SuppressWarnings("unchecked")
  public static SWTBotToolbarDropDownButton toolbarDropDownButton(SWTWorkbenchBot bot,
      final String toolTipRegex) {
    Matcher<Widget> matcher = allOf(
        widgetOfType(ToolItem.class),
        new WithToolTip(toolTipRegex),
        withStyle(SWT.DROP_DOWN, "SWT.DROP_DOWN"));
    return new SWTBotToolbarDropDownButton((ToolItem) bot.widget(matcher, 0), matcher);
  }

  /**
   * Answer a condition that waits for an editor with a title matching the specified regex
   */
  public static WaitForEditor waitForEditorWithTitle(String titleRegex) {
    return waitForEditor(new EditorWithTitle(titleRegex));
  }

  public static void waitForMainShellToDisappear(SWTWorkbenchBot bot) {
    final SWTBotShell mainShell = bot.shell("Dart Editor");
    assertNotNull(mainShell);

    try {
      // Wait for the main shell to loose focus
      bot.waitUntil(new ICondition() {

        @Override
        public String getFailureMessage() {
          return "The Dart Editor shell failed to leave focus.";
        }

        @Override
        public void init(SWTBot bot) {
        }

        @Override
        public boolean test() throws Exception {
          return !mainShell.isActive();
        }
      }, Performance.DEFAULT_TIMEOUT_MS);
      SWTBotShell activeShell = activeShell(bot);

      // If progress dialog, then wait for it to close
      if (activeShell != null && activeShell.getText().startsWith("Launching ")) {
        bot.waitUntil(shellCloses(activeShell), Performance.DEFAULT_TIMEOUT_MS);
      }

    } finally {
    }

  }

  @SuppressWarnings("unchecked")
  private static List<? extends Widget> toolbarDropDownButtons(SWTWorkbenchBot bot) {
    return bot.widgets(allOf(
        widgetOfType(ToolItem.class),
        withStyle(SWT.DROP_DOWN, "SWT.DROP_DOWN")));
  }

  @SuppressWarnings("unchecked")
  private static List<? extends Widget> toolbarPushButtons(SWTWorkbenchBot bot) {
    return bot.widgets(allOf(widgetOfType(ToolItem.class), withStyle(SWT.PUSH, "SWT.PUSH")));
  }

}

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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.WaitForEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarPushButton;
import org.hamcrest.Matcher;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withStyle;

import java.util.List;

public class SWTBotUtil {

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
   * Answer a toolbar dropdown button that has a tooltip matching the specified regex. This differs
   * from {@link SWTBot#toolbarButtonWithTooltip(String)} which returns a toolbar dropdown button
   * that has a tooltip equal to the specified text.
   */
  @SuppressWarnings("unchecked")
  public static SWTBotToolbarDropDownButton toolbarDropDownButton(SWTWorkbenchBot bot,
      final String toolTipRegex) {
    Matcher<Widget> matcher = allOf(widgetOfType(ToolItem.class), new WithToolTip(toolTipRegex),
        withStyle(SWT.DROP_DOWN, "SWT.DROP_DOWN"));
    return new SWTBotToolbarDropDownButton((ToolItem) bot.widget(matcher, 0), matcher);
  }

  /**
   * Answer a condition that waits for an editor with a title matching the specified regex
   */
  public static WaitForEditor waitForEditorWithTitle(String titleRegex) {
    return waitForEditor(new EditorWithTitle(titleRegex));
  }

  @SuppressWarnings("unchecked")
  private static List<? extends Widget> toolbarDropDownButtons(SWTWorkbenchBot bot) {
    return bot.widgets(allOf(widgetOfType(ToolItem.class),
        withStyle(SWT.DROP_DOWN, "SWT.DROP_DOWN")));
  }

  @SuppressWarnings("unchecked")
  private static List<? extends Widget> toolbarPushButtons(SWTWorkbenchBot bot) {
    return bot.widgets(allOf(widgetOfType(ToolItem.class), withStyle(SWT.PUSH, "SWT.PUSH")));
  }
}

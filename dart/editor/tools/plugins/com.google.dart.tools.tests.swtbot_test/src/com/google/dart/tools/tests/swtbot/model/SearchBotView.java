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

import com.google.dart.tools.tests.swtbot.conditions.TreeHasSomeRows;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

/**
 * Model the Search view of Dart Editor.
 */
public class SearchBotView extends AbstractTreeBotView {

  public SearchBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * Collapse all the tree items and wait for the tree to finish updating.
   */
  public void collapseAll() {
    final SWTBotView view = bot.viewByPartName(viewName());
    view.show();
    view.setFocus();
    SWTBotTree tree = view.bot().tree();
    view.bot().waitUntil(new TreeHasSomeRows(tree, 1)); // TODO understand flakiness here, see TestAll
    SWTBotToolbarButton collapseButton = getToolbarButton(view, "Collapse All");
    collapseButton.click();
    waitForTreeContent(tree);
  }

  /**
   * Expand all the tree items and wait for the tree to finish updating.
   */
  public void expandAll() {
    SWTBotView view = bot.viewByPartName(viewName());
    view.show();
    view.setFocus();
    SWTBotTree tree = view.bot().tree();
    view.bot().waitUntil(new TreeHasSomeRows(tree, 1));
    SWTBotToolbarButton expandButton = getToolbarButton(view, "Expand All");
    expandButton.click();
    waitForTreeContent(tree);
  }

  /**
   * Return true if the Search view is empty.
   * 
   * @return true if there are no unexpected problems
   */
  public boolean isEmpty() {
    SWTBotView view = bot.viewByPartName(viewName());
    view.show();
    view.setFocus();
    SWTBotTree tree = view.bot().tree();
    int count = tree.rowCount();
    return count == 0;
  }

  @Override
  protected String viewName() {
    return "Search";
  }

  /**
   * Toolbars are extremely difficult to manage from SWTBot. The buttons all end up in the same
   * composite, regardless of toolbar. So, first find a button unique to Search, then from its
   * parent search for the button we really want.
   * 
   * @param view the Search view
   * @param text the tooltip text of the desired button
   * @return the toolbar button with the given tooltip text
   */
  private SWTBotToolbarButton getToolbarButton(SWTBotView view, String text) {
    SWTBot parent = getParentBot(view.getWidget());
    SWTBotToolbarButton unique = parent.toolbarButtonWithTooltip("Refresh the Current Search");
    return getParentBot(unique.widget).toolbarButtonWithTooltip(text);
  }
}

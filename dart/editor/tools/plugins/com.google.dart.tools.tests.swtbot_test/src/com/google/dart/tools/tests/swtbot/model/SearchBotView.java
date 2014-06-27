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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
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
    clickToolbarButton("Collapse All");
  }

  /**
   * Expand all the tree items and wait for the tree to finish updating.
   */
  public void expandAll() {
    clickToolbarButton("Expand All");
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

  /**
   * Perform the search again and wait for the tree to finish updating.
   */
  public void refreshSearch() {
    clickToolbarButton("Refresh the Current Search");
  }

  /**
   * Remove the all matches from the tree, which disposes all UI elements, so don't wait.
   */
  public void removeAll() {
    SWTBotView view = bot.viewByPartName(viewName());
    view.show();
    view.setFocus();
    SWTBotTree tree = view.bot().tree();
    view.bot().waitUntil(new TreeHasSomeRows(tree, 1));
    SWTBotToolbarButton expandButton = getToolbarButton(view, "Remove All Matches");
    expandButton.click();
  }

  /**
   * Remove the selected match from the tree and wait for the tree to finish updating.
   */
  public void removeSelected() {
    clickToolbarButton("Remove Selected Matches");
  }

  /**
   * Navigate to the next match and wait for the tree to finish updating.
   */
  public void showNext() {
    clickToolbarButton("Show Next Match");
  }

  /**
   * Navigate to the previous match and wait for the tree to finish updating.
   */
  public void showPrevious() {
    clickToolbarButton("Show Previous Match");
  }

  /**
   * Remove potential matches from the tree and wait for the tree to finish updating.
   */
  public void toggleFilterOutPotential() {
    clickToolbarToggleButton("Hide potential matches");
  }

  /**
   * Remove SDK and packages matches from the tree and wait for the tree to finish updating.
   */
  public void toggleFilterOutSdk() {
    clickToolbarToggleButton("Hide SDK and package matches");
  }

  /**
   * Show only matches from the current project and wait for the tree to finish updating.
   */
  public void toggleFilterToProject() {
    clickToolbarToggleButton("Show only current project actions");
  }

  @Override
  protected String viewName() {
    return "Search";
  }

  private void clickToolbarButton(String mnemonic) {
    SWTBotView view = bot.viewByPartName(viewName());
    view.show();
    view.setFocus();
    SWTBotTree tree = view.bot().tree();
    view.bot().waitUntil(new TreeHasSomeRows(tree, 1));
    SWTBotToolbarButton expandButton = getToolbarButton(view, mnemonic);
    expandButton.click();
    waitForAnalysis();
    waitForTreeContent(tree);
  }

  private void clickToolbarToggleButton(String mnemonic) {
    SWTBotView view = bot.viewByPartName(viewName());
    view.show();
    view.setFocus();
    SWTBotTree tree = view.bot().tree();
    view.bot().waitUntil(new TreeHasSomeRows(tree, 1));
    SWTBotToolbarButton expandButton = getToolbarToggleButton(view, mnemonic);
    expandButton.click();
    waitForAnalysis();
    waitForTreeContent(tree);
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

  private SWTBotToolbarToggleButton getToolbarToggleButton(SWTBotView view, String text) {
    SWTBot parent = getParentBot(view.getWidget());
    SWTBotToolbarButton unique = parent.toolbarButtonWithTooltip("Refresh the Current Search");
    return getParentBot(unique.widget).toolbarToggleButtonWithTooltip(text);
  }
}

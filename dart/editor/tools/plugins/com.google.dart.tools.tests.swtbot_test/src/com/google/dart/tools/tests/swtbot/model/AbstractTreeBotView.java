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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

abstract public class AbstractTreeBotView extends AbstractBotView {

  public AbstractTreeBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * Select the item at the end of the given tree path.
   * 
   * @param items the names of tree nodes to expand
   * @return the selected item
   */
  public SWTBotTreeItem select(String... items) {
    assertTrue(items.length > 0);
    SWTBotView files = bot.viewByPartName(viewName());
    SWTBot bot = files.bot();
    SWTBotTree tree = bot.tree();
    waitForAnalysis();
    SWTBotTreeItem item;
    try {
      item = tree.expandNode(items);
    } catch (Exception ex) {
      // TODO[messick] Remove after dartbug/19563 (danrubel) is fixed
      String last = items[items.length - 1];
      int index = last.indexOf('[');
      if (index > 0) {
        last = last.substring(0, index - 1);
        String[] next = new String[items.length];
        System.arraycopy(items, 0, next, 0, items.length - 1);
        next[next.length - 1] = last;
        item = tree.expandNode(next);
        items = next;
      } else {
        fail();
        throw new RuntimeException(ex);
      }
    }
    tree.select(item);
    waitForAnalysis();
    TableCollection selection = tree.selection();
    assertNotNull(selection);
    assertEquals(selection.rowCount(), 1);
    assertEquals(items[items.length - 1], selection.get(0, 0));
    return item;
  }

  /**
   * Get the current tree selection. If the table is empty it is assumed that it has not been
   * updated yet, so loop until it is. DO NOT use this method on a table that is empty!
   * 
   * @return the tree selection
   */
  public TableCollection selection() {
    TableCollection selection = null;
    while (selection == null || selection.rowCount() == 0 || selection.columnCount() == 0) {
      SWTBotView files = bot.viewByPartName(viewName());
      SWTBot bot = files.bot();
      SWTBotTree tree = bot.tree();
      selection = tree.selection();
    }
    return selection;
  }

  /**
   * Change the selection to nothing.
   * 
   * @return the tree item
   */
  public SWTBotTree unselectAll() {
    SWTBotView files = bot.viewByPartName(viewName());
    SWTBot bot = files.bot();
    SWTBotTree tree = bot.tree();
    tree.unselect();
    return tree;
  }

}

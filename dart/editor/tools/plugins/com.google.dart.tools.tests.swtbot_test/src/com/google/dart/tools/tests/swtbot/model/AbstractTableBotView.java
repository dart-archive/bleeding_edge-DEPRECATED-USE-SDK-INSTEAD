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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

abstract public class AbstractTableBotView extends AbstractBotView {

  public AbstractTableBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  public int columnCount() {
    return table().columnCount();
  }

  /**
   * Select the items in the given collection.
   * 
   * @param items the names of table cells to select
   * @return the selected items
   */
  public TableCollection select(String... items) {
    assertTrue(items.length > 0);
    SWTBotTable table = table();
    waitForAnalysis();
    waitForTableContent(table);
    table.select(items);
    waitForAnalysis();
    TableCollection selection = table.selection();
    assertNotNull(selection);
    return selection;
  }

  /**
   * Get the current table selection. If the table is empty it is assumed that it has not been
   * updated yet, so loop until it is. DO NOT use this method on a table that is empty!
   * 
   * @return the table selection
   */
  public TableCollection selection() {
    SWTBotTable table = table();
    waitForTableContent(table);
    TableCollection selection = table.selection();
    return selection;
  }

  /**
   * Get the SWTBotTable for this view.
   * 
   * @return the SWTBotTable
   */
  public SWTBotTable table() {
    SWTBotView parent = bot.viewByPartName(viewName());
    SWTBot bot = parent.bot();
    SWTBotTable table = bot.table();
    return table;
  }

  /**
   * Get the current table size. If the table is empty it is assumed that it has not been updated
   * yet, so loop until it is. DO NOT use this method on a table that is empty!
   * 
   * @return the table selection
   */
  public int tableSize() {
    SWTBotTable table = table();
    waitForTableContent(table);
    int size = table.rowCount();
    return size;
  }

  /**
   * Perform one of the widget traversal operations specified by the given SWT constant.
   * 
   * @param traversal the SWT constant that identifies the traversal, eg SWT.TRAVERSE_ESCAPE
   */
  public void traverse(final int traversal) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        table().widget.traverse(traversal);
      }
    });
  }

  /**
   * Change the selection to nothing.
   * 
   * @return the table item
   */
  public SWTBotTable unselectAll() {
    SWTBotTable table = table();
    table.unselect();
    return table;
  }

  /**
   * Give the <code>botTable</code> time to update, and return when it is finished.
   * 
   * @param botTable a SWTBotTable
   */
  protected void waitForTableContent(SWTBotTable botTable) {
    while (isTableEmpty(botTable) || isTableBusy(botTable)) {
      waitMillis(1);
    }
  }

  /**
   * Viewers are difficult to work with using SWTBot. The only way to know when a table has finished
   * updating is to check <code>isBusy()</code> but there's no easy way to get to the viewer from a
   * table. This technique uses some black magic to determine if the table is busy.
   * 
   * @param botTable the TableViewer bot to query
   * @return true if the table is busy
   */
  private boolean isTableBusy(final SWTBotTable botTable) {
    return UIThreadRunnable.syncExec(new BoolResult() {
      @Override
      public Boolean run() {
        Table table = botTable.widget;
        ViewerColumn col = (ViewerColumn) table.getData("org.eclipse.jface.columnViewer");
        if (col == null) {
          return false;
        }
        TreeViewer treeViewer = (TreeViewer) col.getViewer();
        return treeViewer.isBusy();
      }
    });
  }

  private boolean isTableEmpty(final SWTBotTable botTable) {
    return botTable.rowCount() == 0;
  }
}

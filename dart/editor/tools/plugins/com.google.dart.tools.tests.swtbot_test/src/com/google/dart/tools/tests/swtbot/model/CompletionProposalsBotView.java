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

import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.utils.TableRow;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

public class CompletionProposalsBotView extends AbstractTableBotView {

  private SWTBotTable proposals;

  public CompletionProposalsBotView(SWTWorkbenchBot bot, SWTBotTable proposals) {
    super(bot);
    this.proposals = proposals;
  }

  @Override
  public int columnCount() {
    return 1; // the completion proposal table has columnCount==0 !
  }

  public TableCollection select(final int x) {
    return UIThreadRunnable.syncExec(new Result<TableCollection>() {
      @Override
      public TableCollection run() {
        proposals.widget.select(x);
        final int columnCount = columnCount();
        final TableCollection selection = new TableCollection();
        TableItem[] items = proposals.widget.getSelection();
        for (TableItem item : items) {
          TableRow tableRow = new TableRow();
          for (int j = 0; j < columnCount; j++) {
            tableRow.add(item.getText(j));
          }
          selection.add(tableRow);
        }
        return selection;
      }
    });
  }

  /**
   * Get the SWTBotTable for this view.
   * 
   * @return the SWTBotTable
   */
  @Override
  public SWTBotTable table() {
    return proposals;
  }

  @Override
  protected String viewName() {
    return "Completion Proposals";
  }
}

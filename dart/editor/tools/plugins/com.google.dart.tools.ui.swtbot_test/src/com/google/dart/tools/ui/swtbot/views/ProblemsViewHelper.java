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
package com.google.dart.tools.ui.swtbot.views;

import com.google.dart.tools.ui.test.model.Workbench;

import static com.google.dart.tools.core.utilities.general.FormattedStringBuilder.appendText;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.fail;

/**
 * Access methods for the "Problems" view
 */
public class ProblemsViewHelper {

  private SWTBot bot;

  private SWTBotTable table;

  public ProblemsViewHelper(SWTBot bot) {
    this.bot = bot;
    //TODO(pquitslund): push into model view
    SWTBotView view = ((SWTWorkbenchBot) bot).viewByTitle(Workbench.View.PROBLEMS.getName());
    Composite composite = (Composite) view.getWidget();
    Table problemsTable = bot.widget(widgetOfType(Table.class), composite);
    table = new SWTBotTable(problemsTable);
  }

  public void assertNoProblems() {
    // TODO (jwren) before this assertion is made, we have to wait for the Problems view has enough
    // time to populate itself, figure out a way to write a condition instead of a sleep call.
    bot.sleep(1000);

    int count = getProblemCount();
    if (count == 0) {
      return;
    }
    System.out.println("Problems:");
    for (int i = 0; i < count; i++) {
      SWTBotTableItem row = table.getTableItem(i);
      StringBuilder line = new StringBuilder(100);
      appendText(line, row.getText(0), 50);
      line.append(row.getText(1));
      System.out.println(line);
      if (i == 40) {
        System.out.println(" ... and " + (count - i) + " more problems");
        break;
      }
    }
    fail("Expected 0 problems, but found " + count);
  }

  public int getProblemCount() {
    return table.rowCount();
  }

}

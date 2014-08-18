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

import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.hamcrest.Matcher;

import java.util.List;

public class ExtractMethodBotView extends AbstractBotView {

  public ExtractMethodBotView(SWTWorkbenchBot bot) {
    super(bot);
    bot.waitUntilWidgetAppears(Conditions.shellIsActive(viewName()));
  }

  public void close() {
    extractMethodShell().button("OK").click();
    waitForAnalysis();
  }

  public void editParam(int index, final String type, final String name) {
    SWTBotTable table = extractMethodComposite().table();
    final SWTBotTableItem item = table.getTableItem(index);
    if (type != null) {
      UIThreadRunnable.syncExec(new VoidResult() {
        @Override
        public void run() {
          item.widget.setText(0, type);
        }
      });
    }
    if (name != null) {
      UIThreadRunnable.syncExec(new VoidResult() {
        @Override
        public void run() {
          item.widget.setText(1, name);
        }
      });
    }
  }

  public SWTBotText methodNameField() {
    return extractMethodComposite().text();
  }

  public void moveDown() {
    extractMethodComposite().button("Down").click();
  }

  /**
   * Get all the widgets that are accessible from the Workbench view.
   * 
   * @return a list of widgets
   */
  public List<? extends Widget> widgets() {
    final Matcher<Widget> matcher = WidgetOfType.widgetOfType(Widget.class);
    return UIThreadRunnable.syncExec(new Result<List<? extends Widget>>() {
      @Override
      public List<? extends Widget> run() {
        return bot.widgets(matcher, bot.label("Method name:").widget.getParent());
      }
    });
  }

  @Override
  protected String viewName() {
    return "Extract Method";
  }

  private SWTBot extractMethodComposite() {
    return UIThreadRunnable.syncExec(new Result<SWTBot>() {
      @Override
      public SWTBot run() {
        return new SWTBot(bot.label("Method name:").widget.getParent());
      }
    });
  }

  private SWTBot extractMethodShell() {
    return UIThreadRunnable.syncExec(new Result<SWTBot>() {
      @Override
      public SWTBot run() {
        return new SWTBot(bot.label("Method name:").widget.getParent().getParent().getParent());
      }
    });
  }
}

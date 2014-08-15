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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.hamcrest.Matcher;

import java.util.List;

public class ExtractMethodBotView extends AbstractBotView {

  public ExtractMethodBotView(SWTWorkbenchBot bot) {
    super(bot);
//    bot.waitUntilWidgetAppears(Conditions.waitForView(new ViewWithTitle(viewName())));
  }

  public SWTBotText methodNameField() {
    return wizard().bot().text();
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
        return bot.widgets(matcher);
      }
    });
  }

  @Override
  protected String viewName() {
    return "Extract Method";
  }

  private SWTBotView wizard() {
    waitMillis(500);
    // TODO(messick) This is broken! Fix it next.
    return bot.viewByTitle(viewName());
  }
}

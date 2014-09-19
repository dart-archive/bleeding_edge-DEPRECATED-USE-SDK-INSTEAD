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

import com.google.dart.tools.tests.swtbot.matchers.ViewWithTitle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.hamcrest.Matcher;

import static org.junit.Assert.fail;

public class PackagesBotView extends AbstractTableBotView {

  final static private String WORKING = "Populating data ...";

  public PackagesBotView(SWTWorkbenchBot bot) {
    super(bot);
    bot.waitUntilWidgetAppears(Conditions.waitForView(new ViewWithTitle(viewName())));
    if (tableSize() == 0) {
      fail();
    }
    while (true) {
      TableCollection sel = select(0);
      if (!WORKING.equals(sel.get(0, 0))) {
        return;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        return; // We don't care, really.
      }
    }
  }

  public void filter(String string) {
    final SWTBotLabel label = bot.label("Search by name or description: ");
    final Matcher<Text> matcher = WidgetOfType.widgetOfType(Text.class);
    final SWTBotText text = UIThreadRunnable.syncExec(new Result<SWTBotText>() {
      @Override
      public SWTBotText run() {
        Composite comp = label.widget.getParent();
        return new SWTBotText(bot.widget(matcher, comp));
      }
    });
    text.setText(string);
    notify(SWT.KeyDown, createKeyEvent('\n'), text.widget);
    notify(SWT.KeyUp, createKeyEvent('\n'), text.widget);
  }

  @Override
  protected String viewName() {
    return "Packages";
  }

}

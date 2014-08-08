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

import com.xored.glance.internal.ui.panels.SearchPanelManager;
import com.xored.glance.ui.panels.ISearchPanel;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.ControlFinder;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.hamcrest.Matcher;

import java.util.List;

// TODO: Wrap the ISearchPanel API here. Everything needs to be run in the UI thread.
// See findText() for example usage (which, amazingly, actually works already).
public class FindTextBotView extends AbstractBotView {

  public FindTextBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  public void findText(final String text) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        getCombo().setText(text);
      }
    });
  }

  @Override
  protected String viewName() {
    return "FindText";
  }

  private Combo getCombo() {
    Composite parent = (Composite) getPanel().getControl();
    Matcher<Combo> matcher = WidgetOfType.widgetOfType(Combo.class);
    List<Combo> list = new ControlFinder().findControls(parent, matcher, true);
    if (list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }

  private ISearchPanel getPanel() {
    return SearchPanelManager.getInstance().getPanel(bot.getFocusedWidget());
  }

}

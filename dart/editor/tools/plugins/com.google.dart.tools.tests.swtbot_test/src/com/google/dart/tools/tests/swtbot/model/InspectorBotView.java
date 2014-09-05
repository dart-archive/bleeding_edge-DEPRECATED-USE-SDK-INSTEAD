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

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.hamcrest.Matcher;

import java.util.List;

public class InspectorBotView extends AbstractBotView {

  public InspectorBotView(SWTWorkbenchBot bot) {
    super(bot);
    // while the app is launching the inspector may not yet be available
    bot.waitUntilWidgetAppears(Conditions.waitForView(new ViewWithTitle(viewName())));
  }

  /**
   * Close the Inspector view.
   */
  public void close() {
    inspector().close();
  }

  /**
   * Get a model for the expression evaluation view.
   * 
   * @return an InspectorExpressionBotView
   */
  public InspectorExpressionBotView expressionView() {
    final SWTBotLabel label = bot.label("Enter expression to evaluate:");
    final Matcher<StyledText> matcher = WidgetOfType.widgetOfType(StyledText.class);
    final StyledText text = UIThreadRunnable.syncExec(new Result<StyledText>() {
      @Override
      public StyledText run() {
        Composite comp = label.widget.getParent();
        return bot.widget(matcher, comp);
      }
    });
    return new InspectorExpressionBotView(bot, new SWTBotStyledText(text));
  }

  /**
   * Get the local bot for the inspector view.
   * 
   * @return a SWTBotView rooted at the Inspector view
   */
  public SWTBotView inspector() {
    return bot.viewByTitle(viewName());
  }

  /**
   * Get the model for the inspected object.
   * 
   * @return an InspectorObjectBotView
   */
  public InspectorObjectBotView instanceView() {
    Tree tree = findTreeWithParent(SashForm.class);
    return new InspectorObjectBotView(bot, new SWTBotTree(tree));
  }

  /**
   * Get all the widgets that are accessible from the Inspector view.
   * 
   * @return a list of widgets
   */
  public List<? extends Widget> widgets() {
    return super.widgets(inspector().getWidget());
  }

  @Override
  protected String viewName() {
    return "Inspector";
  }

  private Tree findTreeWithParent(final Class<? extends Composite> parentClass) {
    final Widget root = inspector().getWidget();
    return super.findTreeWithParent(root, parentClass);
  }

}

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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

/**
 * Model the Problems view of Dart Editor.
 */
public class ProblemsBotView extends AbstractBotView {

  public ProblemsBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * Return true if the Problems is empty, or if it has no more that <code>args[0]</code> items.
   * 
   * @param args optional number of problems to ignore
   * @return true if there are no unexpected problems
   */
  public boolean isEmpty(int... args) {
    SWTBotView view = bot.viewByPartName("Problems");
    view.show();
    view.setFocus();
    SWTBotTable tree = view.bot().table();
    boolean result;
    if (args.length > 0) {
      result = tree.rowCount() <= args[0];
    } else {
      result = tree.rowCount() == 0;
    }
    return result;
  }

  @Override
  protected String viewName() {
    return "Problems";
  }
}

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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

/**
 * Model Dart Editor's debugger stack view.
 */
public class DebuggerStackBotView extends AbstractTreeBotView {

  private SWTBotTree treeBot;

  public DebuggerStackBotView(SWTWorkbenchBot bot, SWTBotTree treeBot) {
    super(bot);
    this.treeBot = treeBot;
  }

  /**
   * Get the SWTBotTree for this view.
   * 
   * @return the SWTBotTree
   */
  @Override
  public SWTBotTree tree() {
    return treeBot;
  }

  @Override
  protected String viewName() {
    return "Debugger";
  }

}

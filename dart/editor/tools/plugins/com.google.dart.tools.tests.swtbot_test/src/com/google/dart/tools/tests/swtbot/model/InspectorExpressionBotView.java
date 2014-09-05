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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;

public class InspectorExpressionBotView extends AbstractBotView {

  SWTBotStyledText text;

  public InspectorExpressionBotView(SWTWorkbenchBot bot, SWTBotStyledText text) {
    super(bot);
    this.text = text;
  }

  /**
   * Fetch the content of the text view.
   * 
   * @return the text
   */
  public String content() {
    return text.getText();
  }

  /**
   * Evaluate the expression.
   */
  public void enter() {
    text.typeText("\n");
  }

  /**
   * Ensure the text view has focus.
   */
  public void focus() {
    text.setFocus();
  }

  /**
   * Type an expression into the text view.
   * 
   * @param expr the expression
   */
  public void type(String expr) {
    text.typeText(expr);
  }

  @Override
  protected String viewName() {
    return "Inspector Expression";
  }
}

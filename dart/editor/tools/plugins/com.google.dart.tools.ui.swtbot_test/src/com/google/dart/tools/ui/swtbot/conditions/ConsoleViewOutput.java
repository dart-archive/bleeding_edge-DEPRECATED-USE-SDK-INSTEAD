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
package com.google.dart.tools.ui.swtbot.conditions;

import com.google.dart.tools.ui.swtbot.views.ConsoleViewHelper;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

/**
 * Condition that waits for a specified number of problems to appear in the problems view
 */
public class ConsoleViewOutput implements ICondition {
  private final String expectedConsoleOutput;
  private ConsoleViewHelper helper;

  public ConsoleViewOutput(String expectedConsoleOutput) {
    this.expectedConsoleOutput = expectedConsoleOutput;
  }

  @Override
  public String getFailureMessage() {
    return "Gave up waiting for problems: expected=" + expectedConsoleOutput + " actual="
        + helper.getConsoleOutput();
  }

  @Override
  public void init(SWTBot bot) {
    helper = new ConsoleViewHelper(bot);
  }

  @Override
  public boolean test() throws Exception {
    return helper.getConsoleOutput().matches(expectedConsoleOutput);
  }
}

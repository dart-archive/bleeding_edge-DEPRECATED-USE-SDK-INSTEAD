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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Access methods for the "Console" view.
 */
public class ConsoleViewHelper {

  private SWTBotStyledText text;

  public ConsoleViewHelper(SWTBot bot) {
    //TODO(pquitslund): push into model view
    SWTBotView view = ((SWTWorkbenchBot) bot).viewByTitle(Workbench.View.CONSOLE.getName());
    view.show();
    assertTrue(view.isActive());
    Composite composite = (Composite) view.getWidget();
    StyledText problemsText = bot.widget(widgetOfType(StyledText.class), composite);
    text = new SWTBotStyledText(problemsText);
  }

  public void assertConsoleEquals(String consoleOutput) {
    if (!getConsoleOutput().equals(consoleOutput)) {
      fail("Expected \"" + consoleOutput + "\", but found \"" + getConsoleOutput() + "\"");
    }
  }

  public void assertConsoleMatches(String consoleOutputRegex) {
    if (!getConsoleOutput().matches(consoleOutputRegex)) {
      fail("Expected \"" + consoleOutputRegex + "\", but found \"" + getConsoleOutput() + "\"");
    }
  }

  public void assertNoConsoleLog() {
    if (!getConsoleOutput().isEmpty()) {
      fail("Expected an empty log, but found \"" + getConsoleOutput() + "\"");
    }
  }

  public String getConsoleOutput() {
    return text.getText().trim();
  }

}

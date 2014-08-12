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

import com.google.dart.tools.tests.swtbot.matchers.EditorWithTitle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.ui.forms.widgets.Section;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.List;

/**
 * Model the Welcome page of Dart Editor with methods to create samples.
 */
public class WelcomePageEditor extends AbstractBotView {

  /**
   * The samples UI does not match the expectations of SWTBot very well. Use structural matching to
   * find the label that has an event handler which can be use to create a sample.
   */
  static class SampleMatcher extends BaseMatcher<Widget> {

    private String sectionTitle;
    private int index;

    SampleMatcher(String title, int index) {
      // without rewriting the samples page there is no good way to find the buttons
      this.sectionTitle = title;
      this.index = index;
    }

    @Override
    public void describeTo(Description description) {
      StringBuffer text = new StringBuffer();
      text.append("The index ");
      text.append(index);
      text.append(" image label in section '");
      text.append(sectionTitle);
      text.append("' of Samples");
      description.appendText(text.toString());
    }

    @Override
    public boolean matches(Object item) {
      if (!(item instanceof Label)) {
        return false;
      }
      Label label = (Label) item;
      Composite parent = label.getParent();
      if (!(parent instanceof Composite)) {
        return false;
      }
      parent = parent.getParent();
      if (!(parent instanceof Composite)) {
        return false;
      }
      Composite sectionComp = parent.getParent();
      if (!(sectionComp instanceof Section)) {
        return false;
      }
      Section section = (Section) sectionComp;
      String title = section.getText();
      if (!sectionTitle.equals(title)) {
        return false;
      }
      Control[] children = section.getChildren();
      if (children.length != 2) {
        return false;
      }
      if (!(children[1] instanceof Composite)) {
        return false;
      }
      children = ((Composite) children[1]).getChildren();
      if (children.length <= index) {
        return false;
      }
      return children[index] == label.getParent();
    }

  }

  public WelcomePageEditor(SWTWorkbenchBot bot) {
    super(bot);
    // when the editor is starting up the welcome page may not be immediately available
    bot.waitUntilWidgetAppears(Conditions.waitForEditor(new EditorWithTitle(viewName())));
  }

  /**
   * Create the angular sample.
   */
  public void createAngularTodo() {
    clickAngularTodo();
    waitForProjectToLoad();
  }

  /**
   * Create the polymer sample.
   */
  public void createPolymerTodo() {
    clickPolymerTodo();
    waitForProjectToLoad();
  }

  /**
   * Create the game sample.
   */
  public void createPopPopWin() {
    clickPopPopWin();
    waitForProjectToLoad();
  }

  /**
   * Create the sunflower sample.
   */
  public void createSunflower() {
    clickSunflower();
    waitForProjectToLoad();
  }

  @Override
  protected String viewName() {
    return "Welcome";
  }

  /**
   * Begin creating the angular sample.
   */
  void clickAngularTodo() {
    clickLabel("Early Access", 1);
  }

  /**
   * Begin creating the polymer sample.
   */
  void clickPolymerTodo() {
    clickLabel("Early Access", 0);
  }

  /**
   * Begin creating the game sample.
   */
  void clickPopPopWin() {
    clickLabel("Demos of Dart", 1);
  }

  /**
   * Begin creating the sunflower sample. Only for use by TestSamples.
   */
  void clickSunflower() {
    clickLabel("Demos of Dart", 0);
  }

  private void clickLabel(final String sectionTitle, final int index) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        SWTBotEditor ed = bot.editorByTitle("Welcome");
        ed.setFocus();
        SWTBot edBot = ed.bot();
        List<?> list = edBot.widgets(new SampleMatcher(sectionTitle, index));
        Label label = (Label) list.get(0);
        Listener[] listeners = label.getListeners(SWT.MouseUp);
        Event e = new Event();
        e.widget = label;
        e.type = SWT.MouseUp;
        e.button = 1;
        e.display = label.getDisplay();
        listeners[0].handleEvent(e);
      }
    });
  }
}

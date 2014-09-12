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

import com.google.dart.tools.debug.ui.internal.util.LaunchTargetComposite;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.hamcrest.Matcher;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withStyle;

@SuppressWarnings("restriction")
public abstract class LaunchBrowserBasedBotView extends AbstractBotView {

  protected SWTBot shellBot;

  public LaunchBrowserBasedBotView(SWTWorkbenchBot bot, SWTBot shellBot) {
    super(bot);
    this.shellBot = shellBot;
  }

  /**
   * Select the HTML button and set the path to the HTML file.
   * 
   * @param path the path to the HTML file
   */
  public void htmlFile(String path) {
    deselectDefaultSelection(1);
    htmlRadioButton().click();
    htmlTextField().setText(path);
  }

  /**
   * Select the URL button and set the URL and source path.
   * 
   * @param path URL path
   * @param source source path
   */
  public void url(final String path, final String source) {
    deselectDefaultSelection(0);
    urlRadioButton().click();
    urlTextField().setText(path);
    sourceTextField().setText(source);
  }

  /**
   * Find the LaunchTargetComposite, which provides easy access to multiple, un-named text fields.
   * 
   * @return the LaunchTargetComposite
   */
  protected LaunchTargetComposite launchTargetComposite() {
    final SWTBotRadio htmlRadioButton = htmlRadioButton();
    return UIThreadRunnable.syncExec(new Result<LaunchTargetComposite>() {
      @Override
      public LaunchTargetComposite run() {
        Composite comp = htmlRadioButton.widget.getParent().getParent();
        return (LaunchTargetComposite) comp;
      }
    });
  }

  /**
   * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=344484
   * 
   * @param currSelection The index of the radiobutton to deselect
   */
  void deselectDefaultSelection(final int currSelection) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        @SuppressWarnings("unchecked")
        Matcher<Widget> matcher = allOf(widgetOfType(Button.class),
            withStyle(SWT.RADIO, "SWT.RADIO"));
        Button b = (Button) bot.widget(matcher, currSelection); // the current selection
        b.setSelection(false);
      }
    });
  }

  private SWTBotRadio htmlRadioButton() {
    return shellBot.radio("HTML file:");
  }

  private SWTBotText htmlTextField() {
    LaunchTargetComposite launchTargetComposite = launchTargetComposite();
    Text text = ReflectionUtils.getFieldObject(launchTargetComposite, "htmlText");
    return new SWTBotText(text);
  }

  private SWTBotText sourceTextField() {
    LaunchTargetComposite launchTargetComposite = launchTargetComposite();
    Text text = ReflectionUtils.getFieldObject(launchTargetComposite, "sourceDirectoryText");
    return new SWTBotText(text);
  }

  private SWTBotRadio urlRadioButton() {
    return shellBot.radio("URL:");
  }

  private SWTBotText urlTextField() {
    LaunchTargetComposite launchTargetComposite = launchTargetComposite();
    Text text = ReflectionUtils.getFieldObject(launchTargetComposite, "urlText");
    return new SWTBotText(text);
  }
}

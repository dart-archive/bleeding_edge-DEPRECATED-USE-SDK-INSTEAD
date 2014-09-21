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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.forms.finder.SWTFormsBot;
import org.eclipse.swtbot.forms.finder.widgets.SWTBotHyperlink;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLink;
import org.hamcrest.Matcher;

import java.util.List;

public class PubspecBotEditor extends AbstractBotView {

  private static final String SWITCH_TEXT = "Packages with advanced requirements e.g. transformers, currently \n"
      + "require manual editing. <a href=\"" + "source" + "\">Switch to yaml editing mode</a>";

  public PubspecBotEditor(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * Return the SWTBotEclipseEditor for this editor pane.
   * 
   * @return the SWTBotEclipseEditor
   */
  public SWTFormsBot editor() {
    SWTBotEditor ed = bot.editorByTitle(viewName());
    return new SWTFormsBot(ed.bot().getFinder());
  }

  /**
   * Get a model for the Packages view.
   * 
   * @return the Packages view bot
   */
  public PackagesBotView packagesView() {
    clickPackagesLink();
    return new PackagesBotView(bot);
  }

  public void pubBuild() {
    clickActionLink("Run pub build");
    waitMillis(20000); // No way to tell pub is finished
  }

  public void pubGet() {
    clickActionLink("Run pub get");
    waitMillis(10000); // No way to tell pub is finished
  }

  public TextBotEditor switchToYamlEditor() {
    SWTFormsBot pubedit = editor();
    SWTBotLink link = pubedit.link(SWITCH_TEXT);
    link.click("Switch to yaml editing mode");
    return new TextBotEditor(bot, "pubspec.yaml");
  }

  /**
   * Get all the widgets that are accessible from the editor.
   * 
   * @return a list of widgets
   */
  public List<? extends Widget> widgets() {
    final Matcher<Widget> matcher = WidgetOfType.widgetOfType(Widget.class);
    return UIThreadRunnable.syncExec(new Result<List<? extends Widget>>() {
      @Override
      public List<? extends Widget> run() {
        return editor().widgets(matcher);
      }
    });
  }

  @Override
  protected String viewName() {
    return "pubspec.yaml";
  }

  private void clickActionLink(String linkText) {
    SWTFormsBot pubedit = editor();
    SWTBotHyperlink link = pubedit.hyperlink(linkText);
    link.setFocus();
    Rectangle bounds = getBounds(link.widget);
    int x = bounds.width / 2;
    int y = bounds.height / 2;
    notify(SWT.MouseDown, createMouseEvent(x, y, 1, 0, 1), link.widget);
    notify(SWT.MouseUp, createMouseEvent(x, y, 1, 0, 1), link.widget);
  }

  private void clickPackagesLink() {
    clickActionLink("Show packages on pub.dartlang.org");
  }
}

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
package com.google.dart.tools.ui.swtbot.dialog;

import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.matchers.EditorWithTitle;
import com.google.dart.tools.ui.swtbot.performance.SwtBotPerformance;
import com.google.dart.tools.ui.swtbot.util.SWTBotUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBotControl;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withMnemonic;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withStyle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

public class NewApplicationHelper {

  public enum ContentType {
    WEB,
    SERVER
  }

  //TODO (pquitslund): move this to a common factory class
  private static class SWTBotCheckButton extends AbstractSWTBotControl<Button> {

    public SWTBotCheckButton(Button w, SelfDescribing description) throws WidgetNotFoundException {
      super(w, description);
    }

    public void ensureSelected(boolean selected) {
      if (getSelection() != selected) {
        click();
      }
    }

    public boolean getSelection() {
      return syncExec(new BoolResult() {
        @Override
        public Boolean run() {
          return widget.getSelection();
        }
      });
    }

    @Override
    protected AbstractSWTBot<Button> click() {
      //cribbed from SWTButton
      log.debug(MessageFormat.format("Clicking on {0}", SWTUtils.getText(widget))); //$NON-NLS-1$
      waitForEnabled();
      notify(SWT.MouseEnter);
      notify(SWT.MouseMove);
      notify(SWT.Activate);
      notify(SWT.FocusIn);
      notify(SWT.MouseDown);
      notify(SWT.MouseUp);
      notify(SWT.Selection);
      notify(SWT.MouseHover);
      notify(SWT.MouseMove);
      notify(SWT.MouseExit);
      notify(SWT.Deactivate);
      notify(SWT.FocusOut);
      log.debug(MessageFormat.format("Clicked on {0}", SWTUtils.getText(widget))); //$NON-NLS-1$
      return this;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static SWTBotCheckButton check(SWTBot bot, String mnemonicText) {
    Matcher matcher = allOf(
        widgetOfType(Button.class),
        withMnemonic(mnemonicText),
        withStyle(SWT.CHECK, "SWT.CHECK"));
    return new SWTBotCheckButton((Button) bot.widget(matcher, 0), matcher);
  }

  private final SWTWorkbenchBot bot;

  public NewApplicationHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  /**
   * Drive the "New Application..." dialog to create a new application with the specified name
   * 
   * @param appName the application name
   * @return the new application
   * @throws CoreException
   */
  public DartLib create(String appName, ContentType contentType) throws CoreException {

    //TODO (pquitslund): add param to specify pub support

    // Open wizard
    SWTBotMenu menu = bot.menu("File").menu("New Application");
    menu.setFocus();
    menu.click();
    SWTBotShell shell = bot.activeShell();
    shell.setFocus();
    shell.activate();

    // Reference widgets and Assert content
    SWTBotText appNameField = bot.textWithLabel("Application Name: ");
    assertNotNull(appNameField);
    // By calling setFocus on this widget, we ensure that this dialog is made the top-most
    // window before the click action happens.
    appNameField.setFocus();
    SWTBotUtil.waitForMainShellToDisappear(bot);
    SWTBotText appDirField = bot.textWithLabel("Parent Directory: ");
    SWTBotButton browseButton = bot.button("Browse...");
    SWTBotButton finishButton = bot.button("Finish");

//    bot.widget(widgetOfType(Button.class));

    SWTBotCheckButton webAppRadio = check(bot, "Generate content for a basic web app");

    assertEquals("", appNameField.getText());
    assertTrue(appDirField.getText().length() > 0);
    assertNotNull(browseButton);
    assertNotNull(finishButton);

    File appDir = new File(appDirField.getText(), appName);

    // Make either the selection of the web sample, or the server sample
    switch (contentType) {
      case WEB:
        appDir = new File(appDir, "web");
        webAppRadio.ensureSelected(true);
        break;
      case SERVER:
        webAppRadio.ensureSelected(false);
        break;
    }

    //TODO(pquitslund): this inline cleanup is messy
    // Ensure that the directory to be created does not exist
    DartLib lib = new DartLib(new File(appDirField.getText(), appName), appName);
    lib.deleteDir();

    lib = new DartLib(appDir, appName);

    // Enter name of new app
    appNameField.setFocus();
    appNameField.setText(appName);

    // Click OK button and wait for the operation to complete
    finishButton.click();
    lib.logFullAnalysisTime();
    EditorWithTitle matcher = new EditorWithTitle(lib.dartFile.getName());
    SwtBotPerformance.NEW_APP.log(bot, waitForEditor(matcher), appName);
    lib.editor = bot.editor(matcher).toTextEditor();
    lib.setProject(findProject(lib.editor));
    return lib;
  }

  private IProject findProject(SWTBotEclipseEditor editor) throws PartInitException {

    IEditorInput input = editor.getReference().getEditorInput();
    if (input instanceof IFileEditorInput) {
      return ((IFileEditorInput) input).getFile().getProject();
    }

    return null;
  }
}

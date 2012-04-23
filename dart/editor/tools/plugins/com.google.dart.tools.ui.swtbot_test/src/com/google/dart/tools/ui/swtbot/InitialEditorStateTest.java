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
package com.google.dart.tools.ui.swtbot;

import com.google.dart.tools.ui.dialogs.AboutDartDialog;
import com.google.dart.tools.ui.swtbot.dialog.PreferencesHelper;
import com.google.dart.tools.ui.swtbot.util.SWTBotUtil;
import com.google.dart.tools.ui.swtbot.views.FilesViewHelper;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Initial UI-state tests for the Dart Editor.
 * 
 * @see TestAll
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public final class InitialEditorStateTest extends AbstractDartEditorTest {

  @Ignore("Work in progress")
  @Test
  public void testInitState_aboutDialog() throws Exception {
    new AboutDartDialog(SWTBotUtil.getShell()).open();

    SWTBotShell shell = bot.activeShell();
    shell.activate();

    SWTBotLabel imageLabel = bot.label(0);
    SWTBotLabel productNameLabel = bot.label("Dart Editor");
    SWTBotStyledText buildDetailsText = bot.styledText(0);
    SWTBotLabel copyrightLabel1 = bot.label(2);
    SWTBotLabel copyrightLabel2 = bot.label(3);

    assertNotNull(imageLabel);
    assertTrue(imageLabel.getText().isEmpty());
    assertNotNull(imageLabel.image());

    assertNotNull(productNameLabel);
    assertEquals("Dart Editor", productNameLabel.getText());

    assertNotNull(buildDetailsText);
    assertFalse(buildDetailsText.getText().isEmpty());
    assertTrue(buildDetailsText.getText().indexOf("Dartium version ") != -1);

    assertNotNull(copyrightLabel1);

    assertNotNull(copyrightLabel2);
    assertEquals("All Rights Reserved.", copyrightLabel2.getText());

    shell.close();
    // TODO (jwren) once implemented, this test should assert that:
    // the dialog appears
    // correct widgets are visible and discoverable via SWTBot
    // user can exit the dialog
  }

  @Test
  public void testInitState_editor_welcome() throws Exception {
    SWTBotEditor editor = bot.editorByTitle(AbstractDartEditorTest.WELCOME_EDITOR_NAME);
    assertNotNull(editor);
    IEditorReference editorRef = editor.getReference();
    assertNotNull(editorRef);
    assertFalse(editorRef.isPinned());
    assertFalse(editorRef.isDirty());
    assertEquals(AbstractDartEditorTest.WELCOME_EDITOR_NAME, editorRef.getTitle());
    assertNotNull(editorRef.getTitleImage());
  }

  @Test
  public void testInitState_perspective() throws Exception {
    SWTBotPerspective perspective = bot.activePerspective();
    assertNotNull(perspective);
    assertEquals("Dart", perspective.getLabel());
  }

  @Test
  public void testInitState_preferencesDialog() throws Exception {
    PreferencesHelper preferencesHelper = new PreferencesHelper(bot);
    preferencesHelper.open();
    preferencesHelper.assertDefaultPreferencesSelected();
    preferencesHelper.close();
  }

  @Test
  public void testInitState_view_callers() throws Exception {
    bot.menu(AbstractDartEditorTest.TOOLS_MENU_NAME).menu(AbstractDartEditorTest.CALLERS_VIEW_NAME).click();
    SWTBotView view = baseViewAssertions(AbstractDartEditorTest.CALLERS_VIEW_NAME);
    view.close();
  }

  @Ignore("Old tests,Êneed to fix for the new Console")
  @Test
  public void testInitState_view_console() throws Exception {
    baseViewAssertions(AbstractDartEditorTest.CONSOLE_VIEW_NAME);
  }

  @Test
  public void testInitState_view_debugger() throws Exception {
    bot.menu("Tools").menu(AbstractDartEditorTest.DEBUGGER_VIEW_NAME).click();
    SWTBotView view = baseViewAssertions(AbstractDartEditorTest.DEBUGGER_VIEW_NAME);
    view.close();
  }

  @Test
  public void testInitState_view_files() throws Exception {
    baseViewAssertions(AbstractDartEditorTest.FILES_VIEW_NAME);
    FilesViewHelper filesViewHelper = new FilesViewHelper(bot);
    filesViewHelper.assertTreeItemCount(1);
    filesViewHelper.assertTreeItemsEqual(FilesViewHelper.SDK_TEXT);
    SWTBotTreeItem sdkTreeItem = filesViewHelper.getItems()[0];
    assertFalse(sdkTreeItem.isExpanded());
    assertTrue(sdkTreeItem.isVisible());
  }

  @Test
  public void testInitState_view_outline() throws Exception {
    bot.menu("Tools").menu(AbstractDartEditorTest.OUTLINE_VIEW_NAME).click();
    SWTBotView view = baseViewAssertions(AbstractDartEditorTest.OUTLINE_VIEW_NAME);
    view.close();
  }

  @Test
  public void testInitState_view_problems() throws Exception {
    baseViewAssertions(AbstractDartEditorTest.PROBLEMS_VIEW_NAME);
    ProblemsViewHelper helper = new ProblemsViewHelper(bot);
    helper.assertNoProblems();
  }

  /**
   * A utility method which make a set of base-assertions on the view with the passed title.
   * 
   * @param viewName the name as it appears in the Dart Editor
   * @return the {@link SWTBotView}, handy for make more tests after this method is called
   */
  private SWTBotView baseViewAssertions(String viewName) {
    SWTBotView view = bot.viewByTitle(viewName);
    assertNotNull(view);
    IViewReference viewRef = view.getReference();
    assertNotNull(viewRef);
    assertFalse(viewRef.isFastView());
    assertFalse(viewRef.isDirty());
    assertFalse(viewRef.getTitle().isEmpty());
    assertNotNull(viewRef.getTitleImage());
    return view;
  }

}

/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.refactoring;

import static org.fest.assertions.Assertions.assertThat;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.refactoring.AbstractDartTest;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import java.lang.reflect.Method;

/**
 * Base for {@link DartEditor} tests.
 */
public class AbstractDartEditorTest extends AbstractDartTest {
  protected DartEditor testEditor;
  protected ISourceViewer sourceViewer;
  protected StyledText textWidget;

  public void openEditor(String... lines) throws Exception {
    setTestUnitContent(lines);
    testEditor = (DartEditor) IDE.openEditor(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
        (IFile) testUnit.getResource());
    // prepare ISourceViewer and StyledText
    {
      Method method = AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
      method.setAccessible(true);
      sourceViewer = (ISourceViewer) method.invoke(testEditor);
    }
    textWidget = sourceViewer.getTextWidget();
  }

  /**
   * @return the {@link IAction} with given definition ID, not <code>null</code>.
   */
  protected final IAction getEditorAction(String id) {
    IActionBars actionBars = testEditor.getEditorSite().getActionBars();
    IAction action = actionBars.getGlobalActionHandler(id);
    assertNotNull("Can not find action " + id, action);
    return action;
  }

  /**
   * Attempts to find given pattern in the editor source and then places caret into this position.
   * Fails in position was not found.
   */
  protected final void selectAndReveal(String pattern) throws Exception {
    int position = sourceViewer.getDocument().get().indexOf(pattern);
    assertThat(position).isNotEqualTo(-1);
    testEditor.selectAndReveal(position, 0);
  }

  @Override
  protected void tearDown() throws Exception {
    if (testEditor != null) {
      testEditor.close(false);
    }
    super.tearDown();
  }
}

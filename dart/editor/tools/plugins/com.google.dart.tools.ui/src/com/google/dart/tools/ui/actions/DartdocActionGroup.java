/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * {@link ActionGroup} that adds the Dart Doc action.
 */
public class DartdocActionGroup extends AbstractDartSelectionActionGroup {
  private OpenExternalDartdocAction openExternalDartdocAction;

  public DartdocActionGroup(DartEditor editor) {
    super(editor);
    openExternalDartdocAction = new OpenExternalDartdocAction(editor);
    editor.setAction("OpenDartdoc", openExternalDartdocAction); //$NON-NLS-1$
    addActions(openExternalDartdocAction);
  }

  @Override
  public void dispose() {
    super.dispose();
    openExternalDartdocAction = null;
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    ISelection selection = getContext().getSelection();
    updateActions(selection);
    appendToGroup(menu, ITextEditorActionConstants.GROUP_OPEN);
  }
}

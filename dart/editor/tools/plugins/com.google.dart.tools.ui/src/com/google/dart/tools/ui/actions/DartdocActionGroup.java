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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Action group that adds the Dartdoc action to a context menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class DartdocActionGroup extends ActionGroup {

  private OpenExternalDartdocAction openExternalDartdocAction;

  private ISelectionProvider provider;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public DartdocActionGroup(DartEditor editor) {
    Assert.isNotNull(editor);
    openExternalDartdocAction = new OpenExternalDartdocAction(editor);
    editor.setAction("OpenDartdoc", openExternalDartdocAction); //$NON-NLS-1$
    initialize(editor.getSelectionProvider());
  }

  @Override
  public void dispose() {
    if (provider != null) {
      provider.removeSelectionChangedListener(openExternalDartdocAction);
    }
    openExternalDartdocAction = null;
    super.dispose();
  }

  @Override
  public void fillContextMenu(IMenuManager mm) {
    appendToGroup(mm, openExternalDartdocAction);
  }

  /**
   * This calls by {@link OpenExternalDartdocAction#updateEnabled(ISelection)} each time the
   * {@link ActionContext} is changed. This is what enables or disables the action in the context
   * menu in the Editor.
   */
  @Override
  public void setContext(ActionContext context) {
    if (context != null) {
      openExternalDartdocAction.updateEnabled(context.getSelection());
    }
    super.setContext(context);
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
//    if (action.isEnabled()) {
    menu.appendToGroup(ITextEditorActionConstants.GROUP_OPEN, action);
//    }
  }

  private void initialize(ISelectionProvider provider) {
    this.provider = provider;
    ISelection selection = provider.getSelection();
    openExternalDartdocAction.update(selection);
    provider.addSelectionChangedListener(openExternalDartdocAction);
  }

}

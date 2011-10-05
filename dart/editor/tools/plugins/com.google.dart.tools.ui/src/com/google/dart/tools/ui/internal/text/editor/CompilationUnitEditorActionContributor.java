/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.actions.IJavaEditorActionDefinitionIds;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.ui.texteditor.StatusLineContributionItem;

import java.util.ResourceBundle;

public class CompilationUnitEditorActionContributor extends
    BasicCompilationUnitEditorActionContributor {
  private static final boolean _showOffset = Boolean.valueOf(
      (Platform.getDebugOption("com.google.dart.tools.ui/statusbar/offset"))).booleanValue() || Platform.inDebugMode() || Platform.inDevelopmentMode(); //$NON-NLS-1$

  private RetargetTextEditorAction fToggleInsertModeAction;
  private RetargetTextEditorAction fToggleCommentAction;

  private StatusLineContributionItem fOffsetStatusField = null;

  public CompilationUnitEditorActionContributor() {
    super();

    ResourceBundle b = DartEditorMessages.getBundleForConstructedKeys();

    fToggleInsertModeAction = new RetargetTextEditorAction(b,
        "CompilationUnitEditorActionContributor.ToggleInsertMode.", IAction.AS_CHECK_BOX); //$NON-NLS-1$

    fToggleInsertModeAction.setActionDefinitionId(ITextEditorActionDefinitionIds.TOGGLE_INSERT_MODE);

    fToggleCommentAction = new RetargetTextEditorAction(b, "ToggleComment.", //$NON-NLS-1$
        IJavaEditorActionDefinitionIds.TOGGLE_COMMENT);

    if (_showOffset) {
      fOffsetStatusField = new StatusLineContributionItem(
          IDartEditorActionConstants.STATUS_CATEGORY_OFFSET, true, 10);
    }
  }

  @Override
  public void contributeToMenu(IMenuManager menu) {

    super.contributeToMenu(menu);

    IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
    if (editMenu != null) {
      editMenu.appendToGroup(ITextEditorActionConstants.GROUP_ASSIST, fToggleCommentAction);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToStatusLine(org
   * .eclipse.jface.action.IStatusLineManager)
   */
  @Override
  public void contributeToStatusLine(IStatusLineManager manager) {
    super.contributeToStatusLine(manager);
    if (_showOffset) {
      manager.add(fOffsetStatusField);
    }
  }

  /*
   * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
   */
  @Override
  public void setActiveEditor(IEditorPart part) {
    super.setActiveEditor(part);

    ITextEditor textEditor = null;
    ITextEditorExtension textEditorExtension = null;
    if (part instanceof ITextEditor) {
      textEditor = (ITextEditor) part;
    }
    if (part instanceof ITextEditorExtension) {
      textEditorExtension = (ITextEditorExtension) part;
    }

    if (_showOffset && textEditorExtension != null) {
      textEditorExtension.setStatusField(null, IDartEditorActionConstants.STATUS_CATEGORY_OFFSET);
    }

    // Source menu.
//    IActionBars bars = getActionBars();
//    bars.setGlobalActionHandler(JdtActionConstants.TOGGLE_COMMENT,
//        getAction(textEditor, "ToggleComment")); //$NON-NLS-1$
//    bars.setGlobalActionHandler(JdtActionConstants.FORMAT, getAction(textEditor, "Format")); //$NON-NLS-1$
//    bars.setGlobalActionHandler(JdtActionConstants.FORMAT_ELEMENT,
//        getAction(textEditor, "QuickFormat")); //$NON-NLS-1$
//    bars.setGlobalActionHandler(JdtActionConstants.ADD_BLOCK_COMMENT,
//        getAction(textEditor, "AddBlockComment")); //$NON-NLS-1$
//    bars.setGlobalActionHandler(JdtActionConstants.REMOVE_BLOCK_COMMENT,
//        getAction(textEditor, "RemoveBlockComment")); //$NON-NLS-1$
//    bars.setGlobalActionHandler(JdtActionConstants.INDENT, getAction(textEditor, "Indent")); //$NON-NLS-1$
//
//    IAction action = getAction(textEditor, ActionFactory.REFRESH.getId());
//    bars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), action);

    fToggleInsertModeAction.setAction(getAction(textEditor,
        ITextEditorActionConstants.TOGGLE_INSERT_MODE));

    fToggleCommentAction.setAction(getAction(textEditor, "ToggleComment"));

    if (_showOffset && textEditorExtension != null) {
      textEditorExtension.setStatusField(fOffsetStatusField,
          IDartEditorActionConstants.STATUS_CATEGORY_OFFSET);
      // fOffsetStatusField.setActionHandler(action);
    }
  }
}

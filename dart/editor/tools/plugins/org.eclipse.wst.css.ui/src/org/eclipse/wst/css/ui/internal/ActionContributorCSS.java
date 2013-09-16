/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.wst.sse.ui.internal.actions.ActionContributor;
import org.eclipse.wst.sse.ui.internal.actions.ActionDefinitionIds;
import org.eclipse.wst.sse.ui.internal.actions.StructuredTextEditorActionConstants;

import java.util.ResourceBundle;

/**
 * ActionContributorCSS This class should not be used inside multi page editor's
 * ActionBarContributor, since cascaded init() call from the ActionBarContributor will causes
 * exception and it leads to lose whole toolbars. Instead, use SourcePageActionContributor for
 * source page contributor of multi page editor. Note that this class is still valid for single page
 * editor.
 */
public class ActionContributorCSS extends ActionContributor {
  private static final String[] EDITOR_IDS = {
      "org.eclipse.wst.css.core.csssource.source", "org.eclipse.wst.sse.ui.StructuredTextEditor"}; //$NON-NLS-1$ //$NON-NLS-2$

  protected RetargetTextEditorAction fContentAssist = null;
  protected RetargetTextEditorAction fCleanupDocument = null;
  protected RetargetTextEditorAction fFormatDocument = null;
  protected RetargetTextEditorAction fFormatActiveElements = null;

  public ActionContributorCSS() {
    super();

    ResourceBundle resourceBundle = CSSUIMessages.getResourceBundle();

    // edit commands
    fContentAssist = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);

    // source commands
    fCleanupDocument = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fCleanupDocument.setActionDefinitionId(ActionDefinitionIds.CLEANUP_DOCUMENT);

    fFormatDocument = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fFormatDocument.setActionDefinitionId(ActionDefinitionIds.FORMAT_DOCUMENT);

    fFormatActiveElements = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fFormatActiveElements.setActionDefinitionId(ActionDefinitionIds.FORMAT_ACTIVE_ELEMENTS);
  }

  protected String[] getExtensionIDs() {
    return EDITOR_IDS;
  }

  protected void addToMenu(IMenuManager menu) {
    // edit commands
    IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
    if (editMenu != null) {
      editMenu.add(fCommandsSeparator);
      editMenu.add(fToggleInsertModeAction);
      editMenu.add(fCommandsSeparator);
      editMenu.add(fExpandSelectionToMenu);
      editMenu.add(fCommandsSeparator);
      editMenu.add(fContentAssist);
      editMenu.add(fMenuAdditionsGroupMarker);
    }

    // source commands
    String sourceMenuLabel = CSSUIMessages.SourceMenu_label;
    String sourceMenuId = "sourceMenuId"; //$NON-NLS-1$
    IMenuManager sourceMenu = new MenuManager(sourceMenuLabel, sourceMenuId);
    menu.insertAfter(IWorkbenchActionConstants.M_EDIT, sourceMenu);
    if (sourceMenu != null) {
      sourceMenu.add(fCommandsSeparator);
      sourceMenu.add(fShiftRight);
      sourceMenu.add(fShiftLeft);
      sourceMenu.add(fCleanupDocument);
      sourceMenu.add(fFormatDocument);
      sourceMenu.add(fFormatActiveElements);
      sourceMenu.add(fCommandsSeparator);
    }
    IMenuManager navigateMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
    if (navigateMenu != null) {
      IMenuManager gotoGroup = navigateMenu.findMenuUsingPath(IWorkbenchActionConstants.GO_TO);
      if (gotoGroup != null) {
        gotoGroup.add(fGotoMatchingBracketAction);
      }
    }
  }

  public void setActiveEditor(IEditorPart activeEditor) {
    super.setActiveEditor(activeEditor);

    ITextEditor textEditor = getTextEditor(activeEditor);

    fContentAssist.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_PROPOSALS));

    fCleanupDocument.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_CLEANUP_DOCUMENT));
    fFormatDocument.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT));
    fFormatActiveElements.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_ACTIVE_ELEMENTS));
    fCleanupDocument.setEnabled(textEditor != null && textEditor.isEditable());
    fFormatDocument.setEnabled(textEditor != null && textEditor.isEditable());
    fFormatActiveElements.setEnabled(textEditor != null && textEditor.isEditable());
  }
}

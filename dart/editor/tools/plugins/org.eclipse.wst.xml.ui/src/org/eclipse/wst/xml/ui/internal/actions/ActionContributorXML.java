/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.wst.sse.ui.internal.actions.ActionContributor;
import org.eclipse.wst.sse.ui.internal.actions.ActionDefinitionIds;
import org.eclipse.wst.sse.ui.internal.actions.StructuredTextEditorActionConstants;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

import java.util.ResourceBundle;

/**
 * XMLEditorActionContributor This class should not be used inside multi page editor's
 * ActionBarContributor, since cascaded init() call from the ActionBarContributor will causes
 * exception and it leads to lose whole toolbars. Instead, use SourcePageActionContributor for
 * source page contributor of multi page editor. Note that this class is still valid for single page
 * editor.
 */
public class ActionContributorXML extends ActionContributor {
  private static final String[] EDITOR_IDS = {
      "org.eclipse.core.runtime.xml.source", "org.eclipse.core.runtime.xml.source2", "org.eclipse.wst.sse.ui.StructuredTextEditor"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  private static final String GO_TO_MATCHING_TAG_ID = "org.eclipse.wst.xml.ui.gotoMatchingTag"; //$NON-NLS-1$

  protected RetargetTextEditorAction fCleanupDocument = null;
  protected RetargetTextEditorAction fComment = null;
  protected RetargetTextEditorAction fContentAssist = null;
  protected RetargetTextEditorAction fFindOccurrences = null;
  protected RetargetTextEditorAction fFormatActiveElements = null;
  protected RetargetTextEditorAction fFormatDocument = null;
  protected RetargetTextEditorAction fOpenFileAction = null; // open file

  protected RetargetTextEditorAction fUncomment = null;
  private GoToMatchingTagAction fGoToMatchingTagAction;

  public ActionContributorXML() {
    super();

    ResourceBundle resourceBundle = XMLUIMessages.getResourceBundle();

    fContentAssist = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);

    // source commands
    fCleanupDocument = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fCleanupDocument.setActionDefinitionId(ActionDefinitionIds.CLEANUP_DOCUMENT);

    fFormatDocument = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fFormatDocument.setActionDefinitionId(ActionDefinitionIds.FORMAT_DOCUMENT);

    fFormatActiveElements = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fFormatActiveElements.setActionDefinitionId(ActionDefinitionIds.FORMAT_ACTIVE_ELEMENTS);

    // navigate commands
    fOpenFileAction = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fOpenFileAction.setActionDefinitionId(ActionDefinitionIds.OPEN_FILE);

    fFindOccurrences = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
    fFindOccurrences.setActionDefinitionId(ActionDefinitionIds.FIND_OCCURRENCES);

    fGoToMatchingTagAction = new GoToMatchingTagAction(resourceBundle, "gotoMatchingTag_", null); //$NON-NLS-1$
    fGoToMatchingTagAction.setActionDefinitionId(GO_TO_MATCHING_TAG_ID);
    fGoToMatchingTagAction.setId(GO_TO_MATCHING_TAG_ID);
  }

  /**
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(IMenuManager)
   */
  @Override
  public void contributeToMenu(IMenuManager menu) {
    // navigate commands
    IMenuManager navigateMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
    if (navigateMenu != null) {
      navigateMenu.appendToGroup(IWorkbenchActionConstants.OPEN_EXT, fCommandsSeparator);
      navigateMenu.appendToGroup(IWorkbenchActionConstants.OPEN_EXT, fOpenFileAction);

      IMenuManager gotoGroup = navigateMenu.findMenuUsingPath(IWorkbenchActionConstants.GO_TO);
      if (gotoGroup != null) {
        gotoGroup.appendToGroup("matchingBegin", fGoToMatchingTagAction); //$NON-NLS-1$
      }
    }
    super.contributeToMenu(menu);
  }

  /**
   * @see org.eclipse.ui.IEditorActionBarContributor#setActiveEditor(IEditorPart)
   */
  @Override
  public void setActiveEditor(IEditorPart activeEditor) {
    if (getActiveEditorPart() == activeEditor) {
      return;
    }
    super.setActiveEditor(activeEditor);

    IActionBars actionBars = getActionBars();
    if (actionBars != null) {
      IStatusLineManager statusLineManager = actionBars.getStatusLineManager();
      if (statusLineManager != null) {
        statusLineManager.setMessage(null);
        statusLineManager.setErrorMessage(null);
      }
    }

    ITextEditor textEditor = getTextEditor(activeEditor);

    fContentAssist.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_PROPOSALS));

    fCleanupDocument.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_CLEANUP_DOCUMENT));
    fFormatDocument.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT));
    fFormatActiveElements.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_ACTIVE_ELEMENTS));
    fCleanupDocument.setEnabled((textEditor != null) && textEditor.isEditable());
    fFormatDocument.setEnabled((textEditor != null) && textEditor.isEditable());
    fFormatActiveElements.setEnabled((textEditor != null) && textEditor.isEditable());

    fOpenFileAction.setAction(getAction(textEditor,
        StructuredTextEditorActionConstants.ACTION_NAME_OPEN_FILE));

//    fFindOccurrences.setAction(getAction(textEditor,
//        StructuredTextEditorActionConstants.ACTION_NAME_FIND_OCCURRENCES));

    fGoToMatchingTagAction.setEditor(textEditor);
    if (textEditor != null) {
      if (textEditor.getAction(GO_TO_MATCHING_TAG_ID) != fGoToMatchingTagAction) {
        textEditor.setAction(GO_TO_MATCHING_TAG_ID, fGoToMatchingTagAction);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.ISourceViewerActionBarContributor#setViewerSpecificContributionsEnabled
   * (boolean)
   */
  @Override
  public void setViewerSpecificContributionsEnabled(boolean enabled) {
    super.setViewerSpecificContributionsEnabled(enabled);

    fContentAssist.setEnabled(enabled);
    // cleanup and format document actions do not rely on source viewer
    // being enabled
    // fCleanupDocument.setEnabled(enabled);
    // fFormatDocument.setEnabled(enabled);

    fFormatActiveElements.setEnabled(enabled);
    fOpenFileAction.setEnabled(enabled);
    fFindOccurrences.setEnabled(enabled);

    fGoToMatchingTagAction.setEnabled(enabled);
    fGotoMatchingBracketAction.setEnabled(enabled);
  }

  @Override
  protected void addToMenu(IMenuManager menu) {
    /*
     * // edit commands IMenuManager editMenu =
     * menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT); if (editMenu != null) {
     * editMenu.add(fCommandsSeparator); editMenu.add(fToggleInsertModeAction);
     * editMenu.add(fCommandsSeparator); editMenu.add(fExpandSelectionToMenu);
     * editMenu.add(fCommandsSeparator); editMenu.add(fContentAssist);
     * editMenu.add(fMenuAdditionsGroupMarker); }
     * 
     * // source commands String sourceMenuLabel = XMLUIMessages.SourceMenu_label; String
     * sourceMenuId = "sourceMenuId"; //$NON-NLS-1$ IMenuManager sourceMenu = new
     * MenuManager(sourceMenuLabel, sourceMenuId);
     * menu.insertAfter(IWorkbenchActionConstants.M_EDIT, sourceMenu); if (sourceMenu != null) {
     * sourceMenu.add(fCommandsSeparator); sourceMenu.add(fToggleComment);
     * sourceMenu.add(fAddBlockComment); sourceMenu.add(fRemoveBlockComment);
     * sourceMenu.add(fShiftRight); sourceMenu.add(fShiftLeft); sourceMenu.add(fCleanupDocument);
     * sourceMenu.add(fFormatDocument); sourceMenu.add(fFormatActiveElements);
     * sourceMenu.add(fCommandsSeparator); sourceMenu.add(fFindOccurrences); }
     * 
     * // navigate commands IMenuManager navigateMenu =
     * menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE); if (navigateMenu != null) {
     * navigateMenu.appendToGroup(IWorkbenchActionConstants.OPEN_EXT, fCommandsSeparator);
     * navigateMenu.appendToGroup(IWorkbenchActionConstants.OPEN_EXT, fOpenFileAction);
     * 
     * IMenuManager gotoGroup = navigateMenu.findMenuUsingPath(IWorkbenchActionConstants.GO_TO); if
     * (gotoGroup != null) { gotoGroup.add(fGotoMatchingBracketAction);
     * gotoGroup.add(fGoToMatchingTagAction); gotoGroup.add(new Separator()); } }
     */
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.edit.util.ActionContributor#getExtensionIDs()
   */
  @Override
  protected String[] getExtensionIDs() {
    return EDITOR_IDS;
  }
}

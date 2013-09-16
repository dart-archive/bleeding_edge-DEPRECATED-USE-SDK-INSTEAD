/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring David Carver/STAR -
 * [212330] can't contribute to the XML or any SSE based menu
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.actions;

// import java.util.ResourceBundle;

import com.google.dart.tools.search.ui.IContextMenuConstants;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.wst.sse.ui.internal.ExtendedEditorActionBuilder;
import org.eclipse.wst.sse.ui.internal.GotoAnnotationAction;
import org.eclipse.wst.sse.ui.internal.IExtendedContributor;
import org.eclipse.wst.sse.ui.internal.ISourceViewerActionBarContributor;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.ui.OffsetStatusLineContributionItem;

import java.util.ResourceBundle;

/**
 * This class should not be used inside multi page editor's ActionBarContributor, since cascaded
 * init() call from the ActionBarContributor will causes exception and it leads to lose whole
 * toolbars. Instead, use SourcePageActionContributor for source page contributor of multi page
 * editor. Note that this class is still valid for single page editor
 */
public class ActionContributor extends TextEditorActionContributor implements
    ISourceViewerActionBarContributor, IExtendedContributor {

  public static final boolean _showDebugStatus = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/actioncontributor/debugstatusfields")) || Platform.inDebugMode() || Platform.inDevelopmentMode(); //$NON-NLS-1$ //$NON-NLS-2$

  private static final String[] EDITOR_IDS = {"org.eclipse.wst.sse.ui.StructuredTextEditor"}; //$NON-NLS-1$

  protected IExtendedContributor extendedContributor;
  protected RetargetTextEditorAction fAddBlockComment = null;

  protected Separator fCommandsSeparator = null;

  private OffsetStatusLineContributionItem fDebugStatusOffset = null;
  protected MenuManager fExpandSelectionToMenu = null;
  protected GroupMarker fMenuAdditionsGroupMarker = null;
  protected GotoAnnotationAction fNextAnnotation = null;

  protected GotoAnnotationAction fPreviousAnnotation = null;
  protected RetargetTextEditorAction fRemoveBlockComment = null;
  protected RetargetTextEditorAction fShiftLeft = null;
  protected RetargetTextEditorAction fShiftRight = null;
  protected RetargetTextEditorAction fStructureSelectEnclosingAction = null;
  protected RetargetTextEditorAction fStructureSelectHistoryAction = null;
  protected RetargetTextEditorAction fStructureSelectNextAction = null;
  protected RetargetTextEditorAction fStructureSelectPreviousAction = null;

  protected RetargetTextEditorAction fToggleComment = null;
  protected RetargetTextEditorAction fToggleInsertModeAction;
  protected GroupMarker fToolbarAdditionsGroupMarker = null;
  protected Separator fToolbarSeparator = null;

  protected RetargetTextEditorAction fGotoMatchingBracketAction = null;

  public ActionContributor() {
    super();

    ResourceBundle resourceBundle = SSEUIMessages.getResourceBundle();

    fCommandsSeparator = new Separator();

    // edit commands
    fStructureSelectEnclosingAction = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_ENCLOSING
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fStructureSelectEnclosingAction.setActionDefinitionId(ActionDefinitionIds.STRUCTURE_SELECT_ENCLOSING);

    fStructureSelectNextAction = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_NEXT
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fStructureSelectNextAction.setActionDefinitionId(ActionDefinitionIds.STRUCTURE_SELECT_NEXT);

    fStructureSelectPreviousAction = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_PREVIOUS
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fStructureSelectPreviousAction.setActionDefinitionId(ActionDefinitionIds.STRUCTURE_SELECT_PREVIOUS);

    fStructureSelectHistoryAction = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_HISTORY
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fStructureSelectHistoryAction.setActionDefinitionId(ActionDefinitionIds.STRUCTURE_SELECT_HISTORY);

    fExpandSelectionToMenu = new MenuManager(SSEUIMessages.ExpandSelectionToMenu_label); //$NON-NLS-1$
    fExpandSelectionToMenu.add(fStructureSelectEnclosingAction);
    fExpandSelectionToMenu.add(fStructureSelectNextAction);
    fExpandSelectionToMenu.add(fStructureSelectPreviousAction);
    fExpandSelectionToMenu.add(fStructureSelectHistoryAction);

    // source commands
    fShiftRight = new RetargetTextEditorAction(resourceBundle,
        ITextEditorActionConstants.SHIFT_RIGHT + StructuredTextEditorActionConstants.UNDERSCORE);
    fShiftRight.setActionDefinitionId(ITextEditorActionDefinitionIds.SHIFT_RIGHT);

    fShiftLeft = new RetargetTextEditorAction(resourceBundle, ITextEditorActionConstants.SHIFT_LEFT
        + StructuredTextEditorActionConstants.UNDERSCORE);
    fShiftLeft.setActionDefinitionId(ITextEditorActionDefinitionIds.SHIFT_LEFT);

    fToggleComment = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_TOGGLE_COMMENT
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fToggleComment.setActionDefinitionId(ActionDefinitionIds.TOGGLE_COMMENT);

    fAddBlockComment = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_ADD_BLOCK_COMMENT
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fAddBlockComment.setActionDefinitionId(ActionDefinitionIds.ADD_BLOCK_COMMENT);

    fRemoveBlockComment = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_REMOVE_BLOCK_COMMENT
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fRemoveBlockComment.setActionDefinitionId(ActionDefinitionIds.REMOVE_BLOCK_COMMENT);

    // goto prev/next error
    // CMVC 249017 for JavaEditor consistancy
    fPreviousAnnotation = new GotoAnnotationAction("Previous_annotation", false); //$NON-NLS-1$
    fPreviousAnnotation.setActionDefinitionId("org.eclipse.ui.navigate.previous"); //$NON-NLS-1$

    fNextAnnotation = new GotoAnnotationAction("Next_annotation", true); //$NON-NLS-1$
    fNextAnnotation.setActionDefinitionId("org.eclipse.ui.navigate.next"); //$NON-NLS-1$

    fGotoMatchingBracketAction = new RetargetTextEditorAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_GOTO_MATCHING_BRACKET
            + StructuredTextEditorActionConstants.UNDERSCORE);
    fGotoMatchingBracketAction.setActionDefinitionId(ActionDefinitionIds.GOTO_MATCHING_BRACKET);

    // Read action extensions.
    ExtendedEditorActionBuilder builder = new ExtendedEditorActionBuilder();
    extendedContributor = builder.readActionExtensions(getExtensionIDs());

    fMenuAdditionsGroupMarker = new GroupMarker(
        StructuredTextEditorActionConstants.GROUP_NAME_MENU_ADDITIONS);
    fToolbarSeparator = new Separator();
    fToolbarAdditionsGroupMarker = new GroupMarker(
        StructuredTextEditorActionConstants.GROUP_NAME_TOOLBAR_ADDITIONS);

    fToggleInsertModeAction = new RetargetTextEditorAction(resourceBundle,
        "Editor.ToggleInsertMode.", IAction.AS_CHECK_BOX); //$NON-NLS-1$
    fToggleInsertModeAction.setActionDefinitionId(ITextEditorActionDefinitionIds.TOGGLE_INSERT_MODE);

    if (_showDebugStatus) {
      fDebugStatusOffset = new OffsetStatusLineContributionItem(
          StructuredTextEditorActionConstants.STATUS_CATEGORY_OFFSET, true, 20);
    }
  }

  protected void addToMenu(IMenuManager menu) {
    // edit commands
    /*
     * IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT); if
     * (editMenu != null) { editMenu.add(fCommandsSeparator); editMenu.add(fToggleInsertModeAction);
     * editMenu.add(fCommandsSeparator); editMenu.add(fExpandSelectionToMenu);
     * editMenu.add(fCommandsSeparator); editMenu.add(fMenuAdditionsGroupMarker); }
     * 
     * // source commands String sourceMenuLabel = SSEUIMessages.SourceMenu_label; //$NON-NLS-1$
     * String sourceMenuId = "sourceMenuId"; // This is just a menu id. No //$NON-NLS-1$ // need to
     * translate. // //$NON-NLS-1$ IMenuManager sourceMenu = new MenuManager(sourceMenuLabel,
     * sourceMenuId); menu.insertAfter(IWorkbenchActionConstants.M_EDIT, sourceMenu); if (sourceMenu
     * != null) { sourceMenu.add(fCommandsSeparator); sourceMenu.add(fToggleComment);
     * sourceMenu.add(fAddBlockComment); sourceMenu.add(fRemoveBlockComment);
     * sourceMenu.add(fShiftRight); sourceMenu.add(fShiftLeft); }
     * 
     * IMenuManager gotoMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.GO_TO); if (gotoMenu
     * != null) { gotoMenu.add(fGotoMatchingBracketAction); }
     */
  }

  protected void addToPopupMenu(IMenuManager menu) {
    // add nothing
  }

  protected void addToStatusLine(IStatusLineManager manager) {
    if (_showDebugStatus) {
      manager.add(fDebugStatusOffset);
    }
  }

  protected void addToToolBar(IToolBarManager toolBarManager) {
    /*
     * toolBarManager.add(fToolbarSeparator); toolBarManager.add(fToolbarAdditionsGroupMarker);
     */}

  /**
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(IMenuManager)
   */
  public void contributeToMenu(IMenuManager menu) {
    super.contributeToMenu(menu);

    IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
    if (editMenu != null) {
      editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fToggleInsertModeAction);
    }
    /*
     * addToMenu(menu);
     */
    if (extendedContributor != null) {
      extendedContributor.contributeToMenu(menu);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.extension.IPopupMenuContributor#contributeToPopupMenu(org.eclipse.jface
   * .action.IMenuManager)
   */
  public void contributeToPopupMenu(IMenuManager menu) {
    /*
     * addToPopupMenu(menu);
     */
    if (extendedContributor != null) {
      extendedContributor.contributeToPopupMenu(menu);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.part.EditorActionBarContributor#contributeToStatusLine(org.eclipse.jface.action
   * .IStatusLineManager)
   */
  public void contributeToStatusLine(IStatusLineManager manager) {
    super.contributeToStatusLine(manager);

    addToStatusLine(manager);

    if (extendedContributor != null) {
      extendedContributor.contributeToStatusLine(manager);
    }
  }

  /**
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToToolBar(IToolBarManager)
   */
  public void contributeToToolBar(IToolBarManager toolBarManager) {
    super.contributeToToolBar(toolBarManager);

    /*
     * addToToolBar(toolBarManager);
     */
    if (extendedContributor != null) {
      extendedContributor.contributeToToolBar(toolBarManager);
    }
  }

  /**
   * @see org.eclipse.ui.part.EditorActionBarContributor#dispose()
   */
  public void dispose() {
    // need to call setActiveEditor before super.dispose because in both
    // setActiveEditor & super.setActiveEditor if getEditorPart ==
    // activeEditor,
    // the method is just returned. so to get both methods to run,
    // setActiveEditor
    // needs to be called so that it correctly calls super.setActiveEditor
    setActiveEditor(null);

    super.dispose();

    if (extendedContributor != null)
      extendedContributor.dispose();
  }

  protected String[] getExtensionIDs() {
    return EDITOR_IDS;
  }

  /**
   * @param editor
   * @return
   */
  protected ITextEditor getTextEditor(IEditorPart editor) {
    ITextEditor textEditor = null;
    if (editor instanceof ITextEditor)
      textEditor = (ITextEditor) editor;
    if (textEditor == null && editor != null)
      textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
    return textEditor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorActionBarContributor#init(org.eclipse.ui.IActionBars,
   * org.eclipse.ui.IWorkbenchPage)
   */
  public void init(IActionBars bars, IWorkbenchPage page) {
    super.init(bars, page);
  }

  /**
   * @see org.eclipse.ui.IEditorActionBarContributor#setActiveEditor(IEditorPart)
   */
  public void setActiveEditor(IEditorPart activeEditor) {
    if (getActiveEditorPart() == activeEditor)
      return;
    super.setActiveEditor(activeEditor);

    ITextEditor textEditor = getTextEditor(activeEditor);

    /*
     * IActionBars actionBars = getActionBars(); if (actionBars != null) {
     * actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION,
     * fNextAnnotation);
     * actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION,
     * fPreviousAnnotation); actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(),
     * fPreviousAnnotation); actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(),
     * fNextAnnotation);
     * 
     * if (textEditor != null) {
     * actionBars.setGlobalActionHandler(IDEActionFactory.ADD_TASK.getId(), getAction(textEditor,
     * IDEActionFactory.ADD_TASK.getId()));
     * actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), getAction(textEditor,
     * IDEActionFactory.BOOKMARK.getId())); } }
     * 
     * fStructureSelectEnclosingAction.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_ENCLOSING));
     * fStructureSelectNextAction.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_NEXT));
     * fStructureSelectPreviousAction.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_PREVIOUS));
     * fStructureSelectHistoryAction.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_STRUCTURE_SELECT_HISTORY));
     * 
     * fShiftRight.setAction(getAction(textEditor, ITextEditorActionConstants.SHIFT_RIGHT));
     * fShiftLeft.setAction(getAction(textEditor, ITextEditorActionConstants.SHIFT_LEFT));
     * 
     * fToggleComment.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_TOGGLE_COMMENT));
     * fAddBlockComment.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_ADD_BLOCK_COMMENT));
     * fRemoveBlockComment.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_REMOVE_BLOCK_COMMENT));
     * 
     * // go to prev/next error // CMVC 249017 for JavaEditor consistancy
     * fPreviousAnnotation.setEditor(textEditor); fNextAnnotation.setEditor(textEditor);
     * fGotoMatchingBracketAction.setAction(getAction(textEditor,
     * StructuredTextEditorActionConstants.ACTION_NAME_GOTO_MATCHING_BRACKET));
     */
    fToggleInsertModeAction.setAction(getAction(textEditor,
        ITextEditorActionConstants.TOGGLE_INSERT_MODE));

    if (extendedContributor != null) {
      extendedContributor.setActiveEditor(activeEditor);
    }

    if (_showDebugStatus && textEditor instanceof ITextEditorExtension) {
      ((ITextEditorExtension) textEditor).setStatusField(fDebugStatusOffset,
          StructuredTextEditorActionConstants.STATUS_CATEGORY_OFFSET);
      fDebugStatusOffset.setActiveEditor(textEditor);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.ISourceViewerActionBarContributor#setViewerSpecificContributionsEnabled
   * (boolean)
   */
  public void setViewerSpecificContributionsEnabled(boolean enabled) {
    fToggleInsertModeAction.setEnabled(enabled);
    /*
     * fShiftRight.setEnabled(enabled); fShiftLeft.setEnabled(enabled);
     * fNextAnnotation.setEnabled(enabled); fPreviousAnnotation.setEnabled(enabled);
     */
    /*
     * fComment.setEnabled(enabled); fUncomment.setEnabled(enabled);
     */
    /*
     * fToggleComment.setEnabled(enabled); fAddBlockComment.setEnabled(enabled);
     * fRemoveBlockComment.setEnabled(enabled);
     */
    // convert line delimiters are not source viewer-specific

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.extension.IExtendedContributor#updateToolbarActions()
   */
  public void updateToolbarActions() {
    if (extendedContributor != null) {
      extendedContributor.updateToolbarActions();
    }
  }
}

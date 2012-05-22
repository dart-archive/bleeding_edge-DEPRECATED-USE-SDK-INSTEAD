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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.actions.FoldingActionGroup;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.GoToNextPreviousMemberAction;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.StructureSelectionAction;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Common base class for action contributors for Dart editors.
 */
public class BasicDartEditorActionContributor extends BasicTextEditorActionContributor {

  private List<RetargetAction> fPartListeners = new ArrayList<RetargetAction>();

  private TogglePresentationAction fTogglePresentation;
  private ToggleMarkOccurrencesAction fToggleMarkOccurrencesAction;

  private RetargetTextEditorAction fGotoMatchingBracket;
  private RetargetTextEditorAction fShowOutline;
  private RetargetTextEditorAction fOpenStructure;
  private RetargetTextEditorAction fOpenHierarchy;
  private RetargetTextEditorAction fOpenDeclaration;

  private RetargetTextEditorAction fRetargetShowInformationAction;

  private RetargetTextEditorAction fStructureSelectEnclosingAction;
  private RetargetTextEditorAction fStructureSelectNextAction;
  private RetargetTextEditorAction fStructureSelectPreviousAction;
  private RetargetTextEditorAction fStructureSelectHistoryAction;

  private RetargetTextEditorAction fGotoNextMemberAction;
  private RetargetTextEditorAction fGotoPreviousMemberAction;

  private RetargetTextEditorAction fRemoveOccurrenceAnnotationsAction;

  private RetargetTextEditorAction fOpenCallHierarchy;

  public BasicDartEditorActionContributor() {
    super();

    ResourceBundle b = DartEditorMessages.getBundleForConstructedKeys();

    fRetargetShowInformationAction = new RetargetTextEditorAction(b, "Editor.ShowInformation."); //$NON-NLS-1$
    fRetargetShowInformationAction.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);

    // actions that are "contributed" to editors, they are considered belonging
    // to the active editor
    fTogglePresentation = new TogglePresentationAction();

    fToggleMarkOccurrencesAction = new ToggleMarkOccurrencesAction();

    fGotoMatchingBracket = new RetargetTextEditorAction(b, "GotoMatchingBracket."); //$NON-NLS-1$
    fGotoMatchingBracket.setActionDefinitionId(DartEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);

    fShowOutline = new RetargetTextEditorAction(DartEditorMessages.getBundleForConstructedKeys(),
        "ShowOutline."); //$NON-NLS-1$
    fShowOutline.setActionDefinitionId(DartEditorActionDefinitionIds.SHOW_OUTLINE);

    fOpenDeclaration = new RetargetTextEditorAction(ActionMessages.getBundle(), "OpenAction_"); //$NON-NLS-1$
    fOpenDeclaration.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_EDITOR);

    fOpenHierarchy = new RetargetTextEditorAction(DartEditorMessages.getBundleForConstructedKeys(),
        "OpenHierarchy."); //$NON-NLS-1$
    fOpenHierarchy.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_HIERARCHY);

    fOpenCallHierarchy = new RetargetTextEditorAction(
        DartEditorMessages.getBundleForConstructedKeys(), "OpenCallHierarchy."); //$NON-NLS-1$
    fOpenCallHierarchy.setActionDefinitionId(DartEditorActionDefinitionIds.ANALYZE_CALL_HIERARCHY);

    fOpenStructure = new RetargetTextEditorAction(DartEditorMessages.getBundleForConstructedKeys(),
        "OpenStructure."); //$NON-NLS-1$
    fOpenStructure.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_STRUCTURE);

    fStructureSelectEnclosingAction = new RetargetTextEditorAction(b, "StructureSelectEnclosing."); //$NON-NLS-1$
    fStructureSelectEnclosingAction.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_ENCLOSING);
    fStructureSelectNextAction = new RetargetTextEditorAction(b, "StructureSelectNext."); //$NON-NLS-1$
    fStructureSelectNextAction.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_NEXT);
    fStructureSelectPreviousAction = new RetargetTextEditorAction(b, "StructureSelectPrevious."); //$NON-NLS-1$
    fStructureSelectPreviousAction.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_PREVIOUS);
    fStructureSelectHistoryAction = new RetargetTextEditorAction(b, "StructureSelectHistory."); //$NON-NLS-1$
    fStructureSelectHistoryAction.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_LAST);

    fGotoNextMemberAction = new RetargetTextEditorAction(b, "GotoNextMember."); //$NON-NLS-1$
    fGotoNextMemberAction.setActionDefinitionId(DartEditorActionDefinitionIds.GOTO_NEXT_MEMBER);
    fGotoPreviousMemberAction = new RetargetTextEditorAction(b, "GotoPreviousMember."); //$NON-NLS-1$
    fGotoPreviousMemberAction.setActionDefinitionId(DartEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);

    fRemoveOccurrenceAnnotationsAction = new RetargetTextEditorAction(b,
        "RemoveOccurrenceAnnotations."); //$NON-NLS-1$
    fRemoveOccurrenceAnnotationsAction.setActionDefinitionId(DartEditorActionDefinitionIds.REMOVE_OCCURRENCE_ANNOTATIONS);
  }

  /*
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse
   * .jface.action.IMenuManager)
   */
  @Override
  public void contributeToMenu(IMenuManager menu) {

    super.contributeToMenu(menu);

    IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
    if (editMenu != null) {

      //MenuManager structureSelection = new MenuManager(
      //    DartEditorMessages.ExpandSelectionMenu_label, "expandSelection"); //$NON-NLS-1$
      //editMenu.insertAfter(ITextEditorActionConstants.SELECT_ALL, structureSelection);
      //structureSelection.add(fStructureSelectEnclosingAction);
      //structureSelection.add(fStructureSelectNextAction);
      //structureSelection.add(fStructureSelectPreviousAction);
      //structureSelection.add(fStructureSelectHistoryAction);

      //editMenu.appendToGroup(ITextEditorActionConstants.GROUP_INFORMATION,
      //    fRetargetShowInformationAction);
    }

    IMenuManager navigateMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
    if (navigateMenu != null) {
      navigateMenu.appendToGroup(IWorkbenchActionConstants.OPEN_EXT, fOpenDeclaration);
      navigateMenu.appendToGroup(IWorkbenchActionConstants.OPEN_EXT, fOpenCallHierarchy);
      navigateMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fShowOutline);
      //navigateMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fOpenHierarchy);
    }

    //IMenuManager gotoMenu = menu.findMenuUsingPath("navigate/goTo"); //$NON-NLS-1$
    //if (gotoMenu != null) {
    //  gotoMenu.add(new Separator("additions2")); //$NON-NLS-1$
    //  gotoMenu.appendToGroup("additions2", fGotoPreviousMemberAction); //$NON-NLS-1$
    //  gotoMenu.appendToGroup("additions2", fGotoNextMemberAction); //$NON-NLS-1$
    //  gotoMenu.appendToGroup("additions2", fGotoMatchingBracket); //$NON-NLS-1$
    //}
  }

  /*
   * @see IEditorActionBarContributor#dispose()
   */
  @Override
  public void dispose() {

    Iterator<RetargetAction> e = fPartListeners.iterator();
    while (e.hasNext()) {
      getPage().removePartListener(e.next());
    }
    fPartListeners.clear();

    setActiveEditor(null);
    super.dispose();
  }

  /*
   * @see IEditorActionBarContributor#init(IActionBars, IWorkbenchPage)
   */
  @Override
  public void init(IActionBars bars, IWorkbenchPage page) {
    Iterator<RetargetAction> e = fPartListeners.iterator();
    while (e.hasNext()) {
      page.addPartListener(e.next());
    }

    super.init(bars, page);

    bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY,
        fTogglePresentation);
    bars.setGlobalActionHandler(DartEditorActionDefinitionIds.TOGGLE_MARK_OCCURRENCES,
        fToggleMarkOccurrencesAction);

  }

  /*
   * @see EditorActionBarContributor#setActiveEditor(IEditorPart)
   */
  @Override
  public void setActiveEditor(IEditorPart part) {

    super.setActiveEditor(part);

    ITextEditor textEditor = null;
    if (part instanceof ITextEditor) {
      textEditor = (ITextEditor) part;
    }

    fTogglePresentation.setEditor(textEditor);
    fToggleMarkOccurrencesAction.setEditor(textEditor);

    fGotoMatchingBracket.setAction(getAction(textEditor,
        GotoMatchingBracketAction.GOTO_MATCHING_BRACKET));
    fShowOutline.setAction(getAction(textEditor, DartEditorActionDefinitionIds.SHOW_OUTLINE));
    fOpenCallHierarchy.setAction(getAction(textEditor,
        DartEditorActionDefinitionIds.ANALYZE_CALL_HIERARCHY));
    fOpenHierarchy.setAction(getAction(textEditor, DartEditorActionDefinitionIds.OPEN_HIERARCHY));
    fOpenStructure.setAction(getAction(textEditor, DartEditorActionDefinitionIds.OPEN_STRUCTURE));

    fOpenDeclaration.setAction(getAction(textEditor, "OpenEditor"));

    fStructureSelectEnclosingAction.setAction(getAction(textEditor,
        StructureSelectionAction.ENCLOSING));
    fStructureSelectNextAction.setAction(getAction(textEditor, StructureSelectionAction.NEXT));
    fStructureSelectPreviousAction.setAction(getAction(textEditor,
        StructureSelectionAction.PREVIOUS));
    fStructureSelectHistoryAction.setAction(getAction(textEditor, StructureSelectionAction.HISTORY));

    fGotoNextMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.NEXT_MEMBER));
    fGotoPreviousMemberAction.setAction(getAction(textEditor,
        GoToNextPreviousMemberAction.PREVIOUS_MEMBER));

    fRemoveOccurrenceAnnotationsAction.setAction(getAction(textEditor,
        "RemoveOccurrenceAnnotations")); //$NON-NLS-1$
    fRetargetShowInformationAction.setAction(getAction(textEditor,
        ITextEditorActionConstants.SHOW_INFORMATION));

    if (part instanceof DartEditor) {
      DartEditor javaEditor = (DartEditor) part;
      javaEditor.getActionGroup().fillActionBars(getActionBars());
      FoldingActionGroup foldingActions = javaEditor.getFoldingActionGroup();
      if (foldingActions != null) {
        foldingActions.updateActionBars();
      }
    }

    IActionBars actionBars = getActionBars();
    IStatusLineManager manager = actionBars.getStatusLineManager();
    manager.setMessage(null);
    manager.setErrorMessage(null);

    /** The global actions to be connected with editor actions */
    IAction action = getAction(textEditor, ITextEditorActionConstants.NEXT);
    actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, action);
    actionBars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, action);
    action = getAction(textEditor, ITextEditorActionConstants.PREVIOUS);
    actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION,
        action);
    actionBars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, action);
    action = getAction(textEditor, IDartEditorActionConstants.COPY_QUALIFIED_NAME);
    DartX.todo();
    // actionBars.setGlobalActionHandler(CopyQualifiedNameAction.ACTION_HANDLER_ID,
    // action);
  }

  protected final void markAsPartListener(RetargetAction action) {
    fPartListeners.add(action);
  }
}

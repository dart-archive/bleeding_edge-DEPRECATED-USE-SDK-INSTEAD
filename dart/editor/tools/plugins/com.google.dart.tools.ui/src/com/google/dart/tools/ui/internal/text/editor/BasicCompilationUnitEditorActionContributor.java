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

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.actions.JdtActionConstants;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class BasicCompilationUnitEditorActionContributor extends BasicDartEditorActionContributor {

  /**
   * A menu listener that can remove itself from the menu it listens to.
   */
  private final class MenuListener implements IMenuListener {
    private final IMenuManager fMenu;

    @SuppressWarnings("unused")
    // likely to be used with quick assist
    public MenuListener(IMenuManager menu) {
      fMenu = menu;
    }

    public void dispose() {
      fMenu.removeMenuListener(this);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager) {
      for (int i = 0; i < fSpecificAssistActions.length; i++) {
        fSpecificAssistActions[i].update();
      }
    }
  }

  protected RetargetAction fRetargetContentAssist;
  protected RetargetTextEditorAction fContentAssist;
  protected RetargetTextEditorAction fContextInformation;
  protected RetargetTextEditorAction fQuickAssistAction;
  //protected RetargetTextEditorAction fChangeEncodingAction;

  /*  */
  protected SpecificContentAssistAction[] fSpecificAssistActions;
  /*  */
  private MenuListener fContentAssistMenuListener;

  public BasicCompilationUnitEditorActionContributor() {

    fRetargetContentAssist = new RetargetAction(JdtActionConstants.CONTENT_ASSIST,
        DartEditorMessages.ContentAssistProposal_label);
    fRetargetContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    fRetargetContentAssist.setImageDescriptor(DartPluginImages.DESC_ELCL_CODE_ASSIST);
    fRetargetContentAssist.setDisabledImageDescriptor(DartPluginImages.DESC_DLCL_CODE_ASSIST);
    markAsPartListener(fRetargetContentAssist);

    fContentAssist = new RetargetTextEditorAction(DartEditorMessages.getBundleForConstructedKeys(),
        "ContentAssistProposal."); //$NON-NLS-1$
    fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    fContentAssist.setImageDescriptor(DartPluginImages.DESC_ELCL_CODE_ASSIST);
    fContentAssist.setDisabledImageDescriptor(DartPluginImages.DESC_DLCL_CODE_ASSIST);

    fContextInformation = new RetargetTextEditorAction(
        DartEditorMessages.getBundleForConstructedKeys(), "ContentAssistContextInformation."); //$NON-NLS-1$
    fContextInformation.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);

    fQuickAssistAction = new RetargetTextEditorAction(
        DartEditorMessages.getBundleForConstructedKeys(), "CorrectionAssistProposal."); //$NON-NLS-1$
    fQuickAssistAction.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICK_ASSIST);

    //fChangeEncodingAction = new RetargetTextEditorAction(
    //    DartEditorMessages.getBundleForConstructedKeys(), "Editor.ChangeEncodingAction."); //$NON-NLS-1$
  }

  /*
   * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
   */
  @Override
  public void contributeToMenu(IMenuManager menu) {

    super.contributeToMenu(menu);
    if (fContentAssistMenuListener != null) {
      fContentAssistMenuListener.dispose();
    }

    IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
    if (editMenu != null) {
      //editMenu.add(fChangeEncodingAction);
      //IMenuManager caMenu = new MenuManager(
      //    DartEditorMessages.BasicEditorActionContributor_specific_content_assist_menu,
      //    "specific_content_assist"); //$NON-NLS-1$
      //editMenu.insertAfter(ITextEditorActionConstants.GROUP_ASSIST, caMenu);

      //caMenu.add(fRetargetContentAssist);
      //Collection<?> descriptors = CompletionProposalComputerRegistry.getDefault().getProposalCategories();
      //List<IAction> specificAssistActions = new ArrayList<IAction>(descriptors.size());
      //for (Iterator<?> it = descriptors.iterator(); it.hasNext();) {
      //  final CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
      //  if (cat.hasComputers()) {
      //    IAction caAction = new SpecificContentAssistAction(cat);
      //    caMenu.add(caAction);
      //    specificAssistActions.add(caAction);
      //  }
      //}
      //fSpecificAssistActions = specificAssistActions.toArray(new SpecificContentAssistAction[specificAssistActions.size()]);
      //if (fSpecificAssistActions.length > 0) {
      //  fContentAssistMenuListener = new MenuListener(caMenu);
      //  caMenu.addMenuListener(fContentAssistMenuListener);
      //}
      //caMenu.add(new Separator("context_info")); //$NON-NLS-1$
      //caMenu.add(fContextInformation);

      //editMenu.appendToGroup(ITextEditorActionConstants.GROUP_ASSIST, fQuickAssistAction);
    }
  }

  /*
   * @see com.google.dart.tools.ui.editor.BasicJavaEditorActionContributor#dispose()
   */
  @Override
  public void dispose() {
    if (fRetargetContentAssist != null) {
      fRetargetContentAssist.dispose();
      fRetargetContentAssist = null;
    }
    if (fContentAssistMenuListener != null) {
      fContentAssistMenuListener.dispose();
      fContentAssistMenuListener = null;
    }
    super.dispose();
  }

  /*
   * @see IEditorActionBarContributor#init(IActionBars, IWorkbenchPage)
   */
  @Override
  public void init(IActionBars bars, IWorkbenchPage page) {
    super.init(bars, page);
    // register actions that have a dynamic editor.
    bars.setGlobalActionHandler(JdtActionConstants.CONTENT_ASSIST, fContentAssist);
  }

  /*
   * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
   */
  @Override
  public void setActiveEditor(IEditorPart part) {
    super.setActiveEditor(part);

    ITextEditor textEditor = null;
    if (part instanceof ITextEditor) {
      textEditor = (ITextEditor) part;
    }

    fContentAssist.setAction(getAction(textEditor, "ContentAssistProposal")); //$NON-NLS-1$
    fContextInformation.setAction(getAction(textEditor, "ContentAssistContextInformation")); //$NON-NLS-1$
    fQuickAssistAction.setAction(getAction(textEditor, ITextEditorActionConstants.QUICK_ASSIST));

    if (fSpecificAssistActions != null) {
      for (int i = 0; i < fSpecificAssistActions.length; i++) {
        SpecificContentAssistAction assistAction = fSpecificAssistActions[i];
        assistAction.setActiveEditor(part);
      }
    }

    //fChangeEncodingAction.setAction(getAction(textEditor,
    //    ITextEditorActionConstants.CHANGE_ENCODING));

    IActionBars actionBars = getActionBars();
    actionBars.setGlobalActionHandler(JdtActionConstants.SHIFT_RIGHT,
        getAction(textEditor, "ShiftRight")); //$NON-NLS-1$
    actionBars.setGlobalActionHandler(JdtActionConstants.SHIFT_LEFT,
        getAction(textEditor, "ShiftLeft")); //$NON-NLS-1$

    actionBars.setGlobalActionHandler(IDEActionFactory.ADD_TASK.getId(),
        getAction(textEditor, IDEActionFactory.ADD_TASK.getId()));
    actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(),
        getAction(textEditor, IDEActionFactory.BOOKMARK.getId()));
  }
}

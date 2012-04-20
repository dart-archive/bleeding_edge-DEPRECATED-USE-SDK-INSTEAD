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

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.IContextMenuConstants;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.DartQuickMenuCreator;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Action group that adds refactor actions (for example 'Rename', 'Move') to a context menu and the
 * global menu bar.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RefactorActionGroup extends ActionGroup {

  private static class NoActionAvailable extends Action {
    public NoActionAvailable() {
      setEnabled(true);
      setText(RefactoringMessages.RefactorActionGroup_no_refactoring_available);
    }
  }

  private static final String PERF_REFACTOR_ACTION_GROUP = "com.google.dart.tools.ui/perf/explorer/RefactorActionGroup"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the refactor sub menu (value
   * <code>com.google.dart.tools.ui.refactoring.menu</code>).
   */
  public static final String MENU_ID = "com.google.dart.tools.ui.refactoring.menu"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the reorg group of the refactor sub menu (value <code>reorgGroup</code>).
   */
  public static final String GROUP_REORG = "reorgGroup"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the type group of the refactor sub menu (value <code>typeGroup</code>).
   */
  public static final String GROUP_TYPE = "typeGroup"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the coding group of the refactor sub menu (value <code>codingGroup</code>).
   */
  public static final String GROUP_CODING = "codingGroup"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the coding group 2 of the refactor sub menu (value <code>codingGroup2</code>
   * ).
   */
  public static final String GROUP_CODING2 = "codingGroup2"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the reorg group 2 of the refactor sub menu (value <code>reorgGroup2</code>).
   */
  public static final String GROUP_REORG2 = "reorgGroup2"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the type group 2 of the refactor sub menu (value <code>typeGroup2</code>).
   */
  public static final String GROUP_TYPE2 = "typeGroup2"; //$NON-NLS-1$

  /**
   * Pop-up menu: id of the type group 2 of the refactor sub menu (value <code>typeGroup3</code>).
   */
  public static final String GROUP_TYPE3 = "typeGroup3"; //$NON-NLS-1$
  private final IWorkbenchSite fSite;
  private DartEditor fEditor;
  private String fGroupName = IContextMenuConstants.GROUP_REORGANIZE;

//  private SelectionDispatchAction fMoveAction;
  private final SelectionDispatchAction fRenameAction;
//  private SelectionDispatchAction fModifyParametersAction;
//  private SelectionDispatchAction fConvertAnonymousToNestedAction;
//
//  private SelectionDispatchAction fConvertNestedToTopAction;
//  private SelectionDispatchAction fPullUpAction;
//  private SelectionDispatchAction fPushDownAction;
//  private SelectionDispatchAction fExtractInterfaceAction;
//  private SelectionDispatchAction fExtractSupertypeAction;
//  private SelectionDispatchAction fChangeTypeAction;
//  private SelectionDispatchAction fUseSupertypeAction;
//
//  private SelectionDispatchAction fInferTypeArgumentsAction;
//  private SelectionDispatchAction fInlineAction;
//  //private SelectionDispatchAction fReplaceInvocationsAction;
//  private SelectionDispatchAction fIntroduceIndirectionAction;
//  private SelectionDispatchAction fExtractMethodAction;
//  private SelectionDispatchAction fExtractTempAction;
//  private SelectionDispatchAction fExtractConstantAction;
//  private SelectionDispatchAction fExtractClassAction;
//  private SelectionDispatchAction fIntroduceParameterAction;
//  private SelectionDispatchAction fIntroduceParameterObjectAction;
//  private SelectionDispatchAction fIntroduceFactoryAction;
//  private SelectionDispatchAction fConvertLocalToFieldAction;
//
//  private SelectionDispatchAction fSelfEncapsulateField;

  private UndoRedoActionGroup fUndoRedoActionGroup;

  private final List<SelectionDispatchAction> fActions = new ArrayList<SelectionDispatchAction>();

  private static final String QUICK_MENU_ID = "com.google.dart.tools.ui.edit.text.java.refactor.quickMenu"; //$NON-NLS-1$
  private IHandlerActivation fQuickAccessHandlerActivation;

  private IHandlerService fHandlerService;

  private final Action fNoActionAvailable = new NoActionAvailable();

  private final ISelectionProvider fSelectionProvider;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @param groupName the group name to add the actions to
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public RefactorActionGroup(DartEditor editor, String groupName) {

    final PerformanceStats stats = PerformanceStats.getStats(PERF_REFACTOR_ACTION_GROUP, this);
    stats.startRun();

    fSite = editor.getEditorSite();
    fSelectionProvider = fSite.getSelectionProvider();
    fEditor = editor;
    fGroupName = groupName;

    ISelectionProvider provider = editor.getSelectionProvider();
    ISelection selection = provider.getSelection();

    fRenameAction = new RenameAction(editor);
    initAction(fRenameAction, selection, DartEditorActionDefinitionIds.RENAME_ELEMENT);
    editor.setAction("RenameElement", fRenameAction); //$NON-NLS-1$

//    fMoveAction= new MoveAction(editor);
//    initAction(fMoveAction, selection, IJavaEditorActionDefinitionIds.MOVE_ELEMENT);
//    editor.setAction("MoveElement", fMoveAction); //$NON-NLS-1$
//
//    fModifyParametersAction= new ModifyParametersAction(editor);
//    initAction(fModifyParametersAction, selection, IJavaEditorActionDefinitionIds.MODIFY_METHOD_PARAMETERS);
//    editor.setAction("ModifyParameters", fModifyParametersAction); //$NON-NLS-1$
//
//    fConvertAnonymousToNestedAction= new ConvertAnonymousToNestedAction(editor);
//    initUpdatingAction(fConvertAnonymousToNestedAction, provider, null, selection, IJavaEditorActionDefinitionIds.CONVERT_ANONYMOUS_TO_NESTED);
//    editor.setAction("ConvertAnonymousToNested", fConvertAnonymousToNestedAction); //$NON-NLS-1$
//
//    fConvertNestedToTopAction= new ConvertNestedToTopAction(editor);
//    initAction(fConvertNestedToTopAction, selection, IJavaEditorActionDefinitionIds.MOVE_INNER_TO_TOP);
//    editor.setAction("MoveInnerToTop", fConvertNestedToTopAction); //$NON-NLS-1$
//
//    fPullUpAction= new PullUpAction(editor);
//    initAction(fPullUpAction, selection, IJavaEditorActionDefinitionIds.PULL_UP);
//    editor.setAction("PullUp", fPullUpAction); //$NON-NLS-1$
//
//    fPushDownAction= new PushDownAction(editor);
//    initAction(fPushDownAction, selection, IJavaEditorActionDefinitionIds.PUSH_DOWN);
//    editor.setAction("PushDown", fPushDownAction); //$NON-NLS-1$
//
//    fExtractSupertypeAction= new ExtractSuperClassAction(editor);
//    initAction(fExtractSupertypeAction, selection, ExtractSuperClassAction.EXTRACT_SUPERTYPE);
//    editor.setAction("ExtractSupertype", fExtractSupertypeAction); //$NON-NLS-1$
//
//    fExtractInterfaceAction= new ExtractInterfaceAction(editor);
//    initAction(fExtractInterfaceAction, selection, IJavaEditorActionDefinitionIds.EXTRACT_INTERFACE);
//    editor.setAction("ExtractInterface", fExtractInterfaceAction); //$NON-NLS-1$
//
//    fExtractClassAction= new ExtractClassAction(editor);
//    initAction(fExtractClassAction, selection, IJavaEditorActionDefinitionIds.EXTRACT_CLASS);
//    editor.setAction("ExtractClass", fExtractClassAction); //$NON-NLS-1$
//
//    fChangeTypeAction= new ChangeTypeAction(editor);
//    initUpdatingAction(fChangeTypeAction, provider, null, selection, IJavaEditorActionDefinitionIds.CHANGE_TYPE);
//    editor.setAction("ChangeType", fChangeTypeAction); //$NON-NLS-1$
//
//    fInferTypeArgumentsAction= new InferTypeArgumentsAction(editor);
//    initAction(fInferTypeArgumentsAction, selection, IJavaEditorActionDefinitionIds.INFER_TYPE_ARGUMENTS_ACTION);
//    editor.setAction("InferTypeArguments", fInferTypeArgumentsAction); //$NON-NLS-1$
//
//    fExtractMethodAction= new ExtractMethodAction(editor);
//    initUpdatingAction(fExtractMethodAction, provider, null, selection, IJavaEditorActionDefinitionIds.EXTRACT_METHOD);
//    editor.setAction("ExtractMethod", fExtractMethodAction); //$NON-NLS-1$
//
//    fExtractTempAction= new ExtractTempAction(editor);
//    initUpdatingAction(fExtractTempAction, provider, null, selection, IJavaEditorActionDefinitionIds.EXTRACT_LOCAL_VARIABLE);
//    editor.setAction("ExtractLocalVariable", fExtractTempAction); //$NON-NLS-1$
//
//    fExtractConstantAction= new ExtractConstantAction(editor);
//    initUpdatingAction(fExtractConstantAction, provider, null, selection, IJavaEditorActionDefinitionIds.EXTRACT_CONSTANT);
//    editor.setAction("ExtractConstant", fExtractConstantAction); //$NON-NLS-1$
//
//    fIntroduceParameterAction= new IntroduceParameterAction(editor);
//    initUpdatingAction(fIntroduceParameterAction, provider, null, selection, IJavaEditorActionDefinitionIds.INTRODUCE_PARAMETER);
//    editor.setAction("IntroduceParameter", fIntroduceParameterAction); //$NON-NLS-1$
//
//    fIntroduceFactoryAction= new IntroduceFactoryAction(editor);
//    initUpdatingAction(fIntroduceFactoryAction, provider, null, selection, IJavaEditorActionDefinitionIds.INTRODUCE_FACTORY);
//    editor.setAction("IntroduceFactory", fIntroduceFactoryAction); //$NON-NLS-1$
//
//    fConvertLocalToFieldAction= new ConvertLocalToFieldAction(editor);
//    initUpdatingAction(fConvertLocalToFieldAction, provider, null, selection, IJavaEditorActionDefinitionIds.PROMOTE_LOCAL_VARIABLE);
//    editor.setAction("PromoteTemp", fConvertLocalToFieldAction); //$NON-NLS-1$
//
//    fSelfEncapsulateField= new SelfEncapsulateFieldAction(editor);
//    initAction(fSelfEncapsulateField, selection, IJavaEditorActionDefinitionIds.SELF_ENCAPSULATE_FIELD);
//    editor.setAction("SelfEncapsulateField", fSelfEncapsulateField); //$NON-NLS-1$
//
//    fIntroduceParameterObjectAction= new IntroduceParameterObjectAction(editor);
//    initAction(fIntroduceParameterObjectAction, selection, IJavaEditorActionDefinitionIds.INTRODUCE_PARAMETER_OBJECT);
//    editor.setAction("IntroduceParameterObjectAction", fIntroduceParameterObjectAction); //$NON-NLS-1$
//
//    fIntroduceIndirectionAction= new IntroduceIndirectionAction(editor);
//    initUpdatingAction(fIntroduceIndirectionAction, provider, null, selection, IJavaEditorActionDefinitionIds.INTRODUCE_INDIRECTION);
//    editor.setAction("IntroduceIndirection", fIntroduceIndirectionAction); //$NON-NLS-1$
//
//    fUseSupertypeAction= new UseSupertypeAction(editor);
//    initAction(fUseSupertypeAction, selection, IJavaEditorActionDefinitionIds.USE_SUPERTYPE);
//    editor.setAction("UseSupertype", fUseSupertypeAction); //$NON-NLS-1$
//
//    fInlineAction= new InlineAction(editor);
//    initAction(fInlineAction, selection, IJavaEditorActionDefinitionIds.INLINE);
//    editor.setAction("Inline", fInlineAction); //$NON-NLS-1$

    installQuickAccessAction();

    stats.endRun();
  }

  /**
   * Creates a new <code>RefactorActionGroup</code>. The group requires that the selection provided
   * by the part's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param part the view part that owns this action group
   */
  public RefactorActionGroup(IViewPart part) {
    this(part.getSite(), null);

    IUndoContext workspaceContext = (IUndoContext) ResourcesPlugin.getWorkspace().getAdapter(
        IUndoContext.class);
    fUndoRedoActionGroup = new UndoRedoActionGroup(part.getViewSite(), workspaceContext, true);

    installQuickAccessAction();
  }

  /**
   * Creates a new <code>RefactorActionGroup</code>. The group requires that the selection provided
   * by the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param site the site that will own the action group.
   * @param selectionProvider the selection provider used instead of the page selection provider.
   */
  public RefactorActionGroup(IWorkbenchSite site, ISelectionProvider selectionProvider) {

    final PerformanceStats stats = PerformanceStats.getStats(PERF_REFACTOR_ACTION_GROUP, this);
    stats.startRun();

    fSite = site;
    fSelectionProvider = selectionProvider == null
        ? fSite.getSelectionProvider()
        : selectionProvider;
    ISelection selection = fSelectionProvider.getSelection();

    fRenameAction = new RenameAction(fSite);
    initUpdatingAction(
        fRenameAction,
        fSelectionProvider,
        selectionProvider,
        selection,
        DartEditorActionDefinitionIds.RENAME_ELEMENT);

//    fMoveAction= new MoveAction(fSite);
//    initUpdatingAction(fMoveAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.MOVE_ELEMENT);
//
//    fModifyParametersAction= new ModifyParametersAction(fSite);
//    initUpdatingAction(fModifyParametersAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.MODIFY_METHOD_PARAMETERS);
//
//    fPullUpAction= new PullUpAction(fSite);
//    initUpdatingAction(fPullUpAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.PULL_UP);
//
//    fPushDownAction= new PushDownAction(fSite);
//    initUpdatingAction(fPushDownAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.PUSH_DOWN);
//
//    fSelfEncapsulateField= new SelfEncapsulateFieldAction(fSite);
//    initUpdatingAction(fSelfEncapsulateField, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.SELF_ENCAPSULATE_FIELD);
//
//    fIntroduceParameterObjectAction= new IntroduceParameterObjectAction(fSite);
//    initUpdatingAction(fIntroduceParameterObjectAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_PARAMETER_OBJECT);
//
//    fExtractSupertypeAction= new ExtractSuperClassAction(fSite);
//    initUpdatingAction(fExtractSupertypeAction, fSelectionProvider, selectionProvider, selection, ExtractSuperClassAction.EXTRACT_SUPERTYPE);
//
//    fExtractInterfaceAction= new ExtractInterfaceAction(fSite);
//    initUpdatingAction(fExtractInterfaceAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.EXTRACT_INTERFACE);
//
//    fExtractClassAction= new ExtractClassAction(fSite);
//    initUpdatingAction(fExtractClassAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.EXTRACT_CLASS);
//
//    fChangeTypeAction= new ChangeTypeAction(fSite);
//    initUpdatingAction(fChangeTypeAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.CHANGE_TYPE);
//
//    fConvertNestedToTopAction= new ConvertNestedToTopAction(fSite);
//    initUpdatingAction(fConvertNestedToTopAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.MOVE_INNER_TO_TOP);
//
//    fInferTypeArgumentsAction= new InferTypeArgumentsAction(fSite);
//    initUpdatingAction(fInferTypeArgumentsAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.INFER_TYPE_ARGUMENTS_ACTION);
//
//    fIntroduceFactoryAction= new IntroduceFactoryAction(fSite);
//    initUpdatingAction(fIntroduceFactoryAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_FACTORY);
//
//    fConvertAnonymousToNestedAction= new ConvertAnonymousToNestedAction(fSite);
//    initUpdatingAction(fConvertAnonymousToNestedAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.CONVERT_ANONYMOUS_TO_NESTED);
//
//    fInlineAction= new InlineAction(fSite);
//    initUpdatingAction(fInlineAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.INLINE);
//
//    fUseSupertypeAction= new UseSupertypeAction(fSite);
//    initUpdatingAction(fUseSupertypeAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.USE_SUPERTYPE);
//
//    fIntroduceIndirectionAction= new IntroduceIndirectionAction(fSite);
//    initUpdatingAction(fIntroduceIndirectionAction, fSelectionProvider, selectionProvider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_INDIRECTION);

    stats.endRun();

    installQuickAccessAction();
  }

  /**
   * Creates a new <code>RefactorActionGroup</code>. The action requires that the selection provided
   * by the page's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param page the page that owns this action group
   */
  public RefactorActionGroup(Page page) {
    this(page.getSite(), null);

    installQuickAccessAction();
  }

  @Override
  public void dispose() {
//    disposeAction(fSelfEncapsulateField, fSelectionProvider);
//    disposeAction(fMoveAction, fSelectionProvider);
    disposeAction(fRenameAction, fSelectionProvider);
//    disposeAction(fModifyParametersAction, fSelectionProvider);
//    disposeAction(fPullUpAction, fSelectionProvider);
//    disposeAction(fPushDownAction, fSelectionProvider);
//    disposeAction(fExtractTempAction, fSelectionProvider);
//    disposeAction(fExtractConstantAction, fSelectionProvider);
//    disposeAction(fIntroduceParameterAction, fSelectionProvider);
//    disposeAction(fIntroduceParameterObjectAction, fSelectionProvider);
//    disposeAction(fIntroduceFactoryAction, fSelectionProvider);
//    disposeAction(fExtractMethodAction, fSelectionProvider);
//    disposeAction(fReplaceInvocationsAction, provider);
//    disposeAction(fExtractInterfaceAction, fSelectionProvider);
//    disposeAction(fExtractClassAction, fSelectionProvider);
//    disposeAction(fExtractSupertypeAction, fSelectionProvider);
//    disposeAction(fChangeTypeAction, fSelectionProvider);
//    disposeAction(fConvertNestedToTopAction, fSelectionProvider);
//    disposeAction(fInferTypeArgumentsAction, fSelectionProvider);
//    disposeAction(fConvertLocalToFieldAction, fSelectionProvider);
//    disposeAction(fConvertAnonymousToNestedAction, fSelectionProvider);
//    disposeAction(fIntroduceIndirectionAction, fSelectionProvider);
//    disposeAction(fInlineAction, fSelectionProvider);
//    disposeAction(fUseSupertypeAction, fSelectionProvider);
    if (fQuickAccessHandlerActivation != null && fHandlerService != null) {
      fHandlerService.deactivateHandler(fQuickAccessHandlerActivation);
    }
    if (fUndoRedoActionGroup != null) {
      fUndoRedoActionGroup.dispose();
    }
    super.dispose();
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
//    actionBars.setGlobalActionHandler(JdtActionConstants.SELF_ENCAPSULATE_FIELD, fSelfEncapsulateField);
//    actionBars.setGlobalActionHandler(JdtActionConstants.MOVE, fMoveAction);
    actionBars.setGlobalActionHandler(JdtActionConstants.RENAME, fRenameAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.MODIFY_PARAMETERS, fModifyParametersAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.PULL_UP, fPullUpAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.PUSH_DOWN, fPushDownAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_TEMP, fExtractTempAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_CONSTANT, fExtractConstantAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.INTRODUCE_PARAMETER, fIntroduceParameterAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.INTRODUCE_FACTORY, fIntroduceFactoryAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_METHOD, fExtractMethodAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.REPLACE_INVOCATIONS, fReplaceInvocationsAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_INTERFACE, fExtractInterfaceAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_CLASS, fExtractClassAction);
//    actionBars.setGlobalActionHandler(ExtractSuperClassAction.EXTRACT_SUPERTYPES, fExtractSupertypeAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.CHANGE_TYPE, fChangeTypeAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.CONVERT_NESTED_TO_TOP, fConvertNestedToTopAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.INFER_TYPE_ARGUMENTS, fInferTypeArgumentsAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.CONVERT_LOCAL_TO_FIELD, fConvertLocalToFieldAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.CONVERT_ANONYMOUS_TO_NESTED, fConvertAnonymousToNestedAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.INTRODUCE_PARAMETER_OBJECT, fIntroduceParameterObjectAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.INLINE, fInlineAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.USE_SUPERTYPE, fUseSupertypeAction);
//    actionBars.setGlobalActionHandler(JdtActionConstants.INTRODUCE_INDIRECTION, fIntroduceIndirectionAction);
    if (fUndoRedoActionGroup != null) {
      fUndoRedoActionGroup.fillActionBars(actionBars);
    }
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    addRefactorSubmenu(menu);
  }

  /**
   * Retargets the File actions with the corresponding refactoring actions.
   * 
   * @param actionBars the action bar to register the move and rename action with
   */
  public void retargetFileMenuActions(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), fRenameAction);
//    actionBars.setGlobalActionHandler(ActionFactory.MOVE.getId(), fMoveAction);
  }

  private int addAction(IMenuManager menu, String group, IAction action) {
    if (action != null && action.isEnabled()) {
      menu.appendToGroup(group, action);
      return 1;
    }
    return 0;
  }

  private void addRefactorSubmenu(IMenuManager menu) {
    // TODO(scheglov) really add "Refactoring" sub-menu
//    MenuManager refactorSubmenu = new MenuManager(ActionMessages.RefactorMenu_label, MENU_ID);
//    refactorSubmenu.setActionDefinitionId(QUICK_MENU_ID);
    if (fEditor != null) {
      final DartElement element = getEditorInput();
      if (element != null && ActionUtil.isOnBuildPath(element)) {
//        refactorSubmenu.addMenuListener(new IMenuListener() {
//          @Override
//          public void menuAboutToShow(IMenuManager manager) {
//            refactorMenuShown(manager);
//          }
//        });
//        refactorSubmenu.add(fNoActionAvailable);
//        menu.appendToGroup(fGroupName, refactorSubmenu);
        refactorMenuShown(menu);
      }
    } else {
      ISelection selection = fSelectionProvider.getSelection();
      for (Iterator<SelectionDispatchAction> iter = fActions.iterator(); iter.hasNext();) {
        iter.next().update(selection);
      }
//      if (fillRefactorMenu(refactorSubmenu) > 0) {
//        menu.appendToGroup(fGroupName, refactorSubmenu);
//      }
    }
  }

  private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
    if (action != null) {
      provider.removeSelectionChangedListener(action);
    }
  }

  private void fillQuickMenu(IMenuManager menu) {
    if (fEditor != null) {
//      if (fEditor.isBreadcrumbActive()) {
//        return;
//      }

      DartElement element = getEditorInput();
      if (element == null || !ActionUtil.isOnBuildPath(element)) {
        menu.add(fNoActionAvailable);
        return;
      }
      ITextSelection textSelection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
      DartTextSelection dartSelection = new DartTextSelection(
          element,
          getDocument(),
          textSelection.getOffset(),
          textSelection.getLength());

      for (Iterator<SelectionDispatchAction> iter = fActions.iterator(); iter.hasNext();) {
        iter.next().update(dartSelection);
      }
      fillRefactorMenu(menu);
      for (Iterator<SelectionDispatchAction> iter = fActions.iterator(); iter.hasNext();) {
        iter.next().update(textSelection);
      }

    } else {
      ISelection selection = fSelectionProvider.getSelection();
      for (Iterator<SelectionDispatchAction> iter = fActions.iterator(); iter.hasNext();) {
        iter.next().update(selection);
      }
      fillRefactorMenu(menu);
    }
  }

  private int fillRefactorMenu(IMenuManager refactorSubmenu) {
    int added = 0;
    // TODO(scheglov) restore real "Refactoring" sub-menu
//    refactorSubmenu.add(new Separator(GROUP_REORG));
//    added += addAction(refactorSubmenu, fRenameAction);
    refactorSubmenu.appendToGroup(
        ITextEditorActionConstants.GROUP_EDIT,
        new GroupMarker(fGroupName));
    refactorSubmenu.appendToGroup(fGroupName, new Separator(GROUP_REORG));
    added += addAction(refactorSubmenu, GROUP_REORG, fRenameAction);
//    added += addAction(refactorSubmenu, fMoveAction);

    refactorSubmenu.add(new Separator(GROUP_CODING));
//    added += addAction(refactorSubmenu, fModifyParametersAction);
//    added += addAction(refactorSubmenu, fExtractMethodAction);
//    added += addAction(refactorSubmenu, fExtractTempAction);
//    added += addAction(refactorSubmenu, fExtractConstantAction);
//    added += addAction(refactorSubmenu, fInlineAction);

    refactorSubmenu.add(new Separator(GROUP_REORG2));
//    added += addAction(refactorSubmenu, fConvertAnonymousToNestedAction);
//    added += addAction(refactorSubmenu, fConvertNestedToTopAction);
//    added += addAction(refactorSubmenu, fConvertLocalToFieldAction);

    refactorSubmenu.add(new Separator(GROUP_TYPE));
//    added += addAction(refactorSubmenu, fExtractInterfaceAction);
//    added += addAction(refactorSubmenu, fExtractSupertypeAction);
//    added += addAction(refactorSubmenu, fUseSupertypeAction);
//    added += addAction(refactorSubmenu, fPullUpAction);
//    added += addAction(refactorSubmenu, fPushDownAction);

    refactorSubmenu.add(new Separator(GROUP_TYPE2));
//    added += addAction(refactorSubmenu, fExtractClassAction);
//    added += addAction(refactorSubmenu, fIntroduceParameterObjectAction);

    refactorSubmenu.add(new Separator(GROUP_CODING2));
//    added += addAction(refactorSubmenu, fIntroduceIndirectionAction);
//    added += addAction(refactorSubmenu, fIntroduceFactoryAction);
//    added += addAction(refactorSubmenu, fIntroduceParameterAction);
//    added += addAction(refactorSubmenu, fSelfEncapsulateField);
//    added+= addAction(refactorSubmenu, fReplaceInvocationsAction);

    refactorSubmenu.add(new Separator(GROUP_TYPE3));
//    added += addAction(refactorSubmenu, fChangeTypeAction);
//    added += addAction(refactorSubmenu, fInferTypeArgumentsAction);
    return added;
  }

  private IDocument getDocument() {
    return DartUI.getDocumentProvider().getDocument(fEditor.getEditorInput());
  }

  private DartElement getEditorInput() {
    return DartUI.getEditorInputDartElement(fEditor.getEditorInput());
  }

  private void initAction(
      SelectionDispatchAction action,
      ISelection selection,
      String actionDefinitionId) {
    initUpdatingAction(action, null, null, selection, actionDefinitionId);
  }

  /**
   * Sets actionDefinitionId, updates enablement, adds to fActions, and adds selection changed
   * listener if provider is not <code>null</code>.
   * 
   * @param action the action
   * @param provider can be <code>null</code>
   * @param specialProvider a special selection provider or <code>null</code>
   * @param selection the selection
   * @param actionDefinitionId the action definition id
   */
  private void initUpdatingAction(
      SelectionDispatchAction action,
      ISelectionProvider provider,
      ISelectionProvider specialProvider,
      ISelection selection,
      String actionDefinitionId) {
    action.setActionDefinitionId(actionDefinitionId);
    action.update(selection);
    if (provider != null) {
      provider.addSelectionChangedListener(action);
    }
    if (specialProvider != null) {
      action.setSpecialSelectionProvider(specialProvider);
    }
    fActions.add(action);
  }

  private void installQuickAccessAction() {
    fHandlerService = (IHandlerService) fSite.getService(IHandlerService.class);
    if (fHandlerService != null) {
      IHandler handler = new DartQuickMenuCreator(fEditor) {
        @Override
        protected void fillMenu(IMenuManager menu) {
          fillQuickMenu(menu);
        }
      }.createHandler();
      fQuickAccessHandlerActivation = fHandlerService.activateHandler(QUICK_MENU_ID, handler);
    }
  }

//  private void refactorMenuHidden() {
//    ITextSelection textSelection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
//    for (Iterator<SelectionDispatchAction> iter = fActions.iterator(); iter.hasNext();) {
//      SelectionDispatchAction action = iter.next();
//      action.update(textSelection);
//    }
//  }

  private void refactorMenuShown(IMenuManager refactorSubmenu) {
    // TODO(scheglov) really add "Refactoring" sub-menu
    // we know that we have an MenuManager since we created it in addRefactorSubmenu()
//    Menu menu = ((MenuManager) refactorSubmenu).getMenu();
//    menu.addMenuListener(new MenuAdapter() {
//      @Override
//      public void menuHidden(MenuEvent e) {
//        refactorMenuHidden();
//      }
//    });
    ITextSelection textSelection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
    DartTextSelection dartSelection = new DartTextSelection(
        getEditorInput(),
        getDocument(),
        textSelection.getOffset(),
        textSelection.getLength());

    for (Iterator<SelectionDispatchAction> iter = fActions.iterator(); iter.hasNext();) {
      SelectionDispatchAction action = iter.next();
      action.update(dartSelection);
    }
//    refactorSubmenu.removeAll();
//    if (fillRefactorMenu(refactorSubmenu) == 0) {
//      refactorSubmenu.add(fNoActionAvailable);
//    }
    fillRefactorMenu(refactorSubmenu);
  }

}

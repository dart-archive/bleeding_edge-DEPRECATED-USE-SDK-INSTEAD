/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartResource;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.ui.actions.CloseLibraryAction;
import com.google.dart.tools.ui.actions.NavigateActionGroup;
import com.google.dart.tools.ui.actions.OpenExternalFileDialogAction;
import com.google.dart.tools.ui.actions.OpenNewApplicationWizardAction;
import com.google.dart.tools.ui.actions.OpenNewFileWizardAction;
import com.google.dart.tools.ui.actions.RunInBrowserAction;
import com.google.dart.tools.ui.internal.actions.CollapseAllAction;
import com.google.dart.tools.ui.internal.text.editor.CompositeActionGroup;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.handlers.CollapseAllHandler;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * The {@link CompositeActionGroup} for the {@link LibraryExplorerPart}. This action group brings
 * together actions and action groups contribute actions such as New, Cut, Copy, Paste, Delete,
 * Collapse All, and Link With Editor to the Libraries Explorer View.;
 * 
 * @see CompositeActionGroup
 * @see LibraryExplorerPart
 */
public final class LibraryExplorerActionGroup extends ActionGroup {

  private LibraryExplorerPart part;

  private CollapseAllAction collapseAllAction;

//  private SelectAllAction selectAllAction;

  private ToggleLinkingAction toggleLinkingAction;

//  private RefactorActionGroup refactorActionGroup;

  private NavigateActionGroup navigateActionGroup;

  private RunInBrowserAction runInBrowserAction;

  private CloseLibraryAction closeLibraryAction;

  private OpenNewFileWizardAction newFileWizardAction;

  private OpenNewApplicationWizardAction newApplicationWizardAction;

  private OpenExternalFileDialogAction openFileAction;

  public LibraryExplorerActionGroup(LibraryExplorerPart part) {
    //super();
    this.part = part;
    // TODO: Fix this - do we have to actually create the NavigatorActionGroup?
    navigateActionGroup = new NavigateActionGroup(part);
//    setGroups(new ActionGroup[] {
//        new NewWizardsActionGroup(part), navigateActionGroup = new NavigateActionGroup(part),
//      new CCPActionGroup(part),
//            new GenerateBuildPathActionGroup(fPart),
//      new GenerateActionGroup(fPart),
//      refactorActionGroup= new RefactorActionGroup(fPart),
//      new ImportActionGroup(fPart),
//      new BuildActionGroup(fPart),
//      new JavaSearchActionGroup(fPart),
//      fProjectActionGroup= new ProjectActionGroup(fPart),
//      fViewActionGroup= new ViewActionGroup(fPart.getRootMode(), workingSetListener, site),
//      fCustomFiltersActionGroup= new CustomFiltersActionGroup(fPart, viewer),
//      new LayoutActionGroup(fPart)
//    });

//    fViewActionGroup.fillFilters(viewer);

    newFileWizardAction = new OpenNewFileWizardAction();
    newApplicationWizardAction = new OpenNewApplicationWizardAction();
    openFileAction = new OpenExternalFileDialogAction(part.getSite().getWorkbenchWindow());

    runInBrowserAction = new RunInBrowserAction(part.getSite().getWorkbenchWindow());
    closeLibraryAction = new CloseLibraryAction(part.getSite().getWorkbenchWindow());
    closeLibraryAction.setText(LibraryExplorerMessages.LibraryExplorer_close_library_action);

    // Global View Actions

    // Collapse All
    collapseAllAction = new CollapseAllAction(part.getTreeViewer());
    collapseAllAction.setActionDefinitionId(CollapseAllHandler.COMMAND_ID);

    // Toggle Linking
    toggleLinkingAction = new ToggleLinkingAction(part);
    toggleLinkingAction.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR);

    // TODO: Doc this
    //registerActions(part.getSite());

//    selectAllAction = new SelectAllAction(fPart.getTreeViewer());
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    setGlobalActionHandlers(actionBars);
    fillToolBar(actionBars.getToolBarManager());
    fillViewMenu(actionBars.getMenuManager());
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);

    ISelection selection = getContext().getSelection();

    if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
      // Nothing selected - go with Open File and Open Application
      menu.add(newFileWizardAction);
      menu.add(newApplicationWizardAction);
      menu.add(new Separator());
      menu.add(openFileAction);
      return;
    }

    IStructuredSelection ss = (IStructuredSelection) selection;
    if (ss.size() != 1) {
      for (Object o : ss.toList()) {
        if (!(o instanceof DartLibrary)) {
          return;
        }
      }
      menu.add(closeLibraryAction);
      return;
    }

    Object o = ss.getFirstElement();
    if (!(o instanceof IAdaptable)) {
      return;
    }

    IAdaptable element = (IAdaptable) o;

    if (element instanceof DartLibrary) {
      // Library container
      menu.add(newFileWizardAction);
      menu.add(new Separator());
      menu.add(closeLibraryAction);
      return;
    }

    if (element instanceof CompilationUnit) {
      // .dart file
      menu.add(navigateActionGroup.getOpenAction());
      return;
    }

    if (element instanceof HTMLFile) {
      menu.add(navigateActionGroup.getOpenAction());
      menu.add(new Separator());
      menu.add(runInBrowserAction);
      return;
    }

    if (element instanceof DartResource) {
      menu.add(navigateActionGroup.getOpenAction());
      return;
    }
  }

  /**
   * This handles double click actions to expand or collapse expandable elements in the Library
   * Explorer.
   */
  void handleDoubleClick(DoubleClickEvent event) {
    TreeViewer viewer = part.getTreeViewer();
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    Object element = selection.getFirstElement();
    if (viewer.isExpandable(element)) {

      IAction openAction = navigateActionGroup.getOpenAction();
      if (openAction != null && openAction.isEnabled()
          && OpenStrategy.getOpenMethod() == OpenStrategy.DOUBLE_CLICK) {
        return;
      }
      if (selection instanceof ITreeSelection) {
        TreePath[] paths = ((ITreeSelection) selection).getPathsFor(element);
        for (int i = 0; i < paths.length; i++) {
          viewer.setExpandedState(paths[i], !viewer.getExpandedState(paths[i]));
        }
      } else {
        viewer.setExpandedState(element, !viewer.getExpandedState(element));
      }
    }
    // if the Library Explorer ever has projects as top level elements, we may need to revisit this
//    else if (element instanceof IProject && !((IProject) element).isOpen()) {
//      OpenProjectAction openProjectAction = fProjectActionGroup.getOpenProjectAction();
//      if (openProjectAction.isEnabled()) {
//        openProjectAction.run();
//      }
//    }
  }

  /**
   * Called by Library Explorer.
   * 
   * @param event the open event
   * @param activate <code>true</code> if the opened editor should be activated
   */
  void handleOpen(ISelection event, boolean activate) {
    IAction openAction = navigateActionGroup.getOpenAction();
    if (openAction != null && openAction.isEnabled()) {
      openAction.run();
      return;
    }
  }

  private void fillToolBar(IToolBarManager toolBar) {
    toolBar.add(collapseAllAction);
    toolBar.add(toggleLinkingAction);
    toolBar.update(true);
  }

  /**
   * The view's menu currently doesn't list out anything.
   */
  private void fillViewMenu(IMenuManager menu) {
  }

  /**
   * None of the actions here need to be bound
   */
  private void registerActions(IWorkbenchPartSite site) {
    //ISelectionProvider provider = site.getSelectionProvider();

    //provider.addSelectionChangedListener(runInBrowserAction);
    //provider.addSelectionChangedListener(closeLibraryAction);

    //site.getPage().addPartListener(runInBrowserAction);
  }

  private void setGlobalActionHandlers(IActionBars actionBars) {

    // These are not necessary because there is no change in behavior for these actions
    // for each different view.
    //actionBars.setGlobalActionHandler(RunInBrowserAction.ACTION_ID, runInBrowserAction);
    //actionBars.setGlobalActionHandler(CloseLibraryAction.ID, closeLibraryAction);

    IHandlerService handlerService = (IHandlerService) part.getViewSite().getService(
        IHandlerService.class);
    handlerService.activateHandler(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR,
        new ActionHandler(toggleLinkingAction));
    handlerService.activateHandler(CollapseAllHandler.COMMAND_ID, new ActionHandler(
        collapseAllAction));
  }

}

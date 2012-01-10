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

package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.ProblemsLabelDecorator;
import com.google.dart.tools.ui.internal.actions.CollapseAllAction;
import com.google.dart.tools.ui.internal.libraryview.DeleteAction;
import com.google.dart.tools.ui.internal.projects.OpenNewProjectWizardAction;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CreateFileAction;
import org.eclipse.ui.actions.CreateFolderAction;
import org.eclipse.ui.actions.MoveResourceAction;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

/**
 * File-oriented view for navigating Dart projects.
 */
@SuppressWarnings("deprecation")
public class FilesView extends ViewPart {

  private TreeViewer treeViewer;

  private IMemento memento;

  private LinkWithEditorAction linkWithEditorAction;
  private MoveResourceAction moveAction;
  private RenameResourceAction renameAction;

  public FilesView() {

  }

  @Override
  public void createPartControl(Composite parent) {
    treeViewer = new TreeViewer(parent);
    treeViewer.setContentProvider(new ResourceContentProvider());
    //TODO (pquitslund): replace with WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider() 
    //when we have the linked resource story straightened out
//    treeViewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    treeViewer.setLabelProvider(new DecoratingLabelProvider(new WorkbenchLabelProvider(),
        new ProblemsLabelDecorator()));
    treeViewer.setComparator(new FilesViewerComparator());
    treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        handleDoubleClick(event);
      }
    });
    treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());

    getSite().setSelectionProvider(treeViewer);

    fillInToolbar(getViewSite().getActionBars().getToolBarManager());

    // Create the TreeViewer's context menu.
    createContextMenu();

    parent.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        linkWithEditorAction.syncSelectionToEditor();
      }
    });

    renameAction = new RenameResourceAction(getShell(), treeViewer.getTree());
    treeViewer.addSelectionChangedListener(renameAction);
    moveAction = new MoveResourceAction(getShell());
    treeViewer.addSelectionChangedListener(moveAction);
  }

  @Override
  public void dispose() {
    if (linkWithEditorAction != null) {
      linkWithEditorAction.dispose();
    }

    super.dispose();
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);

    this.memento = memento;
  }

  @Override
  public void saveState(IMemento memento) {
    memento.putBoolean("linkWithEditor", linkWithEditorAction.getLinkWithEditor());
  }

  @Override
  public void setFocus() {
    treeViewer.getTree().setFocus();
  }

  protected void createContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {
        fillContextMenu(manager);
      }
    });
    Menu menu = menuMgr.createContextMenu(treeViewer.getTree());
    treeViewer.getTree().setMenu(menu);
    getSite().registerContextMenu(menuMgr, treeViewer);
  }

  protected void fillContextMenu(IMenuManager manager) {
    //TODO (pquitslund): replace with our new file wizard when it is updated
    manager.add(new CreateFileAction(getShell()));
    //manager.add(new OpenNewFileWizardAction(getViewSite().getWorkbenchWindow()));
    //manager.add(new OpenNewApplicationWizardAction());
    manager.add(new CreateFolderAction(getShell()));
    manager.add(new OpenNewProjectWizardAction());

    IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
    Object element = selection.getFirstElement();
    if (element instanceof IResource) {
      manager.add(new Separator());
      manager.add(renameAction);
      manager.add(moveAction);
    }
    if (!treeViewer.getSelection().isEmpty() && element instanceof IResource) {
      manager.add(new Separator());
      DeleteAction deleteAction = new DeleteAction(getSite());
      deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
          ISharedImages.IMG_TOOL_DELETE));
      manager.add(deleteAction);
    }
  }

  protected void fillInToolbar(IToolBarManager toolbar) {
    linkWithEditorAction = new LinkWithEditorAction(getViewSite().getPage(), treeViewer);

    if (memento != null && memento.getBoolean("linkWithEditor") != null) {
      linkWithEditorAction.setLinkWithEditor(memento.getBoolean("linkWithEditor").booleanValue());
    } else {
      linkWithEditorAction.setLinkWithEditor(true);
    }

    toolbar.add(new CollapseAllAction(treeViewer));
    toolbar.add(linkWithEditorAction);
  }

  protected void handleDoubleClick(DoubleClickEvent event) {
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    Object element = selection.getFirstElement();

    if (treeViewer.isExpandable(element)) {
      treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element));
    }

    if (element instanceof IFile) {
      try {
        IDE.openEditor(getViewSite().getPage(), (IFile) element);
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    } else if (element instanceof IFileStore) {
      try {
        IDE.openEditorOnFileStore(getViewSite().getPage(), (IFileStore) element);
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

  private Shell getShell() {
    return getSite().getShell();
  }

}

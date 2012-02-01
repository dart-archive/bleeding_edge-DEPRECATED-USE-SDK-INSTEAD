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

package com.google.dart.tools.ui.internal.view.files;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.preferences.DartBasePreferencePage;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.ViewPart;

import java.io.File;

/**
 * File-oriented view for navigating Dart projects.
 * <p>
 * TODO hide hidden files and directories --> create a local private static boolean
 * <p>
 * TODO we'll want to persist both a list of directories, list of expanded to directories, and a
 * list of files
 */
public class FilesView extends ViewPart implements ISetSelectionTarget {

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (treeViewer != null) {
        if (DartBasePreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          updateTreeFont();
        }
      }
    }
  }

  private LabelProvider fileLabelProvider = new LabelProvider() {
    @Override
    public String getText(Object element) {
      if (element instanceof File) {
        return ((File) element).getName();
      }
      return element == null ? "" : element.toString();//$NON-NLS-1$
    }
  };

  private TreeViewer treeViewer;

  private IMemento memento;

  /**
   * A final static String for the Link with Editor momento.
   */
//  private static final String LINK_WITH_EDITOR_ID = "linkWithEditor";
//
//  private LinkWithEditorAction linkWithEditorAction;
//  private MoveResourceAction moveAction;
//  private RenameResourceAction renameAction;
//  private DeleteAction deleteAction;
//  private OpenNewFileWizardAction createFileAction;

  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();

  public FilesView() {
  }

  @Override
  public void createPartControl(Composite parent) {
    treeViewer = new TreeViewer(parent);
    treeViewer.setContentProvider(new FilesContentProvider());
    treeViewer.setInput(new TopLevelDirectoriesWrapper());
    //TODO (pquitslund): replace with WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider() 
    // when we have the linked resource story straightened out
//    treeViewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    treeViewer.setLabelProvider(fileLabelProvider);
//    treeViewer.setComparator(new FilesViewerComparator());
    treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        handleDoubleClick(event);
      }
    });
//    treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());

    getSite().setSelectionProvider(treeViewer);

    fillInToolbar(getViewSite().getActionBars().getToolBarManager());

    // Create the TreeViewer's context menu.
    createContextMenu();

    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    updateTreeFont();
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);
    this.memento = memento;
  }

  @Override
  public void saveState(IMemento memento) {
    //memento.putBoolean(LINK_WITH_EDITOR_ID, linkWithEditorAction.getLinkWithEditor());
  }

  @Override
  public void selectReveal(ISelection selection) {
    treeViewer.setSelection(selection);
  }

  @Override
  public void setFocus() {
    treeViewer.getTree().setFocus();
  }

  protected void createContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
//    menuMgr.setRemoveAllWhenShown(true);
//    menuMgr.addMenuListener(new IMenuListener() {
//      @Override
//      public void menuAboutToShow(IMenuManager manager) {
//        fillContextMenu(manager);
//      }
//    });
    Menu menu = menuMgr.createContextMenu(treeViewer.getTree());
    treeViewer.getTree().setMenu(menu);
    getSite().registerContextMenu(menuMgr, treeViewer);
  }

  @SuppressWarnings("rawtypes")
  protected void fillContextMenu(IMenuManager manager) {

  }

  protected void fillInToolbar(IToolBarManager toolbar) {

    // Link with Editor

//    linkWithEditorAction = new LinkWithEditorAction(getViewSite().getPage(), treeViewer);
//
//    if (memento != null && memento.getBoolean(LINK_WITH_EDITOR_ID) != null) {
//      linkWithEditorAction.setLinkWithEditor(memento.getBoolean(LINK_WITH_EDITOR_ID).booleanValue());
//    } else {
//      linkWithEditorAction.setLinkWithEditor(true);
//    }
//
//    // Collapse All
//
//    toolbar.add(new CollapseAllAction(treeViewer));
//    toolbar.add(linkWithEditorAction);
  }

  protected void handleDoubleClick(DoubleClickEvent event) {
    System.out.println("FilesView.handleDoubleClick()");

    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    Object element = selection.getFirstElement();

    if (treeViewer.isExpandable(element)) {
      treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element));
    }

    if (element instanceof File) {
      File file = (File) element;
      // if there is already an IResource for this file, open it
      IFile[] resources = ResourceUtil.getResources(file);
      IFile iFile = null;
      // If already in the model, just open the IFile
      if (resources.length == 1) {
        iFile = resources[0];
      }

      if (DartCore.isDartLikeFileName(file.getName())) {
        resources = ResourceUtil.getResources(file);
        // If already in the model, just open the IFile
        if (resources.length == 1) {
          iFile = resources[0];
        } else {
          // otherwise create call openLibrary to create the IFile
          try {
            DartLibrary dartLibrary = DartCore.openLibrary(file, new NullProgressMonitor());
            resources = ResourceUtil.getResources(file);
            if (resources.length == 0) {
              iFile = null;
            } else if (resources.length == 1) {
              iFile = resources[0];
            } else if (dartLibrary != null) {
              for (IFile f : resources) {
                if (f.getProject().equals(dartLibrary.getDartProject().getProject())) {
                  iFile = f;
                }
              }
            }
          } catch (DartModelException e) {
            e.printStackTrace();
          }
        }
      }

      if (iFile != null) {
        try {
          EditorUtility.openInEditor(iFile, true);
        } catch (PartInitException e) {
          e.printStackTrace();
        } catch (DartModelException e) {
          e.printStackTrace();
        }
      } else {
        IWorkbenchPage p = DartToolsPlugin.getActivePage();
        try {
          IDE.openEditor(p, file.toURI(), IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
        } catch (PartInitException e) {
          // system was unable to open the file selected, fall through
        }
      }
    }

//    if (element instanceof IFile) {
//      try {
//        IDE.openEditor(getViewSite().getPage(), (IFile) element);
//      } catch (PartInitException e) {
//        DartToolsPlugin.log(e);
//      }
//    } else if (element instanceof IFileStore) {
//      try {
//        IDE.openEditorOnFileStore(getViewSite().getPage(), (IFileStore) element);
//      } catch (PartInitException e) {
//        DartToolsPlugin.log(e);
//      }
//    }
  }

  protected void updateTreeFont() {
    Font newFont = JFaceResources.getFont(DartBasePreferencePage.BASE_FONT_KEY);
    Font oldFont = treeViewer.getTree().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    treeViewer.getTree().setFont(font);
  }

  private Shell getShell() {
    return getSite().getShell();
  }

}

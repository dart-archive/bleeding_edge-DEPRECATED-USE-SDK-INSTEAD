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
import com.google.dart.tools.core.DirectorySetManager;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartElementComparator;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.actions.CollapseAllAction;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * File-oriented view for navigating Dart projects.
 * <p>
 * TODO(jwren) we still need to persist both a list of directories, list of expanded to directories,
 * and a list of files which were recently opened
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

  private DirectorySetManager model;

  /**
   * A final static String for the Link with Editor memento.
   */
  private static final String LINK_WITH_EDITOR_ID = "linkWithEditor";

  /**
   * A String for the expanded directory set memento.
   */
  private static final String EXPANDED_DIRS = "expandedDirs";

  private LinkWithEditorAction linkWithEditorAction;
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
    treeViewer.setLabelProvider(fileLabelProvider);
    treeViewer.setComparator(new DartElementComparator());
    treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        handleDoubleClick(event);
      }
    });

    treeViewer.setInput(model = DartCore.getDirectorySetManager());

    if (memento != null && memento.getString(EXPANDED_DIRS) != null) {
      File[] expandedDirs = retrieveExpandedElementsFromMemento(memento.getString(EXPANDED_DIRS));
      for (File file : expandedDirs) {
        treeViewer.expandToLevel(file, 1);
      }
    }

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
    // store link with editor setting
    memento.putBoolean(LINK_WITH_EDITOR_ID, linkWithEditorAction.getLinkWithEditor());

    // store expanded elements list
    storeExpandedElements(memento);
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

  protected void fillContextMenu(IMenuManager manager) {

  }

  protected void fillInToolbar(IToolBarManager toolbar) {

    // Link with Editor

    linkWithEditorAction = new LinkWithEditorAction(getViewSite().getPage(), treeViewer);

    if (memento != null && memento.getBoolean(LINK_WITH_EDITOR_ID) != null) {
      linkWithEditorAction.setLinkWithEditor(memento.getBoolean(LINK_WITH_EDITOR_ID).booleanValue());
    } else {
      linkWithEditorAction.setLinkWithEditor(true);
    }

    // Collapse All

    toolbar.add(new CollapseAllAction(treeViewer));
    toolbar.add(linkWithEditorAction);
  }

  protected void handleDoubleClick(DoubleClickEvent event) {
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
          // open the external editor if the File is not a directory (don't want OS to open Finder/Explorer window)
          if (!file.isDirectory()) {
            IDE.openEditor(p, file.toURI(), IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
          }
        } catch (PartInitException e) {
          // system was unable to open the file selected, fall through
        }
      }
    }
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

  private File[] retrieveExpandedElementsFromMemento(String str) {
    if (str == null || str.length() == 0) {
      return new File[0];
    }
    Set<File> fileSet = new HashSet<File>();
    String[] pathArray = str.split(";");
    for (String strPath : pathArray) {
      File file = new File(strPath);
      if (file.exists() && file.isDirectory()) {
        fileSet.add(file);
      }
    }
    return fileSet.toArray(new File[fileSet.size()]);
  }

  private void storeExpandedElements(IMemento memento) {
    // Get the set of expanded elements
    Object[] expandedElements = treeViewer.getExpandedElements();

    // Convert the Object[] into an array of Files, with some checks on the content
    ArrayList<File> fileList = new ArrayList<File>(expandedElements.length);
    for (Object object : expandedElements) {
      if (object instanceof File) {
        fileList.add((File) object);
      }
    }

    // Put the content into a string representation
    String stringCache = "";
    for (int i = 0; i < fileList.size(); i++) {
      stringCache += fileList.get(i).getAbsolutePath();
      if (i + 1 != fileList.size()) {
        stringCache += ';';
      }
    }

    // Finally, put the content into the EXPANDED_DIRS memento
    memento.putString(EXPANDED_DIRS, stringCache);
  }
}

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
package com.google.dart.tools.ui.internal.appsview;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.DeleteAction;
import com.google.dart.tools.ui.internal.actions.CollapseAllAction;
import com.google.dart.tools.ui.internal.filesview.FilesViewDragAdapter;
import com.google.dart.tools.ui.internal.filesview.FilesViewDropAdapter;
import com.google.dart.tools.ui.internal.filesview.LinkWithEditorAction;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.projects.OpenNewApplicationWizardAction;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;

import java.util.ArrayList;
import java.util.List;

// TODO Unify this with Files View -- we should only have one view on files
// TODO Change icon on editor tab to be the same as used in Files view (and possibly use that here)
public class AppsView extends ViewPart implements ISetSelectionTarget {

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (treeViewer != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          updateTreeFont();
          treeViewer.refresh();
        }
      }
    }
  }

  private static final String LINK_WITH_EDITOR_ID = "linkWithEditor";
  //persistence tags
  private static final String TAG_ELEMENT = "element"; //$NON-NLS-1$
  private static final String TAG_EXPANDED = "expanded"; //$NON-NLS-1$
  private static final String TAG_PATH = "path"; //$NON-NLS-1$
  private static final String TAG_SELECTION = "selection"; //$NON-NLS-1$

  private TreeViewer treeViewer;
  private AppLabelProvider appLabelProvider;
  private IMemento memento;
  private Clipboard clipboard;
  private LinkWithEditorAction linkWithEditorAction;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private OpenNewApplicationWizardAction createApplicationAction;
  private DeleteAction deleteAction;
  private UndoRedoActionGroup undoRedoActionGroup;

  @Override
  public void createPartControl(Composite parent) {
    treeViewer = new TreeViewer(parent);
    treeViewer.setContentProvider(new AppsViewContentProvider());
    appLabelProvider = new AppLabelProvider(treeViewer.getTree().getFont());
    treeViewer.setLabelProvider(appLabelProvider);
    treeViewer.setComparator(new AppsViewComparator());
    treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        handleDoubleClick(event);
      }
    });
    treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());

    initDragAndDrop();
    getSite().setSelectionProvider(treeViewer);
    makeActions();
    fillInToolbar(getViewSite().getActionBars().getToolBarManager());
    fillInActionBars();

    // Create the TreeViewer's context menu.
    createContextMenu();

    parent.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        linkWithEditorAction.syncSelectionToEditor();
      }
    });

    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    updateTreeFont();

    restoreState();
  }

  @Override
  public void dispose() {
    if (linkWithEditorAction != null) {
      linkWithEditorAction.dispose();
    }

    if (clipboard != null) {
      clipboard.dispose();
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
    memento.putBoolean(LINK_WITH_EDITOR_ID, linkWithEditorAction.getLinkWithEditor());

    //save expanded elements
    Object expandedElements[] = treeViewer.getVisibleExpandedElements();
    if (expandedElements.length > 0) {
      IMemento expandedMem = memento.createChild(TAG_EXPANDED);
      for (Object element : expandedElements) {
        if (element instanceof IResource) {
          IMemento elementMem = expandedMem.createChild(TAG_ELEMENT);
          elementMem.putString(TAG_PATH, ((IResource) element).getFullPath().toString());
        }
      }
    }

    //save selection
    Object elements[] = ((IStructuredSelection) treeViewer.getSelection()).toArray();
    if (elements.length > 0) {
      IMemento selectionMem = memento.createChild(TAG_SELECTION);
      for (Object element : elements) {
        if (element instanceof IResource) {
          IMemento elementMem = selectionMem.createChild(TAG_ELEMENT);
          elementMem.putString(TAG_PATH, ((IResource) element).getFullPath().toString());
        }
      }
    }
  }

  @Override
  public void selectReveal(ISelection selection) {
    treeViewer.setSelection(selection, true);
  }

  @Override
  public void setFocus() {
    treeViewer.getTree().setFocus();
  }

  protected void createContextMenu() {
  }

  protected void fillContextMenu(IMenuManager manager) {
    manager.add(createApplicationAction);
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

    if (element instanceof CompilationUnit) {
      try {
        CompilationUnit cu = (CompilationUnit) element;
        EditorUtility.openInEditor(cu);
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      } catch (CoreException ex) {
        DartToolsPlugin.log(ex);
      }
    } else if (element instanceof IFileStore) {
      try {
        IDE.openEditorOnFileStore(getViewSite().getPage(), (IFileStore) element);
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

  protected void restoreState() {
    if (memento == null) {
      return;
    }

    IContainer container = ResourcesPlugin.getWorkspace().getRoot();
    //restore expansion
    IMemento childMem = memento.getChild(TAG_EXPANDED);
    if (childMem != null) {
      List<Object> elements = new ArrayList<Object>();
      for (IMemento mem : childMem.getChildren(TAG_ELEMENT)) {
        Object element = container.findMember(mem.getString(TAG_PATH));
        if (element != null) {
          elements.add(element);
        }
      }
      treeViewer.setExpandedElements(elements.toArray());
    }
    //restore selection
    childMem = memento.getChild(TAG_SELECTION);
    if (childMem != null) {
      ArrayList<Object> list = new ArrayList<Object>();
      for (IMemento mem : childMem.getChildren(TAG_ELEMENT)) {
        Object element = container.findMember(mem.getString(TAG_PATH));
        if (element != null) {
          list.add(element);
        }
      }
      treeViewer.setSelection(new StructuredSelection(list));
    }
  }

  protected void updateTreeFont() {
    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    Font oldFont = treeViewer.getTree().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    treeViewer.getTree().setFont(font);
    appLabelProvider.updateFont(font);
  }

  Shell getShell() {
    return getSite().getShell();
  }

  TreeViewer getViewer() {
    return treeViewer;
  }

  private void fillInActionBars() {
    IActionBars actionBars = getViewSite().getActionBars();
    IUndoContext workspaceContext = (IUndoContext) ResourcesPlugin.getWorkspace().getAdapter(
        IUndoContext.class);
    undoRedoActionGroup = new UndoRedoActionGroup(getViewSite(), workspaceContext, true);
    undoRedoActionGroup.fillActionBars(actionBars);
  }

  private void initDragAndDrop() {
    int ops = DND.DROP_COPY | DND.DROP_MOVE;// | DND.DROP_LINK;
    Transfer[] transfers = new Transfer[] {
        LocalSelectionTransfer.getTransfer(), ResourceTransfer.getInstance(),
        FileTransfer.getInstance(), PluginTransfer.getInstance()};
    treeViewer.addDragSupport(ops, transfers, new FilesViewDragAdapter(treeViewer));
    FilesViewDropAdapter adapter = new FilesViewDropAdapter(treeViewer);
    adapter.setFeedbackEnabled(true);
    treeViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, adapter);
  }

  private void makeActions() {
    createApplicationAction = new OpenNewApplicationWizardAction();

    clipboard = new Clipboard(getShell().getDisplay());
    deleteAction = new DeleteAction(getSite());
    deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_DELETE));
    treeViewer.addSelectionChangedListener(deleteAction);
  }

}

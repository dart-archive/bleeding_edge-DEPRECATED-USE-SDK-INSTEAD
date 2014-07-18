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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.DeleteAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.CollapseAllAction;
import com.google.dart.tools.ui.internal.filesview.FilesViewDragAdapter;
import com.google.dart.tools.ui.internal.filesview.FilesViewDropAdapter;
import com.google.dart.tools.ui.internal.filesview.LinkWithEditorAction;
import com.google.dart.tools.ui.internal.projects.OpenNewApplicationWizardAction;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
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

/**
 * Provide a structured view of applications. The content is organized in a three-level tree. The
 * top level is a list of application names. For each application, the next level is a list of files
 * defined by that application along with imported libraries. The library list contains all
 * libraries used by the application from both direct and indirect imports. The final level of the
 * tree is a list of files defined by each library.
 * <p>
 * TODO Unify this with Files View -- we should only have one view on files TODO Change icon on
 * editor tab to be the same as used in Files view (and possibly use that here)
 */
public class AppsView extends ViewPart implements ISetSelectionTarget {
  public static final String VIEW_ID = "com.google.dart.tools.ui.AppsView";

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
  private OpenNewApplicationWizardAction createApplicationAction;
  private DeleteAction deleteAction;
  private UndoRedoActionGroup undoRedoActionGroup;

  private IPreferenceStore preferences;
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  private DartIgnoreListener ignoreListener = new DartIgnoreListener() {
    @Override
    public void ignoresChanged(DartIgnoreEvent event) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          if (treeViewer != null && !treeViewer.getTree().isDisposed()) {
            treeViewer.refresh();
          }
        };
      });
    }
  };

  @Override
  public void createPartControl(Composite parent) {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("AppsView.createPartControl");
    try {

      preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
      treeViewer = new TreeViewer(parent);
      treeViewer.setContentProvider(new AppsViewContentProvider());
      appLabelProvider = new AppLabelProvider(treeViewer.getTree().getFont());
      treeViewer.setLabelProvider(new LabelProviderWrapper(
          appLabelProvider,
          new AppProblemsDecorator(),
          null));
      treeViewer.setComparator(new AppsViewComparator());
      treeViewer.addDoubleClickListener(new IDoubleClickListener() {
        @Override
        public void doubleClick(DoubleClickEvent event) {
          handleDoubleClick(event);
        }
      });
      treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
      treeViewer.getTree().setBackgroundMode(SWT.INHERIT_FORCE);
      treeViewer.getTree().addListener(SWT.EraseItem, new Listener() {
        @Override
        public void handleEvent(Event event) {
          SWTUtil.eraseSelection(event, treeViewer.getTree(), getPreferences());
        }
      });

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

      SWTUtil.bindJFaceResourcesFontToControl(treeViewer.getTree());
      getPreferences().addPropertyChangeListener(propertyChangeListener);
      updateColors();

      restoreState();
    } finally {
      instrumentation.log();

    }

  }

  @Override
  public void dispose() {

    if (linkWithEditorAction != null) {
      linkWithEditorAction.dispose();
    }

    if (clipboard != null) {
      clipboard.dispose();
    }

    if (ignoreListener != null) {
      DartCore.removeIgnoreListener(ignoreListener);
    }

    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }

    super.dispose();
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {

    super.init(site, memento);
    this.memento = memento;
    DartCore.addIgnoreListener(ignoreListener);

  }

  @Override
  public void saveState(IMemento memento) {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("AppsView.saveState");
    try {

      memento.putBoolean(LINK_WITH_EDITOR_ID, linkWithEditorAction.getLinkWithEditor());

      //save expanded elements
      Object expandedElements[] = treeViewer.getVisibleExpandedElements();
      if (expandedElements.length > 0) {
        IMemento expandedMem = memento.createChild(TAG_EXPANDED);
        for (Object element : expandedElements) {
          ElementTreeNode node = (ElementTreeNode) element;
          DartElement dartElement = node.getModelElement();
          IResource resource = null;
          try {
            resource = dartElement.getCorrespondingResource();
          } catch (DartModelException ex) {
            // ignore it
          }
          if (resource != null) {
            IMemento elementMem = expandedMem.createChild(TAG_ELEMENT);
            elementMem.putString(TAG_PATH, resource.getFullPath().toString());
          }
        }
      }

      //save selection
      Object elements[] = ((IStructuredSelection) treeViewer.getSelection()).toArray();
      if (elements.length > 0) {
        IMemento selectionMem = memento.createChild(TAG_SELECTION);
        for (Object element : elements) {
          ElementTreeNode node = (ElementTreeNode) element;
          DartElement dartElement = node.getModelElement();
          IResource resource = null;
          try {
            resource = dartElement.getCorrespondingResource();
          } catch (DartModelException ex) {
            // ignore it
          }
          if (resource != null) {
            IMemento elementMem = selectionMem.createChild(TAG_ELEMENT);
            elementMem.putString(TAG_PATH, resource.getFullPath().toString());
          }
        }
      }

    } finally {
      instrumentation.log();

    }

  }

  @Override
  public void selectReveal(ISelection selection) {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("AppsView.selectReveal");
    try {
      instrumentation.record(selection);

      try {
        treeViewer.setSelection(selection, true); //This will do the detailed instrumentation
      } catch (NullPointerException ex) {

        instrumentation.metric("SelectReveal", "NPE");
        return;

        // ignore resource creation -- those files are not in apps view
      }

    } finally {
      instrumentation.log();
    }

  }

  @Override
  public void setFocus() {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("AppsView.setFocus");
    try {

      treeViewer.getTree().setFocus();

    } finally {
      instrumentation.log();
    }

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
    manager.add(createApplicationAction);
  }

  protected void fillInToolbar(IToolBarManager toolbar) {
    // Link with Editor
    linkWithEditorAction = new LinkAppsViewWithEditorAction(getViewSite().getPage(), treeViewer);

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

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("AppsView.handleDoubleClick");
    try {

      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      instrumentation.record(selection);

      Object element = selection.getFirstElement();

      if (treeViewer.isExpandable(element)) {
        treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element));
        instrumentation.metric("expandable", "true");
      }

      if (element instanceof ElementTreeNode) {
        element = ((ElementTreeNode) element).getModelElement();

        instrumentation.metric("Element", "ElementTreeNode");
      }
      if (element instanceof CompilationUnit) {
        try {
          CompilationUnit cu = (CompilationUnit) element;
          EditorUtility.openInEditor(cu);
          instrumentation.record(cu);

        } catch (PartInitException e) {
          DartToolsPlugin.log(e);
        } catch (CoreException ex) {
          DartToolsPlugin.log(ex);
        }
      } else if (element instanceof IFileStore) {
        try {
          IDE.openEditorOnFileStore(getViewSite().getPage(), (IFileStore) element);

          instrumentation.metric("element", "FileStore");
          instrumentation.data("name", ((IFileStore) element).getName());

        } catch (PartInitException e) {
          DartToolsPlugin.log(e);
        }
      }
    } finally {
      instrumentation.log();

    }
  }

  protected void restoreState() {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("AppsView.restoreState");
    try {

      if (memento == null) {
        instrumentation.metric("Momento", "null");
        return;
      }

      IContainer container = ResourcesPlugin.getWorkspace().getRoot();
      //restore expansion
      IMemento childMem = memento.getChild(TAG_EXPANDED);
      AppsViewContentProvider contentProvider = (AppsViewContentProvider) treeViewer.getContentProvider();
      if (childMem != null) {
        List<Object> elements = new ArrayList<Object>();
        for (IMemento mem : childMem.getChildren(TAG_ELEMENT)) {
          Object element = container.findMember(mem.getString(TAG_PATH));
          if (element != null) {
            ElementTreeNode node = contentProvider.findNode(element);
            if (node != null) {
              if (node.isLeaf()) {
                node = node.getParentNode();
              }
              if (node != null) {
                elements.add(node);
              }
            }
          }
        }
        treeViewer.setExpandedElements(elements.toArray());

        instrumentation.metric("ElementsExpanded", elements.size());

        //TODO(lukechurch): If necessary add detailed expansion logging here
      }
      //restore selection
      childMem = memento.getChild(TAG_SELECTION);
      if (childMem != null) {
        ArrayList<Object> list = new ArrayList<Object>();
        for (IMemento mem : childMem.getChildren(TAG_ELEMENT)) {
          Object element = container.findMember(mem.getString(TAG_PATH));
          if (element != null) {
            list.add(contentProvider.findNode(element));
          }
        }
        treeViewer.setSelection(new StructuredSelection(list));

        instrumentation.metric("SelectionSize", list.size());
      }
    } finally {
      instrumentation.log();

    }

  }

  protected void updateColors() {
    SWTUtil.setColors(getViewer().getTree(), getPreferences());
  }

  Shell getShell() {
    return getSite().getShell();
  }

  TreeViewer getViewer() {
    return treeViewer;
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
    treeViewer.refresh(false);
  }

  private void fillInActionBars() {
    IActionBars actionBars = getViewSite().getActionBars();
    IUndoContext workspaceContext = (IUndoContext) ResourcesPlugin.getWorkspace().getAdapter(
        IUndoContext.class);
    undoRedoActionGroup = new UndoRedoActionGroup(getViewSite(), workspaceContext, true);
    undoRedoActionGroup.fillActionBars(actionBars);
  }

  private IPreferenceStore getPreferences() {
    return preferences;
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

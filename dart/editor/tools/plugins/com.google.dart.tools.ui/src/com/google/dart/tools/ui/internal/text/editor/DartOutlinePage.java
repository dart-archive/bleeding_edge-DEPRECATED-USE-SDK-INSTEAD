/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.common.base.Objects;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.search.ui.DartSearchActionGroup;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.actions.OpenViewActionGroup;
import com.google.dart.tools.ui.actions.RefactorActionGroup;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.viewsupport.ColoredViewersManager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * {@link IContentOutlinePage} for {@link DartEditor}.
 */
public class DartOutlinePage extends Page implements IContentOutlinePage {

  private class CollapseAllAction extends InstrumentedAction {
    CollapseAllAction() {
      super("Collapse All"); //$NON-NLS-1$
      setDescription("Collapse All"); //$NON-NLS-1$
      setToolTipText("Collapse All"); //$NON-NLS-1$
      DartPluginImages.setLocalImageDescriptors(this, "collapseall.gif"); //$NON-NLS-1$
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.COLLAPSE_ALL_ACTION);
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
      viewer.collapseAll();
    }
  }

  private class DartOutlineViewer extends TreeViewer {
    public DartOutlineViewer(final Tree tree) {
      super(tree);
      setUseHashlookup(true);
      tree.setBackgroundMode(SWT.INHERIT_FORCE);
    }

    private void updateColors() {
      SWTUtil.setColors(getTree(), preferences);
    }
  }

  private class ExpandAllAction extends InstrumentedAction {
    ExpandAllAction() {
      super("Expand All"); //$NON-NLS-1$
      setDescription("Expand All"); //$NON-NLS-1$
      setToolTipText("Expand All"); //$NON-NLS-1$
      DartPluginImages.setLocalImageDescriptors(this, "expandall.gif"); //$NON-NLS-1$
      PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.EXPAND_ALL_ACTION);
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
      viewer.expandAll();
    }
  }

  private class HideNonPublicAction extends InstrumentedAction {
    HideNonPublicAction() {
      super("Hide non-public members", IAction.AS_CHECK_BOX); //$NON-NLS-1$
      setDescription("Hide non-public members"); //$NON-NLS-1$
      setToolTipText("Hide non-public members"); //$NON-NLS-1$
      DartPluginImages.setLocalImageDescriptors(this, "public_co.gif"); //$NON-NLS-1$
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.HIDE_NON_PUBLIC_ACTION);
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
      if (isChecked()) {
        viewer.addFilter(PUBLIC_FILTER);
      } else {
        viewer.removeFilter(PUBLIC_FILTER);
      }
    }
  }

  private class LexicalSortingAction extends InstrumentedAction {
    public LexicalSortingAction() {
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.LEXICAL_SORTING_OUTLINE_ACTION);
      setText(DartEditorMessages.JavaOutlinePage_Sort_label);
      DartPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
      setToolTipText(DartEditorMessages.JavaOutlinePage_Sort_tooltip);
      setDescription(DartEditorMessages.JavaOutlinePage_Sort_description);
      // restore selection
      boolean checked = DartToolsPlugin.getDefault().getPreferenceStore().getBoolean(
          "LexicalSortingAction.isChecked"); //$NON-NLS-1$
      valueChanged(checked, false);
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
      valueChanged(isChecked(), true);
    }

    private void valueChanged(final boolean on, boolean store) {
      setChecked(on);
      // set comparator
      BusyIndicator.showWhile(viewer.getControl().getDisplay(), new Runnable() {
        @Override
        public void run() {
          if (on) {
            viewer.setComparator(LightNodeElements.NAME_COMPARATOR);
          } else {
            viewer.setComparator(LightNodeElements.POSITION_COMPARATOR);
          }
        }
      });
      // may be remember selection
      if (store) {
        DartToolsPlugin.getDefault().getPreferenceStore().setValue(
            "LexicalSortingAction.isChecked", on); //$NON-NLS-1$
      }
    }
  }

  private final ListenerList selectionChangedListeners = new ListenerList(ListenerList.IDENTITY);
  private final String contextMenuID;
  private DartEditor editor;
  private DartOutlineViewer viewer;
  private boolean ignoreSelectionChangedEvent = false;
  private Menu contextMenu;
  private CompositeActionGroup actionGroups;

  private CompilationUnit input;

  private IPreferenceStore preferences;
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  private static final ViewerFilter PUBLIC_FILTER = new ViewerFilter() {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object o) {
      if (o instanceof LightNodeElement) {
        LightNodeElement element = (LightNodeElement) o;
        return !element.isPrivate();
      }
      return false;
    }
  };

  public DartOutlinePage(String contextMenuID, DartEditor editor) {
    Assert.isNotNull(editor);
    this.contextMenuID = contextMenuID;
    this.editor = editor;
  }

  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    if (viewer != null) {
      viewer.addSelectionChangedListener(listener);
    } else {
      selectionChangedListeners.add(listener);
    }
  }

  @Override
  public void createControl(Composite parent) {
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION);
    // create "viewer"
    viewer = new DartOutlineViewer(tree);
    ColoredViewersManager.install(viewer);
    viewer.setContentProvider(LightNodeElements.newTreeContentProvider(editor));
    viewer.setLabelProvider(LightNodeElements.newLabelProvider());
    SWTUtil.bindJFaceResourcesFontToControl(tree);
    // install listeners added before UI creation
    {
      Object[] listeners = selectionChangedListeners.getListeners();
      for (Object listener : listeners) {
        selectionChangedListeners.remove(listener);
        viewer.addSelectionChangedListener((ISelectionChangedListener) listener);
      }
    }
    // prepare context menu
    MenuManager manager = new MenuManager(contextMenuID, contextMenuID);
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager m) {
        contextMenuAboutToShow(m);
      }
    });
    contextMenu = manager.createContextMenu(tree);
    tree.setMenu(contextMenu);
    // register "viewer" as selection provide for this view
    IPageSite site = getSite();
    site.setSelectionProvider(viewer);
    actionGroups = new CompositeActionGroup(new ActionGroup[] {
        new OpenViewActionGroup(site), new RefactorActionGroup(site),
        new DartSearchActionGroup(site)});
    // configure actions
    {
      IActionBars actionBars = site.getActionBars();
      {
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        toolBarManager.add(new HideNonPublicAction());
        toolBarManager.add(new LexicalSortingAction());
        toolBarManager.add(new ExpandAllAction());
        toolBarManager.add(new CollapseAllAction());
      }
      actionGroups.fillActionBars(actionBars);
    }
    // handle TreeViewer actions
    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        if (ignoreSelectionChangedEvent) {
          return;
        }
        editor.doSelectionChanged(event.getSelection());
      }
    });
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        toggleExpansion(event.getSelection());
      }
    });
    // update colors
    preferences.addPropertyChangeListener(propertyChangeListener);
    viewer.updateColors();
    // schedule update in 100ms from now, to make impression that editor opens instantaneously
    new UIJob("Update Outline") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (viewer != null) {
          viewer.setInput(input);
          //LightNodeElements.expandTreeItemsTimeBoxed(viewer, 75L * 1000000L);
        }
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  @Override
  public void dispose() {
    // clear "editor"
    if (editor == null) {
      return;
    }
    editor.outlinePageClosed();
    editor = null;
    // remove property listeners
    if (propertyChangeListener != null) {
      preferences.removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }
    // dispose "contextMenu"
    if (contextMenu != null && !contextMenu.isDisposed()) {
      contextMenu.dispose();
      contextMenu = null;
    }
    // dispose actions
    if (actionGroups != null) {
      actionGroups.dispose();
      actionGroups = null;
    }
    // we are not selection provider anymore
    getSite().setSelectionProvider(null);
    // done
    viewer = null;
    input = null;
    super.dispose();
  }

  @Override
  public Control getControl() {
    if (viewer != null) {
      return viewer.getControl();
    }
    return null;
  }

  @Override
  public ISelection getSelection() {
    if (viewer == null) {
      return StructuredSelection.EMPTY;
    }
    return viewer.getSelection();
  }

  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    if (viewer != null) {
      viewer.removeSelectionChangedListener(listener);
    } else {
      selectionChangedListeners.remove(listener);
    }
  }

  public void select(final LightNodeElement element) {
    updateViewerWithoutDraw(new Runnable() {
      @Override
      public void run() {
        setSelection(new StructuredSelection(element));
        viewer.reveal(element);
      }
    });
  }

  @Override
  public void setFocus() {
    if (viewer != null) {
      viewer.getControl().setFocus();
    }
  }

  public void setInput(final CompilationUnit input) {
    this.input = input;
    updateViewerWithoutDraw(new Runnable() {
      @Override
      public void run() {
        Object[] expandedElements = viewer.getExpandedElements();
        ignoreSelectionChangedEvent = true;
        try {
          viewer.setInput(input);
        } finally {
          ignoreSelectionChangedEvent = false;
        }
        viewer.setExpandedElements(expandedElements);
      }
    });
  }

  @Override
  public void setSelection(final ISelection newSelection) {
    if (!Objects.equal(viewer.getSelection(), newSelection)) {
      updateViewerWithoutDraw(new Runnable() {
        @Override
        public void run() {
          ignoreSelectionChangedEvent = true;
          try {
            viewer.setSelection(newSelection, true);
          } finally {
            ignoreSelectionChangedEvent = false;
          }
        }
      });
    }
  }

  protected void toggleExpansion(ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      Object sel = ((IStructuredSelection) selection).getFirstElement();

      boolean expanded = viewer.getExpandedState(sel);

      if (expanded) {
        viewer.collapseToLevel(sel, 1);
      } else {
        viewer.expandToLevel(sel, 1);
      }
    }
  }

  private void contextMenuAboutToShow(IMenuManager menu) {
    DartToolsPlugin.createStandardGroups(menu);
    IStructuredSelection selection = (IStructuredSelection) getSelection();
    actionGroups.setContext(new ActionContext(selection));
    actionGroups.fillContextMenu(menu);
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    if (viewer != null) {
      viewer.updateColors();
      viewer.refresh(false);
    }
  }

  /**
   * Performs the given operation over {@link #viewer} while redraw is disabled.
   */
  private void updateViewerWithoutDraw(Runnable runnable) {
    if (viewer != null) {
      final Control control = viewer.getControl().getParent();
      control.setRedraw(false);
      try {
        runnable.run();
      } finally {
        if (DartCore.isLinux()) {
          // By some reason on Linux we need to add a delay.
          // It seems like Tree operations are performed not in the UI thread.
          Display.getCurrent().timerExec(50, new Runnable() {
            @Override
            public void run() {
              control.setRedraw(true);
            }
          });
        } else {
          control.setRedraw(true);
        }
      }
    }
  }
}

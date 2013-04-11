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
import com.google.dart.tools.search.internal.ui.DartSearchActionGroup;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.actions.OpenViewActionGroup;
import com.google.dart.tools.ui.actions.RefactorActionGroup;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.viewsupport.ColoredViewersManager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
public class DartOutlinePage extends Page implements IContentOutlinePage, DartOutlinePage_I {

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
      tree.addListener(SWT.EraseItem, new Listener() {
        @Override
        public void handleEvent(Event event) {
          SWTUtil.eraseSelection(event, tree, editor.getPreferences());
        }
      });
      updateColors();
    }

    private void updateColors() {
      SWTUtil.setColors(getTree(), DartOutlinePage.this.editor.getPreferences());
    }

    private void updateTreeFont() {
      Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
      Font oldFont = getTree().getFont();
      Font font = SWTUtil.changeFontSize(oldFont, newFont);
      getTree().setFont(font);
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

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (viewer != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          viewer.updateTreeFont();
        }
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
  private IPropertyChangeListener propertyChangeListener;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private Menu contextMenu;
  private CompositeActionGroup actionGroups;

  private CompilationUnit input;

  public DartOutlinePage(String contextMenuID, DartEditor editor) {
    Assert.isNotNull(editor);
    this.contextMenuID = contextMenuID;
    this.editor = editor;
    // listen for property changes
    propertyChangeListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        doPropertyChange(event);
      }
    };
    DartToolsPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
        propertyChangeListener);
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
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
    Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION);
    // create "viewer"
    viewer = new DartOutlineViewer(tree);
    ColoredViewersManager.install(viewer);
    viewer.setContentProvider(LightNodeElements.newTreeContentProvider());
    viewer.setLabelProvider(LightNodeElements.LABEL_PROVIDER);
    viewer.updateTreeFont();
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
    // TODO(scheglov)
    actionGroups = new CompositeActionGroup(new ActionGroup[] {
        new OpenViewActionGroup(site), new RefactorActionGroup(site),
        new DartSearchActionGroup(site)});
    // configure actions
    {
      IActionBars actionBars = site.getActionBars();
      {
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
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
        editor.doSelectionChanged(event.getSelection());
      }
    });
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        editor.doSelectionChanged(event.getSelection());
        getSite().getPage().activate(editor);
      }
    });
    // schedule update in 100ms from now, to make impression that editor opens instantaneously
    new UIJob("Update Outline") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (viewer != null) {
          viewer.setInput(input);
        }
        return Status.OK_STATUS;
      }
    }.schedule(100);
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
      IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
      preferenceStore.removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }
    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
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

  public void select(LightNodeElement element) {
    if (viewer != null) {
      ISelection newSelection = new StructuredSelection(element);
      if (!Objects.equal(viewer.getSelection(), newSelection)) {
        viewer.setSelection(newSelection);
      }
    }
  }

  @Override
  public void setFocus() {
    if (viewer != null) {
      viewer.getControl().setFocus();
    }
  }

  public void setInput(CompilationUnit input) {
    this.input = input;
    if (viewer != null) {
      Control control = viewer.getControl();
      control.setRedraw(false);
      try {
        Object[] expandedElements = viewer.getExpandedElements();
        viewer.setInput(input);
        viewer.setExpandedElements(expandedElements);
      } finally {
        control.setRedraw(true);
      }
    }
  }

  @Override
  public void setSelection(ISelection selection) {
    if (viewer != null) {
      viewer.setSelection(selection);
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
}

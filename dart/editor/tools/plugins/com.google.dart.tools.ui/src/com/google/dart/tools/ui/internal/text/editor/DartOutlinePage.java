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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.tools.search.internal.ui.DartSearchActionGroup;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.OpenViewActionGroup;
import com.google.dart.tools.ui.actions.RefactorActionGroup;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.viewsupport.ColoredViewersManager;
import com.google.dart.tools.ui.internal.viewsupport.NameElementComparator;
import com.google.dart.tools.ui.internal.viewsupport.SourcePositionElementComparator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
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

  private class CollapseAllAction extends Action {
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
    public void run() {
      fOutlineViewer.collapseAll();
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
          SWTUtil.eraseSelection(event, tree, fEditor.getPreferences());
        }
      });
      updateColors();
    }

    private void updateColors() {
      SWTUtil.setColors(getTree(), DartOutlinePage.this.fEditor.getPreferences());
    }

    private void updateTreeFont() {
      Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
      Font oldFont = getTree().getFont();
      Font font = SWTUtil.changeFontSize(oldFont, newFont);
      getTree().setFont(font);
    }
  }

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (fOutlineViewer != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          fOutlineViewer.updateTreeFont();
        }
      }
    }
  }

  private class LexicalSortingAction extends Action {
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
    public void run() {
      valueChanged(isChecked(), true);
    }

    private void valueChanged(final boolean on, boolean store) {
      setChecked(on);
      // set comparator
      BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
        @Override
        public void run() {
          if (on) {
            fOutlineViewer.setComparator(NameElementComparator.INSTANCE);
          } else {
            fOutlineViewer.setComparator(SourcePositionElementComparator.INSTANCE);
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

  private String fContextMenuID;

  private DartEditor fEditor;
  private IPropertyChangeListener fPropertyChangeListener;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private DartOutlineViewer fOutlineViewer;
  private Menu fMenu;

  private CompositeActionGroup fActionGroups;

  private ListenerList fSelectionChangedListeners = new ListenerList(ListenerList.IDENTITY);

  private CompilationUnit input;

  public DartOutlinePage(String contextMenuID, DartEditor editor) {
    Assert.isNotNull(editor);
    fContextMenuID = contextMenuID;
    fEditor = editor;
    // listen for property changes
    fPropertyChangeListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        doPropertyChange(event);
      }
    };
    DartOutlinePage.this.fEditor.getPreferences().addPropertyChangeListener(fPropertyChangeListener);
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
  }

  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    if (fOutlineViewer != null) {
      fOutlineViewer.addSelectionChangedListener(listener);
    } else {
      fSelectionChangedListeners.add(listener);
    }
  }

  @Override
  public void createControl(Composite parent) {
    Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION);

    fOutlineViewer = new DartOutlineViewer(tree);
    ColoredViewersManager.install(fOutlineViewer);

    fOutlineViewer.setContentProvider(LightNodeElements.newTreeContentProvider());
    fOutlineViewer.setLabelProvider(LightNodeElements.newLabelProvider());
    fOutlineViewer.updateTreeFont();

    {
      Object[] listeners = fSelectionChangedListeners.getListeners();
      for (Object listener : listeners) {
        fSelectionChangedListeners.remove(listener);
        fOutlineViewer.addSelectionChangedListener((ISelectionChangedListener) listener);
      }
    }

    MenuManager manager = new MenuManager(fContextMenuID, fContextMenuID);
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager m) {
        contextMenuAboutToShow(m);
      }
    });
    fMenu = manager.createContextMenu(tree);
    tree.setMenu(fMenu);

    IPageSite site = getSite();
    site.setSelectionProvider(fOutlineViewer);

    // TODO(scheglov)
    fActionGroups = new CompositeActionGroup(new ActionGroup[] {
        new OpenViewActionGroup(this), new RefactorActionGroup(this),
        new DartSearchActionGroup(this)});

    IActionBars actionBars = site.getActionBars();
    fActionGroups.fillActionBars(actionBars);
    registerToolbarActions(actionBars);

    // handle TreeViewer actions
    fOutlineViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        fEditor.doSelectionChanged(event.getSelection());
      }
    });
    fOutlineViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        fEditor.doSelectionChanged(event.getSelection());
        getSite().getPage().activate(fEditor);
      }
    });

    // Schedule update in 100ms from now, to make impression that editor opens instantaneously.
    new UIJob("Update Outline") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        fOutlineViewer.setInput(input);
        return Status.OK_STATUS;
      }
    }.schedule(100);
  }

  @Override
  public void dispose() {
    if (fEditor == null) {
      return;
    }
    fEditor.outlinePageClosed();
    fEditor = null;

    if (fPropertyChangeListener != null) {
      DartToolsPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
          fPropertyChangeListener);
      fPropertyChangeListener = null;
    }
    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
    }

    if (fMenu != null && !fMenu.isDisposed()) {
      fMenu.dispose();
      fMenu = null;
    }

    if (fActionGroups != null) {
      fActionGroups.dispose();
    }

    fOutlineViewer = null;

    super.dispose();
  }

  @Override
  public Control getControl() {
    if (fOutlineViewer != null) {
      return fOutlineViewer.getControl();
    }
    return null;
  }

  @Override
  public ISelection getSelection() {
    if (fOutlineViewer == null) {
      return StructuredSelection.EMPTY;
    }
    return fOutlineViewer.getSelection();
  }

  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    if (fOutlineViewer != null) {
      fOutlineViewer.removeSelectionChangedListener(listener);
    } else {
      fSelectionChangedListeners.remove(listener);
    }
  }

  public void select(LightNodeElement element) {
    if (fOutlineViewer != null) {
      fOutlineViewer.setSelection(new StructuredSelection(element));
    }
  }

  @Override
  public void setFocus() {
    if (fOutlineViewer != null) {
      fOutlineViewer.getControl().setFocus();
    }
  }

  public void setInput(CompilationUnit input) {
    this.input = input;
    if (fOutlineViewer != null) {
      fOutlineViewer.setInput(input);
    }
  }

  @Override
  public void setSelection(ISelection selection) {
    if (fOutlineViewer != null) {
      fOutlineViewer.setSelection(selection);
    }
  }

  protected void contextMenuAboutToShow(IMenuManager menu) {
    DartToolsPlugin.createStandardGroups(menu);
    IStructuredSelection selection = (IStructuredSelection) getSelection();
    fActionGroups.setContext(new ActionContext(selection));
    fActionGroups.fillContextMenu(menu);
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    if (fOutlineViewer != null) {
      fOutlineViewer.updateColors();
      fOutlineViewer.refresh(false);
    }
  }

  private void registerToolbarActions(IActionBars actionBars) {
    IToolBarManager toolBarManager = actionBars.getToolBarManager();
    toolBarManager.add(new LexicalSortingAction());
    toolBarManager.add(new CollapseAllAction());
  }
}

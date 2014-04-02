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
package com.google.dart.tools.ui.internal.typehierarchy;

import com.google.common.base.Predicates;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.tools.internal.search.ui.DartSearchActionGroup;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.OpenAction;
import com.google.dart.tools.ui.actions.OpenViewActionGroup;
import com.google.dart.tools.ui.actions.RefactorActionGroup;
import com.google.dart.tools.ui.internal.text.editor.CompositeActionGroup;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * View showing the super types/sub types of its input.
 */
public class TypeHierarchyViewPart extends ViewPart {

  private static final String TAG_RATIO = "ratio"; //$NON-NLS-1$

  private/*ClassElement*/Object inputType;
  private IMemento fMemento;

  private SashForm fTypeMethodsSplitter;
  private PageBook fPagebook;

  private Label fNoHierarchyShownLabel;
  private ViewForm fTypeViewerViewForm;
  private ViewForm fMethodViewerViewForm;
  private TreeViewer typesViewer;
  private MethodsViewer methodsViewer;

  private OpenAction fOpenAction;
  private Menu contextMenu;
  private CompositeActionGroup actionGroups;

  public TypeHierarchyViewPart() {
  }

  @Override
  public void createPartControl(Composite container) {
    fPagebook = new PageBook(container, SWT.NONE);
    // page 1 of page book (no hierarchy label)
    {
      fNoHierarchyShownLabel = new Label(fPagebook, SWT.TOP | SWT.LEFT | SWT.WRAP);
      fNoHierarchyShownLabel.setText(TypeHierarchyMessages.TypeHierarchyViewPart_empty);
    }
    // page 2 of page book (viewers)
    {
      fTypeMethodsSplitter = new SashForm(fPagebook, SWT.VERTICAL);
      fTypeMethodsSplitter.setVisible(false);

      fTypeViewerViewForm = new ViewForm(fTypeMethodsSplitter, SWT.NONE);

      Control typeViewerControl = createTypeViewerControl(fTypeViewerViewForm);
      fTypeViewerViewForm.setContent(typeViewerControl);

      fMethodViewerViewForm = new ViewForm(fTypeMethodsSplitter, SWT.NONE);
      fTypeMethodsSplitter.setWeights(new int[] {65, 35});

      Control methodViewerPart = createMethodViewerControl(fMethodViewerViewForm);
      fMethodViewerViewForm.setContent(methodViewerPart);
    }

    fPagebook.showPage(fNoHierarchyShownLabel);

    // fill the tool bar
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    methodsViewer.contributeToToolBar(toolbarManager);
    toolbarManager.update(true);

    if (fMemento != null) {
      restoreState(fMemento);
    }

    fOpenAction = new OpenAction(getSite());
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);
    fMemento = memento;
  }

  @Override
  public void saveState(IMemento memento) {
    // part is not created yet, keep old state
    if (fPagebook == null) {
      if (fMemento != null) {
        memento.putMemento(fMemento);
      }
      return;
    }
    // save state
    {
      int weigths[] = fTypeMethodsSplitter.getWeights();
      int ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
      memento.putInteger(TAG_RATIO, ratio);
    }
    methodsViewer.saveState(memento);
  }

  @Override
  public void setFocus() {
    fPagebook.setFocus();
  }

  /**
   * Sets the input to a new element.
   */
  public void setInputElement(/*Element*/Object element) {
    // may be not a Type
    if (!(element instanceof com.google.dart.tools.core.model.Type)
        && !(element instanceof ClassElement)) {
      typesViewer.setInput(null);
      methodsViewer.setInputType(null);
      return;
    }
    // show hierarchy
    inputType = element;
    fPagebook.showPage(fTypeMethodsSplitter);
    typesViewer.setInput(new Object[] {inputType});
    typesViewer.expandToLevel(inputType, 1);
    typesViewer.setSelection(new StructuredSelection(inputType));
  }

  private void contextMenuAboutToShow(IMenuManager menu) {
    DartToolsPlugin.createStandardGroups(menu);
    ISelection selection = typesViewer.getSelection();
    actionGroups.setContext(new ActionContext(selection));
    actionGroups.fillContextMenu(menu);
  }

  private Control createMethodViewerControl(Composite parent) {
    methodsViewer = new MethodsViewer(parent);
    ((StructuredViewer) methodsViewer).addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          fOpenAction.run((IStructuredSelection) selection);
        }
      }
    });
    return ((Viewer) methodsViewer).getControl();
  }

  private Control createTypeViewerControl(Composite parent) {
    typesViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    Tree tree = typesViewer.getTree();

    typesViewer.setContentProvider(new TypeHierarchyContentProvider());
    typesViewer.setLabelProvider(new HierarchyLabelProvider(Predicates.alwaysFalse()));
    // register "viewer" as selection provide for this view
    IViewSite site = getViewSite();
    site.setSelectionProvider(typesViewer);
    // configure actions
    actionGroups = new CompositeActionGroup(new ActionGroup[] {
        new OpenViewActionGroup(site), new RefactorActionGroup(site),
        new DartSearchActionGroup(site)});
    // configure actions
    {
      IActionBars actionBars = site.getActionBars();
      actionGroups.fillActionBars(actionBars);
    }
    // prepare context menu
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager m) {
        contextMenuAboutToShow(m);
      }
    });
    contextMenu = manager.createContextMenu(tree);
    tree.setMenu(contextMenu);

    typesViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          IStructuredSelection structuredSelection = (IStructuredSelection) selection;
          Object selectedElement = structuredSelection.getFirstElement();
          methodsViewer.setInputType(selectedElement);
        }
      }
    });
    typesViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          fOpenAction.run((IStructuredSelection) selection);
        }
      }
    });
    return tree;
  }

  /**
   * Restores the type hierarchy settings from a memento.
   */
  private void restoreState(IMemento memento) {
    {
      Integer ratio = memento.getInteger(TAG_RATIO);
      if (ratio != null) {
        fTypeMethodsSplitter.setWeights(new int[] {ratio.intValue(), 1000 - ratio.intValue()});
      }
    }
    methodsViewer.restoreState(memento);
  }

}

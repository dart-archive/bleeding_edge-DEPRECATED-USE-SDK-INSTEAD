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
package com.google.dart.dev.util.ast;

import com.google.dart.dev.util.DartDevPlugin;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import java.io.IOException;

/**
 * A basic AST explorer view.
 */
@SuppressWarnings("restriction")
public class ASTExplorer extends ViewPart implements AnalysisErrorListener {

  private class EventHandler implements IPartListener, ISelectionListener,
      ISelectionChangedListener {

    //triggers refresh on save and updates label to indicate dirtiness
    private IPropertyListener propertyListener = new IPropertyListener() {
      @Override
      public void propertyChanged(Object source, int propId) {
        if (source instanceof DartEditor) {
          if (propId == IEditorPart.PROP_DIRTY) {
            if (((DartEditor) source).isDirty()) {
              setPartName('*' + getPartName());
            } else {
              refresh();
              String name = getPartName();
              if (name.charAt(0) == '*') {
                name = name.substring(1);
              }
              setPartName(name);
            }
          }
        }
      }
    };

    @Override
    public void partActivated(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.addPropertyListener(propertyListener);
      }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
      //ignore
    }

    @Override
    public void partClosed(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.removePropertyListener(propertyListener);
      }
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.removePropertyListener(propertyListener);
      }
    }

    @Override
    public void partOpened(IWorkbenchPart part) {
      //ignore
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
      //TODO(pquitslund): implement text selection tracking
      if (selection instanceof ITextSelection) {
        int offset = ((ITextSelection) selection).getOffset();
        selectNodeAtOffset(offset);
      }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {

      IEditorPart editor = getActiveEditor();

      if (editor instanceof DartEditor) {
        ISelection selection = event.getSelection();
        if (selection instanceof TreeSelection) {
          Object element = ((TreeSelection) selection).getFirstElement();
          if (element instanceof ASTNode) {
            ASTNode node = (ASTNode) element;
            EditorUtility.revealInEditor(editor, node.getOffset(), node.getLength());
          }
        }
      }

    }

  }

  private static class ExplorerContentProvider implements IStructuredContentProvider,
      ITreeContentProvider {

    private final ASTNode[] NO_NODES = new ASTNode[0];

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parent) {
      if (parent instanceof ASTNode) {
        ASTNode node = (ASTNode) parent;
        CollectingVisitor nodeCollector = new CollectingVisitor();
        node.visitChildren(nodeCollector);
        return nodeCollector.getNodes().toArray();
      }
      return NO_NODES;
    }

    @Override
    public Object[] getElements(Object parent) {
      return getChildren(parent);
    }

    @Override
    public Object getParent(Object child) {
      if (child instanceof ASTNode) {
        return ((ASTNode) child).getParent();
      }
      return NO_NODES;
    }

    @Override
    public boolean hasChildren(Object parent) {
      return getChildren(parent).length > 0;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      viewer.getControl().setRedraw(false);
      ((TreeViewer) viewer).refresh(true);
      viewer.getControl().setRedraw(true);
    }

  }

  private static class ExplorerLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object obj) {
      return DartDevPlugin.getImage("brkpi_obj.gif");
    }

    @Override
    public String getText(Object obj) {
      return obj.getClass().getSimpleName();
    }
  }

  /**
   * View extension id.
   */
  public static final String ID = "com.google.dart.dev.util.ast.ASTExplorer";

  private TreeViewer viewer;

  private Action expandAllAction;
  private Action collapseAllAction;

  private EventHandler eventHandler = new EventHandler();

  @Override
  public void createPartControl(Composite parent) {

    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.setContentProvider(new ExplorerContentProvider());
    viewer.setLabelProvider(new ExplorerLabelProvider());

    makeActions();
    contributeToActionBars();
    hookupListeners();

    refresh();
  }

  @Override
  public void dispose() {
    getSelectionService().removeSelectionListener(eventHandler);
    getPage().removePartListener(eventHandler);
  }

  @Override
  public void onError(AnalysisError error) {
    //TODO(pquitslund): handle errors
  }

  @Override
  public void setFocus() {
    refresh();
    viewer.getControl().setFocus();
  }

  protected IEditorPart getActiveEditor() {
    return getPage().getActiveEditor();
  }

  protected IWorkbenchPage getPage() {
    return getSite().getPage();
  }

  protected ISelectionService getSelectionService() {
    return (ISelectionService) getSite().getService(ISelectionService.class);
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    contributeViewToolItems(bars.getToolBarManager());
  }

  private void contributeViewToolItems(IToolBarManager manager) {
    manager.add(expandAllAction);
    manager.add(collapseAllAction);
  }

  private void hookupListeners() {
    viewer.addSelectionChangedListener(eventHandler);
    getSelectionService().addSelectionListener(eventHandler);
    getPage().addPartListener(eventHandler);
  }

  private void makeActions() {

    expandAllAction = new Action() {
      @Override
      public void run() {
        if (!viewer.getControl().isDisposed()) {
          viewer.getControl().setRedraw(false);
          viewer.expandToLevel(viewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
          viewer.getControl().setRedraw(true);
        }
      }
    };
    expandAllAction.setText("Expand All");
    expandAllAction.setToolTipText("Expand All");
    expandAllAction.setImageDescriptor(DartDevPlugin.getImageDescriptor("expandall.gif"));

    collapseAllAction = new Action() {
      @Override
      public void run() {
        if (!viewer.getControl().isDisposed()) {
          viewer.getControl().setRedraw(false);
          viewer.collapseToLevel(viewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
          viewer.getControl().setRedraw(true);
        }
      }
    };
    collapseAllAction.setText("Collapse All");
    collapseAllAction.setToolTipText("Collapse All");
    collapseAllAction.setImageDescriptor(DartDevPlugin.getImageDescriptor("collapseall.gif"));

  }

  private void refresh() {

    IEditorPart editor = getActiveEditor();
    if (editor != null) {

      IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput) {
        IFile file = ((IFileEditorInput) input).getFile();
        try {

          String contents = IFileUtilities.getContents(file);

          StringScanner scanner = new StringScanner(null, contents, this);
          Parser parser = new Parser(null, this);

          Token token = scanner.tokenize();
          CompilationUnit compilationUnit = parser.parseCompilationUnit(token);

          viewer.setInput(compilationUnit);

        } catch (CoreException e) {
          DartDevPlugin.logError(e);
        } catch (IOException e) {
          DartDevPlugin.logError(e);
        }
      }
    }
  }

  private void selectNodeAtOffset(int offset) {
    //TODO(pquitslund): implement
  }

}

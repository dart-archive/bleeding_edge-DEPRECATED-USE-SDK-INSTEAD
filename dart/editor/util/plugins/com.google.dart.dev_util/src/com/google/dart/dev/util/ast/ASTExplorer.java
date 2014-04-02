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
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.type.Type;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
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
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.ViewPart;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

    private final List<IWorkbenchPart> parts = new ArrayList<IWorkbenchPart>();

    @Override
    public void partActivated(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.addPropertyListener(propertyListener);
        parts.add(part);
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
        parts.remove(part);
      }
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.removePropertyListener(propertyListener);
        parts.remove(part);
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
        int length = ((ITextSelection) selection).getLength();
        selectNodeAtOffset(offset, length);
      }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {

      IEditorPart editor = getActiveEditor();

      if (editor instanceof DartEditor) {
        ISelection selection = event.getSelection();
        if (selection instanceof TreeSelection) {
          Object element = ((TreeSelection) selection).getFirstElement();
          if (element instanceof AstNode) {
            AstNode node = (AstNode) element;
            EditorUtility.revealInEditor(editor, node.getOffset(), node.getLength());
          }
          if (element instanceof AnalysisError) {
            AnalysisError error = (AnalysisError) element;
            EditorUtility.revealInEditor(editor, error.getOffset(), error.getLength());
          }
          tableViewer.setInput(element);
        }
      }

    }

    void dispose() {
      getSelectionService().removeSelectionListener(this);
      getPage().removePartListener(this);
      for (IWorkbenchPart part : parts) {
        part.removePropertyListener(propertyListener);
      }
    }

  }

  private class ExplorerContentProvider implements IStructuredContentProvider, ITreeContentProvider {

    private final AstNode[] NO_NODES = new AstNode[0];

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parent) {
      if (parent instanceof AstNode) {
        AstNode node = (AstNode) parent;
        CollectingVisitor nodeCollector = new CollectingVisitor();
        node.visitChildren(nodeCollector);
        return nodes(nodeCollector, parent);
      }

      return NO_NODES;
    }

    @Override
    public Object[] getElements(Object parent) {
      // TODO(brianwilkerson) Figure out how to get the CompilationUnit to be the top-level node in
      // the tree rather than it's children. The commented out code causes an infinite recursion (as
      // in, the unit appears to have one child, which is the unit, which has one child, etc.).
      return getChildren(parent); // new Object[] {parent};
    }

    @Override
    public Object getParent(Object child) {
      if (child instanceof AstNode) {
        return ((AstNode) child).getParent();
      }
      return null;
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

    private Object[] nodes(CollectingVisitor nodeCollector, Object object) {
      ArrayList<Object> children = new ArrayList<Object>(nodeCollector.getNodes());
      if (object instanceof CompilationUnit) {
        children.addAll(errors);
      }
      return children.toArray();
    }
  }

  private static class ExplorerLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object obj) {
      String img;
      if (obj instanceof AnalysisError) {
        img = "error_obj.gif";
      } else if (obj instanceof Element) {
        // TODO(brianwilkerson) Find a better icon for this
        img = "class_hi.gif";
      } else {
        img = "brkpi_obj.gif";
      }
      return DartDevPlugin.getImage(img);
    }

    @Override
    public String getText(Object obj) {
      if (obj instanceof AnalysisError) {
        return ((AnalysisError) obj).getMessage();
      }
      StringBuilder builder = new StringBuilder();
      builder.append(obj.getClass().getSimpleName());
      if (obj instanceof AstNode) {
        AstNode node = (AstNode) obj;
//        builder.append(" [");
//        builder.append(node.getOffset());
//        builder.append("..");
//        builder.append(node.getOffset() + node.getLength() - 1);
//        builder.append("]");
        String name = getName(node);
        if (name != null) {
          builder.append(" - ");
          builder.append(name);
        }
//        if (obj instanceof Expression) {
//          builder.append(" (");
//          Type staticType = ((Expression) obj).getStaticType();
//          if (staticType == null) {
//            builder.append("null");
//          } else {
//            builder.append(staticType);
//          }
//          builder.append("/");
//          Type propagatedType = ((Expression) obj).getPropagatedType();
//          if (propagatedType == null) {
//            builder.append("null");
//          } else {
//            builder.append(propagatedType);
//          }
//          builder.append(")");
//        }
//      } else if (obj instanceof Element) {
//        String name = ((Element) obj).getDisplayName();
//        if (name != null) {
//          builder.append(" - ");
//          builder.append(name);
//        }
      }
      return builder.toString();
    }

    /**
     * Return the name of the given node, or {@code null} if the given node is not a declaration.
     * 
     * @param node the node whose name is to be returned
     * @return the name of the given node
     */
    private String getName(AstNode node) {
      // TODO(brianwilkerson) Rewrite this to use a visitor.
      if (node instanceof ClassTypeAlias) {
        return ((ClassTypeAlias) node).getName().getName();
      } else if (node instanceof ClassDeclaration) {
        return ((ClassDeclaration) node).getName().getName();
      } else if (node instanceof ConstructorDeclaration) {
        ConstructorDeclaration cd = (ConstructorDeclaration) node;
        if (cd.getName() == null) {
          return cd.getReturnType().getName();
        } else {
          return cd.getReturnType().getName() + '.' + cd.getName().getName();
        }
      } else if (node instanceof ConstructorName) {
        return ((ConstructorName) node).toSource();
      } else if (node instanceof FieldDeclaration) {
        return getNames(((FieldDeclaration) node).getFields());
      } else if (node instanceof FunctionDeclaration) {
        SimpleIdentifier nameNode = ((FunctionDeclaration) node).getName();
        if (nameNode != null) {
          return nameNode.getName();
        }
      } else if (node instanceof FunctionTypeAlias) {
        return ((FunctionTypeAlias) node).getName().getName();
      } else if (node instanceof Identifier) {
        return ((Identifier) node).getName();
      } else if (node instanceof MethodDeclaration) {
        return ((MethodDeclaration) node).getName().getName();
      } else if (node instanceof TopLevelVariableDeclaration) {
        return getNames(((TopLevelVariableDeclaration) node).getVariables());
      } else if (node instanceof TypeName) {
        return ((TypeName) node).toSource();
      } else if (node instanceof TypeParameter) {
        return ((TypeParameter) node).getName().getName();
      } else if (node instanceof VariableDeclaration) {
        return ((VariableDeclaration) node).getName().getName();
      }
      return null;
    }

    /**
     * Return a string containing a comma-separated list of the names of all of the variables in the
     * given list.
     * 
     * @param variables the list containing the variables
     * @return a comma-separated list of the names of the given variables
     */
    private String getNames(VariableDeclarationList variables) {
      boolean first = true;
      StringBuilder builder = new StringBuilder();
      for (VariableDeclaration variable : variables.getVariables()) {
        if (first) {
          first = false;
        } else {
          builder.append(", ");
        }
        builder.append(variable.getName().getName());
      }
      return builder.toString();
    }
  }

  private static class PropertiesContentProvider implements IStructuredContentProvider {
    private static Object[] NO_ELEMENTS = new Object[0];

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getElements(Object inputElement) {
      HashMap<String, String> propertyMap = new HashMap<String, String>();
      addProperties(propertyMap, inputElement);
      if (propertyMap.isEmpty()) {
        return NO_ELEMENTS;
      }
      int count = propertyMap.size();
      String[] names = propertyMap.keySet().toArray(new String[count]);
      Arrays.sort(names);
      String[][] elements = new String[count][];
      for (int i = 0; i < count; i++) {
        String name = names[i];
        elements[i] = new String[] {name, propertyMap.get(name)};
      }
      return elements;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    private void addProperties(HashMap<String, String> propertyMap, Object inputElement) {
      if (inputElement instanceof AstNode) {
        AstNode node = (AstNode) inputElement;
        propertyMap.put("offset", Integer.toString(node.getOffset()));
        propertyMap.put("length", Integer.toString(node.getLength()));
      }
      if (inputElement instanceof Expression) {
        Expression expression = (Expression) inputElement;
        propertyMap.put("staticType", toString(expression.getStaticType()));
        propertyMap.put("propagatedType", toString(expression.getPropagatedType()));
      }
      if (inputElement instanceof BinaryExpression) {
        BinaryExpression expression = (BinaryExpression) inputElement;
        propertyMap.put("staticElement", toString(expression.getStaticElement()));
        propertyMap.put("propagatedElement", toString(expression.getPropagatedElement()));
      } else if (inputElement instanceof CompilationUnit) {
        CompilationUnit unit = (CompilationUnit) inputElement;
        propertyMap.put("element", toString(unit.getElement()));
      } else if (inputElement instanceof ExportDirective) {
        ExportDirective directive = (ExportDirective) inputElement;
        propertyMap.put("element", toString(directive.getElement()));
      } else if (inputElement instanceof FunctionExpressionInvocation) {
        FunctionExpressionInvocation expression = (FunctionExpressionInvocation) inputElement;
        propertyMap.put("staticElement", toString(expression.getStaticElement()));
        propertyMap.put("propagatedElement", toString(expression.getPropagatedElement()));
      } else if (inputElement instanceof ImportDirective) {
        ImportDirective directive = (ImportDirective) inputElement;
        propertyMap.put("element", toString(directive.getElement()));
      } else if (inputElement instanceof LibraryDirective) {
        LibraryDirective directive = (LibraryDirective) inputElement;
        propertyMap.put("element", toString(directive.getElement()));
      } else if (inputElement instanceof PartDirective) {
        PartDirective directive = (PartDirective) inputElement;
        propertyMap.put("element", toString(directive.getElement()));
      } else if (inputElement instanceof PartOfDirective) {
        PartOfDirective directive = (PartOfDirective) inputElement;
        propertyMap.put("element", toString(directive.getElement()));
      } else if (inputElement instanceof PostfixExpression) {
        PostfixExpression expression = (PostfixExpression) inputElement;
        propertyMap.put("staticElement", toString(expression.getStaticElement()));
        propertyMap.put("propagatedElement", toString(expression.getPropagatedElement()));
      } else if (inputElement instanceof PrefixExpression) {
        PrefixExpression expression = (PrefixExpression) inputElement;
        propertyMap.put("staticElement", toString(expression.getStaticElement()));
        propertyMap.put("propagatedElement", toString(expression.getPropagatedElement()));
      } else if (inputElement instanceof SimpleIdentifier) {
        SimpleIdentifier identifier = (SimpleIdentifier) inputElement;
        propertyMap.put("staticElement", toString(identifier.getStaticElement()));
        propertyMap.put("propagatedElement", toString(identifier.getPropagatedElement()));
      }
    }

    private String toString(Element element) {
      if (element == null) {
        return "null";
      }
      String name = element.getDisplayName();
      if (name == null) {
        name = "- unnamed -";
      }
      return name + " (" + element.getClass().getSimpleName() + ")";
    }

    private String toString(Type type) {
      if (type == null) {
        return "null";
      }
      String name = type.getDisplayName();
      if (name == null) {
        return "- unnamed -";
      }
      return name;
    }
  }

  private static class PropertiesLabelProvider implements ITableLabelProvider {
    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
      if (element instanceof String[]) {
        return ((String[]) element)[columnIndex];
      }
      return null;
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }
  }

  /**
   * View extension id.
   */
  public static final String ID = "com.google.dart.dev.util.ast.ASTExplorer";

  private TreeViewer treeViewer;
  private TableViewer tableViewer;

  private Action expandAllAction;
  private Action collapseAllAction;
  private Action refreshAction;

  private EventHandler eventHandler = new EventHandler();

  private ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();

  @Override
  public void createPartControl(Composite parent) {
    // TODO(brianwilkerson) It would be nice to have a slider between the tree and the table.
    parent.setLayout(new FillLayout(SWT.VERTICAL));

    treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    treeViewer.setContentProvider(new ExplorerContentProvider());
    treeViewer.setLabelProvider(new ExplorerLabelProvider());

    tableViewer = new TableViewer(parent);
    tableViewer.setContentProvider(new PropertiesContentProvider());
    tableViewer.setLabelProvider(new PropertiesLabelProvider());
    tableViewer.getTable().setHeaderVisible(true);

    TableColumn nameColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
    nameColumn.setResizable(true);
    nameColumn.setText("Name");
    nameColumn.setWidth(150);

    TableColumn valueColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
    valueColumn.setResizable(true);
    valueColumn.setText("Value");
    valueColumn.setWidth(600);

    makeActions();
    contributeToActionBars();
    hookupListeners();

    refresh();
  }

  @Override
  public void dispose() {
    eventHandler.dispose();
  }

  @Override
  public void onError(AnalysisError error) {
    errors.add(error);
  }

  @Override
  public void setFocus() {
    refresh();
    treeViewer.getControl().setFocus();
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
    manager.add(refreshAction);
  }

  private CompilationUnit getCompilationUnit() {
    IEditorPart editor = getActiveEditor();
    if (editor instanceof CompilationUnitEditor) {
      CompilationUnit unit = ((CompilationUnitEditor) editor).getInputUnit();
      if (unit != null) {
        return unit;
      }
    }
    if (editor != null) {

      IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput || input instanceof FileStoreEditorInput) {
        try {
          String contents = "";
          if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            contents = IFileUtilities.getContents(file);
          } else {
            URI uri = ((FileStoreEditorInput) input).getURI();
            contents = FileUtilities.getDartContents(new File(uri));
          }

          errors.clear();

          Scanner scanner = new Scanner(null, new CharSequenceReader(contents), this);
          Parser parser = new Parser(null, this);

          Token token = scanner.tokenize();
          return parser.parseCompilationUnit(token);
        } catch (CoreException e) {
          DartDevPlugin.logError(e);
        } catch (IOException e) {
          DartDevPlugin.logError(e);
        }
      }
    }
    return null;
  }

  /**
   * Return the element associated with the given AST node, or {@code null} if there is no element.
   * 
   * @param object the AST node whose associated element is to be returned
   * @return the element associated with the given AST node
   */
  private Element getElement(Object object) {
    if (object instanceof BinaryExpression) {
      return ((BinaryExpression) object).getStaticElement();
    } else if (object instanceof CompilationUnit) {
      return ((CompilationUnit) object).getElement();
    } else if (object instanceof ExportDirective) {
      return ((ExportDirective) object).getElement();
    } else if (object instanceof FunctionExpressionInvocation) {
      return ((FunctionExpressionInvocation) object).getStaticElement();
    } else if (object instanceof ImportDirective) {
      return ((ImportDirective) object).getElement();
    } else if (object instanceof LibraryDirective) {
      return ((LibraryDirective) object).getElement();
    } else if (object instanceof PartDirective) {
      return ((PartDirective) object).getElement();
    } else if (object instanceof PartOfDirective) {
      return ((PartOfDirective) object).getElement();
    } else if (object instanceof PostfixExpression) {
      return ((PostfixExpression) object).getStaticElement();
    } else if (object instanceof PrefixExpression) {
      return ((PrefixExpression) object).getStaticElement();
    } else if (object instanceof SimpleIdentifier) {
      return ((SimpleIdentifier) object).getBestElement();
    }
    return null;
  }

  private void hookupListeners() {
    treeViewer.addSelectionChangedListener(eventHandler);
    getSelectionService().addSelectionListener(eventHandler);
    getPage().addPartListener(eventHandler);
  }

  private void makeActions() {

    expandAllAction = new Action() {
      @Override
      public void run() {
        if (!treeViewer.getControl().isDisposed()) {
          treeViewer.getControl().setRedraw(false);
          treeViewer.expandToLevel(treeViewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
          treeViewer.getControl().setRedraw(true);
        }
      }
    };
    expandAllAction.setText("Expand All");
    expandAllAction.setToolTipText("Expand All");
    expandAllAction.setImageDescriptor(DartDevPlugin.getImageDescriptor("expandall.gif"));

    collapseAllAction = new Action() {
      @Override
      public void run() {
        if (!treeViewer.getControl().isDisposed()) {
          treeViewer.getControl().setRedraw(false);
          treeViewer.collapseToLevel(treeViewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
          treeViewer.getControl().setRedraw(true);
        }
      }
    };
    collapseAllAction.setText("Collapse All");
    collapseAllAction.setToolTipText("Collapse All");
    collapseAllAction.setImageDescriptor(DartDevPlugin.getImageDescriptor("collapseall.gif"));

    refreshAction = new Action() {
      @Override
      public void run() {
        refresh();
      }
    };
    refreshAction.setText("Refresh");
    refreshAction.setToolTipText("Refresh");
    refreshAction.setImageDescriptor(DartDevPlugin.getImageDescriptor("refresh.gif"));
  }

  private void refresh() {
    treeViewer.setInput(getCompilationUnit());
  }

  private void selectNodeAtOffset(int offset, int length) {
    //TODO(pquitslund): implement
    if (length <= 0) {
      return;
    }
//    NodeLocator locator = new NodeLocator(offset, offset + length - 1);
//    ASTNode node = locator.searchWithin(getCompilationUnit());
//    viewer.setSelection(new StructuredSelection(node), true);
  }

}

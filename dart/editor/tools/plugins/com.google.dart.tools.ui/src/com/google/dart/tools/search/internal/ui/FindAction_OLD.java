/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.search.internal.ui;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Annotation;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.utilities.ast.DartElementLocator;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.ActionInstrumentationUtilities;
import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.search.DartSearchQuery;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.search.ElementQuerySpecification;
import com.google.dart.tools.ui.search.NodeQuerySpecification;
import com.google.dart.tools.ui.search.QuerySpecification;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.IEditorStatusLine;

/**
 * Abstract class for Dart search actions.
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class FindAction_OLD extends InstrumentedSelectionDispatchAction {

  private static final class DummyElement implements Element {

    @Override
    public <R> R accept(ElementVisitor<R> visitor) {
      return null;
    }

    @Override
    public <E extends Element> E getAncestor(Class<E> elementClass) {
      return null;
    }

    @Override
    public AnalysisContext getContext() {
      return null;
    }

    @Override
    public Element getEnclosingElement() {
      return null;
    }

    @Override
    public ElementKind getKind() {
      return null;
    }

    @Override
    public LibraryElement getLibrary() {
      return null;
    }

    @Override
    public ElementLocation getLocation() {
      return null;
    }

    @Override
    public Annotation[] getMetadata() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public int getNameOffset() {
      return -1;
    }

    @Override
    public Source getSource() {
      return null;
    }

    @Override
    public boolean isAccessibleIn(LibraryElement library) {
      return false;
    }

    @Override
    public boolean isSynthetic() {
      return false;
    }

    @Override
    public void visitChildren(ElementVisitor<?> visitor) {
    }

  }

  // A dummy element that can't be selected in the UI
  private static final Object /*Element*/RETURN_WITHOUT_BEEP = createDummyElement();

  private static Object /*Element*/createDummyElement() {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      return new DummyElement();
    } else {
      return DartCore.create(DartToolsPlugin.getWorkspace().getRoot());
    }
  }

  private DartEditor editor;
  private Class<?>[] validTypes;

  FindAction_OLD(DartEditor editor) {
    this(editor.getEditorSite());
    this.editor = editor;
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  FindAction_OLD(IWorkbenchSite site) {
    super(site);
    validTypes = getValidTypes();
    init();
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(canOperateOn(selection));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
  }

  /**
   * Executes this action for the given dart element.
   * 
   * @param element The dart element to be found.
   */
  protected void doFind(DartElement element, InstrumentationBuilder instrumentation) {
    ActionInstrumentationUtilities.recordElement(element, instrumentation);

    if (!ActionUtil.isProcessable(getShell(), element)) {
      instrumentation.metric("Problem", "Element is not processable");
      return;
    }

    // will return true except for debugging purposes.
    try {
      performNewSearch(new DartSearchQuery(createQuery(element)));
    } catch (DartModelException ex) {
      ExceptionHandler.handle(
          ex,
          getShell(),
          SearchMessages.Search_Error_search_notsuccessful_title,
          SearchMessages.Search_Error_search_notsuccessful_message);
    } catch (InterruptedException e) {
      // cancelled
      instrumentation.metric("Problem", "User cancelled");
    }
  }

  protected void doFind(DartNode node, InstrumentationBuilder instrumentation) {
    ActionInstrumentationUtilities.record(node, instrumentation);
    try {
      performNewSearch(new DartSearchQuery(createQuery(node)));
    } catch (DartModelException ex) {
      ExceptionHandler.handle(
          ex,
          getShell(),
          SearchMessages.Search_Error_search_notsuccessful_title,
          SearchMessages.Search_Error_search_notsuccessful_message);
    } catch (InterruptedException e) {
      // cancelled
      instrumentation.metric("Problem", "User cancelled");
    }
  }

  protected void doFind(Element element, InstrumentationBuilder instrumentation) {
    ActionInstrumentationUtilities.recordElement(element, instrumentation);

    if (!ActionUtil.isProcessable(getShell(), element)) {
      instrumentation.metric("Problem", "Element is not processable");
      return;
    }

    // will return true except for debugging purposes.
    try {
      performNewSearch(new DartSearchQuery(createQuery(element)));
    } catch (DartModelException ex) {
      ExceptionHandler.handle(
          ex,
          getShell(),
          SearchMessages.Search_Error_search_notsuccessful_title,
          SearchMessages.Search_Error_search_notsuccessful_message);
    } catch (InterruptedException e) {
      // cancelled
      instrumentation.metric("Problem", "User cancelled");
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {

    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {

      Element element = getDartElement2(selection, false);
      if (element == null) {
        instrumentation.metric("Problem", "Element null or not exist, showing ");
        showOperationUnavailableDialog();
        return;
      } else if (element == RETURN_WITHOUT_BEEP) {
        instrumentation.metric("Problem", "Find action on non-selectable element");
        return;
      }

      ActionInstrumentationUtilities.recordElement(element, instrumentation);
      doFind(element, instrumentation);

    } else {

      DartElement element = getDartElement(selection, false);
      if (element == null || !element.exists()) {
        instrumentation.metric("Problem", "Element null or not exist, showing ");
        showOperationUnavailableDialog();
        return;
      } else if (element == RETURN_WITHOUT_BEEP) {
        instrumentation.metric("Problem", "Find action on non-selectable element");
        return;
      }

      ActionInstrumentationUtilities.recordElement(element, instrumentation);
      doFind(element, instrumentation);

    }

  }

  @Override
  protected void doRun(ITextSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    if (!ActionUtil.isProcessable(editor)) {
      instrumentation.metric("Problem", "Editor is not processable");
      return;
    }
    DartUnit ast = ((CompilationUnitEditor) editor).getAST();
    CompilationUnit compilationUnit = (CompilationUnit) EditorUtility.getEditorInputDartElement(
        editor,
        false);
    instrumentation.record(compilationUnit);
    int offset = selection.getOffset();
    DartElementLocator elementLocator = new DartElementLocator(compilationUnit, offset, true);
    DartElement dartElement = elementLocator.searchWithin(ast);
    ActionInstrumentationUtilities.recordElement(dartElement, instrumentation);
    if (canOperateOn(dartElement)) {
      doFind(dartElement, instrumentation);
      return;
    } else {
      NodeFinder finder = NodeFinder.find(ast, offset, selection.getLength());
      DartNode node = finder.getCoveredNode();
      if (node == null) {
        node = finder.getCoveringNode();
      }
      if (node != null) {
        ActionInstrumentationUtilities.record(node, instrumentation);
        doFind(node, instrumentation);
        return;
      }
    }
    // not reached on successful search
    instrumentation.metric("Problem", "FindAction on unresovlable selection, showing dialog");
    if (editor != null) {
      IEditorStatusLine statusLine = (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
      if (statusLine != null) {
        statusLine.setMessage(true, SearchMessages.FindAction_unresolvable_selection, null);
      }
    }
    showOperationUnavailableDialog();
  }

  boolean canOperateOn(DartElement element) {
    if (element == null || validTypes == null || validTypes.length == 0
        || !ActionUtil.isOnBuildPath(element)) {
      return false;
    }

    for (int i = 0; i < validTypes.length; i++) {
      if (validTypes[i].isInstance(element)) {
        if (element.getElementType() == DartElement.LIBRARY) {
          return hasChildren((DartLibrary) element);
        } else {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Creates a query for the given element. Subclasses reimplement this method.
   * 
   * @param element the element to create a query for
   * @return returns the query
   * @throws DartModelException thrown when accessing the element failed
   * @throws InterruptedException thrown when the user interrupted the query selection
   */
  QuerySpecification createQuery(DartElement element) throws DartModelException,
      InterruptedException {
    SearchScope scope = SearchScopeFactory.createWorkspaceScope();
    return new ElementQuerySpecification(element, getLimitTo(), scope, "workspace"); //$NON-NLS-1$
  }

  /**
   * Creates a query for the given element.
   */
  QuerySpecification createQuery(DartNode element) throws DartModelException, InterruptedException {
    SearchScope scope = SearchScopeFactory.createWorkspaceScope();
    return new NodeQuerySpecification(element, getLimitTo(), scope, "workspace"); //$NON-NLS-1$
  }

  /**
   * Creates a query for the given element. Subclasses reimplement this method.
   * 
   * @param element the element to create a query for
   * @return returns the query
   * @throws DartModelException thrown when accessing the element failed
   * @throws InterruptedException thrown when the user interrupted the query selection
   */
  QuerySpecification createQuery(Element element) throws DartModelException, InterruptedException {
    //TODO (pquitslund): add support for new element queries
    return null;
  }

  DartElement getDartElement(IStructuredSelection selection, boolean silent) {
    if (selection.size() == 1) {
      Object firstElement = selection.getFirstElement();
      DartElement elem = null;
      if (firstElement instanceof DartElement) {
        elem = (DartElement) firstElement;
      } else if (firstElement instanceof IAdaptable) {
        elem = (DartElement) ((IAdaptable) firstElement).getAdapter(DartElement.class);
      }
      if (elem != null) {
        return getTypeIfPossible(elem, silent);
      }

    }
    return null;
  }

  Element getDartElement2(IStructuredSelection selection, boolean silent) {
    if (selection.size() == 1) {
      Object firstElement = selection.getFirstElement();
      Element elem = null;
      if (firstElement instanceof Element) {
        elem = (Element) firstElement;
      } else if (firstElement instanceof IAdaptable) {
        elem = (Element) ((IAdaptable) firstElement).getAdapter(Element.class);
      }
      if (elem != null) {
        return getTypeIfPossible2(elem, silent);
      }

    }
    return null;
  }

  DartEditor getEditor() {
    return editor;
  }

  abstract int getLimitTo();

  String getOperationUnavailableMessage() {
    return SearchMessages.DartElementAction_operationUnavailable_generic;
  }

  Type getType(DartElement element) {
    if (element == null) {
      return null;
    }

    Type type = null;
    if (element.getElementType() == DartElement.TYPE) {
      type = (Type) element;
    } else if (element instanceof TypeMember) {
      type = ((TypeMember) element).getDeclaringType();
    }
//    else if (element instanceof ILocalVariable) {
//      type= (Type)element.getAncestor(DartElement.TYPE);
//    }
    return type;
  }

  /**
   * Called once by the constructors to get the list of the valid input types of the action. To be
   * overridden by implementors of this action.
   * 
   * @return the valid input types of the action
   */
  abstract Class<?>[] getValidTypes();

  /**
   * Called once by the constructors to initialize label, tooltip, image and help support of the
   * action. To be overridden by implementors of this action.
   */
  abstract void init();

  private boolean canOperateOn(IStructuredSelection sel) {
    return sel != null && !sel.isEmpty() && canOperateOn(getDartElement(sel, true));
  }

  private DartElement findType(CompilationUnit cu, boolean silent) {
    Type[] types = null;
    try {
      types = cu.getTypes();
    } catch (DartModelException ex) {
      if (DartModelUtil.isExceptionToBeLogged(ex)) {
        ExceptionHandler.log(ex, SearchMessages.DartElementAction_error_open_message);
      }
      if (silent) {
        return (DartElement) RETURN_WITHOUT_BEEP;
      } else {
        return null;
      }
    }
    if (types.length == 1 || (silent && types.length > 0)) {
      return types[0];
    }
    if (silent) {
      return (DartElement) RETURN_WITHOUT_BEEP;
    }
    if (types.length == 0) {
      return null;
    }
    String title = SearchMessages.DartElementAction_typeSelectionDialog_title;
    String message = SearchMessages.DartElementAction_typeSelectionDialog_message;
    int flags = (DartElementLabelProvider.SHOW_DEFAULT);

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(),
        new DartElementLabelProvider(flags));
    dialog.setTitle(title);
    dialog.setMessage(message);
    dialog.setElements(types);

    if (dialog.open() == Window.OK) {
      return (Type) dialog.getFirstResult();
    } else {
      return (DartElement) RETURN_WITHOUT_BEEP;
    }
  }

  private Element findType(CompilationUnitElement cu, boolean silent) {

    ClassElement[] types = cu.getTypes();

    if (types.length == 1 || (silent && types.length > 0)) {
      return types[0];
    }
    if (silent) {
      return (Element) RETURN_WITHOUT_BEEP;
    }
    if (types.length == 0) {
      return null;
    }
    String title = SearchMessages.DartElementAction_typeSelectionDialog_title;
    String message = SearchMessages.DartElementAction_typeSelectionDialog_message;
    int flags = (DartElementLabelProvider.SHOW_DEFAULT);

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(),
        new DartElementLabelProvider(flags));
    dialog.setTitle(title);
    dialog.setMessage(message);
    dialog.setElements(types);

    if (dialog.open() == Window.OK) {
      return (Element) dialog.getFirstResult();
    } else {
      return (Element) RETURN_WITHOUT_BEEP;
    }

  }

  private DartElement getTypeIfPossible(DartElement o, boolean silent) {
    switch (o.getElementType()) {
      case DartElement.COMPILATION_UNIT:
        if (silent) {
          return o;
        } else {
          return findType((CompilationUnit) o, silent);
        }
      default:
        return o;
    }
  }

  private Element getTypeIfPossible2(Element o, boolean silent) {
    switch (o.getKind()) {
      case COMPILATION_UNIT:
        if (silent) {
          return o;
        } else {
          return findType((CompilationUnitElement) o, silent);
        }
      default:
        return o;
    }
  }

  private boolean hasChildren(DartLibrary library) {
    try {
      return library.hasChildren();
    } catch (DartModelException ex) {
      return false;
    }
  }

  private void performNewSearch(DartSearchQuery query) throws DartModelException,
      InterruptedException {
    if (query.canRunInBackground()) {
      NewSearchUI.runQueryInBackground(query);
    } else {
      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
      IStatus status = NewSearchUI.runQueryInForeground(progressService, query);
      if (status.matches(IStatus.ERROR | IStatus.INFO | IStatus.WARNING)) {
        ErrorDialog.openError(
            getShell(),
            SearchMessages.Search_Error_search_title,
            SearchMessages.Search_Error_search_message,
            status);
      }
    }
  }

  private void showOperationUnavailableDialog() {
    MessageDialog.openInformation(
        getShell(),
        SearchMessages.DartElementAction_operationUnavailable_title,
        getOperationUnavailableMessage());
  }

}

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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.search.DartSearchQuery;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.search.ElementQuerySpecification;
import com.google.dart.tools.ui.search.QuerySpecification;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import java.lang.reflect.InvocationTargetException;

/**
 * Abstract class for Dart search actions.
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class FindAction extends SelectionDispatchAction {

  // A dummy element that can't be selected in the UI
  private static final DartElement RETURN_WITHOUT_BEEP = DartCore.create(DartToolsPlugin.getWorkspace().getRoot());

  private DartEditor editor;
  private Class<?>[] validTypes;

  FindAction(DartEditor editor) {
    this(editor.getEditorSite());
    this.editor = editor;
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  FindAction(IWorkbenchSite site) {
    super(site);
    validTypes = getValidTypes();
    init();
  }

  /**
   * Executes this action for the given dart element.
   * 
   * @param element The dart element to be found.
   */
  public void run(DartElement element) {

    if (!ActionUtil.isProcessable(getShell(), element)) {
      return;
    }

    // will return true except for debugging purposes.
    try {
      performNewSearch(element);
    } catch (DartModelException ex) {
      ExceptionHandler.handle(ex, getShell(),
          SearchMessages.Search_Error_search_notsuccessful_title,
          SearchMessages.Search_Error_search_notsuccessful_message);
    } catch (InterruptedException e) {
      // cancelled
    }
  }

  @Override
  public void run(IStructuredSelection selection) {
    DartElement element = getDartElement(selection, false);
    if (element == null || !element.exists()) {
      showOperationUnavailableDialog();
      return;
    } else if (element == RETURN_WITHOUT_BEEP) {
      return;
    }

    run(element);
  }

  @Override
  public void run(ITextSelection selection) {
    if (!ActionUtil.isProcessable(editor)) {
      return;
    }
    try {
      String title = SearchMessages.SearchElementSelectionDialog_title;
      String message = SearchMessages.SearchElementSelectionDialog_message;

      DartElement[] elements = SelectionConverter.codeResolveForked(editor, true);
      if (elements.length > 0 && canOperateOn(elements[0])) {
        DartElement element = elements[0];
        if (elements.length > 1) {
          element = SelectionConverter.selectJavaElement(elements, getShell(), title, message);
        }
        if (element != null) {
          run(element);
        }
      } else {
        showOperationUnavailableDialog();
      }
    } catch (InvocationTargetException ex) {
      boolean statusSet = false;
      if (editor != null) {
        IEditorStatusLine statusLine = (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
        if (statusLine != null) {
          statusLine.setMessage(true, SearchMessages.FindAction_unresolvable_selection, null);
          statusSet = true;
        }
      }
      if (!statusSet) {
        String title = SearchMessages.Search_Error_search_title;
        String message = SearchMessages.Search_Error_codeResolve;
        ExceptionHandler.handle(ex, getShell(), title, message);
      }
    } catch (InterruptedException e) {
      // ignore
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(canOperateOn(selection));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
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
        return RETURN_WITHOUT_BEEP;
      } else {
        return null;
      }
    }
    if (types.length == 1 || (silent && types.length > 0)) {
      return types[0];
    }
    if (silent) {
      return RETURN_WITHOUT_BEEP;
    }
    if (types.length == 0) {
      return null;
    }
    String title = SearchMessages.DartElementAction_typeSelectionDialog_title;
    String message = SearchMessages.DartElementAction_typeSelectionDialog_message;
    int flags = (DartElementLabelProvider.SHOW_DEFAULT);

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(),
        new DartElementLabelProvider(flags));
    dialog.setTitle(title);
    dialog.setMessage(message);
    dialog.setElements(types);

    if (dialog.open() == Window.OK) {
      return (Type) dialog.getFirstResult();
    } else {
      return RETURN_WITHOUT_BEEP;
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

  private boolean hasChildren(DartLibrary library) {
    try {
      return library.hasChildren();
    } catch (DartModelException ex) {
      return false;
    }
  }

  private void performNewSearch(DartElement element) throws DartModelException,
      InterruptedException {
    DartSearchQuery query = new DartSearchQuery(createQuery(element));
    if (query.canRunInBackground()) {
      NewSearchUI.runQueryInBackground(query);
    } else {
      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
      IStatus status = NewSearchUI.runQueryInForeground(progressService, query);
      if (status.matches(IStatus.ERROR | IStatus.INFO | IStatus.WARNING)) {
        ErrorDialog.openError(getShell(), SearchMessages.Search_Error_search_title,
            SearchMessages.Search_Error_search_message, status);
      }
    }
  }

  private void showOperationUnavailableDialog() {
    MessageDialog.openInformation(getShell(),
        SearchMessages.DartElementAction_operationUnavailable_title,
        getOperationUnavailableMessage());
  }

}

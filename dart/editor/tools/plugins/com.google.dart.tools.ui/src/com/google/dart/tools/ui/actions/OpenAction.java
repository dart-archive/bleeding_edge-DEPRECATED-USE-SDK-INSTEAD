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
package com.google.dart.tools.ui.actions;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.utilities.ast.DartElementLocator;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.DartModelUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import java.util.Iterator;

/**
 * This action opens a Dart editor on a Dart element or file.
 * <p>
 * The action is applicable to selections containing elements of type <code>ICompilationUnit</code>,
 * <code>IMember</code> or <code>IFile</code>.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenAction extends SelectionDispatchAction {

  private DartEditor fEditor;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public OpenAction(DartEditor editor) {
    this(editor.getEditorSite());
    fEditor = editor;
    setText(ActionMessages.OpenAction_declaration_label);
    setEnabled(EditorUtility.getEditorInputDartElement(fEditor, false) != null);
  }

  /**
   * Creates a new <code>OpenAction</code>. The action requires that the selection provided by the
   * site's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public OpenAction(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.OpenAction_label);
    setToolTipText(ActionMessages.OpenAction_tooltip);
    setDescription(ActionMessages.OpenAction_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.OPEN_ACTION);
  }

  /**
   * Note: this method is for internal use only. Clients should not call this method.
   * 
   * @param object the element to open
   * @return the real element to open
   * @throws DartModelException if an error occurs while accessing the Dart model
   * @noreference This method is not intended to be referenced by clients.
   */
  public Object getElementToOpen(Object object) throws DartModelException {
    return object;
  }

  /**
   * Note: this method is for internal use only. Clients should not call this method.
   * 
   * @param element the element to process
   * @param candidateRegion
   * @noreference This method is not intended to be referenced by clients.
   */
  public void run(DartElement element, IRegion candidateRegion) {
    if (element == null) {
      return;
    }
    IStatus status = Status.OK_STATUS;
    try {
      Object elementToOpen = getElementToOpen(element);
      boolean activateOnOpen = fEditor != null ? true : OpenStrategy.activateOnOpen();
      IEditorPart part = EditorUtility.openInEditor(elementToOpen, activateOnOpen);
      if (part != null && elementToOpen instanceof DartElement) {
        selectInEditor(part, (DartElement) elementToOpen, candidateRegion);
      }
    } catch (PartInitException exception) {
      String message = Messages.format(
          ActionMessages.OpenAction_error_problem_opening_editor,
          new String[] {
              DartElementLabels.getTextLabel(element, DartElementLabels.ALL_DEFAULT),
              exception.getStatus().getMessage()});
      status = new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, IStatus.ERROR, message, null);
    } catch (CoreException exception) {
      String message = Messages.format(
          ActionMessages.OpenAction_error_problem_opening_editor,
          new String[] {
              DartElementLabels.getTextLabel(element, DartElementLabels.ALL_DEFAULT),
              exception.getStatus().getMessage()});
      status = new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, IStatus.ERROR, message, null);
      DartToolsPlugin.log(exception);
    }
    if (!status.isOK()) {
      ErrorDialog.openError(
          getShell(),
          getDialogTitle(),
          ActionMessages.OpenAction_error_message,
          status);
    }
  }

  @Override
  public void run(IStructuredSelection selection) {
    EmitInstrumentationCommand();
    if (!checkEnabled(selection)) {
      return;
    }
    run(selection.toArray());
  }

  @Override
  public void run(ITextSelection selection) {
    EmitInstrumentationCommand();
    if (!isProcessable()) {
      return;
    }
    DartElement element = EditorUtility.getEditorInputDartElement(fEditor, false);
    if (!(element instanceof CompilationUnit)) {
      return;
    }
    DartElementLocator locator = new DartElementLocator(
        (CompilationUnit) element,
        selection.getOffset(),
        selection.getOffset());
    DartUnit unit = fEditor.getAST();
    DartElement targetElement = unit == null ? null : locator.searchWithin(unit);
    if (targetElement == null) {
      IEditorStatusLine statusLine = (IEditorStatusLine) fEditor.getAdapter(IEditorStatusLine.class);
      if (statusLine != null) {
        statusLine.setMessage(true, ActionMessages.OpenAction_error_messageBadSelection, null);
      }
      getShell().getDisplay().beep();
      return;
    }
//    IRegion candidateRegion = locator.getCandidateRegion();
    run(targetElement, locator.getCandidateRegion());
//    try {
//      DartElement[] elements = SelectionConverter.codeResolveForked(fEditor, false);
//      elements = selectOpenableElements(elements);
//      if (elements == null || elements.length == 0) {
//        IEditorStatusLine statusLine = (IEditorStatusLine) fEditor.getAdapter(IEditorStatusLine.class);
//        if (statusLine != null) {
//          statusLine.setMessage(true, ActionMessages.OpenAction_error_messageBadSelection, null);
//        }
//        getShell().getDisplay().beep();
//        return;
//      }
//
//      DartElement element = elements[0];
//      if (elements.length > 1) {
//        element = SelectionConverter.selectJavaElement(elements, getShell(), getDialogTitle(),
//            ActionMessages.OpenAction_select_element);
//        if (element == null) {
//          return;
//        }
//      }
//
//      run(new Object[] {element});
//    } catch (InvocationTargetException e) {
//      boolean statusSet = false;
//      if (fEditor != null) {
//        IEditorStatusLine statusLine = (IEditorStatusLine) fEditor.getAdapter(IEditorStatusLine.class);
//        if (statusLine != null) {
//          statusLine.setMessage(true, ActionMessages.OpenAction_error_messageBadSelection, null);
//          statusSet = true;
//        }
//      }
//      if (!statusSet) {
//        ExceptionHandler.handle(e, getShell(), getDialogTitle(),
//            ActionMessages.OpenAction_error_message);
//      }
//    } catch (InterruptedException e) {
//      // ignore
//    }
  }

  /**
   * Note: this method is for internal use only. Clients should not call this method.
   * 
   * @param elements the elements to process
   * @noreference This method is not intended to be referenced by clients.
   */
  public void run(Object[] elements) {
    EmitInstrumentationCommand();
    if (elements == null) {
      return;
    }

    MultiStatus status = new MultiStatus(
        DartToolsPlugin.PLUGIN_ID,
        IStatus.OK,
        ActionMessages.OpenAction_multistatus_message,
        null);

    for (int i = 0; i < elements.length; i++) {
      Object element = elements[i];
      try {
        element = getElementToOpen(element);
        boolean activateOnOpen = fEditor != null ? true : OpenStrategy.activateOnOpen();
        IEditorPart part = EditorUtility.openInEditor(element, activateOnOpen);
        if (part != null && element instanceof DartElement) {
          selectInEditor(part, (DartElement) element);
        }
      } catch (PartInitException e) {
        String message = Messages.format(
            ActionMessages.OpenAction_error_problem_opening_editor,
            new String[] {
                DartElementLabels.getTextLabel(element, DartElementLabels.ALL_DEFAULT),
                e.getStatus().getMessage()});
        status.add(new Status(
            IStatus.ERROR,
            DartToolsPlugin.PLUGIN_ID,
            IStatus.ERROR,
            message,
            null));
      } catch (CoreException e) {
        String message = Messages.format(
            ActionMessages.OpenAction_error_problem_opening_editor,
            new String[] {
                DartElementLabels.getTextLabel(element, DartElementLabels.ALL_DEFAULT),
                e.getStatus().getMessage()});
        status.add(new Status(
            IStatus.ERROR,
            DartToolsPlugin.PLUGIN_ID,
            IStatus.ERROR,
            message,
            null));
        DartToolsPlugin.log(e);
      }
    }
    if (!status.isOK()) {
      IStatus[] children = status.getChildren();
      ErrorDialog.openError(
          getShell(),
          getDialogTitle(),
          ActionMessages.OpenAction_error_message,
          children.length == 1 ? children[0] : status);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(checkEnabled(selection));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
  }

  protected void selectInEditor(IEditorPart part, DartElement element) {
    DartUI.revealInEditor(part, element);
  }

  protected void selectInEditor(IEditorPart part, DartElement element, IRegion candidateRegion) {
    if (candidateRegion == null) {
      DartUI.revealInEditor(part, element);
    } else {
      EditorUtility.revealInEditor(part, candidateRegion.getOffset(), candidateRegion.getLength());
    }
  }

  private boolean checkEnabled(IStructuredSelection selection) {
    if (selection.isEmpty()) {
      return false;
    }
    for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
      Object element = iter.next();
      if (element instanceof SourceReference) {
        continue;
      }
      if (element instanceof IFile) {
        continue;
      }
      if (DartModelUtil.isOpenableStorage(element)) {
        continue;
      }
      if (element instanceof HTMLFile) {
        continue;
      }
      return false;
    }
    return true;
  }

  private String getDialogTitle() {
    return ActionMessages.OpenAction_error_title;
  }

  private boolean isProcessable() {
    if (fEditor != null) {
      DartElement de = EditorUtility.getEditorInputDartElement(fEditor, false);
      if (de instanceof CompilationUnit && !DartModelUtil.isPrimary((CompilationUnit) de)) {
        return true; // can process non-primary working copies
      }
    }
    return ActionUtil.isProcessable(fEditor);
  }

  /**
   * Selects the openable elements out of the given ones.
   * 
   * @param elements the elements to filter
   * @return the openable elements
   */
//  private DartElement[] selectOpenableElements(DartElement[] elements) {
//    List<DartElement> result = new ArrayList<DartElement>(elements.length);
//    for (int i = 0; i < elements.length; i++) {
//      DartElement element = elements[i];
//      switch (element.getElementType()) {
////        case IJavaElement.PACKAGE_DECLARATION:
////        case IJavaElement.PACKAGE_FRAGMENT:
////        case IJavaElement.PACKAGE_FRAGMENT_ROOT:
////        case IJavaElement.JAVA_PROJECT:
////        case IJavaElement.JAVA_MODEL:
//      //TODO (pquitslund): add more element types to filter
//        case DartElement.DART_MODEL:
//        case DartElement.DART_PROJECT:
//        case DartElement.LIBRARY:
//          break;
//        default:
//          result.add(element);
//          break;
//      }
//    }
//    return result.toArray(new DartElement[result.size()]);
//  }
}

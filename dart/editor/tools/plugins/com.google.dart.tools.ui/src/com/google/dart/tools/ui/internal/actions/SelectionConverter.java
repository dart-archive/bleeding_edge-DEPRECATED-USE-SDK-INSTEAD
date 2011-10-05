/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.core.model.CodeAssistElement;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.DartModelUtil;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

public class SelectionConverter {

  private static final DartElement[] EMPTY_RESULT = new DartElement[0];

  public static boolean canOperateOn(DartEditor editor) {
    if (editor == null) {
      return false;
    }
    return getInput(editor) != null;

  }

  public static DartElement[] codeResolve(DartEditor editor) throws DartModelException {
    return codeResolve(editor, true);
  }

  /**
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   */
  public static DartElement[] codeResolve(DartEditor editor, boolean primaryOnly)
      throws DartModelException {
    return codeResolve(getInput(editor, primaryOnly),
        (ITextSelection) editor.getSelectionProvider().getSelection());
  }

  public static DartElement[] codeResolve(DartElement input, ITextSelection selection)
      throws DartModelException {
    if (input instanceof CodeAssistElement) {
      if (input instanceof CompilationUnit) {
        DartModelUtil.reconcile((CompilationUnit) input);
      }
      DartElement[] elements = ((CodeAssistElement) input).codeSelect(selection.getOffset()
          + selection.getLength(), 0);
      if (elements.length > 0) {
        return elements;
      }
    }
    return EMPTY_RESULT;
  }

  /**
   * Perform a code resolve in a separate thread.
   * 
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   * @throws InterruptedException
   * @throws InvocationTargetException
   */
  public static DartElement[] codeResolveForked(DartEditor editor, boolean primaryOnly)
      throws InvocationTargetException, InterruptedException {
    return performForkedCodeResolve(getInput(editor, primaryOnly),
        (ITextSelection) editor.getSelectionProvider().getSelection());
  }

  public static DartElement[] codeResolveOrInputForked(DartEditor editor)
      throws InvocationTargetException, InterruptedException {
    DartElement input = getInput(editor);
    ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
    DartElement[] result = performForkedCodeResolve(input, selection);
    if (result.length == 0) {
      result = new DartElement[] {input};
    }
    return result;
  }

  public static DartElement getElementAtOffset(DartEditor editor) throws DartModelException {
    return getElementAtOffset(editor, true);
  }

  public static DartElement getElementAtOffset(DartElement input, ITextSelection selection)
      throws DartModelException {
    if (input instanceof CompilationUnit) {
      CompilationUnit cunit = (CompilationUnit) input;
      DartModelUtil.reconcile(cunit);
      DartElement ref = cunit.getElementAt(selection.getOffset());
      if (ref == null) {
        return input;
      } else {
        return ref;
      }
    }
    return null;
  }

  /**
   * Converts the given structured selection into an array of Java elements. An empty array is
   * returned if one of the elements stored in the structured selection is not of type
   * <code>DartElement</code>
   */
  public static DartElement[] getElements(IStructuredSelection selection) {
    if (!selection.isEmpty()) {
      DartElement[] result = new DartElement[selection.size()];
      int i = 0;
      for (Iterator<?> iter = selection.iterator(); iter.hasNext(); i++) {
        Object element = iter.next();
        if (!(element instanceof DartElement)) {
          return EMPTY_RESULT;
        }
        result[i] = (DartElement) element;
      }
      return result;
    }
    return EMPTY_RESULT;
  }

  public static DartElement getInput(DartEditor editor) {
    return getInput(editor, true);
  }

  public static CompilationUnit getInputAsCompilationUnit(DartEditor editor) {
    Object editorInput = SelectionConverter.getInput(editor);
    if (editorInput instanceof CompilationUnit) {
      return (CompilationUnit) editorInput;
    }
    return null;
  }

  /**
   * Converts the selection provided by the given part into a structured selection. The following
   * conversion rules are used:
   * <ul>
   * <li><code>part instanceof DartEditor</code>: returns a structured selection using code resolve
   * to convert the editor's text selection.</li>
   * <li><code>part instanceof IWorkbenchPart</code>: returns the part's selection if it is a
   * structured selection.</li>
   * <li><code>default</code>: returns an empty structured selection.</li>
   * </ul>
   */
  public static IStructuredSelection getStructuredSelection(IWorkbenchPart part)
      throws DartModelException {
    if (part instanceof DartEditor) {
      return new StructuredSelection(codeResolve((DartEditor) part));
    }
    ISelectionProvider provider = part.getSite().getSelectionProvider();
    if (provider != null) {
      ISelection selection = provider.getSelection();
      if (selection instanceof IStructuredSelection) {
        return (IStructuredSelection) selection;
      }
    }
    return StructuredSelection.EMPTY;
  }

  public static Type getTypeAtOffset(DartEditor editor) throws DartModelException {
    DartElement element = SelectionConverter.getElementAtOffset(editor);
    Type type = element.getAncestor(Type.class);
    if (type == null) {
      CompilationUnit unit = SelectionConverter.getInputAsCompilationUnit(editor);
      if (unit != null) {
        Type[] types = unit.getTypes();
        if (types.length == 0) {
          return null;
        }
        int n = ((ITextSelection) editor.getSelectionProvider().getSelection()).getOffset();
        Type p = types[0];
        for (Type t : types) {
          if (t.getSourceRange().getOffset() > n) {
            return p;
          }
          p = t;
        }
      }
    }
    return type;
  }

  public static DartElement resolveEnclosingElement(DartEditor editor, ITextSelection selection)
      throws DartModelException {
    return resolveEnclosingElement(getInput(editor), selection);
  }

  public static DartElement resolveEnclosingElement(DartElement input, ITextSelection selection)
      throws DartModelException {
    DartElement atOffset = null;
    if (input instanceof CompilationUnit) {
      CompilationUnit cunit = (CompilationUnit) input;
      DartModelUtil.reconcile(cunit);
      atOffset = cunit.getElementAt(selection.getOffset());
    } else {
      return null;
    }
    if (atOffset == null) {
      return input;
    } else {
      int selectionEnd = selection.getOffset() + selection.getLength();
      DartElement result = atOffset;
      if (atOffset instanceof SourceReference) {
        SourceRange range = ((SourceReference) atOffset).getSourceRange();
        while (range.getOffset() + range.getLength() < selectionEnd) {
          result = result.getParent();
          if (!(result instanceof SourceReference)) {
            result = input;
            break;
          }
          range = ((SourceReference) result).getSourceRange();
        }
      }
      return result;
    }
  }

  /**
   * Shows a dialog for resolving an ambiguous java element. Utility method that can be called by
   * subclasses.
   */
  public static DartElement selectJavaElement(DartElement[] elements, Shell shell, String title,
      String message) {
    DartX.todo();
//    int nResults = elements.length;
//    if (nResults == 0)
//      return null;
//    if (nResults == 1)
//      return elements[0];
//
//    int flags = JavaScriptElementLabelProvider.SHOW_DEFAULT
//        | JavaScriptElementLabelProvider.SHOW_QUALIFIED
//        | JavaScriptElementLabelProvider.SHOW_ROOT;
//
//    ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell,
//        new JavaScriptElementLabelProvider(flags));
//    dialog.setTitle(title);
//    dialog.setMessage(message);
//    dialog.setElements(elements);
//
//    if (dialog.open() == Window.OK) {
//      return (DartElement) dialog.getFirstResult();
//    }
    return null;
  }

  /**
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   */
  private static DartElement getElementAtOffset(DartEditor editor, boolean primaryOnly)
      throws DartModelException {
    return getElementAtOffset(getInput(editor, primaryOnly),
        (ITextSelection) editor.getSelectionProvider().getSelection());
  }

//	public static DartElement[] resolveSelectedElements(DartElement input, ITextSelection selection) throws DartModelException {
//		DartElement enclosing= resolveEnclosingElement(input, selection);
//		if (enclosing == null)
//			return EMPTY_RESULT;
//		if (!(enclosing instanceof SourceReference))
//			return EMPTY_RESULT;
//		SourceRange sr= ((SourceReference)enclosing).getSourceRange();
//		if (selection.getOffset() == sr.getOffset() && selection.getLength() == sr.getLength())
//			return new DartElement[] {enclosing};
//	}

  /**
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   */
  private static DartElement getInput(DartEditor editor, boolean primaryOnly) {
    if (editor == null) {
      return null;
    }
    return EditorUtility.getEditorInputJavaElement(editor, primaryOnly);
  }

  private static DartElement[] performForkedCodeResolve(final DartElement input,
      final ITextSelection selection) throws InvocationTargetException, InterruptedException {
    final class CodeResolveRunnable implements IRunnableWithProgress {
      DartElement[] result;

      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
          result = codeResolve(input, selection);
        } catch (DartModelException e) {
          throw new InvocationTargetException(e);
        }
      }
    }
    CodeResolveRunnable runnable = new CodeResolveRunnable();
    PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
    return runnable.result;
  }

  private SelectionConverter() {
    // no instance
  }
}

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
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

public class SelectionConverter {

  private static final DartElement[] EMPTY_RESULT = new DartElement[0];

  public static boolean canOperateOn(DartEditor editor) {
    if (editor == null) {
      return false;
    }
    if (!editor.isEditable()) {
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
    return codeResolve(editor, null, (ITextSelection) editor.getSelectionProvider().getSelection());
  }

  public static DartElement[] codeResolve(DartEditor editor, DartElement input,
      ITextSelection selection) throws DartModelException {
    return EMPTY_RESULT;
  }

  public static DartElement getElementAtOffset(DartElement input, ITextSelection selection)
      throws DartModelException {
    return null;
  }

  public static DartElement getInput(DartEditor editor) {
    // Legacy method (unsupported)
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

  /**
   * @param primaryOnly if <code>true</code> only primary working copies will be returned
   */
  private static DartElement getInput(DartEditor editor, boolean primaryOnly) {
    // Legacy method (unsupported)
    return null;
  }

  private SelectionConverter() {
    // no instance
  }
}

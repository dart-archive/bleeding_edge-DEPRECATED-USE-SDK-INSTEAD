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

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

public class SelectionConverter {

  public static boolean canOperateOn(DartEditor editor) {
    return false;
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
      return StructuredSelection.EMPTY;
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

  private SelectionConverter() {
    // no instance
  }
}

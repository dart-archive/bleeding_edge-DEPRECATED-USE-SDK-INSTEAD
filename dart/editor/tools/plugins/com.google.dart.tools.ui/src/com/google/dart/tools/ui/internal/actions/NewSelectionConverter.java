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
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.jface.text.ITextSelection;

/**
 * To replace {@link SelectionConverter}.
 */
public class NewSelectionConverter {

  /**
   * Get the element associated with the editor's current selection.
   * 
   * @param editor the editor
   * @return the associated element, or {@code null} if none can be found
   */
  public static Element getElementAtOffset(DartEditor editor) {
    return getElementAtOffset(editor, true);
  }

  /**
   * Get the element associated with the selected portion of the given input element.
   * 
   * @param editor the editor
   * @param caret the caret position in the editor
   * @return the associated element
   */
  public static Element getElementAtOffset(DartEditor editor, int caret) {

    CompilationUnit cu = editor.getInputUnit();

    ASTNode node = new NodeLocator(caret).searchWithin(cu);
    if (node == null) {
      return null;
    }

    return ElementLocator.locate(node);
  }

  /**
   * Get the element associated with the selected portion of the given input element.
   * 
   * @param editor the editor
   * @param offset the beginning of the selection
   * @param length the length of the selection
   * @return the associated element
   */
  public static Element getElementAtOffset(DartEditor editor, int offset, int length) {

    CompilationUnit cu = editor.getInputUnit();

    ASTNode node = new NodeLocator(offset, offset + length).searchWithin(cu);
    if (node == null) {
      return null;
    }

    return ElementLocator.locate(node);
  }

  /**
   * Get the element associated with the selected portion of the given input element.
   * 
   * @param input the input element
   * @param selection the selection
   * @return the associated element
   */
  public static Element getElementAtOffset(Element input, ITextSelection selection) {

    //TODO (pquitslund): parse selection

    return input;

  }

  private static Element getElementAtOffset(DartEditor editor, boolean primaryOnly) {
    return getElementAtOffset(
        getInput(editor, primaryOnly),
        (ITextSelection) editor.getSelectionProvider().getSelection());
  }

  private static Element getInput(DartEditor editor, boolean primaryOnly) {
    if (editor == null) {
      return null;
    }
    return EditorUtility.getEditorInputDartElement2(editor, primaryOnly);
  }

}

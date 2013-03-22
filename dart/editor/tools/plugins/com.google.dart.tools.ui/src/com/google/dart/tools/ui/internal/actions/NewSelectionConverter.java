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
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
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

    CompilationUnit cu = editor.getParsedUnit();
    if (cu == null) {
      return null;
    }

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

  public static Element getElementEnclosingOffset(CompilationUnit unit, final int offset) {
    final Element result[] = new Element[] {null};
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        if (isNodeEnclosingOffset(node)) {
          result[0] = node.getElement();
          return super.visitClassDeclaration(node);
        }
        return null;
      }

      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        if (isNodeEnclosingOffset(node)) {
          result[0] = node.getElement();
          return super.visitConstructorDeclaration(node);
        }
        return null;
      }

      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        if (isNodeEnclosingOffset(node)) {
          NodeList<VariableDeclaration> variables = node.getFields().getVariables();
          if (!variables.isEmpty()) {
            result[0] = variables.get(0).getElement();
          }
          return super.visitFieldDeclaration(node);
        }
        return null;
      }

      @Override
      public Void visitFunctionDeclaration(FunctionDeclaration node) {
        if (isNodeEnclosingOffset(node)) {
          result[0] = node.getElement();
          return super.visitFunctionDeclaration(node);
        }
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        if (isNodeEnclosingOffset(node)) {
          result[0] = node.getElement();
          return super.visitMethodDeclaration(node);
        }
        return null;
      }

      @Override
      public Void visitNode(ASTNode node) {
        if (isNodeEnclosingOffset(node)) {
          super.visitNode(node);
        }
        return null;
      }

      private boolean isNodeEnclosingOffset(ASTNode node) {
        return node.getOffset() <= offset && offset <= node.getEnd();
      }
    });
    return result[0];
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

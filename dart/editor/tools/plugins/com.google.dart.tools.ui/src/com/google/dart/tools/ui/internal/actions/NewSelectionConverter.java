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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.server.NavigationRegion;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

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
    AssistContext context = editor.getAssistContext();
    if (context == null) {
      return null;
    }
    return context.getCoveredElement();
  }

  /**
   * Get the element associated with the selected portion of the given input element.
   * 
   * @param editor the editor
   * @param caret the caret position in the editor
   * @return the associated element
   */
  public static Element getElementAtOffset(DartEditor editor, int caret) {
    AstNode node = getNodeAtOffset(editor, caret);
    if (node == null) {
      return null;
    }

    return ElementLocator.locateWithOffset(node, caret);
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

    AstNode node = new NodeLocator(offset, offset + length).searchWithin(cu);
    if (node == null) {
      return null;
    }

    return ElementLocator.locateWithOffset(node, offset);
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
    unit.accept(new GeneralizingAstVisitor<Void>() {
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
      public Void visitNode(AstNode node) {
        if (isNodeEnclosingOffset(node)) {
          super.visitNode(node);
        }
        return null;
      }

      private boolean isNodeEnclosingOffset(AstNode node) {
        return node.getOffset() <= offset && offset <= node.getEnd();
      }
    });
    return result[0];
  }

  /**
   * Returns navigation targets for the given context, may be empty, but not {@code null}.
   */
  public static com.google.dart.server.generated.types.Element[] getNavigationTargets(String file,
      int offset) {
    NavigationRegion[] regions = DartCore.getAnalysisServerData().getNavigation(file);
    for (NavigationRegion navigationRegion : regions) {
      if (navigationRegion.containsInclusive(offset)) {
        return navigationRegion.getTargets();
      }
    }
    return com.google.dart.server.generated.types.Element.EMPTY_ARRAY;
  }

  /**
   * Get the {@link AstNode} associated with the selected portion of the given editor.
   * 
   * @param editor the editor
   * @param caret the caret position in the editor
   * @return the associated {@link AstNode}
   */
  public static AstNode getNodeAtOffset(DartEditor editor, int caret) {
    CompilationUnit cu = editor.getInputUnit();
    if (cu == null) {
      return null;
    }
    return new NodeLocator(caret).searchWithin(cu);
  }
}

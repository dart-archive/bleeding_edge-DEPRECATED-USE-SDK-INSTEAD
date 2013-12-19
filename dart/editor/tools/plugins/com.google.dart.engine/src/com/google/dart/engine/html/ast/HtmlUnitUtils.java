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

package com.google.dart.engine.html.ast;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;

/**
 * Utilities locating {@link Expression}s and {@link Element}s in {@link HtmlUnit}.
 */
public class HtmlUnitUtils {
  /**
   * Returns the best {@link Element} of the given {@link Expression}.
   */
  public static Element getElement(Expression expression) {
    return ElementLocator.locate(expression);
  }

  /**
   * Returns the {@link Element} of the {@link Expression} in the given {@link HtmlUnit}, enclosing
   * the given offset.
   */
  public static Element getElement(HtmlUnit htmlUnit, int offset) {
    Expression expression = getExpression(htmlUnit, offset);
    return getElement(expression);
  }

  /**
   * Returns the {@link Element} to open when requested at the given {@link Expression}.
   */
  public static Element getElementToOpen(HtmlUnit htmlUnit, Expression expression) {
    Element element = getElement(expression);
    // special cases for Angular
    if (inAngular(htmlUnit)) {
      // replace artificial controller variable Element with controller ClassElement
      if (element instanceof LocalVariableElement) {
        LocalVariableElement variable = (LocalVariableElement) element;
        Element typeElement = variable.getType().getElement();
        if (typeElement instanceof ClassElement) {
          element = typeElement;
        }
      }
    }
    // done
    return element;
  }

  /**
   * Returns the {@link Expression} that is part of the given {@link HtmlUnit} and encloses the
   * given offset.
   */
  public static Expression getExpression(HtmlUnit htmlUnit, final int offset) {
    if (htmlUnit == null) {
      return null;
    }
    class FoundExpressionError extends Error {
    }
    final Expression[] result = {null};
    try {
      htmlUnit.accept(new RecursiveXmlVisitor<Void>() {
        @Override
        public Void visitXmlTagNode(XmlTagNode node) {
          for (EmbeddedExpression embeddedExpression : node.getExpressions()) {
            Expression expression = embeddedExpression.getExpression();
            Expression at = getExpressionAt(expression, offset);
            if (at != null) {
              result[0] = at;
              throw new FoundExpressionError();
            }
          }
          return super.visitXmlTagNode(node);
        }
      });
    } catch (FoundExpressionError e) {
      return result[0];
    }
    return null;
  }

  /**
   * Returns {@code true} if then given {@link HtmlUnit} has Angular annotation.
   */
  public static boolean inAngular(HtmlUnit htmlUnit) {
    // TODO(scheglov) update later
    return true;
  }

  /**
   * Returns the {@link Expression} that is part of the given root {@link ASTNode} and encloses the
   * given offset.
   */
  private static Expression getExpressionAt(ASTNode root, int offset) {
    if (root.getOffset() < offset && offset < root.getEnd()) {
      ASTNode dartNode = new NodeLocator(offset).searchWithin(root);
      if (dartNode instanceof Expression) {
        return (Expression) dartNode;
      }
    }
    return null;
  }
}

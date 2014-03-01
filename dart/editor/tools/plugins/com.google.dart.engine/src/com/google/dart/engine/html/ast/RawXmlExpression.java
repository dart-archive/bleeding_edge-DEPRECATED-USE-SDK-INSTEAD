/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;

/**
 * Implementation of {@link XmlExpression} for an {@link Expression} embedded without any wrapping
 * characters.
 */
public class RawXmlExpression extends XmlExpression {
  private final Expression expression;

  public RawXmlExpression(Expression expression) {
    this.expression = expression;
  }

  @Override
  public int getEnd() {
    return expression.getEnd();
  }

  /**
   * Return the embedded Dart {@link Expression}.
   */
  public Expression getExpression() {
    return expression;
  }

  @Override
  public int getLength() {
    return expression.getLength();
  }

  @Override
  public int getOffset() {
    return expression.getOffset();
  }

  @Override
  public Reference getReference(int offset) {
    AstNode node = new NodeLocator(offset).searchWithin(expression);
    if (node != null) {
      Element element = ElementLocator.locate(node);
      return new Reference(element, node.getOffset(), node.getLength());
    }
    return null;
  }
}

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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.html.ast.XmlExpression;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

/**
 * Abstract Angular specific {@link XmlExpression}.
 */
public abstract class AngularXmlExpression extends XmlExpression {
  /**
   * The expression that is enclosed between the delimiters.
   */
  protected final AngularExpression expression;

  public AngularXmlExpression(AngularExpression expression) {
    this.expression = expression;
  }

  /**
   * Return the embedded {@link AngularExpression}.
   */
  public AngularExpression getExpression() {
    return expression;
  }

  @Override
  public Reference getReference(int offset) {
    // main expression
    Reference reference = getReference(expression.getExpression(), offset);
    if (reference != null) {
      return reference;
    }
    // filters
    for (AngularFilterNode filter : expression.getFilters()) {
      // filter name
      reference = getReference(filter.getName(), offset);
      if (reference != null) {
        return reference;
      }
      // filter arguments
      for (AngularFilterArgument filterArgument : filter.getArguments()) {
        reference = getReference(filterArgument.getExpression(), offset);
        if (reference != null) {
          return reference;
        }
      }
    }
    return null;
  }

  /**
   * If the given {@link ASTNode} has an {@link Element} at the given offset, then returns
   * {@link Reference} with this {@link Element}.
   */
  private Reference getReference(ASTNode root, int offset) {
    ASTNode node = new NodeLocator(offset).searchWithin(root);
    if (node != null) {
      Element element = ElementLocator.locate(node);
      SourceRange range = SourceRangeFactory.rangeNode(node);
      return new Reference(element, range);
    }
    return null;
  }
}

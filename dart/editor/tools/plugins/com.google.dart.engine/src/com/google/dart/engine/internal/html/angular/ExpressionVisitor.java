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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.RawXmlExpression;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlExpression;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;

import java.util.List;

/**
 * Recursively visits {@link HtmlUnit} and every embedded {@link Expression}.
 */
public abstract class ExpressionVisitor extends RecursiveXmlVisitor<Void> {
  /**
   * Visits the given {@link Expression}s embedded into tag or attribute.
   * 
   * @param expression the {@link Expression} to visit, not {@code null}
   */
  public abstract void visitExpression(Expression expression);

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    visitExpressions(node.getExpressions());
    return super.visitXmlAttributeNode(node);
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    visitExpressions(node.getExpressions());
    return super.visitXmlTagNode(node);
  }

  /**
   * Visits {@link Expression}s of the given {@link XmlExpression}s.
   */
  private void visitExpressions(XmlExpression[] expressions) {
    for (XmlExpression xmlExpression : expressions) {
      if (xmlExpression instanceof AngularXmlExpression) {
        AngularXmlExpression angularXmlExpression = (AngularXmlExpression) xmlExpression;
        List<Expression> dartExpressions = angularXmlExpression.getExpression().getExpressions();
        for (Expression dartExpression : dartExpressions) {
          visitExpression(dartExpression);
        }
      }
      if (xmlExpression instanceof RawXmlExpression) {
        RawXmlExpression rawXmlExpression = (RawXmlExpression) xmlExpression;
        visitExpression(rawXmlExpression.getExpression());
      }
    }
  }
}

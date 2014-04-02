/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code CascadeExpression} represent a sequence of cascaded expressions:
 * expressions that share a common target. There are three kinds of expressions that can be used in
 * a cascade expression: {@link IndexExpression}, {@link MethodInvocation} and
 * {@link PropertyAccess}.
 * 
 * <pre>
 * cascadeExpression ::=
 *     {@link Expression conditionalExpression} cascadeSection*
 * 
 * cascadeSection ::=
 *     '..'  (cascadeSelector arguments*) (assignableSelector arguments*)* (assignmentOperator expressionWithoutCascade)?
 * 
 * cascadeSelector ::=
 *     '[ ' expression '] '
 *   | identifier
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class CascadeExpression extends Expression {
  /**
   * The target of the cascade sections.
   */
  private Expression target;

  /**
   * The cascade sections sharing the common target.
   */
  private NodeList<Expression> cascadeSections = new NodeList<Expression>(this);

  /**
   * Initialize a newly created cascade expression.
   * 
   * @param target the target of the cascade sections
   * @param cascadeSections the cascade sections sharing the common target
   */
  public CascadeExpression(Expression target, List<Expression> cascadeSections) {
    this.target = becomeParentOf(target);
    this.cascadeSections.addAll(cascadeSections);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitCascadeExpression(this);
  }

  @Override
  public Token getBeginToken() {
    return target.getBeginToken();
  }

  /**
   * Return the cascade sections sharing the common target.
   * 
   * @return the cascade sections sharing the common target
   */
  public NodeList<Expression> getCascadeSections() {
    return cascadeSections;
  }

  @Override
  public Token getEndToken() {
    return cascadeSections.getEndToken();
  }

  @Override
  public int getPrecedence() {
    return 2;
  }

  /**
   * Return the target of the cascade sections.
   * 
   * @return the target of the cascade sections
   */
  public Expression getTarget() {
    return target;
  }

  /**
   * Set the target of the cascade sections to the given expression.
   * 
   * @param target the target of the cascade sections
   */
  public void setTarget(Expression target) {
    this.target = becomeParentOf(target);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(target, visitor);
    cascadeSections.accept(visitor);
  }
}

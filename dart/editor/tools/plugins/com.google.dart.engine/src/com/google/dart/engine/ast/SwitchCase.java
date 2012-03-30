/*
 * Copyright 2012, the Dart project authors.
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
 * Instances of the class <code>SwitchCase</code> represent the case in a switch statement.
 * 
 * <pre>
 * switchCase ::=
 *     {@link SimpleIdentifier label}? ('case' {@link Expression expression} ':')+ {@link Statement statement}*
 * </pre>
 */
public class SwitchCase extends SwitchMember {
  /**
   * The expression controlling whether the statements will be executed.
   */
  private Expression expression;

  /**
   * Initialize a newly created switch case.
   */
  public SwitchCase() {
  }

  /**
   * Initialize a newly created switch case.
   * 
   * @param label the label associated with the switch member
   * @param keyword the token representing the 'case' or 'default' keyword
   * @param expression the expression controlling whether the statements will be executed
   * @param colon the colon separating the keyword or the expression from the statements
   * @param statements the statements that will be executed if this switch member is selected
   */
  public SwitchCase(Label label, Token keyword, Expression expression, Token colon,
      List<Statement> statements) {
    super(label, keyword, colon, statements);
    this.expression = becomeParentOf(expression);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSwitchCase(this);
  }

  /**
   * Return the expression controlling whether the statements will be executed.
   * 
   * @return the expression controlling whether the statements will be executed
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Set the expression controlling whether the statements will be executed to the given expression.
   * 
   * @param expression the expression controlling whether the statements will be executed
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(getLabel(), visitor);
    safelyVisitChild(expression, visitor);
    getStatements().accept(visitor);
  }
}

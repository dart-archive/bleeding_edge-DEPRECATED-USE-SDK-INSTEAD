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

/**
 * Instances of the class <code>LabeledStatement</code> represent a statement that has a label
 * associated with them.
 * 
 * <pre>
 * labeledStatement ::=
 *    {@link Label label} {@link Statement statement}
 * </pre>
 */
public class LabeledStatement extends Statement {
  /**
   * The label being associated with the statement.
   */
  private Label label;

  /**
   * The statement with which the label is being associated.
   */
  private Statement statement;

  /**
   * Initialize a newly created labeled statement.
   */
  public LabeledStatement() {
  }

  /**
   * Initialize a newly created labeled statement.
   * 
   * @param label the label being associated with the statement
   * @param statement the statement with which the label is being associated
   */
  public LabeledStatement(Label label, Token colon, Statement statement) {
    this.label = becomeParentOf(label);
    this.statement = becomeParentOf(statement);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitLabeledStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return label.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return statement.getEndToken();
  }

  /**
   * Return the label being associated with the statement.
   * 
   * @return the label being associated with the statement
   */
  public Label getLabel() {
    return label;
  }

  /**
   * Return the statement with which the label is being associated.
   * 
   * @return the statement with which the label is being associated
   */
  public Statement getStatement() {
    return statement;
  }

  /**
   * Set the label being associated with the statement to the given label.
   * 
   * @param label the label being associated with the statement
   */
  public void setLabel(Label label) {
    this.label = becomeParentOf(label);
  }

  /**
   * Set the statement with which the label is being associated to the given statement.
   * 
   * @param statement the statement with which the label is being associated
   */
  public void setStatement(Statement statement) {
    this.statement = becomeParentOf(statement);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(label, visitor);
    safelyVisitChild(statement, visitor);
  }
}

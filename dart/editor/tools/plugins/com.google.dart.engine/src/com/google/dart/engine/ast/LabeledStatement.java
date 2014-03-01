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
 * Instances of the class {@code LabeledStatement} represent a statement that has a label associated
 * with them.
 * 
 * <pre>
 * labeledStatement ::=
 *    {@link Label label}+ {@link Statement statement}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class LabeledStatement extends Statement {
  /**
   * The labels being associated with the statement.
   */
  private NodeList<Label> labels = new NodeList<Label>(this);

  /**
   * The statement with which the labels are being associated.
   */
  private Statement statement;

  /**
   * Initialize a newly created labeled statement.
   * 
   * @param labels the labels being associated with the statement
   * @param statement the statement with which the labels are being associated
   */
  public LabeledStatement(List<Label> labels, Statement statement) {
    this.labels.addAll(labels);
    this.statement = becomeParentOf(statement);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitLabeledStatement(this);
  }

  @Override
  public Token getBeginToken() {
    if (!labels.isEmpty()) {
      return labels.getBeginToken();
    }
    return statement.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return statement.getEndToken();
  }

  /**
   * Return the labels being associated with the statement.
   * 
   * @return the labels being associated with the statement
   */
  public NodeList<Label> getLabels() {
    return labels;
  }

  /**
   * Return the statement with which the labels are being associated.
   * 
   * @return the statement with which the labels are being associated
   */
  public Statement getStatement() {
    return statement;
  }

  /**
   * Set the statement with which the labels are being associated to the given statement.
   * 
   * @param statement the statement with which the labels are being associated
   */
  public void setStatement(Statement statement) {
    this.statement = becomeParentOf(statement);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    labels.accept(visitor);
    safelyVisitChild(statement, visitor);
  }
}

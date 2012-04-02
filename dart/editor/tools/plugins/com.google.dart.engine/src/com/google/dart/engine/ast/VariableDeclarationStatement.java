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
 * Instances of the class <code>VariableDeclarationStatement</code> represent a list of variables
 * that are being declared in a context where a statement is required.
 * 
 * <pre>
 * variableDeclarationStatement ::=
 *     {@link VariableDeclarationList variableList} ';'
 * </pre>
 */
public class VariableDeclarationStatement extends Statement {
  /**
   * The variables being declared.
   */
  private VariableDeclarationList variableList;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created variable declaration statement.
   */
  public VariableDeclarationStatement() {
  }

  /**
   * Initialize a newly created variable declaration statement.
   * 
   * @param variableList the fields being declared
   * @param semicolon the semicolon terminating the statement
   */
  public VariableDeclarationStatement(VariableDeclarationList variableList, Token semicolon) {
    this.variableList = becomeParentOf(variableList);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitVariableDeclarationStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return variableList.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the semicolon terminating the statement.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Return the variables being declared.
   * 
   * @return the variables being declared
   */
  public VariableDeclarationList getVariables() {
    return variableList;
  }

  /**
   * Set the semicolon terminating the statement to the given token.
   * 
   * @param semicolon the semicolon terminating the statement
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  /**
   * Set the variables being declared to the given list of variables.
   * 
   * @param variableList the variables being declared
   */
  public void setVariables(VariableDeclarationList variableList) {
    this.variableList = becomeParentOf(variableList);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(variableList, visitor);
  }
}

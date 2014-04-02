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
 * The abstract class {@code SwitchMember} defines the behavior common to objects representing
 * elements within a switch statement.
 * 
 * <pre>
 * switchMember ::=
 *     switchCase
 *   | switchDefault
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class SwitchMember extends AstNode {
  /**
   * The labels associated with the switch member.
   */
  private NodeList<Label> labels = new NodeList<Label>(this);

  /**
   * The token representing the 'case' or 'default' keyword.
   */
  private Token keyword;

  /**
   * The colon separating the keyword or the expression from the statements.
   */
  private Token colon;

  /**
   * The statements that will be executed if this switch member is selected.
   */
  private NodeList<Statement> statements = new NodeList<Statement>(this);

  /**
   * Initialize a newly created switch member.
   * 
   * @param labels the labels associated with the switch member
   * @param keyword the token representing the 'case' or 'default' keyword
   * @param colon the colon separating the keyword or the expression from the statements
   * @param statements the statements that will be executed if this switch member is selected
   */
  public SwitchMember(List<Label> labels, Token keyword, Token colon, List<Statement> statements) {
    this.labels.addAll(labels);
    this.keyword = keyword;
    this.colon = colon;
    this.statements.addAll(statements);
  }

  @Override
  public Token getBeginToken() {
    if (!labels.isEmpty()) {
      return labels.getBeginToken();
    }
    return keyword;
  }

  /**
   * Return the colon separating the keyword or the expression from the statements.
   * 
   * @return the colon separating the keyword or the expression from the statements
   */
  public Token getColon() {
    return colon;
  }

  @Override
  public Token getEndToken() {
    if (!statements.isEmpty()) {
      return statements.getEndToken();
    }
    return colon;
  }

  /**
   * Return the token representing the 'case' or 'default' keyword.
   * 
   * @return the token representing the 'case' or 'default' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the labels associated with the switch member.
   * 
   * @return the labels associated with the switch member
   */
  public NodeList<Label> getLabels() {
    return labels;
  }

  /**
   * Return the statements that will be executed if this switch member is selected.
   * 
   * @return the statements that will be executed if this switch member is selected
   */
  public NodeList<Statement> getStatements() {
    return statements;
  }

  /**
   * Set the colon separating the keyword or the expression from the statements to the given token.
   * 
   * @param colon the colon separating the keyword or the expression from the statements
   */
  public void setColon(Token colon) {
    this.colon = colon;
  }

  /**
   * Set the token representing the 'case' or 'default' keyword to the given token.
   * 
   * @param keyword the token representing the 'case' or 'default' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }
}

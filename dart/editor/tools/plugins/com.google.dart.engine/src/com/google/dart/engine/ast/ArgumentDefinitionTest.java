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
 * Instances of the class {@code ArgumentDefinitionTest} represent an argument definition test.
 * 
 * <pre>
 * argumentDefinitionTest ::=
 *     '?' {@link SimpleIdentifier identifier}
 * </pre>
 */
public class ArgumentDefinitionTest extends Expression {
  /**
   * The token representing the question mark.
   */
  private Token question;

  /**
   * The identifier representing the argument being tested.
   */
  private SimpleIdentifier identifier;

  /**
   * Initialize a newly created argument definition test.
   * 
   * @param question the token representing the question mark
   * @param identifier the identifier representing the argument being tested
   */
  public ArgumentDefinitionTest(Token question, SimpleIdentifier identifier) {
    this.question = question;
    this.identifier = becomeParentOf(identifier);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitArgumentDefinitionTest(this);
  }

  @Override
  public Token getBeginToken() {
    return question;
  }

  @Override
  public Token getEndToken() {
    return identifier.getEndToken();
  }

  /**
   * Return the identifier representing the argument being tested.
   * 
   * @return the identifier representing the argument being tested
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * Return the token representing the question mark.
   * 
   * @return the token representing the question mark
   */
  public Token getQuestion() {
    return question;
  }

  /**
   * Set the identifier representing the argument being tested to the given identifier.
   * 
   * @param identifier the identifier representing the argument being tested
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  /**
   * Set the token representing the question mark to the given token.
   * 
   * @param question the token representing the question mark
   */
  public void setQuestion(Token question) {
    this.question = question;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(identifier, visitor);
  }
}

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
import com.google.dart.engine.scanner.TokenType;

/**
 * Instances of the class {@code SimpleIdentifier} represent a simple identifier.
 * 
 * <pre>
 * simpleIdentifier ::=
 *     initialCharacter internalCharacter*
 *
 * initialCharacter ::= '_' | '$' | letter
 *
 * internalCharacter ::= '_' | '$' | letter | digit
 * </pre>
 */
public class SimpleIdentifier extends Identifier {
  /**
   * The token representing the identifier.
   */
  private Token token;

  /**
   * Initialize a newly created identifier.
   * 
   * @param token the token representing the identifier
   */
  public SimpleIdentifier(Token token) {
    this.token = token;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSimpleIdentifier(this);
  }

  @Override
  public Token getBeginToken() {
    return token;
  }

  @Override
  public Token getEndToken() {
    return token;
  }

  @Override
  public String getName() {
    return token.getLexeme();
  }

  /**
   * Return the token representing the identifier.
   * 
   * @return the token representing the identifier
   */
  public Token getToken() {
    return token;
  }

  /**
   * Looks to see if this identifier is used to read value.
   */
  public boolean inGetterContext() {
    ASTNode parent = getParent();
    ASTNode target = this;
    // skip prefix
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getIdentifier() == this) {
        parent = prefixed.getParent();
        target = prefixed;
      }
    }
    // analyze usage
    if (parent instanceof AssignmentExpression) {
      AssignmentExpression expr = (AssignmentExpression) parent;
      if (expr.getLeftHandSide() == target && expr.getOperator().getType() == TokenType.EQ) {
        return false;
      }
    }
    return true;
  }

  /**
   * Looks to see if this identifier is used to write value.
   */
  public boolean inSetterContext() {
    ASTNode parent = getParent();
    ASTNode target = this;
    // skip prefix
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getIdentifier() == this) {
        parent = prefixed.getParent();
        target = prefixed;
      }
    }
    // analyze usage
    if (parent instanceof PrefixExpression) {
      PrefixExpression expr = (PrefixExpression) parent;
      return expr.getOperand() == target && expr.getOperator().getType().isIncrementOperator();
    }
    if (parent instanceof PostfixExpression) {
      return true;
    }
    if (parent instanceof AssignmentExpression) {
      AssignmentExpression expr = (AssignmentExpression) parent;
      return expr.getLeftHandSide() == target;
    }
    return false;
  }

  @Override
  public boolean isSynthetic() {
    return token.isSynthetic();
  }

  /**
   * Set the token representing the identifier to the given token.
   * 
   * @param token the token representing the literal
   */
  public void setToken(Token token) {
    this.token = token;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    // There are no children to visit.
  }
}

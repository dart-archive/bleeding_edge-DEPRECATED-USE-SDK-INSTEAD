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

import com.google.dart.engine.element.Element;
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
 * 
 * @coverage dart.engine.ast
 */
public class SimpleIdentifier extends Identifier {
  /**
   * The token representing the identifier.
   */
  private Token token;

  /**
   * The element associated with this identifier, or {@code null} if the AST structure has not been
   * resolved or if this identifier could not be resolved.
   */
  private Element element;

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
  public Element getElement() {
    return element;
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
   * Return {@code true} if this identifier is the name being declared in a declaration.
   * 
   * @return {@code true} if this identifier is the name being declared in a declaration
   */
  public boolean inDeclarationContext() {
    ASTNode parent = getParent();
    if (parent instanceof CatchClause) {
      CatchClause clause = (CatchClause) parent;
      return this == clause.getExceptionParameter() || this == clause.getStackTraceParameter();
    } else if (parent instanceof ClassDeclaration) {
      return this == ((ClassDeclaration) parent).getName();
    } else if (parent instanceof ClassTypeAlias) {
      return this == ((ClassTypeAlias) parent).getName();
    } else if (parent instanceof ConstructorDeclaration) {
      return this == ((ConstructorDeclaration) parent).getName();
    } else if (parent instanceof FunctionDeclaration) {
      return this == ((FunctionDeclaration) parent).getName();
    } else if (parent instanceof FunctionTypeAlias) {
      return this == ((FunctionTypeAlias) parent).getName();
    } else if (parent instanceof Label) {
      return this == ((Label) parent).getLabel()
          && (parent.getParent() instanceof LabeledStatement);
    } else if (parent instanceof MethodDeclaration) {
      return this == ((MethodDeclaration) parent).getName();
    } else if (parent instanceof FunctionTypedFormalParameter
        || parent instanceof SimpleFormalParameter) {
      return this == ((NormalFormalParameter) parent).getIdentifier();
    } else if (parent instanceof TypeParameter) {
      return this == ((TypeParameter) parent).getName();
    } else if (parent instanceof VariableDeclaration) {
      return this == ((VariableDeclaration) parent).getName();
    }
    return false;
  }

  /**
   * Return {@code true} if this expression is computing a right-hand value.
   * <p>
   * Note that {@link #inGetterContext()} and {@link #inSetterContext()} are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return {@code true}
   * when invoked on the same node.
   * 
   * @return {@code true} if this expression is in a context where a getter will be invoked
   */
  public boolean inGetterContext() {
    ASTNode parent = getParent();
    ASTNode target = this;
    // skip prefix
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getPrefix() == this) {
        return true;
      }
      parent = prefixed.getParent();
      target = prefixed;
    } else if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      if (access.getTarget() == this) {
        return true;
      }
      parent = access.getParent();
      target = access;
    }
    // skip label
    if (parent instanceof Label) {
      return false;
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
   * Return {@code true} if this expression is computing a left-hand value.
   * <p>
   * Note that {@link #inGetterContext()} and {@link #inSetterContext()} are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return {@code true}
   * when invoked on the same node.
   * 
   * @return {@code true} if this expression is in a context where a setter will be invoked
   */
  public boolean inSetterContext() {
    ASTNode parent = getParent();
    ASTNode target = this;
    // skip prefix
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      // if this is the prefix, then return false
      if (prefixed.getPrefix() == this) {
        return false;
      }
      parent = prefixed.getParent();
      target = prefixed;
    } else if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      if (access.getTarget() == this) {
        return false;
      }
      parent = access.getParent();
      target = access;
    }
    // analyze usage
    if (parent instanceof PrefixExpression) {
      return ((PrefixExpression) parent).getOperator().getType().isIncrementOperator();
    } else if (parent instanceof PostfixExpression) {
      return true;
    } else if (parent instanceof AssignmentExpression) {
      return ((AssignmentExpression) parent).getLeftHandSide() == target;
    }
    return false;
  }

  @Override
  public boolean isSynthetic() {
    return token.isSynthetic();
  }

  /**
   * Set the element associated with this identifier to the given element.
   * 
   * @param element the element associated with this identifier
   */
  public void setElement(Element element) {
    this.element = element;
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

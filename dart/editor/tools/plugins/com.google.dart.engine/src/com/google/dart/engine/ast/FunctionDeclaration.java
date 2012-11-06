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
 * Instances of the class {@code FunctionDeclaration} wrap a {@link FunctionExpression function
 * expression} as a top-level declaration.
 * 
 * <pre>
 * functionDeclaration ::=
 *     functionSignature {@link FunctionBody functionBody}
 *
 * functionSignature ::=
 *     {@link Type returnType}? {@link SimpleIdentifier functionName} {@link FormalParameterList formalParameterList}
 * </pre>
 */
public class FunctionDeclaration extends CompilationUnitMember {
  /**
   * The token representing the 'external' keyword, or {@code null} if this is not an external
   * function.
   */
  private Token externalKeyword;

  /**
   * The token representing the 'get' or 'set' keyword, or {@code null} if this is a function
   * declaration rather than a property declaration.
   */
  private Token propertyKeyword;

  /**
   * The function expression being wrapped.
   */
  private FunctionExpression functionExpression;

  /**
   * Initialize a newly created function declaration.
   */
  public FunctionDeclaration() {
  }

  /**
   * Initialize a newly created function declaration.
   * 
   * @param comment the documentation comment associated with this function
   * @param metadata the annotations associated with this function
   * @param externalKeyword the token representing the 'external' keyword
   * @param propertyKeyword the token representing the 'get' or 'set' keyword
   * @param functionExpression the function expression being wrapped
   */
  public FunctionDeclaration(Comment comment, List<Annotation> metadata, Token externalKeyword,
      Token propertyKeyword, FunctionExpression functionExpression) {
    super(comment, metadata);
    this.externalKeyword = externalKeyword;
    this.propertyKeyword = propertyKeyword;
    this.functionExpression = becomeParentOf(functionExpression);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFunctionDeclaration(this);
  }

  @Override
  public Token getEndToken() {
    return functionExpression.getEndToken();
  }

  /**
   * Return the token representing the 'external' keyword, or {@code null} if this is not an
   * external function.
   * 
   * @return the token representing the 'external' keyword
   */
  public Token getExternalKeyword() {
    return externalKeyword;
  }

  /**
   * Return the function expression being wrapped.
   * 
   * @return the function expression being wrapped
   */
  public FunctionExpression getFunctionExpression() {
    return functionExpression;
  }

  /**
   * Return the token representing the 'get' or 'set' keyword, or {@code null} if this is a function
   * declaration rather than a property declaration.
   * 
   * @return the token representing the 'get' or 'set' keyword
   */
  public Token getPropertyKeyword() {
    return propertyKeyword;
  }

  /**
   * Set the token representing the 'external' keyword to the given token.
   * 
   * @param externalKeyword the token representing the 'external' keyword
   */
  public void setExternalKeyword(Token externalKeyword) {
    this.externalKeyword = externalKeyword;
  }

  /**
   * Set the function expression being wrapped to the given function expression.
   * 
   * @param functionExpression the function expression being wrapped
   */
  public void setFunctionExpression(FunctionExpression functionExpression) {
    functionExpression = becomeParentOf(functionExpression);
  }

  /**
   * Set the token representing the 'get' or 'set' keyword to the given token.
   * 
   * @param propertyKeyword the token representing the 'get' or 'set' keyword
   */
  public void setPropertyKeyword(Token propertyKeyword) {
    this.propertyKeyword = propertyKeyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(functionExpression, visitor);
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    if (externalKeyword != null) {
      return externalKeyword;
    }
    TypeName returnType = functionExpression.getReturnType();
    if (returnType != null) {
      return returnType.getBeginToken();
    } else if (propertyKeyword != null) {
      return propertyKeyword;
    }
    return functionExpression.getBeginToken();
  }
}

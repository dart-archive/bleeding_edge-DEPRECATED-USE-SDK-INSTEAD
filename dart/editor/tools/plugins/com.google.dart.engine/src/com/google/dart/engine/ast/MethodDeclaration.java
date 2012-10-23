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

import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code MethodDeclaration} represent a method declaration.
 * 
 * <pre>
 * methodDeclaration ::=
 *     methodSignature {@link FunctionBody body}
 *
 * methodSignature ::=
 *     'external'? ('abstract' | 'static')? {@link Type returnType}? ('get' | 'set')? methodName
 *     {@link FormalParameterList formalParameterList}
 *
 * methodName ::=
 *     {@link SimpleIdentifier name} ('.' {@link SimpleIdentifier name})?
 *   | 'operator' {@link SimpleIdentifier operator}
 * </pre>
 */
public class MethodDeclaration extends ClassMember {
  /**
   * The token for the 'external' keyword, or {@code null} if the constructor is not external.
   */
  private Token externalKeyword;

  /**
   * The token representing the 'abstract' or 'static' keyword, or {@code null} if neither modifier
   * was specified.
   */
  private Token modifierKeyword;

  /**
   * The return type of the method, or {@code null} if no return type was declared.
   */
  private TypeName returnType;

  /**
   * The token representing the 'get' or 'set' keyword, or {@code null} if this is a method
   * declaration rather than a property declaration.
   */
  private Token propertyKeyword;

  /**
   * The token representing the 'operator' keyword, or {@code null} if this method does not declare
   * an operator.
   */
  private Token operatorKeyword;

  /**
   * The name of the method.
   */
  private Identifier name;

  /**
   * The parameters associated with the method, or {@code null} if this method declares a getter.
   */
  private FormalParameterList parameters;

  /**
   * The body of the method.
   */
  private FunctionBody body;

  /**
   * Initialize a newly created method declaration.
   */
  public MethodDeclaration() {
  }

  /**
   * Initialize a newly created method declaration.
   * 
   * @param externalKeyword the token for the 'external' keyword
   * @param comment the documentation comment associated with this method
   * @param metadata the annotations associated with this method
   * @param modifierKeyword the token representing the 'abstract' or 'static' keyword
   * @param returnType the return type of the method
   * @param propertyKeyword the token representing the 'get' or 'set' keyword
   * @param operatorKeyword the token representing the 'operator' keyword
   * @param name the name of the method
   * @param parameters the parameters associated with the method, or {@code null} if this method
   *          declares a getter
   * @param body the body of the method
   */
  public MethodDeclaration(Comment comment, List<Annotation> metadata, Token externalKeyword,
      Token modifierKeyword, TypeName returnType, Token propertyKeyword, Token operatorKeyword,
      Identifier name, FormalParameterList parameters, FunctionBody body) {
    super(comment, metadata);
    this.externalKeyword = externalKeyword;
    this.modifierKeyword = modifierKeyword;
    this.returnType = becomeParentOf(returnType);
    this.propertyKeyword = propertyKeyword;
    this.operatorKeyword = operatorKeyword;
    this.name = becomeParentOf(name);
    this.parameters = becomeParentOf(parameters);
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitMethodDeclaration(this);
  }

  @Override
  public Token getBeginToken() {
    Comment comment = getDocumentationComment();
    if (comment != null) {
      return comment.getBeginToken();
    } else if (modifierKeyword != null) {
      return modifierKeyword;
    } else if (returnType != null) {
      return returnType.getBeginToken();
    } else if (propertyKeyword != null) {
      return propertyKeyword;
    } else if (operatorKeyword != null) {
      return operatorKeyword;
    }
    return name.getBeginToken();
  }

  /**
   * Return the body of the method.
   * 
   * @return the body of the method
   */
  public FunctionBody getBody() {
    return body;
  }

  @Override
  public Token getEndToken() {
    return body.getEndToken();
  }

  /**
   * Return the token for the 'external' keyword, or {@code null} if the constructor is not
   * external.
   * 
   * @return the token for the 'external' keyword
   */
  public Token getExternalKeyword() {
    return externalKeyword;
  }

  /**
   * Return the token representing the 'abstract' or 'static' keyword, or {@code null} if neither
   * modifier was specified.
   * 
   * @return the token representing the 'abstract' or 'static' keyword
   */
  public Token getModifierKeyword() {
    return modifierKeyword;
  }

  /**
   * Return the name of the method.
   * 
   * @return the name of the method
   */
  public Identifier getName() {
    return name;
  }

  /**
   * Return the token representing the 'operator' keyword, or {@code null} if this method does not
   * declare an operator.
   * 
   * @return the token representing the 'operator' keyword
   */
  public Token getOperatorKeyword() {
    return operatorKeyword;
  }

  /**
   * Return the parameters associated with the method, or {@code null} if this method declares a
   * getter.
   * 
   * @return the parameters associated with the method
   */
  public FormalParameterList getParameters() {
    return parameters;
  }

  /**
   * Return the token representing the 'get' or 'set' keyword, or {@code null} if this is a method
   * declaration rather than a property declaration.
   * 
   * @return the token representing the 'get' or 'set' keyword
   */
  public Token getPropertyKeyword() {
    return propertyKeyword;
  }

  /**
   * Return the return type of the method, or {@code null} if no return type was declared.
   * 
   * @return the return type of the method
   */
  public TypeName getReturnType() {
    return returnType;
  }

  /**
   * Return {@code true} if this method declares a getter.
   * 
   * @return {@code true} if this method declares a getter
   */
  public boolean isGetter() {
    return propertyKeyword != null && ((KeywordToken) propertyKeyword).getKeyword() == Keyword.GET;
  }

  /**
   * Return {@code true} if this method declares an operator.
   * 
   * @return {@code true} if this method declares an operator
   */
  public boolean isOperator() {
    return operatorKeyword != null;
  }

  /**
   * Return {@code true} if this method declares a setter.
   * 
   * @return {@code true} if this method declares a setter
   */
  public boolean isSetter() {
    return propertyKeyword != null && ((KeywordToken) propertyKeyword).getKeyword() == Keyword.SET;
  }

  /**
   * Set the body of the method to the given function body.
   * 
   * @param functionBody the body of the method
   */
  public void setBody(FunctionBody functionBody) {
    body = becomeParentOf(functionBody);
  }

  /**
   * Set the token for the 'external' keyword to the given token.
   * 
   * @param externalKeyword the token for the 'external' keyword
   */
  public void setExternalKeyword(Token externalKeyword) {
    this.externalKeyword = externalKeyword;
  }

  /**
   * Set the token representing the 'abstract' or 'static' keyword to the given token.
   * 
   * @param modifierKeyword the token representing the 'abstract' or 'static' keyword
   */
  public void setModifierKeyword(Token modifierKeyword) {
    this.modifierKeyword = modifierKeyword;
  }

  /**
   * Set the name of the method to the given identifier.
   * 
   * @param identifier the name of the method
   */
  public void setName(Identifier identifier) {
    name = becomeParentOf(identifier);
  }

  /**
   * Set the token representing the 'operator' keyword to the given token.
   * 
   * @param operatorKeyword the token representing the 'operator' keyword
   */
  public void setOperatorKeyword(Token operatorKeyword) {
    this.operatorKeyword = operatorKeyword;
  }

  /**
   * Set the parameters associated with the method to the given list of parameters.
   * 
   * @param parameters the parameters associated with the method
   */
  public void setParameters(FormalParameterList parameters) {
    this.parameters = becomeParentOf(parameters);
  }

  /**
   * Set the token representing the 'get' or 'set' keyword to the given token.
   * 
   * @param propertyKeyword the token representing the 'get' or 'set' keyword
   */
  public void setPropertyKeyword(Token propertyKeyword) {
    this.propertyKeyword = propertyKeyword;
  }

  /**
   * Set the return type of the method to the given type name.
   * 
   * @param typeName the return type of the method
   */
  public void setReturnType(TypeName typeName) {
    returnType = becomeParentOf(typeName);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(parameters, visitor);
    safelyVisitChild(body, visitor);
  }
}

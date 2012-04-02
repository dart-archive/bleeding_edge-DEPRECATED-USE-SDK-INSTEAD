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
 * Instances of the class <code>MethodDeclaration</code> represent a method declaration.
 * 
 * <pre>
 * methodDeclaration ::=
 *     methodSignature {@link FunctionBody body}
 *
 * methodSignature ::=
 *     ('abstract' | 'static')? {@link Type returnType}? ('get' | 'set')? methodName formalParameterList
 *
 * methodName ::=
 *     {@link SimpleIdentifier name} ('.' {@link SimpleIdentifier name})?
 *   | 'operator' {@link SimpleIdentifier operator}
 *
 * formalParameterList ::=
 *    '(' ')'
 *  | '(' normalFormalParameters (',' namedFormalParameters)? ')'
 *  | '(' namedFormalParameters ')'
 *
 * normalFormalParameters ::=
 *     {@link NormalFormalParameter normalFormalParameter} (',' {@link NormalFormalParameter normalFormalParameter})*
 *
 * namedFormalParameters ::=
 *     '[' {@link NamedFormalParameter namedFormalParameter} (',' {@link NamedFormalParameter namedFormalParameter})* ']'
 * </pre>
 */
public class MethodDeclaration extends TypeMember {
  /**
   * The token representing the 'abstract' or 'static' keyword, or <code>null</code> if neither
   * modifier was specified.
   */
  private Token modifierKeyword;

  /**
   * The return type of the method, or <code>null</code> if no return type was declared.
   */
  private TypeName returnType;

  /**
   * The token representing the 'get' or 'set' keyword, or <code>null</code> if this is a method
   * declaration rather than a property declaration.
   */
  private Token propertyKeyword;

  /**
   * The token representing the 'operator' keyword, or <code>null</code> if this method does not
   * declare an operator.
   */
  private Token operatorKeyword;

  /**
   * The name of the method.
   */
  private Identifier name;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The parameters associated with the method.
   */
  private NodeList<FormalParameter> parameters = new NodeList<FormalParameter>(this);

  /**
   * The left square bracket.
   */
  private Token leftBracket;

  /**
   * The right square bracket.
   */
  private Token rightBracket;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The body of the method.
   */
  private FunctionBody body;

  /**
   * Initialize a newly created method declaration.
   */
  private MethodDeclaration() {
  }

  /**
   * Initialize a newly created method declaration.
   * 
   * @param comment the documentation comment associated with this method
   * @param modifierKeyword the token representing the 'abstract' or 'static' keyword
   * @param returnType the return type of the method
   * @param propertyKeyword the token representing the 'get' or 'set' keyword
   * @param operatorKeyword the token representing the 'operator' keyword
   * @param name the name of the method
   * @param leftParenthesis the left parenthesis
   * @param parameters the parameters associated with the method
   * @param leftBracket the left square bracket
   * @param rightBracket the right square bracket
   * @param rightParenthesis the right parenthesis
   * @param body the body of the method
   */
  private MethodDeclaration(Comment comment, Token modifierKeyword, TypeName returnType,
      Token propertyKeyword, Token operatorKeyword, Identifier name, Token leftParenthesis,
      List<FormalParameter> parameters, Token leftBracket, Token rightBracket,
      Token rightParenthesis, FunctionBody body) {
    super(comment);
    this.modifierKeyword = modifierKeyword;
    this.returnType = becomeParentOf(returnType);
    this.propertyKeyword = propertyKeyword;
    this.operatorKeyword = operatorKeyword;
    this.name = becomeParentOf(name);
    this.leftParenthesis = leftParenthesis;
    this.parameters.addAll(parameters);
    this.leftBracket = leftBracket;
    this.rightBracket = rightBracket;
    this.rightParenthesis = rightParenthesis;
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
   * Return the left square bracket.
   * 
   * @return the left square bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the left parenthesis.
   * 
   * @return the left parenthesis
   */
  public Token getLeftParenthesis() {
    return leftParenthesis;
  }

  /**
   * Return the token representing the 'abstract' or 'static' keyword, or <code>null</code> if
   * neither modifier was specified.
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
   * Return the token representing the 'operator' keyword, or <code>null</code> if this method does
   * not declare an operator.
   * 
   * @return the token representing the 'operator' keyword
   */
  public Token getOperatorKeyword() {
    return operatorKeyword;
  }

  /**
   * Return the parameters associated with the method.
   * 
   * @return the parameters associated with the method
   */
  public NodeList<FormalParameter> getParameters() {
    return parameters;
  }

  /**
   * Return the token representing the 'get' or 'set' keyword, or <code>null</code> if this is a
   * method declaration rather than a property declaration.
   * 
   * @return the token representing the 'get' or 'set' keyword
   */
  public Token getPropertyKeyword() {
    return propertyKeyword;
  }

  /**
   * Return the return type of the method, or <code>null</code> if no return type was declared.
   * 
   * @return the return type of the method
   */
  public TypeName getReturnType() {
    return returnType;
  }

  /**
   * Return the right square bracket.
   * 
   * @return the right square bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return the right parenthesis.
   * 
   * @return the right parenthesis
   */
  public Token getRightParenthesis() {
    return rightParenthesis;
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
   * Set the left square bracket to the given token.
   * 
   * @param bracket the left square bracket
   */
  public void setLeftBracket(Token bracket) {
    leftBracket = bracket;
  }

  /**
   * Set the left parenthesis to the given token.
   * 
   * @param parenthesis the left parenthesis
   */
  public void setLeftParenthesis(Token parenthesis) {
    leftParenthesis = parenthesis;
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

  /**
   * Set the right square bracket to the given token.
   * 
   * @param bracket the right square bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  /**
   * Set the right parenthesis to the given token.
   * 
   * @param parenthesis the right parenthesis
   */
  public void setRightParenthesis(Token parenthesis) {
    rightParenthesis = parenthesis;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(name, visitor);
    parameters.accept(visitor);
    safelyVisitChild(body, visitor);
  }
}

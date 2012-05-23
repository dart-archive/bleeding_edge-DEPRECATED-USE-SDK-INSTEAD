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
 * Instances of the class <code>ConstructorDeclaration</code> represent a constructor declaration.
 * 
 * <pre>
 * constructorDeclaration ::=
 *     constructorSignature {@link FunctionBody body}?
 *   | constructorName formalParameterList ':' 'this' ('.' {@link SimpleIdentifier name})? arguments
 *
 * constructorSignature ::=
 *     constructorName formalParameterList initializerList?
 *   | 'factory' factoryName formalParameterList initializerList?
 *   | 'const'  constructorName formalParameterList initializerList?
 *
 * constructorName ::=
 *     {@link SimpleIdentifier returnType} ('.' {@link SimpleIdentifier name})?
 *
 * factoryName ::=
 *     {@link Identifier returnType} ('.' {@link SimpleIdentifier name})?
 * 
 * initializerList ::=
 *     ':' {@link ConstructorInitializer initializer} (',' {@link ConstructorInitializer initializer})*
 * </pre>
 */
public class ConstructorDeclaration extends TypeMember {
  /**
   * The token for the 'factory' or 'const' keyword.
   */
  private Token keyword;

  /**
   * The type of object being created. This can be different than the type in which the constructor
   * is being declared if the constructor is the implementation of a factory constructor.
   */
  private Identifier returnType;

  /**
   * The token for the period before the constructor name, or <code>null</code> if the constructor
   * being declared is unnamed.
   */
  private Token period;

  /**
   * The name of the constructor, or <code>null</code> if the constructor being declared is unnamed.
   */
  private SimpleIdentifier name;

  /**
   * The parameters associated with the constructor.
   */
  private FormalParameterList parameters;

  /**
   * The token for the colon before the initializers, or <code>null</code> if there are no
   * initializers.
   */
  private Token colon;

  /**
   * The initializers associated with the constructor.
   */
  private NodeList<ConstructorInitializer> initializers = new NodeList<ConstructorInitializer>(this);

  /**
   * The body of the constructor, or <code>null</code> if the constructor does not have a body.
   */
  private FunctionBody body;

  /**
   * Initialize a newly created constructor declaration.
   */
  public ConstructorDeclaration() {
  }

  /**
   * Initialize a newly created constructor declaration.
   * 
   * @param comment the documentation comment associated with this constructor
   * @param keyword the token for the 'factory' or 'const' keyword
   * @param returnType the return type of the constructor
   * @param period the token for the period before the constructor name
   * @param name the name of the constructor
   * @param parameters the parameters associated with the constructor
   * @param colon the token for the colon before the initializers
   * @param initializers the initializers associated with the constructor
   * @param body the body of the constructor
   */
  public ConstructorDeclaration(Comment comment, Token keyword, Identifier returnType,
      Token period, SimpleIdentifier name, FormalParameterList parameters, Token colon,
      List<ConstructorInitializer> initializers, FunctionBody body) {
    super(comment);
    this.keyword = keyword;
    this.returnType = becomeParentOf(returnType);
    this.period = period;
    this.name = becomeParentOf(name);
    this.parameters = becomeParentOf(parameters);
    this.colon = colon;
    this.initializers.addAll(initializers);
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitConstructorDeclaration(this);
  }

  @Override
  public Token getBeginToken() {
    if (keyword != null) {
      return keyword;
    }
    return returnType.getBeginToken();
  }

  /**
   * Return the body of the constructor, or <code>null</code> if the constructor does not have a
   * body.
   * 
   * @return the body of the constructor
   */
  public FunctionBody getBody() {
    return body;
  }

  /**
   * Return the token for the colon before the initializers, or <code>null</code> if there are no
   * initializers.
   * 
   * @return the token for the colon before the initializers
   */
  public Token getColon() {
    return colon;
  }

  @Override
  public Token getEndToken() {
    if (body != null) {
      return body.getEndToken();
    } else if (!initializers.isEmpty()) {
      return initializers.getEndToken();
    }
    return parameters.getEndToken();
  }

  /**
   * Return the initializers associated with the constructor.
   * 
   * @return the initializers associated with the constructor
   */
  public NodeList<ConstructorInitializer> getInitializers() {
    return initializers;
  }

  /**
   * Return the token for the 'factory' or 'const' keyword.
   * 
   * @return the token for the 'factory' or 'const' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the name of the constructor, or <code>null</code> if the constructor being declared is
   * unnamed.
   * 
   * @return the name of the constructor
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the parameters associated with the constructor.
   * 
   * @return the parameters associated with the constructor
   */
  public FormalParameterList getParameters() {
    return parameters;
  }

  /**
   * Return the token for the period before the constructor name, or <code>null</code> if the
   * constructor being declared is unnamed.
   * 
   * @return the token for the period before the constructor name
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Return the type of object being created. This can be different than the type in which the
   * constructor is being declared if the constructor is the implementation of a factory
   * constructor.
   * 
   * @return the type of object being created
   */
  public Identifier getReturnType() {
    return returnType;
  }

  /**
   * Set the body of the constructor to the given function body.
   * 
   * @param functionBody the body of the constructor
   */
  public void setBody(FunctionBody functionBody) {
    body = becomeParentOf(functionBody);
  }

  /**
   * Set the token for the colon before the initializers to the given token.
   * 
   * @param colon the token for the colon before the initializers
   */
  public void setColon(Token colon) {
    this.colon = colon;
  }

  /**
   * Set the token for the 'factory' or 'const' keyword to the given token.
   * 
   * @param keyword the token for the 'factory' or 'const' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the name of the constructor to the given identifier.
   * 
   * @param identifier the name of the constructor
   */
  public void setName(SimpleIdentifier identifier) {
    name = becomeParentOf(identifier);
  }

  /**
   * Set the parameters associated with the constructor to the given list of parameters.
   * 
   * @param parameters the parameters associated with the constructor
   */
  public void setParameters(FormalParameterList parameters) {
    this.parameters = becomeParentOf(parameters);
  }

  /**
   * Set the token for the period before the constructor name to the given token.
   * 
   * @param period the token for the period before the constructor name
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  /**
   * Set the type of object being created to the given type name.
   * 
   * @param typeName the type of object being created
   */
  public void setReturnType(Identifier typeName) {
    returnType = becomeParentOf(typeName);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(parameters, visitor);
    initializers.accept(visitor);
    safelyVisitChild(body, visitor);
  }
}

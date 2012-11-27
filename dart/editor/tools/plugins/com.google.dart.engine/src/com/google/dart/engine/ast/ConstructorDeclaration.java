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
 * Instances of the class {@code ConstructorDeclaration} represent a constructor declaration.
 * 
 * <pre>
 * constructorDeclaration ::=
 *     constructorSignature {@link FunctionBody body}?
 *   | constructorName formalParameterList ':' 'this' ('.' {@link SimpleIdentifier name})? arguments
 *
 * constructorSignature ::=
 *     'external'? constructorName formalParameterList initializerList?
 *   | 'external'? 'factory' factoryName formalParameterList initializerList?
 *   | 'external'? 'const'  constructorName formalParameterList initializerList?
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
public class ConstructorDeclaration extends ClassMember {
  /**
   * The token for the 'external' keyword, or {@code null} if the constructor is not external.
   */
  private Token externalKeyword;

  /**
   * The token for the 'const' keyword.
   */
  private Token constKeyword;

  /**
   * The token for the 'factory' keyword.
   */
  private Token factoryKeyword;

  /**
   * The type of object being created. This can be different than the type in which the constructor
   * is being declared if the constructor is the implementation of a factory constructor.
   */
  private Identifier returnType;

  /**
   * The token for the period before the constructor name, or {@code null} if the constructor being
   * declared is unnamed.
   */
  private Token period;

  /**
   * The name of the constructor, or {@code null} if the constructor being declared is unnamed.
   */
  private SimpleIdentifier name;

  /**
   * The parameters associated with the constructor.
   */
  private FormalParameterList parameters;

  /**
   * The token for the separator (colon or equals) before the initializers, or {@code null} if there
   * are no initializers.
   */
  private Token separator;

  /**
   * The initializers associated with the constructor.
   */
  private NodeList<ConstructorInitializer> initializers = new NodeList<ConstructorInitializer>(this);

  /**
   * The name of the constructor to which this constructor will be redirected, or {@code null} if
   * this is not a redirecting factory constructor.
   */
  private ConstructorName redirectedConstructor;

  /**
   * The body of the constructor, or {@code null} if the constructor does not have a body.
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
   * @param externalKeyword the token for the 'external' keyword
   * @param comment the documentation comment associated with this constructor
   * @param metadata the annotations associated with this constructor
   * @param constKeyword the token for the 'const' keyword
   * @param factoryKeyword the token for the 'factory' keyword
   * @param returnType the return type of the constructor
   * @param period the token for the period before the constructor name
   * @param name the name of the constructor
   * @param parameters the parameters associated with the constructor
   * @param separator the token for the colon or equals before the initializers
   * @param initializers the initializers associated with the constructor
   * @param redirectedConstructor the name of the constructor to which this constructor will be
   *          redirected
   * @param body the body of the constructor
   */
  public ConstructorDeclaration(Comment comment, List<Annotation> metadata, Token externalKeyword,
      Token constKeyword, Token factoryKeyword, Identifier returnType, Token period,
      SimpleIdentifier name, FormalParameterList parameters, Token separator,
      List<ConstructorInitializer> initializers, ConstructorName redirectedConstructor,
      FunctionBody body) {
    super(comment, metadata);
    this.externalKeyword = externalKeyword;
    this.constKeyword = constKeyword;
    this.factoryKeyword = factoryKeyword;
    this.returnType = becomeParentOf(returnType);
    this.period = period;
    this.name = becomeParentOf(name);
    this.parameters = becomeParentOf(parameters);
    this.separator = separator;
    this.initializers.addAll(initializers);
    this.redirectedConstructor = becomeParentOf(redirectedConstructor);
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitConstructorDeclaration(this);
  }

  /**
   * Return the body of the constructor, or {@code null} if the constructor does not have a body.
   * 
   * @return the body of the constructor
   */
  public FunctionBody getBody() {
    return body;
  }

  /**
   * Return the token for the 'const' keyword.
   * 
   * @return the token for the 'const' keyword
   */
  public Token getConstKeyword() {
    return constKeyword;
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
   * Return the token for the 'external' keyword, or {@code null} if the constructor is not
   * external.
   * 
   * @return the token for the 'external' keyword
   */
  public Token getExternalKeyword() {
    return externalKeyword;
  }

  /**
   * Return the token for the 'factory' keyword.
   * 
   * @return the token for the 'factory' keyword
   */
  public Token getFactoryKeyword() {
    return factoryKeyword;
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
   * Return the name of the constructor, or {@code null} if the constructor being declared is
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
   * Return the token for the period before the constructor name, or {@code null} if the constructor
   * being declared is unnamed.
   * 
   * @return the token for the period before the constructor name
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Return the name of the constructor to which this constructor will be redirected, or
   * {@code null} if this is not a redirecting factory constructor.
   * 
   * @return the name of the constructor to which this constructor will be redirected
   */
  public ConstructorName getRedirectedConstructor() {
    return redirectedConstructor;
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
   * Return the token for the separator (colon or equals) before the initializers, or {@code null}
   * if there are no initializers.
   * 
   * @return the token for the separator (colon or equals) before the initializers
   */
  public Token getSeparator() {
    return separator;
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
   * Set the token for the 'const' keyword to the given token.
   * 
   * @param constKeyword the token for the 'const' keyword
   */
  public void setConstKeyword(Token constKeyword) {
    this.constKeyword = constKeyword;
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
   * Set the token for the 'factory' keyword to the given token.
   * 
   * @param factoryKeyword the token for the 'factory' keyword
   */
  public void setFactoryKeyword(Token factoryKeyword) {
    this.factoryKeyword = factoryKeyword;
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
   * Set the name of the constructor to which this constructor will be redirected to the given
   * constructor name.
   * 
   * @param redirectedConstructor the name of the constructor to which this constructor will be
   *          redirected
   */
  public void setRedirectedConstructor(ConstructorName redirectedConstructor) {
    this.redirectedConstructor = becomeParentOf(redirectedConstructor);
  }

  /**
   * Set the type of object being created to the given type name.
   * 
   * @param typeName the type of object being created
   */
  public void setReturnType(Identifier typeName) {
    returnType = becomeParentOf(typeName);
  }

  /**
   * Set the token for the separator (colon or equals) before the initializers to the given token.
   * 
   * @param separator the token for the separator (colon or equals) before the initializers
   */
  public void setSeparator(Token separator) {
    this.separator = separator;
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

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    Token leftMost = leftMost(externalKeyword, constKeyword, factoryKeyword);
    if (leftMost != null) {
      return leftMost;
    }
    return returnType.getBeginToken();
  }

  /**
   * Return the left-most of the given tokens, or {@code null} if there are no tokens given or if
   * all of the given tokens are {@code null}.
   * 
   * @param tokens the tokens being compared to find the left-most token
   * @return the left-most of the given tokens
   */
  private Token leftMost(Token... tokens) {
    Token leftMost = null;
    int offset = Integer.MAX_VALUE;
    for (Token token : tokens) {
      if (token != null && token.getOffset() < offset) {
        leftMost = token;
      }
    }
    return leftMost;
  }
}

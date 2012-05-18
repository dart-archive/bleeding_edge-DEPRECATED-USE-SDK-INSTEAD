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
 * Instances of the class <code>SuperConstructorInvocation</code> represent the invocation of a
 * superclass' constructor from within a constructor's initialization list.
 * 
 * <pre>
 * superInvocation ::=
 *     'super' ('.' {@link SimpleIdentifier name})? {@link ArgumentList argumentList}
 * </pre>
 */
public class SuperConstructorInvocation extends ConstructorInitializer {
  /**
   * The token for the 'super' keyword.
   */
  private Token keyword;

  /**
   * The token for the period before the name of the constructor that is being invoked, or
   * <code>null</code> if the unnamed constructor is being invoked.
   */
  private Token period;

  /**
   * The name of the constructor that is being invoked, or <code>null</code> if the unnamed
   * constructor is being invoked.
   */
  private SimpleIdentifier constructorName;

  /**
   * The list of arguments to the constructor.
   */
  private ArgumentList argumentList;

  /**
   * Initialize a newly created super invocation to invoke the inherited constructor with the given
   * name with the given arguments.
   */
  public SuperConstructorInvocation() {
  }

  /**
   * Initialize a newly created super invocation to invoke the inherited constructor with the given
   * name with the given arguments.
   * 
   * @param keyword the token for the 'super' keyword
   * @param period the token for the period before the name of the constructor that is being invoked
   * @param constructorName the name of the constructor that is being invoked
   * @param argumentList the list of arguments to the constructor
   */
  public SuperConstructorInvocation(Token keyword, Token period, SimpleIdentifier constructorName,
      ArgumentList argumentList) {
    this.keyword = keyword;
    this.period = period;
    this.constructorName = becomeParentOf(constructorName);
    this.argumentList = becomeParentOf(argumentList);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSuperConstructorInvocation(this);
  }

  /**
   * Return the list of arguments to the constructor.
   * 
   * @return the list of arguments to the constructor
   */
  public ArgumentList getArgumentList() {
    return argumentList;
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  /**
   * Return the name of the constructor that is being invoked, or <code>null</code> if the unnamed
   * constructor is being invoked.
   * 
   * @return the name of the constructor that is being invoked
   */
  public SimpleIdentifier getConstructorName() {
    return constructorName;
  }

  @Override
  public Token getEndToken() {
    return argumentList.getEndToken();
  }

  /**
   * Return the token for the 'super' keyword.
   * 
   * @return the token for the 'super' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the token for the period before the name of the constructor that is being invoked, or
   * <code>null</code> if the unnamed constructor is being invoked.
   * 
   * @return the token for the period before the name of the constructor that is being invoked
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Set the list of arguments to the constructor to the given list.
   * 
   * @param argumentList the list of arguments to the constructor
   */
  public void setArgumentList(ArgumentList argumentList) {
    this.argumentList = becomeParentOf(argumentList);
  }

  /**
   * Set the name of the constructor that is being invoked to the given identifier.
   * 
   * @param identifier the name of the constructor that is being invoked
   */
  public void setConstructorName(SimpleIdentifier identifier) {
    constructorName = becomeParentOf(identifier);
  }

  /**
   * Set the token for the 'super' keyword to the given token.
   * 
   * @param keyword the token for the 'super' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the token for the period before the name of the constructor that is being invoked to the
   * given token.
   * 
   * @param period the token for the period before the name of the constructor that is being invoked
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(constructorName, visitor);
    safelyVisitChild(argumentList, visitor);
  }
}

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
 * Instances of the class {@code InstanceCreationExpression} represent an instance creation
 * expression.
 * 
 * <pre>
 * newExpression ::=
 *     ('new' | 'const') {@link TypeName type} ('.' {@link SimpleIdentifier identifier})? {@link ArgumentList argumentList}
 * </pre>
 */
public class InstanceCreationExpression extends Expression {
  /**
   * The keyword used to indicate how an object should be created.
   */
  private Token keyword;

  /**
   * The name of the constructor to be invoked.
   */
  private ConstructorName constructorName;

  /**
   * The list of arguments to the constructor.
   */
  private ArgumentList argumentList;

  /**
   * Initialize a newly created instance creation expression.
   */
  public InstanceCreationExpression() {
  }

  /**
   * Initialize a newly created instance creation expression.
   * 
   * @param keyword the keyword used to indicate how an object should be created
   * @param constructorName the name of the constructor to be invoked
   * @param argumentList the list of arguments to the constructor
   */
  public InstanceCreationExpression(Token keyword, ConstructorName constructorName,
      ArgumentList argumentList) {
    this.keyword = keyword;
    this.constructorName = becomeParentOf(constructorName);
    this.argumentList = becomeParentOf(argumentList);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitInstanceCreationExpression(this);
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
   * Return the name of the constructor to be invoked.
   * 
   * @return the name of the constructor to be invoked
   */
  public ConstructorName getConstructorName() {
    return constructorName;
  }

  @Override
  public Token getEndToken() {
    return argumentList.getEndToken();
  }

  /**
   * Return the keyword used to indicate how an object should be created.
   * 
   * @return the keyword used to indicate how an object should be created
   */
  public Token getKeyword() {
    return keyword;
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
   * Set the name of the constructor to be invoked to the given name.
   * 
   * @param constructorName the name of the constructor to be invoked
   */
  public void setConstructorName(ConstructorName constructorName) {
    this.constructorName = constructorName;
  }

  /**
   * Set the keyword used to indicate how an object should be created to the given keyword.
   * 
   * @param keyword the keyword used to indicate how an object should be created
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(constructorName, visitor);
    safelyVisitChild(argumentList, visitor);
  }
}

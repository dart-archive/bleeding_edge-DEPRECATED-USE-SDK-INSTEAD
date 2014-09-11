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

import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code InstanceCreationExpression} represent an instance creation
 * expression.
 * 
 * <pre>
 * newExpression ::=
 *     ('new' | 'const') {@link TypeName type} ('.' {@link SimpleIdentifier identifier})? {@link ArgumentList argumentList}
 * </pre>
 * 
 * @coverage dart.engine.ast
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
   * The element associated with the constructor based on static type information, or {@code null}
   * if the AST structure has not been resolved or if the constructor could not be resolved.
   */
  private ConstructorElement staticElement;

  /**
   * The result of evaluating this expression, if it is constant.
   */
  private EvaluationResultImpl result;

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
  public <R> R accept(AstVisitor<R> visitor) {
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
   * Return the result of evaluating this constant as a compile-time constant expression, or
   * {@code null} if this variable is not a 'const' expression or an error prevented the result from
   * being computed.
   * 
   * @return the result of evaluating this constant
   */
  public EvaluationResultImpl getEvaluationResult() {
    return result;
  }

  /**
   * Return the keyword used to indicate how an object should be created.
   * 
   * @return the keyword used to indicate how an object should be created
   */
  public Token getKeyword() {
    return keyword;
  }

  @Override
  public int getPrecedence() {
    return 16;
  }

  /**
   * Return the element associated with the constructor based on static type information, or
   * {@code null} if the AST structure has not been resolved or if the constructor could not be
   * resolved.
   * 
   * @return the element associated with the constructor
   */
  public ConstructorElement getStaticElement() {
    return staticElement;
  }

  /**
   * Return {@code true} if this creation expression is used to invoke a constant constructor.
   * 
   * @return {@code true} if this creation expression is used to invoke a constant constructor
   */
  public boolean isConst() {
    return keyword instanceof KeywordToken
        && ((KeywordToken) keyword).getKeyword() == Keyword.CONST;
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
    this.constructorName = becomeParentOf(constructorName);
  }

  /**
   * Set the result of evaluating this expression as a compile-time constant expression to the given
   * result.
   * 
   * @param result the result of evaluating this expression
   */
  public void setEvaluationResult(EvaluationResultImpl result) {
    this.result = result;
  }

  /**
   * Set the keyword used to indicate how an object should be created to the given keyword.
   * 
   * @param keyword the keyword used to indicate how an object should be created
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the element associated with the constructor based on static type information to the given
   * element.
   * 
   * @param element the element to be associated with the constructor
   */
  public void setStaticElement(ConstructorElement element) {
    this.staticElement = element;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(constructorName, visitor);
    safelyVisitChild(argumentList, visitor);
  }
}

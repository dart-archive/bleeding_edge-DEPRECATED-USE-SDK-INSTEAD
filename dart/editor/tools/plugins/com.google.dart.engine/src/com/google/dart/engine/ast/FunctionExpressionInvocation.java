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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code FunctionExpressionInvocation} represent the invocation of a
 * function resulting from evaluating an expression. Invocations of methods and other forms of
 * functions are represented by {@link MethodInvocation method invocation} nodes. Invocations of
 * getters and setters are represented by either {@link PrefixedIdentifier prefixed identifier} or
 * {@link PropertyAccess property access} nodes.
 * 
 * <pre>
 * functionExpressionInvoction ::=
 *     {@link Expression function} {@link ArgumentList argumentList}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class FunctionExpressionInvocation extends Expression {
  /**
   * The expression producing the function being invoked.
   */
  private Expression function;

  /**
   * The list of arguments to the function.
   */
  private ArgumentList argumentList;

  /**
   * The element associated with the function being invoked, or {@code null} if the AST structure
   * has not been resolved or the function could not be resolved.
   */
  private ExecutableElement element;

  /**
   * Initialize a newly created function expression invocation.
   * 
   * @param function the expression producing the function being invoked
   * @param argumentList the list of arguments to the method
   */
  public FunctionExpressionInvocation(Expression function, ArgumentList argumentList) {
    this.function = becomeParentOf(function);
    this.argumentList = becomeParentOf(argumentList);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFunctionExpressionInvocation(this);
  }

  /**
   * Return the list of arguments to the method.
   * 
   * @return the list of arguments to the method
   */
  public ArgumentList getArgumentList() {
    return argumentList;
  }

  @Override
  public Token getBeginToken() {
    return function.getBeginToken();
  }

  /**
   * Return the element associated with the function being invoked, or {@code null} if the AST
   * structure has not been resolved or the function could not be resolved. One common example of
   * the latter case is an expression whose value can change over time.
   * 
   * @return the element associated with the function being invoked
   */
  public ExecutableElement getElement() {
    return element;
  }

  @Override
  public Token getEndToken() {
    return argumentList.getEndToken();
  }

  /**
   * Return the expression producing the function being invoked.
   * 
   * @return the expression producing the function being invoked
   */
  public Expression getFunction() {
    return function;
  }

  /**
   * Set the list of arguments to the method to the given list.
   * 
   * @param argumentList the list of arguments to the method
   */
  public void setArgumentList(ArgumentList argumentList) {
    this.argumentList = becomeParentOf(argumentList);
  }

  /**
   * Set the element associated with the function being invoked to the given element.
   * 
   * @param element the element associated with the function being invoked
   */
  public void setElement(ExecutableElement element) {
    this.element = element;
  }

  /**
   * Set the expression producing the function being invoked to the given expression.
   * 
   * @param function the expression producing the function being invoked
   */
  public void setFunction(Expression function) {
    function = becomeParentOf(function);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(function, visitor);
    safelyVisitChild(argumentList, visitor);
  }
}

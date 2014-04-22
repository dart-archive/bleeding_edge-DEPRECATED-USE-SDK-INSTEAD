/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code VariableElementImpl} implement a {@code VariableElement}.
 * 
 * @coverage dart.engine.element
 */
public abstract class VariableElementImpl extends ElementImpl implements VariableElement {
  /**
   * The declared type of this variable.
   */
  private Type type;

  /**
   * A synthetic function representing this variable's initializer, or {@code null} if this variable
   * does not have an initializer.
   */
  private FunctionElement initializer;

  /**
   * An empty array of variable elements.
   */
  public static final VariableElement[] EMPTY_ARRAY = new VariableElement[0];

  /**
   * Initialize a newly created variable element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public VariableElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created variable element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public VariableElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  /**
   * Return the result of evaluating this variable's initializer as a compile-time constant
   * expression, or {@code null} if this variable is not a 'const' variable, if it does not have an
   * initializer, or if the compilation unit containing the variable has not been resolved.
   * 
   * @return the result of evaluating this variable's initializer
   */
  public EvaluationResultImpl getEvaluationResult() {
    return null;
  }

  @Override
  public FunctionElement getInitializer() {
    return initializer;
  }

  @Override
  public VariableDeclaration getNode() throws AnalysisException {
    return getNodeMatching(VariableDeclaration.class);
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public boolean isConst() {
    return hasModifier(Modifier.CONST);
  }

  @Override
  public boolean isFinal() {
    return hasModifier(Modifier.FINAL);
  }

  /**
   * Return {@code true} if this variable is potentially mutated somewhere in a closure. This
   * information is only available for local variables (including parameters) and only after the
   * compilation unit containing the variable has been resolved.
   * 
   * @return {@code true} if this variable is potentially mutated somewhere in closure
   */
  public boolean isPotentiallyMutatedInClosure() {
    return false;
  }

  /**
   * Return {@code true} if this variable is potentially mutated somewhere in its scope. This
   * information is only available for local variables (including parameters) and only after the
   * compilation unit containing the variable has been resolved.
   * 
   * @return {@code true} if this variable is potentially mutated somewhere in its scope
   */
  public boolean isPotentiallyMutatedInScope() {
    return false;
  }

  /**
   * Set whether this variable is const to correspond to the given value.
   * 
   * @param isConst {@code true} if the variable is const
   */
  public void setConst(boolean isConst) {
    setModifier(Modifier.CONST, isConst);
  }

  /**
   * Set the result of evaluating this variable's initializer as a compile-time constant expression
   * to the given result.
   * 
   * @param result the result of evaluating this variable's initializer
   */
  public void setEvaluationResult(EvaluationResultImpl result) {
    throw new IllegalStateException("Invalid attempt to set a compile-time constant result");
  }

  /**
   * Set whether this variable is final to correspond to the given value.
   * 
   * @param isFinal {@code true} if the variable is final
   */
  public void setFinal(boolean isFinal) {
    setModifier(Modifier.FINAL, isFinal);
  }

  /**
   * Set the function representing this variable's initializer to the given function.
   * 
   * @param initializer the function representing this variable's initializer
   */
  public void setInitializer(FunctionElement initializer) {
    if (initializer != null) {
      ((FunctionElementImpl) initializer).setEnclosingElement(this);
    }
    this.initializer = initializer;
  }

  /**
   * Set the declared type of this variable to the given type.
   * 
   * @param type the declared type of this variable
   */
  public void setType(Type type) {
    this.type = type;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(initializer, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append(getType());
    builder.append(" ");
    builder.append(getDisplayName());
  }
}

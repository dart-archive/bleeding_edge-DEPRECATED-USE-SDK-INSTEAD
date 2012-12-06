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
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code VariableElementImpl} implement a {@code VariableElement}.
 */
public class VariableElementImpl extends ElementImpl implements VariableElement {
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

  @Override
  public FunctionElement getInitializer() {
    return initializer;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.VARIABLE;
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
   * Set whether this variable is const to correspond to the given value.
   * 
   * @param isConst {@code true} if the variable is const
   */
  public void setConst(boolean isConst) {
    setModifier(Modifier.CONST, isConst);
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
}

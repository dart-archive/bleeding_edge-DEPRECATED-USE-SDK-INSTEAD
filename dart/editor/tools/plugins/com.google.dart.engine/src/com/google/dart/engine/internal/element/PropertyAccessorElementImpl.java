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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code PropertyAccessorElementImpl} implement a
 * {@code PropertyAccessorElement}.
 * 
 * @coverage dart.engine.element
 */
public class PropertyAccessorElementImpl extends ExecutableElementImpl implements
    PropertyAccessorElement {
  /**
   * The variable associated with this accessor.
   */
  private PropertyInducingElement variable;

  /**
   * An empty array of property accessor elements.
   */
  public static final PropertyAccessorElement[] EMPTY_ARRAY = new PropertyAccessorElement[0];

  /**
   * Initialize a newly created property accessor element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public PropertyAccessorElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created synthetic property accessor element to be associated with the given
   * variable.
   * 
   * @param variable the variable with which this access is associated
   */
  @DartName("forVariable")
  public PropertyAccessorElementImpl(PropertyInducingElementImpl variable) {
    super(variable.getName(), variable.getNameOffset());
    this.variable = variable;
    setStatic(variable.isStatic());
    setSynthetic(true);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitPropertyAccessorElement(this);
  }

  @Override
  public boolean equals(Object object) {
    return super.equals(object) && isGetter() == ((PropertyAccessorElement) object).isGetter();
  }

  @Override
  public PropertyAccessorElement getCorrespondingGetter() {
    if (isGetter() || variable == null) {
      return null;
    }
    return variable.getGetter();
  }

  @Override
  public PropertyAccessorElement getCorrespondingSetter() {
    if (isSetter() || variable == null) {
      return null;
    }
    return variable.getSetter();
  }

  @Override
  public ElementKind getKind() {
    if (isGetter()) {
      return ElementKind.GETTER;
    }
    return ElementKind.SETTER;
  }

  @Override
  public String getName() {
    if (isSetter()) {
      return super.getName() + '=';
    }
    return super.getName();
  }

  @Override
  public AstNode getNode() throws AnalysisException {
    if (isSynthetic()) {
      return null;
    }
    if (getEnclosingElement() instanceof ClassElement) {
      return getNodeMatching(MethodDeclaration.class);
    }
    if (getEnclosingElement() instanceof CompilationUnitElement) {
      return getNodeMatching(FunctionDeclaration.class);
    }
    return null;
  }

  @Override
  public PropertyInducingElement getVariable() {
    return variable;
  }

  @Override
  public int hashCode() {
    return ObjectUtilities.combineHashCodes(super.hashCode(), isGetter() ? 1 : 2);
  }

  @Override
  public boolean isAbstract() {
    return hasModifier(Modifier.ABSTRACT);
  }

  @Override
  public boolean isGetter() {
    return hasModifier(Modifier.GETTER);
  }

  @Override
  public boolean isSetter() {
    return hasModifier(Modifier.SETTER);
  }

  @Override
  public boolean isStatic() {
    return hasModifier(Modifier.STATIC);
  }

  /**
   * Set whether this accessor is abstract to correspond to the given value.
   * 
   * @param isAbstract {@code true} if the accessor is abstract
   */
  public void setAbstract(boolean isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set whether this accessor is a getter to correspond to the given value.
   * 
   * @param isGetter {@code true} if the accessor is a getter
   */
  public void setGetter(boolean isGetter) {
    setModifier(Modifier.GETTER, isGetter);
  }

  /**
   * Set whether this accessor is a setter to correspond to the given value.
   * 
   * @param isSetter {@code true} if the accessor is a setter
   */
  public void setSetter(boolean isSetter) {
    setModifier(Modifier.SETTER, isSetter);
  }

  /**
   * Set whether this accessor is static to correspond to the given value.
   * 
   * @param isStatic {@code true} if the accessor is static
   */
  public void setStatic(boolean isStatic) {
    setModifier(Modifier.STATIC, isStatic);
  }

  /**
   * Set the variable associated with this accessor to the given variable.
   * 
   * @param variable the variable associated with this accessor
   */
  public void setVariable(PropertyInducingElement variable) {
    this.variable = variable;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append(isGetter() ? "get " : "set ");
    builder.append(getVariable().getDisplayName());
    super.appendTo(builder);
  }

  @Override
  protected String getIdentifier() {
    String name = getDisplayName();
    String suffix = isGetter() ? "?" : "=";
    return name + suffix;
  }
}

/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.element.member;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * Instances of the class {@code ParameterMember} represent a parameter element defined in a
 * parameterized type where the values of the type parameters are known.
 */
public class ParameterMember extends VariableMember implements ParameterElement {
  /**
   * If the given parameter's type is different when any type parameters from the defining type's
   * declaration are replaced with the actual type arguments from the defining type, create a
   * parameter member representing the given parameter. Return the member that was created, or the
   * base parameter if no member was created.
   * 
   * @param baseParameter the base parameter for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the parameter element that will return the correctly substituted types
   */
  public static ParameterElement from(ParameterElement baseParameter, InterfaceType definingType) {
    if (baseParameter == null || definingType.getTypeArguments().length == 0) {
      return baseParameter;
    }
    Type baseType = baseParameter.getType();
    Type[] argumentTypes = definingType.getTypeArguments();
    Type[] parameterTypes = TypeVariableTypeImpl.getTypes(definingType.getElement().getTypeVariables());
    Type substitutedType = baseType.substitute(argumentTypes, parameterTypes);
    if (baseType.equals(substitutedType)) {
      return baseParameter;
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    return new ParameterMember(baseParameter, definingType);
  }

  /**
   * Initialize a newly created element to represent a parameter of the given parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public ParameterMember(ParameterElement baseElement, InterfaceType definingType) {
    super(baseElement, definingType);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends Element> E getAncestor(Class<E> elementClass) {
    E element = getBaseElement().getAncestor(elementClass);
    if (element instanceof MethodElement) {
      return (E) MethodMember.from((MethodElement) element, getDefiningType());
    } else if (element instanceof PropertyAccessorElement) {
      return (E) PropertyAccessorMember.from((PropertyAccessorElement) element, getDefiningType());
    }
    return element;
  }

  @Override
  public ParameterElement getBaseElement() {
    return (ParameterElement) super.getBaseElement();
  }

  @Override
  public Element getEnclosingElement() {
    return getBaseElement().getEnclosingElement();
  }

  @Override
  public ParameterKind getParameterKind() {
    return getBaseElement().getParameterKind();
  }

  @Override
  public ParameterElement[] getParameters() {
    ParameterElement[] baseParameters = getBaseElement().getParameters();
    int parameterCount = baseParameters.length;
    if (parameterCount == 0) {
      return baseParameters;
    }
    ParameterElement[] parameterizedParameters = new ParameterElement[parameterCount];
    for (int i = 0; i < parameterCount; i++) {
      parameterizedParameters[i] = ParameterMember.from(baseParameters[i], getDefiningType());
    }
    return parameterizedParameters;
  }

  @Override
  public SourceRange getVisibleRange() {
    return getBaseElement().getVisibleRange();
  }

  @Override
  public boolean isInitializingFormal() {
    return getBaseElement().isInitializingFormal();
  }

  @Override
  public String toString() {
    ParameterElement baseElement = getBaseElement();
    String left = "";
    String right = "";
    switch (baseElement.getParameterKind()) {
      case NAMED:
        left = "{";
        right = "}";
        break;
      case POSITIONAL:
        left = "[";
        right = "]";
        break;
    }
    StringBuilder builder = new StringBuilder();
    builder.append(left);
    builder.append(getType());
    builder.append(" ");
    builder.append(baseElement.getName());
    builder.append(right);
    return builder.toString();
  }
}

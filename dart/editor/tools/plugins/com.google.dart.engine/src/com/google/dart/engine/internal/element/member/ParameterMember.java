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

import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.ParameterizedType;
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
  public static ParameterElement from(ParameterElement baseParameter, ParameterizedType definingType) {
    if (baseParameter == null || definingType.getTypeArguments().length == 0) {
      return baseParameter;
    }
    // Check if parameter type depends on defining type type arguments.
    // It is possible that we did not resolve field formal parameter yet, so skip this check for it.
    boolean isFieldFormal = baseParameter instanceof FieldFormalParameterElement;
    if (!isFieldFormal) {
      Type baseType = baseParameter.getType();
      Type[] argumentTypes = definingType.getTypeArguments();
      Type[] parameterTypes = TypeParameterTypeImpl.getTypes(definingType.getTypeParameters());
      Type substitutedType = baseType.substitute(argumentTypes, parameterTypes);
      if (baseType.equals(substitutedType)) {
        return baseParameter;
      }
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    if (isFieldFormal) {
      return new FieldFormalParameterMember(
          (FieldFormalParameterElement) baseParameter,
          definingType);
    }
    return new ParameterMember(baseParameter, definingType);
  }

  /**
   * Initialize a newly created element to represent a parameter of the given parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public ParameterMember(ParameterElement baseElement, ParameterizedType definingType) {
    super(baseElement, definingType);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitParameterElement(this);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends Element> E getAncestor(Class<E> elementClass) {
    E element = getBaseElement().getAncestor(elementClass);
    ParameterizedType definingType = getDefiningType();
    if (definingType instanceof InterfaceType) {
      InterfaceType definingInterfaceType = (InterfaceType) definingType;
      if (element instanceof ConstructorElement) {
        return (E) ConstructorMember.from((ConstructorElement) element, definingInterfaceType);
      } else if (element instanceof MethodElement) {
        return (E) MethodMember.from((MethodElement) element, definingInterfaceType);
      } else if (element instanceof PropertyAccessorElement) {
        return (E) PropertyAccessorMember.from(
            (PropertyAccessorElement) element,
            definingInterfaceType);
      }
    }
    return element;
  }

  @Override
  public ParameterElement getBaseElement() {
    return (ParameterElement) super.getBaseElement();
  }

  @Override
  public String getDefaultValueCode() {
    return getBaseElement().getDefaultValueCode();
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
      case REQUIRED:
        // No need to change the default.
        break;
    }
    StringBuilder builder = new StringBuilder();
    builder.append(left);
    builder.append(getType());
    builder.append(" ");
    builder.append(baseElement.getDisplayName());
    builder.append(right);
    return builder.toString();
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(getParameters(), visitor);
  }
}

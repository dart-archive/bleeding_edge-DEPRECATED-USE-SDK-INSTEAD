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

import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code MethodMember} represent a method element defined in a parameterized
 * type where the values of the type parameters are known.
 */
public class MethodMember extends ExecutableMember implements MethodElement {
  /**
   * If the given method's type is different when any type parameters from the defining type's
   * declaration are replaced with the actual type arguments from the defining type, create a method
   * member representing the given method. Return the member that was created, or the base method if
   * no member was created.
   * 
   * @param baseMethod the base method for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the method element that will return the correctly substituted types
   */
  public static MethodElement from(MethodElement baseMethod, InterfaceType definingType) {
    if (baseMethod == null || definingType.getTypeArguments().length == 0) {
      return baseMethod;
    }
    FunctionType baseType = baseMethod.getType();
    Type[] argumentTypes = definingType.getTypeArguments();
    Type[] parameterTypes = definingType.getElement().getType().getTypeArguments();
    FunctionType substitutedType = baseType.substitute(argumentTypes, parameterTypes);
    if (baseType.equals(substitutedType)) {
      return baseMethod;
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    return new MethodMember(baseMethod, definingType);
  }

  /**
   * Initialize a newly created element to represent a method of the given parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public MethodMember(MethodElement baseElement, InterfaceType definingType) {
    super(baseElement, definingType);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitMethodElement(this);
  }

  @Override
  public MethodElement getBaseElement() {
    return (MethodElement) super.getBaseElement();
  }

  @Override
  public ClassElement getEnclosingElement() {
    return getBaseElement().getEnclosingElement();
  }

  @Override
  public MethodDeclaration getNode() throws AnalysisException {
    return getBaseElement().getNode();
  }

  @Override
  public boolean isAbstract() {
    return getBaseElement().isAbstract();
  }

  @Override
  public String toString() {
    MethodElement baseElement = getBaseElement();
    ParameterElement[] parameters = getParameters();
    FunctionType type = getType();
    StringBuilder builder = new StringBuilder();
    builder.append(baseElement.getEnclosingElement().getDisplayName());
    builder.append(".");
    builder.append(baseElement.getDisplayName());
    builder.append("(");
    int parameterCount = parameters.length;
    for (int i = 0; i < parameterCount; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(parameters[i]).toString();
    }
    builder.append(")");
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.getReturnType());
    }
    return builder.toString();
  }
}

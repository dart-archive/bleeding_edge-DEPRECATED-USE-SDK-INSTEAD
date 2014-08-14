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

import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * The abstract class {@code ExecutableMember} defines the behavior common to members that represent
 * an executable element defined in a parameterized type where the values of the type parameters are
 * known.
 */
public abstract class ExecutableMember extends Member implements ExecutableElement {
  /**
   * Initialize a newly created element to represent an executable element of the given
   * parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public ExecutableMember(ExecutableElement baseElement, InterfaceType definingType) {
    super(baseElement, definingType);
  }

  @Override
  public ExecutableElement getBaseElement() {
    return (ExecutableElement) super.getBaseElement();
  }

  @Override
  public FunctionElement[] getFunctions() {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
//    return getBaseElement().getFunctions();
  }

  @Override
  public LabelElement[] getLabels() {
    return getBaseElement().getLabels();
  }

  @Override
  public LocalVariableElement[] getLocalVariables() {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
//    return getBaseElement().getLocalVariables();
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
  public Type getReturnType() {
    return substituteFor(getBaseElement().getReturnType());
  }

  @Override
  public FunctionType getType() {
    return substituteFor(getBaseElement().getType());
  }

  @Override
  public boolean isAsynchronous() {
    return getBaseElement().isAsynchronous();
  }

  @Override
  public boolean isGenerator() {
    return getBaseElement().isGenerator();
  }

  @Override
  public boolean isOperator() {
    return getBaseElement().isOperator();
  }

  @Override
  public boolean isStatic() {
    return getBaseElement().isStatic();
  }

  @Override
  public boolean isSynchronous() {
    return getBaseElement().isSynchronous();
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    // TODO(brianwilkerson) We need to finish implementing the accessors used below so that we can
    // safely invoke them.
    super.visitChildren(visitor);
    safelyVisitChildren(getBaseElement().getFunctions(), visitor);
    safelyVisitChildren(getLabels(), visitor);
    safelyVisitChildren(getBaseElement().getLocalVariables(), visitor);
    safelyVisitChildren(getParameters(), visitor);
  }
}

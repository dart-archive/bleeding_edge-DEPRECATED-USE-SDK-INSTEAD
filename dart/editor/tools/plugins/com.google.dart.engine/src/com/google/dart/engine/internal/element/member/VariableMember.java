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

import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.type.ParameterizedType;
import com.google.dart.engine.type.Type;

/**
 * The abstract class {@code VariableMember} defines the behavior common to members that represent a
 * variable element defined in a parameterized type where the values of the type parameters are
 * known.
 */
public abstract class VariableMember extends Member implements VariableElement {
  /**
   * Initialize a newly created element to represent an executable element of the given
   * parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public VariableMember(VariableElement baseElement, ParameterizedType definingType) {
    super(baseElement, definingType);
  }

  @Override
  public VariableElement getBaseElement() {
    return (VariableElement) super.getBaseElement();
  }

  @Override
  public FunctionElement getInitializer() {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
//    return getBaseElement().getInitializer();
  }

  @Override
  public VariableDeclaration getNode() throws AnalysisException {
    return getBaseElement().getNode();
  }

  @Override
  public Type getType() {
    return substituteFor(getBaseElement().getType());
  }

  @Override
  public boolean isConst() {
    return getBaseElement().isConst();
  }

  @Override
  public boolean isFinal() {
    return getBaseElement().isFinal();
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    // TODO(brianwilkerson) We need to finish implementing the accessors used below so that we can
    // safely invoke them.
    super.visitChildren(visitor);
    safelyVisitChild(getBaseElement().getInitializer(), visitor);
  }
}

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
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.utilities.dart.ParameterKind;

/**
 * Instances of the class {@code ParameterElementImpl} implement a {@code ParameterElement}.
 */
public class ParameterElementImpl extends VariableElementImpl implements ParameterElement {
  /**
   * The kind of this parameter.
   */
  private ParameterKind parameterKind;

  /**
   * An empty array of field elements.
   */
  public static final ParameterElement[] EMPTY_ARRAY = new ParameterElement[0];

  /**
   * Initialize a newly created parameter element to have the given name.
   * 
   * @param name the name of this element
   */
  public ParameterElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.PARAMETER;
  }

  @Override
  public ParameterKind getParameterKind() {
    return parameterKind;
  }

  /**
   * Set the kind of this parameter to the given kind.
   * 
   * @param parameterKind the new kind of this parameter
   */
  public void setParameterKind(ParameterKind parameterKind) {
    this.parameterKind = parameterKind;
  }
}

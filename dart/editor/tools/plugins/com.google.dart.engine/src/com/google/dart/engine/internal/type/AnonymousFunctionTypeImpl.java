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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.internal.element.ParameterElementImpl;

/**
 * Instances of the class {@code AnonymousFunctionTypeImpl} extend the behavior of
 * {@link FunctionTypeImpl} to support anonymous function types created for function typed
 * parameters.
 * 
 * @coverage dart.engine.type
 */
public class AnonymousFunctionTypeImpl extends FunctionTypeImpl {
  /**
   * An array of parameters elements of this type of function.
   */
  private ParameterElement[] baseParameters = ParameterElementImpl.EMPTY_ARRAY;

  public AnonymousFunctionTypeImpl() {
    super((FunctionTypeAliasElement) null);
  }

  /**
   * Sets the parameters elements of this type of function.
   * 
   * @param parameters the parameters elements of this type of function
   */
  public void setBaseParameters(ParameterElement[] parameters) {
    this.baseParameters = parameters;
  }

  @Override
  protected ParameterElement[] getBaseParameters() {
    return baseParameters;
  }
}

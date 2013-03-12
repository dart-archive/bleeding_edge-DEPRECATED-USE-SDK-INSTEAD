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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;

/**
 * Instances of the class {@code ConstLocalVariableElementImpl} implement a
 * {@code LocalVariableElement} for a local 'const' variable that has an initializer.
 * 
 * @coverage dart.engine.element
 */
public class ConstLocalVariableElementImpl extends LocalVariableElementImpl {
  /**
   * The result of evaluating this variable's initializer.
   */
  private EvaluationResultImpl result;

  /**
   * Initialize a newly created local variable element to have the given name.
   * 
   * @param name the name of this element
   */
  public ConstLocalVariableElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public EvaluationResultImpl getEvaluationResult() {
    return result;
  }

  @Override
  public void setEvaluationResult(EvaluationResultImpl result) {
    this.result = result;
  }
}

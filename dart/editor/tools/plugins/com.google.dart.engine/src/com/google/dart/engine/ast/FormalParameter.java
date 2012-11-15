/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.utilities.dart.ParameterKind;

/**
 * The abstract class {@code FormalParameter} defines the behavior of objects representing a
 * parameter to a function.
 * 
 * <pre>
 * formalParameter ::=
 *     {@link NormalFormalParameter normalFormalParameter}
 *   | {@link DefaultFormalParameter namedFormalParameter}
 *   | {@link DefaultFormalParameter optionalFormalParameter}
 * </pre>
 */
public abstract class FormalParameter extends ASTNode {
  /**
   * Return the kind of this parameter.
   * 
   * @return the kind of this parameter
   */
  public abstract ParameterKind getKind();
}

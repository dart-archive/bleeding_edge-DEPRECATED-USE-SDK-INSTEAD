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

import com.google.dart.engine.element.ParameterElement;
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
 * 
 * @coverage dart.engine.ast
 */
public abstract class FormalParameter extends AstNode {
  /**
   * Return the element representing this parameter, or {@code null} if this parameter has not been
   * resolved.
   * 
   * @return the element representing this parameter
   */
  public ParameterElement getElement() {
    SimpleIdentifier identifier = getIdentifier();
    if (identifier == null) {
      return null;
    }
    return (ParameterElement) identifier.getStaticElement();
  }

  /**
   * Return the name of the parameter being declared.
   * 
   * @return the name of the parameter being declared
   */
  public abstract SimpleIdentifier getIdentifier();

  /**
   * Return the kind of this parameter.
   * 
   * @return the kind of this parameter
   */
  public abstract ParameterKind getKind();

  /**
   * Return {@code true} if this parameter was declared with the 'const' modifier.
   * 
   * @return {@code true} if this parameter was declared with the 'const' modifier
   */
  public abstract boolean isConst();

  /**
   * Return {@code true} if this parameter was declared with the 'final' modifier. Parameters that
   * are declared with the 'const' modifier will return {@code false} even though they are
   * implicitly final.
   * 
   * @return {@code true} if this parameter was declared with the 'final' modifier
   */
  public abstract boolean isFinal();
}

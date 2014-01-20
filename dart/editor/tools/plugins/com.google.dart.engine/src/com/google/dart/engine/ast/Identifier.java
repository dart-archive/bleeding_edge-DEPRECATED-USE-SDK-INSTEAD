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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * The abstract class {@code Identifier} defines the behavior common to nodes that represent an
 * identifier.
 * 
 * <pre>
 * identifier ::=
 *     {@link SimpleIdentifier simpleIdentifier}
 *   | {@link PrefixedIdentifier prefixedIdentifier}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class Identifier extends Expression {
  /**
   * Return {@code true} if the given name is visible only within the library in which it is
   * declared.
   * 
   * @param name the name being tested
   * @return {@code true} if the given name is private
   */
  public static boolean isPrivateName(String name) {
    return StringUtilities.startsWithChar(name, '_');
  }

  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then {@code null} will be returned.
   * 
   * @return the best element available for this operator
   */
  public abstract Element getBestElement();

  /**
   * Return the lexical representation of the identifier.
   * 
   * @return the lexical representation of the identifier
   */
  public abstract String getName();

  /**
   * Return the element associated with this identifier based on propagated type information, or
   * {@code null} if the AST structure has not been resolved or if this identifier could not be
   * resolved. One example of the latter case is an identifier that is not defined within the scope
   * in which it appears.
   * 
   * @return the element associated with this identifier
   */
  public abstract Element getPropagatedElement();

  /**
   * Return the element associated with this identifier based on static type information, or
   * {@code null} if the AST structure has not been resolved or if this identifier could not be
   * resolved. One example of the latter case is an identifier that is not defined within the scope
   * in which it appears
   * 
   * @return the element associated with the operator
   */
  public abstract Element getStaticElement();

  @Override
  public boolean isAssignable() {
    return true;
  }
}

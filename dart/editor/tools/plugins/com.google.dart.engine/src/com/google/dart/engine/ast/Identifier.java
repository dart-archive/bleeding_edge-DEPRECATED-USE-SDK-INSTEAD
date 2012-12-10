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

/**
 * The abstract class {@code Identifier} defines the behavior common to nodes that represent an
 * identifier.
 * 
 * <pre>
 * identifier ::=
 *     {@link SimpleIdentifier simpleIdentifier}
 *   | {@link PrefixedIdentifier prefixedIdentifier}
 * </pre>
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
    return name.startsWith("_");
  }

  /**
   * The element associated with this identifier, or {@code null} if the AST structure has not been
   * resolved or if this identifier could not be resolved.
   */
  private Element element;

  /**
   * Return the element associated with this identifier, or {@code null} if the AST structure has
   * not been resolved or if this identifier could not be resolved. One example of the latter case
   * is an identifier that is not defined within the scope in which it appears.
   * 
   * @return the element associated with this identifier
   */
  public Element getElement() {
    return element;
  }

  /**
   * Return the lexical representation of the identifier.
   * 
   * @return the lexical representation of the identifier
   */
  public abstract String getName();

  @Override
  public boolean isAssignable() {
    return true;
  }

  /**
   * Set the element associated with this identifier to the given element.
   * 
   * @param element the element associated with this identifier
   */
  public void setElement(Element element) {
    this.element = element;
  }
}

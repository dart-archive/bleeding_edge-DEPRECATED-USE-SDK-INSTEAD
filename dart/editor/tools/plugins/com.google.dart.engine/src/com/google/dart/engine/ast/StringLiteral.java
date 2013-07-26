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

/**
 * Instances of the class {@code StringLiteral} represent a string literal expression.
 * 
 * <pre>
 * stringLiteral ::=
 *     {@link SimpleStringLiteral simpleStringLiteral}
 *   | {@link AdjacentStrings adjacentStrings}
 *   | {@link StringInterpolation stringInterpolation}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class StringLiteral extends Literal {
  /**
   * Return the value of the string literal, or {@code null} if the string is not a constant string
   * without any string interpolation.
   * 
   * @return the value of the string literal
   */
  public String getStringValue() {
    StringBuilder builder = new StringBuilder();
    try {
      appendStringValue(builder);
    } catch (IllegalArgumentException exception) {
      return null;
    }
    return builder.toString();
  }

  /**
   * Append the value of the given string literal to the given string builder.
   * 
   * @param builder the builder to which the string's value is to be appended
   * @throws IllegalArgumentException if the string is not a constant string without any string
   *           interpolation
   */
  protected abstract void appendStringValue(StringBuilder builder) throws IllegalArgumentException;
}

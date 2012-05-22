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

import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class <code>InterpolationString</code> represent a non-empty substring of an
 * interpolated string.
 * 
 * <pre>
 * interpolationString ::=
 *     characters
 * </pre>
 */
public class InterpolationString extends InterpolationElement {
  /**
   * The characters that will be added to the string.
   */
  private Token contents;

  /**
   * Initialize a newly created string of characters that are part of a string interpolation.
   */
  public InterpolationString() {
  }

  /**
   * Initialize a newly created string of characters that are part of a string interpolation.
   * 
   * @param the characters that will be added to the string
   */
  public InterpolationString(Token contents) {
    this.contents = contents;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitInterpolationString(this);
  }

  @Override
  public Token getBeginToken() {
    return contents;
  }

  /**
   * Return the characters that will be added to the string.
   * 
   * @return the characters that will be added to the string
   */
  public Token getContents() {
    return contents;
  }

  @Override
  public Token getEndToken() {
    return contents;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  /**
   * Set the characters that will be added to the string to those in the given string.
   * 
   * @param string the characters that will be added to the string
   */
  public void setContents(Token string) {
    contents = string;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
  }
}

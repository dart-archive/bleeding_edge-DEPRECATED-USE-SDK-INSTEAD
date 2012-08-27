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

import java.util.List;

/**
 * Instances of the class {@code StringInterpolation} represent a string interpolation literal.
 * 
 * <pre>
 * stringInterpolation ::=
 *     ''' {@link InterpolationElement interpolationElement}* '''
 *   | '"' {@link InterpolationElement interpolationElement}* '"'
 * </pre>
 */
public class StringInterpolation extends StringLiteral {
  /**
   * The elements that will be composed to produce the resulting string.
   */
  private NodeList<InterpolationElement> elements = new NodeList<InterpolationElement>(this);

  /**
   * Initialize a newly created string interpolation expression.
   */
  public StringInterpolation() {
  }

  /**
   * Initialize a newly created string interpolation expression.
   * 
   * @param elements the elements that will be composed to produce the resulting string
   */
  public StringInterpolation(List<InterpolationElement> elements) {
    this.elements.addAll(elements);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitStringInterpolation(this);
  }

  @Override
  public Token getBeginToken() {
    return elements.getBeginToken();
  }

  /**
   * Return the elements that will be composed to produce the resulting string.
   * 
   * @return the elements that will be composed to produce the resulting string
   */
  public NodeList<InterpolationElement> getElements() {
    return elements;
  }

  @Override
  public Token getEndToken() {
    return elements.getEndToken();
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    elements.accept(visitor);
  }
}

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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code AdjacentStrings} represents two or more string literals that are
 * implicitly concatenated because of being adjacent (separated only by whitespace).
 * 
 * <pre>
 * adjacentStrings ::=
 *     {@link StringLiteral string} {@link StringLiteral string}+
 * </pre>
 */
public class AdjacentStrings extends StringLiteral {
  /**
   * The strings that are implicitly concatenated.
   */
  private NodeList<StringLiteral> strings = new NodeList<StringLiteral>(this);

  /**
   * Initialize a newly created list of adjacent strings.
   * 
   * @param strings the strings that are implicitly concatenated
   */
  public AdjacentStrings(List<StringLiteral> strings) {
    this.strings.addAll(strings);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitAdjacentStrings(this);
  }

  @Override
  public Token getBeginToken() {
    return strings.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return strings.getEndToken();
  }

  /**
   * Return the strings that are implicitly concatenated.
   * 
   * @return the strings that are implicitly concatenated
   */
  public NodeList<StringLiteral> getStrings() {
    return strings;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    strings.accept(visitor);
  }
}

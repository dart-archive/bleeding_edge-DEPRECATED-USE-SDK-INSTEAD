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
 * Instances of the class {@code HideCombinator} represent a combinator that restricts the names
 * being imported to those that are not in a given list.
 * 
 * <pre>
 * hideCombinator ::=
 *     'hide' {@link SimpleIdentifier identifier} (',' {@link SimpleIdentifier identifier})*
 * </pre>
 */
public class HideCombinator extends Combinator {
  /**
   * The list of names from the library that are hidden by this combinator.
   */
  private NodeList<SimpleIdentifier> hiddenNames = new NodeList<SimpleIdentifier>(this);

  /**
   * Initialize a newly created import show combinator.
   */
  public HideCombinator() {
    super();
  }

  /**
   * Initialize a newly created import show combinator.
   * 
   * @param keyword the comma introducing the combinator
   * @param hiddenNames the list of names from the library that are hidden by this combinator
   */
  public HideCombinator(Token keyword, List<SimpleIdentifier> hiddenNames) {
    super(keyword);
    this.hiddenNames.addAll(hiddenNames);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitHideCombinator(this);
  }

  @Override
  public Token getEndToken() {
    return hiddenNames.getEndToken();
  }

  /**
   * Return the list of names from the library that are hidden by this combinator.
   * 
   * @return the list of names from the library that are hidden by this combinator
   */
  public NodeList<SimpleIdentifier> getHiddenNames() {
    return hiddenNames;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    hiddenNames.accept(visitor);
  }
}

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
 * Instances of the class {@code ShowCombinator} represent a combinator that restricts the names
 * being imported to those in a given list.
 * 
 * <pre>
 * showCombinator ::=
 *     'show' {@link SimpleIdentifier identifier} (',' {@link SimpleIdentifier identifier})*
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ShowCombinator extends Combinator {
  /**
   * The list of names from the library that are made visible by this combinator.
   */
  private NodeList<SimpleIdentifier> shownNames = new NodeList<SimpleIdentifier>(this);

  /**
   * Initialize a newly created import show combinator.
   * 
   * @param keyword the comma introducing the combinator
   * @param shownNames the list of names from the library that are made visible by this combinator
   */
  public ShowCombinator(Token keyword, List<SimpleIdentifier> shownNames) {
    super(keyword);
    this.shownNames.addAll(shownNames);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitShowCombinator(this);
  }

  @Override
  public Token getEndToken() {
    return shownNames.getEndToken();
  }

  /**
   * Return the list of names from the library that are made visible by this combinator.
   * 
   * @return the list of names from the library that are made visible by this combinator
   */
  public NodeList<SimpleIdentifier> getShownNames() {
    return shownNames;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    shownNames.accept(visitor);
  }
}

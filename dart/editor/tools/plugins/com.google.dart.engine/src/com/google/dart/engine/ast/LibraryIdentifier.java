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
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code LibraryIdentifier} represent the identifier for a library.
 * 
 * <pre>
 * libraryIdentifier ::=
 *     {@link SimpleIdentifier component} ('.' {@link SimpleIdentifier component})*
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class LibraryIdentifier extends Identifier {
  /**
   * The components of the identifier.
   */
  private NodeList<SimpleIdentifier> components = new NodeList<SimpleIdentifier>(this);

  /**
   * Initialize a newly created prefixed identifier.
   * 
   * @param components the components of the identifier
   */
  public LibraryIdentifier(List<SimpleIdentifier> components) {
    this.components.addAll(components);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitLibraryIdentifier(this);
  }

  @Override
  public Token getBeginToken() {
    return components.getBeginToken();
  }

  @Override
  public Element getBestElement() {
    return getStaticElement();
  }

  /**
   * Return the components of the identifier.
   * 
   * @return the components of the identifier
   */
  public NodeList<SimpleIdentifier> getComponents() {
    return components;
  }

  @Override
  public Token getEndToken() {
    return components.getEndToken();
  }

  @Override
  public String getName() {
    StringBuilder builder = new StringBuilder();
    boolean needsPeriod = false;
    for (SimpleIdentifier identifier : components) {
      if (needsPeriod) {
        builder.append(".");
      } else {
        needsPeriod = true;
      }
      builder.append(identifier.getName());
    }
    return builder.toString();
  }

  @Override
  public int getPrecedence() {
    return 15;
  }

  @Override
  public Element getPropagatedElement() {
    return null;
  }

  @Override
  public Element getStaticElement() {
    return null;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    components.accept(visitor);
  }
}

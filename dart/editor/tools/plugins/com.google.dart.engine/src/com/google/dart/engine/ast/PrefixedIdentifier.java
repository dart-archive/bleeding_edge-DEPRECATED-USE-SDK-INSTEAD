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
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code PrefixedIdentifier} represent either an identifier that is prefixed
 * or an access to an object property where the target of the property access is a simple
 * identifier.
 * 
 * <pre>
 * prefixedIdentifier ::=
 *     {@link SimpleIdentifier prefix} '.' {@link SimpleIdentifier identifier}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class PrefixedIdentifier extends Identifier {
  /**
   * The prefix associated with the library in which the identifier is defined.
   */
  private SimpleIdentifier prefix;

  /**
   * The period used to separate the prefix from the identifier.
   */
  private Token period;

  /**
   * The identifier being prefixed.
   */
  private SimpleIdentifier identifier;

  /**
   * Initialize a newly created prefixed identifier.
   * 
   * @param prefix the identifier being prefixed
   * @param period the period used to separate the prefix from the identifier
   * @param identifier the prefix associated with the library in which the identifier is defined
   */
  public PrefixedIdentifier(SimpleIdentifier prefix, Token period, SimpleIdentifier identifier) {
    this.prefix = becomeParentOf(prefix);
    this.period = period;
    this.identifier = becomeParentOf(identifier);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitPrefixedIdentifier(this);
  }

  @Override
  public Token getBeginToken() {
    return prefix.getBeginToken();
  }

  @Override
  public Element getBestElement() {
    if (identifier == null) {
      return null;
    }
    return identifier.getBestElement();
  }

  @Override
  public Token getEndToken() {
    return identifier.getEndToken();
  }

  /**
   * Return the identifier being prefixed.
   * 
   * @return the identifier being prefixed
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  @Override
  public String getName() {
    return prefix.getName() + "." + identifier.getName();
  }

  /**
   * Return the period used to separate the prefix from the identifier.
   * 
   * @return the period used to separate the prefix from the identifier
   */
  public Token getPeriod() {
    return period;
  }

  @Override
  public int getPrecedence() {
    return 15;
  }

  /**
   * Return the prefix associated with the library in which the identifier is defined.
   * 
   * @return the prefix associated with the library in which the identifier is defined
   */
  public SimpleIdentifier getPrefix() {
    return prefix;
  }

  @Override
  public Element getPropagatedElement() {
    if (identifier == null) {
      return null;
    }
    return identifier.getPropagatedElement();
  }

  @Override
  public Element getStaticElement() {
    if (identifier == null) {
      return null;
    }
    return identifier.getStaticElement();
  }

  /**
   * Return {@code true} if this type is a deferred type.
   * <p>
   * 15.1 Static Types: A type <i>T</i> is deferred iff it is of the form </i>p.T</i> where <i>p</i>
   * is a deferred prefix.
   * 
   * @return {@code true} if this type is a deferred type
   */
  public boolean isDeferred() {
    Element element = prefix.getStaticElement();
    if (!(element instanceof PrefixElement)) {
      return false;
    }
    PrefixElement prefixElement = (PrefixElement) element;
    ImportElement[] imports = prefixElement.getEnclosingElement().getImportsWithPrefix(
        prefixElement);
    if (imports.length != 1) {
      return false;
    }
    return imports[0].isDeferred();
  }

  /**
   * Set the identifier being prefixed to the given identifier.
   * 
   * @param identifier the identifier being prefixed
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  /**
   * Set the period used to separate the prefix from the identifier to the given token.
   * 
   * @param period the period used to separate the prefix from the identifier
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  /**
   * Set the prefix associated with the library in which the identifier is defined to the given
   * identifier.
   * 
   * @param identifier the prefix associated with the library in which the identifier is defined
   */
  public void setPrefix(SimpleIdentifier identifier) {
    prefix = becomeParentOf(identifier);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(prefix, visitor);
    safelyVisitChild(identifier, visitor);
  }
}

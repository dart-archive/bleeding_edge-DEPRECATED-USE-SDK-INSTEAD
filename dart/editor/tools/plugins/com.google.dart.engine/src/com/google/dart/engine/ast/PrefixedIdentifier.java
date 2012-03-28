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
 * Instances of the class <code>PrefixedIdentifier</code> represent an identifier that is prefixed.
 * 
 * <pre>
 * prefixedIdentifier ::=
 *     {@link SimpleIdentifier prefix} '.' {@link SimpleIdentifier identifier}
 * </pre>
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
   */
  public PrefixedIdentifier() {
  }

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
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitPrefixedIdentifier(this);
  }

  @Override
  public Token getBeginToken() {
    return prefix.getBeginToken();
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

  /**
   * Return the period used to separate the prefix from the identifier.
   * 
   * @return the period used to separate the prefix from the identifier
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Return the prefix associated with the library in which the identifier is defined.
   * 
   * @return the prefix associated with the library in which the identifier is defined
   */
  public SimpleIdentifier getPrefix() {
    return prefix;
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
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(prefix, visitor);
    safelyVisitChild(identifier, visitor);
  }
}

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
 * Instances of the class <code>ImplementsClause</code> represent the "implements" clause in an
 * class declaration.
 * 
 * <pre>
 * implementsClause ::=
 *     'implements' {@link TypeName superclass} (',' {@link TypeName superclass})*
 * </pre>
 */
public class ImplementsClause extends ASTNode {
  /**
   * The token representing the 'implements' keyword.
   */
  private Token keyword;

  /**
   * The interfaces that are being implemented.
   */
  private NodeList<TypeName> interfaces = new NodeList<TypeName>(this);

  /**
   * Initialize a newly created extends clause.
   */
  public ImplementsClause() {
  }

  /**
   * Initialize a newly created extends clause.
   * 
   * @param keyword the token representing the 'implements' keyword
   * @param interfaces the interfaces that are being implemented
   */
  public ImplementsClause(Token keyword, List<TypeName> interfaces) {
    this.keyword = keyword;
    this.interfaces.addAll(interfaces);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitImplementsClause(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return interfaces.getEndToken();
  }

  /**
   * Return the list of the interfaces that are being implemented.
   * 
   * @return the list of the interfaces that are being implemented
   */
  public NodeList<TypeName> getInterfaces() {
    return interfaces;
  }

  /**
   * Return the token representing the 'implements' keyword.
   * 
   * @return the token representing the 'implements' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Set the token representing the 'implements' keyword to the given token.
   * 
   * @param keyword the token representing the 'implements' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    interfaces.accept(visitor);
  }
}

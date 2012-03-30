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
 * Instances of the class <code>InterfaceExtendsClause</code> represent the "extends" clause in an
 * interface declaration.
 * 
 * <pre>
 * interfaceExtendsClause ::=
 *     'extends' {@link TypeName superinterface} (',' {@link TypeName superinterface})*
 * </pre>
 */
public class InterfaceExtendsClause extends ASTNode {
  /**
   * The token representing the 'extends' keyword.
   */
  private Token keyword;

  /**
   * The interfaces that are being extended.
   */
  private NodeList<TypeName> interfaces = new NodeList<TypeName>(this);

  /**
   * Initialize a newly created extends clause.
   */
  public InterfaceExtendsClause() {
  }

  /**
   * Initialize a newly created extends clause.
   * 
   * @param keyword the token representing the 'extends' keyword
   * @param interfaces the interfaces that are being extended
   */
  public InterfaceExtendsClause(Token keyword, List<TypeName> interfaces) {
    this.keyword = keyword;
    this.interfaces.addAll(interfaces);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitInterfaceExtendsClause(this);
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
   * Return the list of the interfaces that are being extended.
   * 
   * @return the list of the interfaces that are being extended
   */
  public NodeList<TypeName> getInterfaces() {
    return interfaces;
  }

  /**
   * Return the token representing the 'extends' keyword.
   * 
   * @return the token representing the 'extends' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Set the token representing the 'extends' keyword to the given token.
   * 
   * @param keyword the token representing the 'extends' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    interfaces.accept(visitor);
  }
}

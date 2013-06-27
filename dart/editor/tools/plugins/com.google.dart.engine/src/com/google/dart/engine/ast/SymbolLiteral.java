/*
 * Copyright 2013, the Dart project authors.
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
 * Instances of the class {@code SymbolLiteral} represent a symbol literal expression.
 * 
 * <pre>
 * symbolLiteral ::=
 *     '#' {@link SimpleIdentifier identifier} ('.' {@link SimpleIdentifier identifier})*
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class SymbolLiteral extends Literal {
  /**
   * The token introducing the literal.
   */
  private Token poundSign;

  /**
   * The components of the literal.
   */
  private NodeList<SimpleIdentifier> components = new NodeList<SimpleIdentifier>(this);

  /**
   * Initialize a newly created symbol literal.
   * 
   * @param poundSign the token introducing the literal
   * @param components the components of the literal
   */
  public SymbolLiteral(Token poundSign, List<SimpleIdentifier> components) {
    this.poundSign = poundSign;
    this.components.addAll(components);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitSymbolLiteral(this);
  }

  @Override
  public Token getBeginToken() {
    return poundSign;
  }

  /**
   * Return the components of the literal.
   * 
   * @return the components of the literal
   */
  public NodeList<SimpleIdentifier> getComponents() {
    return components;
  }

  @Override
  public Token getEndToken() {
    return components.getEndToken();
  }

  /**
   * Return the token introducing the literal.
   * 
   * @return the token introducing the literal
   */
  public Token getPoundSign() {
    return poundSign;
  }

  /**
   * Set the token introducing the literal to the given token.
   * 
   * @param poundSign the token introducing the literal
   */
  public void setPoundSign(Token poundSign) {
    this.poundSign = poundSign;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    components.accept(visitor);
  }
}

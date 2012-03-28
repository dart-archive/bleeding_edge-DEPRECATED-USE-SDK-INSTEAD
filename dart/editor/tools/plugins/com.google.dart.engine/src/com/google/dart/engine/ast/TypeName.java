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
 * Instances of the class <code>TypeName</code> represent the name of a type, which can optionally
 * include type arguments.
 * 
 * <pre>
 * typeName ::=
 *     {@link Identifier identifier} ('<' typeName (',' typeName)* '>')?
 * </pre>
 */
public class TypeName extends ASTNode {
  /**
   * The name of the type.
   */
  private Identifier name;

  /**
   * The left bracket, or <code>null</code> if there are no type arguments.
   */
  private Token leftBracket;

  /**
   * The type arguments associated with the type.
   */
  private NodeList<TypeName> arguments = new NodeList<TypeName>(this);

  /**
   * The right bracket, or <code>null</code> if there are no type arguments.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created type name.
   */
  public TypeName() {
  }

  /**
   * Initialize a newly created type name.
   * 
   * @param name the name of the type
   * @param leftBracket the left bracket, or <code>null</code> if there are no type arguments
   * @param arguments the type arguments associated with the type
   * @param rightBracket the right bracket, or <code>null</code> if there are no type arguments
   */
  public TypeName(Identifier name, Token leftBracket, List<TypeName> arguments, Token rightBracket) {
    this.name = becomeParentOf(name);
    this.leftBracket = leftBracket;
    this.arguments.addAll(arguments);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitTypeName(this);
  }

  /**
   * Return the type arguments associated with the type.
   * 
   * @return the type arguments associated with the type
   */
  public NodeList<TypeName> getArguments() {
    return arguments;
  }

  @Override
  public Token getBeginToken() {
    return name.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    if (rightBracket != null) {
      return rightBracket;
    }
    return name.getEndToken();
  }

  /**
   * Return the left bracket, or <code>null</code> if there are no type arguments.
   * 
   * @return the left bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the name of the type.
   * 
   * @return the name of the type
   */
  public Identifier getName() {
    return name;
  }

  /**
   * Return the right bracket, or <code>null</code> if there are no type arguments.
   * 
   * @return the right bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Set the left bracket to the given token.
   * 
   * @param bracket the left bracket
   */
  public void setLeftBracket(Token bracket) {
    leftBracket = bracket;
  }

  /**
   * Set the name of the type to the given identifier.
   * 
   * @param identifier the name of the type
   */
  public void setName(Identifier identifier) {
    name = becomeParentOf(identifier);
  }

  /**
   * Set the right bracket to the given token.
   * 
   * @param bracket the right bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(name, visitor);
    arguments.accept(visitor);
  }
}

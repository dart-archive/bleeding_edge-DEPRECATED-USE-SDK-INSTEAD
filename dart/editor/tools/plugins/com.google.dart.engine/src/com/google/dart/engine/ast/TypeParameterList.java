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
 * Instances of the class {@code TypeParameterList} represent type parameters within a declaration.
 *
 * <pre>
 * typeParameterList ::=
 *     '<' {@link TypeParameter typeParameter} (',' {@link TypeParameter typeParameter})* '>'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class TypeParameterList extends AstNode {
  /**
   * The left angle bracket.
   */
  private Token leftBracket;

  /**
   * The type parameters in the list.
   */
  private NodeList<TypeParameter> typeParameters = new NodeList<TypeParameter>(this);

  /**
   * The right angle bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created list of type parameters.
   * 
   * @param leftBracket the left angle bracket
   * @param typeParameters the type parameters in the list
   * @param rightBracket the right angle bracket
   */
  public TypeParameterList(Token leftBracket, List<TypeParameter> typeParameters, Token rightBracket) {
    this.leftBracket = leftBracket;
    this.typeParameters.addAll(typeParameters);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitTypeParameterList(this);
  }

  @Override
  public Token getBeginToken() {
    return leftBracket;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the left angle bracket.
   * 
   * @return the left angle bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the right angle bracket.
   * 
   * @return the right angle bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return the type parameters for the type.
   * 
   * @return the type parameters for the type
   */
  public NodeList<TypeParameter> getTypeParameters() {
    return typeParameters;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    typeParameters.accept(visitor);
  }
}

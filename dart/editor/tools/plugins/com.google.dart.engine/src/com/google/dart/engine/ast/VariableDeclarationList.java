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
 * Instances of the class {@code VariableDeclarationList} represent the declaration of one or more
 * variables of the same type.
 * 
 * <pre>
 * variableDeclarationList ::=
 *     finalConstVarOrType {@link VariableDeclaration variableDeclaration} (',' {@link VariableDeclaration variableDeclaration})*
 * 
 * finalConstVarOrType ::=
 *   | 'final' {@link TypeName type}?
 *   | 'const' {@link TypeName type}?
 *   | 'var'
 *   | {@link TypeName type}
 * </pre>
 */
public class VariableDeclarationList extends ASTNode {
  /**
   * The token representing the 'final', 'const' or 'var' keyword, or {@code null} if no keyword was
   * included.
   */
  private Token keyword;

  /**
   * The type of the variables being declared, or {@code null} if no type was provided.
   */
  private TypeName type;

  /**
   * A list containing the individual variables being declared.
   */
  private NodeList<VariableDeclaration> variables = new NodeList<VariableDeclaration>(this);

  /**
   * Initialize a newly created variable declaration list.
   */
  public VariableDeclarationList() {
  }

  /**
   * Initialize a newly created variable declaration list.
   * 
   * @param keyword the token representing the 'final', 'const' or 'var' keyword
   * @param type the type of the variables being declared
   * @param variables a list containing the individual variables being declared
   */
  public VariableDeclarationList(Token keyword, TypeName type, List<VariableDeclaration> variables) {
    this.keyword = keyword;
    this.type = becomeParentOf(type);
    this.variables.addAll(variables);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitVariableDeclarationList(this);
  }

  @Override
  public Token getBeginToken() {
    if (keyword != null) {
      return keyword;
    } else if (type != null) {
      return type.getBeginToken();
    }
    return variables.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return variables.getEndToken();
  }

  /**
   * Return the token representing the 'final', 'const' or 'var' keyword, or {@code null} if no
   * keyword was included.
   * 
   * @return the token representing the 'final', 'const' or 'var' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the type of the variables being declared, or {@code null} if no type was provided.
   * 
   * @return the type of the variables being declared
   */
  public TypeName getType() {
    return type;
  }

  /**
   * Return a list containing the individual variables being declared.
   * 
   * @return a list containing the individual variables being declared
   */
  public NodeList<VariableDeclaration> getVariables() {
    return variables;
  }

  /**
   * Set the token representing the 'final', 'const' or 'var' keyword to the given token.
   * 
   * @param keyword the token representing the 'final', 'const' or 'var' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the type of the variables being declared to the given type name.
   * 
   * @param typeName the type of the variables being declared
   */
  public void setType(TypeName typeName) {
    type = becomeParentOf(typeName);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(type, visitor);
    variables.accept(visitor);
  }
}

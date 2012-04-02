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
 * Instances of the class <code>VariableDeclarationList</code> represent the declaration of one or
 * more variables of the same type.
 * 
 * <pre>
 * variableDeclarationList ::=
 *     finalVarOrType {@link VariableDeclaration variableDeclaration} (',' {@link VariableDeclaration variableDeclaration})*
 * 
 * finalVarOrType ::=
 *   | 'final' {@link TypeName type}?
 *   | 'var'
 *   | {@link TypeName type}
 * </pre>
 */
public class VariableDeclarationList extends ASTNode {
  /**
   * The token representing the 'var' keyword, or <code>null</code> if the 'var' keyword was not
   * included.
   */
  private Token varKeyword;

  /**
   * The token representing the 'final' keyword, or <code>null</code> if the 'final' keyword was not
   * included.
   */
  private Token finalKeyword;

  /**
   * The type of the variables being declared, or <code>null</code> if no type was provided.
   */
  private TypeName type;

  /**
   * A list containing the individual variables being declared.
   */
  private NodeList<VariableDeclaration> variables = new NodeList<VariableDeclaration>(this);

  /**
   * Initialize a newly created field declaration.
   */
  public VariableDeclarationList() {
  }

  /**
   * Initialize a newly created field declaration.
   * 
   * @param varKeyword the token representing the 'var' keyword
   * @param finalKeyword the token representing the 'final' keyword
   * @param type the type of the variables being declared
   * @param fields a list containing the individual variables being declared
   */
  public VariableDeclarationList(Token varKeyword, Token finalKeyword, TypeName type,
      List<VariableDeclaration> fields) {
    this.varKeyword = varKeyword;
    this.finalKeyword = finalKeyword;
    this.type = becomeParentOf(type);
    this.variables.addAll(fields);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitVariableDeclarationList(this);
  }

  @Override
  public Token getBeginToken() {
    if (varKeyword != null) {
      return varKeyword;
    } else if (finalKeyword != null) {
      return finalKeyword;
    }
    return type.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    if (type != null) {
      return type.getEndToken();
    } else if (finalKeyword != null) {
      return finalKeyword;
    }
    return varKeyword;
  }

  /**
   * Return the token representing the 'final' keyword, or <code>null</code> if the 'final' keyword
   * was not included.
   * 
   * @return the token representing the 'final' keyword
   */
  public Token getFinalKeyword() {
    return finalKeyword;
  }

  /**
   * Return the type of the variables being declared, or <code>null</code> if no type was provided.
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
   * Return the token representing the 'var' keyword, or <code>null</code> if the 'var' keyword was
   * not included.
   * 
   * @return the token representing the 'var' keyword
   */
  public Token getVarKeyword() {
    return varKeyword;
  }

  /**
   * Set the token representing the 'final' keyword to the given token.
   * 
   * @param finalKeyword the token representing the 'final' keyword
   */
  public void setFinalKeyword(Token finalKeyword) {
    this.finalKeyword = finalKeyword;
  }

  /**
   * Set the type of the variables being declared to the given type name.
   * 
   * @param typeName the type of the variables being declared
   */
  public void setType(TypeName typeName) {
    type = becomeParentOf(typeName);
  }

  /**
   * Set the token representing the 'var' keyword to the given token.
   * 
   * @param varKeyword the token representing the 'var' keyword
   */
  public void setVarKeyword(Token varKeyword) {
    this.varKeyword = varKeyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(type, visitor);
    variables.accept(visitor);
  }
}

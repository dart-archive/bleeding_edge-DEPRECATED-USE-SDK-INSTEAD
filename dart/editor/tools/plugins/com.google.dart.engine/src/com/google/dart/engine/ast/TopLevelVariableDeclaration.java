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
 * Instances of the class {@code TopLevelVariableDeclaration} represent the declaration of one or
 * more top-level variables of the same type.
 * 
 * <pre>
 * topLevelVariableDeclaration ::=
 *     ('final' | 'const') type? staticFinalDeclarationList ';'
 *   | variableDeclaration ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class TopLevelVariableDeclaration extends CompilationUnitMember {
  /**
   * The top-level variables being declared.
   */
  private VariableDeclarationList variableList;

  /**
   * The semicolon terminating the declaration.
   */
  private Token semicolon;

  /**
   * Initialize a newly created top-level variable declaration.
   * 
   * @param comment the documentation comment associated with this variable
   * @param metadata the annotations associated with this variable
   * @param variableList the top-level variables being declared
   * @param semicolon the semicolon terminating the declaration
   */
  public TopLevelVariableDeclaration(Comment comment, List<Annotation> metadata,
      VariableDeclarationList variableList, Token semicolon) {
    super(comment, metadata);
    this.variableList = becomeParentOf(variableList);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitTopLevelVariableDeclaration(this);
  }

  @Override
  public Element getElement() {
    return null;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the semicolon terminating the declaration.
   * 
   * @return the semicolon terminating the declaration
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Return the top-level variables being declared.
   * 
   * @return the top-level variables being declared
   */
  public VariableDeclarationList getVariables() {
    return variableList;
  }

  /**
   * Set the semicolon terminating the declaration to the given token.
   * 
   * @param semicolon the semicolon terminating the declaration
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  /**
   * Set the top-level variables being declared to the given list of variables.
   * 
   * @param variableList the top-level variables being declared
   */
  public void setVariables(VariableDeclarationList variableList) {
    variableList = becomeParentOf(variableList);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(variableList, visitor);
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    return variableList.getBeginToken();
  }
}

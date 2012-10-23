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
 * Instances of the class {@code FieldDeclaration} represent the declaration of one or more fields
 * of the same type.
 * 
 * <pre>
 * fieldDeclaration ::=
 *     'static'? {@link VariableDeclarationList fieldList} ';'
 * </pre>
 */
public class FieldDeclaration extends ClassMember {
  /**
   * The token representing the 'static' keyword, or {@code null} if the fields are not static.
   */
  private Token keyword;

  /**
   * The fields being declared.
   */
  private VariableDeclarationList fieldList;

  /**
   * The semicolon terminating the declaration.
   */
  private Token semicolon;

  /**
   * Initialize a newly created field declaration.
   */
  public FieldDeclaration() {
  }

  /**
   * Initialize a newly created field declaration.
   * 
   * @param comment the documentation comment associated with this field
   * @param metadata the annotations associated with this field
   * @param keyword the token representing the 'static' keyword
   * @param fieldList the fields being declared
   * @param semicolon the semicolon terminating the declaration
   */
  public FieldDeclaration(Comment comment, List<Annotation> metadata, Token keyword,
      VariableDeclarationList fieldList, Token semicolon) {
    super(comment, metadata);
    this.keyword = keyword;
    this.fieldList = becomeParentOf(fieldList);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFieldDeclaration(this);
  }

  @Override
  public Token getBeginToken() {
    Comment comment = getDocumentationComment();
    if (comment != null) {
      return comment.getBeginToken();
    } else if (keyword != null) {
      return keyword;
    }
    return fieldList.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the fields being declared.
   * 
   * @return the fields being declared
   */
  public VariableDeclarationList getFields() {
    return fieldList;
  }

  /**
   * Return the token representing the 'static' keyword, or {@code null} if the fields are not
   * static.
   * 
   * @return the token representing the 'static' keyword
   */
  public Token getKeyword() {
    return keyword;
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
   * Set the fields being declared to the given list of variables.
   * 
   * @param fieldList the fields being declared
   */
  public void setFields(VariableDeclarationList fieldList) {
    fieldList = becomeParentOf(fieldList);
  }

  /**
   * Set the token representing the 'static' keyword to the given token.
   * 
   * @param keyword the token representing the 'static' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the semicolon terminating the declaration to the given token.
   * 
   * @param semicolon the semicolon terminating the declaration
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(fieldList, visitor);
  }
}

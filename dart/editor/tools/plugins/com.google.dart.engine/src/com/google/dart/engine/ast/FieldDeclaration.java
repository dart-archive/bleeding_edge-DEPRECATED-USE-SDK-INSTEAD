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
 * Instances of the class {@code FieldDeclaration} represent the declaration of one or more fields
 * of the same type.
 * 
 * <pre>
 * fieldDeclaration ::=
 *     'static'? {@link VariableDeclarationList fieldList} ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class FieldDeclaration extends ClassMember {
  /**
   * The token representing the 'static' keyword, or {@code null} if the fields are not static.
   */
  private Token staticKeyword;

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
   * 
   * @param comment the documentation comment associated with this field
   * @param metadata the annotations associated with this field
   * @param staticKeyword the token representing the 'static' keyword
   * @param fieldList the fields being declared
   * @param semicolon the semicolon terminating the declaration
   */
  public FieldDeclaration(Comment comment, List<Annotation> metadata, Token staticKeyword,
      VariableDeclarationList fieldList, Token semicolon) {
    super(comment, metadata);
    this.staticKeyword = staticKeyword;
    this.fieldList = becomeParentOf(fieldList);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitFieldDeclaration(this);
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
  public Token getStaticKeyword() {
    return staticKeyword;
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
   * Return {@code true} if the fields are static.
   * 
   * @return {@code true} if the fields are declared to be static
   */
  public boolean isStatic() {
    return staticKeyword != null;
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
  public void setStaticKeyword(Token keyword) {
    this.staticKeyword = keyword;
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
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(fieldList, visitor);
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    if (staticKeyword != null) {
      return staticKeyword;
    }
    return fieldList.getBeginToken();
  }
}

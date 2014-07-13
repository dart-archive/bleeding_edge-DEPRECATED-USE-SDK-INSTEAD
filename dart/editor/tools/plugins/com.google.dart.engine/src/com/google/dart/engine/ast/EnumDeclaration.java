/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code EnumDeclaration} represent the declaration of an enumeration.
 * 
 * <pre>
 * enumType ::=
 *     metadata 'enum' {@link SimpleIdentifier name} '{' {@link SimpleIdentifier constant} (',' {@link SimpleIdentifier constant})* (',')? '}'
 * </pre>
 */
public class EnumDeclaration extends CompilationUnitMember {
  /**
   * The 'enum' keyword.
   */
  private Token keyword;

  /**
   * The name of the enumeration.
   */
  private SimpleIdentifier name;

  /**
   * The left curly bracket.
   */
  private Token leftBracket;

  /**
   * The enumeration constants being declared.
   */
  private NodeList<EnumConstantDeclaration> constants = new NodeList<EnumConstantDeclaration>(this);

  /**
   * The right curly bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created enumeration declaration.
   * 
   * @param comment the documentation comment associated with this member
   * @param metadata the annotations associated with this member
   * @param keyword the 'enum' keyword
   * @param name the name of the enumeration
   * @param leftBracket the left curly bracket
   * @param constants the enumeration constants being declared
   * @param rightBracket the right curly bracket
   */
  public EnumDeclaration(Comment comment, List<Annotation> metadata, Token keyword,
      SimpleIdentifier name, Token leftBracket, List<EnumConstantDeclaration> constants,
      Token rightBracket) {
    super(comment, metadata);
    this.keyword = keyword;
    this.name = becomeParentOf(name);
    this.leftBracket = leftBracket;
    this.constants.addAll(constants);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitEnumDeclaration(this);
  }

  /**
   * Return the enumeration constants being declared.
   * 
   * @return the enumeration constants being declared
   */
  public NodeList<EnumConstantDeclaration> getConstants() {
    return constants;
  }

  @Override
  public ClassElement getElement() {
    return name != null ? (ClassElement) name.getStaticElement() : null;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the 'enum' keyword.
   * 
   * @return the 'enum' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the left curly bracket.
   * 
   * @return the left curly bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the name of the enumeration.
   * 
   * @return the name of the enumeration
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the right curly bracket.
   * 
   * @return the right curly bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Set the 'enum' keyword to the given token.
   * 
   * @param keyword the 'enum' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param leftBracket the left curly bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftBracket = leftBracket;
  }

  /**
   * set the name of the enumeration to the given identifier.
   * 
   * @param name the name of the enumeration
   */
  public void setName(SimpleIdentifier name) {
    this.name = becomeParentOf(name);
  }

  /**
   * Set the right curly bracket to the given token.
   * 
   * @param rightBracket the right curly bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightBracket = rightBracket;
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    return keyword;
  }
}

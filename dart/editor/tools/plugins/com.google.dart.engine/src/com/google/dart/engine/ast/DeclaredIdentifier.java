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

import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code DeclaredIdentifier} represent the declaration of a single
 * identifier.
 * 
 * <pre>
 * declaredIdentifier ::=
 *     ({@link Annotation metadata} finalConstVarOrType {@link SimpleIdentifier identifier}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class DeclaredIdentifier extends Declaration {
  /**
   * The token representing either the 'final', 'const' or 'var' keyword, or {@code null} if no
   * keyword was used.
   */
  private Token keyword;

  /**
   * The name of the declared type of the parameter, or {@code null} if the parameter does not have
   * a declared type.
   */
  private TypeName type;

  /**
   * The name of the variable being declared.
   */
  private SimpleIdentifier identifier;

  /**
   * Initialize a newly created formal parameter.
   * 
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param keyword the token representing either the 'final', 'const' or 'var' keyword
   * @param type the name of the declared type of the parameter
   * @param identifier the name of the parameter being declared
   */
  public DeclaredIdentifier(Comment comment, List<Annotation> metadata, Token keyword,
      TypeName type, SimpleIdentifier identifier) {
    super(comment, metadata);
    this.keyword = keyword;
    this.type = becomeParentOf(type);
    this.identifier = becomeParentOf(identifier);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitDeclaredIdentifier(this);
  }

  @Override
  public LocalVariableElement getElement() {
    SimpleIdentifier identifier = getIdentifier();
    if (identifier == null) {
      return null;
    }
    return (LocalVariableElement) identifier.getStaticElement();
  }

  @Override
  public Token getEndToken() {
    return identifier.getEndToken();
  }

  /**
   * Return the name of the variable being declared.
   * 
   * @return the name of the variable being declared
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * Return the token representing either the 'final', 'const' or 'var' keyword, or {@code null} if
   * no keyword was used.
   * 
   * @return the token representing either the 'final', 'const' or 'var' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the name of the declared type of the parameter, or {@code null} if the parameter does
   * not have a declared type.
   * 
   * @return the name of the declared type of the parameter
   */
  public TypeName getType() {
    return type;
  }

  /**
   * Return {@code true} if this variable was declared with the 'const' modifier.
   * 
   * @return {@code true} if this variable was declared with the 'const' modifier
   */
  public boolean isConst() {
    return (keyword instanceof KeywordToken)
        && ((KeywordToken) keyword).getKeyword() == Keyword.CONST;
  }

  /**
   * Return {@code true} if this variable was declared with the 'final' modifier. Variables that are
   * declared with the 'const' modifier will return {@code false} even though they are implicitly
   * final.
   * 
   * @return {@code true} if this variable was declared with the 'final' modifier
   */
  public boolean isFinal() {
    return (keyword instanceof KeywordToken)
        && ((KeywordToken) keyword).getKeyword() == Keyword.FINAL;
  }

  /**
   * Set the token representing either the 'final', 'const' or 'var' keyword to the given token.
   * 
   * @param keyword the token representing either the 'final', 'const' or 'var' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the name of the declared type of the parameter to the given type name.
   * 
   * @param typeName the name of the declared type of the parameter
   */
  public void setType(TypeName typeName) {
    type = becomeParentOf(typeName);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(type, visitor);
    safelyVisitChild(identifier, visitor);
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    if (keyword != null) {
      return keyword;
    } else if (type != null) {
      return type.getBeginToken();
    }
    return identifier.getBeginToken();
  }
}

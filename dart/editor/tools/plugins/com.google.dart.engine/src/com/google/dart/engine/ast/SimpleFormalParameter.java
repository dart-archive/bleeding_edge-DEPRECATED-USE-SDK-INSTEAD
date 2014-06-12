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

import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code SimpleFormalParameter} represent a simple formal parameter.
 * 
 * <pre>
 * simpleFormalParameter ::=
 *     ('final' {@link TypeName type} | 'var' | {@link TypeName type})? {@link SimpleIdentifier identifier}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class SimpleFormalParameter extends NormalFormalParameter {
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
   * Initialize a newly created formal parameter.
   * 
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param keyword the token representing either the 'final', 'const' or 'var' keyword
   * @param type the name of the declared type of the parameter
   * @param identifier the name of the parameter being declared
   */
  public SimpleFormalParameter(Comment comment, List<Annotation> metadata, Token keyword,
      TypeName type, SimpleIdentifier identifier) {
    super(comment, metadata, identifier);
    this.keyword = keyword;
    this.type = becomeParentOf(type);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitSimpleFormalParameter(this);
  }

  @Override
  public Token getBeginToken() {
    NodeList<Annotation> metadata = getMetadata();
    if (!metadata.isEmpty()) {
      return metadata.getBeginToken();
    } else if (keyword != null) {
      return keyword;
    } else if (type != null) {
      return type.getBeginToken();
    }
    return getIdentifier().getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return getIdentifier().getEndToken();
  }

  /**
   * Return the token representing either the 'final', 'const' or 'var' keyword.
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

  @Override
  public boolean isConst() {
    return (keyword instanceof KeywordToken)
        && ((KeywordToken) keyword).getKeyword() == Keyword.CONST;
  }

  @Override
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
    safelyVisitChild(getIdentifier(), visitor);
  }
}

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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code ClassTypeAlias} represent a class type alias.
 * 
 * <pre>
 * classTypeAlias ::=
 *     {@link SimpleIdentifier identifier} {@link TypeParameterList typeParameters}? '=' 'abstract'? mixinApplication
 * 
 * mixinApplication ::=
 *     {@link TypeName superclass} {@link WithClause withClause} {@link ImplementsClause implementsClause}? ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ClassTypeAlias extends TypeAlias {
  /**
   * The name of the class being declared.
   */
  private SimpleIdentifier name;

  /**
   * The type parameters for the class, or {@code null} if the class does not have any type
   * parameters.
   */
  private TypeParameterList typeParameters;

  /**
   * The token for the '=' separating the name from the definition.
   */
  private Token equals;

  /**
   * The token for the 'abstract' keyword, or {@code null} if this is not defining an abstract
   * class.
   */
  private Token abstractKeyword;

  /**
   * The name of the superclass of the class being declared.
   */
  private TypeName superclass;

  /**
   * The with clause for this class.
   */
  private WithClause withClause;

  /**
   * The implements clause for this class, or {@code null} if there is no implements clause.
   */
  private ImplementsClause implementsClause;

  /**
   * Initialize a newly created class type alias.
   * 
   * @param comment the documentation comment associated with this type alias
   * @param metadata the annotations associated with this type alias
   * @param keyword the token representing the 'typedef' keyword
   * @param name the name of the class being declared
   * @param typeParameters the type parameters for the class
   * @param equals the token for the '=' separating the name from the definition
   * @param abstractKeyword the token for the 'abstract' keyword
   * @param superclass the name of the superclass of the class being declared
   * @param withClause the with clause for this class
   * @param implementsClause the implements clause for this class
   * @param semicolon the semicolon terminating the declaration
   */
  public ClassTypeAlias(Comment comment, List<Annotation> metadata, Token keyword,
      SimpleIdentifier name, TypeParameterList typeParameters, Token equals, Token abstractKeyword,
      TypeName superclass, WithClause withClause, ImplementsClause implementsClause, Token semicolon) {
    super(comment, metadata, keyword, semicolon);
    this.name = becomeParentOf(name);
    this.typeParameters = becomeParentOf(typeParameters);
    this.equals = equals;
    this.abstractKeyword = abstractKeyword;
    this.superclass = becomeParentOf(superclass);
    this.withClause = becomeParentOf(withClause);
    this.implementsClause = becomeParentOf(implementsClause);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitClassTypeAlias(this);
  }

  /**
   * Return the token for the 'abstract' keyword, or {@code null} if this is not defining an
   * abstract class.
   * 
   * @return the token for the 'abstract' keyword
   */
  public Token getAbstractKeyword() {
    return abstractKeyword;
  }

  @Override
  public ClassElement getElement() {
    return name != null ? (ClassElement) name.getStaticElement() : null;
  }

  /**
   * Return the token for the '=' separating the name from the definition.
   * 
   * @return the token for the '=' separating the name from the definition
   */
  public Token getEquals() {
    return equals;
  }

  /**
   * Return the implements clause for this class, or {@code null} if there is no implements clause.
   * 
   * @return the implements clause for this class
   */
  public ImplementsClause getImplementsClause() {
    return implementsClause;
  }

  /**
   * Return the name of the class being declared.
   * 
   * @return the name of the class being declared
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the name of the superclass of the class being declared.
   * 
   * @return the name of the superclass of the class being declared
   */
  public TypeName getSuperclass() {
    return superclass;
  }

  /**
   * Return the type parameters for the class, or {@code null} if the class does not have any type
   * parameters.
   * 
   * @return the type parameters for the class
   */
  public TypeParameterList getTypeParameters() {
    return typeParameters;
  }

  /**
   * Return the with clause for this class.
   * 
   * @return the with clause for this class
   */
  public WithClause getWithClause() {
    return withClause;
  }

  /**
   * Return {@code true} if this class is declared to be an abstract class.
   * 
   * @return {@code true} if this class is declared to be an abstract class
   */
  public boolean isAbstract() {
    return abstractKeyword != null;
  }

  /**
   * Set the token for the 'abstract' keyword to the given token.
   * 
   * @param abstractKeyword the token for the 'abstract' keyword
   */
  public void setAbstractKeyword(Token abstractKeyword) {
    this.abstractKeyword = abstractKeyword;
  }

  /**
   * Set the token for the '=' separating the name from the definition to the given token.
   * 
   * @param equals the token for the '=' separating the name from the definition
   */
  public void setEquals(Token equals) {
    this.equals = equals;
  }

  /**
   * Set the implements clause for this class to the given implements clause.
   * 
   * @param implementsClause the implements clause for this class
   */
  public void setImplementsClause(ImplementsClause implementsClause) {
    this.implementsClause = becomeParentOf(implementsClause);
  }

  /**
   * Set the name of the class being declared to the given identifier.
   * 
   * @param name the name of the class being declared
   */
  public void setName(SimpleIdentifier name) {
    this.name = becomeParentOf(name);
  }

  /**
   * Set the name of the superclass of the class being declared to the given name.
   * 
   * @param superclass the name of the superclass of the class being declared
   */
  public void setSuperclass(TypeName superclass) {
    this.superclass = becomeParentOf(superclass);
  }

  /**
   * Set the type parameters for the class to the given list of parameters.
   * 
   * @param typeParameters the type parameters for the class
   */
  public void setTypeParameters(TypeParameterList typeParameters) {
    this.typeParameters = becomeParentOf(typeParameters);
  }

  /**
   * Set the with clause for this class to the given with clause.
   * 
   * @param withClause the with clause for this class
   */
  public void setWithClause(WithClause withClause) {
    this.withClause = becomeParentOf(withClause);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(typeParameters, visitor);
    safelyVisitChild(superclass, visitor);
    safelyVisitChild(withClause, visitor);
    safelyVisitChild(implementsClause, visitor);
  }
}

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
 * Instances of the class <code>ClassDeclaration</code> represent the declaration of a class.
 * 
 * <pre>
 * classDeclaration ::=
 *     'class' {@link SimpleIdentifier name} {@link TypeParameterList typeParameterList}?
 *     {@link ClassExtendsClause classExtendsClause}?
 *     {@link ImplementsClause implementsClause}?
 *     '{' classMemberDefinition* '}'
 * </pre>
 */
public class ClassDeclaration extends TypeDeclaration {
  /**
   * The extends clause for the class, or <code>null</code> if the class does not extend any other
   * class.
   */
  private ClassExtendsClause classExtendsClause;

  /**
   * The implements clause for the class, or <code>null</code> if the class does not implement any
   * interfaces.
   */
  private ImplementsClause implementsClause;

  /**
   * Initialize a newly created class declaration.
   */
  public ClassDeclaration() {
  }

  /**
   * Initialize a newly created class declaration.
   * 
   * @param comment the documentation comment associated with this member
   * @param keyword the token representing the 'class' keyword
   * @param name the name of the class being declared
   * @param typeParameters the type parameters for the class
   * @param classExtendsClause the extends clause for the class
   * @param implementsClause the implements clause for the class
   * @param leftBracket the left curly bracket
   * @param members the members defined by the class
   * @param rightBracket the right curly bracket
   */
  public ClassDeclaration(Comment comment, Token keyword, SimpleIdentifier name,
      TypeParameterList typeParameters, ClassExtendsClause classExtendsClause,
      ImplementsClause implementsClause, Token leftBracket, List<TypeMember> members,
      Token rightBracket) {
    super(comment, keyword, name, typeParameters, leftBracket, members, rightBracket);
    this.classExtendsClause = becomeParentOf(classExtendsClause);
    this.implementsClause = becomeParentOf(implementsClause);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitClassDeclaration(this);
  }

  /**
   * Return the extends clause for this class, or <code>null</code> if the class does not extend any
   * other class.
   * 
   * @return the extends clause for this class
   */
  public ClassExtendsClause getExtendsClause() {
    return classExtendsClause;
  }

  /**
   * Return the implements clause for the class, or <code>null</code> if the class does not
   * implement any interfaces.
   * 
   * @return the implements clause for the class
   */
  public ImplementsClause getImplementsClause() {
    return implementsClause;
  }

  /**
   * Set the extends clause for this class to the given clause.
   * 
   * @param clause the extends clause for this class
   */
  public void setExtendsClause(ClassExtendsClause clause) {
    classExtendsClause = becomeParentOf(clause);
  }

  /**
   * Set the implements clause for the class to the given clause.
   * 
   * @param clause the implements clause for the class
   */
  public void setImplementsClause(ImplementsClause clause) {
    implementsClause = becomeParentOf(clause);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(getDocumentationComment(), visitor);
    safelyVisitChild(getName(), visitor);
    safelyVisitChild(getTypeParameters(), visitor);
    safelyVisitChild(classExtendsClause, visitor);
    safelyVisitChild(implementsClause, visitor);
    getMembers().accept(visitor);
  }
}

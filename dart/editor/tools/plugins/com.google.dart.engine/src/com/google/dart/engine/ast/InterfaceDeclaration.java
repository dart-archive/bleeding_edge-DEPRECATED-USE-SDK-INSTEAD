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
 * Instances of the class <code>InterfaceDeclaration</code> represent the declaration of an
 * interface.
 *
 * <pre>
 * interfaceDeclaration ::=
 *     'interface' {@link SimpleIdentifier name} typeParameters?
 *     {@link InterfaceExtendsClause interfaceExtendsClause}?
 *     {@link DefaultClause defaultClause}?
 *     '{' (interfaceMemberDefinition)* '}'
 *
 * typeParameters ::=
 *     '<' {@link TypeParameter typeParameter} (',' {@link TypeParameter typeParameter})* '>'
 * </pre>
 */
public class InterfaceDeclaration extends TypeDeclaration {
  /**
   * The extends clause for the interface, or <code>null</code> if the interface does not extend any
   * other interfaces.
   */
  private InterfaceExtendsClause interfaceExtendsClause;

  /**
   * The default clause for the interface, or <code>null</code> if the interface does not define a
   * default class.
   */
  private DefaultClause defaultClause;

  /**
   * Initialize a newly created type declaration to represent an interface declaration.
   */
  public InterfaceDeclaration() {
  }

  /**
   * Initialize a newly created type declaration to represent an interface declaration.
   * 
   * @param comment the documentation comment associated with this member
   * @param keyword the token representing the 'interface' keyword
   * @param name the name of the interface being declared
   * @param typeParameters the type parameters for the interface
   * @param interfaceExtendsClause the extends clause for the interface
   * @param factoryClause the factory clause for the interface
   * @param members the members defined by the interface
   */
  public InterfaceDeclaration(Comment comment, Token keyword, SimpleIdentifier name,
      Token leftAngleBracket, List<TypeParameter> typeParameters, Token rightAngleBracket,
      InterfaceExtendsClause interfaceExtendsClause, DefaultClause factoryClause,
      Token leftBracket, List<TypeMember> members, Token rightBracket) {
    super(comment, keyword, name, leftAngleBracket, typeParameters, rightAngleBracket, leftBracket,
        members, rightBracket);
    this.interfaceExtendsClause = becomeParentOf(interfaceExtendsClause);
    this.defaultClause = becomeParentOf(factoryClause);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitInterfaceDeclaration(this);
  }

  /**
   * Return the default clause for the interface, or <code>null</code> if the interface does not
   * define a default class.
   * 
   * @return the default clause for the interface
   */
  public DefaultClause getDefaultClause() {
    return defaultClause;
  }

  /**
   * Return the extends clause for the interface, or <code>null</code> if the interface does not
   * extend any other interfaces.
   * 
   * @return the extends clause for this interface
   */
  public InterfaceExtendsClause getExtendsClause() {
    return interfaceExtendsClause;
  }

  /**
   * Set the default clause for the interface to the given clause.
   * 
   * @param clause the default clause for the interface
   */
  public void setDefaultClause(DefaultClause clause) {
    defaultClause = becomeParentOf(clause);
  }

  /**
   * Set the extends clause for the interface to the given clause.
   * 
   * @param clause the extends clause for this interface
   */
  public void setExtendsClause(InterfaceExtendsClause clause) {
    interfaceExtendsClause = becomeParentOf(clause);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(getDocumentationComment(), visitor);
    safelyVisitChild(getName(), visitor);
    getTypeParameters().accept(visitor);
    safelyVisitChild(interfaceExtendsClause, visitor);
    safelyVisitChild(defaultClause, visitor);
    getMembers().accept(visitor);
  }
}

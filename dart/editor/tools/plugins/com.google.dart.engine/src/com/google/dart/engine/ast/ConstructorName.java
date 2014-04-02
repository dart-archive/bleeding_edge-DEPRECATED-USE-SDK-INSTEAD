/*
 * Copyright (c) 2012, the Dart project authors.
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

import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code ConstructorName} represent the name of the constructor.
 * 
 * <pre>
 * constructorName:
 *     type ('.' identifier)?
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ConstructorName extends AstNode {
  /**
   * The name of the type defining the constructor.
   */
  private TypeName type;

  /**
   * The token for the period before the constructor name, or {@code null} if the specified
   * constructor is the unnamed constructor.
   */
  private Token period;

  /**
   * The name of the constructor, or {@code null} if the specified constructor is the unnamed
   * constructor.
   */
  private SimpleIdentifier name;

  /**
   * The element associated with this constructor name based on static type information, or
   * {@code null} if the AST structure has not been resolved or if this constructor name could not
   * be resolved.
   */
  private ConstructorElement staticElement;

  /**
   * Initialize a newly created constructor name.
   * 
   * @param type the name of the type defining the constructor
   * @param period the token for the period before the constructor name
   * @param name the name of the constructor
   */
  public ConstructorName(TypeName type, Token period, SimpleIdentifier name) {
    this.type = becomeParentOf(type);
    this.period = period;
    this.name = becomeParentOf(name);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitConstructorName(this);
  }

  @Override
  public Token getBeginToken() {
    return type.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    if (name != null) {
      return name.getEndToken();
    }
    return type.getEndToken();
  }

  /**
   * Return the name of the constructor, or {@code null} if the specified constructor is the unnamed
   * constructor.
   * 
   * @return the name of the constructor
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the token for the period before the constructor name, or {@code null} if the specified
   * constructor is the unnamed constructor.
   * 
   * @return the token for the period before the constructor name
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Return the element associated with this constructor name based on static type information, or
   * {@code null} if the AST structure has not been resolved or if this constructor name could not
   * be resolved.
   * 
   * @return the element associated with this constructor name
   */
  public ConstructorElement getStaticElement() {
    return staticElement;
  }

  /**
   * Return the name of the type defining the constructor.
   * 
   * @return the name of the type defining the constructor
   */
  public TypeName getType() {
    return type;
  }

  /**
   * Set the name of the constructor to the given name.
   * 
   * @param name the name of the constructor
   */
  public void setName(SimpleIdentifier name) {
    this.name = becomeParentOf(name);
  }

  /**
   * Return the token for the period before the constructor name to the given token.
   * 
   * @param period the token for the period before the constructor name
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  /**
   * Set the element associated with this constructor name based on static type information to the
   * given element.
   * 
   * @param element the element to be associated with this constructor name
   */
  public void setStaticElement(ConstructorElement element) {
    staticElement = element;
  }

  /**
   * Set the name of the type defining the constructor to the given type name.
   * 
   * @param type the name of the type defining the constructor
   */
  public void setType(TypeName type) {
    this.type = becomeParentOf(type);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(type, visitor);
    safelyVisitChild(name, visitor);
  }
}

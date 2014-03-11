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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code Annotation} represent an annotation that can be associated with an
 * AST node.
 * 
 * <pre>
 * metadata ::=
 *     annotation*
 * 
 * annotation ::=
 *     '@' {@link Identifier qualified} ('.' {@link SimpleIdentifier identifier})? {@link ArgumentList arguments}?
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class Annotation extends AstNode {
  /**
   * The at sign that introduced the annotation.
   */
  private Token atSign;

  /**
   * The name of the class defining the constructor that is being invoked or the name of the field
   * that is being referenced.
   */
  private Identifier name;

  /**
   * The period before the constructor name, or {@code null} if this annotation is not the
   * invocation of a named constructor.
   */
  private Token period;

  /**
   * The name of the constructor being invoked, or {@code null} if this annotation is not the
   * invocation of a named constructor.
   */
  private SimpleIdentifier constructorName;

  /**
   * The arguments to the constructor being invoked, or {@code null} if this annotation is not the
   * invocation of a constructor.
   */
  private ArgumentList arguments;

  /**
   * The element associated with this annotation, or {@code null} if the AST structure has not been
   * resolved or if this annotation could not be resolved.
   */
  private Element element;

  /**
   * The element annotation representing this annotation in the element model.
   */
  private ElementAnnotation elementAnnotation;

  /**
   * Initialize a newly created annotation.
   * 
   * @param atSign the at sign that introduced the annotation
   * @param name the name of the class defining the constructor that is being invoked or the name of
   *          the field that is being referenced
   * @param period the period before the constructor name, or {@code null} if this annotation is not
   *          the invocation of a named constructor
   * @param constructorName the name of the constructor being invoked, or {@code null} if this
   *          annotation is not the invocation of a named constructor
   * @param arguments the arguments to the constructor being invoked, or {@code null} if this
   *          annotation is not the invocation of a constructor
   */
  public Annotation(Token atSign, Identifier name, Token period, SimpleIdentifier constructorName,
      ArgumentList arguments) {
    this.atSign = atSign;
    this.name = becomeParentOf(name);
    this.period = period;
    this.constructorName = becomeParentOf(constructorName);
    this.arguments = becomeParentOf(arguments);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitAnnotation(this);
  }

  /**
   * Return the arguments to the constructor being invoked, or {@code null} if this annotation is
   * not the invocation of a constructor.
   * 
   * @return the arguments to the constructor being invoked
   */
  public ArgumentList getArguments() {
    return arguments;
  }

  /**
   * Return the at sign that introduced the annotation.
   * 
   * @return the at sign that introduced the annotation
   */
  public Token getAtSign() {
    return atSign;
  }

  @Override
  public Token getBeginToken() {
    return atSign;
  }

  /**
   * Return the name of the constructor being invoked, or {@code null} if this annotation is not the
   * invocation of a named constructor.
   * 
   * @return the name of the constructor being invoked
   */
  public SimpleIdentifier getConstructorName() {
    return constructorName;
  }

  /**
   * Return the element associated with this annotation, or {@code null} if the AST structure has
   * not been resolved or if this annotation could not be resolved.
   * 
   * @return the element associated with this annotation
   */
  public Element getElement() {
    if (element != null) {
      return element;
    }
    if (name != null) {
      return name.getStaticElement();
    }
    return null;
  }

  /**
   * Return the element annotation representing this annotation in the element model.
   * 
   * @return the element annotation representing this annotation in the element model
   */
  public ElementAnnotation getElementAnnotation() {
    return elementAnnotation;
  }

  @Override
  public Token getEndToken() {
    if (arguments != null) {
      return arguments.getEndToken();
    } else if (constructorName != null) {
      return constructorName.getEndToken();
    }
    return name.getEndToken();
  }

  /**
   * Return the name of the class defining the constructor that is being invoked or the name of the
   * field that is being referenced.
   * 
   * @return the name of the constructor being invoked or the name of the field being referenced
   */
  public Identifier getName() {
    return name;
  }

  /**
   * Return the period before the constructor name, or {@code null} if this annotation is not the
   * invocation of a named constructor.
   * 
   * @return the period before the constructor name
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Set the arguments to the constructor being invoked to the given arguments.
   * 
   * @param arguments the arguments to the constructor being invoked
   */
  public void setArguments(ArgumentList arguments) {
    this.arguments = becomeParentOf(arguments);
  }

  /**
   * Set the at sign that introduced the annotation to the given token.
   * 
   * @param atSign the at sign that introduced the annotation
   */
  public void setAtSign(Token atSign) {
    this.atSign = atSign;
  }

  /**
   * Set the name of the constructor being invoked to the given name.
   * 
   * @param constructorName the name of the constructor being invoked
   */
  public void setConstructorName(SimpleIdentifier constructorName) {
    this.constructorName = becomeParentOf(constructorName);
  }

  /**
   * Set the element associated with this annotation based.
   * 
   * @param element the element to be associated with this identifier
   */
  public void setElement(Element element) {
    this.element = element;
  }

  /**
   * Set the element annotation representing this annotation in the element model to the given
   * element annotation.
   * 
   * @param elementAnnotation the element annotation representing this annotation in the element
   *          model
   */
  public void setElementAnnotation(ElementAnnotation elementAnnotation) {
    this.elementAnnotation = elementAnnotation;
  }

  /**
   * Set the name of the class defining the constructor that is being invoked or the name of the
   * field that is being referenced to the given name.
   * 
   * @param name the name of the constructor being invoked or the name of the field being referenced
   */
  public void setName(Identifier name) {
    this.name = becomeParentOf(name);
  }

  /**
   * Set the period before the constructor name to the given token.
   * 
   * @param period the period before the constructor name
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(name, visitor);
    safelyVisitChild(constructorName, visitor);
    safelyVisitChild(arguments, visitor);
  }
}

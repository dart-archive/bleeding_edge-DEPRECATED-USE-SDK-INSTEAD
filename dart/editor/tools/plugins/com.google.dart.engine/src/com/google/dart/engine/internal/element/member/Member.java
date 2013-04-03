/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.element.member;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Annotation;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * The abstract class {@code Member} defines the behavior common to elements that represent members
 * of parameterized types.
 */
public abstract class Member implements Element {
  /**
   * The element on which the parameterized element was created.
   */
  private Element baseElement;

  /**
   * The type in which the element is defined.
   */
  private InterfaceType definingType;

  /**
   * Initialize a newly created element to represent the member of the given parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public Member(Element baseElement, InterfaceType definingType) {
    this.baseElement = baseElement;
    this.definingType = definingType;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
//    return getBaseElement().accept(visitor);
  }

  @Override
  public <E extends Element> E getAncestor(Class<E> elementClass) {
    return getBaseElement().getAncestor(elementClass);
  }

  /**
   * Return the element on which the parameterized element was created.
   * 
   * @return the element on which the parameterized element was created
   */
  public Element getBaseElement() {
    return baseElement;
  }

  @Override
  public AnalysisContext getContext() {
    return baseElement.getContext();
  }

  @Override
  public ElementKind getKind() {
    return baseElement.getKind();
  }

  @Override
  public LibraryElement getLibrary() {
    return baseElement.getLibrary();
  }

  @Override
  public ElementLocation getLocation() {
    return baseElement.getLocation();
  }

  @Override
  public Annotation[] getMetadata() {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
//    return baseElement.getMetadata();
  }

  @Override
  public String getName() {
    return baseElement.getName();
  }

  @Override
  public int getNameOffset() {
    return baseElement.getNameOffset();
  }

  @Override
  public Source getSource() {
    return baseElement.getSource();
  }

  @Override
  public boolean isAccessibleIn(LibraryElement library) {
    return baseElement.isAccessibleIn(library);
  }

  @Override
  public boolean isSynthetic() {
    return baseElement.isSynthetic();
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
//    getBaseElement().visitChildren(visitor);
  }

  /**
   * Return the type in which the element is defined.
   * 
   * @return the type in which the element is defined
   */
  protected InterfaceType getDefiningType() {
    return definingType;
  }

  /**
   * Return the type that results from replacing the type parameters in the given type with the type
   * arguments.
   * 
   * @param type the type to be transformed
   * @return the result of transforming the type
   */
  @SuppressWarnings("unchecked")
  protected <E extends Type> E substituteFor(E type) {
    Type[] argumentTypes = definingType.getTypeArguments();
    Type[] parameterTypes = TypeVariableTypeImpl.getTypes(definingType.getElement().getTypeVariables());
    return (E) type.substitute(argumentTypes, parameterTypes);
  }

  /**
   * Return the array of types that results from replacing the type parameters in the given types
   * with the type arguments.
   * 
   * @param types the types to be transformed
   * @return the result of transforming the types
   */
  protected InterfaceType[] substituteFor(InterfaceType[] types) {
    int count = types.length;
    InterfaceType[] substitutedTypes = new InterfaceType[count];
    for (int i = 0; i < count; i++) {
      substitutedTypes[i] = substituteFor(types[i]);
    }
    return substitutedTypes;
  }
}

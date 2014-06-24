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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.ParameterizedType;
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
  private ParameterizedType definingType;

  /**
   * Initialize a newly created element to represent the member of the given parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public Member(Element baseElement, ParameterizedType definingType) {
    this.baseElement = baseElement;
    this.definingType = definingType;
  }

  @Override
  public String computeDocumentationComment() throws AnalysisException {
    return baseElement.computeDocumentationComment();
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
  public String getDisplayName() {
    return baseElement.getDisplayName();
  }

  @Override
  public String getExtendedDisplayName(String shortName) {
    return baseElement.getExtendedDisplayName(shortName);
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
  public ElementAnnotation[] getMetadata() {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    // TODO(brianwilkerson) Figure out whether this is actually useful for annotations and implement
    // this method correctly if it is.
    //
    return baseElement.getMetadata();
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
  public AstNode getNode() throws AnalysisException {
    return baseElement.getNode();
  }

  @Override
  public Source getSource() {
    return baseElement.getSource();
  }

  @Override
  public CompilationUnit getUnit() throws AnalysisException {
    return baseElement.getUnit();
  }

  @Override
  public boolean isAccessibleIn(LibraryElement library) {
    return baseElement.isAccessibleIn(library);
  }

  @Override
  public boolean isDeprecated() {
    return baseElement.isDeprecated();
  }

  @Override
  public boolean isOverride() {
    return baseElement.isOverride();
  }

  @Override
  public boolean isPrivate() {
    return baseElement.isPrivate();
  }

  @Override
  public boolean isPublic() {
    return baseElement.isPublic();
  }

  @Override
  public boolean isSynthetic() {
    return baseElement.isSynthetic();
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    // There are no children to visit
  }

  /**
   * Return the type in which the element is defined.
   * 
   * @return the type in which the element is defined
   */
  protected ParameterizedType getDefiningType() {
    return definingType;
  }

  /**
   * If the given child is not {@code null}, use the given visitor to visit it.
   * 
   * @param child the child to be visited
   * @param visitor the visitor to be used to visit the child
   */
  protected void safelyVisitChild(Element child, ElementVisitor<?> visitor) {
    if (child != null) {
      child.accept(visitor);
    }
  }

  /**
   * Use the given visitor to visit all of the children in the given array.
   * 
   * @param children the children to be visited
   * @param visitor the visitor being used to visit the children
   */
  protected void safelyVisitChildren(Element[] children, ElementVisitor<?> visitor) {
    if (children != null) {
      for (Element child : children) {
        child.accept(visitor);
      }
    }
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
    if (type == null) {
      return null;
    }
    Type[] argumentTypes = definingType.getTypeArguments();
    Type[] parameterTypes = TypeParameterTypeImpl.getTypes(definingType.getTypeParameters());
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

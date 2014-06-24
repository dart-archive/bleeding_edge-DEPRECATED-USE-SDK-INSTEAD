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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

import java.util.HashSet;

/**
 * Instances of the class {@code MultiplyDefinedElementImpl} represent a collection of elements that
 * have the same name within the same scope.
 * 
 * @coverage dart.engine.element
 */
public class MultiplyDefinedElementImpl implements MultiplyDefinedElement {
  /**
   * Return an element that represents the given conflicting elements.
   * 
   * @param context the analysis context in which the multiply defined elements are defined
   * @param firstElement the first element that conflicts
   * @param secondElement the second element that conflicts
   */
  public static Element fromElements(AnalysisContext context, Element firstElement,
      Element secondElement) {
    Element[] conflictingElements = computeConflictingElements(firstElement, secondElement);
    int length = conflictingElements.length;
    if (length == 0) {
      return null;
    } else if (length == 1) {
      return conflictingElements[0];
    }
    return new MultiplyDefinedElementImpl(context, conflictingElements);
  }

  /**
   * Add the given element to the list of elements. If the element is a multiply-defined element,
   * add all of the conflicting elements that it represents.
   * 
   * @param elements the list to which the element(s) are to be added
   * @param element the element(s) to be added
   */
  private static void add(HashSet<Element> elements, Element element) {
    if (element instanceof MultiplyDefinedElementImpl) {
      for (Element conflictingElement : ((MultiplyDefinedElementImpl) element).conflictingElements) {
        elements.add(conflictingElement);
      }
    } else {
      elements.add(element);
    }
  }

  /**
   * Use the given elements to construct an array of conflicting elements. If either of the given
   * elements are multiply-defined elements then the conflicting elements they represent will be
   * included in the array. Otherwise, the element itself will be included.
   * 
   * @param firstElement the first element to be included
   * @param secondElement the second element to be included
   * @return an array containing all of the conflicting elements
   */
  private static Element[] computeConflictingElements(Element firstElement, Element secondElement) {
    HashSet<Element> elements = new HashSet<Element>();
    add(elements, firstElement);
    add(elements, secondElement);
    return elements.toArray(new Element[elements.size()]);
  }

  /**
   * The analysis context in which the multiply defined elements are defined.
   */
  private AnalysisContext context;

  /**
   * The name of the conflicting elements.
   */
  private String name;

  /**
   * A list containing all of the elements that conflict.
   */
  private Element[] conflictingElements;

  /**
   * Initialize a newly created element to represent a list of conflicting elements.
   * 
   * @param context the analysis context in which the multiply defined elements are defined
   * @param conflictingElements the elements that conflict
   */
  public MultiplyDefinedElementImpl(AnalysisContext context, Element[] conflictingElements) {
    this.context = context;
    name = conflictingElements[0].getName();
    this.conflictingElements = conflictingElements;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitMultiplyDefinedElement(this);
  }

  @Override
  public String computeDocumentationComment() throws AnalysisException {
    return null;
  }

  @Override
  public <E extends Element> E getAncestor(Class<E> elementClass) {
    return null;
  }

  @Override
  public Element[] getConflictingElements() {
    return conflictingElements;
  }

  @Override
  public AnalysisContext getContext() {
    return context;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public Element getEnclosingElement() {
    return null;
  }

  @Override
  public String getExtendedDisplayName(String shortName) {
    if (shortName != null) {
      return shortName;
    }
    return getDisplayName();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.ERROR;
  }

  @Override
  public LibraryElement getLibrary() {
    return null;
  }

  @Override
  public ElementLocation getLocation() {
    return null;
  }

  @Override
  public ElementAnnotation[] getMetadata() {
    return ElementAnnotationImpl.EMPTY_ARRAY;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getNameOffset() {
    return -1;
  }

  @Override
  public AstNode getNode() throws AnalysisException {
    return null;
  }

  @Override
  public Source getSource() {
    return null;
  }

  @Override
  public Type getType() {
    return DynamicTypeImpl.getInstance();
  }

  @Override
  public CompilationUnit getUnit() throws AnalysisException {
    return null;
  }

  @Override
  public boolean isAccessibleIn(LibraryElement library) {
    for (Element element : conflictingElements) {
      if (element.isAccessibleIn(library)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isOverride() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    String name = getDisplayName();
    if (name == null) {
      return false;
    }
    return Identifier.isPrivateName(name);
  }

  @Override
  public boolean isPublic() {
    return !isPrivate();
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    int count = conflictingElements.length;
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      ((ElementImpl) conflictingElements[i]).appendTo(builder);
    }
    builder.append("]");
    return builder.toString();
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    // There are no children to visit
  }
}

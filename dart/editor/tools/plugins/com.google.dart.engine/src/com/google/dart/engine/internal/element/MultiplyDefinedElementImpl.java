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

import com.google.dart.engine.element.Annotation;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MultiplyDefinedElement;

import java.util.ArrayList;

/**
 * Instances of the class {@code MultiplyDefinedElementImpl} represent a collection of elements that
 * have the same name within the same scope.
 */
public class MultiplyDefinedElementImpl implements MultiplyDefinedElement {
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
   * @param firstElement the first element that conflicts
   * @param secondElement the second element that conflicts
   */
  public MultiplyDefinedElementImpl(Element firstElement, Element secondElement) {
    name = firstElement.getName();
    conflictingElements = computeConflictingElements(firstElement, secondElement);
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
  public Element getEnclosingElement() {
    return null;
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
  public Annotation[] getMetadata() {
    return AnnotationImpl.EMPTY_ARRAY;
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
  public boolean isSynthetic() {
    return true;
  }

  /**
   * Add the given element to the list of elements. If the element is a multiply-defined element,
   * add all of the conflicting elements that it represents.
   * 
   * @param elements the list to which the element(s) are to be added
   * @param element the element(s) to be added
   */
  private void add(ArrayList<Element> elements, Element element) {
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
  private Element[] computeConflictingElements(Element firstElement, Element secondElement) {
    ArrayList<Element> elements = new ArrayList<Element>();
    add(elements, firstElement);
    add(elements, secondElement);
    return elements.toArray(new Element[elements.size()]);
  }
}

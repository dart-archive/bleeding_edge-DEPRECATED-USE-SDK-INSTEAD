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

import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;

/**
 * Instances of the class {@code ElementAnnotationImpl} implement an {@link ElementAnnotation}.
 * 
 * @coverage dart.engine.element
 */
public class ElementAnnotationImpl implements ElementAnnotation {
  /**
   * The element representing the field, variable, or constructor being used as an annotation.
   */
  private Element element;

  /**
   * An empty array of annotations.
   */
  public static final ElementAnnotationImpl[] EMPTY_ARRAY = new ElementAnnotationImpl[0];

  /**
   * The name of the class used to mark an element as being deprecated.
   */
  private static final String DEPRECATED_CLASS_NAME = "Deprecated";

  /**
   * The name of the top-level variable used to mark an element as being deprecated.
   */
  private static final String DEPRECATED_VARIABLE_NAME = "deprecated";

  /**
   * The name of the top-level variable used to mark a method as being expected to override an
   * inherited method.
   */
  private static final String OVERRIDE_VARIABLE_NAME = "override";

  /**
   * The name of the top-level variable used to mark a class as implementing a proxy object.
   */
  public static final String PROXY_VARIABLE_NAME = "proxy";

  /**
   * Initialize a newly created annotation.
   * 
   * @param element the element representing the field, variable, or constructor being used as an
   *          annotation
   */
  public ElementAnnotationImpl(Element element) {
    this.element = element;
  }

  @Override
  public Element getElement() {
    return element;
  }

  @Override
  public boolean isDeprecated() {
    if (element != null) {
      LibraryElement library = element.getLibrary();
      if (library != null && library.isDartCore()) {
        if (element instanceof ConstructorElement) {
          ConstructorElement constructorElement = (ConstructorElement) element;
          if (constructorElement.getEnclosingElement().getName().equals(DEPRECATED_CLASS_NAME)) {
            return true;
          }
        } else if (element instanceof PropertyAccessorElement
            && element.getName().equals(DEPRECATED_VARIABLE_NAME)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isOverride() {
    if (element != null) {
      LibraryElement library = element.getLibrary();
      if (library != null && library.isDartCore()) {
        if (element instanceof PropertyAccessorElement
            && element.getName().equals(OVERRIDE_VARIABLE_NAME)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isProxy() {
    if (element != null) {
      LibraryElement library = element.getLibrary();
      if (library != null && library.isDartCore()) {
        if (element instanceof PropertyAccessorElement
            && element.getName().equals(PROXY_VARIABLE_NAME)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "@" + element.toString();
  }
}

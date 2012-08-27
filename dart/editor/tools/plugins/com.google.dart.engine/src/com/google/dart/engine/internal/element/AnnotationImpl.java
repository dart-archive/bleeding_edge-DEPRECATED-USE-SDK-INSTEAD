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

/**
 * Instances of the class {@code AnnotationImpl} implement an {@link Annotation}.
 */
public class AnnotationImpl implements Annotation {
  /**
   * The element representing the field, variable, or constructor being used as an annotation.
   */
  private Element element;

  /**
   * An empty array of annotations.
   */
  public static final AnnotationImpl[] EMPTY_ARRAY = new AnnotationImpl[0];

  /**
   * Initialize a newly created annotation.
   * 
   * @param element the element representing the field, variable, or constructor being used as an
   *          annotation
   */
  public AnnotationImpl(Element element) {
    this.element = element;
  }

  @Override
  public Element getElement() {
    return element;
  }

}

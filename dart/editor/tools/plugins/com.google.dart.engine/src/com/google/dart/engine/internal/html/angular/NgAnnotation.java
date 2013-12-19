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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.element.ClassElement;

/**
 * {@link NgAnnotation} describes any <code>NgAnnotation</code> annotation instance.
 */
abstract class NgAnnotation {
  private final ClassElement element;
  private final InjectSelector selector;
  private final String name;

  public NgAnnotation(ClassElement element, InjectSelector selector, String name) {
    this.element = element;
    this.selector = selector;
    this.name = name;
  }

  /**
   * @return the {@link ClassElement} that is annotated with this annotation.
   */
  public ClassElement getElement() {
    return element;
  }

  /**
   * @return the name under which the annotated entity should be published.
   */
  public String getName() {
    return name;
  }

  /**
   * @return the {@link InjectSelector} of this annotation.
   */
  public InjectSelector getSelector() {
    return selector;
  }
}

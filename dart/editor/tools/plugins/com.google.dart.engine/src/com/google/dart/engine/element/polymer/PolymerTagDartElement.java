/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.element.polymer;

import com.google.dart.engine.element.ClassElement;

/**
 * The interface {@code PolymerTagDartElement} defines a Polymer custom tag in Dart.
 * 
 * <pre>
 * @CustomTag('my-example')
 * </pre>
 * 
 * @coverage dart.engine.element
 */
public interface PolymerTagDartElement extends PolymerElement {
  /**
   * Return the {@link ClassElement} that is associated with this Polymer custom tag. Not
   * {@code null}, because {@link PolymerTagDartElement}s are created for {@link ClassElement}s
   * marked with the {@code @CustomTag} annotation.
   */
  ClassElement getClassElement();

  /**
   * Return the {@link PolymerTagHtmlElement} part of this Polymer custom tag. Maybe {@code null} if
   * it has not been resolved yet or there are no corresponding Dart part defined.
   */
  PolymerTagHtmlElement getHtmlElement();
}

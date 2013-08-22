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
package com.google.dart.engine.element;

/**
 * The interface {@code ClassMemberElement} defines the behavior of elements that are contained
 * within a {@link ClassElement}.
 */
public interface ClassMemberElement extends Element {
  /**
   * Return the type in which this member is defined.
   * 
   * @return the type in which this member is defined
   */
  @Override
  public ClassElement getEnclosingElement();

  /**
   * Return {@code true} if this element is a static element. A static element is an element that is
   * not associated with a particular instance, but rather with an entire library or class.
   * 
   * @return {@code true} if this executable element is a static element
   */
  public boolean isStatic();
}

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
package com.google.dart.engine.element;

/**
 * The interface {@code FieldElement} defines the behavior of elements representing a field defined
 * within a type.
 * 
 * @coverage dart.engine.element
 */
public interface FieldElement extends ClassMemberElement, PropertyInducingElement {

  /**
   * Return {@code true} if this element is an enum constant.
   * 
   * @return {@code true} if this an enum constant
   */
  public boolean isEnumConstant();
}

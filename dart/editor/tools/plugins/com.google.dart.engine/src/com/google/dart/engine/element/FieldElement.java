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
 * within a type. Note that explicitly defined fields implicitly define a synthetic getter and that
 * non-{@code final} explicitly defined fields implicitly define a synthetic setter. Symmetrically,
 * synthetic fields are implicitly created for explicitly defined getters and setters. The following
 * rules apply:
 * <ul>
 * <li>Every explicit field is represented by a non-synthetic {@link FieldElement}.
 * <li>Every explicit field induces a getter and possibly a setter, both of which are represented by
 * synthetic {@link PropertyAccessorElement}s.
 * <li>Every explicit getter or setter is represented by a non-synthetic
 * {@link PropertyAccessorElement}.
 * <li>Every explicit getter or setter (or pair thereof if they have the same name) induces a field
 * that is represented by a synthetic {@link FieldElement}.
 * </ul>
 */
public interface FieldElement extends VariableElement {
  /**
   * Return the getter associated with this field. If this field was explicitly defined (is not
   * synthetic) then the getter associated with it will be synthetic.
   * 
   * @return the getter associated with this field
   */
  public PropertyAccessorElement getGetter();

  /**
   * Return the setter associated with this field, or {@code null} if the field is effectively
   * {@code final} and therefore does not have a setter associated with it. (This can happen either
   * because the field is explicitly defined as being {@code final} or because the field is induced
   * by an explicit getter that does not have a corresponding setter.) If this field was explicitly
   * defined (is not synthetic) then the setter associated with it will be synthetic.
   * 
   * @return the setter associated with this field
   */
  public PropertyAccessorElement getSetter();

  /**
   * Return {@code true} if this field is a static field.
   * 
   * @return {@code true} if this field is a static field
   */
  public boolean isStatic();
}

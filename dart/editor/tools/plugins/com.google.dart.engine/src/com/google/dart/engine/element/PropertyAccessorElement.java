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
 * The interface {@code PropertyAccessorElement} defines the behavior of elements representing a
 * getter or a setter. Note that explicitly defined property accessors implicitly define a synthetic
 * field. Symmetrically, synthetic accessors are implicitly created for explicitly defined fields.
 * The following rules apply:
 * <ul>
 * <li>Every explicit field is represented by a non-synthetic {@link FieldElement}.
 * <li>Every explicit field induces a getter and possibly a setter, both of which are represented by
 * synthetic {@link PropertyAccessorElement}s.
 * <li>Every explicit getter or setter is represented by a non-synthetic
 * {@link PropertyAccessorElement}.
 * <li>Every explicit getter or setter (or pair thereof if they have the same name) induces a field
 * that is represented by a synthetic {@link FieldElement}.
 * </ul>
 * 
 * @coverage dart.engine.element
 */
public interface PropertyAccessorElement extends ExecutableElement {
  /**
   * Return the accessor representing the getter that corresponds to (has the same name as) this
   * setter, or {@code null} if this accessor is not a setter or if there is no corresponding
   * getter.
   * 
   * @return the getter that corresponds to this setter
   */
  public PropertyAccessorElement getCorrespondingGetter();

  /**
   * Return the accessor representing the setter that corresponds to (has the same name as) this
   * getter, or {@code null} if this accessor is not a getter or if there is no corresponding
   * setter.
   * 
   * @return the setter that corresponds to this getter
   */
  public PropertyAccessorElement getCorrespondingSetter();

  /**
   * Return the field or top-level variable associated with this accessor. If this accessor was
   * explicitly defined (is not synthetic) then the variable associated with it will be synthetic.
   * 
   * @return the variable associated with this accessor
   */
  public PropertyInducingElement getVariable();

  /**
   * Return {@code true} if this accessor is abstract. Accessors are abstract if they are not
   * external and have no body.
   * 
   * @return {@code true} if this accessor is abstract
   */
  public boolean isAbstract();

  /**
   * Return {@code true} if this accessor represents a getter.
   * 
   * @return {@code true} if this accessor represents a getter
   */
  public boolean isGetter();

  /**
   * Return {@code true} if this accessor represents a setter.
   * 
   * @return {@code true} if this accessor represents a setter
   */
  public boolean isSetter();
}

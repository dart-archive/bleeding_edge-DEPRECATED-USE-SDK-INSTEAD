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
package com.google.dart.engine.type;

import com.google.dart.engine.element.TypeElement;

/**
 * The interface {@code InterfaceType} defines the behavior common to objects representing the type
 * introduced by either a class or an interface, or a reference to such a type.
 */
public interface InterfaceType extends Type {
  @Override
  public TypeElement getElement();

  /**
   * Return an array containing the actual types of the type arguments. If this type's element does
   * not have type parameters, then the array should be empty (although it is possible for type
   * arguments to be erroneously declared). If the element has type parameters and the actual type
   * does not explicitly include argument values, then the type "dynamic" will be automatically
   * provided.
   * 
   * @return the actual types of the type arguments
   */
  public Type[] getTypeArguments();

  /**
   * Return {@code true} if this type is a direct supertype of the given type. The implicit
   * interface of class <i>I</i> is a direct supertype of the implicit interface of class <i>J</i>
   * iff:
   * <ul>
   * <li><i>I</i> is Object, and <i>J</i> has no extends clause.</li>
   * <li><i>I</i> is listed in the extends clause of <i>J</i>.</li>
   * <li><i>I</i> is listed in the implements clause of <i>J</i>.</li>
   * </ul>
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is a direct supertype of the given type
   */
  public boolean isDirectSupertypeOf(InterfaceType type);

  /**
   * Return {@code true} if this type is more specific than the given type. An interface type
   * <i>T</i> is more specific than an interface type <i>S</i>, written <i>T &laquo; S</i>, if one
   * of the following conditions is met:
   * <ul>
   * <li>Reflexivity: <i>T</i> is <i>S</i>.
   * <li><i>T</i> is bottom.
   * <li><i>S</i> is dynamic.
   * <li>Direct supertype: <i>S</i> is a direct supertype of <i>T</i>.
   * <li><i>T</i> is a type variable and <i>S</i> is the upper bound of <i>T</i>.
   * <li>Covariance: <i>T</i> is of the form <i>I&lt;T<sub>1</sub>, &hellip;, T<sub>n</sub>&gt;</i>
   * and S</i> is of the form <i>I&lt;S<sub>1</sub>, &hellip;, S<sub>n</sub>&gt;</i> and
   * <i>T<sub>i</sub> &laquo; S<sub>i</sub></i>, <i>1 <= i <= n</i>.
   * <li>Transitivity: <i>T &laquo; U</i> and <i>U &laquo; S</i>.
   * </ul>
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is more specific than the given type
   */
  public boolean isMoreSpecificThan(Type type);

  /**
   * Return {@code true} if this type is a subtype of the given type. An interface type <i>T</i> is
   * a subtype of an interface type <i>S</i>, written <i>T</i> <: <i>S</i>, iff
   * <i>[bottom/dynamic]T</i> &laquo; <i>S</i> (<i>T</i> is more specific than <i>S</i>). If an
   * interface type <i>I</i> includes a method named <i>call()</i>, and the type of <i>call()</i> is
   * the function type <i>F</i>, then <i>I</i> is considered to be a subtype of <i>F</i>.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is a subtype of the given type
   */
  @Override
  public boolean isSubtypeOf(Type type);
}

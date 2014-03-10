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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * The interface {@code Type} defines the behavior of objects representing the declared type of
 * elements in the element model.
 * 
 * @coverage dart.engine.type
 */
@DartName("DartType")
public interface Type {
  /**
   * Return the name of this type as it should appear when presented to users in contexts such as
   * error messages.
   * 
   * @return the name of this type
   */
  public String getDisplayName();

  /**
   * Return the element representing the declaration of this type, or {@code null} if the type has
   * not, or cannot, be associated with an element. The former case will occur if the element model
   * is not yet complete; the latter case will occur if this object represents an undefined type.
   * 
   * @return the element representing the declaration of this type
   */
  public Element getElement();

  /**
   * Return the least upper bound of this type and the given type, or {@code null} if there is no
   * least upper bound.
   * 
   * @param type the other type used to compute the least upper bound
   * @return the least upper bound of this type and the given type
   */
  public Type getLeastUpperBound(Type type);

  /**
   * Return the name of this type, or {@code null} if the type does not have a name, such as when
   * the type represents the type of an unnamed function.
   * 
   * @return the name of this type
   */
  public String getName();

  /**
   * Return {@code true} if this type is assignable to the given type. A type <i>T</i> may be
   * assigned to a type <i>S</i>, written <i>T</i> &hArr; <i>S</i>, iff either <i>T</i> <: <i>S</i>
   * or <i>S</i> <: <i>T</i>.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is assignable to the given type
   */
  public boolean isAssignableTo(Type type);

  /**
   * Return {@code true} if this type represents the bottom type.
   * 
   * @return {@code true} if this type represents the bottom type
   */
  public boolean isBottom();

  /**
   * Return {@code true} if this type represents the type 'Function' defined in the dart:core
   * library.
   * 
   * @return {@code true} if this type represents the type 'Function' defined in the dart:core
   *         library
   */
  public boolean isDartCoreFunction();

  /**
   * Return {@code true} if this type represents the type 'dynamic'.
   * 
   * @return {@code true} if this type represents the type 'dynamic'
   */
  public boolean isDynamic();

  /**
   * Return {@code true} if this type is more specific than the given type.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is more specific than the given type
   */
  public boolean isMoreSpecificThan(Type type);

  /**
   * Return {@code true} if this type represents the type 'Object'.
   * 
   * @return {@code true} if this type represents the type 'Object'
   */
  public boolean isObject();

  /**
   * Return {@code true} if this type is a subtype of the given type.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is a subtype of the given type
   */
  public boolean isSubtypeOf(Type type);

  /**
   * Return {@code true} if this type is a supertype of the given type. A type <i>S</i> is a
   * supertype of <i>T</i>, written <i>S</i> :> <i>T</i>, iff <i>T</i> is a subtype of <i>S</i>.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is a supertype of the given type
   */
  public boolean isSupertypeOf(Type type);

  /**
   * Return {@code true} if this type represents the type 'void'.
   * 
   * @return {@code true} if this type represents the type 'void'
   */
  public boolean isVoid();

  /**
   * Return the type resulting from substituting the given arguments for the given parameters in
   * this type. The specification defines this operation in section 2: <blockquote> The notation
   * <i>[x<sub>1</sub>, ..., x<sub>n</sub>/y<sub>1</sub>, ..., y<sub>n</sub>]E</i> denotes a copy of
   * <i>E</i> in which all occurrences of <i>y<sub>i</sub>, 1 <= i <= n</i> have been replaced with
   * <i>x<sub>i</sub></i>.</blockquote> Note that, contrary to the specification, this method will
   * not create a copy of this type if no substitutions were required, but will return this type
   * directly.
   * 
   * @param argumentTypes the actual type arguments being substituted for the parameters
   * @param parameterTypes the parameters to be replaced
   * @return the result of performing the substitution
   */
  public Type substitute(Type[] argumentTypes, Type[] parameterTypes);
}

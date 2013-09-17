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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;

/**
 * The interface {@code InterfaceType} defines the behavior common to objects representing the type
 * introduced by either a class or an interface, or a reference to such a type.
 * 
 * @coverage dart.engine.type
 */
public interface InterfaceType extends ParameterizedType {
  /**
   * Return an array containing all of the accessors (getters and setters) declared in this type.
   * 
   * @return the accessors declared in this type
   */
  public PropertyAccessorElement[] getAccessors();

  @Override
  public ClassElement getElement();

  /**
   * Return the element representing the getter with the given name that is declared in this class,
   * or {@code null} if this class does not declare a getter with the given name.
   * 
   * @param getterName the name of the getter to be returned
   * @return the getter declared in this class with the given name
   */
  public PropertyAccessorElement getGetter(String getterName);

  /**
   * Return an array containing all of the interfaces that are implemented by this interface. Note
   * that this is <b>not</b>, in general, equivalent to getting the interfaces from this type's
   * element because the types returned by this method will have had their type parameters replaced.
   * 
   * @return the interfaces that are implemented by this type
   */
  public InterfaceType[] getInterfaces();

  /**
   * Return the least upper bound of this type and the given type, or {@code null} if there is no
   * least upper bound.
   * <p>
   * Given two interfaces <i>I</i> and <i>J</i>, let <i>S<sub>I</sub></i> be the set of
   * superinterfaces of <i>I<i>, let <i>S<sub>J</sub></i> be the set of superinterfaces of <i>J</i>
   * and let <i>S = (I &cup; S<sub>I</sub>) &cap; (J &cup; S<sub>J</sub>)</i>. Furthermore, we
   * define <i>S<sub>n</sub> = {T | T &isin; S &and; depth(T) = n}</i> for any finite <i>n</i>,
   * where <i>depth(T)</i> is the number of steps in the longest inheritance path from <i>T</i> to
   * <i>Object</i>. Let <i>q</i> be the largest number such that <i>S<sub>q</sub></i> has
   * cardinality one. The least upper bound of <i>I</i> and <i>J</i> is the sole element of
   * <i>S<sub>q</sub></i>.
   * 
   * @param type the other type used to compute the least upper bound
   * @return the least upper bound of this type and the given type
   */
  @Override
  public Type getLeastUpperBound(Type type);

  /**
   * Return the element representing the method with the given name that is declared in this class,
   * or {@code null} if this class does not declare a method with the given name.
   * 
   * @param methodName the name of the method to be returned
   * @return the method declared in this class with the given name
   */
  public MethodElement getMethod(String methodName);

  /**
   * Return an array containing all of the methods declared in this type.
   * 
   * @return the methods declared in this type
   */
  public MethodElement[] getMethods();

  /**
   * Return an array containing all of the mixins that are applied to the class being extended in
   * order to derive the superclass of this class. Note that this is <b>not</b>, in general,
   * equivalent to getting the mixins from this type's element because the types returned by this
   * method will have had their type parameters replaced.
   * 
   * @return the mixins that are applied to derive the superclass of this class
   */
  public InterfaceType[] getMixins();

  /**
   * Return the element representing the setter with the given name that is declared in this class,
   * or {@code null} if this class does not declare a setter with the given name.
   * 
   * @param setterName the name of the setter to be returned
   * @return the setter declared in this class with the given name
   */
  public PropertyAccessorElement getSetter(String setterName);

  /**
   * Return the type representing the superclass of this type, or null if this type represents the
   * class 'Object'. Note that this is <b>not</b>, in general, equivalent to getting the superclass
   * from this type's element because the type returned by this method will have had it's type
   * parameters replaced.
   * 
   * @return the superclass of this type
   */
  public InterfaceType getSuperclass();

  /**
   * Return {@code true} if this type is a direct supertype of the given type. The implicit
   * interface of class <i>I</i> is a direct supertype of the implicit interface of class <i>J</i>
   * iff:
   * <ul>
   * <li><i>I</i> is Object, and <i>J</i> has no extends clause.</li>
   * <li><i>I</i> is listed in the extends clause of <i>J</i>.</li>
   * <li><i>I</i> is listed in the implements clause of <i>J</i>.</li>
   * <li><i>I</i> is listed in the with clause of <i>J</i>.</li>
   * <li><i>J</i> is a mixin application of the mixin of <i>I</i>.</li>
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
   * <li><i>T</i> is a type parameter and <i>S</i> is the upper bound of <i>T</i>.
   * <li>Covariance: <i>T</i> is of the form <i>I&lt;T<sub>1</sub>, &hellip;, T<sub>n</sub>&gt;</i>
   * and S</i> is of the form <i>I&lt;S<sub>1</sub>, &hellip;, S<sub>n</sub>&gt;</i> and
   * <i>T<sub>i</sub> &laquo; S<sub>i</sub></i>, <i>1 <= i <= n</i>.
   * <li>Transitivity: <i>T &laquo; U</i> and <i>U &laquo; S</i>.
   * </ul>
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is more specific than the given type
   */
  @Override
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

  /**
   * Return the element representing the constructor that results from looking up the given
   * constructor in this class with respect to the given library, or {@code null} if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.11.1: <blockquote>If <i>e</i> is of the form <b>new</b> <i>T.id()</i> then let <i>q<i> be
   * the constructor <i>T.id</i>, otherwise let <i>q<i> be the constructor <i>T<i>. Otherwise, if
   * <i>q</i> is not defined or not accessible, a NoSuchMethodException is thrown. </blockquote>
   * 
   * @param constructorName the name of the constructor being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given constructor in this class with respect to the given
   *         library
   */
  public ConstructorElement lookUpConstructor(String constructorName, LibraryElement library);

  /**
   * Return the element representing the getter that results from looking up the given getter in
   * this class with respect to the given library, or {@code null} if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.15.1:
   * <blockquote>The result of looking up getter (respectively setter) <i>m</i> in class <i>C</i>
   * with respect to library <i>L</i> is:
   * <ul>
   * <li>If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.</li>
   * </ul>
   * </blockquote>
   * 
   * @param getterName the name of the getter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given getter in this class with respect to the given
   *         library
   */
  public PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library);

  /**
   * Return the element representing the getter that results from looking up the given getter in the
   * superclass of this class with respect to the given library, or {@code null} if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.15.1: <blockquote>The result of looking up getter (respectively setter) <i>m</i> in class
   * <i>C</i> with respect to library <i>L</i> is:
   * <ul>
   * <li>If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.</li>
   * </ul>
   * </blockquote>
   * 
   * @param getterName the name of the getter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given getter in this class with respect to the given
   *         library
   */
  public PropertyAccessorElement lookUpGetterInSuperclass(String getterName, LibraryElement library);

  /**
   * Return the element representing the method that results from looking up the given method in
   * this class with respect to the given library, or {@code null} if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.15.1:
   * <blockquote> The result of looking up method <i>m</i> in class <i>C</i> with respect to library
   * <i>L</i> is:
   * <ul>
   * <li>If <i>C</i> declares an instance method named <i>m</i> that is accessible to <i>L</i>, then
   * that method is the result of the lookup. Otherwise, if <i>C</i> has a superclass <i>S</i>, then
   * the result of the lookup is the result of looking up method <i>m</i> in <i>S</i> with respect
   * to <i>L</i>. Otherwise, we say that the lookup has failed.</li>
   * </ul>
   * </blockquote>
   * 
   * @param methodName the name of the method being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given method in this class with respect to the given
   *         library
   */
  public MethodElement lookUpMethod(String methodName, LibraryElement library);

  /**
   * Return the element representing the method that results from looking up the given method in the
   * superclass of this class with respect to the given library, or {@code null} if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.15.1: <blockquote> The result of looking up method <i>m</i> in class <i>C</i> with respect
   * to library <i>L</i> is:
   * <ul>
   * <li>If <i>C</i> declares an instance method named <i>m</i> that is accessible to <i>L</i>, then
   * that method is the result of the lookup. Otherwise, if <i>C</i> has a superclass <i>S</i>, then
   * the result of the lookup is the result of looking up method <i>m</i> in <i>S</i> with respect
   * to <i>L</i>. Otherwise, we say that the lookup has failed.</li>
   * </ul>
   * </blockquote>
   * 
   * @param methodName the name of the method being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given method in this class with respect to the given
   *         library
   */
  public MethodElement lookUpMethodInSuperclass(String methodName, LibraryElement library);

  /**
   * Return the element representing the setter that results from looking up the given setter in
   * this class with respect to the given library, or {@code null} if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.16:
   * <blockquote> The result of looking up getter (respectively setter) <i>m</i> in class <i>C</i>
   * with respect to library <i>L</i> is:
   * <ul>
   * <li>If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.</li>
   * </ul>
   * </blockquote>
   * 
   * @param setterName the name of the setter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given setter in this class with respect to the given
   *         library
   */
  public PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library);

  /**
   * Return the element representing the setter that results from looking up the given setter in the
   * superclass of this class with respect to the given library, or {@code null} if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.16: <blockquote> The result of looking up getter (respectively setter) <i>m</i> in class
   * <i>C</i> with respect to library <i>L</i> is:
   * <ul>
   * <li>If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.</li>
   * </ul>
   * </blockquote>
   * 
   * @param setterName the name of the setter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given setter in this class with respect to the given
   *         library
   */
  public PropertyAccessorElement lookUpSetterInSuperclass(String setterName, LibraryElement library);

  /**
   * Return the type resulting from substituting the given arguments for this type's parameters.
   * This is fully equivalent to {@code substitute(argumentTypes, getTypeArguments())}.
   * 
   * @param argumentTypes the actual type arguments being substituted for the type parameters
   * @return the result of performing the substitution
   */
  public InterfaceType substitute(Type[] argumentTypes);

  @Override
  public InterfaceType substitute(Type[] argumentTypes, Type[] parameterTypes);
}

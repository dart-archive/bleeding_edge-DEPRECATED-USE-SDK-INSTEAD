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

import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.type.InterfaceType;

/**
 * The interface {@code ClassElement} defines the behavior of elements that represent a class.
 * 
 * @coverage dart.engine.element
 */
public interface ClassElement extends Element {
  /**
   * Return an array containing all of the accessors (getters and setters) declared in this class.
   * 
   * @return the accessors declared in this class
   */
  public PropertyAccessorElement[] getAccessors();

  /**
   * Return an array containing all the supertypes defined for this class and its supertypes. This
   * includes superclasses, mixins and interfaces.
   * 
   * @return all the supertypes of this class, including mixins
   */
  public InterfaceType[] getAllSupertypes();

  /**
   * Return an array containing all of the constructors declared in this class.
   * 
   * @return the constructors declared in this class
   */
  public ConstructorElement[] getConstructors();

  /**
   * Return the field (synthetic or explicit) defined in this class that has the given name, or
   * {@code null} if this class does not define a field with the given name.
   * 
   * @param fieldName the name of the field to be returned
   * @return the field with the given name that is defined in this class
   */
  public FieldElement getField(String fieldName);

  /**
   * Return an array containing all of the fields declared in this class.
   * 
   * @return the fields declared in this class
   */
  public FieldElement[] getFields();

  /**
   * Return the element representing the getter with the given name that is declared in this class,
   * or {@code null} if this class does not declare a getter with the given name.
   * 
   * @param getterName the name of the getter to be returned
   * @return the getter declared in this class with the given name
   */
  public PropertyAccessorElement getGetter(String getterName);

  /**
   * Return an array containing all of the interfaces that are implemented by this class.
   * <p>
   * <b>Note:</b> Because the element model represents the state of the code, it is possible for it
   * to be semantically invalid. In particular, it is not safe to assume that the inheritance
   * structure of a class does not contain a cycle. Clients that traverse the inheritance structure
   * must explicitly guard against infinite loops.
   * 
   * @return the interfaces that are implemented by this class
   */
  public InterfaceType[] getInterfaces();

  /**
   * Return the element representing the method with the given name that is declared in this class,
   * or {@code null} if this class does not declare a method with the given name.
   * 
   * @param methodName the name of the method to be returned
   * @return the method declared in this class with the given name
   */
  public MethodElement getMethod(String methodName);

  /**
   * Return an array containing all of the methods declared in this class.
   * 
   * @return the methods declared in this class
   */
  public MethodElement[] getMethods();

  /**
   * Return an array containing all of the mixins that are applied to the class being extended in
   * order to derive the superclass of this class.
   * <p>
   * <b>Note:</b> Because the element model represents the state of the code, it is possible for it
   * to be semantically invalid. In particular, it is not safe to assume that the inheritance
   * structure of a class does not contain a cycle. Clients that traverse the inheritance structure
   * must explicitly guard against infinite loops.
   * 
   * @return the mixins that are applied to derive the superclass of this class
   */
  public InterfaceType[] getMixins();

  /**
   * Return the named constructor declared in this class with the given name, or {@code null} if
   * this class does not declare a named constructor with the given name.
   * 
   * @param name the name of the constructor to be returned
   * @return the element representing the specified constructor
   */
  public ConstructorElement getNamedConstructor(String name);

  /**
   * Return the resolved {@link ClassDeclaration} node that declares this {@link ClassElement}.
   * <p>
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   * 
   * @return the resolved {@link ClassDeclaration}, not {@code null}.
   */
  @Override
  public ClassDeclaration getNode() throws AnalysisException;

  /**
   * Return the element representing the setter with the given name that is declared in this class,
   * or {@code null} if this class does not declare a setter with the given name.
   * 
   * @param setterName the name of the getter to be returned
   * @return the setter declared in this class with the given name
   */
  public PropertyAccessorElement getSetter(String setterName);

  /**
   * Return the superclass of this class, or {@code null} if the class represents the class
   * 'Object'. All other classes will have a non-{@code null} superclass. If the superclass was not
   * explicitly declared then the implicit superclass 'Object' will be returned.
   * <p>
   * <b>Note:</b> Because the element model represents the state of the code, it is possible for it
   * to be semantically invalid. In particular, it is not safe to assume that the inheritance
   * structure of a class does not contain a cycle. Clients that traverse the inheritance structure
   * must explicitly guard against infinite loops.
   * 
   * @return the superclass of this class
   */
  public InterfaceType getSupertype();

  /**
   * Return an array containing all of the toolkit specific objects associated with this class. The
   * array will be empty if the class does not have any toolkit specific objects or if the
   * compilation unit containing the class has not yet had toolkit references resolved.
   * 
   * @return the toolkit objects associated with this class
   */
  public ToolkitObjectElement[] getToolkitObjects();

  /**
   * Return the type defined by the class.
   * 
   * @return the type defined by the class
   */
  public InterfaceType getType();

  /**
   * Return an array containing all of the type parameters declared for this class.
   * 
   * @return the type parameters declared for this class
   */
  public TypeParameterElement[] getTypeParameters();

  /**
   * Return the unnamed constructor declared in this class, or {@code null} if this class does not
   * declare an unnamed constructor but does declare named constructors. The returned constructor
   * will be synthetic if this class does not declare any constructors, in which case it will
   * represent the default constructor for the class.
   * 
   * @return the unnamed constructor defined in this class
   */
  public ConstructorElement getUnnamedConstructor();

  /**
   * Return {@code true} if this class or its superclass declares a non-final instance field.
   * 
   * @return {@code true} if this class or its superclass declares a non-final instance field
   */
  public boolean hasNonFinalField();

  /**
   * Return {@code true} if this class has reference to super (so, for example, cannot be used as a
   * mixin).
   * 
   * @return {@code true} if this class has reference to super
   */
  public boolean hasReferenceToSuper();

  /**
   * Return {@code true} if this class declares a static member.
   * 
   * @return {@code true} if this class declares a static member
   */
  public boolean hasStaticMember();

  /**
   * Return {@code true} if this class is abstract. A class is abstract if it has an explicit
   * {@code abstract} modifier. Note, that this definition of <i>abstract</i> is different from
   * <i>has unimplemented members</i>.
   * 
   * @return {@code true} if this class is abstract
   */
  public boolean isAbstract();

  /**
   * Return {@code true} if this class is defined by an enum declaration.
   * 
   * @return {@code true} if this class is defined by an enum declaration
   */
  public boolean isEnum();

  /**
   * Return {@code true} if this class {@link #isProxy()}, or if it inherits the proxy annotation
   * from a supertype.
   * 
   * @return {@code true} if this class defines or inherits a proxy
   */
  public boolean isOrInheritsProxy();

  /**
   * Return {@code true} if this element has an annotation of the form '@proxy'.
   * 
   * @return {@code true} if this element defines a proxy
   */
  public boolean isProxy();

  /**
   * Return {@code true} if this class is defined by a typedef construct.
   * 
   * @return {@code true} if this class is defined by a typedef construct
   */
  public boolean isTypedef();

  /**
   * Return {@code true} if this class can validly be used as a mixin when defining another class.
   * The behavior of this method is defined by the Dart Language Specification in section 9:
   * <blockquote>It is a compile-time error if a declared or derived mixin refers to super. It is a
   * compile-time error if a declared or derived mixin explicitly declares a constructor. It is a
   * compile-time error if a mixin is derived from a class whose superclass is not
   * Object.</blockquote>
   * 
   * @return {@code true} if this class can validly be used as a mixin
   */
  public boolean isValidMixin();

  /**
   * Return the element representing the method that results from looking up the given method in
   * this class with respect to the given library, ignoring abstract methods, or {@code null} if the
   * look up fails. The behavior of this method is defined by the Dart Language Specification in
   * section 12.15.1: <blockquote> The result of looking up method <i>m</i> in class <i>C</i> with
   * respect to library <i>L</i> is:
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
  public MethodElement lookUpConcreteMethod(String methodName, LibraryElement library);

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
   * superclass of this class with respect to the given library, ignoring abstract getters, or
   * {@code null} if the look up fails. The behavior of this method is defined by the Dart Language
   * Specification in section 12.15.1: <blockquote>The result of looking up getter (respectively
   * setter) <i>m</i> in class <i>C</i> with respect to library <i>L</i> is:
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
  public PropertyAccessorElement lookUpInheritedConcreteGetter(String getterName,
      LibraryElement library);

  /**
   * Return the element representing the method that results from looking up the given method in the
   * superclass of this class with respect to the given library, ignoring abstract methods, or
   * {@code null} if the look up fails. The behavior of this method is defined by the Dart Language
   * Specification in section 12.15.1: <blockquote> The result of looking up method <i>m</i> in
   * class <i>C</i> with respect to library <i>L</i> is:
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
   * @return the result of looking up the given method in the superclass of this class with respect
   *         to the given library
   */
  public MethodElement lookUpInheritedConcreteMethod(String methodName, LibraryElement library);

  /**
   * Return the element representing the setter that results from looking up the given setter in the
   * superclass of this class with respect to the given library, ignoring abstract setters, or
   * {@code null} if the look up fails. The behavior of this method is defined by the Dart Language
   * Specification in section 12.16: <blockquote> The result of looking up getter (respectively
   * setter) <i>m</i> in class <i>C</i> with respect to library <i>L</i> is:
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
  public PropertyAccessorElement lookUpInheritedConcreteSetter(String setterName,
      LibraryElement library);

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
   * @return the result of looking up the given method in the superclass of this class with respect
   *         to the given library
   */
  public MethodElement lookUpInheritedMethod(String methodName, LibraryElement library);

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
}

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
package com.google.dart.engine.mirrors;

import com.google.dart.engine.mirrors.core.Symbol;

/**
 * A [MethodMirror] reflects a Dart language function, method, constructor, getter, or setter.
 */
public interface MethodMirror extends DeclarationMirror {
  /**
   * The constructor name for named constructors and factory methods. For unnamed constructors, this
   * is the empty string. For non-constructors, this is the empty string. For example, [:'bar':] is
   * the constructor name for constructor [:Foo.bar:] of type [:Foo:].
   */
  Symbol getConstructorName();

  /**
   * A list of mirrors on the parameters for the reflectee.
   */
  ParameterMirror[] getParameters();

  /**
   * A mirror on the return type for the reflectee.
   */
  TypeMirror getReturnType();

  /**
   * Is the reflectee abstract?
   */
  boolean isAbstract();

  /**
   * Is the reflectee a const constructor?
   */
  boolean isConstConstructor();

  /**
   * Is the reflectee a constructor?
   */
  boolean isConstructor();

  /**
   * Is the reflectee a factory constructor?
   */
  boolean isFactoryConstructor();

  /**
   * Is the reflectee a generative constructor?
   */
  boolean isGenerativeConstructor();

  /**
   * Is the reflectee a getter?
   */
  boolean isGetter();

  /**
   * Is the reflectee an operator?
   */
  boolean isOperator();

  /**
   * Is the reflectee a redirecting constructor?
   */
  boolean isRedirectingConstructor();

  /**
   * Is the reflectee a regular function or method? A function or method is regular if it is not a
   * getter, setter, or constructor. Note that operators, by this definition, are regular methods.
   */
  boolean isRegularMethod();

  /**
   * Is the reflectee a setter?
   */
  boolean isSetter();

  /**
   * Is the reflectee static? For the purposes of the mirrors library, a top-level function is
   * considered static.
   */
  boolean isStatic();
}

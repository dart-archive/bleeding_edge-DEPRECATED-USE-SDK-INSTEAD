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

import com.google.dart.engine.element.ExecutableElement;

import java.util.Map;

/**
 * The interface {@code FunctionType} defines the behavior common to objects representing the type
 * of a function, method, constructor, getter, or setter. Function types come in three variations:
 * <ol>
 * <li>The types of functions that only have required parameters. These have the general form
 * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T</i>.</li>
 * <li>The types of functions with optional positional parameters. These have the general form
 * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, [T<sub>n+1</sub>, &hellip;, T<sub>n+k</sub>]) &rarr;
 * T</i>.</li>
 * <li>The types of functions with named positional parameters. These have the general form
 * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, {T<sub>x1</sub> x1, &hellip;, T<sub>xk</sub> xk})
 * &rarr; T</i>.</li>
 * </ol>
 */
public interface FunctionType extends Type {
  @Override
  public ExecutableElement getElement();

  /**
   * Return a map from the names of named parameters to the types of the named parameters of this
   * type of function. The entries in the map will be iterated in the same order as the order in
   * which the named parameters were defined. If there were no named parameters declared then the
   * map will be empty.
   * 
   * @return a map from the name to the types of the named parameters of this type of function
   */
  public Map<String, Type> getNamedParameterTypes();

  /**
   * Return an array containing the types of the normal parameters of this type of function. The
   * parameter types are in the same order as they appear in the declaration of the function.
   * 
   * @return the types of the normal parameters of this type of function
   */
  public Type[] getNormalParameterTypes();

  /**
   * Return a map from the names of optional (positional) parameters to the types of the optional
   * parameters of this type of function. The entries in the map will be iterated in the same order
   * as the order in which the optional parameters were defined. If there were no optional
   * parameters declared then the map will be empty.
   * 
   * @return a map from the name to the types of the optional parameters of this type of function
   */
  public Type[] getOptionalParameterTypes();

  /**
   * Return the type of object returned by this type of function.
   * 
   * @return the type of object returned by this type of function
   */
  public Type getReturnType();

  /**
   * Return {@code true} if this type is a subtype of the given type.
   * <p>
   * A function type <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T</i> is a subtype of the
   * function type <i>(S<sub>1</sub>, &hellip;, S<sub>n</sub>) &rarr; S</i>, if all of the following
   * conditions are met:
   * <ul>
   * <li>Either
   * <ul>
   * <li><i>S</i> is void, or</li>
   * <li><i>T &hArr; S</i>.</li>
   * </ul>
   * </li>
   * <li>For all <i>i</i>, 1 <= <i>i</i> <= <i>n</i>, <i>T<sub>i</sub> &hArr; S<sub>i</sub></i>.</li>
   * </ul>
   * A function type <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, [T<sub>n+1</sub>, &hellip;,
   * T<sub>n+k</sub>]) &rarr; T</i> is a subtype of the function type <i>(S<sub>1</sub>, &hellip;,
   * S<sub>n</sub>, [S<sub>n+1</sub>, &hellip;, S<sub>n+m</sub>]) &rarr; S</i>, if all of the
   * following conditions are met:
   * <ul>
   * <li>Either
   * <ul>
   * <li><i>S</i> is void, or</li>
   * <li><i>T &hArr; S</i>.</li>
   * </ul>
   * </li>
   * <li><i>k</i> >= <i>m</i> and for all <i>i</i>, 1 <= <i>i</i> <= <i>n+m</i>, <i>T<sub>i</sub>
   * &hArr; S<sub>i</sub></i>.</li>
   * </ul>
   * A function type <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, {T<sub>x1</sub> x1, &hellip;,
   * T<sub>xk</sub> xk}) &rarr; T</i> is a subtype of the function type <i>(S<sub>1</sub>, &hellip;,
   * S<sub>n</sub>, {S<sub>y1</sub> y1, &hellip;, S<sub>ym</sub> ym}) &rarr; S</i>, if all of the
   * following conditions are met:
   * <ul>
   * <li>Either
   * <ul>
   * <li><i>S</i> is void,</li>
   * <li>or <i>T &hArr; S</i>.</li>
   * </ul>
   * </li>
   * <li>For all <i>i</i>, 1 <= <i>i</i> <= <i>n</i>, <i>T<sub>i</sub> &hArr; S<sub>i</sub></i>.</li>
   * <li><i>k</i> >= <i>m</i> and <i>y<sub>i</sub></i> in <i>{x<sub>1</sub>, &hellip;,
   * x<sub>k</sub>}</i>, 1 <= <i>i</i> <= <i>m</i>.</li>
   * <li>For all <i>y<sub>i</sub></i> in <i>{y<sub>1</sub>, &hellip;, y<sub>m</sub>}</i>,
   * <i>y<sub>i</sub> = x<sub>j</sub> => Tj &hArr; Si</i>.</li>
   * </ul>
   * In addition, the following subtype rules apply:
   * <p>
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, []) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>) &rarr; T.</i><br>
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>, {}) &rarr; T.</i><br>
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, {}) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>) &rarr; T.</i><br>
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>, []) &rarr; T.</i>
   * <p>
   * All functions implement the class {@code Function}. However not all function types are a
   * subtype of {@code Function}. If an interface type <i>I</i> includes a method named
   * {@code call()}, and the type of {@code call()} is the function type <i>F</i>, then <i>I</i> is
   * considered to be a subtype of <i>F</i>.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is a subtype of the given type
   */
  @Override
  public boolean isSubtypeOf(Type type);
}

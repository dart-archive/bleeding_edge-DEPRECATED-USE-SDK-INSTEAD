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

import com.google.dart.engine.mirrors.core.Future;
import com.google.dart.engine.mirrors.core.Symbol;

import java.util.Map;

/**
 * An {@code ObjectMirror} is a common superinterface of {@link InstanceMirror}, {
 * 
 * @link ClassMirror}, and {@link LibraryMirror} that represents their shared functionality. For the
 *       purposes of the mirrors library, these types are all object-like, in that they support
 *       method invocation and field access. Real Dart objects are represented by the
 *       {@link InstanceMirror} type. See {@link InstanceMirror}, {@link ClassMirror}, and
 *       {@link LibraryMirror}.
 */
public interface ObjectMirror extends Mirror {

  /**
   * Invokes a getter and returns a mirror on the result. The getter can be the implicit getter for
   * a field or a user-defined getter method.
   */
  /* TODO(turnidge): Handle ambiguous names.*/
  InstanceMirror getField(Symbol fieldName);

  /**
   * Invokes a getter and returns a mirror on the result. The getter can be the implicit getter for
   * a field or a user-defined getter method.
   */
  /* TODO(turnidge): Handle ambiguous names.*/
  Future<InstanceMirror> getFieldAsync(Symbol fieldName);

  /**
   * Invokes the named function and returns a mirror on the result. The arguments are objects local
   * to the current isolate.
   */
  /* TODO(turnidge): Properly document.
   * TODO(turnidge): Handle ambiguous names.
   * TODO(turnidge): Handle optional & named arguments.
   */
  InstanceMirror invoke(Symbol memberName, Object[] positionalArguments);

  /**
   * Invokes the named function and returns a mirror on the result. The arguments are objects local
   * to the current isolate.
   */
  /* TODO(turnidge): Properly document.
   * TODO(turnidge): Handle ambiguous names.
   * TODO(turnidge): Handle optional & named arguments.
   */
  InstanceMirror invoke(Symbol memberName, Object[] positionalArguments,
      Map<Symbol, Object> namedArguments);

  /**
   * Invokes the named function and returns a mirror on the result. The arguments must be instances
   * of {@link InstanceMirror}, {@link num}, {@link String}, or {@link bool}.
   */
  /* TODO(turnidge): Properly document.
   * TODO(turnidge): Handle ambiguous names.
   * TODO(turnidge): Handle optional & named arguments.
   */
  Future<InstanceMirror> invokeAsync(Symbol memberName, Object[] positionalArguments);

  /**
   * Invokes the named function and returns a mirror on the result. The arguments must be instances
   * of {@link InstanceMirror}, {@link num}, {@link String}, or {@link bool}.
   */
  /* TODO(turnidge): Properly document.
   * TODO(turnidge): Handle ambiguous names.
   * TODO(turnidge): Handle optional & named arguments.
   */
  Future<InstanceMirror> invokeAsync(Symbol memberName, Object[] positionalArguments,
      Map<Symbol, Object> namedArguments);

  /**
   * Invokes a setter and returns a mirror on the result. The setter may be either the implicit
   * setter for a non-final field or a user-defined setter method. The argument is an object local
   * to the current isolate.
   */
  /* TODO(turnidge): Handle ambiguous names.*/
  InstanceMirror setField(Symbol fieldName, Object arg);

  /**
   * Invokes a setter and returns a mirror on the result. The setter may be either the implicit
   * setter for a non-final field or a user-defined setter method. The argument must be an instance
   * of either {@link InstanceMirror}, {@link num}, {@link String}, or {@link bool}.
   */
  /* TODO(turnidge): Handle ambiguous names.*/
  Future<InstanceMirror> setFieldAsync(Symbol fieldName, Object value);
}

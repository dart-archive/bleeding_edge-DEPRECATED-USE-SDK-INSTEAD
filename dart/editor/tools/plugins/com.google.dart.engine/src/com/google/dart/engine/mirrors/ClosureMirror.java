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
 * A [ClosureMirror] reflects a closure. A [ClosureMirror] provides access to its captured variables
 * and provides the ability to execute its reflectee.
 */
public interface ClosureMirror extends InstanceMirror {
  /**
   * Executes the closure. The arguments are objects local to the current isolate.
   */
  InstanceMirror apply(Object[] positionalArguments);

  /**
   * Executes the closure. The arguments are objects local to the current isolate.
   */
  InstanceMirror apply(Object[] positionalArguments, Map<Symbol, Object> namedArguments);

  /**
   * Executes the closure. The arguments must be instances of [InstanceMirror], [num], [String], or
   * [bool].
   */
  Future<InstanceMirror> applyAsync(Object[] positionalArguments);

  /**
   * Executes the closure. The arguments must be instances of [InstanceMirror], [num], [String], or
   * [bool].
   */
  Future<InstanceMirror> applyAsync(Object[] positionalArguments,
      Map<Symbol, Object> namedArguments);

  /**
   * Looks up the value of a name in the scope of the closure. The result is a mirror on that value.
   */
  Future<InstanceMirror> findInContext(Symbol name);

  /**
   * A mirror on the function associated with this closure.
   */
  MethodMirror getFunction();

  /**
   * The source code for this closure, if available. Otherwise null. TODO(turnidge): Would this just
   * be available in function?
   */
  String getSource();
}

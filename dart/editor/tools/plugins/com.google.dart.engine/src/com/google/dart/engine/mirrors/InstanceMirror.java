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

import com.google.dart.engine.mirrors.core.Invocation;

/**
 * An {@code InstanceMirror} reflects an instance of a Dart language object.
 */
public interface InstanceMirror extends ObjectMirror {
  /**
   * Perform {@link #invocation} on reflectee. If reflectee doesn't support the invocation, its
   * noSuchMethod method will be called with either {@link #invocation} or another equivalent
   * instance of {@link Invocation}.
   */
  void delegate(Invocation invocation);

  /**
   * If the [InstanceMirror] reflects an instance it is meaningful to have a local reference to, we
   * provide access to the actual instance here. If you access {@link #getReflectee} when
   * {@link #hasReflectee} is {@code false}, an exception is thrown.
   */
  Object getReflectee();

  /**
   * A mirror on the type of the reflectee.
   */
  ClassMirror getType();

  /**
   * Does {@link #getReflectee} return the instance reflected by this mirror? This will always be
   * true in the local case (reflecting instances in the same isolate), but only true in the remote
   * case if this mirror reflects a simple value. A value is simple if one of the following holds: -
   * the value is null - the value is of type [num] - the value is of type [bool] - the value is of
   * type [String]
   */
  boolean hasReflectee();
}

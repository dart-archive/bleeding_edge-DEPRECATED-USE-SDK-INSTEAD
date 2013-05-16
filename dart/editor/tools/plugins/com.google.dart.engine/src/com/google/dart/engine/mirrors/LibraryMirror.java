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

import java.net.URI;
import java.util.Map;

/**
 * A {@code LibraryMirror} reflects a Dart language library, providing access to the variables,
 * functions, and classes of the library.
 */
public interface LibraryMirror extends DeclarationMirror, ObjectMirror {
  /**
   * An immutable map from names to mirrors for all class declarations in this library.
   */
  Map<Symbol, ClassMirror> getClasses();

  /**
   * An immutable map from names to mirrors for all function, getter, and setter declarations in
   * this library.
   */
  Map<Symbol, MethodMirror> getFunctions();

  /**
   * An immutable map from names to mirrors for all getter declarations in this library.
   */
  Map<Symbol, MethodMirror> getGetters();

  /**
   * An immutable map from from names to mirrors for all members in this library. The members of a
   * library are its top-level classes, functions, variables, getters, and setters.
   */
  Map<Symbol, Mirror> getMembers();

  /**
   * An immutable map from names to mirrors for all setter declarations in this library.
   */
  Map<Symbol, MethodMirror> getSetters();

  /**
   * The absolute uri of the library.
   */
  URI getUri();

  /**
   * An immutable map from names to mirrors for all variable declarations in this library.
   */
  Map<Symbol, VariableMirror> getVariables();
}

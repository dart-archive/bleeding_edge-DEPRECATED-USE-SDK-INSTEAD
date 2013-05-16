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
 * A [ClassMirror] reflects a Dart language class.
 */
public interface ClassMirror extends TypeMirror, ObjectMirror {
  /**
   * An immutable map from names to mirrors for all constructor declarations for this type.
   */
  Map<Symbol, MethodMirror> getConstructors();

  /**
   * A mirror on the default factory class or null if there is none. TODO(turnidge): This functions
   * goes away after the class/interface changes.
   */
  ClassMirror getDefaultFactory();

  /**
   * An immutable map from names to mirrors for all getter declarations for this type.
   */
  Map<Symbol, MethodMirror> getGetters();

  /**
   * An immutable map from from names to mirrors for all members of this type. The members of a type
   * are its methods, fields, getters, and setters. Note that constructors and type variables are
   * not considered to be members of a type. This does not include inherited members.
   */
  Map<Symbol, Mirror> getMembers();

  /**
   * An immutable map from names to mirrors for all method, declarations for this type. This does
   * not include getters and setters.
   */
  Map<Symbol, MethodMirror> getMethods();

  /**
   * A mirror on the original declaration of this type. For most classes, they are their own
   * original declaration. For generic classes, however, there is a distinction between the original
   * class declaration, which has unbound type variables, and the instantiations of generic classes,
   * which have bound type variables.
   */
  ClassMirror getOriginalDeclaration();

  /**
   * An immutable map from names to mirrors for all setter declarations for this type.
   */
  Map<Symbol, MethodMirror> getSetters();

  /**
   * A mirror on the superclass on the reflectee. If this type is [:Object:] or a typedef, the
   * superClass will be null.
   */
  ClassMirror getSuperclass();

  /**
   * A list of mirrors on the superinterfaces of the reflectee.
   */
  ClassMirror[] getSuperinterfaces();

  /**
   * An immutable map from names to mirrors for all type arguments for this type. This map preserves
   * the order of declaration of the type variables.
   */
  Map<Symbol, TypeMirror> getTypeArguments();

  /**
   * An immutable map from names to mirrors for all type variables for this type. This map preserves
   * the order of declaration of the type variables.
   */
  Map<Symbol, TypeVariableMirror> getTypeVariables();

  /**
   * An immutable map from names to mirrors for all variable declarations for this type.
   */
  Map<Symbol, VariableMirror> getVariables();

  /**
   * Does this mirror represent a class? TODO(turnidge): This functions goes away after the
   * class/interface changes.
   */
  boolean isClass();

  /**
   * Is this the original declaration of this type? For most classes, they are their own original
   * declaration. For generic classes, however, there is a distinction between the original class
   * declaration, which has unbound type variables, and the instantiations of generic classes, which
   * have bound type variables.
   */
  boolean isOriginalDeclaration();

  /**
   * Invokes the named constructor and returns a mirror on the result. The arguments are objects
   * local to the current isolate
   */
  /* TODO(turnidge): Properly document.*/
  InstanceMirror newInstance(Symbol constructorName, Object[] positionalArguments);

  /**
   * Invokes the named constructor and returns a mirror on the result. The arguments are objects
   * local to the current isolate
   */
  /* TODO(turnidge): Properly document.*/
  InstanceMirror newInstance(Symbol constructorName, Object[] positionalArguments,
      Map<Symbol, Object> namedArguments);

  /**
   * Invokes the named constructor and returns a mirror on the result. The arguments must be
   * instances of [InstanceMirror], [num], [String] or [bool].
   */
  /* TODO(turnidge): Properly document.*/
  Future<InstanceMirror> newInstanceAsync(Symbol constructorName, Object[] positionalArguments);

  /**
   * Invokes the named constructor and returns a mirror on the result. The arguments must be
   * instances of [InstanceMirror], [num], [String] or [bool].
   */
  /* TODO(turnidge): Properly document.*/
  Future<InstanceMirror> newInstanceAsync(Symbol constructorName, Object[] positionalArguments,
      Map<Symbol, Object> namedArguments);
}

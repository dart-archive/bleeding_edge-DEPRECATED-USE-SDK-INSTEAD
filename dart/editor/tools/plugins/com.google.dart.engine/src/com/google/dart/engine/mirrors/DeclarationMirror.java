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
 * A [DeclarationMirror] reflects some entity declared in a Dart program.
 */
public interface DeclarationMirror extends Mirror {
  /**
   * Is this declaration private? Note that for libraries, this will be [:false:].
   */
  boolean isPrivate();

  /**
   * The source location of this Dart language entity.
   */
  SourceLocation getLocation();

  /**
   * A mirror on the owner of this function. This is the declaration immediately surrounding the
   * reflectee. Note that for libraries, the owner will be [:null:].
   */
  DeclarationMirror getOwner();

  /**
   * The fully-qualified name for this Dart language entity. This name is qualified by the name of
   * the owner. For instance, the qualified name of a method 'method' in class 'Class' in library
   * 'library' is 'library.Class.method'. TODO(turnidge): Specify whether this name is unique.
   * Currently this is a gray area due to lack of clarity over whether library names are unique.
   */
  Symbol getQualifiedName();

  /**
   * The simple name for this Dart language entity. The simple name is in most cases the the
   * identifier name of the entity, such as 'method' for a method [:void method() {...}:] or
   * 'mylibrary' for a [:#library('mylibrary');:] declaration.
   */
  Symbol getSimpleName();

  /**
   * Is this declaration top-level? This is defined to be equivalent to: [:mirror.owner != null &&
   * mirror.owner is LibraryMirror:]
   */
  boolean isTopLevel();
}

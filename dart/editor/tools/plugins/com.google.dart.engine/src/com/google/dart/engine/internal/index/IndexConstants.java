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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.UniverseElement;

/**
 * Constants used when populating and accessing the index.
 * 
 * @coverage dart.engine.index
 */
public interface IndexConstants {
//  /**
//   * An {@link Resource} for unresolved elements.
//   */
//  Resource DYNAMIC = new Resource("--dynamic--");

  /**
   * An element used to represent the universe.
   */
  Element UNIVERSE = UniverseElement.INSTANCE;

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a class at a specific location (the right operand).
   */
  Relationship DEFINES_CLASS = Relationship.getRelationship("defines-class");

//  /**
//   * The relationship used to indicate that a container (the left-operand) contains the definition
//   * of a field at a specific location (the right operand).
//   */
//  Relationship DEFINES_FIELD = Relationship.getRelationship("defines-field");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a function at a specific location (the right operand).
   */
  Relationship DEFINES_FUNCTION = Relationship.getRelationship("defines-function");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a class type alias at a specific location (the right operand).
   */
  Relationship DEFINES_CLASS_ALIAS = Relationship.getRelationship("defines-class-alias");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a function type at a specific location (the right operand).
   */
  Relationship DEFINES_FUNCTION_TYPE = Relationship.getRelationship("defines-function-type");

//  /**
//   * The relationship used to indicate that a container (the left-operand) contains the definition
//   * of a method at a specific location (the right operand).
//   */
//  Relationship DEFINES_METHOD = Relationship.getRelationship("defines-method");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a method at a specific location (the right operand).
   */
  Relationship DEFINES_VARIABLE = Relationship.getRelationship("defines-variable");

//  /**
//   * The relationship used to indicate that a type (the left-operand) extends (subtypes) a type at a
//   * specific location (the right operand).
//   */
//  Relationship EXTENDS = Relationship.getRelationship("extends");

//  /**
//   * The relationship used to indicate that a type (the left-operand) implements a type at a
//   * specific location (the right operand).
//   */
//  Relationship IMPLEMENTS = Relationship.getRelationship("implements");

  /**
   * The relationship used to indicate that a name (the left-operand) is defined at a specific
   * location (the right operand).
   */
  Relationship IS_DEFINED_BY = Relationship.getRelationship("is-defined-by");

  /**
   * The relationship used to indicate that a type (the left-operand) is extended by a type at a
   * specific location (the right operand).
   */
  Relationship IS_EXTENDED_BY = Relationship.getRelationship("is-extended-by");

  /**
   * The relationship used to indicate that a type (the left-operand) is implemented by a type at a
   * specific location (the right operand).
   */
  Relationship IS_IMPLEMENTED_BY = Relationship.getRelationship("is-implemented-by");

  /**
   * The relationship used to indicate that a type (the left-operand) is mixed into a type at a
   * specific location (the right operand).
   */
  Relationship IS_MIXED_IN_BY = Relationship.getRelationship("is-mixed-in-by");

  /**
   * The relationship used to indicate that a parameter or variable (the left-operand) is read at a
   * specific location (the right operand).
   */
  Relationship IS_READ_BY = Relationship.getRelationship("is-read-by");

  /**
   * The relationship used to indicate that a parameter or variable (the left-operand) is both read
   * and modified at a specific location (the right operand).
   */
  Relationship IS_READ_WRITTEN_BY = Relationship.getRelationship("is-read-written-by");

  /**
   * The relationship used to indicate that a parameter or variable (the left-operand) is modified
   * (assigned to) at a specific location (the right operand).
   */
  Relationship IS_WRITTEN_BY = Relationship.getRelationship("is-written-by");

//  /**
//   * The relationship used to indicate that a method (the left-operand) is overridden by a method at
//   * a specific location (the right operand).
//   */
//  Relationship IS_OVERRIDDEN_BY = Relationship.getRelationship("is-overridden-by");

  /**
   * The relationship used to indicate that an element (the left-operand) is referenced at a
   * specific location (the right operand). This is used for everything except read/write operations
   * for fields, parameters, and variables. Those use either {@link #IS_REFERENCED_BY_QUALIFIED},
   * {@link #IS_REFERENCED_BY_UNQUALIFIED}, {@link #IS_READ_BY}, {@link #IS_WRITTEN_BY} or
   * {@link #IS_READ_WRITTEN_BY}, as appropriate.
   */
  Relationship IS_REFERENCED_BY = Relationship.getRelationship("is-referenced-by");

  /**
   * The relationship used to indicate that an {@link NameElementImpl} (the left-operand) is
   * referenced at a specific location (the right operand). This is used for qualified resolved
   * references to methods and fields.
   */
  Relationship IS_REFERENCED_BY_QUALIFIED_RESOLVED = Relationship.getRelationship("is-referenced-by_qualified-resolved");
  /**
   * The relationship used to indicate that an {@link NameElementImpl} (the left-operand) is
   * referenced at a specific location (the right operand). This is used for qualified unresolved
   * references to methods and fields.
   */
  Relationship IS_REFERENCED_BY_QUALIFIED_UNRESOLVED = Relationship.getRelationship("is-referenced-by_qualified-unresolved");

  /**
   * The relationship used to indicate that an element (the left-operand) is referenced at a
   * specific location (the right operand). This is used for field accessors and methods.
   */
  Relationship IS_REFERENCED_BY_QUALIFIED = Relationship.getRelationship("is-referenced-by-qualified");

  /**
   * The relationship used to indicate that an element (the left-operand) is referenced at a
   * specific location (the right operand). This is used for field accessors and methods.
   */
  Relationship IS_REFERENCED_BY_UNQUALIFIED = Relationship.getRelationship("is-referenced-by-unqualified");

  /**
   * The relationship used to indicate that an element (the left-operand) is invoked at a specific
   * location (the right operand). This is used for functions.
   */
  Relationship IS_INVOKED_BY = Relationship.getRelationship("is-invoked-by");

  /**
   * The relationship used to indicate that an element (the left-operand) is invoked at a specific
   * location (the right operand). This is used for methods.
   */
  Relationship IS_INVOKED_BY_QUALIFIED = Relationship.getRelationship("is-invoked-by-qualified");

  /**
   * The relationship used to indicate that an element (the left-operand) is invoked at a specific
   * location (the right operand). This is used for methods.
   */
  Relationship IS_INVOKED_BY_UNQUALIFIED = Relationship.getRelationship("is-invoked-by-unqualified");

  /**
   * Reference to some {@link AngularElement}.
   */
  Relationship ANGULAR_REFERENCE = Relationship.getRelationship("angular-reference");

  /**
   * Reference to some closing tag of an XML element.
   */
  Relationship ANGULAR_CLOSING_TAG_REFERENCE = Relationship.getRelationship("angular-closing-tag-reference");
}

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
package com.google.dart.tools.core.internal.index.contributor;

import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.Resource;

/**
 * The interface <code>IndexConstants</code> defines constants used when populating and accessing
 * the index, such as constants for the relationships and attributes.
 */
public interface IndexConstants {
  /**
   * An element used to represent the workspace.
   */
  public static final Element UNIVERSE = new Element(new Resource("--universe--"), "--universe--");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a class at a specific location (the right operand).
   */
  public static final Relationship DEFINES_CLASS = Relationship.getRelationship("defines-class");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a field at a specific location (the right operand).
   */
  public static final Relationship DEFINES_FIELD = Relationship.getRelationship("defines-field");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a function at a specific location (the right operand).
   */
  public static final Relationship DEFINES_FUNCTION = Relationship.getRelationship("defines-function");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a function type at a specific location (the right operand).
   */
  public static final Relationship DEFINES_FUNCTION_TYPE = Relationship.getRelationship("defines-function-type");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of an interface at a specific location (the right operand).
   */
  public static final Relationship DEFINES_INTERFACE = Relationship.getRelationship("defines-interface");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a method at a specific location (the right operand).
   */
  public static final Relationship DEFINES_METHOD = Relationship.getRelationship("defines-method");

  /**
   * The relationship used to indicate that a container (the left-operand) contains the definition
   * of a method at a specific location (the right operand).
   */
  public static final Relationship DEFINES_VARIABLE = Relationship.getRelationship("defines-variable");

  /**
   * The relationship used to indicate that a type (the left-operand) extends (subtypes) a type at a
   * specific location (the right operand).
   */
  public static final Relationship EXTENDS = Relationship.getRelationship("extends");

  /**
   * The relationship used to indicate that a type (the left-operand) implements a type at a
   * specific location (the right operand).
   */
  public static final Relationship IMPLEMENTS = Relationship.getRelationship("implements");

  /**
   * The relationship used to indicate that a field, parameter, or variable (the left-operand) is
   * accessed at a specific location (the right operand). With some qualifier.
   */
  public static final Relationship IS_ACCESSED_BY_QUALIFIED = Relationship.getRelationship("is-accessed-by-qualified");

  /**
   * The relationship used to indicate that a field, parameter, or variable (the left-operand) is
   * accessed at a specific location (the right operand). Without qualifier.
   */
  public static final Relationship IS_ACCESSED_BY_UNQUALIFIED = Relationship.getRelationship("is-accessed-by-unqualified");

  /**
   * The relationship used to indicate that a type (the left-operand) is extended (subtyped) by a
   * type at a specific location (the right operand).
   */
  public static final Relationship IS_EXTENDED_BY = Relationship.getRelationship("is-extended-by");

  /**
   * The relationship used to indicate that a type (the left-operand) is implemented by a type at a
   * specific location (the right operand).
   */
  public static final Relationship IS_IMPLEMENTED_BY = Relationship.getRelationship("is-implemented-by");

  /**
   * The relationship used to indicate that a field, parameter, or variable (the left-operand) is
   * modified (assigned to) at a specific location (the right operand). With some qualifier.
   */
  public static final Relationship IS_MODIFIED_BY_QUALIFIED = Relationship.getRelationship("is-modified-by-qualified");

  /**
   * The relationship used to indicate that a field, parameter, or variable (the left-operand) is
   * modified (assigned to) at a specific location (the right operand). Without qualifier.
   */
  public static final Relationship IS_MODIFIED_BY_UNQUALIFIED = Relationship.getRelationship("is-modified-by-unqualified");

  /**
   * The relationship used to indicate that a method (the left-operand) is overridden by a method at
   * a specific location (the right operand).
   */
  public static final Relationship IS_OVERRIDDEN_BY = Relationship.getRelationship("is-overridden-by");

  /**
   * The relationship used to indicate that an element (the left-operand) is referenced at a
   * specific location (the right operand). This is used for everything except fields, parameters,
   * and variables. Those use either {@link #IS_ACCESSED_BY_QUALIFIED},
   * {@link #IS_ACCESSED_BY_UNQUALIFIED}, {@link #IS_MODIFIED_BY_QUALIFIED} or
   * {@link #IS_MODIFIED_BY_UNQUALIFIED}, as appropriate.
   */
  public static final Relationship IS_REFERENCED_BY = Relationship.getRelationship("is-referenced-by");

  /**
   * The relationship used to indicate that an element (the left-operand) is invoked at a specific
   * location (the right operand). This is used for methods and functions.
   */
  public static final Relationship IS_INVOKED_BY_QUALIFIED = Relationship.getRelationship("is-invoked-by-qualified");

  /**
   * The relationship used to indicate that an element (the left-operand) is invoked at a specific
   * location (the right operand). This is used for methods and functions.
   */
  public static final Relationship IS_INVOKED_BY_UNQUALIFIED = Relationship.getRelationship("is-invoked-by-unqualified");
}

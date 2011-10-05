/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.indexer;

import com.google.dart.indexer.index.layers.LayerId;

/**
 * The interface <code>DartLayers</code> defines constants representing all of the layers in the
 * index.
 */
public interface DartLayers {
  /**
   * The identifier for a reverse edges layer in which the source can be any element and the
   * destination is a category, and the source is a member of the destination.
   */
  public final static LayerId ELEMENTS_BY_CATEGORY = new LayerId(
      "com.google.indexer.layer.dart.elementsByCategory");

  /**
   * The identifier for a reverse edges layer in which the source can be any element and the
   * destination is a field, and the source is a reference to the destination.
   */
  public final static LayerId FIELD_ACCESSES = new LayerId(
      "com.google.indexer.layer.dart.fieldReferences");

  /**
   * The identifier for a reverse edges layer in which the source can be any element and the
   * destination is a function, method, or constructor, and the source is an invocation of the
   * destination.
   */
  public final static LayerId METHOD_CALLS = new LayerId(
      "com.google.indexer.layer.dart.functionReferences");

  /**
   * The identifier for a reverse edges layer in which both the source and the destination are
   * methods (which includes constructors), and the source overrides the destination.
   */
  public final static LayerId METHOD_OVERRIDE = new LayerId(
      "com.google.indexer.layer.dart.methodOverride");

  /**
   * The identifier for a bi-directional layer in which both the source and the destination are
   * types, and the source is a subtype of the destination.
   */
  public final static LayerId TYPE_HIERARCHY = new LayerId(
      "com.google.indexer.layer.dart.typeHierarchy");

  /**
   * The identifier for a reverse edges layer in which the source can be any element and the
   * destination is a type, and the source is a reference to the destination.
   */
  public final static LayerId TYPE_REFERENCES = new LayerId(
      "com.google.indexer.layer.dart.typeReferences");
}

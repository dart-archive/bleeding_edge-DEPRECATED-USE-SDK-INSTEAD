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
package com.google.dart.tools.core.model;


/**
 * A type hierarchy provides navigations between a type and its resolved supertypes and subtypes for
 * a specific type or for all types within a region. Supertypes may extend outside of the type
 * hierarchy's region in which it was created such that the root of the hierarchy is always
 * included.
 * <p>
 * A type hierarchy is static and can become stale. Although consistent when created, it does not
 * automatically track changes in the model. As changes in the model potentially invalidate the
 * hierarchy, change notifications are sent to registered <code>TypeHierarchyChangedListener</code>
 * s. Listeners should use the <code>exists</code> method to determine if the hierarchy has become
 * completely invalid (for example, when the type or project the hierarchy was created on has been
 * removed). To refresh a hierarchy, use the <code>refresh</code> method.
 * <p>
 * The type hierarchy may contain cycles due to malformed supertype declarations. Most type
 * hierarchy queries are oblivious to cycles; the <code>getAll* </code> methods are implemented such
 * that they are unaffected by cycles.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @coverage dart.tools.core.model
 */
public interface TypeHierarchy {
  /**
   * Returns whether the given type is part of this hierarchy.
   * 
   * @param type the given type
   * @return true if the given type is part of this hierarchy, false otherwise
   */
  public boolean contains(Type type);

  /**
   * Returns whether the type and project this hierarchy was created on exist.
   * 
   * @return true if the type and project this hierarchy was created on exist, false otherwise
   */
  public boolean exists();

  /**
   * Returns all resolved superclasses of the given class, in bottom-up order. An empty array is
   * returned if there are no resolved superclasses for the given class.
   * <p>
   * NOTE: once a type hierarchy has been created, it is more efficient to query the hierarchy for
   * superclasses than to query a class recursively up the superclass chain. Querying an element
   * performs a dynamic resolution, whereas the hierarchy returns a pre-computed result.
   * 
   * @param type the given type
   * @return all resolved superclasses of the given class, in bottom-up order, an empty array if
   *         none.
   */
  public Type[] getAllSuperclasses(Type type);

  /**
   * Returns all resolved superinterfaces (direct and indirect) of the given type. If the given type
   * is a class, this includes all superinterfaces of all superclasses. An empty array is returned
   * if there are no resolved superinterfaces for the given type.
   * <p>
   * NOTE: once a type hierarchy has been created, it is more efficient to query the hierarchy for
   * superinterfaces than to query a type recursively. Querying an element performs a dynamic
   * resolution, whereas the hierarchy returns a pre-computed result.
   * 
   * @param type the given type
   * @return all resolved superinterfaces (direct and indirect) of the given type, an empty array if
   *         none
   */
  public Type[] getAllSuperInterfaces(Type type);

  /**
   * Returns the direct resolved subtypes of the given type, in no particular order, limited to the
   * types in this type hierarchy's graph. If the type is a class, this returns the resolved
   * subclasses. If the type is an interface, this returns both the classes which implement the
   * interface and the interfaces which extend it.
   * 
   * @param type the given type
   * @return the direct resolved subtypes of the given type limited to the types in this type
   *         hierarchy's graph
   */
  public Type[] getSubtypes(Type type);

  /**
   * Returns the resolved superclass of the given class, or <code>null</code> if the given class has
   * no superclass, the superclass could not be resolved, or if the given type is an interface.
   * 
   * @param type the given type
   * @return the resolved superclass of the given class, or <code>null</code> if the given class has
   *         no superclass, the superclass could not be resolved, or if the given type is an
   *         interface
   */
  public Type getSuperclass(Type type);
}

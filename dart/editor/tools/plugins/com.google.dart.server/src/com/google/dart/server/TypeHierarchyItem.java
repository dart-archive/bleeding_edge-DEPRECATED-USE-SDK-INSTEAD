/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

/**
 * The interface {@code TypeHierarchyItem} defines the behavior of objects representing an item in a
 * type hierarchy.
 * 
 * @coverage dart.server
 */
public interface TypeHierarchyItem {
  /**
   * An empty array of hierarchy items.
   */
  TypeHierarchyItem[] EMPTY_ARRAY = new TypeHierarchyItem[0];

  /**
   * Return the class element associated with this item. Not {@code null}.
   * 
   * @return the class element associated with this item
   */
  public Element getClassElement();

  /**
   * Return the type that is extended by this type, {@code null} if this item is {@code Object}.
   * 
   * @return the type that is extended by this type
   */
  public TypeHierarchyItem getExtendedType();

  /**
   * Return the types that are implemented by this type, {@code null} if not a super item.
   * 
   * @return the types that are implemented by this type
   */
  public TypeHierarchyItem[] getImplementedTypes();

  /**
   * Return the member element associated with this item. May be {@code null} if this type does not
   * define the member which hierarchy is requested.
   * 
   * @return the member element associated with this item
   */
  public Element getMemberElement();

  /**
   * Return the types that are mixed into this type, {@code null} if not a super item.
   * 
   * @return the types that are mixed into this type
   */
  public TypeHierarchyItem[] getMixedTypes();

  /**
   * Return the subtypes of this type, may be empty, but not {@code null}.
   * 
   * @return the subtypes of this type
   */
  public TypeHierarchyItem[] getSubtypes();
}

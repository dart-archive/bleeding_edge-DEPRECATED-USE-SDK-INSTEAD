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
   * Return the best name to display. If the display name is {@code null}, then the name from the
   * {@link #getClassElement()} is returned, otherwise the {@link #getDisplayName()} is returned.
   * 
   * @return the best name to display
   */
  public String getBestName();

  /**
   * Return the class element represented by this item. Not {@code null}.
   * 
   * @return the class element represented by this item
   */
  public Element getClassElement();

  /**
   * Return the name to be displayed for the class. This field will be omitted if the display name
   * is the same as the name of the element. The display name is different if there is additional
   * type information to be displayed, such as type arguments.
   * 
   * @return the name to be displayed for the class
   */
  public String getDisplayName();

  /**
   * Return the items representing the interfaces implemented by this class. The list will be empty
   * if there are no implemented interfaces.
   * 
   * @return the items representing the interfaces implemented by this class
   */
  public TypeHierarchyItem[] getInterfaces();

  /**
   * The member in the class corresponding to the member on which the hierarchy was requested. This
   * field will be omitted if the hierarchy was not requested for a member or if the class does not
   * have a corresponding member.
   * 
   * @return the member in the class corresponding to the member on which the hierarchy was
   *         requested
   */
  public Element getMemberElement();

  /**
   * Return the items representing the mixins referenced by this class. The list will be empty if
   * there are no classes mixed in to this class.
   * 
   * @return the items representing the mixins referenced by this class
   */
  public TypeHierarchyItem[] getMixins();

  /**
   * Return the items representing the subtypes of this class. The list will be empty if there are
   * no subtypes or if this item represents a supertype of the pivot type.
   * 
   * @return the items representing the subtypes of this class
   */
  public TypeHierarchyItem[] getSubclasses();

  /**
   * The item representing the superclass of this class. This field will be omitted if this item
   * represents the class Object
   * 
   * @return the item representing the superclass of this class
   */
  public TypeHierarchyItem getSuperclass();
}

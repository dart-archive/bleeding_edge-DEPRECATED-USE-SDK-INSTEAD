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
package com.google.dart.engine.index;

import com.google.dart.engine.element.Element;

/**
 * The interface <code>RelationshipCallback</code> defines the behavior of objects that are invoked
 * with the results of a query about a given relationship.
 * 
 * @coverage dart.engine.index
 */
public interface RelationshipCallback {
  /**
   * This method is invoked when the locations that have a specified relationship with a specified
   * element are available. For example, if the element is a field and the relationship is the
   * is-referenced-by relationship, then this method will be invoked with each location at which the
   * field is referenced.
   * 
   * @param element the {@link Element} that has the relationship with the locations
   * @param relationship the relationship between the given element and the locations
   * @param locations the locations that were found
   */
  public void hasRelationships(Element element, Relationship relationship, Location[] locations);
}

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

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;

/**
 * Relationship between an element and a location. Relationships are identified by a globally unique
 * identifier.
 * 
 * @coverage dart.engine.index
 */
public class Relationship {
  /**
   * The unique identifier for this relationship.
   */
  private final String uniqueId;

  /**
   * A table mapping relationship identifiers to relationships.
   */
  private static final Map<String, Relationship> RelationshipMap = Maps.newHashMap();

  /**
   * Return the relationship with the given unique identifier.
   * 
   * @param uniqueId the unique identifier for the relationship
   * @return the relationship with the given unique identifier
   */
  public static Relationship getRelationship(String uniqueId) {
    synchronized (RelationshipMap) {
      Relationship relationship = RelationshipMap.get(uniqueId);
      if (relationship == null) {
        relationship = new Relationship(uniqueId);
        RelationshipMap.put(uniqueId, relationship);
      }
      return relationship;
    }
  }

  /**
   * @return all registered {@link Relationship}s.
   */
  public static Collection<Relationship> values() {
    return RelationshipMap.values();
  }

  /**
   * Initialize a newly created relationship to have the given unique identifier.
   * 
   * @param uniqueId the unique identifier for this relationship
   */
  private Relationship(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  /**
   * Return the unique identifier for this relationship.
   * 
   * @return the unique identifier for this relationship
   */
  public String getIdentifier() {
    return uniqueId;
  }

  @Override
  public String toString() {
    return uniqueId;
  }
}

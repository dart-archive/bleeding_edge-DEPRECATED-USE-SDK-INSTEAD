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
package com.google.dart.tools.core.index;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class <code>Attribute</code> represent an attribute defined for an element.
 * Attributes are identified by a globally unique identifier.
 */
public class Attribute {
  /**
   * The unique identifier for this attribute.
   */
  private String attributeId;

  /**
   * A table mapping attribute identifiers to attributes.
   */
  private static Map<String, Attribute> AttributeMap = new HashMap<String, Attribute>();

  /**
   * Return the attribute with the given unique identifier.
   * 
   * @param attributeId the unique identifier for the attribute
   * @return the attribute with the given unique identifier
   */
  public static Attribute getAttribute(String attributeId) {
    synchronized (AttributeMap) {
      Attribute attribute = AttributeMap.get(attributeId);
      if (attribute == null) {
        attribute = new Attribute(attributeId);
        AttributeMap.put(attributeId, attribute);
      }
      return attribute;
    }
  }

  /**
   * Initialize a newly created attribute to have the given unique identifier.
   * 
   * @param uniqueId the unique identifier for this attribute
   */
  private Attribute(String uniqueId) {
    this.attributeId = uniqueId;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Attribute)) {
      return false;
    }
    return attributeId.equals(((Attribute) object).attributeId);
  }

  /**
   * Return the unique identifier for this attribute.
   * 
   * @return the unique identifier for this attribute
   */
  public String getIdentifier() {
    return attributeId;
  }

  @Override
  public int hashCode() {
    return attributeId.hashCode();
  }

  @Override
  public String toString() {
    return attributeId;
  }
}

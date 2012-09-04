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

import com.google.common.base.Objects;

/**
 * Instances of the class <code>Element</code> represent a program element (such as a type, field,
 * or method) within a resource.
 */
public final class Element {
  /**
   * The resource containing this element.
   */
  private Resource resource;

  /**
   * The resource unique identifier for this element.
   */
  private String elementId;

  /**
   * Initialize a newly create element to have the given identifier and be contained in the given
   * resource.
   * 
   * @param resource the resource containing this element
   * @param elementId the resource unique identifier for this element
   */
  public Element(Resource resource, String elementId) {
    this.resource = resource;
    this.elementId = elementId;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Element)) {
      return false;
    }
    Element element = (Element) object;
    return Objects.equal(element.resource, resource) && Objects.equal(element.elementId, elementId);
  }

  /**
   * Return the resource unique identifier for this element.
   * 
   * @return the resource unique identifier for this element
   */
  public String getElementId() {
    return elementId;
  }

  /**
   * Return the resource containing this element.
   * 
   * @return the resource containing this element
   */
  public Resource getResource() {
    return resource;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(resource, elementId);
  }

  @Override
  public String toString() {
    return elementId + " in " + resource;
  }
}

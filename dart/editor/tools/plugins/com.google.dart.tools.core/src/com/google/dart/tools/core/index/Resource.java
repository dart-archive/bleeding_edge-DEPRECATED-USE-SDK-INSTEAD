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

/**
 * Instances of the class <code>Resource</code> represent a resource that contains elements.
 * Resources conceptually correspond to files, but are not required to be actual files in a file
 * system.
 */
public final class Resource {
  /**
   * The globally unique identifier for this resource.
   */
  private String resourceId;

  /**
   * Initialize a newly create resource to have the given identifier.
   * 
   * @param resourceId the globally unique identifier for this resource
   */
  public Resource(String resourceId) {
    this.resourceId = resourceId;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Resource)) {
      return false;
    }
    return resourceId.equals(((Resource) object).resourceId);
  }

  /**
   * Return the globally unique identifier for this resource.
   * 
   * @return the globally unique identifier for this resource
   */
  public String getResourceId() {
    return resourceId;
  }

  @Override
  public int hashCode() {
    return resourceId.hashCode();
  }

  @Override
  public String toString() {
    return resourceId;
  }
}

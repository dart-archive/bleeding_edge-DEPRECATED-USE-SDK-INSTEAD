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
package com.google.dart.tools.search.internal.ui.text;

/**
 * A file resource (used for backing matches).
 */
public abstract class FileResource<T> {

  private final T resource;

  /**
   * Create an instance.
   * 
   * @param resource the resource
   */
  FileResource(T resource) {
    this.resource = resource;
  }

  /**
   * Get the resource.
   * 
   * @return the resource
   */
  public T getResource() {
    return resource;
  }

}

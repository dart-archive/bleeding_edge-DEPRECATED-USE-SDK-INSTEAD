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
package com.google.dart.engine.internal.cache;

/**
 * Instances of the class {@code DataDescriptor} are immutable constants representing data that can
 * be stored in the cache.
 */
public class DataDescriptor<E> {
  /**
   * The name of the descriptor, used for debugging purposes.
   */
  private String name;

  /**
   * Initialize a newly created descriptor to have the given name.
   * 
   * @param name the name of the descriptor
   */
  public DataDescriptor(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}

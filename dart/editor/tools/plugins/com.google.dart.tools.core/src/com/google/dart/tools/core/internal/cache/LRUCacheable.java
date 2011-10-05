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
package com.google.dart.tools.core.internal.cache;

/**
 * The interface <code>LRUCacheable</code> defines the behavior of objects that can have a memory
 * size associated with them. It is used by the class {@link LRUCache} to manage cached objects.
 * Elements added to the cache that do not implement this interface are assumed to have a footprint
 * of one (1).
 */
public interface LRUCacheable {
  /**
   * Return the space the receiver consumes in an LRU Cache. The default space value is 1.
   * 
   * @return int the amount of cache space taken by the receiver
   */
  public int getCacheFootprint();
}

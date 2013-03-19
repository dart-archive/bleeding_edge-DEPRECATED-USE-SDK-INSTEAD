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
package com.google.dart.engine.internal.context;

/**
 * The enumeration {@code CacheState} defines the possible states of cached data.
 */
public enum CacheState {
  /**
   * A state representing the fact that the data was up-to-date but flushed from the cache in order
   * to control memory usage.
   */
  FLUSHED,

  /**
   * A state representing the fact that the data was removed from the cache because it was invalid
   * and needs to be recomputed.
   */
  INVALID,

  /**
   * A state representing the fact that the data is in the cache and valid.
   */
  VALID;
}

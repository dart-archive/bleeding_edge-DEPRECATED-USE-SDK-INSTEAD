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
 * The enumeration {@code CacheState} defines the possible states of cached data.
 */
public enum CacheState {
  /**
   * The data is not in the cache and the last time an attempt was made to compute the data an
   * exception occurred, making it pointless to attempt.
   * <p>
   * Valid Transitions:
   * <ul>
   * <li>{@link #INVALID} if a source was modified that might cause the data to be computable</li>
   * </ul>
   */
  ERROR,

  /**
   * The data is not in the cache because it was flushed from the cache in order to control memory
   * usage. If the data is recomputed, results do not need to be reported.
   * <p>
   * Valid Transitions:
   * <ul>
   * <li>{@link #IN_PROCESS} if the data is being recomputed</li>
   * <li>{@link #INVALID} if a source was modified that causes the data to need to be recomputed</li>
   * </ul>
   */
  FLUSHED,

  /**
   * The data might or might not be in the cache but is in the process of being recomputed.
   * <p>
   * Valid Transitions:
   * <ul>
   * <li>{@link #ERROR} if an exception occurred while trying to compute the data</li>
   * <li>{@link #VALID} if the data was successfully computed and stored in the cache</li>
   * </ul>
   */
  IN_PROCESS,

  /**
   * The data is not in the cache and needs to be recomputed so that results can be reported.
   * <p>
   * Valid Transitions:
   * <ul>
   * <li>{@link #IN_PROCESS} if an attempt is being made to recompute the data</li>
   * </ul>
   */
  INVALID,

  /**
   * The data is in the cache and up-to-date.
   * <p>
   * Valid Transitions:
   * <ul>
   * <li>{@link #FLUSHED} if the data is removed in order to manage memory usage</li>
   * <li>{@link #INVALID} if a source was modified in such a way as to invalidate the previous data</li>
   * </ul>
   */
  VALID;
}

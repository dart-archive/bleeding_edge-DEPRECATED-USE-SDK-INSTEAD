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
 * The enumerated type {@code RetentionPriority} represents the priority of data in the cache in
 * terms of the desirability of retaining some specified data about a specified source.
 */
public enum RetentionPriority {
  /**
   * A priority indicating that a given piece of data can be removed from the cache without
   * reservation.
   */
  LOW,

  /**
   * A priority indicating that a given piece of data should not be removed from the cache unless
   * there are no sources for which the corresponding data has a lower priority. Currently used for
   * data that is needed in order to finish some outstanding analysis task.
   */
  MEDIUM,

  /**
   * A priority indicating that a given piece of data should not be removed from the cache.
   * Currently used for data related to a priority source.
   */
  HIGH;
}

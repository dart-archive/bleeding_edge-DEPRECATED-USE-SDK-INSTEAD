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
package com.google.dart.tools.core.internal.index.impl;

/**
 * Instances of the class <code>IndexPerformanceRecorder</code> record performance data related to
 * the index.
 */
public class IndexPerformanceRecorder {
  /**
   * The number of resources that have been indexed since the last time performance statistics were
   * cleared.
   */
  private int resourceCount = 0;

  /**
   * The number of milliseconds spent indexing since the last time this performance statistics were
   * cleared.
   */
  private long totalIndexTime = 0L;

  /**
   * The number of milliseconds spent resolving the AST structures since the last time this
   * performance statistics were cleared.
   */
  private long totalBindingTime = 0L;

  /**
   * Initialize a newly created index performance recorder.
   */
  public IndexPerformanceRecorder() {
    super();
  }

  /**
   * Clear any recorded performance statistics.
   */
  public void clear() {
    resourceCount = 0;
    totalIndexTime = 0L;
    totalBindingTime = 0L;
  }

  /**
   * Return the number of resources that have been indexed since the last time performance
   * statistics were cleared.
   * 
   * @return the number of resources that have been indexed
   */
  public int getResourceCount() {
    return resourceCount;
  }

  /**
   * Return the number of milliseconds spent resolving the AST structures since the last time this
   * performance statistics were cleared.
   * 
   * @return the number of milliseconds spent resolving the AST structures
   */
  public long getTotalBindingTime() {
    return totalBindingTime;
  }

  /**
   * Return the number of milliseconds spent indexing since the last time this performance
   * statistics were cleared.
   * 
   * @return the number of milliseconds spent indexing
   */
  public long getTotalIndexTime() {
    return totalIndexTime;
  }

  /**
   * Record that a single resource was indexed in the given amount of time, some portion of which
   * was the amount of time spent mapping the compiler's model back to the core's model.
   * 
   * @param indexTime the number of milliseconds spent indexing the resource
   * @param bindingTime the number of milliseconds spent resolving the AST structures while indexing
   *          the resource
   */
  public void recordIndexingTime(long indexTime, long bindingTime) {
    resourceCount++;
    totalIndexTime += indexTime;
    totalBindingTime += bindingTime;
  }
}

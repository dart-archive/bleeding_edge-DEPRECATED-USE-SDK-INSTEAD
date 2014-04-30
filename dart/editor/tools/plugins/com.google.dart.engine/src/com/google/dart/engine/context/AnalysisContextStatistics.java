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

package com.google.dart.engine.context;

import com.google.dart.engine.internal.cache.CacheState;
import com.google.dart.engine.source.Source;

/**
 * The interface {@code AnalysisContextStatistics} defines access to statistics about a single
 * {@link AnalysisContext}.
 */
public interface AnalysisContextStatistics {
  /**
   * Information about single piece of data in the cache.
   */
  public static interface CacheRow {
    /**
     * Return the number of entries whose state is {@link CacheState#ERROR}.
     */
    public int getErrorCount();

    /**
     * Return the number of entries whose state is {@link CacheState#FLUSHED}.
     */
    public int getFlushedCount();

    /**
     * Return the number of entries whose state is {@link CacheState#IN_PROCESS}.
     */
    public int getInProcessCount();

    /**
     * Return the number of entries whose state is {@link CacheState#INVALID}.
     */
    public int getInvalidCount();

    /**
     * Return the name of the data represented by this object.
     */
    public String getName();

    /**
     * Return the number of entries whose state is {@link CacheState#VALID}.
     */
    public int getValidCount();
  }

  /**
   * Information about a single partition in the cache.
   */
  public static interface PartitionData {
    /**
     * Return the number of entries in the partition that have an AST structure in one state or
     * another.
     */
    public int getAstCount();

    /**
     * Return the total number of entries in the partition.
     */
    public int getTotalCount();
  }

  /**
   * Return the statistics for each kind of cached data.
   */
  public CacheRow[] getCacheRows();

  /**
   * Return the exceptions that caused some entries to have a state of {@link CacheState#ERROR}.
   */
  public AnalysisException[] getExceptions();

  /**
   * Return information about each of the partitions in the cache.
   */
  public PartitionData[] getPartitionData();

  /**
   * Return an array containing all of the sources in the cache.
   */
  public Source[] getSources();
}

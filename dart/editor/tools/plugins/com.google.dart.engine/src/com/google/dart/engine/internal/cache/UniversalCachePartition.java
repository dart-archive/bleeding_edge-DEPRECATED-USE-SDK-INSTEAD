/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code UniversalCachePartition} implement a cache partition that contains
 * all sources not contained in other partitions.
 */
public class UniversalCachePartition extends CachePartition {
  /**
   * Initialize a newly created partition.
   * 
   * @param context the context that owns this partition
   * @param maxCacheSize the maximum number of sources for which AST structures should be kept in
   *          the cache
   * @param retentionPolicy the policy used to determine which pieces of data to remove from the
   *          cache
   */
  public UniversalCachePartition(InternalAnalysisContext context, int maxCacheSize,
      CacheRetentionPolicy retentionPolicy) {
    super(context, maxCacheSize, retentionPolicy);
  }

  @Override
  public boolean contains(Source source) {
    return true;
  }
}

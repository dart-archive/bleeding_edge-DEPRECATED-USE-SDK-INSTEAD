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
import com.google.dart.engine.sdk.DartSdk;

import java.util.HashMap;

/**
 * Instances of the class {@code PartitionManager} manage the partitions that can be shared between
 * analysis contexts.
 */
public class PartitionManager {
  /**
   * A table mapping SDK's to the partitions used for those SDK's.
   */
  private HashMap<DartSdk, SdkCachePartition> sdkPartitions = new HashMap<DartSdk, SdkCachePartition>();

  /**
   * The default cache size for a Dart SDK partition.
   */
  private static final int DEFAULT_SDK_CACHE_SIZE = 256;

  /**
   * Initialize a newly created partition manager.
   */
  public PartitionManager() {
    super();
  }

  /**
   * Clear any cached data being maintained by this manager.
   */
  public void clearCache() {
    sdkPartitions.clear();
  }

  /**
   * Return the partition being used for the given SDK, creating the partition if necessary.
   * 
   * @param sdk the SDK for which a partition is being requested
   * @return the partition being used for the given SDK
   */
  public SdkCachePartition forSdk(DartSdk sdk) {
    SdkCachePartition partition = sdkPartitions.get(sdk);
    if (partition == null) {
      partition = new SdkCachePartition(
          (InternalAnalysisContext) sdk.getContext(),
          DEFAULT_SDK_CACHE_SIZE);
      sdkPartitions.put(sdk, partition);
    }
    return partition;
  }
}

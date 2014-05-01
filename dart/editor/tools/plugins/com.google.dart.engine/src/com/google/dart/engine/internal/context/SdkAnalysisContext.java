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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.internal.cache.AnalysisCache;
import com.google.dart.engine.internal.cache.CachePartition;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.SourceFactory;

/**
 * Instances of the class {@code SdkAnalysisContext} implement an {@link AnalysisContext} that only
 * contains sources for a Dart SDK.
 */
public class SdkAnalysisContext extends AnalysisContextImpl {
  @Override
  protected AnalysisCache createCacheFromSourceFactory(SourceFactory factory) {
    if (factory == null) {
      return super.createCacheFromSourceFactory(factory);
    }
    DartSdk sdk = factory.getDartSdk();
    if (sdk == null) {
      throw new IllegalArgumentException(
          "The source factory for an SDK analysis context must have a DartUriResolver");
    }
    return new AnalysisCache(
        new CachePartition[] {AnalysisEngine.getInstance().getPartitionManager().forSdk(sdk)});
  }
}

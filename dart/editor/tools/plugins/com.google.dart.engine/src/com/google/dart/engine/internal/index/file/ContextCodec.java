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
package com.google.dart.engine.internal.index.file;

import com.google.common.collect.MapMaker;
import com.google.dart.engine.context.AnalysisContext;

import java.util.Map;

/**
 * A helper that encodes/decodes {@link AnalysisContext}s from/to integers.
 * 
 * @coverage dart.engine.index
 */
public class ContextCodec {
  /**
   * A table mapping contexts to their unique indices.
   */
  private final Map<AnalysisContext, Integer> contextToIndex = new MapMaker().weakKeys().makeMap();

  /**
   * A table mapping indices to the corresponding contexts.
   */
  private final Map<Integer, AnalysisContext> indexToContext = new MapMaker().weakValues().makeMap();

  /**
   * Returns the {@link AnalysisContext} that corresponds to the given index.
   */
  public AnalysisContext decode(int index) {
    return indexToContext.get(index);
  }

  /**
   * Returns an unique index for the given {@link AnalysisContext}.
   */
  public int encode(AnalysisContext context) {
    Integer index = contextToIndex.get(context);
    if (index == null) {
      index = indexToContext.size();
      contextToIndex.put(context, index);
      indexToContext.put(index, context);
    }
    return index;
  }
}

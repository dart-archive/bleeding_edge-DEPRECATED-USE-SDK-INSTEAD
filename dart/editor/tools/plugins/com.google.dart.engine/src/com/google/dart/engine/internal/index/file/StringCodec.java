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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * A helper that encodes/decodes {@link String}s from/to integers.
 * 
 * @coverage dart.engine.index
 */
public class StringCodec {
  /**
   * A table mapping names to their unique indices.
   */
  private final Map<String, Integer> nameToIndex = Maps.newHashMap();

  /**
   * A table mapping indices to the corresponding strings.
   */
  private final List<String> indexToName = Lists.newArrayList();

  /**
   * Returns the {@link String} that corresponds to the given index.
   */
  public String decode(int index) {
    return indexToName.get(index);
  }

  /**
   * Returns an unique index for the given {@link String}.
   */
  public int encode(String name) {
    Integer index = nameToIndex.get(name);
    if (index == null) {
      index = indexToName.size();
      nameToIndex.put(name, index);
      indexToName.add(name);
    }
    return index;
  }

  @VisibleForTesting
  public Map<String, Integer> getNameToIndex() {
    return nameToIndex;
  }
}

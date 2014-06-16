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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.internal.element.ElementLocationImpl;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * A helper that encodes/decodes {@link Element}s to/from integers.
 * 
 * @coverage dart.engine.index
 */
public class ElementCodec {
  private final StringCodec stringCodec;

  /**
   * A table mapping element locations (in form of integer arrays) into a single integer.
   */
  private final Map<List<Integer>, Integer> pathToIndex = Maps.newHashMap();

  /**
   * A list that works as a mapping of integers to element encodings (in form of integer arrays).
   */
  private final List<List<Integer>> indexToPath = Lists.newArrayList();

  public ElementCodec(StringCodec stringCodec) {
    this.stringCodec = stringCodec;
  }

  /**
   * Returns an {@link Element} that corresponds to the given location.
   * 
   * @param context the {@link AnalysisContext} to find {@link Element} in
   * @param index an integer corresponding to the {@link Element}
   * @return the {@link Element} or {@code null}
   */
  public Element decode(AnalysisContext context, int index) {
    List<Integer> pathList = indexToPath.get(index);
    int[] path = Ints.toArray(pathList);
    String[] components = getLocationComponents(path);
    ElementLocation location = new ElementLocationImpl(components);
    return context.getElement(location);
  }

  /**
   * Returns a unique integer that corresponds to the given {@link Element}.
   */
  public int encode(Element element) {
    int[] path = getLocationPath(element);
    List<Integer> pathList = Ints.asList(path);
    Integer index = pathToIndex.get(pathList);
    if (index == null) {
      index = indexToPath.size();
      pathToIndex.put(pathList, index);
      indexToPath.add(pathList);
    }
    return index;
  }

  private String[] getLocationComponents(int[] path) {
    int length = path.length;
    // localVariable@1234
    if (length != 0 && path[length - 1] < 0) {
      String[] components = new String[length - 1];
      for (int i = 0; i < length - 1; i++) {
        int componentIndex = path[i];
        components[i] = stringCodec.decode(componentIndex);
      }
      // TODO(scheglov) use for every component
      components[length - 2] += "@" + (-path[length - 1]);
      return components;
    }
    // normal element
    String[] components = new String[length];
    for (int i = 0; i < length; i++) {
      int componentIndex = path[i];
      components[i] = stringCodec.decode(componentIndex);
    }
    return components;
  }

  private int[] getLocationPath(Element element) {
    String[] components = element.getLocation().getComponents();
    int length = components.length;
    // localVariable@1234
    if (length != 0) {
      String lastComponent = components[length - 1];
      // TODO(scheglov) use for every component
      if (lastComponent.contains("@")) {
        int[] path = new int[length + 1];
        for (int i = 0; i < length; i++) {
          String component = components[i];
          if (i == length - 1) {
            String preAtString = StringUtils.substringBefore(lastComponent, "@");
            String atString = StringUtils.substringAfter(lastComponent, "@");
            path[i] = stringCodec.encode(preAtString);
            path[i + 1] = -1 * Integer.parseInt(atString);
          } else {
            path[i] = stringCodec.encode(component);
          }
        }
        return path;
      }
    }
    // normal element
    int[] path = new int[length];
    for (int i = 0; i < length; i++) {
      String component = components[i];
      path[i] = stringCodec.encode(component);
    }
    return path;
  }
}

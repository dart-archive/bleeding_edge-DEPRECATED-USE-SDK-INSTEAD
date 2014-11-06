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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.source.Source;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

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
  private final IntArrayToIntMap pathToIndex = new IntArrayToIntMap(10000, 0.75f);

  /**
   * A list that works as a mapping of integers to element encodings (in form of integer arrays).
   */
  private final List<int[]> indexToPath = Lists.newArrayList();

  public ElementCodec(StringCodec stringCodec) {
    this.stringCodec = stringCodec;
  }

  /**
   * Returns an {@link Element} that corresponds to the given location.
   * 
   * @param context the {@link AnalysisContext} to find {@link Element} in
   * @param id an integer corresponding to the {@link Element}
   * @return the {@link Element} or {@code null}
   */
  public Element decode(AnalysisContext context, int id) {
    int[] path = indexToPath.get(id);
    String[] components = getLocationComponents(path);
    ElementLocation location = new ElementLocationImpl(components);
    return context.getElement(location);
  }

  /**
   * Returns a unique integer that corresponds to the given {@link Element}.
   * 
   * @param forKey is {@code true} when "element" is a part of a key, so it should use file paths
   *          instead of {@link Element} location URIs.
   */
  public int encode(Element element, boolean forKey) {
    int[] path = getLocationPath(element, forKey);
    int index = pathToIndex.get(path, -1);
    if (index == -1) {
      index = indexToPath.size();
      pathToIndex.put(path, index);
      indexToPath.add(path);
    }
    return index;
  }

  /**
   * Returns an integer that corresponds to an approximated location of the given {@link Element}.
   */
  public int encodeHash(Element element) {
    int[] path = getLocationPathLimited(element);
    int index = pathToIndex.get(path, -1);
    if (index == -1) {
      index = indexToPath.size();
      pathToIndex.put(path, index);
      indexToPath.add(path);
    }
    return index;
  }

  private String[] getLocationComponents(int[] path) {
    int length = path.length;
    String[] components = new String[length];
    int componentCount = 0;
    for (int i = 0; i < length; i++) {
      int componentId = path[i];
      String component = stringCodec.decode(componentId);
      if (i < length - 1 && path[i + 1] < 0) {
        component += "@" + (-path[i + 1]);
        i++;
      }
      components[componentCount++] = component;
    }
    components = ArrayUtils.subarray(components, 0, componentCount);
    return components;
  }

  /**
   * @param usePath is {@code true} when {@link Source} path should be used instead of URI.
   */
  private int[] getLocationPath(Element element, boolean usePath) {
    // prepare the location components
    String[] components = element.getLocation().getComponents();
    if (usePath) {
      LibraryElement library = element.getLibrary();
      if (library != null) {
        components[0] = library.getSource().getFullName();
        if (element.getEnclosingElement() instanceof CompilationUnitElement) {
          components[1] = library.getDefiningCompilationUnit().getSource().getFullName();
        }
      }
    }
    // encode the location
    components = components.clone();
    int length = components.length;
    if (hasLocalOffset(components)) {
      int[] path = new int[2 * length];
      int pathLength = 0;
      for (String component : components) {
        int atOffset = component.indexOf('@');
        if (atOffset == -1) {
          path[pathLength++] = stringCodec.encode(component);
        } else {
          String preAtString = component.substring(0, atOffset);
          String atString = component.substring(atOffset + 1);
          path[pathLength++] = stringCodec.encode(preAtString);
          path[pathLength++] = -1 * Integer.parseInt(atString);
        }
      }
      path = ArrayUtils.subarray(path, 0, pathLength);
      return path;
    } else {
      int[] path = new int[length];
      for (int i = 0; i < length; i++) {
        String component = components[i];
        path[i] = stringCodec.encode(component);
      }
      return path;
    }
  }

  /**
   * Returns an approximation of the given {@link Element}'s location.
   */
  private int[] getLocationPathLimited(Element element) {
    String firstComponent;
    {
      LibraryElement libraryElement = element.getLibrary();
      if (libraryElement != null) {
        firstComponent = libraryElement.getSource().getFullName();
      } else {
        firstComponent = "null";
      }
    }
    String lastComponent = element.getDisplayName();
    int firstId = stringCodec.encode(firstComponent);
    int lastId = stringCodec.encode(lastComponent);
    return new int[] {firstId, lastId};
  }

  private boolean hasLocalOffset(String[] components) {
    for (String component : components) {
      if (component.indexOf('@') != -1) {
        return true;
      }
    }
    return false;
  }
}

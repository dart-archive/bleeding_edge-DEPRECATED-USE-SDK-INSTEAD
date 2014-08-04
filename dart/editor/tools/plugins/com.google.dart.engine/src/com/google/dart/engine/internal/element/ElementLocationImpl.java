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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;

import java.util.ArrayList;

/**
 * Instances of the class {@code ElementLocationImpl} implement an {@link ElementLocation}.
 * 
 * @coverage dart.engine.element
 */
public class ElementLocationImpl implements ElementLocation {
  /**
   * The path to the element whose location is represented by this object.
   */
  private String[] components;

  /**
   * The character used to separate components in the encoded form.
   */
  private static final char SEPARATOR_CHAR = ';';

  /**
   * Initialize a newly created location to represent the given element.
   * 
   * @param element the element whose location is being represented
   */
  public ElementLocationImpl(Element element) {
    ArrayList<String> components = new ArrayList<String>();
    Element ancestor = element;
    while (ancestor != null) {
      components.add(0, ((ElementImpl) ancestor).getIdentifier());
      ancestor = ancestor.getEnclosingElement();
    }
    this.components = components.toArray(new String[components.size()]);
  }

  /**
   * Initialize a newly created location from the given encoded form.
   * 
   * @param encoding the encoded form of a location
   */
  public ElementLocationImpl(String encoding) {
    this.components = decode(encoding);
  }

  /**
   * Initialize a newly created location from the given components.
   * 
   * @param components the components of a location
   */
  public ElementLocationImpl(String[] components) {
    this.components = components;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof ElementLocationImpl)) {
      return false;
    }
    ElementLocationImpl location = (ElementLocationImpl) object;
    String[] otherComponents = location.components;
    int length = components.length;
    if (otherComponents.length != length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (!components[i].equals(otherComponents[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String[] getComponents() {
    return components;
  }

  @Override
  public String getEncoding() {
    StringBuilder builder = new StringBuilder();
    int length = components.length;
    for (int i = 0; i < length; i++) {
      if (i > 0) {
        builder.append(SEPARATOR_CHAR);
      }
      encode(builder, components[i]);
    }
    return builder.toString();
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = 0; i < components.length; i++) {
      String component = components[i];
      result = 31 * result + component.hashCode();
    }
    return result;
  }

  @Override
  public String toString() {
    return getEncoding();
  }

  /**
   * Decode the encoded form of a location into an array of components.
   * 
   * @param encoding the encoded form of a location
   * @return the components that were encoded
   */
  private String[] decode(String encoding) {
    ArrayList<String> components = new ArrayList<String>();
    StringBuilder builder = new StringBuilder();
    int index = 0;
    int length = encoding.length();
    while (index < length) {
      char currentChar = encoding.charAt(index);
      if (currentChar == SEPARATOR_CHAR) {
        if (index + 1 < length && encoding.charAt(index + 1) == SEPARATOR_CHAR) {
          builder.append(SEPARATOR_CHAR);
          index += 2;
        } else {
          components.add(builder.toString());
          builder.setLength(0);
          index++;
        }
      } else {
        builder.append(currentChar);
        index++;
      }
    }
    components.add(builder.toString());
    return components.toArray(new String[components.size()]);
  }

  /**
   * Append an encoded form of the given component to the given builder.
   * 
   * @param builder the builder to which the encoded component is to be appended
   * @param component the component to be appended to the builder
   */
  private void encode(StringBuilder builder, String component) {
    int length = component.length();
    for (int i = 0; i < length; i++) {
      char currentChar = component.charAt(i);
      if (currentChar == SEPARATOR_CHAR) {
        builder.append(SEPARATOR_CHAR);
      }
      builder.append(currentChar);
    }
  }
}

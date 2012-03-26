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
package com.google.dart.tools.core.index;

/**
 * Instances of the class <code>Location</code> represent a location related to an element. The
 * location is expressed as an offset and length, but the offset is relative to the resource
 * containing the element rather than the start of the element within that resource.
 */
public final class Location {
  /**
   * The element containing this location.
   */
  private Element element;

  /**
   * The offset of this location within the resource containing the element.
   */
  private int offset;

  /**
   * The length of this location.
   */
  private int length;

  /**
   * An empty array of locations.
   */
  public static final Location[] EMPTY_ARRAY = new Location[0];

  /**
   * Initialize a newly create location to be relative to the given element at the given offset with
   * the given length.
   * 
   * @param element the element containing this location
   * @param offset the offset of this location within the resource containing the element
   * @param length the length of this location
   */
  public Location(Element element, int offset, int length) {
    if (element == null) {
      throw new IllegalArgumentException("element cannot be null");
    }
    this.element = element;
    this.offset = offset;
    this.length = length;
  }

  /**
   * Return the element containing this location.
   * 
   * @return the element containing this location
   */
  public Element getElement() {
    return element;
  }

  /**
   * Return the length of this location.
   * 
   * @return the length of this location
   */
  public int getLength() {
    return length;
  }

  /**
   * Return the offset of this location within the resource containing the element.
   * 
   * @return the offset of this location within the resource containing the element
   */
  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    return "[" + offset + " - " + (offset + length - 1) + "] in " + element;
  }
}

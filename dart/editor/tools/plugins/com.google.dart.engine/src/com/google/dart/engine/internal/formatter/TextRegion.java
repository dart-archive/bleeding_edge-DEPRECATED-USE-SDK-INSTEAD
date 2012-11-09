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
package com.google.dart.engine.internal.formatter;

/**
 * A text region describes a certain range in an indexed text store. Text stores are for example
 * documents or strings. A text region is defined by its offset into the text store and its length.
 * <p>
 * A text region is considered a value object. Its offset and length do not change over time.
 */
public class TextRegion {

  /** The region offset */
  private int offset;

  /** The region length */
  private int length;

  /**
   * Create a new region.
   * 
   * @param offset the offset of the region
   * @param length the length of the region
   */
  public TextRegion(int offset, int length) {
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TextRegion) {
      TextRegion r = (TextRegion) o;
      return r.getOffset() == offset && r.getLength() == length;
    }
    return false;
  }

  /**
   * Returns the length of the region.
   * 
   * @return the length of the region
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns the offset of the region.
   * 
   * @return the offset of the region
   */
  public int getOffset() {
    return offset;
  }

  @Override
  public int hashCode() {
    return (offset << 24) | (length << 16);
  }

  @Override
  public String toString() {
    return "offset: " + offset + ", length: " + length; //$NON-NLS-1$ //$NON-NLS-2$;
  }
}

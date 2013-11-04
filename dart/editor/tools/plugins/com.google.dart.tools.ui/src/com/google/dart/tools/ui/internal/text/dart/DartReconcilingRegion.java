/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.dart;

/**
 * Instances of {@code DartReconcilingRegion} represent a source region that has changed.
 */
public class DartReconcilingRegion {

  public static final DartReconcilingRegion EMPTY = new DartReconcilingRegion(0, 0, 0);

  private final int offset;
  private final int oldLength;
  private final int newLength;

  /**
   * Construct a new instance representing a region of source.
   * 
   * @param offset the offset of the first character that changed
   * @param oldLength the number of characters that were replaced
   * @param newLength the number of characters in the replacement text
   */
  public DartReconcilingRegion(int offset, int oldLength, int newLength) {
    this.offset = offset;
    this.oldLength = oldLength;
    this.newLength = newLength;
  }

  /**
   * Return a new region representing the union of the receiver with the specified region or
   * {@code null} if the resulting region is disjoint and cannot be represented by this class.
   * 
   * @param offset the offset of the first character that changed
   * @param oldLength the number of characters that were replaced
   * @param newLength the number of characters in the replacement text
   * @return the region or {@code null} if disjoint
   */
  public DartReconcilingRegion add(int offset, int oldLength, int newLength) {
    if (oldLength == 0 && newLength == 0) {
      return this;
    }
    if (isEmpty()) {
      return new DartReconcilingRegion(offset, oldLength, newLength);
    }
    if (offset + oldLength < this.offset) {
      return null;
    }
    if (offset + oldLength > this.offset + this.newLength) {
      return null;
    }
    int offset2 = Math.min(this.offset, offset);
    int oldLength2 = this.oldLength + (this.offset - offset2);
    int newLength2 = this.newLength + (newLength - oldLength) + (this.offset - offset2);
    return new DartReconcilingRegion(offset2, oldLength2, newLength2);
  }

  /**
   * Return the number of characters in the replacement text.
   * 
   * @return the offset (greater than or equal to zero)
   */
  public int getNewLength() {
    return newLength;
  }

  /**
   * Return the offset of the first character that changed.
   * 
   * @return the offset (greater than or equal to zero)
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Return the number of characters that were replaced.
   * 
   * @return the offset (greater than or equal to zero)
   */
  public int getOldLength() {
    return oldLength;
  }

  /**
   * Return {@code true} if the range represent by the receiver is empty.
   * 
   * @return {@code true} if empty
   */
  public boolean isEmpty() {
    return oldLength == 0 && newLength == 0;
  }
}

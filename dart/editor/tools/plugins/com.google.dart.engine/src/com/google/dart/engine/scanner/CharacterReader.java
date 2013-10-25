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
package com.google.dart.engine.scanner;

/**
 * The interface {@code CharacterReader}
 */
public interface CharacterReader {
  /**
   * Advance the current position and return the character at the new current position.
   * 
   * @return the character at the new current position
   */
  public int advance();

  /**
   * Return the current offset relative to the beginning of the source. Return the initial offset if
   * the scanner has not yet scanned the source code, and one (1) past the end of the source code if
   * the entire source code has been scanned.
   * 
   * @return the current offset of the scanner in the source
   */
  public int getOffset();

  /**
   * Return the substring of the source code between the start offset and the modified current
   * position. The current position is modified by adding the end delta.
   * 
   * @param start the offset to the beginning of the string, relative to the start of the file
   * @param endDelta the number of characters after the current location to be included in the
   *          string, or the number of characters before the current location to be excluded if the
   *          offset is negative
   * @return the specified substring of the source code
   */
  public String getString(int start, int endDelta);

  /**
   * Return the character at the current position without changing the current position.
   * 
   * @return the character at the current position
   */
  public int peek();

  /**
   * Set the current offset relative to the beginning of the source. The new offset must be between
   * the initial offset and one (1) past the end of the source code.
   * 
   * @param offset the new offset in the source
   */
  public void setOffset(int offset);
}

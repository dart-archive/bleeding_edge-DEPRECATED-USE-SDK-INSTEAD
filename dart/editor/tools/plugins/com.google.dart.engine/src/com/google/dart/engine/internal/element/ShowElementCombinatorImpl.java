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

import com.google.dart.engine.element.ShowElementCombinator;
import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * Instances of the class {@code ShowElementCombinatorImpl} implement a
 * {@link ShowElementCombinator}.
 * 
 * @coverage dart.engine.element
 */
public class ShowElementCombinatorImpl implements ShowElementCombinator {
  /**
   * The names that are to be made visible in the importing library if they are defined in the
   * imported library.
   */
  private String[] shownNames = StringUtilities.EMPTY_ARRAY;

  /**
   * The offset of the character immediately following the last character of this node.
   */
  private int end = -1;

  /**
   * The offset of the 'show' keyword of this element.
   */
  private int offset;

  /**
   * Initialize a newly created combinator.
   */
  public ShowElementCombinatorImpl() {
    super();
  }

  @Override
  public int getEnd() {
    return end;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public String[] getShownNames() {
    return shownNames;
  }

  /**
   * Set the the offset of the character immediately following the last character of this node.
   */
  public void setEnd(int endOffset) {
    this.end = endOffset;
  }

  /**
   * Sets the offset of the 'show' keyword of this directive.
   */
  public void setOffset(int offset) {
    this.offset = offset;
  }

  /**
   * Set the names that are to be made visible in the importing library if they are defined in the
   * imported library to the given names.
   * 
   * @param shownNames the names that are to be made visible in the importing library
   */
  public void setShownNames(String[] shownNames) {
    this.shownNames = shownNames;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("show ");
    int count = shownNames.length;
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(shownNames[i]);
    }
    return builder.toString();
  }
}

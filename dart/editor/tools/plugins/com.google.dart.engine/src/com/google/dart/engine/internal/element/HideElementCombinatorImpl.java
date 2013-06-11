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

import com.google.dart.engine.element.HideElementCombinator;
import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * Instances of the class {@code HideElementCombinatorImpl} implement a
 * {@link HideElementCombinator}.
 * 
 * @coverage dart.engine.element
 */
public class HideElementCombinatorImpl implements HideElementCombinator {
  /**
   * The names that are not to be made visible in the importing library even if they are defined in
   * the imported library.
   */
  private String[] hiddenNames = StringUtilities.EMPTY_ARRAY;

  /**
   * Initialize a newly created combinator.
   */
  public HideElementCombinatorImpl() {
    super();
  }

  @Override
  public String[] getHiddenNames() {
    return hiddenNames;
  }

  /**
   * Set the names that are not to be made visible in the importing library even if they are defined
   * in the imported library to the given names.
   * 
   * @param hiddenNames the names that are not to be made visible in the importing library
   */
  public void setHiddenNames(String[] hiddenNames) {
    this.hiddenNames = hiddenNames;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("show ");
    int count = hiddenNames.length;
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(hiddenNames[i]);
    }
    return builder.toString();
  }
}

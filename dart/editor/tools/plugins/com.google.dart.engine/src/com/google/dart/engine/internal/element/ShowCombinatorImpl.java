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

import com.google.dart.engine.element.ShowCombinator;
import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * Instances of the class {@code ShowCombinatorImpl} implement a {@link ShowCombinator}.
 */
public class ShowCombinatorImpl implements ShowCombinator {
  /**
   * The names that are to be made visible in the importing library if they are defined in the
   * imported library.
   */
  private String[] shownNames = StringUtilities.EMPTY_ARRAY;

  /**
   * Initialize a newly created combinator.
   */
  public ShowCombinatorImpl() {
    super();
  }

  @Override
  public String[] getShownNames() {
    return shownNames;
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
}

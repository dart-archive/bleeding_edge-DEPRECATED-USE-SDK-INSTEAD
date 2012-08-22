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
package com.google.dart.tools.ui.omni;

import com.google.dart.tools.ui.omni.elements.TypeProvider.SearchInProgressPlaceHolder;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Base class for all proposal providers.
 */
public abstract class OmniProposalProvider {

  private OmniElement[] sortedElements;

  /**
   * Returns the element for the given ID if available, or null if no matching element is available.
   * 
   * @param id the ID of an element
   * @return the element with the given ID, or null if not found.
   */
  public abstract OmniElement getElementForId(String id);

  /**
   * Get matching elements.
   * 
   * @param pattern the pattern to match
   * @return this provider's matching elements
   */
  public abstract OmniElement[] getElements(String pattern);

  /**
   * Get a sorted list of matching elements.
   * 
   * @param pattern the pattern to match
   * @return this provider's matching elements
   */
  public OmniElement[] getElementsSorted(String pattern) {
    if (sortedElements == null) {
      sortedElements = getElements(pattern);
      Arrays.sort(sortedElements, new Comparator<OmniElement>() {
        @Override
        public int compare(OmniElement e1, OmniElement e2) {
          //ensure search progress status sorts last
          if (e1 instanceof SearchInProgressPlaceHolder) {
            return 1;
          }
          return e1.getLabel().compareTo(e2.getLabel());
        }
      });
    }
    return sortedElements;
  }

  /**
   * Returns the unique ID of this provider.
   * 
   * @return the unique ID
   */
  public abstract String getId();

  /**
   * Returns the name of this provider to be displayed to the user.
   * 
   * @return the name
   */
  public abstract String getName();

  /**
   * Return this provider to a fresh state by clearing all cached elements.
   */
  public void reset() {
    sortedElements = null;
  }
}

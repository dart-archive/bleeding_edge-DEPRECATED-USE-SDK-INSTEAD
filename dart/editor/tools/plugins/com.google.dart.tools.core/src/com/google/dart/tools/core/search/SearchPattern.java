/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.search;

import com.google.dart.tools.core.model.DartElement;

/**
 * The interface <code>SearchPattern</code> defines the behavior common to objects that determine
 * whether a given Dart element matches the elements being searched for. Specific kinds of patterns
 * can be created using the static methods defined in the class {@link SearchPatternFactory}.
 */
public interface SearchPattern {
  /**
   * Return the quality of the match if the given element matches this pattern, or <code>null</code>
   * if the element does not match this pattern.
   * 
   * @param element the element being matched against
   * @return the quality of the match if the given element matches this pattern
   */
  public MatchQuality matches(DartElement element);
}

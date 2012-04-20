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
package com.google.dart.tools.ui.search;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.search.SearchScope;

/**
 * Describes a search query by giving the {@link DartElement} to search for.
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 * 
 * @see QuerySpecification
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ElementQuerySpecification extends QuerySpecification {

  private final DartElement element;

  /**
   * A constructor.
   * 
   * @param element The dart element the query should search for.
   * @param limitTo The kind of occurrence the query should search for.
   * @param scope The scope to search in.
   * @param scopeDescription A human readable description of the search scope.
   */
  public ElementQuerySpecification(DartElement element, int limitTo, SearchScope scope,
      String scopeDescription) {
    super(limitTo, scope, scopeDescription);
    this.element = element;
  }

  /**
   * Returns the element to search for.
   * 
   * @return The element to search for.
   */
  public DartElement getElement() {
    return element;
  }

}

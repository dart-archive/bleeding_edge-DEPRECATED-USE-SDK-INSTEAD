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
package com.google.dart.engine.search;

import com.google.dart.engine.element.Element;

/**
 * The interface <code>SearchScope</code> defines the behavior common to objects that define where
 * search result should be found by a {@link SearchEngine}. Clients must pass an instance of this
 * interface to the <code>search(...)</code> methods. Such an instance can be created using the
 * {@link SearchScopeFactory}.
 * 
 * @coverage dart.engine.search
 */
public interface SearchScope {
  /**
   * Return {@code true} if this scope encloses the given element.
   * 
   * @param element the element being checked
   * @return {@code true} if the element is in this scope
   */
  boolean encloses(Element element);
}

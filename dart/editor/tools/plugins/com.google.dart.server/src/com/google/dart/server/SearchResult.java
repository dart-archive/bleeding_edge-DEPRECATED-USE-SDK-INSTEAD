/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

/**
 * The interface {@code SearchResult} defines the behavior of objects that represent a search
 * result.
 * 
 * @coverage dart.server
 */
public interface SearchResult {
  /**
   * An empty array of {@link SearchResult}s.
   */
  SearchResult[] EMPTY_ARRAY = new SearchResult[0];

  /**
   * Return the kind to this result.
   * 
   * @return the kind of this result
   */
  public SearchResultKind getKind();

  /**
   * Return the location of the code that matched the search criteria.
   * 
   * @return the location
   */
  public Location getLocation();

  /**
   * Return the path to this result starting with the element that encloses it, then for its
   * enclosing element, etc up to the library.
   * 
   * @return the path to this result
   */
  public Element[] getPath();

  /**
   * Return {@code true} is this search result is a potential reference to a class member.
   * 
   * @return {@code true} is this search result is a potential reference to a class member
   */
  public boolean isPotential();
}

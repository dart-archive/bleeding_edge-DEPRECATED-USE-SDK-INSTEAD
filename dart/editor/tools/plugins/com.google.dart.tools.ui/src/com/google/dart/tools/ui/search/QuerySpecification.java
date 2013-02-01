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

import com.google.dart.tools.core.search.SearchScope;

/**
 * <p>
 * Describes a Dart search query. A query is described by giving a scope, a scope description, what
 * kind of match to search for (reference, declarations, etc) and either a Dart element or a string
 * and what kind of element to search for (type, field, etc). What exactly it means to, for example,
 * to search for "references to type Foo" is up to query participants. For example, a participant
 * might consider the name of a package in a pubspec.yaml file to be a reference to the package.
 * </p>
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class QuerySpecification {

  public static final int LIMIT_REFERENCES = 1;
  public static final int LIMIT_DECLARATIONS = 2;
  public static final int LIMIT_OVERRIDES = 4;

  private final SearchScope scope;
  private final int limitTo;
  private final String scopeDescription;

  QuerySpecification(int limitTo, SearchScope scope, String scopeDescription) {
    this.scope = scope;
    this.limitTo = limitTo;
    this.scopeDescription = scopeDescription;
  }

  /**
   * Returns what kind of occurrences the query should look for.
   * 
   * @return Whether to search for references (LIMIT_REFERENCES), declarations (LIMIT_DECLARATIONS),
   *         etc. This is a bit field; a single specification may define multiple searches.
   */
  public int getLimitTo() {
    //TODO (pquitslund): migrate this to a SearchFilter
    return limitTo;
  }

  /**
   * Returns the search scope to be used in the query.
   * 
   * @return The search scope.
   */
  public SearchScope getScope() {
    return scope;
  }

  /**
   * Returns a human readable description of the search scope.
   * 
   * @return A description of the search scope.
   * @see QuerySpecification#getScope()
   */
  public String getScopeDescription() {
    return scopeDescription;
  }

  public boolean hasElement() {
    return false;
  }

  public boolean hasNode() {
    return false;
  }

  public boolean isDeclarationsSearch() {
    return (getLimitTo() & LIMIT_DECLARATIONS) == LIMIT_DECLARATIONS;
  }

  public boolean isOverridesSearch() {
    return (getLimitTo() & LIMIT_OVERRIDES) == LIMIT_OVERRIDES;
  }

  public boolean isReferencesSearch() {
    return (getLimitTo() & LIMIT_REFERENCES) == LIMIT_REFERENCES;
  }
}

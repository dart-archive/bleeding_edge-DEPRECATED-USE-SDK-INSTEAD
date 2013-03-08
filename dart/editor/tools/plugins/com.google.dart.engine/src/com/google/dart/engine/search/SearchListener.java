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

/**
 * The interface <code>SearchListener</code> defines the behavior of objects that are listening for
 * the results of a search.
 * 
 * @coverage dart.engine.search
 */
public interface SearchListener {
  /**
   * Record the fact that the given match was found.
   * 
   * @param match the match that was found
   */
  void matchFound(SearchMatch match);

  /**
   * This method is invoked when the search is complete and no additional matches will be found.
   */
  void searchComplete();
}

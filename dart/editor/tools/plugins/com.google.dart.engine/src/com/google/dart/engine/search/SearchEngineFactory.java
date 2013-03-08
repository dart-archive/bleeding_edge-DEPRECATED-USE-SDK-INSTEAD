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

import com.google.dart.engine.index.Index;
import com.google.dart.engine.internal.search.SearchEngineImpl;

/**
 * Factory for {@link SearchEngine}.
 * 
 * @coverage dart.engine.search
 */
public final class SearchEngineFactory {
  /**
   * @return the new {@link SearchEngine} instance based on the given {@link Index}.
   */
  public static SearchEngine createSearchEngine(Index index) {
    return new SearchEngineImpl(index);
  }
}

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
package com.google.dart.tools.core.search;

import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.search.NewSearchEngineImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

/**
 * The class <code>SearchEngineFactory</code> defines utility methods that can be used to create
 * {@link SearchEngine search engines}.
 */
public final class SearchEngineFactory {
  /**
   * Create a search engine.
   */
  public static SearchEngine createSearchEngine() {
    return new NewSearchEngineImpl(InMemoryIndex.getInstance());
  }

  /**
   * Create a search engine that will find matches in the given working copies rather than in the
   * saved versions of those compilation units.
   * 
   * @param workingCopies the working copies that take precedence over their original compilation
   *          units
   */
  public static SearchEngine createSearchEngine(CompilationUnit[] workingCopies) {
    // TODO(brianwilkerson) Need to figure out how to handle alternate universes.
    return new NewSearchEngineImpl(InMemoryIndex.getInstance());
  }

  /**
   * Create a search engine that will find matches in working copies with the given working copy
   * owner rather than in the saved versions of those compilation units.
   * 
   * @param workingCopyOwner the working copy owner used to identify the working copies that take
   *          precedence over their original compilation units
   */
  public static SearchEngine createSearchEngine(WorkingCopyOwner workingCopyOwner) {
    // TODO(brianwilkerson) Need to figure out how to handle alternate universes.
    return new NewSearchEngineImpl(InMemoryIndex.getInstance());
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private SearchEngineFactory() {
    super();
  }
}

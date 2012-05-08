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
package com.google.dart.tools.search.core.text;

import com.google.dart.tools.search.internal.core.text.TextSearchExecutor;
import com.google.dart.tools.search.internal.ui.SearchPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.util.regex.Pattern;

/**
 * A {@link TextSearchEngine} searches the content of a workspace file resources for matches to a
 * given search pattern.
 * <p>
 * {@link #create()} gives access to an instance of the search engine. By default this is the
 * default text search engine (see {@link #createDefault()}) but extensions can offer more
 * sophisticated search engine implementations.
 * </p>
 */
public abstract class TextSearchEngine {

  /**
   * Creates an instance of the search engine. By default this is the default text search engine
   * (see {@link #createDefault()}), but extensions can offer more sophisticated search engine
   * implementations.
   * 
   * @return the created {@link TextSearchEngine}.
   */
  public static TextSearchEngine create() {
    return SearchPlugin.getDefault().getTextSearchEngineRegistry().getPreferred();
  }

  /**
   * Creates the default, built-in, text search engine that implements a brute-force search, not
   * using any search index. Note that clients should always use the search engine provided by
   * {@link #create()}.
   * 
   * @return an instance of the default text search engine {@link TextSearchEngine}.
   */
  public static TextSearchEngine createDefault() {
    return new TextSearchEngine() {
      @Override
      public IStatus search(TextSearchScope scope, TextSearchRequestor requestor,
          Pattern searchPattern, IProgressMonitor monitor) {
        return new TextSearchExecutor(requestor, searchPattern).search(scope, monitor);
      }
    };
  }

  /**
   * Uses a given search pattern to find matches in the content of workspace file resources. If a
   * file is open in an editor, the editor buffer is searched.
   * 
   * @param requestor the search requestor that gets the search results
   * @param scope the scope defining the resources to search in
   * @param searchPattern The search pattern used to find matches in the file contents.
   * @param monitor the progress monitor to use
   * @return the status containing information about problems in resources searched.
   */
  public abstract IStatus search(TextSearchScope scope, TextSearchRequestor requestor,
      Pattern searchPattern, IProgressMonitor monitor);

}

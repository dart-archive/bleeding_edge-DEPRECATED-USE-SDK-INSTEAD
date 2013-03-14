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
package com.google.dart.engine.context;

import com.google.dart.engine.source.Source;

import java.util.HashSet;

/**
 * Instances of {@code ChangeResult} are returned by {@link AnalysisContext#applyChanges(ChangeSet)}
 * to indicate what operations need to be performed as a result of the change.
 * 
 * @coverage dart.engine
 */
public class ChangeResult {
  /**
   * A list containing the sources for which analysis results have been invalidated, or {@code null}
   * if there are no such sources.
   */
  private HashSet<Source> invalidatedSources;

  /**
   * Initialize a newly created result object to be empty.
   */
  public ChangeResult() {
    super();
  }

  /**
   * Return an array containing the sources for which analysis results have been invalidated.
   * 
   * @return an array containing the sources for which analysis results have been invalidated
   */
  public Source[] getInvalidatedSources() {
    if (invalidatedSources == null) {
      return Source.EMPTY_ARRAY;
    }
    return invalidatedSources.toArray(new Source[invalidatedSources.size()]);
  }

  /**
   * Record the fact that analysis results related to the given source have been invalidated.
   * 
   * @param source the source related to the invalidated results
   */
  public void invalidated(Source source) {
    if (invalidatedSources == null) {
      invalidatedSources = new HashSet<Source>();
    }
    invalidatedSources.add(source);
  }
}

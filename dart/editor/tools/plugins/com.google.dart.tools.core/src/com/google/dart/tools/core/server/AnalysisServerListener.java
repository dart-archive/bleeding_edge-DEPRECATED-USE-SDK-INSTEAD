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
package com.google.dart.tools.core.server;

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.Source;

/**
 * The interface {@code AnalysisServerListener} defines the behavior of objects that listen for
 * results from an analysis server.
 */
public interface AnalysisServerListener {
  /**
   * A new collection of errors has been computed for the given source.
   * 
   * @param contextId the id of the context in which the source was analyzed
   * @param source the source for which new errors have been computed
   * @param errors the errors that should be reported for the source
   */
  public void computedErrors(String contextId, Source source, AnalysisError[] errors);

  /**
   * A new collection of highlight regions has been computed for the given source. Each highlight
   * region represents a particular syntactic or semantic meaning associated with some range. Note
   * that the highlight regions that are returned can overlap other highlight regions if there is
   * more than one meaning associated with a particular region.
   * 
   * @param contextId the id of the context in which the source was analyzed
   * @param source the source for which new highlight regions have been computed
   * @param highlights the highlight regions associated with the source
   */
  public void computedHighlights(String contextId, Source source, HighlightRegion[] highlights);

  /**
   * A new collection of navigation regions has been computed for the given source. Each navigation
   * region represents a list of targets associated with some range. The lists will usually contain
   * a single target, but can contain more in the case of a part that is included in multiple
   * libraries or an Dart code that is compiled against multiple versions of a package. Note that
   * the navigation regions that are returned do not overlap other navigation regions.
   * 
   * @param contextId the id of the context in which the source was analyzed
   * @param source the source for which new highlight regions have been computed
   * @param highlights the highlight regions associated with the source
   */
  public void computedNavigation(String contextId, Source source, NavigationRegion[] targets);

  /**
   * A new outline has been computed for the given source.
   * 
   * @param contextId the id of the context in which the source was analyzed
   * @param source the source for which new highlight regions have been computed
   * @param outline the outline associated with the source
   */
  public void computedOutline(String contextId, Source source, Outline outline);
}

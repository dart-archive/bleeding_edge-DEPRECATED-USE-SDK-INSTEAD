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

package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Outline;

/**
 * Instances of {@code AnalysisServerData} provide access to analysis results reported by
 * {@link AnalysisServer}.
 * 
 * @coverage dart.tools.core.model
 */
public interface AnalysisServerData {
  /**
   * Returns {@link NavigationRegion}s associated with the given context and {@link Source}. May be
   * empty, but not {@code null}.
   */
  NavigationRegion[] getNavigation(String contextId, Source source);

  /**
   * Specifies that the client wants to be notified about new {@link HighlightRegion}s.
   */
  void subscribeHighlights(String contextId, Source source,
      AnalysisServerHighlightsListener listener);

  /**
   * Specifies that the client wants to request {@link #getNavigation(String, Source)}.
   */
  void subscribeNavigation(String contextId, Source source);

  /**
   * Specifies that the client wants to be notified about new {@link Outline}.
   */
  void subscribeOutline(String contextId, Source source, AnalysisServerOutlineListener listener);

  /**
   * Specifies that the client doesn't want to be notified about {@link HighlightRegion}s anymore.
   */
  void unsubscribeHighlights(String contextId, Source source,
      AnalysisServerHighlightsListener listener);

  /**
   * Specifies that the client doesn't need navigation information for the given source anymore.
   */
  void unsubscribeNavigation(String contextId, Source source);

  /**
   * Specifies that the client doesn't want to be notified about {@link Outline} anymore.
   */
  void unsubscribeOutline(String contextId, Source source, AnalysisServerOutlineListener listener);
}

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
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.HighlightRegion;
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.Occurrences;
import com.google.dart.server.generated.types.Outline;
import com.google.dart.server.generated.types.OverrideMember;
import com.google.dart.server.generated.types.SearchResult;

/**
 * Instances of {@code AnalysisServerData} provide access to analysis results reported by
 * {@link AnalysisServer}.
 * 
 * @coverage dart.tools.core.model
 */
public interface AnalysisServerData {
  /**
   * True if analysis is currently being performed.
   */
  public boolean isAnalyzing();

  /**
   * Add {@link HighlightRegion}s listener for the file.
   */
  void addHighlightsListener(String file, AnalysisServerHighlightsListener listener);

  /**
   * Add {@link NavigationRegion}s listener.
   */
  void addNavigationListener(String file, AnalysisServerNavigationListener listener);

  /**
   * Add {@link Occurrences} listener for the file.
   */
  void addOccurrencesListener(String file, AnalysisServerOccurrencesListener listener);

  /**
   * Add {@link Outline} listener for the file.
   */
  void addOutlineListener(String file, AnalysisServerOutlineListener listener);

  /**
   * Add {@link OverrideMember}s listener for the file.
   */
  void addOverridesListener(String file, AnalysisServerOverridesListener listener);

  /**
   * Add {@link SearchResult}s listener.
   */
  void addSearchResultsListener(String id, SearchResultsListener listener);

  /**
   * Returns {@link AnalysisError}s associated with the given file. May be empty, but not
   * {@code null}.
   */
  AnalysisError[] getErrors(String file);

  /**
   * Returns {@link NavigationRegion}s associated with the given context and {@link Source}. May be
   * empty, but not {@code null}.
   */
  NavigationRegion[] getNavigation(String file);

  /**
   * Returns {@link Occurrences}s associated with the given context and {@link Source}. May be
   * empty, but not {@code null}.
   */
  Occurrences[] getOccurrences(String file);

  /**
   * Remove {@link HighlightRegion}s listener for the file.
   */
  void removeHighlightsListener(String file, AnalysisServerHighlightsListener listener);

  /**
   * Remove {@link NavigationRegion}s listener for the file.
   */
  void removeNavigationListener(String file, AnalysisServerNavigationListener listener);

  /**
   * Remove {@link Occurrences} listener for the file.
   */
  void removeOccurrencesListener(String file, AnalysisServerOccurrencesListener listener);

  /**
   * Remove {@link Outline} listener for the file.
   */
  void removeOutlineListener(String file, AnalysisServerOutlineListener listener);

  /**
   * Remove {@link OverrideMember}s listener for the file.
   */
  void removeOverridesListener(String file, AnalysisServerOverridesListener listener);

  /**
   * Remove {@link SearchResult} listener.
   */
  void removeSearchResultsListener(String id, SearchResultsListener listener);

  /**
   * Specifies that the client was to be notified about "execution.launchData".
   */
  void subscribeLaunchData(AnalysisServerLaunchDataListener listener);

  /**
   * Specifies that the client doesn't wan to be notified about "execution.launchData".
   */
  void unsubscribeLaunchData(AnalysisServerLaunchDataListener listener);

  /**
   * Update the analysis options.
   */
  void updateOptions();
}

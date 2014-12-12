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

package com.google.dart.tools.core.internal.analysis.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.AnalysisOptions;
import com.google.dart.server.generated.types.AnalysisStatus;
import com.google.dart.server.generated.types.ExecutionService;
import com.google.dart.server.generated.types.HighlightRegion;
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.Occurrences;
import com.google.dart.server.generated.types.Outline;
import com.google.dart.server.generated.types.OverrideMember;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;
import com.google.dart.tools.core.analysis.model.AnalysisServerHighlightsListener;
import com.google.dart.tools.core.analysis.model.AnalysisServerLaunchDataListener;
import com.google.dart.tools.core.analysis.model.AnalysisServerNavigationListener;
import com.google.dart.tools.core.analysis.model.AnalysisServerOccurrencesListener;
import com.google.dart.tools.core.analysis.model.AnalysisServerOutlineListener;
import com.google.dart.tools.core.analysis.model.AnalysisServerOverridesListener;
import com.google.dart.tools.core.analysis.model.SearchResultsListener;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Instances of {@code AnalysisServerData} manage and provide access to analysis results reported by
 * {@link AnalysisServer}.
 * 
 * @coverage dart.tools.core.model
 */
public class AnalysisServerDataImpl implements AnalysisServerData {
  private boolean isAnalyzing = false;
  private final Map<String, Set<AnalysisServerHighlightsListener>> highlightsSubscriptions = Maps.newHashMap();
  private final Map<String, Set<AnalysisServerNavigationListener>> navigationSubscriptions = Maps.newHashMap();
  private final Map<String, Set<AnalysisServerOccurrencesListener>> occurrencesSubscriptions = Maps.newHashMap();
  private final Map<String, Set<AnalysisServerOutlineListener>> outlineSubscriptions = Maps.newHashMap();
  private final Map<String, Set<AnalysisServerOverridesListener>> overridesSubscriptions = Maps.newHashMap();
  private final Map<String, AnalysisError[]> errorData = Maps.newHashMap();
  private final Map<String, NavigationRegion[]> navigationData = Maps.newHashMap();
  private final Map<String, Occurrences[]> occurrencesData = Maps.newHashMap();
  private final Map<String, SearchResultsListener> searchResultsListeners = Maps.newHashMap();
  private final Map<String, List<SearchResultsSet>> searchResultsData = Maps.newHashMap();
  private final List<String> executionSubscriptions = Lists.newArrayList();
  private final List<AnalysisServerLaunchDataListener> launchDataListeners = Lists.newArrayList();

  private AnalysisServer server;

  @Override
  public void addHighlightsListener(String file, AnalysisServerHighlightsListener listener) {
    Set<AnalysisServerHighlightsListener> subscriptions = highlightsSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      highlightsSubscriptions.put(file, subscriptions);
    }
    subscriptions.add(listener);
  }

  @Override
  public void addNavigationListener(String file, AnalysisServerNavigationListener listener) {
    Set<AnalysisServerNavigationListener> subscriptions = navigationSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      navigationSubscriptions.put(file, subscriptions);
    }
    subscriptions.add(listener);
  }

  @Override
  public void addOccurrencesListener(String file, AnalysisServerOccurrencesListener listener) {
    Set<AnalysisServerOccurrencesListener> subscriptions = occurrencesSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      occurrencesSubscriptions.put(file, subscriptions);
    }
    subscriptions.add(listener);
  }

  @Override
  public void addOutlineListener(String file, AnalysisServerOutlineListener listener) {
    Set<AnalysisServerOutlineListener> subscriptions = outlineSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      outlineSubscriptions.put(file, subscriptions);
    }
    subscriptions.add(listener);
  }

  @Override
  public void addOverridesListener(String file, AnalysisServerOverridesListener listener) {
    Set<AnalysisServerOverridesListener> subscriptions = overridesSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      overridesSubscriptions.put(file, subscriptions);
    }
    subscriptions.add(listener);
  }

  @Override
  public synchronized void addSearchResultsListener(String searchId, SearchResultsListener listener) {
    List<SearchResultsSet> resultsSets = searchResultsData.remove(searchId);
    boolean hasLast = false;
    if (resultsSets != null) {
      for (SearchResultsSet searchResultsSet : resultsSets) {
        listener.computedSearchResults(searchResultsSet.results, searchResultsSet.last);
        hasLast |= searchResultsSet.last;
      }
    }
    if (!hasLast) {
      searchResultsListeners.put(searchId, listener);
    }
  }

  @Override
  public AnalysisError[] getErrors(String file) {
    AnalysisError[] errors = errorData.get(file);
    if (errors == null) {
      return AnalysisError.EMPTY_ARRAY;
    }
    return errors;
  }

  @Override
  public NavigationRegion[] getNavigation(String file) {
    NavigationRegion[] sourceRegions = navigationData.get(file);
    if (sourceRegions == null) {
      return NavigationRegion.EMPTY_ARRAY;
    }
    return sourceRegions;
  }

  @Override
  public Occurrences[] getOccurrences(String file) {
    Occurrences[] occurrencesArray = occurrencesData.get(file);
    if (occurrencesArray == null) {
      return Occurrences.EMPTY_ARRAY;
    }
    return occurrencesArray;
  }

  @Override
  public boolean isAnalyzing() {
    return isAnalyzing;
  }

  @Override
  public void removeHighlightsListener(String file, AnalysisServerHighlightsListener listener) {
    Set<AnalysisServerHighlightsListener> subscriptions = highlightsSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        highlightsSubscriptions.remove(file);
      }
    }
  }

  @Override
  public void removeNavigationListener(String file, AnalysisServerNavigationListener listener) {
    Set<AnalysisServerNavigationListener> subscriptions = navigationSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        navigationSubscriptions.remove(file);
      }
    }
  }

  @Override
  public void removeOccurrencesListener(String file, AnalysisServerOccurrencesListener listener) {
    Set<AnalysisServerOccurrencesListener> subscriptions = occurrencesSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        occurrencesSubscriptions.remove(file);
      }
    }
  }

  @Override
  public void removeOutlineListener(String file, AnalysisServerOutlineListener listener) {
    Set<AnalysisServerOutlineListener> subscriptions = outlineSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        outlineSubscriptions.remove(file);
      }
    }
  }

  @Override
  public void removeOverridesListener(String file, AnalysisServerOverridesListener listener) {
    Set<AnalysisServerOverridesListener> subscriptions = overridesSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        overridesSubscriptions.remove(file);
      }
    }
  }

  @Override
  public void removeSearchResultsListener(String searchId, SearchResultsListener listener) {
    searchResultsData.remove(searchId);
    searchResultsListeners.remove(searchId);
  }

  /**
   * Sets the {@link AnalysisServer} to talk to.
   */
  public void setServer(AnalysisServer server) {
    this.server = server;
  }

  @Override
  public synchronized void subscribeLaunchData(AnalysisServerLaunchDataListener listener) {
    if (launchDataListeners.add(listener)) {
      if (executionSubscriptions.add(ExecutionService.LAUNCH_DATA)) {
        executionSubscriptions.add(ExecutionService.LAUNCH_DATA);
        server.execution_setSubscriptions(executionSubscriptions);
      }
    }
  }

  @Override
  public void unsubscribeLaunchData(AnalysisServerLaunchDataListener listener) {
    if (launchDataListeners.remove(listener)) {
      if (executionSubscriptions.remove(ExecutionService.LAUNCH_DATA)) {
        server.execution_setSubscriptions(executionSubscriptions);
      }
    }
  }

  @Override
  public void updateOptions() {
    server.analysis_updateOptions(new AnalysisOptions(
        DartCoreDebug.ENABLE_ASYNC,
        DartCoreDebug.ENABLE_DEFERRED_LOADING,
        DartCoreDebug.ENABLE_ENUMS,
        DartCore.getPlugin().isHintsDart2JSEnabled(),
        DartCore.getPlugin().isHintsEnabled()));
  }

  void internalComputedErrors(String file, AnalysisError[] errors) {
    errorData.put(file, errors);
  }

  void internalComputedHighlights(String file, HighlightRegion[] highlights) {
    Set<AnalysisServerHighlightsListener> subscriptions = highlightsSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    subscriptions = ImmutableSet.copyOf(subscriptions);
    for (AnalysisServerHighlightsListener listener : subscriptions) {
      listener.computedHighlights(file, highlights);
    }
  }

  void internalComputedLaunchData(String file, String kind, String[] referencedFiles) {
    List<AnalysisServerLaunchDataListener> listeners = launchDataListeners;
    listeners = ImmutableList.copyOf(listeners);
    for (AnalysisServerLaunchDataListener listener : listeners) {
      listener.computedLaunchData(file, kind, referencedFiles);
    }
  }

  void internalComputedNavigation(String file, NavigationRegion[] targets) {
    navigationData.put(file, targets);
    Set<AnalysisServerNavigationListener> subscriptions = navigationSubscriptions.get(file);
    if (subscriptions != null) {
      subscriptions = ImmutableSet.copyOf(subscriptions);
      for (AnalysisServerNavigationListener listener : subscriptions) {
        listener.computedNavigation(file, targets);
      }
    }
  }

  void internalComputedOccurrences(String file, Occurrences[] occurrences) {
    occurrencesData.put(file, occurrences);
    Set<AnalysisServerOccurrencesListener> subscriptions = occurrencesSubscriptions.get(file);
    if (subscriptions != null) {
      subscriptions = ImmutableSet.copyOf(subscriptions);
      for (AnalysisServerOccurrencesListener listener : subscriptions) {
        listener.computedOccurrences(file, occurrences);
      }
    }
  }

  void internalComputedOutline(String file, Outline outline) {
    Set<AnalysisServerOutlineListener> subscriptions = outlineSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    subscriptions = ImmutableSet.copyOf(subscriptions);
    for (AnalysisServerOutlineListener listener : subscriptions) {
      listener.computedOutline(file, outline);
    }
  }

  void internalComputedOverrides(String file, OverrideMember[] overrides) {
    Set<AnalysisServerOverridesListener> subscriptions = overridesSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    subscriptions = ImmutableSet.copyOf(subscriptions);
    for (AnalysisServerOverridesListener listener : subscriptions) {
      listener.computedHighlights(file, overrides);
    }
  }

  synchronized void internalComputedSearchResults(String searchId, List<SearchResult> results,
      boolean last) {
    SearchResultsListener listener = searchResultsListeners.get(searchId);
    if (listener != null) {
      if (last) {
        searchResultsListeners.remove(searchId);
      }
      listener.computedSearchResults(results, last);
    }
  }

  /**
   * Clears all information associated with the given files.
   */
  void internalFlushResults(List<String> files) {
    for (String file : files) {
      highlightsSubscriptions.remove(file);
      outlineSubscriptions.remove(file);
      overridesSubscriptions.remove(file);
      errorData.remove(file);
      navigationData.remove(file);
      occurrencesData.remove(file);
    }
  }

  void internalServerStatus(AnalysisStatus status) {
    isAnalyzing = status != null && status.isAnalyzing();
  }
}

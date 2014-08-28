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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.SearchResult;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.AnalysisService;
import com.google.dart.server.generated.types.HighlightRegion;
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.Occurrences;
import com.google.dart.server.generated.types.Outline;
import com.google.dart.server.generated.types.OverrideMember;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;
import com.google.dart.tools.core.analysis.model.AnalysisServerHighlightsListener;
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
  private final Map<String, Set<AnalysisServerHighlightsListener>> highlightsSubscriptions = Maps.newHashMap();
  private final Map<String, Set<AnalysisServerOutlineListener>> outlineSubscriptions = Maps.newHashMap();
  private final Map<String, Set<AnalysisServerOverridesListener>> overridesSubscriptions = Maps.newHashMap();
  private final Map<String, AnalysisError[]> errorData = Maps.newHashMap();
  private final Map<String, NavigationRegion[]> navigationData = Maps.newHashMap();
  private final Map<String, Occurrences[]> occurrencesData = Maps.newHashMap();
  private final Map<String, List<String>> analysisSubscriptions = Maps.newHashMap();
  private final Map<String, SearchResultsListener> searchResultsListeners = Maps.newHashMap();
  private final Map<String, List<SearchResultsSet>> searchResultsData = Maps.newHashMap();
  // TODO(scheglov) restore or remove for the new API
//  private final Map<String, Set<ErrorCode>> fixableErrorCodesData = Maps.newHashMap();

  private AnalysisServer server;

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
  public boolean isFixableErrorCode(String file, ErrorCode errorCode) {
    // TODO(scheglov) restore or remove for the new API
    return false;
//    Set<ErrorCode> fixableErrorCodes = fixableErrorCodesData.get(contextId);
//    if (fixableErrorCodes == null) {
//      return false;
//    }
//    return fixableErrorCodes.contains(errorCode);
  }

  @Override
  public synchronized void removeSearchResultsListener(String searchId,
      SearchResultsListener listener) {
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
  public void subscribeHighlights(String file, AnalysisServerHighlightsListener listener) {
    Set<AnalysisServerHighlightsListener> subscriptions = highlightsSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      highlightsSubscriptions.put(file, subscriptions);
    }
    if (subscriptions.add(listener)) {
      addAnalysisSubscription(AnalysisService.HIGHLIGHTS, file);
    }
  }

  @Override
  public void subscribeNavigation(String file) {
    addAnalysisSubscription(AnalysisService.NAVIGATION, file);
  }

  @Override
  public void subscribeOccurrences(String file) {
    addAnalysisSubscription(AnalysisService.OCCURRENCES, file);
  }

  @Override
  public void subscribeOutline(String file, AnalysisServerOutlineListener listener) {
    Set<AnalysisServerOutlineListener> subscriptions = outlineSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      outlineSubscriptions.put(file, subscriptions);
    }
    if (subscriptions.add(listener)) {
      addAnalysisSubscription(AnalysisService.OUTLINE, file);
    }
  }

  @Override
  public void subscribeOverrides(String file, AnalysisServerOverridesListener listener) {
    Set<AnalysisServerOverridesListener> subscriptions = overridesSubscriptions.get(file);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      overridesSubscriptions.put(file, subscriptions);
    }
    if (subscriptions.add(listener)) {
      addAnalysisSubscription(AnalysisService.OVERRIDES, file);
    }
  }

  @Override
  public void unsubscribeHighlights(String file, AnalysisServerHighlightsListener listener) {
    Set<AnalysisServerHighlightsListener> subscriptions = highlightsSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        removeAnalysisSubscription(AnalysisService.HIGHLIGHTS, file);
      }
    }
  }

  @Override
  public void unsubscribeNavigation(String file) {
    removeAnalysisSubscription(AnalysisService.NAVIGATION, file);
  }

  @Override
  public void unsubscribeOccurrences(String file) {
    removeAnalysisSubscription(AnalysisService.OCCURRENCES, file);
  }

  @Override
  public void unsubscribeOutline(String file, AnalysisServerOutlineListener listener) {
    Set<AnalysisServerOutlineListener> subscriptions = outlineSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        removeAnalysisSubscription(AnalysisService.OUTLINE, file);
      }
    }
  }

  @Override
  public void unsubscribeOverrides(String file, AnalysisServerOverridesListener listener) {
    Set<AnalysisServerOverridesListener> subscriptions = overridesSubscriptions.get(file);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        removeAnalysisSubscription(AnalysisService.OVERRIDES, file);
      }
    }
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

  void internalComputedNavigation(String file, NavigationRegion[] targets) {
    navigationData.put(file, targets);
  }

  void internalComputedOccurrences(String file, Occurrences[] occurrencesArray) {
    occurrencesData.put(file, occurrencesArray);
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

  synchronized void internalComputedSearchResults(String searchId, SearchResult[] results,
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
      analysisSubscriptions.remove(file);
    }
  }

  /**
   * Remembers the {@link ErrorCode} that may be fixed in the given context.
   */
  void internalSetFixableErrorCodes(String file, ErrorCode[] errorCodes) {
    // TODO(scheglov) restore or remove for the new API
//    fixableErrorCodesData.put(contextId, Sets.newHashSet(errorCodes));
  }

  /**
   * Adds the given file to the subscription list for the given {@link AnalysisService}.
   */
  private void addAnalysisSubscription(String analysisService, String file) {
    List<String> files = analysisSubscriptions.get(analysisService);
    if (files == null) {
      files = Lists.newArrayList();
      analysisSubscriptions.put(analysisService, files);
    }
    if (!files.contains(file)) {
      files.add(file);
      server.analysis_setSubscriptions(analysisSubscriptions);
    }
  }

  /**
   * Removes the given file from the subscription list for the given {@link AnalysisService}.
   */
  private void removeAnalysisSubscription(String analysisService, String file) {
    List<String> files = analysisSubscriptions.get(analysisService);
    if (files == null) {
      return;
    }
    if (files.remove(file)) {
      if (files.isEmpty()) {
        analysisSubscriptions.remove(analysisService);
      }
      // TODO (jwren) re-implement after this is working
//      server.analysis_setSubscriptions(analysisSubscriptions);
    }
  }
}

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

package com.google.dart.server.internal;

import com.google.common.collect.Lists;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Occurrences;
import com.google.dart.server.Outline;
import com.google.dart.server.ServerStatus;

import java.util.List;

/**
 * The class {@code BroadcastAnalysisServerListener} implements {@link AnalysisServerListener} that
 * broadcasts events to other listeners.
 * 
 * @coverage dart.server
 */
public class BroadcastAnalysisServerListener implements AnalysisServerListener {
  private final List<AnalysisServerListener> listeners = Lists.newArrayList();

  /**
   * Add the given listener to the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be added
   */
  public void addListener(AnalysisServerListener listener) {
    synchronized (listeners) {
      if (listeners.contains(listener)) {
        return;
      }
      listeners.add(listener);
    }
  }

  @Override
  public void computedCompletion(String completionId, CompletionSuggestion[] completions,
      boolean last) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedCompletion(completionId, completions, last);
    }
  }

  @Override
  public void computedErrors(String file, AnalysisError[] errors) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedErrors(file, errors);
    }
  }

  @Override
  public void computedHighlights(String file, HighlightRegion[] highlights) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedHighlights(file, highlights);
    }
  }

  @Override
  public void computedNavigation(String file, NavigationRegion[] targets) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedNavigation(file, targets);
    }
  }

  @Override
  public void computedOccurrences(String file, Occurrences[] occurrencesArray) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedOccurrences(file, occurrencesArray);
    }
  }

  @Override
  public void computedOutline(String file, Outline outline) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedOutline(file, outline);
    }
  }

  /**
   * Remove the given listener from the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be removed
   */
  public void removeListener(AnalysisServerListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  @Override
  public void serverConnected() {
    for (AnalysisServerListener listener : getListeners()) {
      listener.serverConnected();
    }
  }

  @Override
  public void serverError(boolean isFatal, String message, String stackTrace) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.serverError(isFatal, message, stackTrace);
    }
  }

  @Override
  public void serverStatus(ServerStatus status) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.serverStatus(status);
    }
  }

  /**
   * Returns an immutable copy of {@link #listeners}.
   */
  private List<AnalysisServerListener> getListeners() {
    synchronized (listeners) {
      return Lists.newArrayList(listeners);
    }
  }
}

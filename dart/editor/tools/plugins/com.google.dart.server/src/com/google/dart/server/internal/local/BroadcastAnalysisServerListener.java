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

package com.google.dart.server.internal.local;

import com.google.common.collect.Lists;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Outline;

import java.util.List;

/**
 * The class {@code BroadcastAnalysisServerListener} implements {@link AnalysisServerListener} that
 * broadcasts events to other listeners.
 * 
 * @coverage dart.server.local
 */
public class BroadcastAnalysisServerListener implements AnalysisServerListener {
  private final List<AnalysisServerListener> listeners = Lists.newArrayList();

  /**
   * Add the given listener to the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be added
   */
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    synchronized (listeners) {
      if (listeners.contains(listener)) {
        return;
      }
      listeners.add(listener);
    }
  }

  @Override
  public void computedErrors(String contextId, Source source, AnalysisError[] errors) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedErrors(contextId, source, errors);
    }
  }

  @Override
  public void computedHighlights(String contextId, Source source, HighlightRegion[] highlights) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedHighlights(contextId, source, highlights);
    }
  }

  @Override
  public void computedNavigation(String contextId, Source source, NavigationRegion[] targets) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedNavigation(contextId, source, targets);
    }
  }

  @Override
  public void computedOutline(String contextId, Source source, Outline outline) {
    for (AnalysisServerListener listener : getListeners()) {
      listener.computedOutline(contextId, source, outline);
    }
  }

  /**
   * Remove the given listener from the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be removed
   */
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
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

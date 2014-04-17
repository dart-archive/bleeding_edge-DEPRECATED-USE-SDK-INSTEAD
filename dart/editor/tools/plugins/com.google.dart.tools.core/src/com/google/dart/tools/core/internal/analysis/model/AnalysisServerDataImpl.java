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

import com.google.common.collect.Maps;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.NavigationRegion;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;

import java.util.Map;

/**
 * Instances of {@code AnalysisServerData} manage and provide access to analysis results reported by
 * {@link AnalysisServer}.
 * 
 * @coverage dart.tools.core.model
 */
public class AnalysisServerDataImpl implements AnalysisServerData {
  private final Map<String, Map<Source, NavigationRegion[]>> navigationData = Maps.newHashMap();

  @Override
  public NavigationRegion[] getNavigation(String contextId, Source source) {
    Map<Source, NavigationRegion[]> contextRegions = navigationData.get(contextId);
    if (contextRegions == null) {
      return NavigationRegion.EMPTY_ARRAY;
    }
    NavigationRegion[] sourceRegions = contextRegions.get(source);
    if (sourceRegions == null) {
      return NavigationRegion.EMPTY_ARRAY;
    }
    return sourceRegions;
  }

  public void internalComputedNavigation(String contextId, Source source, NavigationRegion[] targets) {
    Map<Source, NavigationRegion[]> contextRegions = navigationData.get(contextId);
    if (contextRegions == null) {
      contextRegions = Maps.newHashMap();
      navigationData.put(contextId, contextRegions);
    }
    contextRegions.put(source, targets);
  }

  /**
   * Deletes all the data associated with the given context.
   */
  public void internalDeleteContext(String contextId) {
    navigationData.remove(contextId);
  }
}

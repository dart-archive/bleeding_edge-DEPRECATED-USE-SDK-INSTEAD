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

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServerError;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Outline;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;

import org.eclipse.core.resources.IResource;

/**
 * Implementation of {@link AnalysisServerListener} for the Eclipse workspace.
 */
public class WorkspaceAnalysisServerListener implements AnalysisServerListener {
  private final AnalysisServerDataImpl dataImpl;

  public WorkspaceAnalysisServerListener(AnalysisServerDataImpl dataImpl) {
    this.dataImpl = dataImpl;
  }

  @Override
  public void computedErrors(String contextId, Source source, AnalysisError[] errors) {
    IResource resource = DartCore.getProjectManager().getResource(source);
    if (resource != null) {
      // TODO(scheglov) Analysis Server: LineInfo
      AnalysisMarkerManager.getInstance().queueErrors(resource, null, errors);
    }
  }

  @Override
  public void computedHighlights(String contextId, Source source, HighlightRegion[] highlights) {
  }

  @Override
  public void computedNavigation(String contextId, Source source, NavigationRegion[] targets) {
    dataImpl.internalComputedNavigation(contextId, source, targets);
  }

  @Override
  public void computedOutline(String contextId, Source source, Outline outline) {
  }

  /**
   * Deletes all the data associated with the given context.
   */
  public void internalDeleteContext(String contextId) {
    dataImpl.internalDeleteContext(contextId);
  }

  @Override
  public void onServerError(AnalysisServerError error) {
  }
}

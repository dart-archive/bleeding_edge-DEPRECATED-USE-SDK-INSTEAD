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

import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Outline;
import com.google.dart.server.ServerStatus;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager_NEW;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import org.eclipse.core.resources.IResource;

import java.io.File;

/**
 * Implementation of {@link AnalysisServerListener} for the Eclipse workspace.
 */
public class WorkspaceAnalysisServerListener implements AnalysisServerListener {
  private final AnalysisServerDataImpl dataImpl;

  public WorkspaceAnalysisServerListener(AnalysisServerDataImpl dataImpl) {
    this.dataImpl = dataImpl;
  }

  @Override
  public void computedErrors(String filePath, AnalysisError[] errors) {
    dataImpl.internalComputedErrors(filePath, errors);
    File file = new File(filePath);
    if (file.exists()) {
      IResource resource = ResourceUtil.getResource(file);
      if (resource != null) {
        // TODO(scheglov) Analysis Server: LineInfo
        AnalysisMarkerManager_NEW.getInstance().queueErrors(resource, null, errors);
      }
    }
  }

  @Override
  public void computedHighlights(String file, HighlightRegion[] highlights) {
    dataImpl.internalComputedHighlights(file, highlights);
  }

  @Override
  public void computedNavigation(String file, NavigationRegion[] targets) {
    dataImpl.internalComputedNavigation(file, targets);
  }

  @Override
  public void computedOutline(String file, Outline outline) {
    dataImpl.internalComputedOutline(file, outline);
  }

  @Override
  public void serverConnected() {
    // TODO(scheglov) Analysis Server
  }

  @Override
  public void serverError(boolean isFatal, String message, String stackTrace) {
    // TODO(scheglov) Analysis Server
  }

  @Override
  public void serverStatus(ServerStatus status) {
    // TODO(scheglov) Analysis Server
  }
}

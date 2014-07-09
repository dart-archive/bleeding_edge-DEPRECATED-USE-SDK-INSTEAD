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
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Occurrences;
import com.google.dart.server.Outline;
import com.google.dart.server.SearchResult;
import com.google.dart.server.ServerStatus;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager_NEW;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.File;

/**
 * Implementation of {@link AnalysisServerListener} for the Eclipse workspace.
 */
public class WorkspaceAnalysisServerListener implements AnalysisServerListener {
  private final AnalysisServerDataImpl dataImpl;
  private DartProjectManager projectManager;

  private final Object statusLock = new Object();
  private boolean statusAnalyzing = false;
  private Job statusJob;

  public WorkspaceAnalysisServerListener(AnalysisServerDataImpl dataImpl,
      DartProjectManager projectManager) {
    this.dataImpl = dataImpl;
    this.projectManager = projectManager;
  }

  @Override
  public void computedCompletion(String completionId, CompletionSuggestion[] completions,
      boolean last) {
    // TODO(jwren/scheglov) not yet implemented
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
  public void computedOccurrences(String file, Occurrences[] occurrencesArray) {
    // TODO(jwren/scheglov) not yet implemented
  }

  @Override
  public void computedOutline(String file, Outline outline) {
    dataImpl.internalComputedOutline(file, outline);
  }

  @Override
  public void computedSearchResults(String searchId, SearchResult[] results, boolean last) {
    //TODO (danrubel): not yet implemented
  }

  @Override
  public void serverConnected() {
    projectManager.start();
  }

  @Override
  public void serverError(boolean isFatal, String message, String stackTrace) {
    // TODO(scheglov) Analysis Server
  }

  @Override
  public void serverStatus(ServerStatus status) {
    synchronized (statusLock) {
      if (status.getAnalysisStatus().isAnalyzing()) {
        if (statusJob == null) {
          //
          // Start a build level job to display progress in the status area
          //
          statusAnalyzing = true;
          statusJob = new Job("Analyzing...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
              waitUntilAnalysisComplete();
              return Status.OK_STATUS;
            }
          };
          statusJob.setPriority(Job.BUILD);
          statusJob.schedule();
        }
      } else {
        if (statusJob != null) {
          //
          // Signal the status job to exit
          //
          statusAnalyzing = false;
          statusLock.notifyAll();
        }
      }
    }
  }

  private void waitUntilAnalysisComplete() {
    synchronized (statusLock) {
      while (statusAnalyzing) {
        try {
          statusLock.wait(3000);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      statusJob = null;
    }
  }
}

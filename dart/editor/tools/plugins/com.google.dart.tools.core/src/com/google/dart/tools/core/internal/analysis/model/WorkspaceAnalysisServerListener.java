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

import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.AnalysisStatus;
import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.server.generated.types.HighlightRegion;
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.Occurrences;
import com.google.dart.server.generated.types.Outline;
import com.google.dart.server.generated.types.OverrideMember;
import com.google.dart.server.generated.types.PubStatus;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.server.generated.types.RequestErrorCode;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager_NEW;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.utilities.io.PrintStringWriter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.File;
import java.util.List;

/**
 * Implementation of {@link AnalysisServerListener} for the Eclipse workspace.
 */
public class WorkspaceAnalysisServerListener implements AnalysisServerListener {
  private final AnalysisServerDataImpl dataImpl;
  private final DartProjectManager projectManager;

  private final Object statusLock = new Object();
  private boolean serverBusy = false;
  private Job statusJob;

  public WorkspaceAnalysisServerListener(AnalysisServerDataImpl dataImpl,
      DartProjectManager projectManager) {
    this.dataImpl = dataImpl;
    this.projectManager = projectManager;
  }

  @Override
  public void computedCompletion(String completionId, int replacementOffset, int replacementLength,
      List<CompletionSuggestion> completions, boolean isLast) {
    // TODO(jwren/scheglov) not yet implemented
  }

  @Override
  public void computedErrors(String filePath, List<AnalysisError> errors) {
    AnalysisError[] errorsArray = errors.toArray(new AnalysisError[errors.size()]);
    dataImpl.internalComputedErrors(filePath, errorsArray);
    scheduleResourceErrorMarkersUpdate(filePath, errorsArray);
  }

  @Override
  public void computedHighlights(String file, List<HighlightRegion> highlights) {
    HighlightRegion[] highlightsArray = highlights.toArray(new HighlightRegion[highlights.size()]);
    dataImpl.internalComputedHighlights(file, highlightsArray);
  }

  @Override
  public void computedLaunchData(String file, String kind, String[] referencedFiles) {
    dataImpl.internalComputedLaunchData(file, kind, referencedFiles);
  }

  @Override
  public void computedNavigation(String file, List<NavigationRegion> targets) {
    NavigationRegion[] targetsArray = targets.toArray(new NavigationRegion[targets.size()]);
    dataImpl.internalComputedNavigation(file, targetsArray);
  }

  @Override
  public void computedOccurrences(String file, List<Occurrences> occurrences) {
    Occurrences[] occurrencesArray = occurrences.toArray(new Occurrences[occurrences.size()]);
    dataImpl.internalComputedOccurrences(file, occurrencesArray);
  }

  @Override
  public void computedOutline(String file, Outline outline) {
    dataImpl.internalComputedOutline(file, outline);
  }

  @Override
  public void computedOverrides(String file, List<OverrideMember> overrideMember) {
    OverrideMember[] overridesArray = overrideMember.toArray(new OverrideMember[overrideMember.size()]);
    dataImpl.internalComputedOverrides(file, overridesArray);
  }

  @Override
  public void computedSearchResults(String searchId, List<SearchResult> results, boolean last) {
    dataImpl.internalComputedSearchResults(searchId, results, last);
  }

  @Override
  public void flushedResults(List<String> files) {
    // clear information
    dataImpl.internalFlushResults(files);
    // remove markers
    for (String file : files) {
      scheduleResourceErrorMarkersUpdate(file, AnalysisError.EMPTY_ARRAY);
    }
  }

  @Override
  public void requestError(RequestError requestError) {
    String errorCode = requestError.getCode();
    if (RequestErrorCode.SERVER_ERROR.equals(errorCode)) {
      DartCore.logRequestError(requestError);
    }
  }

  @Override
  public void serverConnected(String version) {
    projectManager.start();
  }

  @Override
  public void serverError(boolean isFatal, String message, String stackTrace) {
    @SuppressWarnings("resource")
    PrintStringWriter buf = new PrintStringWriter();
    buf.println("ServerError: Fatal=" + isFatal);
    buf.println(message);
    buf.println(stackTrace);
    DartCore.logError(buf.toString());
  }

  @Override
  public void serverIncompatibleVersion(String version) {
    // TODO(scheglov) not yet implemented
  }

  @Override
  public void serverStatus(AnalysisStatus analysisStatus, PubStatus pubStatus) {
    dataImpl.internalServerStatus(analysisStatus);
    String statusMessage = getStatus(analysisStatus, pubStatus);
    synchronized (statusLock) {
      if (statusMessage != null) {
        serverBusy = true;
        if (statusJob == null) {
          //
          // Start a build level job to display progress in the status area.
          //
          statusJob = new Job(statusMessage) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
              waitUntilServerIdle();
              return Status.OK_STATUS;
            }
          };
          statusJob.setPriority(Job.BUILD);
          statusJob.schedule();
        } else {
          statusJob.setName(statusMessage);
        }
      } else {
        if (statusJob != null) {
          //
          // Signal the status job to exit.
          //
          serverBusy = false;
          statusLock.notifyAll();
        }
      }
    }
  }

  /**
   * Return the status message that should be displayed given the analysis and pub status objects,
   * or {@code null} if there is no status message to be displayed.
   * 
   * @param analysisStatus the current analysis status of the server, or {@code null} if there is no
   *          analysis status
   * @param pubStatus the current pub status of the server, or {@code null} if there is no pub
   *          status
   * @return the status message that should be displayed
   */
  private String getStatus(AnalysisStatus analysisStatus, PubStatus pubStatus) {
    if (pubStatus != null && pubStatus.isListingPackageDirs()) {
      return "Running pub...";
    } else if (analysisStatus != null && analysisStatus.isAnalyzing()) {
      return "Analyzing...";
    }
    return null;
  }

  private void scheduleResourceErrorMarkersUpdate(String filePath, AnalysisError[] errors) {
    File file = new File(filePath);
    if (file.exists()) {
      IResource resource = ResourceUtil.getResource(file);
      if (resource != null) {
        AnalysisMarkerManager_NEW.getInstance().queueErrors(resource, errors);
      }
    }
  }

  private void waitUntilServerIdle() {
    synchronized (statusLock) {
      while (serverBusy) {
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

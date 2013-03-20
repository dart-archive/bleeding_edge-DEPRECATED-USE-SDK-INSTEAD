/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.ArrayList;

/**
 * Instances of {@code AnalysisWorker} perform analysis by repeatedly calling
 * {@link AnalysisContext#performAnalysisTask()} and update both the index and the error markers
 * based upon the analysis results.
 */
public class AnalysisWorker {

  /**
   * The result of a parse or resolve operation.
   */
  private static class Results {
    private final IResource resource;
    private final LineInfo lineInfo;
    private final AnalysisError[] errors;

    public Results(IResource resource, LineInfo lineInfo, AnalysisError[] errors) {
      this.resource = resource;
      this.lineInfo = lineInfo;
      this.errors = errors;
    }

    /**
     * Set markers on the specified resource to represent the cached analysis errors
     */
    public void showErrors() throws CoreException {
      resource.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_ZERO);
      for (AnalysisError error : errors) {
        int severity;
        ErrorSeverity errorSeverity = error.getErrorCode().getErrorSeverity();
        if (errorSeverity == ErrorSeverity.ERROR) {
          severity = IMarker.SEVERITY_ERROR;
        } else if (errorSeverity == ErrorSeverity.WARNING) {
          severity = IMarker.SEVERITY_WARNING;
//        } else if (errorSeverity == ErrorSeverity.INFO) {
//          severity = IMarker.SEVERITY_INFO;
        } else {
          continue;
        }

        int lineNum = lineInfo.getLocation(error.getOffset()).getLineNumber();

        IMarker marker = resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.CHAR_START, error.getOffset());
        marker.setAttribute(IMarker.CHAR_END, error.getOffset() + error.getLength());
        marker.setAttribute(IMarker.LINE_NUMBER, lineNum);
//        marker.setAttribute("errorCode", error.getErrorCode());
        marker.setAttribute(IMarker.MESSAGE, error.getMessage());
      }
    }
  }

  /**
   * The project containing the source for this context.
   */
  protected final Project project;

  /**
   * The analysis context on which analysis is performed.
   */
  protected final AnalysisContext context;

  /**
   * A collection of analysis errors that have yet to be translated to markers.
   */
  private ArrayList<Results> results;

  /**
   * The index to be updated (not {@code null}).
   */
  private final Index index;

  /**
   * Construct a new instance for performing analysis.
   * 
   * @param project the project containing sources for the specified context (not {@code null})
   * @param context the context used to perform the analysis (not {@code null})
   * @param index the index to be updated (not {@code null})
   */
  public AnalysisWorker(Project project, AnalysisContext context, Index index) {
    this.project = project;
    this.context = context;
    this.index = index;
  }

  /**
   * Perform analysis by repeatedly calling {@link AnalysisContext#performAnalysisTask()} and update
   * both the index and the error markers based upon the analysis results.
   */
  public void performAnalysis() {
    ChangeNotice[] changes = context.performAnalysisTask();
    while (processResults(changes) && checkContext()) {
      changes = context.performAnalysisTask();
    }
    showCachedErrors(DartCore.DART_PROBLEM_MARKER_TYPE);
  }

  /**
   * Subclasses may override this method to call various "get" methods on the context looking to see
   * if information it needs is cached.
   * 
   * @return {@code true} if analysis should continue, or {@code false} to exit the
   *         {@link #performAnalysis()} loop.
   */
  protected boolean checkContext() {
    return true;
  }

  /**
   * Update the index and error markers based upon the specified change.
   * 
   * @param change the analysis change (not {@code null})
   */
  private void processChange(ChangeNotice change) {

    // If errors are available, then queue the errors to be translated to markers
    AnalysisError[] errors = change.getErrors();
    if (errors != null) {
      Source source = change.getSource();
      IResource res = project.getResource(source);
      if (res == null) {
        // TODO (danrubel): log unmatched sources once context only returns errors for added sources
//        DartCore.logError("Failed to determine resource for: " + source);
      } else {
        LineInfo lineInfo = change.getLineInfo();
        if (lineInfo == null) {
          DartCore.logError("Missing line information for: " + source);
        } else {
          if (results == null) {
            results = new ArrayList<Results>();
          }
          results.add(new Results(res, lineInfo, errors));
        }
      }
    }

    // If there is a unit to be indexed, then do so
    CompilationUnit unit = change.getCompilationUnit();
    if (unit != null) {
      index.indexUnit(context, unit);
    }
  }

  /**
   * Update both the index and the error markers based upon the analysis results.
   * 
   * @param changes the changes or {@code null} if there is no more work to be done
   * @return {@code true} if there may be more analysis, or {@code false} if not
   */
  private boolean processResults(ChangeNotice[] changes) {

    // if no more tasks, then return false indicating analysis is complete
    if (changes == null) {
      return false;
    }

    // process results and return true indicating there might be more analysis
    for (ChangeNotice change : changes) {
      processChange(change);
    }
    return true;
  }

  /**
   * Add markers for the cached analysis errors.
   * 
   * @param markerType the type of marker to be created (not {@code null})
   */
  private void showCachedErrors(final String markerType) {
    if (results == null) {
      return;
    }
    final ArrayList<Results> resultsToShow = results;
    results = null;
    IWorkspaceRunnable op = new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        for (Results result : resultsToShow) {
          result.showErrors();
        }
      }
    };
    try {
      project.getResource().getWorkspace().run(op, null);
    } catch (CoreException e) {
      DartCore.logError("Exception translating errors/warnings into markers", e);
    }
  }
}

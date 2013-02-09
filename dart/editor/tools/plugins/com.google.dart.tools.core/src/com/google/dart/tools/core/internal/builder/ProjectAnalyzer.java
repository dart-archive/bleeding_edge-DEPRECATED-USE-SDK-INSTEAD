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
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * {@code ProjectAnalyzer} analyzes sources in a project and updates Eclipse markers. Attach
 * instances of {@code ProjectAnalyzer} to a delta processor via
 * {@link DeltaProcessor#addDeltaListener(DeltaListener)} then call the appropriate
 * {@link DeltaProcessor} traverse method. Once the traverse method completes, call
 * {@link #updateMarkers()} to update the Eclipse markers.
 */
public class ProjectAnalyzer extends AbstractDeltaListener {

  /**
   * A collection of changes for a specific analysis context
   */
  private class ChangeSet {

    private final Project project;
    private final AnalysisContext context;
    private final ArrayList<Source> changedPackageSources = new ArrayList<Source>();
    private final ArrayList<Source> changedSources = new ArrayList<Source>();
    private final HashMap<IResource, AnalysisError[]> errorMap = new HashMap<IResource, AnalysisError[]>();

    ChangeSet(Project project, AnalysisContext context) {
      this.project = project;
      this.context = context;
    }

    void addPackageSource(Source source) {
      changedPackageSources.add(source);
    }

    void addSource(Source source) {
      changedSources.add(source);
    }

    /**
     * Answer the resource associated with the specified source or null if the resource should be
     * ignored or cannot be determined
     */
    IResource getResourceFor(Source source) {
      IResource res = project.getResourceFor(source);
      if (res == null) {
        DartCore.logError("Failed to determine resource for: " + source);
        return null;
      }
      IPath location = res.getLocation();
      if (location == null) {
        DartCore.logError("Failed to determine location for " + res);
        return null;
      }
      if (ignoreManager.isIgnored(location.toPortableString())) {
        return null;
      }
      return res;
    }

    /**
     * Parse all changed sources (not changed package sources) in this set and update the markers.
     * 
     * @param monitor the progress monitor (not <code>null</code>) to use for reporting progress to
     *          the user and detecting whether the operation has been canceled
     */
    void parseChanged(IProgressMonitor monitor) {
      for (Source source : changedSources) {
        if (monitor.isCanceled()) {
          return;
        }
        monitor.setTaskName("Parsing " + source.getShortName());
        parseSource(source);
      }
      // TODO (danrubel): pass DART_PARSE_PROBLEM_MARKER_TYPE
      showCachedErrors(DartCore.DART_PROBLEM_MARKER_TYPE);
    }

    /**
     * Parse the source and queue the markers to be created.
     * 
     * @param source the source to be parsed (not {@code null})
     */
    void parseSource(Source source) {
      IResource res = getResourceFor(source);
      if (res == null) {
        return;
      }
      try {
        CompilationUnit unit = context.parse(source);
        errorMap.put(res, unit.getParsingErrors());
      } catch (AnalysisException e) {
        DartCore.logError("Exception parsing source: " + source, e);
        return;
      }
    }

    /**
     * Query the context for the set of sources that need to be resolved, then resolve and update
     * the markers.
     * 
     * @param monitor the progress monitor (not <code>null</code>) to use for reporting progress to
     *          the user and detecting whether the operation has been canceled
     */
    void resolve(IProgressMonitor monitor) {
      ArrayList<Source> allChangedSources = new ArrayList<Source>(changedSources.size()
          + changedPackageSources.size());
      allChangedSources.addAll(changedSources);
      allChangedSources.addAll(changedPackageSources);
      Iterator<Source> iter = context.sourcesToResolve(
          changedSources.toArray(new Source[changedSources.size()])).iterator();
      while (iter.hasNext()) {
        if (monitor.isCanceled()) {
          return;
        }
        Source source = iter.next();
        monitor.setTaskName("Resolving " + source.getShortName());
        try {
          resolveSource(source);
        } catch (Exception e) {
          // TODO (danrubel): Remove this once semantic errors are reported
          if (!reportedNoSemanticErrors) {
            reportedNoSemanticErrors = true;
            DartCore.logError(">>>> Semantic error reporting not implemented yet", e);
          }
        }
      }
      // TODO (danrubel): pass DART_RESOLUTION_PROBLEM_MARKER_TYPE
      showCachedErrors(null);
    }

    /**
     * Resolve the source and queue the markers to be created.
     * 
     * @param source the source to be resolved (not {@code null})
     */
    void resolveSource(Source source) {
      LibraryElement library = context.getLibraryElement(source);
      if (library == null) {
        DartCore.logError("Failed to determine library for source: " + source);
        return;
      }
      IResource res = getResourceFor(source);
      if (res == null) {
        return;
      }
      // TODO (danrubel): do not show errors on sources in the "packages" directory
      try {
        CompilationUnit unit = context.resolve(source, library);
        errorMap.put(res, unit.getResolutionErrors());
      } catch (AnalysisException e) {
        DartCore.logError("Exception resolving source: " + source, e);
        return;
      }
    }

    void showCachedErrors(final String markerType) {
      IWorkspaceRunnable op = new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          for (Entry<IResource, AnalysisError[]> entry : errorMap.entrySet()) {
            IResource resource = entry.getKey();
            // TODO (danrubel): Split DartCore.DART_PROBLEM_MARKER_TYPE 
            // into syntactic markers and semantic markers 
            // so that each can be cleared and created in separate passes
            if (markerType != null) {
              resource.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
            }
            showErrors(resource, entry.getValue());
          }
          errorMap.clear();
        }
      };
      try {
        ResourcesPlugin.getWorkspace().run(op, null);
      } catch (CoreException e) {
        DartCore.logError("Exception translating errors/warnings into markers", e);
      }
    }

    /**
     * Add markers on the specified resource representing the specified analysis errors
     * 
     * @param resource the resource (not {@code null})
     * @param errorsToShow the errors to be shown (not {@code null}, contains no {@code null}s)
     */
    void showErrors(IResource resource, AnalysisError[] errorsToShow) throws CoreException {
      for (AnalysisError error : errorsToShow) {
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

        IMarker marker = resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.CHAR_START, error.getOffset());
        marker.setAttribute(IMarker.CHAR_END, error.getOffset() + error.getLength());
//        marker.setAttribute(IMarker.LINE_NUMBER, error.getLineNumber());
//        marker.setAttribute("errorCode", error.getErrorCode());
        marker.setAttribute(IMarker.MESSAGE, error.getMessage());
      }
    }
  }

  /**
   * A mapping of {@link AnalysisContext} to a set of sources in that analysis context having
   * content has changedSources.
   */
  HashMap<AnalysisContext, ChangeSet> changeSets = new HashMap<AnalysisContext, ChangeSet>();

  // TODO (danrubel): remove this once semantic errors are being reported
  private static boolean reportedNoSemanticErrors = false;

  /**
   * The object (not {@code null}) used to manage which resources should be not be analyzed.
   */
  private final DartIgnoreManager ignoreManager;

  public ProjectAnalyzer(DartIgnoreManager ignoreManager) {
    this.ignoreManager = ignoreManager;
  }

  /**
   * Update markers for any sources that were parsed.
   * 
   * @param monitor the progress monitor (not <code>null</code>) to use for reporting progress to
   *          the user and detecting whether the operation has been canceled
   */
  public void analyze(IProgressMonitor monitor) {
    for (ChangeSet changes : changeSets.values()) {
      if (monitor.isCanceled()) {
        return;
      }
      changes.parseChanged(monitor);
      if (monitor.isCanceled()) {
        return;
      }
      changes.resolve(monitor);
    }
  }

  @Override
  public void packageSourceAdded(SourceDeltaEvent event) {
    getChangeSet(event).addPackageSource(event.getSource());
  }

  @Override
  public void packageSourceChanged(SourceDeltaEvent event) {
    getChangeSet(event).addPackageSource(event.getSource());
  }

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    getChangeSet(event).addSource(event.getSource());
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    getChangeSet(event).addSource(event.getSource());
  }

  /**
   * Answer the change set used to analyze the source in the specified event.
   * 
   * @param event the event (not {@code null})
   * @return the change set (not {@code null})
   */
  private ChangeSet getChangeSet(SourceDeltaEvent event) {
    AnalysisContext context = event.getContext();
    ChangeSet changes = changeSets.get(context);
    if (changes == null) {
      changes = new ChangeSet(event.getProject(), context);
      changeSets.put(context, changes);
    }
    return changes;
  }
}

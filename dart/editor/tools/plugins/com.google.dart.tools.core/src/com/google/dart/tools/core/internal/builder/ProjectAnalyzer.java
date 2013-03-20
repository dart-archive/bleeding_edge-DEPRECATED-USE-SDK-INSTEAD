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
import com.google.dart.engine.index.Index;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * {@code ProjectAnalyzer} analyzes sources in a project, updates Eclipse markers, and updates the
 * index. Attach instances of {@code ProjectAnalyzer} to a delta processor via
 * {@link DeltaProcessor#addDeltaListener(DeltaListener)} then call the appropriate
 * {@link DeltaProcessor} traverse method. Once the traverse method completes, call
 * {@link #analyze(IProgressMonitor)} to update the markers and the index.
 */
public class ProjectAnalyzer extends DeltaAdapter {

  /**
   * A collection of changes for a specific analysis context
   */
  private class ChangeSet {

    private final Project project;
    private final AnalysisContext context;
    private final ArrayList<Source> changedPackageSources = new ArrayList<Source>();
    private final ArrayList<Source> changedSources = new ArrayList<Source>();
    private final HashMap<IResource, Result> results = new HashMap<IResource, Result>();

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
      IResource res = project.getResource(source);
      if (res == null) {
        DartCore.logError("Failed to determine resource for: " + source);
        return null;
      }
      if (ignoreManager.isIgnored(res)) {
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
      showCachedErrors(DartCore.DART_PARSING_PROBLEM_MARKER_TYPE);
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
      CompilationUnit unit;
      try {
        unit = context.parseCompilationUnit(source);
      } catch (AnalysisException e) {
        DartCore.logError("Exception parsing source: " + source, e);
        return;
      }
      results.put(res, new Result(unit, unit.getParsingErrors()));
    }

    /**
     * Query the context for the set of sources that need to be resolved, then resolve and update
     * the markers.
     * 
     * @param monitor the progress monitor (not <code>null</code>) to use for reporting progress to
     *          the user and detecting whether the operation has been canceled
     */
    void resolve(IProgressMonitor monitor) {
      ArrayList<Source> sources;
      if (changedSources.size() == 0) {
        sources = changedPackageSources;
      } else if (changedPackageSources.size() == 0) {
        sources = changedSources;
      } else {
        sources = new ArrayList<Source>(changedSources.size() + changedPackageSources.size());
        sources.addAll(changedSources);
        sources.addAll(changedPackageSources);
      }
      Source[] sourcesArray = sources.toArray(new Source[sources.size()]);
      Iterator<Source> iter = context.sourcesToResolve(sourcesArray).iterator();
      while (iter.hasNext()) {
        if (monitor.isCanceled()) {
          return;
        }
        Source source = iter.next();
        monitor.setTaskName("Resolving " + source.getShortName());
        try {
          resolveSource(source);
        } catch (Exception e) {
          // TODO (danrubel): Remove this once engine and analyzer are more stable
          logError("Exception resolving source", e);
        }
      }
      showCachedErrors(DartCore.DART_RESOLUTION_PROBLEM_MARKER_TYPE);
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
      CompilationUnit unit;
      try {
        unit = context.resolveCompilationUnit(source, library);
      } catch (AnalysisException e) {
        DartCore.logError("Exception resolving source: " + source, e);
        return;
      }
      results.put(res, new Result(unit, unit.getResolutionErrors()));
      index.indexUnit(context, unit);
    }

    /**
     * Add markers for the cached analysis errors
     * 
     * @param markerType the type of marker to be created (not {@code null})
     */
    void showCachedErrors(final String markerType) {
      IWorkspaceRunnable op = new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          for (Entry<IResource, Result> entry : results.entrySet()) {
            entry.getValue().showErrors(entry.getKey(), markerType);
          }
          results.clear();
        }
      };
      try {
        ResourcesPlugin.getWorkspace().run(op, null);
      } catch (CoreException e) {
        DartCore.logError("Exception translating errors/warnings into markers", e);
      }
    }
  }

  /**
   * The result of a parse or resolve operation
   */
  private class Result {
    private final LineInfo lineInfo;
    private final AnalysisError[] errors;

    public Result(CompilationUnit unit, AnalysisError[] errors) {
      this.lineInfo = unit.getLineInfo();
      this.errors = errors;
    }

    /**
     * Set markers on the specified resource to represent the cached analysis errors
     * 
     * @param resource the resource (not {@code null})
     * @param markerType the type of marker to be created (not {@code null})
     */
    public void showErrors(IResource resource, String markerType) throws CoreException {
      resource.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
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

        IMarker marker = resource.createMarker(markerType);
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
   * A mapping of {@link AnalysisContext} to a set of sources in that analysis context having
   * content has changedSources.
   */
  HashMap<AnalysisContext, ChangeSet> changeSets = new HashMap<AnalysisContext, ChangeSet>();

  // TODO (danrubel): remove this once engine, analyzer, and index are more stable
  private static int numberOfErrorsLogged = 0;

  /**
   * The object (not {@code null}) used to manage which resources should be not be analyzed.
   */
  private final DartIgnoreManager ignoreManager;

  /**
   * The index to be updated (not {@code null}).
   */
  private final Index index;

  public ProjectAnalyzer(DartIgnoreManager ignoreManager, Index index) {
    this.ignoreManager = ignoreManager;
    this.index = index;
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
  public void packageSourceContainerRemoved(SourceContainerDeltaEvent event) {
    index.removeSources(event.getContext(), event.getSourceContainer());
  }

  @Override
  public void packageSourceRemoved(SourceDeltaEvent event) {
    index.removeSource(event.getContext(), event.getSource());
  }

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    getChangeSet(event).addSource(event.getSource());
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    getChangeSet(event).addSource(event.getSource());
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    index.removeSources(event.getContext(), event.getSourceContainer());
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    index.removeSource(event.getContext(), event.getSource());
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

  // TODO (danrubel): Remove this once engine, analyzer, and index are more stable
  private void logError(String message, Exception e) {
    if (numberOfErrorsLogged < 10) {
      numberOfErrorsLogged++;
      DartCore.logError(message, e);
    }
  }
}

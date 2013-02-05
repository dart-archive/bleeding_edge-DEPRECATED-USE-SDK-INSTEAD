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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.PubFolder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
  public static class ChangeSet {

    private final PubFolder pubFolder;
    private final AnalysisContext context;
    private final HashSet<Source> changed = new HashSet<Source>();

    public ChangeSet(PubFolder pubFolder, AnalysisContext context) {
      this.pubFolder = pubFolder;
      this.context = context;
    }

    public Source[] getChangedSources() {
      return changed.toArray(new Source[changed.size()]);
    }

    public AnalysisContext getContext() {
      return context;
    }

    public PubFolder getPubFolder() {
      return pubFolder;
    }

    private void add(Source source) {
      changed.add(source);
    }
  }

  /**
   * Internal class for caching a parse result for later conversion to markers
   */
  private class ParseResult {
    private final IResource resource;
    private final AnalysisError[] errors;

    public ParseResult(IResource resource, AnalysisError[] errors) {
      this.resource = resource;
      this.errors = errors;
    }

    public void updateMarkers() throws CoreException {
      // TODO (danrubel): Split DartCore.DART_PROBLEM_MARKER_TYPE 
      // into syntactic markers and semantic markers 
      // so that each can be cleared and created in separate passes
      resource.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ZERO);

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
   * content has changed.
   */
  HashMap<AnalysisContext, ChangeSet> changeSets = new HashMap<AnalysisContext, ChangeSet>();

  /**
   * A collection of results to be converted into markers
   */
  private final ArrayList<ParseResult> parseResults = new ArrayList<ProjectAnalyzer.ParseResult>();

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    parse(event);
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    parse(event);
  }

  /**
   * Update markers for any sources that were parsed
   */
  public void updateMarkers() {
    IWorkspaceRunnable op = new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        while (parseResults.size() > 0) {
          parseResults.remove(parseResults.size() - 1).updateMarkers();
        }
        parseResults.clear();
      }
    };
    try {
      ResourcesPlugin.getWorkspace().run(op, null);
    } catch (CoreException e) {
      DartCore.logError("Exception translating errors/warnings into markers", e);
    }
  }

  /**
   * Parse the specified source file and cache the result such that error markers can be created
   * when {@link #updateMarkers()} is called
   * 
   * @param event the source event (not {@code null})
   */
  private void parse(SourceDeltaEvent event) {
    AnalysisContext context = event.getContext();
    Source source = event.getSource();

    // Cache the changed sources for later resolution
    ChangeSet changes = changeSets.get(context);
    if (changes == null) {
      changes = new ChangeSet(event.getPubFolder(), context);
      changeSets.put(context, changes);
    }
    changes.add(source);

    // Parse the changed source
    try {
      CompilationUnit unit = context.parse(source);
      parseResults.add(new ParseResult(event.getResource(), unit.getSyntacticErrors()));
    } catch (AnalysisException e) {
      DartCore.logError("Exception parsing source: " + source, e);
      return;
    }
  }
}

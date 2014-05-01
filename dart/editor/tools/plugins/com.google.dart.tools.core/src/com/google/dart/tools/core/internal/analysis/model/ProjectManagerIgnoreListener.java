package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.builder.DeltaAdapter;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.ResourceDeltaEvent;
import com.google.dart.tools.core.internal.builder.SourceDeltaEvent;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.File;

/**
 * Instances of {@code ProjectManagerIgnoreListener} listen for changes broadcast by the
 * {@link DartIgnoreManager} and update the contexts affected by that change.
 */
public class ProjectManagerIgnoreListener implements DartIgnoreListener {
  private final ProjectManager projectManager;
  private final IWorkspaceRoot workspaceRoot;
  private final AnalysisManager analysisManager;
  private final AnalysisMarkerManager markerManager;
  private final Index index;

  public ProjectManagerIgnoreListener(ProjectManager projectManager, IWorkspaceRoot workspaceRoot,
      AnalysisManager analysisManager, AnalysisMarkerManager markerManager, Index index) {
    this.projectManager = projectManager;
    this.workspaceRoot = workspaceRoot;
    this.analysisManager = analysisManager;
    this.markerManager = markerManager;
    this.index = index;
  }

  @Override
  public void ignoresChanged(DartIgnoreEvent event) {
    String[] ignoresAdded = event.getAdded();
    String[] ignoresRemoved = event.getRemoved();
    if (ignoresAdded.length > 0) {
      processIgnoresAdded(ignoresAdded);
    }
    if (ignoresRemoved.length > 0) {
      processIgnoresRemoved(ignoresRemoved);
    }
  }

  /**
   * Answer the resource with the given path.
   * 
   * @param path the absolute path to the resource, not <code>null</code>
   * @return the resource or <code>null</code> if no match is found
   */
  private IResource getResourceFromPath(String path) {
    Path location = new Path(path);
    if (isFilePath(location)) {
      return workspaceRoot.getFileForLocation(location);
    } else {
      return workspaceRoot.getContainerForLocation(location);
    }
  }

  /**
   * Determine if the given path represents an existing file. If no such file or directory exists,
   * then return true if the specified path has a file extension.
   * 
   * @param location the path
   * @return <code>true</code> if the path is for a file
   */
  private boolean isFilePath(Path location) {
    File file = location.toFile();
    if (file.isFile()) {
      return true;
    }
    if (file.isDirectory()) {
      return false;
    }
    // File does not exist, so guess based upon the extension
    return location.getFileExtension() != null;
  }

  /**
   * Update all contexts based upon the specified paths to be ignored.
   * 
   * @param paths an array of zero or more paths to be ignored, not <code>null</code>
   */
  private void processIgnoresAdded(String[] paths) {
    for (String path : paths) {
      IResource resource = getResourceFromPath(path);
      if (resource != null) {
        //
        // Visit each source and notify the appropriate context that it should not be analyzed
        //
        final Project project = projectManager.getProject(resource.getProject());
        if (project != null) {
          DeltaProcessor processor = new DeltaProcessor(project);
          DeltaAdapter visitor = new DeltaAdapter() {
            AnalysisContext context;
            AnalysisDelta delta;

            @Override
            public void packageSourceAdded(SourceDeltaEvent event) {
              Source source = event.getSource();
              if (source != null) {
                delta.setAnalysisLevel(source, AnalysisLevel.NONE);
                index.removeSource(context, source);
              }
            }

            @Override
            public void sourceAdded(SourceDeltaEvent event) {
              Source source = event.getSource();
              if (source != null) {
                delta.setAnalysisLevel(source, AnalysisLevel.NONE);
                index.removeSource(context, source);
              }
            }

            @Override
            public void visitContext(ResourceDeltaEvent event) {
              if (context != null) {
                context.applyAnalysisDelta(delta);
              }
              if (event != null) {
                context = event.getContext();
                delta = new AnalysisDelta();
              }
            }
          };
          processor.addDeltaListener(visitor);
          try {
            processor.traverse(resource);
          } catch (CoreException e) {
            DartCore.logError("Failed to ignore " + resource, e);
          }
          visitor.visitContext(null);
        }
        markerManager.clearMarkers(resource);
      }
    }
  }

  /**
   * Update all contexts based upon the specified paths to be analyzed.
   * 
   * @param paths an array of zero or more paths to be included in analysis, not <code>null</code>
   */
  private void processIgnoresRemoved(String[] paths) {
    for (String path : paths) {
      IResource resource = getResourceFromPath(path);
      if (resource != null && resource.isAccessible()) {
        //
        // Visit each source and notify the appropriate context that it should be analyzed
        //
        final Project project = projectManager.getProject(resource.getProject());
        if (project != null) {
          DeltaProcessor processor = new DeltaProcessor(project);
          DeltaAdapter visitor = new DeltaAdapter() {
            AnalysisContext context;
            AnalysisDelta delta;

            @Override
            public void packageSourceAdded(SourceDeltaEvent event) {
              Source source = event.getSource();
              if (source != null) {
                delta.setAnalysisLevel(source, AnalysisLevel.RESOLVED);
              }
            }

            @Override
            public void sourceAdded(SourceDeltaEvent event) {
              Source source = event.getSource();
              if (source != null) {
                delta.setAnalysisLevel(source, AnalysisLevel.ALL);
                AnalysisErrorInfo errorInfo = context.getErrors(source);
                markerManager.queueErrors(
                    projectManager.getResource(source),
                    errorInfo.getLineInfo(),
                    errorInfo.getErrors());
              }
            }

            @Override
            public void visitContext(ResourceDeltaEvent event) {
              if (context != null) {
                context.applyAnalysisDelta(delta);
                analysisManager.performAnalysisInBackground(project, context);
              }
              if (event != null) {
                context = event.getContext();
                delta = new AnalysisDelta();
              }
            }
          };
          processor.addDeltaListener(visitor);
          try {
            processor.traverse(resource);
          } catch (CoreException e) {
            DartCore.logError("Failed to analyze " + resource, e);
          }
          visitor.visitContext(null);
        }
      }
    }
  }
}

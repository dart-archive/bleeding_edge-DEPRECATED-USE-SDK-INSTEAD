package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;

import static com.google.dart.tools.core.DartCore.isDartLikeFileName;
import static com.google.dart.tools.core.DartCore.isHtmlLikeFileName;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Instances of {@code ProjectManagerIgnoreListener} listen for changes broadcast by the
 * {@link DartIgnoreManager} and update the contexts affected by that change.
 */
public class ProjectManagerIgnoreListener implements DartIgnoreListener {
  private final ProjectManager projectManager;
  private final IWorkspaceRoot workspaceRoot;
  private AnalysisManager analysisManager;
  private AnalysisMarkerManager markerManager;

  public ProjectManagerIgnoreListener(ProjectManager projectManager, IWorkspaceRoot workspaceRoot,
      AnalysisManager analysisManager, AnalysisMarkerManager markerManager) {
    this.projectManager = projectManager;
    this.workspaceRoot = workspaceRoot;
    this.analysisManager = analysisManager;
    this.markerManager = markerManager;
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
   * Add the given sources to the given context and start background analysis.
   */
  private void analyzeSources(Project project, AnalysisContext context, List<Source> sources) {
    ChangeSet changeSet = new ChangeSet();
    for (Source source : sources) {
      changeSet.addedSource(source);
    }
    context.applyChanges(changeSet);
    for (Source source : sources) {
      AnalysisErrorInfo errorInfo = context.getErrors(source);
      markerManager.queueErrors(
          projectManager.getResource(source),
          errorInfo.getLineInfo(),
          errorInfo.getErrors());
    }
    analysisManager.performAnalysisInBackground(project, context);
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
   * Answer the sources associated with or contained in the given resource.
   * 
   * @param resource the resource, not <code>null</code>
   * @return a collection of zero or more sources, not <code>null</code>
   */
  private List<Source> getSourcesIn(IResource resource) {
    final AnalysisContext context = projectManager.getContext(resource);
    final List<Source> sources = new ArrayList<Source>();
    try {
      resource.accept(new IResourceProxyVisitor() {
        @Override
        public boolean visit(IResourceProxy proxy) throws CoreException {
          if (proxy.getType() == IResource.FILE) {
            String name = proxy.getName();
            if (isDartLikeFileName(name) || isHtmlLikeFileName(name)) {
              if (proxy.requestResource().getLocation() != null) {
                Source source = new FileBasedSource(proxy.requestResource().getLocation().toFile());
                sources.add(source);
              }
            }
          } else if (proxy.getType() == IResource.FOLDER) {
            if (proxy.getName().startsWith(".")) {
              // Do not include sources in hidden folders
              return false;
            }
            if (projectManager.getContext(proxy.requestResource()) != context) {
              // Do not include sources from other contexts
              return false;
            }
          }
          return true;
        }
      },
          0);
    } catch (CoreException e) {
      DartCore.logError(e);
    }
    return sources;
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
        ChangeSet changeSet = new ChangeSet();
        if (resource instanceof IFile) {
          changeSet.removedSource(projectManager.getSource((IFile) resource));
        } else {
          IContainer container = (IContainer) resource;
          changeSet.removedContainer(new DirectoryBasedSourceContainer(new File(path)));
          for (PubFolder pubFolder : projectManager.getContainedPubFolders(container)) {
            pubFolder.getContext().applyChanges(changeSet);
          }
        }
        projectManager.getContext(resource).applyChanges(changeSet);
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
        Project project = projectManager.getProject(resource.getProject());
        if (project != null) {
          List<Source> sources;
          if (resource instanceof IFile) {
            sources = new ArrayList<Source>();
            sources.add(projectManager.getSource((IFile) resource));
          } else {
            IContainer container = (IContainer) resource;
            for (PubFolder pubFolder : projectManager.getContainedPubFolders(container)) {
              analyzeSources(project, pubFolder.getContext(), getSourcesIn(pubFolder.getResource()));
            }
            sources = getSourcesIn(resource);
          }
          analyzeSources(project, projectManager.getContext(resource), sources);
        }
      }
    }
  }
}

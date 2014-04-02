/*
 * Copyright 2013 Dart project authors.
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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectEvent;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.analysis.model.ResolvedHtmlEvent;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.instrumentation.InstrumentationLogger;
import com.google.dart.tools.core.internal.builder.AnalysisEngineParticipant;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.model.DartIgnoreListener;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Concrete implementation of {@link ProjectManager}.
 * 
 * @coverage dart.tools.core.model
 */
public class ProjectManagerImpl extends ContextManagerImpl implements ProjectManager {

  private static final PubFolder[] NO_PUB_FOLDERS = new PubFolder[] {};

  private final IWorkspaceRoot resource;
  private final HashMap<IProject, Project> projects = new HashMap<IProject, Project>();
  private final Index index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());
  private final DartIgnoreManager ignoreManager;
  private final ArrayList<ProjectListener> listeners = new ArrayList<ProjectListener>();

  private final AnalysisListener indexNotifier = new AnalysisListener() {
    @Override
    public void complete(AnalysisEvent event) {
    }

    @Override
    public void resolved(ResolvedEvent event) {
      index.indexUnit(event.getContext(), event.getUnit());
    }

    @Override
    public void resolvedHtml(ResolvedHtmlEvent event) {
      index.indexHtmlUnit(event.getContext(), event.getUnit());
    }
  };

  /**
   * A listener that updates the manager when a project is closed. In addition, this listener
   * processes changes in packages directory because Eclipse builders do not receive deltas for
   * changes in symlinked folders.
   */
  private IResourceChangeListener resourceChangeListener = new WorkspaceDeltaProcessor(this);

  private DartIgnoreListener ignoreListener = new ProjectManagerIgnoreListener(
      this,
      ResourcesPlugin.getWorkspace().getRoot(),
      AnalysisManager.getInstance(),
      AnalysisMarkerManager.getInstance(),
      index);

  public ProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk, DartIgnoreManager ignoreManager) {
    super(sdk);
    this.resource = resource;
    this.ignoreManager = ignoreManager;
  }

  @Override
  public void addProjectListener(ProjectListener listener) {
    synchronized (listeners) {
      if (listener != null && !listeners.contains(listener)) {
        listeners.add(listener);
      }
    }
  }

  @Override
  public PubFolder[] getContainedPubFolders(IContainer container) {
    Project project = getProject(container.getProject());
    if (project != null) {
      return project.getContainedPubFolders(container);
    }
    return NO_PUB_FOLDERS;
  }

  @Override
  public AnalysisContext getContext(IResource resource) {
    if (resource == null) {
      return null;
    }
    Project project = getProject(resource.getProject());
    if (project == null) {
      return null;
    }
    return project.getContext(resource);
  }

  @Override
  public IResource getHtmlFileForLibrary(Source source) {
    AnalysisContext context = getContext(getResource(source));
    if (context != null) {
      Source[] htmlSource = context.getHtmlFilesReferencing(source);
      if (htmlSource.length > 0) {
        return getResource(htmlSource[0]);
      }
    }
    return null;
  }

  @Override
  public DartIgnoreManager getIgnoreManager() {
    return ignoreManager;
  }

  @Override
  public Index getIndex() {
    return index;
  }

  @Override
  public Source[] getLibrarySources(IFile file) {
    AnalysisContext context = getContext(file);
    Source source = getSource(file);
    return context.getLibrariesContaining(source);
  }

  @Override
  public Source[] getLibrarySources(IProject projectResource) {
    Project project = getProject(projectResource);
    if (project == null) {
      return Source.EMPTY_ARRAY;
    }
    return project.getLibrarySources();
  }

  @Override
  public Project getProject(IProject resource) {
    if (!resource.exists()) {
      return null;
    }
    synchronized (projects) {
      Project result = projects.get(resource);
      if (result == null) {
        if (resource.getFile("lib/_internal/libraries.dart").exists()) {
          //
          // If this file exists, then the project is assumed to have been opened on the root of an
          // SDK. In such a case we want to use the SDK being edited as the SDK for analysis so that
          // analysis engine can correctly recognize files within dart:core. Failure to do so causes
          // analysis engine to implicitly import (a potentially different version of) dart:core
          // into the dart:core files in the project, which leads to a large number of false
          // positives.
          //
          result = new ProjectImpl(resource, new DirectoryBasedDartSdk(
              resource.getLocation().toFile()));
        } else {
          result = new ProjectImpl(resource, getSdk());
        }
        projects.put(resource, result);
      }
      return result;
    }
  }

  @Override
  public IProject getProjectForContext(AnalysisContext context) {
    for (Project project : getProjects()) {
      if (project.isContextInProject(context)) {
        return project.getResource();
      }
    }
    return null;
  }

  @Override
  public Project[] getProjects() {
    IProject[] childResources = resource.getProjects();
    List<Project> result = new ArrayList<Project>();
    for (int index = 0; index < childResources.length; index++) {
      IProject prj = childResources[index];
      try {
        if (prj.hasNature(DartCore.DART_PROJECT_NATURE)) {
          Project project = getProject(childResources[index]);
          if (project != null) {
            result.add(project);
          }
        }
      } catch (CoreException e) {
        // do nothing, just continue
      }
    }
    return result.toArray(new Project[result.size()]);
  }

  @Override
  public PubFolder getPubFolder(IResource resource) {
    Project project = getProject(resource.getProject());
    if (project == null) {
      return null;
    }
    return project.getPubFolder(resource);
  }

  @Override
  public IWorkspaceRoot getResource() {
    return resource;
  }

  @Override
  public IResource getResource(Source source) {
    // TODO (danrubel): revisit and optimize performance
    if (source == null) {
      return null;
    }
    for (Project project : getProjects()) {
      IResource res = project.getResource(source);
      if (res != null) {
        return res;
      }
    }
    return null;
  }

  @Override
  public ResourceMap getResourceMap(AnalysisContext context) {
    for (Project project : getProjects()) {
      ResourceMap map = project.getResourceMap(context);
      if (map != null) {
        return map;
      }
    }
    return null;
  }

  @Override
  public ResourceMap getResourceMap(IResource resource) {
    Project project = getProject(resource.getProject());
    if (project == null) {
      return null;
    }
    return project.getResourceMap(resource);
  }

  @Override
  public void hookListeners() {
    resource.getWorkspace().addResourceChangeListener(resourceChangeListener);
    ignoreManager.addListener(ignoreListener);
    AnalysisWorker.addListener(indexNotifier);
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    IResource resource = getResource(librarySource);
    if (resource != null) {
      AnalysisContext context = getContext(resource);
      return context.isClientLibrary(librarySource);
    }
    return false;
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    IResource resource = getResource(librarySource);
    if (resource != null) {
      AnalysisContext context = getContext(resource);
      return context.isServerLibrary(librarySource);
    }
    return false;
  }

  @Override
  public SearchEngine newSearchEngine() {
    return SearchEngineFactory.createSearchEngine(getIndex());
  }

  @Override
  public void projectAnalyzed(Project project) {
    final ProjectEvent event = new ProjectEvent(project);
    ProjectListener[] currentListeners = listeners.toArray(new ProjectListener[listeners.size()]);
    for (ProjectListener listener : currentListeners) {
      try {
        listener.projectAnalyzed(event);
      } catch (Exception e) {
        DartCore.logError("Exception while notifying listeners project was analyzed", e);
      }
    }
  }

  @Override
  public void projectRemoved(IProject projectResource) {
    Project result;
    synchronized (projects) {
      result = projects.remove(projectResource);
    }
    if (result != null) {
      result.discardContextsIn(projectResource);
    }
  }

  @Override
  public void removeProjectListener(ProjectListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  @Override
  public IFile resolvePackageUri(IResource relativeTo, String uri) {
    ResourceMap map = getResourceMap(relativeTo);
    if (map == null) {
      return null;
    }

    Source source = map.getContext().getSourceFactory().forUri(uri);

    if (source == null) {
      return null;
    } else {
      return map.getResource(source);
    }
  }

  @Override
  public String resolvePathToPackage(IResource resource, String path) {
    Project project = getProject(resource.getProject());
    if (project != null) {
      return project.resolvePathToPackage(path);
    }
    return null;
  }

  @Override
  public IFileInfo resolveUriToFileInfo(IResource relativeTo, String uri) {
    Project project = getProject(relativeTo.getProject());
    if (project != null) {
      return project.resolveUriToFileInfo(relativeTo, uri);
    }
    return null;
  }

  @Override
  public void setDart2JSHintOption(boolean enableDart2JSHints) {
    for (Project project : getProjects()) {
      project.setDart2JSHintOption(enableDart2JSHints);
    }
  }

  @Override
  public void setHintOption(boolean enableHint) {
    for (Project project : getProjects()) {
      project.setHintOption(enableHint);
    }
  }

  @Override
  public void start() {
    new Thread() {
      @Override
      public void run() {
        index.run();
      }
    }.start();
    InstrumentationLogger.ensureLoggerStarted();
    new AnalysisWorker(this, getSdkContext()).performAnalysisInBackground();
    analyzeAllProjects();
  }

  @Override
  public void stop() {
    resource.getWorkspace().removeResourceChangeListener(resourceChangeListener);
    ignoreManager.removeListener(ignoreListener);
    AnalysisWorker.removeListener(indexNotifier);
    AnalysisMarkerManager.getInstance().stop();
    index.stop();
  }

  private void analyzeAllProjects() {
    for (Project project : getProjects()) {
      BuildEvent event = new BuildEvent(project.getResource(), null, new NullProgressMonitor());
      AnalysisEngineParticipant participant = new AnalysisEngineParticipant(
          this,
          AnalysisMarkerManager.getInstance());
      try {
        participant.build(event, new NullProgressMonitor());
      } catch (CoreException e) {
        DartCore.logError(e);
      }
    }
  }
}

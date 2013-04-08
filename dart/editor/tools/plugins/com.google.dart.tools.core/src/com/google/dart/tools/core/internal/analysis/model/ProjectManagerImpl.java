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
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectEvent;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.jobs.CleanLibrariesJob;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Concrete implementation of {@link ProjectManager}
 */
public class ProjectManagerImpl extends ContextManagerImpl implements ProjectManager {

  private final IWorkspaceRoot resource;
  private final HashMap<IProject, Project> projects = new HashMap<IProject, Project>();
  private final Index index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());
  private final DartSdk sdk;
  private final DartIgnoreManager ignoreManager;
  private final ArrayList<ProjectListener> listeners = new ArrayList<ProjectListener>();

  /**
   * A listener that updates the manager when a project is closed.
   */
  private IResourceChangeListener

  resourceChangeListener = new IResourceChangeListener() {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      IResourceDelta delta = event.getDelta();
      if (delta != null) {
        try {
          delta.accept(new IResourceDeltaVisitor() {
            @Override
            public boolean visit(IResourceDelta delta) throws CoreException {
              IResource res = delta.getResource();
              if (res == null) {
                return false;
              }
              if (res.getType() == IResource.ROOT) {
                return true;
              }
              if (res.getType() == IResource.PROJECT) {
                if (delta.getKind() == IResourceDelta.REMOVED) {
                  removeProject((IProject) res);
                }
              }
              return false;
            }
          });
        } catch (CoreException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  };

  public ProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk, DartIgnoreManager ignoreManager) {
    this.resource = resource;
    this.sdk = sdk;
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
  public AnalysisContext getContext(IResource resource) {
    return getProject(resource.getProject()).getContext(resource);
  }

  @Override
  public IResource getHtmlFileForLibrary(Source source) {
    AnalysisContext context = getContext(getResource(source));
    Source[] htmlSource = context.getHtmlFilesReferencing(source);
    if (htmlSource.length > 0) {
      return getResource(htmlSource[0]);
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
  public Source[] getLaunchableClientLibrarySources() {
    List<Source> sources = new ArrayList<Source>();
    for (Project project : getProjects()) {
      sources.addAll(Arrays.asList(project.getLaunchableClientLibrarySources()));
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLaunchableClientLibrarySources(IProject project) {
    Project prj = getProject(project);
    return prj.getLaunchableClientLibrarySources();
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    List<Source> sources = new ArrayList<Source>();
    for (Project project : getProjects()) {
      sources.addAll(Arrays.asList(project.getLaunchableServerLibrarySources()));
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLaunchableServerLibrarySources(IProject project) {
    Project prj = getProject(project);
    return prj.getLaunchableServerLibrarySources();
  }

  @Override
  public Source[] getLibrarySources(IFile file) {
    AnalysisContext context = getContext(file);
    Source source = getSource(file);
    return context.getLibrariesContaining(source);
  }

  @Override
  public Source[] getLibrarySources(IProject project) {
    return getProject(project).getLibrarySources();
  }

  @Override
  public Project getProject(IProject resource) {
    synchronized (projects) {
      Project result = projects.get(resource);
      if (result == null) {
        result = new ProjectImpl(resource, sdk);
        projects.put(resource, result);
      }
      return result;
    }
  }

  @Override
  public Project[] getProjects() {
    IProject[] childResources = resource.getProjects();
    Project[] result = new Project[childResources.length];
    for (int index = 0; index < result.length; index++) {
      result[index] = getProject(childResources[index]);
    }
    return result;
  }

  @Override
  public PubFolder getPubFolder(IResource resource) {
    return getProject(resource.getProject()).getPubFolder(resource);
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
  public DartSdk getSdk() {
    return sdk;
  }

  @Override
  public AnalysisContext getSdkContext() {
    return sdk.getContext();
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
  public void removeProjectListener(ProjectListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
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
    resource.getWorkspace().addResourceChangeListener(resourceChangeListener);
    new CleanLibrariesJob().schedule();
  }

  @Override
  public void stop() {
    resource.getWorkspace().removeResourceChangeListener(resourceChangeListener);
    index.stop();
  }

  /**
   * Called by the {@link #resourceChangeListener} when a project has been removed.
   * 
   * @param projectResource the project that was removed
   */
  private void removeProject(IProject projectResource) {
    Project result;
    synchronized (projects) {
      result = projects.remove(projectResource);
    }
    if (result != null) {
      result.discardContextsIn(projectResource);
    }
  }
}

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

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectEvent;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Concrete implementation of {@link ProjectManager}
 */
public class ProjectManagerImpl extends ContextManagerImpl implements ProjectManager {

  private final IWorkspaceRoot resource;
  @VisibleForTesting
  protected final HashMap<IProject, Project> projects = new HashMap<IProject, Project>();
  private final Index index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());
  private final DartSdk sdk;
  private final AnalysisContext sdkContext;
  private final DartIgnoreManager ignoreManager;
  private final ArrayList<ProjectListener> listeners = new ArrayList<ProjectListener>();

  public ProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk, DartIgnoreManager ignoreManager) {
    this.resource = resource;
    this.sdk = sdk;
    this.sdkContext = AnalysisEngine.getInstance().createAnalysisContext();
    this.sdkContext.setSourceFactory(new SourceFactory(new DartUriResolver(sdk)));
    this.ignoreManager = ignoreManager;
    // TODO(scheglov) Dan, can you check if this is correct place to start Index? Where to stop?
    new Thread() {
      @Override
      public void run() {
        index.run();
      }
    }.start();
  }

  @Override
  public void addProjectListener(ProjectListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
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
    Project result = projects.get(resource);
    if (result == null) {
      result = new ProjectImpl(resource, sdk);
      projects.put(resource, result);
    }
    return result;
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
    return sdkContext;
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
    AnalysisContext context = getContext(getResource(librarySource));
    return context.isServerLibrary(librarySource);
  }

  @Override
  public SearchEngine newSearchEngine() {
    return SearchEngineFactory.createSearchEngine(getIndex());
  }

  @Override
  public void projectAnalyzed(Project project) {
    final ProjectEvent event = new ProjectEvent(project);
    for (ProjectListener listener : listeners) {
      try {
        listener.projectAnalyzed(event);
      } catch (Exception e) {
        DartCore.logError("Exception while notifying listeners project was analyzed", e);
      }
    }
  }

  @Override
  public void removeProjectListener(ProjectListener listener) {
    listeners.remove(listener);
  }

}

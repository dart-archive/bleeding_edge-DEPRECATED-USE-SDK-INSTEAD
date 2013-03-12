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
import com.google.dart.tools.core.internal.builder.DeltaAdapter;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.SourceDeltaEvent;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete implementation of {@link ProjectManager}
 */
public class ProjectManagerImpl extends ContextManagerImpl implements ProjectManager {

  private final IWorkspaceRoot resource;
  @VisibleForTesting
  protected final HashMap<IProject, Project> projects = new HashMap<IProject, Project>();
  private final Index index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());
  private final DartSdk sdk;
  private final DartIgnoreManager ignoreManager;
  private final ArrayList<ProjectListener> listeners = new ArrayList<ProjectListener>();

  public ProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk, DartIgnoreManager ignoreManager) {
    this.resource = resource;
    this.sdk = sdk;
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

    // TODO(keertip): implement when there is API in the context to get to the 
    // html file that has a reference to a particular library source

    //AnalysisContext context = getContext(getResource(source));
    // Source htmlSource = context.getHtmlForLibrary(source);
    // return getResource(source);
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
  public Source[] getLibrarySources(IResource resource) {
    final AnalysisContext context = getContext(resource);
    if (resource instanceof IContainer) {
      final Set<Source> sources = new HashSet<Source>();
      Project project = getProject(resource.getProject());
      DeltaProcessor processor = new DeltaProcessor(project);
      processor.addDeltaListener(new DeltaAdapter() {
        @Override
        public void sourceAdded(SourceDeltaEvent event) {
          Source source = getSource((IFile) event.getResource());
          if (source != null) {
            sources.addAll(Arrays.asList(context.getLibrariesContaining(source)));
          }
        }
      });
      try {
        processor.traverse((IContainer) resource);
      } catch (CoreException e) {
        DartCore.logError("Failed to traverse container", e);
      }
      return sources.toArray(new Source[sources.size()]);
    } else {
      Source source = getSource((IFile) resource);
      return context.getLibrariesContaining(source);
    }
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

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
package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

/**
 * Instances of {@code ProjectManager} manage Eclipse projects that have the Dart nature.
 */
public interface ProjectManager extends ContextManager {

  /**
   * Notify the specified object when a project has been analyzed.
   * 
   * @param listener the object to be notified (not {@code null})
   */
  void addProjectListener(ProjectListener listener);

  /**
   * Answer with the html file which has a reference to the library represented by the given source
   * 
   * @param source the source for a library
   * @return IResource that is the html file with a reference to the given library, or {@code null}
   */
  IResource getHtmlFileForLibrary(Source source);

  /**
   * Answer the global ignore manager used for all Dart source
   * 
   * @return the ignore manager (not {@code null})
   */
  DartIgnoreManager getIgnoreManager();

  /**
   * Answer the global index used for all Dart source
   * 
   * @return the index (not {@code null})
   */
  Index getIndex();

  /**
   * Answer with all the library sources for the given project that can be launched on the browser
   * 
   * @return library sources for the given project that can be launched on the browser
   */
  Source[] getLaunchableClientLibrarySources(IProject project);

  /**
   * Answer with all the library sources for the given project that can be launched on the VM
   * 
   * @return library sources for the given project that can be launched on the VM
   */
  Source[] getLaunchableServerLibrarySources(IProject project);

  /**
   * Answer with all the library sources that the given resource is part of or is the library file
   * 
   * @return the {@link Source}[] for all the libraries that the given resource is part of or is the
   *         library file
   */
  Source[] getLibrarySources(IResource resource);

  /**
   * Answer the project for the specified Eclipse resource
   * 
   * @param resource the Eclipse resource
   * @return the project (not {@code null})
   */
  Project getProject(IProject resource);

  /**
   * Answer an array containing all of the projects currently defined in the workspace
   * 
   * @return array of projects (not {@code null}, contains no {@code null})
   */
  Project[] getProjects();

  /**
   * Answer the underlying Eclipse workspace associated with this object
   * 
   * @return the Eclipse workspace (not {@code null})
   */
  IWorkspaceRoot getResource();

  /**
   * Answer the {@link DartSdk} default Dart SDK
   * 
   * @return the sdk (not {@code null})
   */
  DartSdk getSdk();

  /**
   * Create and answer a new search engine backed by the global index
   * 
   * @return a search engine (not {@code null})
   */
  SearchEngine newSearchEngine();

  /**
   * Called by the builder when a project has been analyzed.
   * 
   * @param project the project that was analyzed (not {@code null})
   */
  void projectAnalyzed(Project project);

  /**
   * Stop notifying the specified object when a project has been analyzed.
   * 
   * @param listener the object that should not be notified (not {@code null})
   */
  void removeProjectListener(ProjectListener listener);
}

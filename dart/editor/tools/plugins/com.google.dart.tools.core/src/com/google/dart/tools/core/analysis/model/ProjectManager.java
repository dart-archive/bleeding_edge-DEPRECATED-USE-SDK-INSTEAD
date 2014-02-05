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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.internal.analysis.model.WorkspaceDeltaProcessor;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

/**
 * Instances of {@code ProjectManager} manage Eclipse projects that have the Dart nature.
 * 
 * @coverage dart.tools.core.model
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
   * Answer with all the library sources that the given file is part of or is the library file
   * 
   * @return the {@link Source}[] for all the libraries that the given file is part of or is the
   *         library file
   */
  Source[] getLibrarySources(IFile file);

  /**
   * Answer with all the library sources that are in the given project. These include all the sdk
   * and external libraries referenced by code in the project
   * 
   * @return the {@link Source}[] for all the libraries that are in the given project.
   */
  Source[] getLibrarySources(IProject project);

  /**
   * Answer the project for the specified Eclipse resource
   * 
   * @param resource the Eclipse resource
   * @return the project, may be {@code null} if resource doesn't exist
   */
  Project getProject(IProject resource);

  /**
   * Answer the Eclipse project associated with this analysis context.
   * 
   * @param context the analysis context
   * @return the associated project, may be {@code null} if project doesn't exist
   */
  IProject getProjectForContext(AnalysisContext context);

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
  @Override
  IWorkspaceRoot getResource();

  /**
   * Attach listeners for resource changes, files to be ignored, and the like.
   */
  void hookListeners();

  /**
   * Answer if the given source is known to be the defining compilation unit of a library that can
   * be run on a client
   * 
   * @param librarySource the source
   * @return {@code true} if the given source is known to be a library that can be run on a client
   */
  boolean isClientLibrary(Source librarySource);

  /**
   * Answer if the given source is known to be the defining compilation unit of a library that can
   * be run on the server
   * 
   * @param librarySource the source
   * @return {@code true} if the given source is known to be a library that can be run on the server
   */
  boolean isServerLibrary(Source librarySource);

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
   * Called by the {@link WorkspaceDeltaProcessor} when a project has been removed.
   * 
   * @param projectResource the project that was removed
   */
  void projectRemoved(IProject projectResource);

  /**
   * Stop notifying the specified object when a project has been analyzed.
   * 
   * @param listener the object that should not be notified (not {@code null})
   */
  void removeProjectListener(ProjectListener listener);

  /**
   * Use the getResourceMap() method to locate a workspace resource for the given package uri.
   * 
   * @param relativeTo the resource to use to locate an analysis context
   * @param uri a package: uri
   * @return a workspace resource or {@code null}
   */
  IFile resolvePackageUri(IResource relativeTo, String uri);

  /**
   * Resolve the given file path to a package uri path, if any
   * 
   * @param resource the given resource
   * @param path the file path for the resource wrt to package structure
   * @return the package name or {@code null} if resource is not in a package
   */
  String resolvePathToPackage(IResource resource, String path);

  /**
   * Start background analysis such as updating the index.
   */
  void start();

  /**
   * Stop background analysis.
   */
  void stop();
}

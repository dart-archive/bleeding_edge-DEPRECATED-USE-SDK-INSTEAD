/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.model.DartLibrary;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Instances of the class <code>DartModelInfo</code> maintain the cached data shared by all model objects.
 */
public class DartModelInfo extends OpenableElementInfo {
  /**
   * The bundled core library.
   */
  private DartLibrary coreLibrary;

  /**
   * The bundled libraries.
   */
  private DartLibrary[] bundledLibraries;

  /**
   * A cached array containing all of the non-Dart projects in the workspace.
   */
  private IProject[] nonDartProjects;

  /**
   * Initialize a newly created information holder.
   */
  public DartModelInfo() {
    initializeBundledLibraries();
  }

  /**
   * Return an array containing the bundled libraries.
   * 
   * @return an array containing the bundled libraries
   */
  public DartLibrary[] getBundledLibraries() {
    return bundledLibraries;
  }

  /**
   * Return the bundled core library.
   * 
   * @return the bundled core library
   */
  public DartLibrary getCoreLibrary() {
    return coreLibrary;
  }

  /**
   * Return an array containing all of the non-Dart projects in the workspace.
   * 
   * @return an array containing all of the non-Dart projects in the workspace
   */
  public IProject[] getNonDartResources() {
    if (nonDartProjects == null) {
      initializeCache();
    }
    return nonDartProjects;
  }

  private DartLibraryImpl createBundledLibrary(PackageLibraryManager libraryManager, String urlSpec)
      throws URISyntaxException {
    URI libUri = new URI(urlSpec);
    return new DartLibraryImpl(new UrlLibrarySource(libUri, libraryManager));
  }

  private void initializeBundledLibraries() {
    ArrayList<DartLibrary> libraries = new ArrayList<DartLibrary>();
    try {
      PackageLibraryManager libraryManager = PackageLibraryManagerProvider
        .getPackageLibraryManager();
      coreLibrary = createBundledLibrary(libraryManager, "dart:core");
      for (String spec : libraryManager.getAllLibrarySpecs()) {
        libraries.add(createBundledLibrary(libraryManager, spec));
      }
    } catch (URISyntaxException exception) {
      throw new AssertionError(exception);
    } finally {
      bundledLibraries = libraries.toArray(new DartLibrary[libraries.size()]);
    }
  }

  private void initializeCache() {
    initializeBundledLibraries();
    initializeNonDartProjects();
  }

  private void initializeNonDartProjects() {
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    int count = projects.length;
    ArrayList<IProject> nonDart = new ArrayList<IProject>(count);
    for (IProject project : projects) {
      if (!DartProjectNature.hasDartNature(project)) {
        nonDart.add(project);
      }
    }
    nonDartProjects = nonDart.toArray(new IProject[nonDart.size()]);
  }
}

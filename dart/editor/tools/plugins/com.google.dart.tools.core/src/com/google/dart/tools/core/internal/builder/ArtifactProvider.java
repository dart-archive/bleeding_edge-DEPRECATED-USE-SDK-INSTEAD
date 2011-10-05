/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.DefaultDartArtifactProvider;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.backend.js.JavascriptBackend;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * An Eclipse specific implementation of {@link DartArtifactProvider} for
 * <ul>
 * <li>locating generated files in a directory separate from source files</li>
 * <li>marking generated files as derived</li>
 * <li>tracking projects in which files are generated, which directly correlates to projects upon
 * which the receiver's project depends</li>
 * </ul>
 * <p>
 * Consumers of this class should call {@link #refreshAndMarkDerived(IProgressMonitor)} after
 * compilation is complete regardless of whether any compilation errors occurred.
 */
public class ArtifactProvider extends DefaultDartArtifactProvider {

  /**
   * Answer the JavaScript application file for the specified source.
   * 
   * @param source the application source file (not <code>null</code>)
   * @return the application file (may not exist)
   */
  public static File getJsAppArtifactFile(IResource source) {
    return source.getLocation().removeFileExtension().addFileExtension(
        JavascriptBackend.EXTENSION_APP_JS).toFile();
  }

  /**
   * Answer the artifact folder for the specified project
   * 
   * @param project the project (not <code>null</code>)
   * @return the folder (may not exist)
   */
  private static IContainer getArtifactFolder(IProject project) {
    DartProject dartProject = DartCore.create(project);

    return ResourcesPlugin.getWorkspace().getRoot().getFolder(dartProject.getArtifactLocation());
  }

  /**
   * Resources that need to be marked as derived/generated after compilation is complete.
   */
  private Collection<IResource> derivedResources = new HashSet<IResource>();

  /**
   * Resources that need to be refreshed after compilation is complete so that the Eclipse state is
   * in-sync with on-disk changes made by the Dart compiler.
   */
  private Collection<IResource> refreshResources = new HashSet<IResource>();

  /**
   * A collection of projects containing artifacts that were referenced during compilation.
   */
  private final Collection<IProject> prerequisiteProjects = new HashSet<IProject>();

  /**
   * Clear the cache of projects upon which the receiver's project depends
   */
  public void clearPrerequisiteProjects() {
    prerequisiteProjects.clear();
  }

  /**
   * Delete the files generated from bundled libraries
   */
  public void deleteBundledLibraryOutput() {
    BuilderUtil.deleteAll(getBundledLibraryOutputLocation().toFile());
  }

  /**
   * Answer the location where output files for bundled libraries are stored.
   * <p>
   * TODO(devoncarew): this location is not specific to any project but to the whole workspace.
   * Perhaps move it to DartCore or DartModel?
   */
  public IPath getBundledLibraryOutputLocation() {
    return DartCore.getPlugin().getStateLocation().append("out");
  }

  /**
   * Answer the projects upon which the receiver's project depends
   */
  public IProject[] getPrerequisiteProjects() {
    return prerequisiteProjects.toArray(new IProject[prerequisiteProjects.size()]);
  }

  /**
   * Refresh any queued resources and mark each as derived. This should be called after compilation
   * is complete regardless of whether any compilation errors occurred.
   */
  public void refreshAndMarkDerived(IProgressMonitor monitor) {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    // Refresh resources

    for (IResource res : refreshResources) {
      if (!res.exists()) {
        continue;
      }
      try {
        res.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      } catch (CoreException e) {
        Util.log(e, "Failed to refresh resource: " + res);
      }
    }
    refreshResources.clear();

    // Mark resources as derived

    for (IResource res : derivedResources) {
      if (!res.exists()) {
        continue;
      }
      try {
        res.setDerived(true, monitor);
      } catch (CoreException e) {
        Util.log(e, "Failed to mark resource as derived: " + res);
      }
    }
    derivedResources.clear();
  }

  /**
   * Override superclass implementation to refresh and mark them derived once compilation is
   * complete. Place all derived resources in a "out" folder at the project level
   */
  @Override
  protected File getArtifactFile(Source source, String part, String extension) {
    File file;

    // Determine the artifact file for a file in the workspace

    IResource srcRes = ResourceUtil.getResource(source);
    if (srcRes != null) {
      file = getLocalArtifactFile(source, srcRes, extension);
    }

    // Determine the artifact file for a file not in the workspace
    // either on disk or bundled in an installed plugin

    else {
      file = getNonLocalArtifactFile(source, extension);
    }

    if (file == null) {
      throw new RuntimeException("Failed to determine resource for source: "
          + source.getClass().getSimpleName() + "[" + source.getName() + "]");
    }

    // If a component part is specified...
    if (!part.isEmpty()) {
      String name = file.getName();

      // Sanity check
      if (!name.endsWith("." + extension)) {
        throw new RuntimeException("Expected '" + name + "' to end with '." + extension + "'");
      }

      int index = name.length() - extension.length() - 1;
      name = name.substring(0, index) + "$" + part + "." + extension;
      file = new File(file.getParentFile(), name);
    }
    if (file != null) {
      File parentDir = file.getParentFile();
      if (!parentDir.exists()) {
        parentDir.mkdirs();
      }
    }
    return file;
  }

  // TODO (danrubel): investigate bundling artifacts in Dart Editor
  @Override
  protected DartSource getBundledArtifact(Source source, Source base, String part, String extension) {
    return null;
  }

  /**
   * Delete the derived resources in the project's "out" folder
   */
  void deleteDerivedDartResources(IProject project, IProgressMonitor monitor) throws CoreException {
    // TODO(devoncarew): we may also want to remove any application or library .js files
    // written to the output folder.
    IContainer artifactFolder = getArtifactFolder(project);

    if (artifactFolder.exists()) {
      deleteDerivedResources(artifactFolder, monitor);
    }
  }

  /**
   * Create the folder if it does not already exist
   * 
   * @param container the folder to be created (not <code>null</code>)
   */
  private void createFolder(IContainer container) {
    if (container.exists()) {
      return;
    }
    IContainer parent = container.getParent();
    if (!parent.exists()) {
      createFolder(parent);
    }
    try {
      ((IFolder) container).create(false, true, null);
      derivedResource(container);
    } catch (Exception e) {
      //
      // Sometimes the folder gets created on a different thread before we try to create it. We
      // don't really care how it got created as long as it exists.
      //
      if (container.exists()) {
        return;
      }
      throw new RuntimeException("Failed to create folder: " + container, e);
    }
  }

  /**
   * Perform a "clean" by removing all derived resources
   * 
   * @param container the container to be cleaned
   */
  private void deleteDerivedResources(IContainer container, IProgressMonitor monitor)
      throws CoreException {
    IResource[] members = container.members();
    for (IResource res : members) {
      if (res instanceof IContainer) {
        deleteDerivedResources((IContainer) res, monitor);
      }
      if (res instanceof IFile) {
        if (res.isDerived()) {
          res.delete(false, monitor);
        }
      }
    }
    if (container.isDerived() && container.members().length == 0) {
      container.delete(false, monitor);
    }
  }

  /**
   * Queue the resource for later post-compilation processing. Callers of this method should call
   * {@link #refreshAndMarkDerived(IProgressMonitor)} after compilation is complete regardless of
   * whether any compilation errors occurred.
   * 
   * @param res the derived resource (not <code>null</code>)
   */
  private void derivedResource(IResource res) {
    derivedResources.add(res);
    IContainer parent = res.getParent();
    while (!parent.exists()) {
      parent = parent.getParent();
    }
    refreshResources.add(parent);
  }

  /**
   * Answer a file in the "out" folder of the specified project. This file may not exist and is not
   * marked derived by this method.
   * 
   * @param project the project (not <code>null</code>)
   * @param libPath the Dart library path (not <code>null</code>, not empty)
   * @param srcPath the source file path (not <code>null</code>, not empty)
   * @param extension the artifact extension (not <code>null</code>, not empty)
   * @return the artifact file (parent containers may not exist)
   */
  private IFile getArtifactFile(IProject project, String libPath, String srcPath, String extension) {
    Path libName = new Path(new Path(libPath).lastSegment());
    Path srcName = new Path(new Path(srcPath + "." + extension).lastSegment());
    return getArtifactFolder(project).getFile(libName.append(srcName));
  }

  /**
   * Answer the artifact file for a source file located within the workspace
   * 
   * @param source the source file
   * @param sourceResource the corresponding resource within the workspace
   * @param extension the file extension for this artifact (not null, not empty)
   * @return the artifact file or <code>null</code> if it could not be determined
   */
  private File getLocalArtifactFile(Source source, IResource sourceResource, String extension) {
    IProject project = sourceResource.getProject();
    prerequisiteProjects.add(project);

    // Get the file (may not exist)
    IFile file;
    if (source instanceof DartSource) {
      DartSource dartSource = (DartSource) source;
      String libPath = stripFileExtension(dartSource.getLibrary().getName());
      String srcPath = dartSource.getName();
      file = getArtifactFile(project, libPath, srcPath, extension);
    } else if (source instanceof LibrarySource) {
      String srcPath = source.getName();
      String libPath = stripFileExtension(srcPath);
      if (extension.equals(JavascriptBackend.EXTENSION_APP_JS)) {
        return getJsAppArtifactFile(sourceResource);
      } else if (extension.equals(JavascriptBackend.EXTENSION_APP_JS_SRC_MAP)) {
        return sourceResource.getLocation().removeFileExtension().addFileExtension(
            JavascriptBackend.EXTENSION_APP_JS_SRC_MAP).toFile();
      } else {
        file = getArtifactFile(project, libPath, srcPath, extension);
      }
    } else {
      return null;
    }

    // Ensure that the folder that contains the file exists
    createFolder(file.getParent());

    // Queue the artifact to be refreshed and marked as derived
    derivedResource(file);
    return file.getLocation().toFile();
  }

  /**
   * Answer the artifact file for a source file located outside the workspace on disk or bundled in
   * a plugin.
   * 
   * @param source the source file
   * @param extension the file extension for this artifact (not null, not empty)
   * @return the artifact file or <code>null</code> if it could not be determined
   */
  private File getNonLocalArtifactFile(Source source, String extension) {
    LibrarySource library;
    if (source instanceof DartSource) {
      library = ((DartSource) source).getLibrary();
    } else if (source instanceof LibrarySource) {
      library = (LibrarySource) source;
    } else {
      return null;
    }
    String libName = new Path(library.getName()).lastSegment();
    String srcName = new Path(source.getName()).lastSegment();
    File file = getBundledLibraryOutputLocation().append(libName).append(srcName + "." + extension).toFile();
    File dir = file.getParentFile();
    if (!dir.exists() && !dir.mkdirs()) {
      if (!dir.exists()) { // check again for safety
        throw new RuntimeException("Failed to create directory for library " + libName + ": " + dir);
      }
    }
    return file;
  }

  /**
   * Answer name without any file extension.
   */
  private String stripFileExtension(String name) {
    if (name == null) {
      return null;
    }
    int index = name.lastIndexOf('.');
    if (index == -1) {
      return name;
    }
    return name.substring(0, index);
  }
}

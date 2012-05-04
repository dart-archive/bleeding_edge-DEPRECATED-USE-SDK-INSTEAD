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
package com.google.dart.tools.core;

import com.google.dart.tools.core.generator.DartProjectGenerator;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.File;

/**
 * Utility methods for {@link OpenFileHandler}
 */
public class OpenFileUtil {
  private static final String DART_PROJECT_NAME_PREFIX = "DartProject";
  private static int dartProjectCount = 0;

  /**
   * Find the first Dart application or library file in the directory containing the specified file
   * or any parent directories which contains a reference to the specified file. If the specified
   * file is a Dart application or library file, then return that file.
   * 
   * @return the application or library file, or <code>null</code> if none found.
   */
  @Deprecated
  public static File getFirstAppOrLib(File file) {
    return getFirstAppOrLib(file, true);
  }

  /**
   * Find the first Dart application or library file in the directory containing the specified file
   * or any parent directories which contains a reference to the specified file. If the specified
   * file is a Dart application or library file, then return that file.
   * 
   * @return the application or library file, or <code>null</code> if none found.
   */
  @Deprecated
  public static File getFirstAppOrLib(File file, boolean fileNeedsToExist) {
    return null;
  }

  /**
   * Answer the resource for the specified file. If necessary create a synthetic project and linked
   * folder. Any call to this method must be from within a {@link IWorkspaceRunnable}.
   * <p>
   * WARNING! This method removes any overlapping folders. Any resources originally contained within
   * an unlinked folder will be contained within the newly linked folder instead.
   */
  @Deprecated
  public static IFile getOrCreateResource(File file, IProgressMonitor monitor) throws CoreException {
    IResource resource = ResourceUtil.getResource(file);
    if (resource instanceof IFile) {
      return (IFile) resource;
    }

    IProject newProj = newProject(monitor);
    IFolder newFolder = newProj.getFolder("src");

    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    newFolder.createLink(new Path(file.getParentFile().getAbsolutePath()), 0, monitor);
    resource = ResourceUtil.getResource(file);
    if (!(resource instanceof IFile)) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          IStatus.ERROR,
          "Failed to create resource for " + file,
          null));
    }

    // Remove any overlapping linked folders

    IPath newFolderLoc = newFolder.getLocation();
    for (IProject proj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (!proj.getName().startsWith(DART_PROJECT_NAME_PREFIX) || proj.equals(newProj)) {
        continue;
      }
      for (IResource member : proj.members()) {
        if (member.getType() != IResource.FOLDER || !member.isLinked()) {
          continue;
        }
        if (newFolderLoc.isPrefixOf(member.getLocation())) {
          member.delete(false, monitor);
        }
      }
      // All "empty" projects have exactly one file... the ".project" file
      if (proj.members().length == 1) {
        proj.delete(false, monitor);
      }
    }

    return (IFile) resource;
  }

  /**
   * Create a new Dart project named "DartProject<#>" where <#> is a positive number. Any call to
   * this method must be from within a {@link IWorkspaceRunnable}.
   */
  private static IProject newProject(IProgressMonitor monitor) throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject proj;
    String projName;
    do {
      dartProjectCount++;
      projName = DART_PROJECT_NAME_PREFIX + dartProjectCount;
      proj = root.getProject(projName);
    } while (proj.exists());
    DartProjectGenerator projGen = new DartProjectGenerator();
    projGen.setName(projName);
    projGen.execute(monitor);
    return projGen.getProject();
  }
}

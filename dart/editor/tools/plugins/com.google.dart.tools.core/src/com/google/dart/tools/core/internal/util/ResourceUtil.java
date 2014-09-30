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
package com.google.dart.tools.core.internal.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import java.io.File;

/**
 * Utilities for dealing with resources.
 * 
 * @coverage dart.tools.core
 */
public class ResourceUtil {
  /**
   * The root of the workspace, cached for efficiency.
   */
  public static IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

  /**
   * Return the file resource associated with the given file, or <code>null</code> if the file does
   * not correspond to an existing file resource.
   * 
   * @param file the file representing the file resource to be returned
   * @return the file resource associated with the given file
   */
  public static IFile getFile(File file) {
    if (file == null) {
      return null;
    }
    if (!file.isFile()) {
      return null;
    }
    IResource resource = getResource(file);
    return (IFile) resource;
  }

  /**
   * Return the file resource associated with the given file path, or {@code null} if the file does
   * not correspond to an existing file resource.
   * 
   * @param path the path of the file representing the file resource to be returned
   * @return the {@link IFile} resource associated with the given file path
   */
  public static IFile getFile(String path) {
    if (path == null) {
      return null;
    }
    File file = new File(path);
    return getFile(file);
  }

  public static IPath getProjectLocation(IProject project) {
    if (project.getRawLocation() == null) {
      return project.getLocation();
    } else {
      return project.getRawLocation();
    }
  }

  /**
   * Answer the Eclipse resource associated with the specified file or <code>null</code> if none
   */
  public static IResource getResource(File file) {
    if (file == null) {
      return null;
    }
    if (file.isDirectory()) {
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IContainer[] containers = workspaceRoot.findContainersForLocationURI(file.toURI());
      if (containers.length == 0) {
        return null;
      }
      return containers[0];
    }
    if (file.isFile()) {
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IFile[] files = workspaceRoot.findFilesForLocationURI(file.toURI());
      if (files.length == 0) {
        return null;
      }
      return files[0];
    }
    return null;
  }

  public static boolean isExistingProject(IProject project) {
    if (!project.isOpen()) {
      return false;
    }
    IPath location = getProjectLocation(project);
    if (location == null) {
      return false;
    }
    File file = location.toFile();
    if (file == null) {
      return false;
    }
    return file.exists();
  }

  // No instances
  private ResourceUtil() {
  }
}

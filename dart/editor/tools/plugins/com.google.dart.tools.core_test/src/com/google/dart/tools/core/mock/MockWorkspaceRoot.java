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
package com.google.dart.tools.core.mock;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.net.URI;

public class MockWorkspaceRoot extends MockContainer implements IWorkspaceRoot {

  private final MockWorkspace workspace;

  public MockWorkspaceRoot() {
    this(new MockWorkspace());
  }

  public MockWorkspaceRoot(MockWorkspace workspace) {
    super(null, "/");
    this.workspace = workspace;
  }

  /**
   * Create a {@link MockProject} and add it to the receiver as a child
   */
  public MockProject addProject(String name) {
    return add(new MockProject(this, name));
  }

  @Override
  public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public IContainer[] findContainersForLocation(IPath location) {
    return null;
  }

  @Override
  public IContainer[] findContainersForLocationURI(URI location) {
    return null;
  }

  @Override
  public IContainer[] findContainersForLocationURI(URI location, int memberFlags) {
    return null;
  }

  @Override
  public IFile[] findFilesForLocation(IPath location) {
    return null;
  }

  @Override
  public IFile[] findFilesForLocationURI(URI location) {
    return null;
  }

  @Override
  public IFile[] findFilesForLocationURI(URI location, int memberFlags) {
    return null;
  }

  @Override
  public IContainer getContainerForLocation(IPath location) {
    if (location == null || location.segmentCount() == 0) {
      return null;
    }
    if (getLocation().isPrefixOf(location)) {
      IPath relPath = location.removeFirstSegments(getLocation().segmentCount());
      if (relPath.segmentCount() > 0) {
        IProject project = getProject(relPath.segment(0));
        if (relPath.segmentCount() > 1) {
          return project.getFolder(relPath.removeFirstSegments(1));
        }
        return project;
      }
    }
    return null;
  }

  @Override
  public IFile getFileForLocation(IPath location) {
    if (location == null || location.segmentCount() == 0) {
      return null;
    }
    if (getLocation().isPrefixOf(location)) {
      IPath relPath = location.removeFirstSegments(getLocation().segmentCount());
      if (relPath.segmentCount() > 0) {
        IProject project = getProject(relPath.segment(0));
        if (relPath.segmentCount() > 1) {
          return project.getFile(relPath.removeFirstSegments(1));
        }
        return null;
      }
    }
    return null;
  }

  @Override
  public IPath getLocation() {
    return ResourcesPlugin.getWorkspace().getRoot().getLocation();
  }

  @Override
  public IProject getProject(String name) {
    if (name == null || name.length() == 0) {
      return null;
    }
    MockResource existingChild = getExistingChild(name);
    if (existingChild instanceof MockProject) {
      return (MockProject) existingChild;
    }
    return new MockProject(this, name, false);
  }

  @Override
  public IProject[] getProjects() {
    IResource[] roots;
    try {
      roots = members();
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
    IProject[] result = new IProject[roots.length];
    System.arraycopy(roots, 0, result, 0, roots.length);
    return result;
  }

  @Override
  public IProject[] getProjects(int memberFlags) {
    return null;
  }

  @Override
  public int getType() {
    return IResource.ROOT;
  }

  @Override
  public IWorkspace getWorkspace() {
    return workspace;
  }
}

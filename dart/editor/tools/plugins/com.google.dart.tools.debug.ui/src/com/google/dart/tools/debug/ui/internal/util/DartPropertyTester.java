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
package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.tools.core.internal.model.DartProjectNature;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

/**
 * A {@link PropertyTester} for use in launch configuration declarations. It defines two properties:
 * <ul>
 * <li>"isDartProject" - whether the given project is a Dart project
 * <li>"isInDartProject" - whether the given resource is in a Dart project.
 */
public class DartPropertyTester extends PropertyTester {

  @SuppressWarnings("unchecked")
  private static <T> T getAdapter(Object adaptable, Class<? extends T> adapterType) {
    if (adapterType.isInstance(adaptable)) {
      return (T) adaptable;
    }

    IAdapterManager adapterManager = Platform.getAdapterManager();
    assert (adapterManager != null);
    return (T) adapterManager.getAdapter(adaptable, adapterType);
  }

  /**
   * Returns a resource for the given absolute or workspace-relative path.
   * <p>
   * If the path has a device (e.g. c:\ on Windows), it will be tried as an absolute path.
   * Otherwise, it is first tried as a workspace-relative path, and failing that an absolute path.
   * 
   * @param path the absolute or workspace-relative path to a resource
   * @return the resource, or null
   */
  private static IResource getResource(IPath path) {
    IResource res = null;
    if (path != null) {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      if (path.getDevice() == null) {
        // try searching relative to the workspace first
        res = root.findMember(path);
      }

      if (res == null) {
        // look for files
        IFile[] files = root.findFilesForLocation(path);
        // Check for accessibility since for directories, the above will return
        // a non-accessible IFile
        if (files.length > 0 && files[0].isAccessible()) {
          res = files[0];
        }
      }

      if (res == null) {
        // look for folders
        IContainer[] containers = root.findContainersForLocation(path);
        if (containers.length > 0) {
          res = containers[0];
        }
      }
    }
    return res;
  }

  /**
   * Resolves a linked resource to its target resource, or returns the given resource if it is not
   * linked or the target resource cannot be resolved.
   */
  private static IResource resolveTargetResource(IResource resource) {
    if (!resource.isLinked()) {
      return resource;
    }

    IResource resolvedResource = getResource(resource.getLocation());

    return resolvedResource != null ? resolvedResource : resource;
  }

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IResource resource = getAdapter(receiver, IResource.class);

    if (resource == null) {
      // Unexpected case; we were asked to test against something that's not a resource.
      return false;
    }

    // Resolve to the actual resource (if it is linked)
    resource = resolveTargetResource(resource);

    if (property.equals("isDartProject")) {
      return isDartProject(resource);
    } else if (property.equals("isInDartProject")) {
      return isInDartProject(resource);
    } else {
      return false;
    }
  }

  protected boolean isDartProject(IResource resource) {
    return DartProjectNature.hasDartNature(resource.getProject())
        && resource == resource.getProject();
  }

  protected boolean isInDartProject(IResource resource) {
    return DartProjectNature.hasDartNature(resource.getProject());
  }

}

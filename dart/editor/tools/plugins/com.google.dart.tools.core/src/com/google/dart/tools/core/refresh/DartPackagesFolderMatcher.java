/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse License v1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.core.refresh;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.io.FileUtilities;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.filtermatchers.AbstractFileInfoMatcher;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Filter out the children of symlinked 'packages' directories. This does not filter the main
 * 'packages' directory.
 */
public class DartPackagesFolderMatcher extends AbstractFileInfoMatcher {
  public static final String MATCHER_ID = "com.google.dart.tools.core.packagesFolderMatcher";

  public DartPackagesFolderMatcher() {

  }

  @Override
  public void initialize(IProject project, Object arguments) throws CoreException {

  }

  @Override
  public boolean matches(IContainer parent, IFileInfo fileInfo) throws CoreException {
    // Check that the folder's name is "packages".
    if (!parent.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
      return false;
    }

    // Don't filter out the top-level packages reference.
    if (parent.getParent() instanceof IProject) {
      return false;
    }

    // If it's a system symlink, filter it out.
    if (isSymLinked(parent)) {
      return true;
    }

    return false;
  }

  private boolean isSymLinked(IContainer parent) {
    IPath location = parent.getLocation();

    if (location != null) {
      try {
        return FileUtilities.isLinkedFile(location.toFile());
      } catch (CoreException e) {

      }
    }

    return false;
  }

}

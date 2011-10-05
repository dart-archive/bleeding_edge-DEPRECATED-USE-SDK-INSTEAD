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
package com.google.dart.tools.core.utilities.resource;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.File;

/**
 * The class <code>IProjectUtilities</code> defines utility methods used to work with projects.
 */
public final class IProjectUtilities {
  /**
   * Add to the given project a file that is linked to the given file.
   * 
   * @param project the project to which the linked file should be added
   * @param file the file that the linked file should be linked with
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @return the file that is linked to the given file
   * @throws CoreException if the link could not be created for some reason
   */
  public static IFile addLinkToProject(IProject project, File file, IProgressMonitor monitor)
      throws CoreException {
    IFile newFile = computeLinkPoint(project, file.getName());
    newFile.createLink(new Path(file.getAbsolutePath()), 0, monitor);
    if (!newFile.exists()) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, IStatus.ERROR,
          "Failed to create a link to " + file.getAbsolutePath() + " in " + project.getLocation(),
          null));
    }
    return newFile;
  }

  /**
   * Return a file in the given project whose name is derived from the given base name that does not
   * already exist.
   * 
   * @param project the project containing the file that is returned
   * @param baseName the base of the name of the returned file
   * @return a file that can be used to link to the given file
   */
  private static IFile computeLinkPoint(IProject project, String fileName) {
    IFile newFile = project.getFile(fileName);
    if (!newFile.exists()) {
      return newFile;
    }
    int dotIndex = fileName.lastIndexOf('.');
    String extension;
    String baseName;
    if (dotIndex < 0) {
      extension = "";
      baseName = fileName;
    } else {
      extension = fileName.substring(dotIndex);
      baseName = fileName.substring(0, dotIndex);
    }
    int index = 2;
    while (newFile.exists()) {
      newFile = project.getFile(baseName + index++ + extension);
    }
    return newFile;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private IProjectUtilities() {
    super();
  }
}

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
package com.google.dart.tools.search.ui.text;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.File;

/**
 * Identifies file types that should be filtered from dart search scopes.
 */
public class TextSearchScopeFilter {

  /**
   * A whitelist of file extensions that should be included in external file search.
   */
  private static final String[] SEARCHABLE_EXTERNAL_FILE_EXTENSIONS = {
      ".css", ".dart", ".html", ".js"};

  /**
   * Checks if the given file should be filtered out of a search scope.
   * 
   * @param file the file name to check
   * @return <code>true</code> if the file should be excluded, <code>false</code> otherwise
   */
  public static boolean isFiltered(File file) {
    return isPatchFile(file) || (file.isFile() && isExternaFileNameFiltered(file.getName()));
  }

  /**
   * Checks if the given workspace file should be filtered out of a search scope.
   * 
   * @param file the file to check
   * @return <code>true</code> if the file should be excluded, <code>false</code> otherwise
   */
  public static boolean isFiltered(IResourceProxy file) {
    return isWorkspaceFileNameFiltered(file.getName())
        || isSelfLinkedPackageResource(file.requestResource());
  }

  /**
   * Test for files that are in the packages directory and linked to another resource in the
   * workspace.
   */
  public static boolean isSelfLinkedPackageResource(IResource resource) {

    IPath relativePath = resource.getProjectRelativePath();

    if (DartCore.PACKAGES_DIRECTORY_NAME.equals(relativePath.segment(0))) {

      try {

        File canonicalFile = resource.getLocation().toFile().getCanonicalFile();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath loc = Path.fromOSString(canonicalFile.getAbsolutePath());
        IFile wsFile = workspace.getRoot().getFileForLocation(loc);

        if (wsFile != null && wsFile.exists()) {
          return true;
        }

      } catch (Exception e) {
        //ignore exceptions
      }
    }

    return false;
  }

  /**
   * Checks if the given external file should be filtered out of a search scope.
   * 
   * @param fileName the fileName to check
   * @return <code>true</code> if the file should be excluded, <code>false</code> otherwise
   */
  private static boolean isExternaFileNameFiltered(String fileName) {
    for (String ext : SEARCHABLE_EXTERNAL_FILE_EXTENSIONS) {
      if (fileName.endsWith(ext)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isPatchFile(File file) {
    return DartCore.isPatchfile(file);
  }

  /**
   * Checks if the given file name should be filtered out of a search scope.
   * 
   * @param fileName the file name to check
   * @return <code>true</code> if the file should be excluded, <code>false</code> otherwise
   */
  private static boolean isWorkspaceFileNameFiltered(String fileName) {
    //ignore .files (and avoid traversing into folders prefixed with a '.')
    if (fileName.startsWith(".")) {
      return true;
    }
    if (fileName.endsWith(".dart.js")) {
      return true;
    }

    return DartCore.isImageLikeFileName(fileName);
  }

}

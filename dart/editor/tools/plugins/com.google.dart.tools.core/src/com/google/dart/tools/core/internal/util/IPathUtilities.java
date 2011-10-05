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
package com.google.dart.tools.core.internal.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

/**
 * The class <code>IPathUtilities</code> defines utility methods for working with instances of the
 * class {@link IPath}.
 */
public class IPathUtilities {
  public static Object getTarget(IPath path, boolean checkResourceExistence) {
    // Implicitly checks resource existence.
    Object target = getWorkspaceTarget(path);
    if (target != null) {
      return target;
    }
    // return getExternalTarget(path, checkResourceExistence);
    return null;
  }

  public static IResource getWorkspaceTarget(IPath path) {
    if (path == null || path.getDevice() != null) {
      return null;
    }
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    if (workspace == null) {
      return null;
    }
    return workspace.getRoot().findMember(path);
  }
  // public static Object getExternalTarget(IPath path, boolean
  // checkResourceExistence) {
  // if (path == null) {
  // return null;
  // }
  // ExternalFoldersManager externalFoldersManager =
  // DartModelManager.getExternalManager();
  // Object linkedFolder = externalFoldersManager.getFolder(path);
  // if (linkedFolder != null) {
  // if (checkResourceExistence) {
  // // check if external folder is present
  // File externalFile = new File(path.toOSString());
  // if (!externalFile.isDirectory()) {
  // return null;
  // }
  // }
  // return linkedFolder;
  // }
  // File externalFile = new File(path.toOSString());
  // if (!checkResourceExistence) {
  // return externalFile;
  // } else if (existingExternalFilesContains(externalFile)) {
  // return externalFile;
  // } else {
  // if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
  //        System.out.println("(" + Thread.currentThread() + ") [JavaModel.getTarget(...)] Checking existence of " + path.toString()); //$NON-NLS-1$ //$NON-NLS-2$
  // }
  // if (externalFile.isFile()) { // isFile() checks for existence (it returns
  // false if a directory)
  // // cache external file
  // existingExternalFilesAdd(externalFile);
  // return externalFile;
  // }
  // }
  // return null;
  // }
}

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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.Util;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;

/**
 * Static builder utility methods shared by multiple classes
 */
class BuilderUtil {
  /**
   * Clear all error markers from the specified file
   * 
   * @param res the resource (not <code>null</code>)
   */
  static void clearErrorMarkers(IResource res) {
    if (res == null || !res.isAccessible()) {
      return;
    }
    try {
      res.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      Util.log(e, "Failed to clear markers for " + res);
    }
  }

  /**
   * Create an error marker for the specified file
   * 
   * @param res the resource (not <code>null</code>)
   * @param offset the character offset into the file where the error occurred
   * @param errMsg the error message (not <code>null</code>)
   */
  static void createErrorMarker(IResource res, int offset, int length, int lineNumber, String errMsg) {
    createMarker(res, IMarker.SEVERITY_ERROR, offset, length, lineNumber, errMsg);
  }

  /**
   * Create an error marker for the specified file
   * 
   * @param res the resource (not <code>null</code>)
   * @param offset the character offset into the file where the error occurred
   * @param warnMsg the warning message (not <code>null</code>)
   */
  static void createWarningMarker(IResource res, int offset, int length, int lineNumber,
      String warnMsg) {
    createMarker(res, IMarker.SEVERITY_WARNING, offset, length, lineNumber, warnMsg);
  }

  /**
   * Recursively delete the specified file or directory and all files/directories directly or
   * indirectly contained therein.
   * 
   * @return <code>true</code> if successfully deleted (or the file is null), or <code>false</code>
   *         if some files/directories could not be deleted
   */
  static boolean deleteAll(File file) {
    if (file == null || !file.exists()) {
      return true;
    }
    String[] childNames = file.list();
    if (childNames != null) {
      boolean deleted = true;
      for (String name : childNames) {
        if (!deleteAll(new File(file, name))) {
          deleted = false;
        }
      }
      if (!deleted) {
        return false;
      }
    }
    return file.delete();
  }

  /**
   * Perform any workspace modifications by wrapping them in a {@link IWorkspaceRunnable} and
   * calling this method.
   * 
   * @param runnable the runnable containing the workspace modification code
   * @param monitor the progress monitor (not <code>null</code>)
   */
  static void run(IWorkspaceRunnable runnable, IProgressMonitor monitor) throws CoreException {
    ResourcesPlugin.getWorkspace().run(runnable, monitor);
  }

  /**
   * Create an error marker for the specified file
   * 
   * @param res the resource (not <code>null</code>)
   * @param offset the character offset into the file where the error occurred
   * @param errMsg the error message (not <code>null</code>)
   */
  private static void createMarker(IResource res, int severity, int offset, int length,
      int lineNumber, String errMsg) {
    if (res == null || !res.exists()) {
      return;
    }

    // Remove newlines and indent spaces from the compiler's error messages.
    if (errMsg.indexOf('\n') != -1) {
      errMsg = errMsg.replace('\n', ' ');
      errMsg = errMsg.replaceAll(" +", " ");
    }

    try {
      IMarker marker = res.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
      marker.setAttribute(IMarker.SEVERITY, severity);
      marker.setAttribute(IMarker.MESSAGE, errMsg);
      marker.setAttribute(IMarker.CHAR_START, offset);
      marker.setAttribute(IMarker.CHAR_END, offset + length);
      marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
    } catch (CoreException e) {
      Util.log(e, "Failed to create marker for " + res + "\n   at " + offset + " message: "
          + errMsg);
    }
  }
}

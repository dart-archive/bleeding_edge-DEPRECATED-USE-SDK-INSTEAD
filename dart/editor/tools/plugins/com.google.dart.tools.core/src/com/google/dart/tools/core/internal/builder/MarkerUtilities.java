/*
 * Copyright 2012 Dart project authors.
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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * A utility class to manipulate markers.
 */
public class MarkerUtilities {
  public static final String ISSUE_MARKER = DartCore.PLUGIN_ID + ".issue";

  public static void createErrorMarker(IFile file, String message, int line) throws CoreException {
    IMarker marker = file.createMarker(ISSUE_MARKER);
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    marker.setAttribute(IMarker.MESSAGE, message);
    marker.setAttribute(IMarker.LINE_NUMBER, line);
  }

  public static void createErrorMarker(IFile file, String message, int line, int charStart,
      int charEnd) throws CoreException {
    IMarker marker = file.createMarker(ISSUE_MARKER);
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    marker.setAttribute(IMarker.MESSAGE, message);
    marker.setAttribute(IMarker.LINE_NUMBER, line);
    if (charStart != -1) {
      marker.setAttribute(IMarker.CHAR_START, charStart);
    }
    if (charEnd != -1) {
      marker.setAttribute(IMarker.CHAR_END, charEnd);
    }
  }

  public static void createWarningMarker(IFile file, String message, int line) throws CoreException {
    IMarker marker = file.createMarker(ISSUE_MARKER);
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    marker.setAttribute(IMarker.MESSAGE, message);
    marker.setAttribute(IMarker.LINE_NUMBER, line);
  }

  public static void createWarningMarker(IFile file, String message, int line, int charStart,
      int charEnd) throws CoreException {
    IMarker marker = file.createMarker(ISSUE_MARKER);
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    marker.setAttribute(IMarker.MESSAGE, message);
    marker.setAttribute(IMarker.LINE_NUMBER, line);
    if (charStart != -1) {
      marker.setAttribute(IMarker.CHAR_START, charStart);
    }
    if (charEnd != -1) {
      marker.setAttribute(IMarker.CHAR_END, charEnd);
    }
  }

  public static void deleteMarkers(IContainer container) throws CoreException {
    container.deleteMarkers(ISSUE_MARKER, true, IResource.DEPTH_INFINITE);
  }

  public static void deleteMarkers(IFile file) throws CoreException {
    file.deleteMarkers(ISSUE_MARKER, true, IResource.DEPTH_ZERO);
  }

  private MarkerUtilities() {

  }

}

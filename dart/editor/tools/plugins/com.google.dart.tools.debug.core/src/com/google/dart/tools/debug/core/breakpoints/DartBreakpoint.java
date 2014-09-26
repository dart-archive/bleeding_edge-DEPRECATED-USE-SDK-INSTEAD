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
package com.google.dart.tools.debug.core.breakpoints;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * The implementation of a Dart line breakpoint.
 * 
 * @see ILineBreakpoint
 */
public class DartBreakpoint extends LineBreakpoint {

  private static final String FILE_PATH = "fileUri";

  public static IMarker createBreakpointMarker(IResource file, int line, String filePath)
      throws CoreException {
    IMarker marker = file.createMarker(DartDebugCorePlugin.DEBUG_MARKER_ID);

    marker.setAttribute(IMarker.LINE_NUMBER, line);
    marker.setAttribute(IBreakpoint.ID, DartDebugCorePlugin.DEBUG_MODEL_ID);
    marker.setAttribute(
        IMarker.MESSAGE,
        NLS.bind("Line Breakpoint: {0} [line: {1}]", file.getName(), line));
    marker.setAttribute(ENABLED, true);
    if (filePath != null) {
      marker.setAttribute(FILE_PATH, filePath);
    }

    return marker;
  }

  /**
   * A default constructor is required for the breakpoint manager to re-create persisted
   * breakpoints. After instantiating a breakpoint, the setMarker method is called to restore this
   * breakpoint's attributes.
   */
  public DartBreakpoint() {

  }

  /**
   * Create a new DartBreakpoint.
   * 
   * @param resource
   * @param line
   * @throws CoreException
   */
  public DartBreakpoint(final IResource resource, final int line) throws CoreException {
    this(resource, line, null);
  }

  public DartBreakpoint(final IResource resource, final int line, final String fileUri)
      throws CoreException {

    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        IMarker marker = createBreakpointMarker(resource, line, fileUri);

        setMarker(marker);
      }
    };

    run(getMarkerRule(resource), runnable);
  }

  public String getCharset() {
    IResource resource = getFile();
    if (resource != null && resource instanceof IFile) {
      try {
        return ((IFile) resource).getCharset();
      } catch (CoreException e) {

      }
    }
    return "UTF-8";
  }

  public InputStream getContents() {

    IResource resource = getFile();
    if (resource != null && resource instanceof IFile) {
      try {
        return ((IFile) resource).getContents();
      } catch (CoreException e) {

      }
    }

    String fileUri = getMarker().getAttribute(FILE_PATH, "");
    if (!fileUri.isEmpty()) {
      try {
        return new FileInputStream(new File(fileUri));
      } catch (FileNotFoundException e) {
        DartCore.logError(e);
      }

    }
    return null;
  }

  public IFile getFile() {
    if (getMarker().getResource() instanceof IFile) {
      return (IFile) getMarker().getResource();
    }
    return null;
  }

  public String getFilePath() {
    try {
      return (String) getMarker().getAttribute(FILE_PATH);
    } catch (CoreException e) {
      return null;
    }
  }

  public String getActualFilePath() {
    IFile file = getFile();
    if (file != null) {
      return file.getLocation().toOSString();
    }
    return getFilePath();
  }

  public int getLine() {
    IMarker marker = getMarker();

    if (marker != null) {
      return marker.getAttribute(IMarker.LINE_NUMBER, -1);
    }

    return -1;
  }

  @Override
  public String getModelIdentifier() {
    return DartDebugCorePlugin.DEBUG_MODEL_ID;
  }

  public String getName() {
    if (getMarker().getResource() instanceof IFile) {
      return ((IFile) getMarker().getResource()).getName();
    }

    try {
      Path path = new Path((String) getMarker().getAttribute(FILE_PATH));
      return path.lastSegment();

    } catch (CoreException e) {
      DartCore.logError(e);
    }
    return null;
  }

  public boolean isBreakpointEnabled() {
    IMarker marker = getMarker();

    if (marker != null) {
      return marker.getAttribute(ENABLED, false);
    }

    return false;
  }

  @Override
  public String toString() {
    return getName() + ":" + getLine();
  }

  public void updateLineNumber(int newLine) {
    try {
      getMarker().setAttribute(IMarker.LINE_NUMBER, newLine);
    } catch (CoreException e) {
      // We make a best effort to update the breakpoint's line.

    }
  }

}

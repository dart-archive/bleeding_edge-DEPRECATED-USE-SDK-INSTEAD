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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SubSystem;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.File;

/**
 * Updates problem markers based upon information from the {@link AnalysisServer}.
 */
public class AnalysisMarkerManager implements AnalysisListener {

  private final AnalysisServer server;

  public AnalysisMarkerManager(AnalysisServer server) {
    this.server = server;
  }

  @Override
  public void idle(boolean idle) {
    // ignored
  }

  /**
   * Remove all existing problem markers for the specified files and create new problem markers for
   * any parse errors that were found.
   */
  @Override
  public void parsed(AnalysisEvent event) {
    for (File file : event.getFiles()) {
      deleteMarkers(file);
    }
    for (DartCompilationError error : event.getErrors()) {
      createMarker(error);
    }
  }

  /**
   * The {@link #parsed(AnalysisEvent)} notification has already removed any existing resolution
   * problem markers so just create new problem markers for any resolution errors that were found.
   */
  @Override
  public void resolved(AnalysisEvent event) {
    for (DartCompilationError error : event.getErrors()) {
      createMarker(error);
    }
  }

  /**
   * Create an error marker for the specified file
   */
  private void createMarker(DartCompilationError error) {
    if (error == null) {
      return;
    }
    Source source = error.getSource();
    if (source == null) {
      return;
    }
    IResource res = ResourceUtil.getResource(toFile(server, source.getUri()));
    if (res == null || !res.exists() || !DartCore.isAnalyzed(res)) {
      return;
    }

    int severity;
    if (error.getErrorCode().getSubSystem() == SubSystem.STATIC_TYPE) {
      severity = IMarker.SEVERITY_WARNING;
    } else if (error.getErrorCode().getErrorSeverity() == ErrorSeverity.ERROR) {
      severity = IMarker.SEVERITY_ERROR;
    } else if (error.getErrorCode().getErrorSeverity() == ErrorSeverity.WARNING) {
      severity = IMarker.SEVERITY_WARNING;
    } else {
      return;
    }

    int offset = error.getStartPosition();
    int length = error.getLength();
    int lineNumber = error.getLineNumber();
    String errMsg = error.getMessage();

    // Remove newlines and indent spaces from the compiler's error messages

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
      DartCore.logError("Failed to create marker for " + res + "\n   at " + offset + " message: "
          + errMsg, e);
    }
  }

  /**
   * Clear all error markers from the specified file a
   */
  private void deleteMarkers(File file) {
    IResource res = ResourceUtil.getResource(file);
    if (res == null || !res.isAccessible()) {
      return;
    }
    try {
      res.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      DartCore.logError("Failed to clear markers for " + res, e);
    }
  }
}

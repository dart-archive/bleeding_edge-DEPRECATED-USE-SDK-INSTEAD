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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ErrorCode;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.mock.ui.CorrectionEngine;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import java.util.Iterator;

public class DartMarkerAnnotation extends MarkerAnnotation implements IJavaAnnotation {

  public static final String JAVA_MARKER_TYPE_PREFIX = "org.eclipse.wst.jsdt"; //$NON-NLS-1$
  public static final String ERROR_ANNOTATION_TYPE = "com.google.dart.tools.ui.error"; //$NON-NLS-1$
  public static final String WARNING_ANNOTATION_TYPE = "com.google.dart.tools.ui.warning"; //$NON-NLS-1$
  public static final String INFO_ANNOTATION_TYPE = "com.google.dart.tools.ui.info"; //$NON-NLS-1$
  public static final String TASK_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.task"; //$NON-NLS-1$

  /**
   * @return <code>true</code> if the marker can be treated as a Dart annotation.
   */
  static final boolean isJavaAnnotation(IMarker marker) {
    // Performance
    String markerType = MarkerUtilities.getMarkerType(marker);
    if (DartCore.DART_PROBLEM_MARKER_TYPE.equals(markerType)) {
      return true;
    }
    // Generic
    return MarkerUtilities.isMarkerType(marker, DartCore.DART_PROBLEM_MARKER_TYPE);
  }

  private IJavaAnnotation fOverlay;

  public DartMarkerAnnotation(IMarker marker) {
    super(marker);
  }

  @Override
  public void addOverlaid(IJavaAnnotation annotation) {
    // not supported
  }

  @Override
  public String[] getArguments() {
    IMarker marker = getMarker();
    if (marker != null && marker.exists() && isProblem()) {
      return CorrectionEngine.getProblemArguments(marker);
    }
    return null;
  }

  @Override
  public ErrorCode getId() {
    IMarker marker = getMarker();
    if (marker == null || !marker.exists()) {
      return null;
    }

    if (isProblem()) {
      String qualifiedName = marker.getAttribute("errorCode", (String) null);
      if (qualifiedName != null) {
        return ErrorCode.Helper.forQualifiedName(qualifiedName);
      }
      return null;
    }

//		if (TASK_ANNOTATION_TYPE.equals(getAnnotationType())) {
//			try {
//				if (marker.isSubtypeOf(IJavaScriptModelMarker.TASK_MARKER)) {
//					return Problem.Task;
//				}
//			} catch (CoreException e) {
//				DartToolsPlugin.log(e); // should no happen, we test for marker.exists
//			}
//		}

    return null;
  }

  @Override
  public String getMarkerType() {
    IMarker marker = getMarker();
    if (marker == null || !marker.exists()) {
      return null;
    }

    return MarkerUtilities.getMarkerType(getMarker());
  }

  @Override
  public Iterator<IJavaAnnotation> getOverlaidIterator() {
    // not supported
    return null;
  }

  @Override
  public IJavaAnnotation getOverlay() {
    return fOverlay;
  }

  @Override
  public boolean hasOverlay() {
    return fOverlay != null;
  }

  @Override
  public boolean isProblem() {
    String type = getType();
    return INFO_ANNOTATION_TYPE.equals(type) || WARNING_ANNOTATION_TYPE.equals(type)
        || ERROR_ANNOTATION_TYPE.equals(type);
  }

  @Override
  public void removeOverlaid(IJavaAnnotation annotation) {
    // not supported
  }

  /**
   * Overlays this annotation with the given javaAnnotation.
   * 
   * @param javaAnnotation annotation that is overlaid by this annotation
   */
  public void setOverlay(IJavaAnnotation javaAnnotation) {
    if (fOverlay != null) {
      fOverlay.removeOverlaid(this);
    }

    fOverlay = javaAnnotation;
    if (!isMarkedDeleted()) {
      markDeleted(fOverlay != null);
    }

    if (fOverlay != null) {
      fOverlay.addOverlaid(this);
    }
  }
}

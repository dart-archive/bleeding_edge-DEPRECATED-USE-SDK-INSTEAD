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
package com.google.dart.tools.ui.internal.text.correction;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.problem.CategorizedProblem;
import com.google.dart.tools.core.problem.Problem;
import com.google.dart.tools.ui.internal.text.editor.DartMarkerAnnotation;
import com.google.dart.tools.ui.internal.text.editor.IJavaAnnotation;
import com.google.dart.tools.ui.text.dart.IProblemLocation;

/**
 * @coverage dart.editor.ui.correction
 */
public class ProblemLocation implements IProblemLocation {

  private final int fId;
  private final String[] fArguments;
  private final int fOffset;
  private final int fLength;
  private final boolean fIsError;
  private final String fMarkerType;

  public ProblemLocation(int offset, int length, IJavaAnnotation annotation) {
    fId = annotation.getId();
    String[] arguments = annotation.getArguments();
    fArguments = arguments != null ? arguments : new String[0];
    fOffset = offset;
    fLength = length;
    fIsError = DartMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(annotation.getType());

    String markerType = annotation.getMarkerType();
    fMarkerType = markerType != null ? markerType : DartCore.DART_PROBLEM_MARKER_TYPE;
  }

  public ProblemLocation(int offset, int length, int id, String[] arguments, boolean isError,
      String markerType) {
    fId = id;
    fArguments = arguments;
    fOffset = offset;
    fLength = length;
    fIsError = isError;
    fMarkerType = markerType;
  }

  public ProblemLocation(Problem problem) {
    fId = problem.getID();
    fArguments = problem.getArguments();
    fOffset = problem.getSourceStart();
    fLength = problem.getSourceEnd() - fOffset + 1;
    fIsError = problem.isError();
    fMarkerType = problem instanceof CategorizedProblem
        ? ((CategorizedProblem) problem).getMarkerType()
        : DartCore.DART_PROBLEM_MARKER_TYPE;
  }

  @Override
  public DartNode getCoveredNode(DartUnit astRoot) {
    NodeFinder finder = NodeFinder.find(astRoot, fOffset, fLength);
    return finder.getCoveredNode();
  }

  @Override
  public DartNode getCoveringNode(DartUnit astRoot) {
    NodeFinder finder = NodeFinder.find(astRoot, fOffset, fLength);
    return finder.getCoveringNode();
  }

  @Override
  public int getLength() {
    return fLength;
  }

  @Override
  public String getMarkerType() {
    return fMarkerType;
  }

  @Override
  public int getOffset() {
    return fOffset;
  }

  @Override
  public String[] getProblemArguments() {
    return fArguments;
  }

  @Override
  public int getProblemId() {
    return fId;
  }

  @Override
  public boolean isError() {
    return fIsError;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Id: ").append(getErrorCode(fId)).append('\n'); //$NON-NLS-1$
    buf.append('[').append(fOffset).append(", ").append(fLength).append(']').append('\n'); //$NON-NLS-1$
    String[] arg = fArguments;
    for (int i = 0; i < arg.length; i++) {
      buf.append(arg[i]);
      buf.append('\n');
    }
    return buf.toString();
  }

  private String getErrorCode(int code) {
    // TODO(scheglov) may be restore
    return "" + code;
//    StringBuffer buf = new StringBuffer();
//
//    if ((code & Problem.TypeRelated) != 0) {
//      buf.append("TypeRelated + "); //$NON-NLS-1$
//    }
//    if ((code & IProblem.FieldRelated) != 0) {
//      buf.append("FieldRelated + "); //$NON-NLS-1$
//    }
//    if ((code & IProblem.ConstructorRelated) != 0) {
//      buf.append("ConstructorRelated + "); //$NON-NLS-1$
//    }
//    if ((code & IProblem.MethodRelated) != 0) {
//      buf.append("MethodRelated + "); //$NON-NLS-1$
//    }
//    if ((code & IProblem.ImportRelated) != 0) {
//      buf.append("ImportRelated + "); //$NON-NLS-1$
//    }
//    if ((code & IProblem.Internal) != 0) {
//      buf.append("Internal + "); //$NON-NLS-1$
//    }
//    if ((code & IProblem.Syntax) != 0) {
//      buf.append("Syntax + "); //$NON-NLS-1$
//    }
//    if ((code & IProblem.Javadoc) != 0) {
//      buf.append("Javadoc + "); //$NON-NLS-1$
//    }
//    buf.append(code & IProblem.IgnoreCategoriesMask);
//
//    return buf.toString();
  }

}

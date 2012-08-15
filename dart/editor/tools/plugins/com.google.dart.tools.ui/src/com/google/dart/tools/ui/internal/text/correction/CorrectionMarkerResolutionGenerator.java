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

import com.google.common.collect.Lists;
import com.google.dart.compiler.ErrorCode;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.CorrectionEngine;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.text.editor.DartMarkerAnnotation;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.text.editor.ICompilationUnitDocumentProvider;
import com.google.dart.tools.ui.text.dart.CompletionProposalComparator;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IInvocationContext;
import com.google.dart.tools.ui.text.dart.IProblemLocation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

  private static class CorrectionMarkerResolution implements IMarkerResolution {
    private final CompilationUnit unit;
    private final IDartCompletionProposal proposal;
    private final int offset;
    private final int length;

    public CorrectionMarkerResolution(CompilationUnit unit, int offset, int length,
        IDartCompletionProposal proposal) {
      this.unit = unit;
      this.offset = offset;
      this.length = length;
      this.proposal = proposal;
    }

    @Override
    public String getLabel() {
      return proposal.getDisplayString();
    }

    @Override
    public void run(IMarker marker) {
      try {
        IEditorPart part = EditorUtility.isOpenInEditor(unit);
        if (part == null) {
          part = DartUI.openInEditor(unit, true, false);
          if (part instanceof ITextEditor) {
            ((ITextEditor) part).selectAndReveal(offset, length);
          }
        }
        if (part != null) {
          IEditorInput input = part.getEditorInput();
          IDocument doc = getDocumentProvider().getDocument(input);
          proposal.apply(doc);
        }
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

  private static final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

  private static IProblemLocation createFromMarker(IMarker marker, CompilationUnit cu) {
    try {
      ErrorCode id = new DartMarkerAnnotation(marker).getId();
      int start = marker.getAttribute(IMarker.CHAR_START, -1);
      int end = marker.getAttribute(IMarker.CHAR_END, -1);
      int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
      String[] arguments = CorrectionEngine.getProblemArguments(marker);
      String markerType = marker.getType();
      if (cu != null && id != null && start != -1 && end != -1 && arguments != null) {
        boolean isError = (severity == IMarker.SEVERITY_ERROR);
        return new ProblemLocation(start, end - start, id, arguments, isError, markerType);
      }
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  private static IProblemLocation findProblemLocation(IEditorInput input, IMarker marker) {
    IAnnotationModel model = getDocumentProvider().getAnnotationModel(input);
    if (model != null) { // open in editor
      @SuppressWarnings("unchecked")
      Iterator<Annotation> iter = model.getAnnotationIterator();
      while (iter.hasNext()) {
        Annotation curr = iter.next();
        if (curr instanceof DartMarkerAnnotation) {
          DartMarkerAnnotation annot = (DartMarkerAnnotation) curr;
          if (marker.equals(annot.getMarker())) {
            Position pos = model.getPosition(annot);
            if (pos != null) {
              return new ProblemLocation(pos.getOffset(), pos.getLength(), annot);
            }
          }
        }
      }
    } else { // not open in editor
      CompilationUnit cu = getCompilationUnit(marker);
      return createFromMarker(marker, cu);
    }
    return null;
  }

  private static CompilationUnit getCompilationUnit(IMarker marker) {
    IResource res = marker.getResource();
    if (res instanceof IFile && res.isAccessible()) {
      DartElement element = DartCore.create((IFile) res);
      if (element instanceof CompilationUnit) {
        return (CompilationUnit) element;
      }
    }
    return null;
  }

  private static ICompilationUnitDocumentProvider getDocumentProvider() {
    return DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider();
  }

  private static IMarkerResolution[] internalGetResolutions(IMarker marker) {
    if (!internalHasResolutions(marker)) {
      return NO_RESOLUTIONS;
    }

    CompilationUnit cu = getCompilationUnit(marker);
    if (cu != null) {
      IEditorInput input = EditorUtility.getEditorInput(cu);
      if (input != null) {
        IProblemLocation location = findProblemLocation(input, marker);
        if (location != null) {

          IInvocationContext context = new AssistContext(
              cu,
              location.getOffset(),
              location.getLength());

          // TODO(scheglov) do we need this?
//          if (!hasProblem(context.getASTRoot().getProblems(), location)) {
//            return NO_RESOLUTIONS;
//          }

          List<IDartCompletionProposal> proposals = Lists.newArrayList();
          DartCorrectionProcessor.collectCorrections(
              context,
              new IProblemLocation[] {location},
              proposals);
          Collections.sort(proposals, new CompletionProposalComparator());

          int nProposals = proposals.size();
          IMarkerResolution[] resolutions = new IMarkerResolution[nProposals];
          for (int i = 0; i < nProposals; i++) {
            resolutions[i] = new CorrectionMarkerResolution(
                cu,
                location.getOffset(),
                location.getLength(),
                proposals.get(i));
          }
          return resolutions;
        }
      }
    }
    return NO_RESOLUTIONS;
  }

  private static boolean internalHasResolutions(IMarker marker) {
    ErrorCode id = new DartMarkerAnnotation(marker).getId();
    CompilationUnit cu = getCompilationUnit(marker);
    return cu != null
        && DartCorrectionProcessor.hasCorrections(cu, id, MarkerUtilities.getMarkerType(marker));
  }

  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    return internalGetResolutions(marker);
  }

  @Override
  public boolean hasResolutions(IMarker marker) {
    return internalHasResolutions(marker);
  }

}

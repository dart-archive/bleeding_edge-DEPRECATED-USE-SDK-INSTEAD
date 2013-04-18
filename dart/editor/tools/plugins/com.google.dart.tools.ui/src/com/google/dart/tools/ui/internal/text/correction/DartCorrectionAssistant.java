/*
 * Copyright (c) 2013, the Dart project authors.
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
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.services.correction.ProblemLocation;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.Iterator;
import java.util.List;

/**
 * @coverage dart.editor.ui.correction
 */
public class DartCorrectionAssistant extends QuickAssistAssistant {
  /**
   * @return the {@link ProblemLocation} created from the given {@link Annotation}. May be
   *         {@code null}.
   */
  static ProblemLocation createProblemLocation(Annotation annotation) {
    // prepare marker
    if (!(annotation instanceof MarkerAnnotation)) {
      return null;
    }
    IMarker marker = ((MarkerAnnotation) annotation).getMarker();
    if (marker == null) {
      return null;
    }
    // prepare ErrorCode
    ErrorCode errorCode = DartCore.getErrorCode(marker);
    if (errorCode == null) {
      return null;
    }
    // prepare message
    String message = marker.getAttribute(IMarker.MESSAGE, (String) null);
    if (message == null) {
      return null;
    }
    // prepare 'offset'
    int offset;
    {
      offset = marker.getAttribute(IMarker.CHAR_START, -1);
      if (offset == -1) {
        return null;
      }
    }
    // prepare 'length'
    int length;
    {
      int end = marker.getAttribute(IMarker.CHAR_END, -1);
      if (end == -1) {
        return null;
      }
      length = end - offset;
    }
    // OK
    return new ProblemLocation(errorCode, offset, length, message);
  }

  private static IRegion getRegionOfInterest(ITextEditor editor, int invocationLocation)
      throws BadLocationException {
    IDocumentProvider documentProvider = editor.getDocumentProvider();
    if (documentProvider == null) {
      return null;
    }
    IDocument document = documentProvider.getDocument(editor.getEditorInput());
    if (document == null) {
      return null;
    }
    return document.getLineInformationOfOffset(invocationLocation);
  }

  private static boolean isInside(int offset, int start, int end) {
    // make sure to handle 0-length ranges
    return offset == start || offset == end || offset > start && offset < end;
  }

  private final DartEditor editor;
  private ITextViewer viewer;
  /**
   * The {@link ProblemLocation} to propose fixes for.
   */
  private ProblemLocation problemLocationToFix;

  public DartCorrectionAssistant(ITextEditor editor) {
    Assert.isNotNull(editor);
    if (editor instanceof DartEditor) {
      this.editor = (DartEditor) editor;
      DartCorrectionProcessor processor = new DartCorrectionProcessor(this);
      setQuickAssistProcessor(processor);
    } else {
      this.editor = null;
    }
  }

  /**
   * @return the underlying {@link DartEditor}.
   */
  public DartEditor getEditor() {
    return editor;
  }

  /**
   * @return the {@link ProblemLocation} to compute fixes for.
   */
  public ProblemLocation getProblemLocationToFix() {
    return problemLocationToFix;
  }

  @Override
  public void install(ISourceViewer sourceViewer) {
    super.install(sourceViewer);
    this.viewer = sourceViewer;
  }

  /**
   * Show completions at caret position. If current position does not contain quick fixes look for
   * next quick fix on same line by moving from left to right and restarting at end of line if the
   * beginning of the line is reached.
   * 
   * @see IQuickAssistAssistant#showPossibleQuickAssists()
   */
  @Override
  public String showPossibleQuickAssists() {
    prepareProblemsAtCaretLocation();
    return super.showPossibleQuickAssists();
  }

  /**
   * Fills {@link #problemLocationToFix}.
   */
  private void prepareProblemsAtCaretLocation() {
    problemLocationToFix = null;
    try {
      Point selectedRange = viewer.getSelectedRange();
      int currOffset = selectedRange.x;
      // prepare IAnnotationModel
      IAnnotationModel model;
      {
        IEditorInput editorInput = editor.getEditorInput();
        model = DartUI.getDocumentProvider().getAnnotationModel(editorInput);
        if (model == null) {
          return;
        }
      }
      // prepare current line range
      IRegion lineInfo = getRegionOfInterest(editor, currOffset);
      if (lineInfo == null) {
        return;
      }
      int rangeStart = lineInfo.getOffset();
      int rangeEnd = rangeStart + lineInfo.getLength();

      List<ProblemLocation> allProblemLocations = Lists.newArrayList();
      List<Position> allPositions = Lists.newArrayList();
      @SuppressWarnings("unchecked")
      Iterator<Annotation> iter = model.getAnnotationIterator();
      while (iter.hasNext()) {
        Annotation annotation = iter.next();
        // prepare Annotation position
        Position pos = model.getPosition(annotation);
        if (pos == null) {
          continue;
        }
        // check that Annotation is on the current line
        if (!isInside(pos.offset, rangeStart, rangeEnd)) {
          continue;
        }
        // 
        ProblemLocation problemLocation = createProblemLocation(annotation);
        if (problemLocation == null) {
          continue;
        }
        if (QuickFixProcessor.hasFix(problemLocation)) {
          allProblemLocations.add(problemLocation);
          allPositions.add(pos);
        }
      }
      problemLocationToFix = null;
      // problem under caret
      for (int i = 0; i < allPositions.size(); i++) {
        Position pos = allPositions.get(i);
        if (pos.includes(currOffset)) {
          problemLocationToFix = allProblemLocations.get(i);
          break;
        }
      }
      // problem after caret
      if (problemLocationToFix == null) {
        int bestOffset = Integer.MAX_VALUE;
        for (int i = 0; i < allPositions.size(); i++) {
          Position pos = allPositions.get(i);
          if (pos.offset > currOffset) {
            int offset = pos.offset - currOffset;
            if (offset < bestOffset) {
              bestOffset = offset;
              problemLocationToFix = allProblemLocations.get(i);
            }
          }
        }
      }
      // problem before caret
      if (problemLocationToFix == null) {
        int bestOffset = Integer.MAX_VALUE;
        for (int i = 0; i < allPositions.size(); i++) {
          Position pos = allPositions.get(i);
          if (pos.offset < currOffset) {
            int offset = currOffset - pos.offset;
            if (offset < bestOffset) {
              bestOffset = offset;
              problemLocationToFix = allProblemLocations.get(i);
            }
          }
        }
      }
      // not found
      if (problemLocationToFix == null) {
        return;
      }
      // show problem
      {
        int offset = problemLocationToFix.getOffset();
        viewer.setSelectedRange(offset, 0);
        viewer.revealRange(offset, 0);
      }
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }
}

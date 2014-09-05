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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.RunnableObject;
import com.google.dart.tools.ui.internal.util.TimeboxUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @coverage dart.editor.ui.correction
 */
public class DartCorrectionAssistant_OLD extends QuickAssistAssistant {
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
   * The {@link AnalysisError} to propose fixes for.
   */
  private AnalysisError problemToFix;

  public DartCorrectionAssistant_OLD(ITextEditor editor) {
    Assert.isNotNull(editor);
    if (editor instanceof DartEditor) {
      this.editor = (DartEditor) editor;
      DartCorrectionProcessor_OLD processor = new DartCorrectionProcessor_OLD(this);
      setQuickAssistProcessor(processor);
    } else {
      this.editor = null;
    }
    setInformationControlCreator(getInformationControlCreator());
  }

  /**
   * @return the underlying {@link DartEditor}.
   */
  public DartEditor getEditor() {
    return editor;
  }

  /**
   * @return the {@link AnalysisError} to compute fixes for.
   */
  public AnalysisError getProblemToFix() {
    return problemToFix;
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

  public void showProblemToFix() {
    if (problemToFix == null) {
      return;
    }
    int offset = problemToFix.getOffset();
    viewer.setSelectedRange(offset, 0);
    viewer.revealRange(offset, 0);
  }

  /**
   * Finds {@link AnalysisError} corresponding to the given {@link Annotation}. May be {@code null}
   * if underlying {@link DartEditor} has no resolved unit.
   */
  AnalysisError getAnalysisError(Annotation annotation) {
    // prepare marker
    if (!(annotation instanceof MarkerAnnotation)) {
      return null;
    }
    IMarker marker = ((MarkerAnnotation) annotation).getMarker();
    if (marker == null) {
      return null;
    }
    int markerOffset = marker.getAttribute(IMarker.CHAR_START, -1);
    int markerLength = marker.getAttribute(IMarker.CHAR_END, -1) - markerOffset;
    // prepare ErrorCode
    ErrorCode errorCode = DartCore.getErrorCode(marker);
    if (errorCode == null) {
      return null;
    }
    // prepare context
    AssistContext context = editor.getAssistContext();
    if (context == null) {
      return null;
    }
    // find AnalysisError
    AnalysisError[] errors = getErrorsTimeBoxed(context);
    for (AnalysisError error : errors) {
      if (error.getErrorCode() == errorCode && error.getOffset() == markerOffset
          && error.getLength() == markerLength) {
        return error;
      }
    }
    // not found
    return null;
  }

  private AnalysisError[] getErrorsTimeBoxed(final AssistContext context) {
    return TimeboxUtils.runObject(new RunnableObject<AnalysisError[]>() {
      @Override
      public AnalysisError[] runObject() {
        return context.getErrors();
      }
    }, AnalysisError.NO_ERRORS, 50, TimeUnit.MILLISECONDS);
  }

  /**
   * @return the {@link IInformationControlCreator} used to display prefix and help user to decide
   *         which correction to choose.
   */
  private IInformationControlCreator getInformationControlCreator() {
    return new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(
            parent,
            DartToolsPlugin.getAdditionalInfoAffordanceString());
      }
    };
  }

  /**
   * Fills {@link #problemToFix}.
   */
  private void prepareProblemsAtCaretLocation() {
    problemToFix = null;
    try {
      Point selectedRange = viewer.getSelectedRange();
      int currOffset = selectedRange.x;
      // prepare AssistContext
      AssistContext context = editor.getAssistContext();
      if (context == null) {
        return;
      }
      // prepare errors
      AnalysisError[] errors = getErrorsTimeBoxed(context);
      // prepare current line range
      IRegion lineInfo = getRegionOfInterest(editor, currOffset);
      if (lineInfo == null) {
        return;
      }
      int rangeStart = lineInfo.getOffset();
      int rangeEnd = rangeStart + lineInfo.getLength();
      // prepare fixable problems on the current line
      List<AnalysisError> allProblems = Lists.newArrayList();
      List<Position> allPositions = Lists.newArrayList();
      for (AnalysisError error : errors) {
        Position pos = new Position(error.getOffset(), error.getLength());
        // check that error is on the current line
        if (!isInside(pos.offset, rangeStart, rangeEnd)) {
          continue;
        }
        // add only if has fix 
        if (QuickFixProcessor_OLD.hasFix(error)) {
          allProblems.add(error);
          allPositions.add(pos);
        }
      }
      // problem under caret
      for (int i = 0; i < allPositions.size(); i++) {
        Position pos = allPositions.get(i);
        if (pos.includes(currOffset)) {
          problemToFix = allProblems.get(i);
          break;
        }
      }
      // problem after caret
      if (problemToFix == null) {
        int bestOffset = Integer.MAX_VALUE;
        for (int i = 0; i < allPositions.size(); i++) {
          Position pos = allPositions.get(i);
          if (pos.offset > currOffset) {
            int offset = pos.offset - currOffset;
            if (offset < bestOffset) {
              bestOffset = offset;
              problemToFix = allProblems.get(i);
            }
          }
        }
      }
      // problem before caret
      if (problemToFix == null) {
        int bestOffset = Integer.MAX_VALUE;
        for (int i = 0; i < allPositions.size(); i++) {
          Position pos = allPositions.get(i);
          if (pos.offset < currOffset) {
            int offset = currOffset - pos.offset;
            if (offset < bestOffset) {
              bestOffset = offset;
              problemToFix = allProblems.get(i);
            }
          }
        }
      }
      // not found
      if (problemToFix == null) {
        return;
      }
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }
}

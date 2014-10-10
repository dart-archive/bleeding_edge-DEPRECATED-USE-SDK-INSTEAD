/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.text;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProcessor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;

/**
 * Instances of {@code DartEditorContentAssistant} wait for analysis to complete on a background
 * thread rather than on the UI thread.
 */
public class DartEditorContentAssistant extends ContentAssistant {
  protected class DartEditorAutoAssistListener extends AutoAssistListener {
    @Override
    protected void showAssist(int showStyle) {
      // Not on the UI thread, so block up to 8 seconds waiting for analysis
      if (waitUntilProcessorReady(true)) {
        super.showAssist(showStyle);
      }
    }
  }

  private final ISourceViewer sourceViewer;

  public DartEditorContentAssistant(ISourceViewer sourceViewer) {
    this.sourceViewer = sourceViewer;
  }

  @Override
  public String showPossibleCompletions() {
    // Defer operation to a background thread waiting for the processor to be ready
    Thread thread = new Thread(getClass().getSimpleName() + " wait for content") {
      @Override
      public void run() {
        if (waitUntilProcessorReady(false)) {
          final StyledText control = sourceViewer.getTextWidget();
          if (control.isDisposed()) {
            return;
          }
          control.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {

              // Filter proposals based upon the current document text
              int offset = getOffset(control);
              IContentAssistProcessor p = getProcessor(sourceViewer, offset);
              if (p instanceof DartCompletionProcessor) {
                IDocument document = sourceViewer.getDocument();
                ((DartCompletionProcessor) p).filterProposals(document, offset);
              }

              DartEditorContentAssistant.super.showPossibleCompletions();
            }
          });
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
    return null;
  }

  @Override
  protected AutoAssistListener createAutoAssistListener() {
    return new DartEditorAutoAssistListener();
  }

  private int getOffset(StyledText control) {
    final int[] result = new int[1];
    if (!control.isDisposed()) {
      control.getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          result[0] = sourceViewer.getSelectedRange().x;
        }
      });
    }
    return result[0];
  }

  /**
   * Returns the content assist processor for the content type of the specified document position.
   * Copied from org.eclipse.jface.text.contentassist.ContentAssistant#getProcessor
   */
  private IContentAssistProcessor getProcessor(ITextViewer viewer, int offset) {
    try {
      IDocument document = viewer.getDocument();
      String type = TextUtilities.getContentType(document, getDocumentPartitioning(), offset, true);
      return getContentAssistProcessor(type);
    } catch (BadLocationException x) {
    }
    return null;
  }

  private String getText(final StyledText control, final int start, final int end) {
    final String[] result = new String[1];
    control.getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        result[0] = control.getText(start, end);
      }
    });
    return result[0];
  }

  private boolean hasFocus(final StyledText control) {
    final boolean[] result = new boolean[1];
    control.getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        result[0] = control.isFocusControl();
      }
    });
    return result[0];
  }

  /**
   * Determine if the content assist computed for the given editor and offset is still valid.
   * 
   * @param control the editor control
   * @param originalOffset the offset in the editor at which the content assist was triggered
   * @return {@code true} if the content assist is still valid, else {@code false}
   */
  private boolean isValid(StyledText control, int originalOffset) {
    if (control.isDisposed() || !hasFocus(control)) {
      return false;
    }
    int newOffset = getOffset(control);
    if (newOffset == originalOffset) {
      return true;
    }
    if (newOffset < originalOffset) {
      return false;
    }
    // If the user has typed characters in an identifier, then the completion results
    // are still valid and will be filtered based upon the newly typed characters
    String text = getText(control, originalOffset, newOffset - 1);
    for (int index = 0; index < text.length(); ++index) {
      char ch = text.charAt(index);
      if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '$') {
        return false;
      }
    }
    return true;
  }

  /**
   * Wait up to the given amount of time for the content assist processor to ready. This may involve
   * communication with the Analysis Server and should not be called on the UI thread.
   * 
   * @param auto {@code true} if triggered automatically such as when the user types a "."
   * @return {@code true} if the processor is ready, else {@code false}
   */
  private boolean waitUntilProcessorReady(boolean auto) {
    StyledText control = sourceViewer.getTextWidget();
    if (control.isDisposed()) {
      return false;
    }
    if (control.getDisplay().getThread() == Thread.currentThread()) {
      throw new RuntimeException("Do not wait for content assist on the UI thread");
    }
    int offset = getOffset(control);
    IContentAssistProcessor p = getProcessor(sourceViewer, offset);
    if (p instanceof DartCompletionProcessor) {
      InstrumentationBuilder instrumentation = Instrumentation.builder("WaitForProposals");
      try {
        instrumentation.metric("Auto", auto);
        boolean ready = ((DartCompletionProcessor) p).waitUntilReady();
        instrumentation.metric("Ready", ready);
        // If a result was computed, then check if the current selection has moved in such as way
        // that the result is no longer useful and should be discarded
        if (ready && !isValid(control, offset)) {
          instrumentation.metric("Discarded", true);
          return false;
        } else {
          instrumentation.metric("Discarded", false);
        }
        return ready;
      } finally {
        instrumentation.log();
      }
    }
    return true;
  }
}

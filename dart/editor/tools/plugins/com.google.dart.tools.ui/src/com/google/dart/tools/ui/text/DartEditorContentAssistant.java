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
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProcessor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Display;

/**
 * Instances of {@code DartEditorContentAssistant} wait for analysis to complete on a background
 * thread rather than on the UI thread.
 */
public class DartEditorContentAssistant extends ContentAssistant {
  protected class DartEditorAutoAssistListener extends AutoAssistListener {
    @Override
    public void keyPressed(final KeyEvent e) {
      // Run in async, so let the widget to update the caret offset.
      Display.getCurrent().asyncExec(new Runnable() {
        @Override
        public void run() {
          super_keyPressed(e);
        }
      });
    }

    @Override
    protected void showAssist(int showStyle) {
      // Not on the UI thread, so block up to 8 seconds waiting for analysis
      if (waitUntilProcessorReady(true, caretOffset)) {
        StyledText control = sourceViewer.getTextWidget();
        if (control.isDisposed()) {
          return;
        }
        filterProposals();
        super.showAssist(showStyle);
      }
    }

    private void super_keyPressed(KeyEvent e) {
      super.keyPressed(e);
    }
  }

  private final ISourceViewer sourceViewer;

  private boolean hasFocus;
  private final FocusListener focusListener = new FocusListener() {
    @Override
    public void focusGained(FocusEvent e) {
      hasFocus = true;
    }

    @Override
    public void focusLost(FocusEvent e) {
      hasFocus = false;
    }
  };

  private int caretOffset;
  private final CaretListener caretListener = new CaretListener() {
    @Override
    public void caretMoved(CaretEvent event) {
      caretOffset = event.caretOffset;
    }
  };

  public DartEditorContentAssistant(ISourceViewer sourceViewer) {
    this.sourceViewer = sourceViewer;
  }

  @Override
  public void install(ITextViewer textViewer) {
    super.install(textViewer);
    sourceViewer.getTextWidget().addFocusListener(focusListener);
    sourceViewer.getTextWidget().addCaretListener(caretListener);
  }

  @Override
  public String showPossibleCompletions() {
    // Defer operation to a background thread waiting for the processor to be ready
    Thread thread = new Thread(getClass().getSimpleName() + " wait for content") {
      @Override
      public void run() {
        if (waitUntilProcessorReady(false, caretOffset)) {
          final StyledText control = sourceViewer.getTextWidget();
          if (control.isDisposed()) {
            return;
          }
          control.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
              filterProposals();
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
  public void uninstall() {
    sourceViewer.getTextWidget().removeFocusListener(focusListener);
    sourceViewer.getTextWidget().removeCaretListener(caretListener);
    super.uninstall();
  }

  @Override
  protected AutoAssistListener createAutoAssistListener() {
    return new DartEditorAutoAssistListener();
  }

  /**
   * Filter proposals based upon the current document text.
   */
  private void filterProposals() {
    IContentAssistProcessor p = getProcessor(sourceViewer, caretOffset);
    if (p instanceof DartCompletionProcessor) {
      IDocument document = sourceViewer.getDocument();
      ((DartCompletionProcessor) p).filterProposals(document, caretOffset);
    }
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

  /**
   * Determine if the content assist computed for the given editor and offset is still valid.
   * 
   * @param control the editor control
   * @param originalOffset the offset in the editor at which the content assist was triggered
   * @return {@code true} if the content assist is still valid, else {@code false}
   */
  private boolean isValid(StyledText control, int originalOffset) {
    if (control.isDisposed() || !hasFocus) {
      return false;
    }
    if (caretOffset == originalOffset) {
      return true;
    }
    if (caretOffset < originalOffset) {
      return false;
    }
    // If the user has typed characters in an identifier, then the completion results
    // are still valid and will be filtered based upon the newly typed characters
    String text = getText(control, originalOffset, caretOffset - 1);
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
  private boolean waitUntilProcessorReady(boolean auto, int offset) {
    StyledText control = sourceViewer.getTextWidget();
    if (control.isDisposed()) {
      return false;
    }
    if (control.getDisplay().getThread() == Thread.currentThread()) {
      throw new RuntimeException("Do not wait for content assist on the UI thread");
    }
    IContentAssistProcessor p = getProcessor(sourceViewer, offset);
    if (p instanceof DartCompletionProcessor) {
      InstrumentationBuilder instrumentation = Instrumentation.builder("WaitForProposals");
      try {
        instrumentation.metric("Auto", auto);
        instrumentation.metric("ServerEnabled", DartCoreDebug.ENABLE_ANALYSIS_SERVER);
        boolean ready = ((DartCompletionProcessor) p).waitUntilReady(auto, offset);
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

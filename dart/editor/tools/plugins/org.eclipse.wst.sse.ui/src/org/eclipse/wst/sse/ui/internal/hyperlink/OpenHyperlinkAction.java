/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.hyperlink;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import java.util.ResourceBundle;

/**
 * Open hyperlink action
 */
public class OpenHyperlinkAction extends TextEditorAction {
  private IHyperlinkDetector[] fHyperlinkDetectors;
  private ITextViewer fTextViewer;

  public OpenHyperlinkAction(ResourceBundle bundle, String prefix, ITextEditor editor,
      ITextViewer viewer) {
    super(bundle, prefix, editor);
    fTextViewer = viewer;
  }

  public void setHyperlinkDetectors(IHyperlinkDetector[] detectors) {
    fHyperlinkDetectors = detectors;
  }

  public void run() {
    if (fHyperlinkDetectors == null)
      return;
    ISelection selection = getTextEditor().getSelectionProvider().getSelection();
    if (selection == null || !(selection instanceof ITextSelection)) {
      return;
    }

    ITextSelection textSelection = (ITextSelection) selection;
    final IRegion region = new Region(textSelection.getOffset(), textSelection.getLength());
    IHyperlink hyperlink = null;

    synchronized (fHyperlinkDetectors) {
      for (int i = 0, length = fHyperlinkDetectors.length; i < length && hyperlink == null; i++) {
        final IHyperlinkDetector detector = fHyperlinkDetectors[i];
        if (detector == null)
          continue;

        /* The array is final, but its contents aren't */
        final IHyperlink[][] hyperlinks = new IHyperlink[1][];

        /*
         * Detect from within a SafeRunnable since the list of detectors is extensible
         */
        SafeRunnable detectorRunnable = new SafeRunnable() {
          public void run() throws Exception {
            hyperlinks[0] = detector.detectHyperlinks(fTextViewer, region, false);
          }
        };
        SafeRunner.run(detectorRunnable);

        if (hyperlinks[0] == null)
          continue;

        if (hyperlinks.length > 0)
          hyperlink = hyperlinks[0][0];
      }
    }
    if (hyperlink != null) {
      /**
       * Force the highlight range to change when the hyperlink is opened by altering the
       * highlighted range beforehand.
       */
      getTextEditor().setHighlightRange(Math.max(0, region.getOffset() - 1), 0, false);
      hyperlink.open();
    }
  }
}

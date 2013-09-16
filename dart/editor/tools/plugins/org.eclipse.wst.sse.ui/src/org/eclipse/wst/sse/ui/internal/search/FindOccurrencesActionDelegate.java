/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.util.PlatformStatusLineUtil;

import java.util.Iterator;
import java.util.List;

/**
 * Performs the appropriate FindOccurrencesProcessor action call based on selection. Clients can add
 * processors for different partitions via <code>getProcessors</code>
 */
abstract public class FindOccurrencesActionDelegate implements IEditorActionDelegate,
    IActionDelegate2, IViewActionDelegate {
  private IEditorPart fEditor;

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    fEditor = targetEditor;
  }

  public void dispose() {
    // nulling out just in case
    fEditor = null;
  }

  public void init(IAction action) {
    if (action != null) {
      action.setText(SSEUIMessages.FindOccurrences_label);
    }
  }

  public void runWithEvent(IAction action, Event event) {
    run(action);
  }

  public void run(IAction action) {
    boolean okay = false;
    if (fEditor instanceof ITextEditor) {
      ITextEditor textEditor = (ITextEditor) fEditor;
      IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
      if (document != null) {
        ITextSelection textSelection = getTextSelection(textEditor);
        FindOccurrencesProcessor findOccurrenceProcessor = getProcessorForCurrentSelection(
            document, textSelection);
        if (findOccurrenceProcessor != null) {
          if (textEditor.getEditorInput() instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) textEditor.getEditorInput()).getFile();
            okay = findOccurrenceProcessor.findOccurrences(document, textSelection, file);
          }
        }
      }
    }
    if (okay) {
      // clear status message
      PlatformStatusLineUtil.clearStatusLine();
    } else {
      String errorMessage = SSEUIMessages.FindOccurrencesActionProvider_0; //$NON-NLS-1$
      if (fEditor instanceof StructuredTextEditor) {
        PlatformStatusLineUtil.displayTemporaryErrorMessage(
            ((StructuredTextEditor) fEditor).getTextViewer(), errorMessage);

      } else {
        PlatformStatusLineUtil.displayErrorMessage(errorMessage);
        PlatformStatusLineUtil.addOneTimeClearListener();
      }
    }
  }

  public void init(IViewPart view) {
    // do nothing
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // clear status message
    PlatformStatusLineUtil.clearStatusLine();
  }

  /**
   * Get the appropriate find occurrences processor
   * 
   * @param document - assumes not null
   * @param textSelection
   * @return
   */
  private FindOccurrencesProcessor getProcessorForCurrentSelection(IDocument document,
      ITextSelection textSelection) {
    // check if we have an action that's enabled on the current partition
    ITypedRegion tr = getPartition(document, textSelection);
    String partition = tr != null ? tr.getType() : ""; //$NON-NLS-1$

    Iterator it = getProcessors().iterator();
    FindOccurrencesProcessor processor = null;
    while (it.hasNext()) {
      processor = (FindOccurrencesProcessor) it.next();
      // we just choose the first action that can handle the partition
      if (processor.enabledForParitition(partition))
        return processor;
    }
    List extendedFindOccurrencesProcessors = ExtendedConfigurationBuilder.getInstance().getConfigurations(
        FindOccurrencesProcessor.class.getName(), partition);
    for (int i = 0; i < extendedFindOccurrencesProcessors.size(); i++) {
      Object o = extendedFindOccurrencesProcessors.get(i);
      if (o instanceof FindOccurrencesProcessor) {
        /*
         * We just choose the first registered processor that explicitly says it can handle the
         * partition
         */
        processor = (FindOccurrencesProcessor) o;
        if (processor.enabledForParitition(partition))
          return processor;
      }
    }
    return null;
  }

  private ITypedRegion getPartition(IDocument document, ITextSelection textSelection) {
    ITypedRegion region = null;
    if (textSelection != null) {
      try {
        region = document.getPartition(textSelection.getOffset());
      } catch (BadLocationException e) {
        region = null;
      }
    }
    return region;
  }

  private ITextSelection getTextSelection(ITextEditor textEditor) {
    ITextSelection textSelection = null;
    ISelection selection = textEditor.getSelectionProvider().getSelection();
    if (selection instanceof ITextSelection && !selection.isEmpty()) {
      textSelection = (ITextSelection) selection;
    }
    return textSelection;
  }

  abstract protected List getProcessors();
}

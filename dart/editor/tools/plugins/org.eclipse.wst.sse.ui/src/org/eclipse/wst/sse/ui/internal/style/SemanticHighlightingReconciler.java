/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.style;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.sse.core.internal.model.ModelManagerImpl;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.ISemanticHighlighting;
import org.eclipse.wst.sse.ui.ISemanticHighlightingExtension;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.style.SemanticHighlightingManager.HighlightedPosition;
import org.eclipse.wst.sse.ui.internal.style.SemanticHighlightingManager.HighlightingStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic highlighting reconciler for Structured Source Editors. Based on
 * org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingReconciler
 * 
 * @since 3.1
 */
public class SemanticHighlightingReconciler implements IReconcilingStrategy,
    IReconcilingStrategyExtension {

  private IDocument fDocument;

  private ITextViewer fViewer;
  private SemanticHighlightingPresenter fPresenter;
  private ISemanticHighlighting[] fSemanticHighlightings;
  private HighlightingStyle[] fHighlightings;

  private List fAddedPositions = new ArrayList();
  private List fRemovedPositions = new ArrayList();
  /** Number of removed positions */
  private int fNOfRemovedPositions;

  /** Background job */
  private Job fJob;
  /** Background job lock */
  private final Object fJobLock = new Object();

  /** Reconcile operation lock. */
  private final Object fReconcileLock = new Object();
  private boolean fIsReconciling = false;
  /**
   * The semantic highlighting presenter - cache for background thread, only valid during
   * {@link #reconcile(IRegion)}
   */
  private SemanticHighlightingPresenter fJobPresenter;
  /**
   * Semantic highlightings - cache for background thread, only valid during
   * {@link #reconcile(IRegion)}
   */
  private ISemanticHighlighting[] fJobSemanticHighlightings;
  /** HighlightingStyle - cache for background thread, only valid during {@link #reconcile(IRegion)} */
  private HighlightingStyle[] fJobHighlightings;

  private boolean fIsInstalled;

  public void install(ITextViewer sourceViewer, SemanticHighlightingPresenter presenter,
      ISemanticHighlighting[] semanticHighlightings, HighlightingStyle[] highlightings) {
    fViewer = sourceViewer;
    fPresenter = presenter;
    fSemanticHighlightings = semanticHighlightings;
    fHighlightings = highlightings;
    fIsInstalled = true;
  }

  public void uninstall() {
    fIsInstalled = false;
    fViewer = null;
    fPresenter = null;
    fSemanticHighlightings = null;
    fHighlightings = null;
  }

  public void reconcile(IRegion partition) {
    // ensure at most one thread can be reconciling at any time
    synchronized (fReconcileLock) {
      if (fIsReconciling)
        return;
      else
        fIsReconciling = true;
    }
    fJobPresenter = fPresenter;
    fJobSemanticHighlightings = fSemanticHighlightings;
    fJobHighlightings = fHighlightings;
    IStructuredModel model = null;
    try {
      if (fJobPresenter == null || fJobSemanticHighlightings == null || fJobHighlightings == null
          || fDocument == null)
        return;

      fJobPresenter.setCanceled(false);

      startReconcilingPositions();
      IStructuredDocument document = (IStructuredDocument) fDocument;
      model = ModelManagerImpl.getInstance().getModelForRead(document);
      IStructuredDocumentRegion[] regions = document.getStructuredDocumentRegions(
          partition.getOffset(), partition.getLength());
      for (int i = 0; i < regions.length && fIsInstalled; i++) {
        if (document.containsReadOnly(regions[i].getStartOffset(), regions[i].getLength()))
          addPosition(new Position(regions[i].getStartOffset(), regions[i].getLength()), null, true);
        else {
          for (int j = 0; j < fJobSemanticHighlightings.length && fIsInstalled; j++) {
            if (fJobHighlightings[j].isEnabled()) {
              Position[] consumes = null;
              if (fJobSemanticHighlightings[j] instanceof ISemanticHighlightingExtension
                  && model != null) {
                consumes = ((ISemanticHighlightingExtension) fJobSemanticHighlightings[j]).consumes(
                    regions[i], model.getIndexedRegion(regions[i].getStartOffset()));
              } else {
                consumes = fJobSemanticHighlightings[j].consumes(regions[i]);
              }
              if (consumes != null) {
                for (int k = 0; k < consumes.length; k++)
                  addPosition(consumes[k], fJobHighlightings[j]);
              }
            }
          }
        }
      }

      if (fIsInstalled) {
        List oldPositions = fRemovedPositions;
        List newPositions = new ArrayList(fNOfRemovedPositions);
        for (int i = 0, n = oldPositions.size(); i < n && fIsInstalled; i++) {
          Object current = oldPositions.get(i);
          if (current != null)
            newPositions.add(current);
        }
        fRemovedPositions = newPositions;

        TextPresentation presentation = null;
        if (!fJobPresenter.isCanceled())
          presentation = fJobPresenter.createPresentation(fAddedPositions, fRemovedPositions);
        if (!fJobPresenter.isCanceled())
          updatePresentation(presentation, fAddedPositions, fRemovedPositions);
      }
      stopReconcilingPositions();
    } finally {
      fJobPresenter = null;
      fJobSemanticHighlightings = null;
      fJobHighlightings = null;
      if (model != null)
        model.releaseFromRead();
      synchronized (fReconcileLock) {
        fIsReconciling = false;
      }
    }
  }

  private void addPosition(Position position, HighlightingStyle highlighting) {
    addPosition(position, highlighting, false);
  }

  private void addPosition(Position position, HighlightingStyle highlighting, boolean isReadOnly) {
    boolean isExisting = false;
    // TODO: use binary search
    for (int i = 0, n = fRemovedPositions.size(); i < n; i++) {
      HighlightedPosition highlightedPosition = (HighlightedPosition) fRemovedPositions.get(i);
      if (highlightedPosition == null)
        continue;
      if (highlightedPosition.isEqual(position, highlighting)) {
        isExisting = true;
        fRemovedPositions.set(i, null);
        fNOfRemovedPositions--;
        break;
      }
    }
    if (!isExisting) {
      fAddedPositions.add(fJobPresenter.createHighlightedPosition(position, highlighting,
          isReadOnly));
    }
  }

  /**
   * Update the presentation.
   * 
   * @param textPresentation the text presentation
   * @param addedPositions the added positions
   * @param removedPositions the removed positions
   */
  private void updatePresentation(TextPresentation textPresentation, List addedPositions,
      List removedPositions) {
    Runnable runnable = fJobPresenter.createUpdateRunnable(textPresentation, addedPositions,
        removedPositions);
    if (runnable == null)
      return;

    if (fViewer == null)
      return;

    Control viewerControl = fViewer.getTextWidget();
    if (viewerControl == null)
      return;

    Display display = viewerControl.getDisplay();
    if (display == null || display.isDisposed())
      return;

    display.asyncExec(runnable);
  }

  /**
   * Start reconciling positions.
   */
  private void startReconcilingPositions() {
    fJobPresenter.addAllPositions(fRemovedPositions);
    fNOfRemovedPositions = fRemovedPositions.size();
  }

  /**
   * Stop reconciling positions.
   */
  private void stopReconcilingPositions() {
    fRemovedPositions.clear();
    fNOfRemovedPositions = 0;
    fAddedPositions.clear();
  }

  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    reconcile(dirtyRegion);
  }

  public void setDocument(IDocument document) {
    fDocument = (document instanceof IStructuredDocument) ? document : null;
  }

  public void initialReconcile() {
    // Do nothing
  }

  public void setProgressMonitor(IProgressMonitor monitor) {
  }

  /**
   * Schedule a background job for reconciling the Semantic Highlighting model.
   */
  private void scheduleJob() {
    synchronized (fJobLock) {
      final Job oldJob = fJob;
      if (fJob != null) {
        fJob.cancel();
        fJob = null;
      }

      fJob = new Job("Semantic Highlighting Job") {
        protected IStatus run(IProgressMonitor monitor) {
          if (oldJob != null) {
            try {
              oldJob.join();
            } catch (InterruptedException e) {
              Logger.logException(e);
              return Status.CANCEL_STATUS;
            }
          }
          if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

          reconcile(new Region(0, fDocument.getLength()));
          synchronized (fJobLock) {
            // allow the job to be gc'ed
            if (fJob == this)
              fJob = null;
          }
          return Status.OK_STATUS;
        }
      };
      fJob.setSystem(true);
      fJob.setPriority(Job.DECORATE);
      fJob.schedule();
    }
  }

  public void refresh() {
    if (fDocument != null)
      scheduleJob();
  }
}

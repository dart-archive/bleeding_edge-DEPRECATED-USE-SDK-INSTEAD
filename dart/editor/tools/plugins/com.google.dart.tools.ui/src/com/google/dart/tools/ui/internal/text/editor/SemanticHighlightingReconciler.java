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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.dart.IDartReconcilingListener;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.HighlightedPosition;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.Highlighting;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Semantic highlighting reconciler - Background thread implementation.
 */
public class SemanticHighlightingReconciler implements IDartReconcilingListener, ITextInputListener {

  /**
   * Collects positions from the AST.
   */
  private class PositionCollector extends GeneralizingAstVisitor<Void> {
    /**
     * Cache tokens for performance.
     */
    private final SemanticToken token = new SemanticToken();

    @Override
    public Void visitNode(AstNode node) {
      processNode(token, node);
      return super.visitNode(node);
    }

    /**
     * Add a position with the given range and highlighting iff it does not exist already.
     * 
     * @param offset The range offset
     * @param length The range length
     * @param highlighting The highlighting
     */
    private void addPosition(int offset, int length, Highlighting highlighting) {
      boolean isExisting = false;

      Position[] positions = removedPositions;
      int left = 0;
      int right = positions.length - 1;
      int oldMid = -1;

      // Do a binary search through the positions array, looking for the given offset.
      while (left <= right) {
        int mid;

        // Choose a mid.
        if (positions[left].getOffset() == offset) {
          mid = left;
        } else if (positions[right].getOffset() == offset) {
          mid = right;
        } else {
          mid = (left + right) / 2;
        }

        if (oldMid == mid) {
          // If we didn't make any progress, exit the search.
          break;
        } else {
          oldMid = mid;
        }

        if (offset > positions[mid].getOffset()) {
          left = mid;
        } else if (offset < positions[mid].getOffset()) {
          right = mid;
        } else {
          int index = mid;

          // We found offset. Back up until we reach the first instance of that offset.
          while (index > 0 && positions[index - 1].getOffset() == offset) {
            index--;
          }

          // Check to see if we want to keep all the positions which equal offset.
          while (index < positions.length && positions[index].getOffset() == offset) {
            if (!removedPositionsDeleted[index]) {
              HighlightedPosition position = (HighlightedPosition) positions[index];

              if (position.isEqual(offset, length, highlighting)) {
                isExisting = true;
                removedPositionsDeleted[index] = true;
                fNOfRemovedPositions--;
                break;
              }
            }

            index++;
          }

          break;
        }
      }

      // Older (and much simpler) O(n^2) algorithm for updating removedPositions.
//      for (int i = 0, n = removedPositions.length; i < n; i++) {
//        if (removedPositionsDeleted[i]) {
//          continue;
//        }
//
//        HighlightedPosition position = (HighlightedPosition) removedPositions[i];
//
//        if (position.isEqual(offset, length, highlighting)) {
//          isExisting = true;
//          removedPositionsDeleted[i] = true;
//          fNOfRemovedPositions--;
//          break;
//        }
//      }

      if (!isExisting) {
        fAddedPositions.add(fJobPresenter.createHighlightedPosition(offset, length, highlighting));
      }
    }
  }

  /** Position collector */
  private final PositionCollector fCollector = new PositionCollector();

  private Comparator<Position> positionsComparator = new Comparator<Position>() {
    @Override
    public int compare(Position position1, Position position2) {
      return position1.offset - position2.offset;
    }
  };

  /** The Dart editor on which this semantic highlighting reconciler is installed */
  private DartEditor fEditor;

  /** The source viewer this semantic highlighting reconciler is installed on */
  private ISourceViewer fSourceViewer;

  /** The semantic highlighting presenter */
  private SemanticHighlightingPresenter fPresenter;

  /** Semantic highlightings */
  private SemanticHighlighting[] fSemanticHighlightings;

  /** Highlightings */
  private Highlighting[] fHighlightings;

  /** Background job's added highlighted positions */
  private final List<Position> fAddedPositions = new ArrayList<Position>();

  /** Background job's removed highlighted positions */
  private List<Position> fRemovedPositions = new ArrayList<Position>();
  private Position[] removedPositions;
  private boolean[] removedPositionsDeleted;

  /** Number of removed positions */
  private int fNOfRemovedPositions;

  /** Background job */
  private Job fJob;

  /** Background job lock */
  private final Object fJobLock = new Object();

  /**
   * Reconcile operation lock.
   */
  private final Object fReconcileLock = new Object();

  /**
   * The semantic highlighting presenter - cache for background thread, only valid during
   * {@link #reconciled(DartUnit, boolean, IProgressMonitor)}
   */
  private SemanticHighlightingPresenter fJobPresenter;

  /**
   * Semantic highlightings - cache for background thread, only valid during
   * {@link #reconciled(DartUnit, boolean, IProgressMonitor)}
   */
  private SemanticHighlighting[] fJobSemanticHighlightings;

  /**
   * Highlightings - cache for background thread, only valid during
   * {@link #reconciled(DartUnit, boolean, IProgressMonitor)}
   */
  private Highlighting[] fJobHighlightings;

  @Override
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    synchronized (fJobLock) {
      if (fJob != null) {
        fJob.cancel();
        fJob = null;
      }
    }
  }

  @Override
  public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
    if (newInput != null) {
      scheduleJob();
    }
  }

  /**
   * Install this reconciler on the given editor, presenter and highlightings.
   * 
   * @param editor the editor
   * @param sourceViewer the source viewer
   * @param presenter the semantic highlighting presenter
   * @param semanticHighlightings the semantic highlightings
   * @param highlightings the highlightings
   */
  public void install(DartEditor editor, DartSourceViewer sourceViewer,
      SemanticHighlightingPresenter presenter, SemanticHighlighting[] semanticHighlightings,
      Highlighting[] highlightings) {
    fPresenter = presenter;
    fSemanticHighlightings = semanticHighlightings;
    fHighlightings = highlightings;

    fEditor = editor;
    fSourceViewer = sourceViewer;

    if (fEditor instanceof CompilationUnitEditor) {
      ((CompilationUnitEditor) fEditor).addReconcileListener(this);
      scheduleJob();
    } else if (fEditor == null) {
      fSourceViewer.addTextInputListener(this);
      scheduleJob();
    }
  }

  @Override
  public void reconciled(CompilationUnit ast) {
    synchronized (fReconcileLock) {
      fJobPresenter = fPresenter;
      fJobSemanticHighlightings = fSemanticHighlightings;
      fJobHighlightings = fHighlightings;

      try {
        if (fJobPresenter == null || fJobSemanticHighlightings == null || fJobHighlightings == null) {
          return;
        }

        fJobPresenter.setCanceled(false);

        startReconcilingPositions();

        if (!fJobPresenter.isCanceled()) {
          reconcilePositions(ast);
        }

        TextPresentation textPresentation = null;
        if (!fJobPresenter.isCanceled()) {
          textPresentation = fJobPresenter.createPresentation(fAddedPositions, fRemovedPositions);
        }

        if (!fJobPresenter.isCanceled()) {
          updatePresentation(textPresentation, fAddedPositions, fRemovedPositions);
        }

        stopReconcilingPositions();
      } finally {
        fJobPresenter = null;
        fJobSemanticHighlightings = null;
        fJobHighlightings = null;
      }
    }
  }

  /**
   * Refreshes the highlighting.
   */
  public void refresh() {
    scheduleJob();
  }

  /**
   * Uninstall this reconciler from the editor
   */
  public void uninstall() {
    if (fPresenter != null) {
      fPresenter.setCanceled(true);
    }

    if (fEditor != null) {
      if (fEditor instanceof CompilationUnitEditor) {
        ((CompilationUnitEditor) fEditor).removeReconcileListener(this);
      } else {
        fSourceViewer.removeTextInputListener(this);
      }
      fEditor = null;
    }

    fSourceViewer = null;
    fSemanticHighlightings = null;
    fHighlightings = null;
    fPresenter = null;
  }

  private final void processNode(SemanticToken token, AstNode node) {
    ISourceViewer sourceViewer = this.fSourceViewer;
    if (sourceViewer == null) {
      return;
    }
    IDocument document = sourceViewer.getDocument();
    if (document == null) {
      return;
    }
    // update token
    token.update(node);
    token.attachSource(document);
    // try SemanticHighlighting instances
    for (int i = 0, n = fJobSemanticHighlightings.length; i < n; i++) {
      if (fJobHighlightings[i].isEnabled()) {
        SemanticHighlighting semanticHighlighting = fJobSemanticHighlightings[i];
        // try multiple positions
        {
          List<SourceRange> ranges = semanticHighlighting.consumesMulti(token);
          if (ranges != null) {
            for (SourceRange range : ranges) {
              int offset = range.getOffset();
              int length = range.getLength();
              if (offset > -1 && length > 0) {
                fCollector.addPosition(offset, length, fJobHighlightings[i]);
              }
            }
            break;
          }
        }
        // try single position
        boolean consumes;
        if (node instanceof SimpleIdentifier) {
          consumes = semanticHighlighting.consumesIdentifier(token);
        } else {
          consumes = semanticHighlighting.consumes(token);
        }
        if (consumes) {
          int offset = node.getOffset();
          int length = node.getLength();
          if (offset > -1 && length > 0) {
            fCollector.addPosition(offset, length, fJobHighlightings[i]);
          }
          break;
        }
      }
    }
    token.clear();
  }

  /**
   * Reconcile positions based on the AST subtrees
   * 
   * @param subtrees the AST subtrees
   */
  private void reconcilePositions(CompilationUnit unit) {
    // copy fRemovedPositions into removedPositions and removedPositionsDeleted
    removedPositions = fRemovedPositions.toArray(new Position[fRemovedPositions.size()]);
    Arrays.sort(removedPositions, positionsComparator);
    removedPositionsDeleted = new boolean[removedPositions.length];

    unit.accept(fCollector);

    // copy removedPositions and removedPositionsDeleted into fRemovedPositions
    fRemovedPositions = new ArrayList<Position>(removedPositions.length);
    for (int i = 0; i < removedPositions.length; i++) {
      if (!removedPositionsDeleted[i]) {
        fRemovedPositions.add(removedPositions[i]);
      }
    }

    List<Position> oldPositions = fRemovedPositions;
    List<Position> newPositions = new ArrayList<Position>(fNOfRemovedPositions);
    for (int i = 0, n = oldPositions.size(); i < n; i++) {
      Position current = oldPositions.get(i);
      if (current != null) {
        newPositions.add(current);
      }
    }
    fRemovedPositions = newPositions;

    Collections.sort(fRemovedPositions, positionsComparator);
    Collections.sort(fAddedPositions, positionsComparator);
  }

  /**
   * Schedule a background job for retrieving the AST and reconciling the Semantic Highlighting
   * model.
   */
  private void scheduleJob() {
//    final DartElement element = fEditor.getInputDartElement();

    synchronized (fJobLock) {
      final Job oldJob = fJob;
      if (fJob != null) {
        fJob.cancel();
        fJob = null;
      }

      fJob = new Job(DartEditorMessages.SemanticHighlighting_job) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          if (oldJob != null) {
            try {
              oldJob.join();
            } catch (InterruptedException e) {
              DartToolsPlugin.log(e);
              return Status.CANCEL_STATUS;
            }
          }
          if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
          }
          // prepare CompilationUnit
          CompilationUnit unit = null;
          {
            DartEditor editor = fEditor;
            if (editor == null) {
              return Status.CANCEL_STATUS;
            }
            while (!monitor.isCanceled()) {
              unit = editor.getInputUnit();
              if (unit != null) {
                break;
              }
              Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
          }
          // if has CompilationUnit, do reconcile
          if (unit != null) {
            reconciled(unit);
          }
          // done
          synchronized (fJobLock) {
            // allow the job to be gc'ed
            if (fJob == this) {
              fJob = null;
            }
          }
          return Status.OK_STATUS;
        }
      };
      fJob.setSystem(true);
      fJob.setPriority(Job.DECORATE);
      fJob.schedule();
    }
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

  /**
   * Update the presentation.
   * 
   * @param textPresentation the text presentation
   * @param addedPositions the added positions
   * @param removedPositions the removed positions
   */
  private void updatePresentation(TextPresentation textPresentation, List<Position> addedPositions,
      List<Position> removedPositions) {
    Runnable runnable = fJobPresenter.createUpdateRunnable(
        textPresentation,
        addedPositions,
        removedPositions);
    if (runnable == null) {
      return;
    }

    DartEditor editor = fEditor;
    if (editor == null) {
      return;
    }

    IWorkbenchPartSite site = editor.getSite();
    if (site == null) {
      return;
    }

    Shell shell = site.getShell();
    if (shell == null || shell.isDisposed()) {
      return;
    }

    Display display = shell.getDisplay();
    if (display == null || display.isDisposed()) {
      return;
    }

//    display.asyncExec(runnable);

    addedPositions = Lists.newArrayList(addedPositions);
    removedPositions = Lists.newArrayList(removedPositions);
    fJobPresenter.updatePresentation(display, addedPositions, removedPositions);
  }
}

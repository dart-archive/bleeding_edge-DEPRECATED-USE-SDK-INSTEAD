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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartExportDirective;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPartOfDirective;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;
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

/**
 * Semantic highlighting reconciler - Background thread implementation.
 */
public class SemanticHighlightingReconciler implements IDartReconcilingListener, ITextInputListener {

  /**
   * Collects positions from the AST.
   */
  private class PositionCollector extends ASTVisitor<Void> {

    /**
     * Cache tokens for performance.
     */
    private final SemanticToken token = new SemanticToken();

    @Override
    public Void visitClass(DartClass node) {
      processNode(token, node);
      return super.visitClass(node);
    }

    @Override
    public Void visitDoubleLiteral(DartDoubleLiteral node) {
      processNode(token, node);
      return null;
    }

    @Override
    public Void visitExportDirective(DartExportDirective node) {
      processNode(token, node);
      return super.visitExportDirective(node);
    }

    @Override
    public Void visitExpression(DartExpression node) {
      processNode(token, node);
      return super.visitExpression(node);
    }

    @Override
    public Void visitFieldDefinition(DartFieldDefinition node) {
      processNode(token, node);
      return super.visitFieldDefinition(node);
    }

    @Override
    public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
      processNode(token, node);
      return super.visitFunctionTypeAlias(node);
    }

    @Override
    public Void visitIdentifier(DartIdentifier node) {
      processNode(token, node);
      return null;
    }

    @Override
    public Void visitImportDirective(DartImportDirective node) {
      processNode(token, node);
      return super.visitImportDirective(node);
    }

    @Override
    public Void visitIntegerLiteral(DartIntegerLiteral node) {
      processNode(token, node);
      return null;
    }

    @Override
    public Void visitLibraryDirective(DartLibraryDirective node) {
      processNode(token, node);
      return super.visitLibraryDirective(node);
    }

    @Override
    public Void visitMethodDefinition(DartMethodDefinition node) {
      processNode(token, node);
      return super.visitMethodDefinition(node);
    }

    @Override
    public Void visitPartOfDirective(DartPartOfDirective node) {
      processNode(token, node);
      return super.visitPartOfDirective(node);
    }

    @Override
    public Void visitPropertyAccess(DartPropertyAccess node) {
      processNode(token, node);
      return super.visitPropertyAccess(node);
    }

    @Override
    public Void visitSourceDirective(DartSourceDirective node) {
      processNode(token, node);
      return super.visitSourceDirective(node);
    }

    @Override
    public Void visitStatement(DartStatement node) {
      processNode(token, node);
      return super.visitStatement(node);
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
   * <code>true</code> if any thread is executing <code>reconcile</code>, <code>false</code>
   * otherwise.
   */
  private boolean fIsReconciling = false;

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
  public void aboutToBeReconciled() {
    // Do nothing
  }

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
  public void install(DartEditor editor, ISourceViewer sourceViewer,
      SemanticHighlightingPresenter presenter, SemanticHighlighting[] semanticHighlightings,
      Highlighting[] highlightings) {
    fPresenter = presenter;
    fSemanticHighlightings = semanticHighlightings;
    fHighlightings = highlightings;

    fEditor = editor;
    fSourceViewer = sourceViewer;

    if (fEditor instanceof CompilationUnitEditor) {
      ((CompilationUnitEditor) fEditor).addReconcileListener(this);
    } else if (fEditor == null) {
      fSourceViewer.addTextInputListener(this);
      scheduleJob();
    }
  }

  @Override
  public void reconciled(DartUnit ast, boolean forced, IProgressMonitor progressMonitor) {
    // don't update semantic highlighting if there are parsing problems to avoid "flashing"
    if (ast != null && ast.hasParseErrors()) {
      return;
    }

    // ensure at most one thread can be reconciling at any time
    synchronized (fReconcileLock) {
      if (fIsReconciling) {
        return;
      } else {
        fIsReconciling = true;
      }
    }
    fJobPresenter = fPresenter;
    fJobSemanticHighlightings = fSemanticHighlightings;
    fJobHighlightings = fHighlightings;

    try {
      if (fJobPresenter == null || fJobSemanticHighlightings == null || fJobHighlightings == null) {
        return;
      }

      fJobPresenter.setCanceled(progressMonitor.isCanceled());

      if (ast == null || ast.getLibrary() == null || fJobPresenter.isCanceled()) {
        return;
      }

      DartNode[] subtrees = getAffectedSubtrees(ast);
      if (subtrees.length == 0) {
        return;
      }

      startReconcilingPositions();

      if (!fJobPresenter.isCanceled()) {
        reconcilePositions(subtrees);
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
      synchronized (fReconcileLock) {
        fIsReconciling = false;
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

  /**
   * @param node Root node
   * @return Array of subtrees that may be affected by past document changes
   */
  private DartNode[] getAffectedSubtrees(DartNode node) {
    // TODO: only return nodes which are affected by document changes - would
    // require an 'anchor' concept for taking distant effects into account
    return new DartNode[] {node};
  }

  private final void processNode(SemanticToken token, DartNode node) {
    token.update(node);
    token.attachSource(fSourceViewer.getDocument());
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
        if (node instanceof DartIdentifier) {
          consumes = semanticHighlighting.consumesIdentifier(token);
        } else {
          consumes = semanticHighlighting.consumes(token);
        }
        if (consumes) {
          int offset = node.getSourceInfo().getOffset();
          int length = node.getSourceInfo().getLength();
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
  private void reconcilePositions(DartNode[] subtrees) {
    // copy fRemovedPositions into removedPositions and removedPositionsDeleted
    removedPositions = fRemovedPositions.toArray(new Position[fRemovedPositions.size()]);
    Arrays.sort(removedPositions, positionsComparator);
    removedPositionsDeleted = new boolean[removedPositions.length];

    // FIXME: remove positions not covered by subtrees
    for (int i = 0, n = subtrees.length; i < n; i++) {
      // subtrees[i].accept(fCollector);
      subtrees[i].accept(fCollector);
    }

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
    final DartElement element = fEditor.getInputDartElement();

    synchronized (fJobLock) {
      final Job oldJob = fJob;
      if (fJob != null) {
        fJob.cancel();
        fJob = null;
      }

      if (element != null) {
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
            DartUnit ast = DartToolsPlugin.getDefault().getASTProvider().getAST(
                element,
                ASTProvider.WAIT_YES,
                monitor);
            reconciled(ast, false, monitor);
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

    display.asyncExec(runnable);
  }
}

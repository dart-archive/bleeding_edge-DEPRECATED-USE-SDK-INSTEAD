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
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPartOfDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.DartElement;
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
import java.util.List;

/**
 * Semantic highlighting reconciler - Background thread implementation.
 */
public class SemanticHighlightingReconciler implements IDartReconcilingListener, ITextInputListener {

  /**
   * Collects positions from the AST.
   */
  private class PositionCollector extends ASTVisitor<Void> {

    //result holders cached and reused for performance
    private final SemanticToken<DartIdentifier> identifierToken = new SemanticToken<DartIdentifier>();

    //    private final SemanticToken<DartLiteral> literalToken = new SemanticToken<DartLiteral>();
    private final SemanticToken<DartDirective> directiveToken = new SemanticToken<DartDirective>();

    @Override
    public Void visitIdentifier(DartIdentifier node) {

      processNode(
          identifierToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          IDENTIFIER_TEST);

      return super.visitIdentifier(node);
    }

    @Override
    public Void visitImportDirective(DartImportDirective node) {

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          IMPORT_DIRECTIVE_TEST);

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          SHOW_CLAUSE_TEST);

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          HIDE_CLAUSE_TEST);

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          EXPORT_CLAUSE_TEST);

      return super.visitImportDirective(node);
    }

    @Override
    public Void visitLibraryDirective(DartLibraryDirective node) {

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          LIBRARY_DIRECTIVE_TEST);

      return super.visitLibraryDirective(node);
    }

    @Override
    public Void visitPartOfDirective(DartPartOfDirective node) {

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          PART_OF_DIRECTIVE_TEST);

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          OF_CLAUSE_TEST);

      return super.visitPartOfDirective(node);
    }

    @Override
    public Void visitSourceDirective(DartSourceDirective node) {

      processNode(
          directiveToken,
          node,
          SemanticHighlightingReconciler.this,
          PositionCollector.this,
          PART_DIRECTIVE_TEST);

      return super.visitSourceDirective(node);
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
      // TODO: use binary search
      for (int i = 0, n = fRemovedPositions.size(); i < n; i++) {
        HighlightedPosition position = (HighlightedPosition) fRemovedPositions.get(i);
        if (position == null) {
          continue;
        }
        if (position.isEqual(offset, length, highlighting)) {
          isExisting = true;
          fRemovedPositions.set(i, null);
          fNOfRemovedPositions--;
          break;
        }
      }

      if (!isExisting) {
        Position position = fJobPresenter.createHighlightedPosition(offset, length, highlighting);
        fAddedPositions.add(position);
      }
    }

//    /**
//     * Retain the positions completely contained in the given range.
//     * 
//     * @param offset The range offset
//     * @param length The range length
//     */
//    private void retainPositions(int offset, int length) {
//      // TODO: use binary search
//      for (int i = 0, n = fRemovedPositions.size(); i < n; i++) {
//        HighlightedPosition position = (HighlightedPosition) fRemovedPositions.get(i);
//        if (position != null && position.isContained(offset, length)) {
//          fRemovedPositions.set(i, null);
//          fNOfRemovedPositions--;
//        }
//      }
//    }

//    private boolean visitLiteralNode(DartExpression node) {
//      fToken.update(node);
//      for (int i = 0, n = fJobSemanticHighlightings.length; i < n; i++) {
//        SemanticHighlighting semanticHighlighting = fJobSemanticHighlightings[i];
//        if (fJobHighlightings[i].isEnabled() && semanticHighlighting.consumesLiteral(fToken)) {
//          int offset = node.getSourceInfo().getOffset();
//          int length = node.getSourceInfo().getLength();
//          if (offset > -1 && length > 0) {
//            addPosition(offset, length, fJobHighlightings[i]);
//          }
//          break;
//        }
//      }
//      fToken.clear();
//      return false;
//    }
  }

  /**
   * Tests if a highlighting consumes a given token.
   */
  private static interface Predicate<T extends DartNode> {
    boolean consumes(SemanticHighlighting h, SemanticToken<T> token);
  }

  private static final Predicate<DartDirective> LIBRARY_DIRECTIVE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesLibraryDirective(token);
    }
  };

  private static final Predicate<DartDirective> PART_OF_DIRECTIVE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesPartOfDirective(token);
    }
  };

  private static final Predicate<DartDirective> OF_CLAUSE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesOfClause(token);
    }
  };

  private static final Predicate<DartDirective> SHOW_CLAUSE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesShowClause(token);
    }
  };

  private static final Predicate<DartDirective> HIDE_CLAUSE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesHideClause(token);
    }
  };

  private static final Predicate<DartDirective> EXPORT_CLAUSE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesExportClause(token);
    }
  };

  private static final Predicate<DartDirective> PART_DIRECTIVE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesPartDirective(token);
    }
  };

  private static final Predicate<DartDirective> IMPORT_DIRECTIVE_TEST = new Predicate<DartDirective>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartDirective> token) {
      return h.consumesImportDirective(token);
    }
  };

  private static final Predicate<DartIdentifier> IDENTIFIER_TEST = new Predicate<DartIdentifier>() {
    @Override
    public boolean consumes(SemanticHighlighting h, SemanticToken<DartIdentifier> token) {
      return h.consumesIdentifier(token);
    }
  };

  private static <T extends DartNode> void processNode(SemanticToken<T> token, T node,
      SemanticHighlightingReconciler reconciler, PositionCollector collector, Predicate<T> tester) {
    token.update(node);
    token.attachSource(reconciler.fSourceViewer.getDocument());
    for (int i = 0, n = reconciler.fJobSemanticHighlightings.length; i < n; i++) {
      SemanticHighlighting semanticHighlighting = reconciler.fJobSemanticHighlightings[i];
      if (reconciler.fJobHighlightings[i].isEnabled()
          && tester.consumes(semanticHighlighting, token)) {
        int offset = semanticHighlighting.getSourceOffset(node);
        int length = semanticHighlighting.getSourceLength(node);
        if (offset > -1 && length > 0) {
          collector.addPosition(offset, length, reconciler.fJobHighlightings[i]);
        }
        break;
      }
    }
    token.clear();
  }

  /** Position collector */
  private PositionCollector fCollector = new PositionCollector();

  /** The Java editor this semantic highlighting reconciler is installed on */
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
  private List<Position> fAddedPositions = new ArrayList<Position>();
  /** Background job's removed highlighted positions */
  private List<Position> fRemovedPositions = new ArrayList<Position>();
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

      if (ast == null || fJobPresenter.isCanceled()) {
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

  /**
   * Reconcile positions based on the AST subtrees
   * 
   * @param subtrees the AST subtrees
   */
  private void reconcilePositions(DartNode[] subtrees) {
    // FIXME: remove positions not covered by subtrees
    for (int i = 0, n = subtrees.length; i < n; i++) {
      // subtrees[i].accept(fCollector);
      subtrees[i].accept(fCollector);
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

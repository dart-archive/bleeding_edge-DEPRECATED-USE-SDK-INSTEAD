/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.internal.text.dart.DartReconcilingStrategy;
import com.google.dart.tools.ui.internal.text.dart.IProblemRequestorExtension;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Reconciling strategy for Dart code. This is a composite strategy containing the regular model
 * reconciler and the comment spelling strategy.
 */
public class DartCompositeReconcilingStrategy extends CompositeReconcilingStrategy {

  private ITextEditor fEditor;
  private DartReconcilingStrategy dartStrategy;

  /**
   * Creates a new Java reconciling strategy.
   * 
   * @param viewer the source viewer
   * @param editor the editor of the strategy's reconciler
   * @param documentPartitioning the document partitioning this strategy uses for configuration
   */
  public DartCompositeReconcilingStrategy(ISourceViewer viewer, ITextEditor editor,
      String documentPartitioning) {
    fEditor = editor;
    dartStrategy = new DartReconcilingStrategy(editor);
    DartX.todo("spelling");
    setReconcilingStrategies(new IReconcilingStrategy[] {dartStrategy,
    // new JavaSpellingReconcileStrategy(viewer, EditorsUI.getSpellingService(),
    // DartPartitions.DART_PARTITIONING)
    });
  }

  /**
   * Called before reconciling is started.
   */
  public void aboutToBeReconciled() {
    dartStrategy.aboutToBeReconciled();

  }

  /*
   * @see org.eclipse.jface.text.reconciler.CompositeReconcilingStrategy#initialReconcile ()
   */
  @Override
  public void initialReconcile() {
    IProblemRequestorExtension e = getProblemRequestorExtension();
    if (e != null) {
      try {
        e.beginReportingSequence();
        super.initialReconcile();
      } finally {
        e.endReportingSequence();
      }
    } else {
      super.initialReconcile();
    }
  }

  /**
   * Tells this strategy whether to inform its listeners.
   * 
   * @param notify <code>true</code> if listeners should be notified
   */
  public void notifyListeners(boolean notify) {
    dartStrategy.notifyListeners(notify);
  }

  /*
   * @see org.eclipse.jface.text.reconciler.CompositeReconcilingStrategy#reconcile
   * (org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
   */
  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    IProblemRequestorExtension e = getProblemRequestorExtension();
    if (e != null) {
      try {
        e.beginReportingSequence();
        super.reconcile(dirtyRegion, subRegion);
      } finally {
        e.endReportingSequence();
      }
    } else {
      super.reconcile(dirtyRegion, subRegion);
    }
  }

  /*
   * @see org.eclipse.jface.text.reconciler.CompositeReconcilingStrategy#reconcile
   * (org.eclipse.jface.text.IRegion)
   */
  @Override
  public void reconcile(IRegion partition) {
    IProblemRequestorExtension e = getProblemRequestorExtension();
    if (e != null) {
      try {
        e.beginReportingSequence();
        super.reconcile(partition);
      } finally {
        e.endReportingSequence();
      }
    } else {
      super.reconcile(partition);
    }
  }

  /**
   * Returns the problem requestor for the editor's input element.
   * 
   * @return the problem requestor for the editor's input element
   */
  private IProblemRequestorExtension getProblemRequestorExtension() {
    IDocumentProvider p = fEditor.getDocumentProvider();
    if (p == null) {
      // work around for https://bugs.eclipse.org/bugs/show_bug.cgi?id=51522
      p = DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider();
    }
    IAnnotationModel m = p.getAnnotationModel(fEditor.getEditorInput());
    if (m instanceof IProblemRequestorExtension) {
      return (IProblemRequestorExtension) m;
    }
    return null;
  }
}

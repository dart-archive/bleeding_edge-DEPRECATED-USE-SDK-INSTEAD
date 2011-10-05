/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.WorkingCopyManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class DartReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

  private ITextEditor fEditor;

  private WorkingCopyManager fManager;
  private IDocumentProvider fDocumentProvider;
  private IProgressMonitor fProgressMonitor;
  private boolean fNotify = true;

  private IDartReconcilingListener fJavaReconcilingListener;
  private boolean fIsJavaReconcilingListener;

  public DartReconcilingStrategy(ITextEditor editor) {
    fEditor = editor;
    fManager = DartToolsPlugin.getDefault().getWorkingCopyManager();
    fDocumentProvider = DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider();
    fIsJavaReconcilingListener = fEditor instanceof IDartReconcilingListener;
    if (fIsJavaReconcilingListener) {
      fJavaReconcilingListener = (IDartReconcilingListener) fEditor;
    }
  }

  /**
   * Called before reconciling is started.
   */
  public void aboutToBeReconciled() {
    if (fIsJavaReconcilingListener) {
      fJavaReconcilingListener.aboutToBeReconciled();
    }
  }

  /*
   * @see IReconcilingStrategyExtension#initialReconcile()
   */
  @Override
  public void initialReconcile() {
    reconcile(true);
  }

  /**
   * Tells this strategy whether to inform its listeners.
   * 
   * @param notify <code>true</code> if listeners should be notified
   */
  public void notifyListeners(boolean notify) {
    fNotify = notify;
  }

  /*
   * @see IReconcilingStrategy#reconcile(DirtyRegion, IRegion)
   */
  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    reconcile(false);
  }

  /*
   * @see IReconcilingStrategy#reconcile(IRegion)
   */
  @Override
  public void reconcile(IRegion partition) {
    reconcile(false);
  }

  /*
   * @see IReconcilingStrategy#setDocument(IDocument)
   */
  @Override
  public void setDocument(IDocument document) {
  }

  /*
   * @see IReconcilingStrategyExtension#setProgressMonitor(IProgressMonitor)
   */
  @Override
  public void setProgressMonitor(IProgressMonitor monitor) {
    fProgressMonitor = monitor;
  }

  private IProblemRequestorExtension getProblemRequestorExtension() {
    IAnnotationModel model = fDocumentProvider.getAnnotationModel(fEditor.getEditorInput());
    if (model instanceof IProblemRequestorExtension) {
      return (IProblemRequestorExtension) model;
    }
    return null;
  }

  private void reconcile(final boolean initialReconcile) {
    //TODO (pquitslund): implement reconcile
//    final DartUnit[] ast = new DartUnit[1];
//    try {
//      final CompilationUnit unit = fManager.getWorkingCopy(
//          fEditor.getEditorInput(), false);
//      if (unit != null) {
//        SafeRunner.run(new ISafeRunnable() {
//          public void handleException(Throwable ex) {
//            IStatus status = new Status(IStatus.ERROR, DartUI.ID_PLUGIN,
//                IStatus.OK, "Error in Dart Core during reconcile", ex); //$NON-NLS-1$
//            DartToolsPlugin.getDefault().getLog().log(status);
//          }
//
//          public void run() {
//            try {
//
//              /* fix for missing cancel flag communication */
//              IProblemRequestorExtension extension = getProblemRequestorExtension();
//              if (extension != null) {
//                extension.setProgressMonitor(fProgressMonitor);
//                extension.setIsActive(true);
//              }
//
//              try {
//                boolean isASTNeeded = initialReconcile
//                    || DartToolsPlugin.getDefault().getASTProvider().isActive(
//                        unit);
//                // reconcile
//                if (fIsJavaReconcilingListener && isASTNeeded) {
//                  int reconcileFlags = CompilationUnit.FORCE_PROBLEM_DETECTION
//                      | (ASTProvider.SHARED_AST_STATEMENT_RECOVERY
//                          ? CompilationUnit.ENABLE_STATEMENTS_RECOVERY : 0)
//                      | (ASTProvider.SHARED_BINDING_RECOVERY
//                          ? CompilationUnit.ENABLE_BINDINGS_RECOVERY : 0);
//
//                  ast[0] = unit.reconcile(ASTProvider.SHARED_AST_LEVEL,
//                      reconcileFlags, null, fProgressMonitor);
//                  if (ast[0] != null) {
//                    // mark as unmodifiable
//                    ASTNodes.setFlagsToAST(ast[0], DartNode.PROTECT);
//                  }
//                } else
//                  unit.reconcile(CompilationUnit.NO_AST, true, null,
//                      fProgressMonitor);
//              } catch (OperationCanceledException ex) {
//                Assert.isTrue(fProgressMonitor == null
//                    || fProgressMonitor.isCanceled());
//                ast[0] = null;
//              } finally {
//                /* fix for missing cancel flag communication */
//                if (extension != null) {
//                  extension.setProgressMonitor(null);
//                  extension.setIsActive(false);
//                }
//              }
//
//            } catch (DartModelException ex) {
//              handleException(ex);
//            }
//          }
//        });
//
//      }
//    } finally {
//      // Always notify listeners, see
//      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=55969 for the final
//      // solution
//      try {
//        if (fIsJavaReconcilingListener) {
//          IProgressMonitor pm = fProgressMonitor;
//          if (pm == null)
//            pm = new NullProgressMonitor();
//          fJavaReconcilingListener.reconciled(ast[0], !fNotify, pm);
//        }
//      } finally {
//        fNotify = true;
//      }
//    }
  }
}

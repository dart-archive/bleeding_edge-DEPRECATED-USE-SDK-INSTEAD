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
package com.google.dart.tools.ui.internal.text.dart;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.ui.texteditor.ITextEditor;

public class DartReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

  @SuppressWarnings("unused")
  private final ITextEditor editor;

  public DartReconcilingStrategy(ITextEditor editor) {
    this.editor = editor;
  }

  @Override
  public void initialReconcile() {
    //TODO (pquitslund): implement new world reconciliation
  }

  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    //TODO (pquitslund): implement new world reconciliation
  }

  @Override
  public void reconcile(IRegion partition) {
    //TODO (pquitslund): implement new world reconciliation
  }

  @Override
  public void setDocument(IDocument document) {
    //TODO (pquitslund): implement new world reconciliation
  }

  @Override
  public void setProgressMonitor(IProgressMonitor monitor) {
    //TODO (pquitslund): implement new world reconciliation
  }

}

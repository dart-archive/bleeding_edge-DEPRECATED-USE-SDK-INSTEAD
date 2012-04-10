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

package com.google.dart.tools.ui.web.utils;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;

/**
 * This IReconcilingStrategy notifies the editor that a reconcile is occuring.
 */
public class WebEditorReconcilingStrategy implements IReconcilingStrategy {
  private WebEditor editor;

  /**
   * Create a new GoEditorReconcilingStrategy.
   * 
   * @param editor
   */
  public WebEditorReconcilingStrategy(WebEditor editor) {
    this.editor = editor;
  }

  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    // This really won't get called, as we indicate that we don't support incremental
    // reconciliation.
    editor.handleReconcilation(null);
  }

  @Override
  public void reconcile(IRegion partition) {
    editor.handleReconcilation(partition);
  }

  @Override
  public void setDocument(IDocument document) {

  }

}

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
package com.google.dart.tools.ui.internal.text.functions;

import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * "New world" dart reconciler.
 */
public class DartReconciler extends MonoReconciler {

  @SuppressWarnings("unused")
  private final ITextEditor editor;

  /**
   * Creates a new reconciler.
   * 
   * @param editor the editor
   * @param strategy the reconcile strategy
   * @param isIncremental <code>true</code> if this is an incremental reconciler
   */
  public DartReconciler(ITextEditor editor, DartCompositeReconcilingStrategy strategy,
      boolean isIncremental) {
    super(strategy, isIncremental);
    this.editor = editor;
    //TODO (pquitslund): implement new world reconciliation
  }

}

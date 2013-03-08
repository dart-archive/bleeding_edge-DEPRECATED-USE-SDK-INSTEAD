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

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Reconciling strategy for Dart code. This is a composite strategy containing the regular model
 * reconciler and the comment spelling strategy.
 */
public class DartCompositeReconcilingStrategy extends CompositeReconcilingStrategy {

  @SuppressWarnings("unused")
  private final ITextEditor editor;

  /**
   * Creates a new Dart reconciling strategy.
   * 
   * @param viewer the source viewer
   * @param editor the editor of the strategy's reconciler
   * @param documentPartitioning the document partitioning this strategy uses for configuration
   */
  public DartCompositeReconcilingStrategy(ISourceViewer viewer, ITextEditor editor,
      String documentPartitioning) {
    this.editor = editor;
    //TODO (pquitslund): implement new world reconciliation
  }

}

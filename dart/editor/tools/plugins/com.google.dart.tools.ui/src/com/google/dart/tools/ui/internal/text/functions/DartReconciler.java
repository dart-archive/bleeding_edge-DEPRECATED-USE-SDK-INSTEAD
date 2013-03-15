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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * "New world" dart reconciler.
 */
public class DartReconciler extends MonoReconciler {
  private final DartEditor editor;
  private volatile Thread thread;

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
    this.editor = editor instanceof DartEditor ? (DartEditor) editor : null;
  }

  @Override
  public void install(ITextViewer textViewer) {
    super.install(textViewer);
    if (editor != null) {
      thread = new Thread() {
        @Override
        public void run() {
          refreshLoop();
        }
      };
      thread.setDaemon(true);
      thread.start();
    }
  }

  @Override
  public void uninstall() {
    super.uninstall();
    thread = null;
  }

  @Override
  protected void process(DirtyRegion dirtyRegion) {
    super.process(dirtyRegion);
    if (editor != null) {
      DartEditor dartEditor = editor;
      dartEditor.applyChangesToContext();
    }
  }

  /**
   * Performs main refresh loop to reflect changes in {@link DartEditor} and/or environment.
   */
  private void refreshLoop() {
    CompilationUnitElement previousUnitElement = null;
    while (thread != null) {
      try {
        CompilationUnit unitNode = editor.getInputUnit();
        CompilationUnitElement unitElement = unitNode.getElement();
        if (unitElement != previousUnitElement) {
          previousUnitElement = unitElement;
//          System.out.println("unitElement: " + ObjectUtils.identityToString(unitElement));
          // unit was resolved
          if (unitElement != null) {
            editor.applyCompilationUnitElement(unitNode);
          }
        }
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    }
  }
}

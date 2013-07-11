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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

public class DartReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

  /**
   * The editor containing the document with source to be reconciled (not {@code null}).
   */
  // TODO (danrubel): Replace with interface so that html editor can use this strategy
  private final DartEditor editor;

  /**
   * The source being edited.
   */
  private Source source;

  /**
   * The document with source to be reconciled. This is set by the {@link IReconciler} and should
   * not be {@code null}.
   */
  private IDocument document;

  /**
   * The time when the context was last notified of a source change.
   */
  private volatile long notifyContextTime;

  /**
   * The time when the source was last modified.
   */
  private volatile long sourceChangedTime;

  /**
   * Listen for analysis results for the source being edited and update the editor.
   */
  private final AnalysisListener analysisListener = new AnalysisListener() {

    @Override
    public void complete(AnalysisEvent event) {
      AnalysisContext context = editor.getInputAnalysisContext();
      if (event.getContext().equals(context)) {
        Source source = editor.getInputSource();
        Source[] libraries = context.getLibrariesContaining(source);
        if (libraries != null && libraries.length > 0) {
          // TODO (danrubel): Handle multiple libraries gracefully
          CompilationUnit unit = context.getResolvedCompilationUnit(source, libraries[0]);
          if (unit != null) {
            applyAnalysisResult(unit);
          }
        }
      }
    }

    @Override
    public void resolved(ResolvedEvent event) {
      if (event.getContext().equals(editor.getInputAnalysisContext())) {
        if (event.getSource().equals(editor.getInputSource())) {
          applyAnalysisResult(event.getUnit());
        }
      }
    }
  };

  /**
   * Listen for changes to the source and record the last modification time.
   */
  final IDocumentListener documentListener = new IDocumentListener() {
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    @Override
    public void documentChanged(DocumentEvent event) {
      sourceChangedTime = System.currentTimeMillis();
    }
  };

  /**
   * Construct a new instance for the specified editor.
   * 
   * @param editor the editor (not {@code null})
   */
  public DartReconcilingStrategy(DartEditor editor) {
    this.editor = editor;
    this.source = editor.getInputSource();

    // Cleanup the receiver when editor is closed
    editor.getViewer().getTextWidget().addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        dispose();
      }
    });

    AnalysisWorker.addListener(analysisListener);
  }

  @Override
  public void initialReconcile() {
    notifyContext(document.get());
    try {
      CompilationUnit unit = editor.getInputAnalysisContext().parseCompilationUnit(source);
      editor.applyCompilationUnitElement(unit);
    } catch (AnalysisException e) {
      DartCore.logError("Parse failed for " + editor.getTitle(), e);
    }
  }

  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    notifyContext(document.get());
  }

  @Override
  public void reconcile(IRegion partition) {
    notifyContext(document.get());
  }

  /**
   * Cache the document and add document changed and analysis result listeners.
   */
  @Override
  public void setDocument(final IDocument document) {
    if (this.document != null) {
      document.removeDocumentListener(documentListener);
    }
    this.document = document;
    document.addDocumentListener(documentListener);
  }

  @Override
  public void setProgressMonitor(IProgressMonitor monitor) {
  }

  /**
   * Apply analysis results only if there are no pending source changes.
   */
  private void applyAnalysisResult(CompilationUnit unit) {
    if (notifyContextTime > sourceChangedTime && unit != null) {
      editor.applyCompilationUnitElement(unit);
    }
  }

  /**
   * Cleanup when the editor is closed.
   */
  private void dispose() {
    // clear the cached source content so that the source will be read from disk
    notifyContext(null);
    document.removeDocumentListener(documentListener);
    AnalysisWorker.removeListener(analysisListener);
  }

  /**
   * Notify the underlying analysis context that a source change has occurred, and record the time
   * at which it occurred.
   */
  private void notifyContext(String code) {
    AnalysisContext context = editor.getInputAnalysisContext();
    ContextManager manager = editor.getInputProject();
    if (manager == null) {
      manager = DartCore.getProjectManager();
    }
    notifyContextTime = System.currentTimeMillis();
    context.setContents(source, code);
    AnalysisWorker.performAnalysisInBackground(manager, context);
  }
}

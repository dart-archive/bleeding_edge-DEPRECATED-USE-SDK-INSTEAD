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

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.server.ContentChange;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.analysis.model.ResolvedHtmlEvent;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

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

import java.io.IOException;
import java.util.Map;

public class DartReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

  /**
   * The editor containing the document with source to be reconciled (not {@code null}).
   */
  private final DartReconcilingEditor editor;

  /**
   * The manager used by the receiver to request background analysis (not {@code null}).
   */
  private final AnalysisManager analysisManager;

  /**
   * The ignore manager used by the receiver to determine if a source should be analyzed.
   */
  private final DartIgnoreManager ignoreManager;

  /**
   * The document with source to be reconciled. This is set by the {@link IReconciler} and should
   * not be {@code null}.
   */
  private IDocument document;

  /**
   * Synchronize against this field before accessing {@link #dirtyRegion}
   */
  private final Object lock = new Object();

  /**
   * The region of source that has changed and needs to be reconciled, or empty if analysis of this
   * file is up to date, or {@code null} if the entire file needs to be reconciled. Synchronize
   * against {@link #lock} before accessing this field.
   */
  private DartReconcilingRegion dirtyRegion = DartReconcilingRegion.EMPTY;

  /**
   * Listen for analysis results for the source being edited and update the editor.
   */
  private final AnalysisListener analysisListener = new AnalysisListener() {

    @Override
    public void complete(AnalysisEvent event) {
      AnalysisContext context = editor.getInputAnalysisContext();
      if (event.getContext().equals(context)) {
        applyResolvedUnit();
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

    @Override
    public void resolvedHtml(ResolvedHtmlEvent event) {
      // ignored
    }
  };

  /**
   * Listen for changes to the source to clear the cached AST and record the last modification time.
   */
  private final IDocumentListener documentListener = new IDocumentListener() {
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    @Override
    public void documentChanged(DocumentEvent event) {

      // Record the source region that has changed
      String newText = event.getText();
      int newLength = newText != null ? newText.length() : 0;
      synchronized (lock) {
        if (dirtyRegion != null) {
          dirtyRegion = dirtyRegion.add(event.getOffset(), event.getLength(), newLength);
        }
      }
      editor.applyResolvedUnit(null);

      // Start analysis immediately if "." pressed to improve code completion response
      if (newText.endsWith(".")) {
        reconcile();
      }
    }
  };

  /**
   * Construct a new instance for the specified editor.
   * 
   * @param editor the editor (not {@code null})
   */
  public DartReconcilingStrategy(DartReconcilingEditor editor) {
    this(editor, AnalysisManager.getInstance(), DartCore.getProjectManager().getIgnoreManager());
  }

  /**
   * Construct a new instance for the specified editor.
   * 
   * @param editor the editor (not {@code null})
   * @param analysisManager the analysis manager (not {@code null})
   * @param ignoreManager the ignore manager (not {@code null})
   */
  public DartReconcilingStrategy(DartReconcilingEditor editor, AnalysisManager analysisManager,
      DartIgnoreManager ignoreManager) {
    this.editor = editor;
    this.analysisManager = analysisManager;
    this.ignoreManager = ignoreManager;
    editor.setDartReconcilingStrategy(this);

    // Cleanup the receiver when editor is closed
    editor.addViewerDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        dispose();
      }
    });

    AnalysisWorker.addListener(analysisListener);
  }

  /**
   * Cleanup when the editor is closed.
   */
  public void dispose() {
    // clear the cached source content to ensure the source will be read from disk
    if (document != null) {
      document.removeDocumentListener(documentListener);
    }
    AnalysisWorker.removeListener(analysisListener);
    sourceChanged(null);
  }

  @Override
  public void initialReconcile() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      return;
    }
    if (!applyResolvedUnit()) {
      Source source = editor.getInputSource();
      if (ignoreManager.isAnalyzed(source.getFullName())) {
        try {
          AnalysisContext context = editor.getInputAnalysisContext();
          if (context != null && source != null) {
            // TODO (danrubel): Push this into background analysis
            // once AnalysisWorker notifies listeners when units are parsed before resolved
            CompilationUnit unit = context.parseCompilationUnit(source);
            editor.applyResolvedUnit(unit);
            performAnalysisInBackground();
          }
        } catch (AnalysisException e) {
          if (!(e.getCause() instanceof IOException)) {
            DartCore.logError("Parse failed for " + editor.getTitle(), e);
          }
        }
      }
    }
  }

  /**
   * Activates reconciling of the current dirty region.
   */
  public void reconcile() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("DartReconcilingStrategy-reconcile");
    try {
      instrumentation.data("Name", editor.getTitle());
      DartReconcilingRegion region;
      synchronized (lock) {
        region = dirtyRegion;
        dirtyRegion = DartReconcilingRegion.EMPTY;
      }
      if (region == null) {
        String code = document.get();
        instrumentation.data("Length", code.length());
        sourceChanged(code);
      } else if (!region.isEmpty()) {
        instrumentation.data("Offset", region.getOffset());
        instrumentation.data("OldLength", region.getOldLength());
        instrumentation.data("NewLength", region.getNewLength());
        sourceChanged(
            document.get(),
            region.getOffset(),
            region.getOldLength(),
            region.getNewLength());
      }
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    reconcile();
  }

  @Override
  public void reconcile(IRegion partition) {
    reconcile();
  }

  /**
   * Cache the document and add document changed and analysis result listeners.
   */
  @Override
  public void setDocument(IDocument document) {
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
    if (unit == null) {
      return;
    }
    synchronized (lock) {
      if (dirtyRegion == null || !dirtyRegion.isEmpty()) {
        return;
      }
    }
    editor.applyResolvedUnit(unit);
  }

  /**
   * Get the resolved compilation unit from the editor's analysis context and apply that unit.
   * 
   * @return {@code true} if a resolved unit was obtained and applied
   */
  private boolean applyResolvedUnit() {
    AnalysisContext context = editor.getInputAnalysisContext();
    Source source = editor.getInputSource();
    if (context != null && source != null) {
      Source[] libraries = context.getLibrariesContaining(source);
      if (libraries != null && libraries.length > 0) {
        // TODO (danrubel): Handle multiple libraries gracefully
        CompilationUnit unit = context.getResolvedCompilationUnit(source, libraries[0]);
        if (unit != null) {
          applyAnalysisResult(unit);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Return the {@link ContextManager} to use for this editor.
   */
  private ContextManager getContextManager() {
    ContextManager manager = editor.getInputProject();
    if (manager == null) {
      manager = DartCore.getProjectManager();
    }
    return manager;
  }

  /**
   * Start background analysis of the context containing the source being edited.
   */
  private void performAnalysisInBackground() {
    AnalysisContext context = editor.getInputAnalysisContext();
    if (context != null) {
      ContextManager manager = getContextManager();
      analysisManager.performAnalysisInBackground(manager, context);
    }
  }

  /**
   * Schedules the source change notification and analysis.
   * 
   * @param code the new source code or {@code null} if the source should be pulled from disk
   */
  private void sourceChanged(String code) {
    Source source = editor.getInputSource();
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      String file = editor.getInputFilePath();
      if (file != null) {
        ContentChange change = new ContentChange(code);
        Map<String, ContentChange> files = ImmutableMap.of(file, change);
        // TODO (jwren) re-implement after analysis_updateContent is working again
//        DartCore.getAnalysisServer().analysis_updateContent(files);
      }
    } else {
      AnalysisContext context = editor.getInputAnalysisContext();
      if (context != null && source != null) {
        ContextManager manager = getContextManager();
        DartUpdateSourceHelper.getInstance().updateFast(
            analysisManager,
            manager,
            context,
            source,
            code);
      }
    }
  }

  /**
   * Schedules the source change notification and analysis.
   * 
   * @param code the new source code or {@code null} if the source should be pulled from disk
   * @param offset the offset into the current contents
   * @param oldLength the number of characters in the original contents that were replaced
   * @param newLength the number of characters in the replacement text
   */
  private void sourceChanged(String code, int offset, int oldLength, int newLength) {
    Source source = editor.getInputSource();
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      String file = editor.getInputFilePath();
      if (file != null) {
        ContentChange change = new ContentChange(code, offset, oldLength, newLength);
        Map<String, ContentChange> files = ImmutableMap.of(file, change);
        // TODO (jwren) re-implement after analysis_updateContent is working again
//        DartCore.getAnalysisServer().analysis_updateContent(files);
      }
    } else {
      AnalysisContext context = editor.getInputAnalysisContext();
      if (context != null && source != null) {
        ContextManager manager = getContextManager();
        DartUpdateSourceHelper.getInstance().updateFast(
            analysisManager,
            manager,
            context,
            source,
            code,
            offset,
            oldLength,
            newLength);
      }
    }
  }
}

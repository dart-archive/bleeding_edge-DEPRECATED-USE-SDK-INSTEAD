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

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.server.UpdateContentConsumer;
import com.google.dart.server.generated.types.AddContentOverlay;
import com.google.dart.server.generated.types.ChangeContentOverlay;
import com.google.dart.server.generated.types.RemoveContentOverlay;
import com.google.dart.server.generated.types.SourceEdit;
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
import org.eclipse.jface.text.DocumentRewriteSessionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentRewriteSessionListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
   * The contents of the document the last time it was updated, or null if this file is up to date.
   * Synchronize against {@link #lock} before accessing this field.
   */
  private String codeAsOfLastUpdate = null;

  /**
   * A flag indicating whether an "overlay" has been already added for this file.
   */
  private boolean isOverlayAdded = false;

  /**
   * A flag indicating whether there are changes in {@link #document} that have not yet been sent to
   * the analysis.
   */
  private boolean hasPendingDocumentChanges = false;

  /**
   * A counter of requests and responses to 'analysis.updateContent'. When it is {@code 0}, then
   * every request had a response so far.
   */
  private final AtomicInteger updateContentBalance = new AtomicInteger();

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
      if (!editor.isDirty()) {
        return;
      }
      hasPendingDocumentChanges = true;

      // Record the source region that has changed
      String newText = event.getText();
      int newLength = newText != null ? newText.length() : 0;
      synchronized (lock) {
        if (dirtyRegion != null) {
          dirtyRegion = dirtyRegion.add(event.getOffset(), event.getLength(), newLength);
        }
        codeAsOfLastUpdate = document.get();
      }
      editor.applyResolvedUnit(null);

      // Start analysis immediately if "." pressed to improve code completion response
      if (newText.endsWith(".")) {
        reconcile();
      }
    }
  };

  /**
   * Reconciler usually waits for some time, like 500 ms, before actually performing reconciliation,
   * so that it happens when user stops typing. But when we apply a Quick Fix/Assist or a
   * refactoring, we should apply changes as soon as possible, because these changes form their own
   * complete unit of work.
   */
  private final IDocumentRewriteSessionListener rewriteSessionListener = new IDocumentRewriteSessionListener() {
    @Override
    public void documentRewriteSessionChanged(DocumentRewriteSessionEvent event) {
      if (event.getChangeType() == DocumentRewriteSessionEvent.SESSION_STOP) {
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
    this(editor, AnalysisManager.getInstance(), null);
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

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      this.editor = editor;
      this.analysisManager = null;
      this.ignoreManager = DartIgnoreManager.getInstance();
    } else {
      this.editor = editor;
      this.analysisManager = analysisManager;
      if (ignoreManager == null) {
        this.ignoreManager = DartCore.getIgnoreManager();
      } else {
        this.ignoreManager = ignoreManager;
      }
      AnalysisWorker.addListener(analysisListener);
    }
    editor.setDartReconcilingStrategy(this);

    // Cleanup the receiver when editor is closed
    editor.addViewerDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        dispose();
      }
    });
  }

  /**
   * Cleanup when the editor is closed.
   */
  public void dispose() {
    if (document != null) {
      document.removeDocumentListener(documentListener);
      if (document instanceof IDocumentExtension4) {
        IDocumentExtension4 document4 = (IDocumentExtension4) document;
        document4.removeDocumentRewriteSessionListener(rewriteSessionListener);
      }
    }
    AnalysisWorker.removeListener(analysisListener);
    // clear the cached source content to ensure the source will be read from disk
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      removeOverlay();
    } else {
      sourceChanged(null);
    }
  }

  /**
   * Return {@code true} if there are pending document changes that have not been sent to the server
   * yet or if there is a request to which the server has not responded yet.
   */
  public boolean hasPendingContentChanges() {
    return hasPendingDocumentChanges || updateContentBalance.get() != 0;
  }

  @Override
  public void initialReconcile() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      return;
    }
    if (!applyResolvedUnit()) {
      Source source = editor.getInputSource();
      if (source != null && ignoreManager.isAnalyzed(source.getFullName())) {
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
      String code;
      synchronized (lock) {
        region = dirtyRegion;
        code = codeAsOfLastUpdate;
        dirtyRegion = DartReconcilingRegion.EMPTY;
        codeAsOfLastUpdate = null;
      }
      if (region == null) {
        instrumentation.data("Length", code.length());
        sourceChanged(code);
      } else if (!region.isEmpty()) {
        instrumentation.data("Offset", region.getOffset());
        instrumentation.data("OldLength", region.getOldLength());
        instrumentation.data("NewLength", region.getNewLength());
        sourceChanged(code, region.getOffset(), region.getOldLength(), region.getNewLength());
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

  public void saved() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      hasPendingDocumentChanges = false;
      reconcile();
      removeOverlay();
    } else {
      // We don't use overlays with the Java based analyzer.
    }
  }

  /**
   * Cache the document and add document changed and analysis result listeners.
   */
  @Override
  public void setDocument(IDocument document) {
    IDocument oldDocument = this.document;
    if (oldDocument != null) {
      oldDocument.removeDocumentListener(documentListener);
      if (oldDocument instanceof IDocumentExtension4) {
        IDocumentExtension4 oldDocument4 = (IDocumentExtension4) oldDocument;
        oldDocument4.removeDocumentRewriteSessionListener(rewriteSessionListener);
      }
    }
    this.document = document;
    document.addDocumentListener(documentListener);
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (document instanceof IDocumentExtension4) {
        IDocumentExtension4 document4 = (IDocumentExtension4) document;
        document4.addDocumentRewriteSessionListener(rewriteSessionListener);
      }
    }
  }

  @Override
  public void setProgressMonitor(IProgressMonitor monitor) {
  }

  /**
   * Adds overlay for this file.
   */
  private void addOverlay(String code) {
    AddContentOverlay change = new AddContentOverlay(code);
    updateFileContent(change);
    isOverlayAdded = true;
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

  private void removeOverlay() {
    if (isOverlayAdded) {
      RemoveContentOverlay change = new RemoveContentOverlay();
      updateFileContent(change);
      isOverlayAdded = false;
    }
  }

  /**
   * Schedules the source change notification and analysis.
   * 
   * @param code the new source code or {@code null} if the source should be pulled from disk. Will
   *          never be {@code null} when analysis server is in use.
   */
  private void sourceChanged(String code) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (!isOverlayAdded) {
        addOverlay(code);
      } else {
        AddContentOverlay change = new AddContentOverlay(code);
        updateFileContent(change);
      }
    } else {
      AnalysisContext context = editor.getInputAnalysisContext();
      Source source = editor.getInputSource();
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
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (editor.isDirty()) {
        if (!isOverlayAdded) {
          addOverlay(code);
        } else {
          List<SourceEdit> sourceEdits = Lists.newArrayList();
          String replacement = code.substring(offset, offset + newLength);
          sourceEdits.add(new SourceEdit(offset, oldLength, replacement, null));
          ChangeContentOverlay change = new ChangeContentOverlay(sourceEdits);
          updateFileContent(change);
        }
      } else {
        removeOverlay();
      }
    } else {
      AnalysisContext context = editor.getInputAnalysisContext();
      Source source = editor.getInputSource();
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

  private void updateFileContent(Object change) {
    String file = editor.getInputFilePath();
    if (file != null) {
      updateContentBalance.incrementAndGet();
      hasPendingDocumentChanges = false;
      Map<String, Object> files = new HashMap<String, Object>();
      files.put(file, change);
      DartCore.getAnalysisServer().analysis_updateContent(files, new UpdateContentConsumer() {
        @Override
        public void onResponse() {
          updateContentBalance.decrementAndGet();
        }
      });
    }
  }
}

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
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.ui.instrumentation.util.Base64;
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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DartReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

  /**
   * The editor containing the document with source to be reconciled (not {@code null}).
   */
  // TODO (danrubel): Replace with interface so that html editor can use this strategy
  private final DartReconcilingEditor editor;

  /**
   * The document with source to be reconciled. This is set by the {@link IReconciler} and should
   * not be {@code null}.
   */
  private IDocument document;

  /**
   * The display in which the editor is visible.
   */
  private final Display display;

  /**
   * The time when the context was last notified of a source change.
   */
  private volatile long notifyContextTime;

  /**
   * The time when the source was last modified.
   */
  private volatile long sourceChangedTime;

  /**
   * Flag indicating that a document change has occurred, but analysis context has not been
   * notified. This is used to prevent a flurry rapid fire of document changes from triggering a
   * flurry of delayed analysis requests.
   */
  private volatile boolean analysisNeeded = false;

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
      sourceChangedTime = System.currentTimeMillis();
      editor.applyCompilationUnitElement(null);

      // Start analysis immediately if "." pressed to improve code completion response

      // This may need to be modified or removed 
      // if we enable/set content assist immediate-activation character(s)

      if (".".equals(event.getText())) {
        updateSourceInBackground(true);
      } else {
        analysisNeeded = true;
      }
    }
  };

  /**
   * Construct a new instance for the specified editor.
   * 
   * @param editor the editor (not {@code null})
   */
  public DartReconcilingStrategy(DartReconcilingEditor editor) {
    this.editor = editor;
    this.display = Display.getDefault();

    // Prioritize analysis when editor becomes active
    editor.addViewerFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        updateAnalysisPriorityOrder(true);
      }

      @Override
      public void focusLost(FocusEvent e) {
      }
    });

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
    updateSourceInBackground(false);
    document.removeDocumentListener(documentListener);
    AnalysisWorker.removeListener(analysisListener);
    updateAnalysisPriorityOrder(false);
  }

  @Override
  public void initialReconcile() {
    updateAnalysisPriorityOrder(true);
    if (!applyResolvedUnit()) {
      try {
        AnalysisContext context = editor.getInputAnalysisContext();
        Source source = editor.getInputSource();
        if (context != null && source != null) {
          CompilationUnit unit = context.parseCompilationUnit(source);
          editor.applyCompilationUnitElement(unit);
          performAnalysisInBackground();
        }
      } catch (AnalysisException e) {
        if (!(e.getCause() instanceof IOException)) {
          DartCore.logError("Parse failed for " + editor.getTitle(), e);
        }
      }
    }
  }

  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("DartReconcilingStrategy-reconcile");
    try {
      instrumentation.data("Name", editor.getTitle());
      instrumentation.data("Offset", dirtyRegion.getOffset());
      instrumentation.data("Length", dirtyRegion.getLength());
      String newText = dirtyRegion.getText();
      if (newText != null) {
        instrumentation.data("NewText", Base64.encodeBytes(newText.getBytes()));
      } else {
        // region was deleted
      }
      if (analysisNeeded) {
        analysisNeeded = false;
        sourceChanged(document.get());
        performAnalysisInBackground();
      } else {
        instrumentation.data("AnalysisNeeded", false);
      }
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void reconcile(IRegion partition) {
    sourceChanged(document.get());
    performAnalysisInBackground();
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
    if (notifyContextTime >= sourceChangedTime && unit != null) {
      editor.applyCompilationUnitElement(unit);
    }
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
   * Answer the list of sources associated with the specified context that are open and visible.
   * This method blocks until the information can be retrieved on the UI thread.
   * 
   * @param context the context (not {@code null})
   * @return a list of sources (not {@code null}, contains no {@code null}s)
   */
  private List<Source> getVisibleSourcesForContext(final AnalysisContext context) {
    final ArrayList<Source> sources = new ArrayList<Source>();
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
          for (IWorkbenchPage page : window.getPages()) {
            IEditorReference[] allEditors = page.getEditorReferences();
            for (IEditorReference editorRef : allEditors) {
              IEditorPart part = editorRef.getEditor(false);
              if (part instanceof DartEditor) {
                DartEditor otherEditor = (DartEditor) part;
                if (otherEditor.getInputAnalysisContext() == context && otherEditor.isVisible()) {
                  Source otherSource = otherEditor.getInputSource();
                  if (otherSource != null) {
                    sources.add(otherSource);
                  }
                }
              }
            }
          }
        }
      }
    });
    return sources;
  }

  /**
   * Start background analysis of the context containing the source being edited.
   */
  private void performAnalysisInBackground() {
    AnalysisContext context = editor.getInputAnalysisContext();
    if (context != null) {
      ContextManager manager = editor.getInputProject();
      if (manager == null) {
        manager = DartCore.getProjectManager();
      }
      AnalysisWorker.performAnalysisInBackground(manager, context);
    }
  }

  /**
   * Clear the cached compilation unit and notify the context that the source has changed.
   * 
   * @param code the new source code or {@code null} if the source should be pulled from disk
   */
  private void sourceChanged(String code) {
    notifyContextTime = System.currentTimeMillis();
    AnalysisContext context = editor.getInputAnalysisContext();
    Source source = editor.getInputSource();
    if (context != null && source != null) {
      context.setContents(source, code);
    }
  }

  /**
   * Update the order in which sources are analyzed in the context associated with the editor. This
   * is called once per instantiated editor on startup and then once for each editor as it becomes
   * active. For example, if there are 2 of 7 editors visible on startup, then this will be called
   * for the 2 visible editors.
   * 
   * @param isOpen {@code true} if the editor is open and the source should be the first source
   *          analyzed or {@code false} if the editor is closed and the source should be removed
   *          from the priority list.
   */
  private void updateAnalysisPriorityOrder(final boolean isOpen) {
    // TODO (danrubel): Revisit when reviewing performance of startup, open, and activate
    AnalysisContext context = editor.getInputAnalysisContext();
    Source source = editor.getInputSource();
    if (context != null && source != null) {
      final List<Source> sources = getVisibleSourcesForContext(context);
      sources.remove(source);
      if (isOpen) {
        sources.add(0, source);
      }
      // TODO (danrubel): Keep this off the UI thread until lock contention has been resolved
      if (Display.getCurrent() == null) {
        context.setAnalysisPriorityOrder(sources);
      } else {
        new Thread("updateAnalysisPriorityOrder") {
          @Override
          public void run() {
            AnalysisContext context = editor.getInputAnalysisContext();
            if (context != null) {
              context.setAnalysisPriorityOrder(sources);
            }
          };
        }.start();
      }
    }
  }

  /**
   * Update the source being analyzed in the context associated with the editor.
   * 
   * @param isOpen {@code true} if the editor is open and the source should be cached or
   *          {@code false} if the editor is closed and the source should read from disk.
   */
  private void updateSourceInBackground(final boolean isOpen) {
    // TODO (danrubel): Keep this off the UI thread until lock contention has been resolved
    new Thread("updateSourceInBackground") {
      @Override
      public void run() {
        sourceChanged(isOpen ? document.get() : null);
        performAnalysisInBackground();
      }
    }.start();
  }
}

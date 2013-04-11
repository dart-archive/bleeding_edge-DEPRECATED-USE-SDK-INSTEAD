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

import com.google.common.base.Objects;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import java.lang.ref.WeakReference;

/**
 * "New world" dart reconciler.
 */
public class DartReconciler extends MonoReconciler {
  private static class EditorState {
    private final long time = System.currentTimeMillis();
    private final String code;
    private final Point selectionRange;

    public EditorState(String code, Point selectionRange) {
      this.code = code;
      this.selectionRange = selectionRange;
    }
  }

  private class Listener implements IDocumentListener, ITextInputListener,
      ISelectionChangedListener {

    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    @Override
    public void documentChanged(DocumentEvent event) {
      putEditorState(true);
    }

    @Override
    public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    }

    @Override
    public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
      if (oldInput != null) {
        oldInput.removeDocumentListener(this);
      }
      if (newInput != null) {
        newInput.addDocumentListener(this);
      }
      if (oldInput != null) {
        putEditorState(true);
      }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      putEditorState(false);
    }
  }

  private static final String UNCHANGED_CODE = new String();

  private final DartEditor editor;
  private final IFile file;
  private final Project project;
  private volatile Thread thread;

  private final Object editorStateLock = new Object();
  private EditorState editorState;
  private EditorState loopEditorState;
  private String oldCode = UNCHANGED_CODE;
  private final Listener documentListener = new Listener();

  private Boolean lastReadOnly = null;

  public DartReconciler(ITextEditor editor, DartCompositeReconcilingStrategy strategy) {
    super(strategy, false);
    this.editor = editor instanceof DartEditor ? (DartEditor) editor : null;
    this.file = this.editor != null ? this.editor.getInputResourceFile() : null;
    if (this.editor != null) {
      this.project = this.editor.getInputProject();
      this.editor.setReconciler(this);
    } else {
      this.project = null;
    }
  }

  /**
   * @return <code>true</code> if there are changes to be applied, so {@link DartReconciler} will
   *         change resolved {@link CompilationUnit} in {@link DartEditor}.
   */
  public boolean hasPendingUnitChanges() {
    synchronized (editorStateLock) {
      if (loopEditorState != null) {
        return true;
      }
      return editorState != null && !Objects.equal(editorState.code, oldCode);
    }
  }

  @Override
  public void install(ITextViewer textViewer) {
    super.install(textViewer);
    if (editor != null) {
      putEditorState(false);
      // add listener
      {
        IPostSelectionProvider provider = (IPostSelectionProvider) editor.getSelectionProvider();
        provider.addPostSelectionChangedListener(documentListener);
        getTextViewer().addTextInputListener(documentListener);
      }
      // start thread
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
    // this editor was closed, reset content
    notifyContextAboutCode(null);
    // remove listeners
    {
      IPostSelectionProvider provider = (IPostSelectionProvider) editor.getSelectionProvider();
      if (provider != null) {
        provider.removePostSelectionChangedListener(documentListener);
      }
      getTextViewer().removeTextInputListener(documentListener);
    }
    // notify thread that it should be stopped
    thread = null;
  }

  /**
   * Asynchronously notify {@link DartEditor} about parsed {@link CompilationUnit} and selection.
   */
  private void displayReconcilerTick(final CompilationUnit unit, final boolean newUnit,
      final Point selectionRange) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        editor.applyParsedUnitAndSelection(unit, newUnit, selectionRange);
      }
    });
  }

  /**
   * @return the {@link AnalysisContext} which corresponds to the {@link IEditorInput}.
   */
  private AnalysisContext getContext() {
    return editor.getInputAnalysisContext();
  }

  /**
   * @return the parsed {@link CompilationUnit}, may be <code>null</code>/
   */
  private CompilationUnit getParsedUnit() throws AnalysisException {
    Source source = getSource();
    AnalysisContext context = getContext();
    if (source == null || context == null) {
      return null;
    }
    // parse
    return context.parseCompilationUnit(source);
  }

  /**
   * @return the resolved {@link CompilationUnit}, may be <code>null</code>/
   */
  private CompilationUnit getResolvedUnit(boolean forceResolve) throws Exception {
    Source source = getSource();
    AnalysisContext context = getContext();
    if (source == null || context == null) {
      return null;
    }
    // resolve
    Source[] librarySources = context.getLibrariesContaining(source);
    if (librarySources.length != 0) {
      if (forceResolve) {
        return context.resolveCompilationUnit(source, librarySources[0]);
      } else {
        return context.getResolvedCompilationUnit(source, librarySources[0]);
      }
    }
    return null;
  }

  /**
   * @return the {@link Source} which corresponds to the {@link IEditorInput}.
   */
  private Source getSource() {
    return editor.getInputSource();
  }

  /**
   * Notifies {@link AnalysisContext} that {@link Source} was changed.
   */
  private void notifyContextAboutCode(String code) {
    if (project == null) {
      return;
    }
    // only if changed
    if (oldCode == UNCHANGED_CODE) {
      oldCode = code;
    }
    if (Objects.equal(code, oldCode)) {
      return;
    }
    oldCode = code;
    // prepare Source
    Source source = getSource();
    AnalysisContext context = getContext();
    if (source == null || context == null) {
      return;
    }
    // notify AnalysisContext about change
    context.setContents(source, code);
    // schedule re-analyzing
    new AnalysisWorker(project, context).performAnalysisInBackground();
  }

  /**
   * Fills {@link #editorState} with current state (text and selection) of editor.
   */
  private void putEditorState(boolean clearUnitElement) {
    IDocument document = getDocument();
    ITextViewer textViewer = getTextViewer();
    if (document != null && textViewer != null) {
      try {
        String code = document.get();
        Point selectionRange = textViewer.getSelectedRange();
        synchronized (editorStateLock) {
          editorState = new EditorState(code, selectionRange);
          // notify editor that CompilationUnit is not valid anymore
          if (clearUnitElement) {
            editor.applyCompilationUnitElement(null);
          }
        }
      } catch (Throwable e) {
      }
    }
  }

  /**
   * Performs main refresh loop to reflect changes in {@link DartEditor} and/or environment.
   */
  private void refreshLoop() {
    // change Thread name
    {
      Source source = getSource();
      if (source != null) {
        Thread.currentThread().setName("Reconciler: " + source.getShortName());
      }
    }
    // schedule initial resolution
    if (project != null) {
      AnalysisContext context = getContext();
      new AnalysisWorker(project, context).performAnalysisInBackground();
    }
    // TODO(scheglov) temporary? at least right now we need to ask one time to resolve
    // because AST may be removed from cache at this moment, and AnalysisWorker will not
    // do anything until change.
    try {
      getResolvedUnit(true);
    } catch (Throwable e) {
      DartCore.logError(e);
    }
    // run loop and wait for CompilationUnit changes
    WeakReference<CompilationUnit> lastParsedUnit = new WeakReference<CompilationUnit>(null);
    CompilationUnitElement previousUnitElement = null;
    while (thread != null) {
      try {
        // wait
        try {
          Thread.sleep(25);
        } catch (InterruptedException e) {
        }
        // update read-only flag
        updateReadOnlyFlag();
        // process EditorState
        {
          // prepare EditorState to apply
          loopEditorState = null;
          synchronized (editorStateLock) {
            if (editorState != null && System.currentTimeMillis() - editorState.time > 100) {
              loopEditorState = editorState;
              editorState = null;
            }
          }
          // apply EditorState if it is ready
          if (loopEditorState != null) {
            notifyContextAboutCode(loopEditorState.code);
            CompilationUnit parsedUnit = getParsedUnit();
            boolean newUnit = !Objects.equal(parsedUnit, lastParsedUnit.get());
            lastParsedUnit = new WeakReference<CompilationUnit>(parsedUnit);
            displayReconcilerTick(parsedUnit, newUnit, loopEditorState.selectionRange);
          }
        }
        // may be resolved
        CompilationUnit unitNode = getResolvedUnit(false);
        if (unitNode != null) {
          CompilationUnitElement unitElement = unitNode.getElement();
          if (unitElement != null) {
            if (unitElement != previousUnitElement) {
              previousUnitElement = unitElement;
              editor.applyCompilationUnitElement(unitNode);
            }
          }
        }
        // done with loop state
        loopEditorState = null;
      } catch (Throwable e) {
      }
    }
  }

  /**
   * Checks if input {@link IFile} read-only flag was changed, and notifies {@link DartEditor}.
   */
  private void updateReadOnlyFlag() {
    if (file == null) {
      return;
    }
    boolean readOnly = file.isReadOnly();
    if (lastReadOnly == null || lastReadOnly.booleanValue() != readOnly) {
      lastReadOnly = readOnly;
      editor.setEditables(!readOnly);
    }
  }
}

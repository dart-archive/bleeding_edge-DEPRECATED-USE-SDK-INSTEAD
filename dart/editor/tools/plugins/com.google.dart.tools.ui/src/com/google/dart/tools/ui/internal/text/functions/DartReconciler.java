/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ElementChangedListener;
import com.google.dart.tools.core.utilities.general.Timer;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/**
 * A reconciler that is also activated on editor activation.
 */
public class DartReconciler extends MonoReconciler {

  /**
   * Internal resource change listener.
   */
  class ResourceChangeListener implements IResourceChangeListener {

    /*
     * @see IResourceChangeListener#resourceChanged(org.eclipse.core.resources.
     * IResourceChangeEvent)
     */
    @Override
    public void resourceChanged(IResourceChangeEvent e) {
      IResourceDelta delta = e.getDelta();
      IResource resource = getResource();
      if (delta != null && resource != null) {
        IResourceDelta child = delta.findMember(resource.getFullPath());
        if (child != null) {
          IMarkerDelta[] deltas = child.getMarkerDeltas();
          int i = deltas.length;
          while (--i >= 0) {
            try {
              if (deltas[i].getMarker().isSubtypeOf(IMarker.PROBLEM)) {
                forceReconciling();
                return;
              }
            } catch (CoreException e1) {
              // ignore and try next one
            }
          }
        }
      }
    }

    private IResource getResource() {
      IEditorInput input = fTextEditor.getEditorInput();
      if (input instanceof IFileEditorInput) {
        IFileEditorInput fileInput = (IFileEditorInput) input;
        return fileInput.getFile();
      }
      return null;
    }
  }

  /**
   * Internal Shell activation listener for activating the reconciler.
   */
  private class ActivationListener extends ShellAdapter {

    private Control fControl;

    public ActivationListener(Control control) {
      Assert.isNotNull(control);
      fControl = control;
    }

    /*
     * @see org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt.events .ShellEvent)
     */
    @Override
    public void shellActivated(ShellEvent e) {
      if (!fControl.isDisposed() && fControl.isVisible()) {
        if (hasJavaModelChanged()) {
          DartReconciler.this.forceReconciling();
        }
        setEditorActive(true);
      }
    }

    /*
     * @see org.eclipse.swt.events.ShellListener#shellDeactivated(org.eclipse.swt
     * .events.ShellEvent)
     */
    @Override
    public void shellDeactivated(ShellEvent e) {
      if (!fControl.isDisposed() && fControl.getShell() == e.getSource()) {
        setJavaModelChanged(false);
        setEditorActive(false);
      }
    }
  }

  /**
   * Internal part listener for activating the reconciler.
   */
  private class PartListener implements IPartListener {

    /*
     * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void partActivated(IWorkbenchPart part) {
      if (part == fTextEditor) {
        if (hasJavaModelChanged()) {
          DartReconciler.this.forceReconciling();
        }
        setEditorActive(true);
      }
    }

    /*
     * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart )
     */
    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
    }

    /*
     * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void partClosed(IWorkbenchPart part) {
    }

    /*
     * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart )
     */
    @Override
    public void partDeactivated(IWorkbenchPart part) {
      if (part == fTextEditor) {
        setJavaModelChanged(false);
        setEditorActive(false);
      }
    }

    /*
     * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void partOpened(IWorkbenchPart part) {
    }
  }

  /**
   * Internal Java element changed listener
   */
  private class ReconcilerElementChangedListener implements ElementChangedListener {
    /*
     * @see org.eclipse.wst.jsdt.core.IElementChangedListener#elementChanged(org.
     * eclipse.wst.jsdt.core.ElementChangedEvent)
     */
    @Override
    public void elementChanged(ElementChangedEvent event) {
      if (event.getDelta().getFlags() == DartElementDelta.F_AST_AFFECTED) {
        return;
      }
      setJavaModelChanged(true);
      if (!fIsReconciling && isEditorActive()) {
        DartReconciler.this.forceReconciling();
      }
    }
  }

  /** The reconciler's editor */
  private ITextEditor fTextEditor;
  /** The part listener */
  private IPartListener fPartListener;
  /** The shell listener */
  private ShellListener fActivationListener;
  /**
   * The mutex that keeps us from running multiple reconcilers on one editor.
   */
  private Object fMutex;
  /**
   * The Java element changed listener.
   */
  private ElementChangedListener fJavaElementChangedListener;
  /**
   * Tells whether the Java model sent out a changed event.
   */
  private volatile boolean fHasJavaModelChanged = true;
  /**
   * Tells whether this reconciler's editor is active.
   */
  private volatile boolean fIsEditorActive = true;
  /**
   * The resource change listener.
   */
  private IResourceChangeListener fResourceChangeListener;
  /**
   * The property change listener.
   */
  private IPropertyChangeListener fPropertyChangeListener;
  /**
   * Tells whether a reconcile is in progress.
   */
  private volatile boolean fIsReconciling = false;

  private boolean fIninitalProcessDone = false;

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
    fTextEditor = editor;

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=63898
    // when re-using editors, a new reconciler is set up by the source viewer
    // and the old one uninstalled. However, the old reconciler may still be
    // running.
    // To avoid having to reconcilers calling CompilationUnitEditor.reconciled,
    // we synchronized on a lock object provided by the editor.
    // The critical section is really the entire run() method of the reconciler
    // thread, but synchronizing process() only will keep
// DartReconcilingStrategy
    // from running concurrently on the same editor.
    // TODO remove once we have ensured that there is only one reconciler per
// editor.
    if (editor instanceof CompilationUnitEditor) {
      fMutex = ((CompilationUnitEditor) editor).getReconcilerLock();
    } else {
      fMutex = new Object(); // Null Object
    }
  }

  /*
   * @see org.eclipse.jface.text.reconciler.IReconciler#install(org.eclipse.jface .text.ITextViewer)
   */
  @Override
  public void install(ITextViewer textViewer) {
    super.install(textViewer);

    fPartListener = new PartListener();
    IWorkbenchPartSite site = fTextEditor.getSite();
    IWorkbenchWindow window = site.getWorkbenchWindow();
    window.getPartService().addPartListener(fPartListener);

    fActivationListener = new ActivationListener(textViewer.getTextWidget());
    Shell shell = window.getShell();
    shell.addShellListener(fActivationListener);

    fJavaElementChangedListener = new ReconcilerElementChangedListener();
    DartCore.addElementChangedListener(fJavaElementChangedListener);

    fResourceChangeListener = new ResourceChangeListener();
    IWorkspace workspace = DartToolsPlugin.getWorkspace();
    workspace.addResourceChangeListener(fResourceChangeListener);

    fPropertyChangeListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (SpellingService.PREFERENCE_SPELLING_ENABLED.equals(event.getProperty())
            || SpellingService.PREFERENCE_SPELLING_ENGINE.equals(event.getProperty())) {
          forceReconciling();
        }
      }
    };
    DartToolsPlugin.getDefault().getCombinedPreferenceStore().addPropertyChangeListener(
        fPropertyChangeListener);
  }

  /*
   * @see org.eclipse.jface.text.reconciler.IReconciler#uninstall()
   */
  @Override
  public void uninstall() {

    IWorkbenchPartSite site = fTextEditor.getSite();
    IWorkbenchWindow window = site.getWorkbenchWindow();
    window.getPartService().removePartListener(fPartListener);
    fPartListener = null;

    Shell shell = window.getShell();
    if (shell != null && !shell.isDisposed()) {
      shell.removeShellListener(fActivationListener);
    }
    fActivationListener = null;

    DartCore.removeElementChangedListener(fJavaElementChangedListener);
    fJavaElementChangedListener = null;

    IWorkspace workspace = DartToolsPlugin.getWorkspace();
    workspace.removeResourceChangeListener(fResourceChangeListener);
    fResourceChangeListener = null;

    DartToolsPlugin.getDefault().getCombinedPreferenceStore().removePropertyChangeListener(
        fPropertyChangeListener);
    fPropertyChangeListener = null;

    super.uninstall();
  }

  /*
   * @see org.eclipse.jface.text.reconciler.AbstractReconciler#aboutToReconcile()
   */
  @Override
  protected void aboutToBeReconciled() {
    DartCompositeReconcilingStrategy strategy = (DartCompositeReconcilingStrategy) getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
    strategy.aboutToBeReconciled();
  }

  /*
   * @see org.eclipse.jface.text.reconciler.AbstractReconciler#forceReconciling()
   */
  @Override
  protected void forceReconciling() {
    if (!fIninitalProcessDone) {
      return;
    }

    super.forceReconciling();
    DartCompositeReconcilingStrategy strategy = (DartCompositeReconcilingStrategy) getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
    strategy.notifyListeners(false);
  }

  /*
   * @see org.eclipse.jface.text.reconciler.MonoReconciler#initialProcess()
   */
  @Override
  protected void initialProcess() {
    synchronized (fMutex) {
      super.initialProcess();
    }
    fIninitalProcessDone = true;
  }

  /*
   * @see org.eclipse.jface.text.reconciler.MonoReconciler#process(org.eclipse.jface
   * .text.reconciler.DirtyRegion)
   */
  @Override
  protected void process(DirtyRegion dirtyRegion) {
    Timer timer = new Timer("reconcile");

    synchronized (fMutex) {
      fIsReconciling = true;
      super.process(dirtyRegion);
      fIsReconciling = false;
    }

    timer.stop();
  }

  /*
   * @see org.eclipse.jface.text.reconciler.AbstractReconciler#reconcilerReset()
   */
  @Override
  protected void reconcilerReset() {
    super.reconcilerReset();
    DartCompositeReconcilingStrategy strategy = (DartCompositeReconcilingStrategy) getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
    strategy.notifyListeners(true);
  }

  /**
   * Tells whether the Java Model has changed or not.
   * 
   * @return <code>true</code> iff the Java Model has changed
   */
  private synchronized boolean hasJavaModelChanged() {
    return fHasJavaModelChanged;
  }

  /**
   * Tells whether this reconciler's editor is active.
   * 
   * @return <code>true</code> iff the editor is active
   */
  private synchronized boolean isEditorActive() {
    return fIsEditorActive;
  }

  /**
   * Sets whether this reconciler's editor is active.
   * 
   * @param state <code>true</code> iff the editor is active
   */
  private synchronized void setEditorActive(boolean state) {
    fIsEditorActive = state;
  }

  /**
   * Sets whether the Java Model has changed or not.
   * 
   * @param state <code>true</code> iff the java model has changed
   */
  private synchronized void setJavaModelChanged(boolean state) {
    fHasJavaModelChanged = state;
  }
}

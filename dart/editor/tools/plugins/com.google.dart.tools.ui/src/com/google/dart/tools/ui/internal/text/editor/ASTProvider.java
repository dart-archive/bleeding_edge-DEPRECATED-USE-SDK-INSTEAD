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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Provides a shared AST for clients. The shared AST is the AST of the active Dart editor's input
 * element.
 */
public final class ASTProvider {

  /**
   * Wait flag.
   */
  public static final class WAIT_FLAG {

    String fName;

    private WAIT_FLAG(String name) {
      fName = name;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return fName;
    }
  }

  /**
   * Internal activation listener.
   */
  private class ActivationListener implements IPartListener2, IWindowListener {

    /*
     * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partActivated(IWorkbenchPartReference ref) {
      if (isJavaEditor(ref) && !isActiveEditor(ref)) {
        activeJavaEditorChanged(ref.getPart(true));
      }
    }

    /*
     * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partBroughtToTop(IWorkbenchPartReference ref) {
      if (isJavaEditor(ref) && !isActiveEditor(ref)) {
        activeJavaEditorChanged(ref.getPart(true));
      }
    }

    /*
     * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partClosed(IWorkbenchPartReference ref) {
      if (isActiveEditor(ref)) {
        if (DEBUG) {
          System.out.println(getThreadName()
              + " - " + DEBUG_PREFIX + "closed active editor: " + ref.getTitle()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        activeJavaEditorChanged(null);
      }
    }

    /*
     * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partDeactivated(IWorkbenchPartReference ref) {
    }

    /*
     * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partHidden(IWorkbenchPartReference ref) {
    }

    /*
     * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partInputChanged(IWorkbenchPartReference ref) {
      if (isJavaEditor(ref) && isActiveEditor(ref)) {
        activeJavaEditorChanged(ref.getPart(true));
      }
    }

    /*
     * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partOpened(IWorkbenchPartReference ref) {
      if (isJavaEditor(ref) && !isActiveEditor(ref)) {
        activeJavaEditorChanged(ref.getPart(true));
      }
    }

    /*
     * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui. IWorkbenchPartReference)
     */
    @Override
    public void partVisible(IWorkbenchPartReference ref) {
      if (isJavaEditor(ref) && !isActiveEditor(ref)) {
        activeJavaEditorChanged(ref.getPart(true));
      }
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui. IWorkbenchWindow)
     */
    @Override
    public void windowActivated(IWorkbenchWindow window) {
      IWorkbenchPartReference ref = window.getPartService().getActivePartReference();
      if (isJavaEditor(ref) && !isActiveEditor(ref)) {
        activeJavaEditorChanged(ref.getPart(true));
      }
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow )
     */
    @Override
    public void windowClosed(IWorkbenchWindow window) {
      if (fActiveEditor != null && fActiveEditor.getSite() != null
          && window == fActiveEditor.getSite().getWorkbenchWindow()) {
        if (DEBUG) {
          System.out.println(getThreadName()
              + " - " + DEBUG_PREFIX + "closed active editor: " + fActiveEditor.getTitle()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        activeJavaEditorChanged(null);
      }
      window.getPartService().removePartListener(this);
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui. IWorkbenchWindow)
     */
    @Override
    public void windowDeactivated(IWorkbenchWindow window) {
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow )
     */
    @Override
    public void windowOpened(IWorkbenchWindow window) {
      window.getPartService().addPartListener(this);
    }

    private boolean isActiveEditor(IWorkbenchPart part) {
      return part != null && (part == fActiveEditor);
    }

    private boolean isActiveEditor(IWorkbenchPartReference ref) {
      return ref != null && isActiveEditor(ref.getPart(false));
    }

    private boolean isJavaEditor(IWorkbenchPartReference ref) {
      if (ref == null) {
        return false;
      }

      String id = ref.getId();

      // The instanceof check is not need but helps clients, see
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=84862
      return DartUI.ID_CF_EDITOR.equals(id) || DartUI.ID_CU_EDITOR.equals(id)
          || ref.getPart(false) instanceof DartEditor;
    }
  }

  /**
   * Wait flag indicating that a client requesting an AST wants to wait until an AST is ready.
   * <p>
   * An AST will be created by this AST provider if the shared AST is not for the given java
   * element.
   */
  public static final WAIT_FLAG WAIT_YES = new WAIT_FLAG("wait yes"); //$NON-NLS-1$

  /**
   * Wait flag indicating that a client requesting an AST only wants to wait for the shared AST of
   * the active editor.
   * <p>
   * No AST will be created by the AST provider.
   */
  public static final WAIT_FLAG WAIT_ACTIVE_ONLY = new WAIT_FLAG("wait active only"); //$NON-NLS-1$

  /**
   * Wait flag indicating that a client requesting an AST only wants the already available shared
   * AST.
   * <p>
   * No AST will be created by the AST provider.
   */
  public static final WAIT_FLAG WAIT_NO = new WAIT_FLAG("don't wait"); //$NON-NLS-1$

  /**
   * Tells whether this class is in debug mode.
   */
  private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("com.google.dart.tools.ui/debug/ASTProvider")); //$NON-NLS-1$//$NON-NLS-2$

  public static final int SHARED_AST_LEVEL = 0;
  public static final boolean SHARED_AST_STATEMENT_RECOVERY = true;
  public static final boolean SHARED_BINDING_RECOVERY = true;

  private static final String DEBUG_PREFIX = "ASTProvider > "; //$NON-NLS-1$

  /**
   * Returns the Java plug-in's AST provider.
   * 
   * @return the AST provider
   */
  public static ASTProvider getASTProvider() {
    return DartToolsPlugin.getDefault().getASTProvider();
  }

  private DartElement fReconcilingJavaElement;
  private DartElement fActiveJavaElement;
  private ActivationListener fActivationListener;
  private Object fReconcileLock = new Object();
  private Object fWaitLock = new Object();
  private boolean fIsReconciling;

  private IWorkbenchPart fActiveEditor;

  /**
   * Creates a new AST provider.
   */
  public ASTProvider() {
    install();
  }

  /**
   * Disposes this AST provider.
   */
  public void dispose() {

    // Dispose activation listener
    PlatformUI.getWorkbench().removeWindowListener(fActivationListener);
    fActivationListener = null;

    synchronized (fWaitLock) {
      fWaitLock.notifyAll();
    }
  }

  /**
   * Installs this AST provider.
   */
  void install() {
    // Create and register activation listener
    fActivationListener = new ActivationListener();
    PlatformUI.getWorkbench().addWindowListener(fActivationListener);

    // Ensure existing windows get connected
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (int i = 0, length = windows.length; i < length; i++) {
      windows[i].getPartService().addPartListener(fActivationListener);
    }
  }

  void reconciled(DartElement javaElement, IProgressMonitor progressMonitor) {

    if (DEBUG) {
      System.out.println(getThreadName()
          + " - " + DEBUG_PREFIX + "reconciled: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    synchronized (fReconcileLock) {

      fIsReconciling = progressMonitor != null && progressMonitor.isCanceled();
      if (javaElement == null || !javaElement.equals(fReconcilingJavaElement)) {

        if (DEBUG) {
          System.out.println(getThreadName()
              + " - " + DEBUG_PREFIX + "  ignoring AST of out-dated editor"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Signal - threads might wait for wrong element
        synchronized (fWaitLock) {
          fWaitLock.notifyAll();
        }

        return;
      }
    }
  }

  private void activeJavaEditorChanged(IWorkbenchPart editor) {

    DartElement dartElement = null;

    synchronized (this) {
      fActiveEditor = editor;
      fActiveJavaElement = dartElement;
    }

    if (DEBUG) {
      System.out.println(getThreadName()
          + " - " + DEBUG_PREFIX + "active editor is: " + toString(dartElement)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    synchronized (fReconcileLock) {
      if (fIsReconciling
          && (fReconcilingJavaElement == null || !fReconcilingJavaElement.equals(dartElement))) {
        fIsReconciling = false;
        fReconcilingJavaElement = null;
      } else if (dartElement == null) {
        fIsReconciling = false;
        fReconcilingJavaElement = null;
      }
    }
  }

  private String getThreadName() {
    String name = Thread.currentThread().getName();
    if (name != null) {
      return name;
    } else {
      return Thread.currentThread().toString();
    }
  }

  /**
   * Returns a string for the given Java element used for debugging.
   * 
   * @param javaElement the compilation unit AST
   * @return a string used for debugging
   */
  private String toString(DartElement javaElement) {
    if (javaElement == null) {
      return "null"; //$NON-NLS-1$
    } else {
      return javaElement.getElementName();
    }

  }
}

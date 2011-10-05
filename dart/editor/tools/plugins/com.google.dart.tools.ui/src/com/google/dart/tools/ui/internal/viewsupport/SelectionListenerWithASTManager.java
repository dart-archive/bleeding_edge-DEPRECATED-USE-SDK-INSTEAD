/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.internal.text.editor.ASTProvider;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.HashMap;
import java.util.Map;

/**
 * Infrastructure to share an AST for editor post selection listeners.
 */
public class SelectionListenerWithASTManager {

  private final static class PartListenerGroup {
    private ITextEditor fPart;
    private ISelectionListener fPostSelectionListener;
    private ISelectionChangedListener fSelectionListener;
    private Job fCurrentJob;
    private ListenerList fAstListeners;
    /**
     * Lock to avoid having more than one calculateAndInform job in parallel. Only jobs may
     * synchronize on this as otherwise deadlocks are possible.
     */
    private final Object fJobLock = new Object();

    public PartListenerGroup(ITextEditor editorPart) {
      fPart = editorPart;
      fCurrentJob = null;
      fAstListeners = new ListenerList(ListenerList.IDENTITY);

      fSelectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
          ISelection selection = event.getSelection();
          if (selection instanceof ITextSelection) {
            fireSelectionChanged((ITextSelection) selection);
          }
        }
      };

      fPostSelectionListener = new ISelectionListener() {
        @Override
        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
          if (part == fPart && selection instanceof ITextSelection) {
            firePostSelectionChanged((ITextSelection) selection);
          }
        }
      };
    }

    public void firePostSelectionChanged(final ITextSelection selection) {
      if (fCurrentJob != null) {
        fCurrentJob.cancel();
      }
      final DartElement input = EditorUtility.getEditorInputJavaElement(fPart, false);
      if (input == null) {
        return;
      }

      fCurrentJob = new Job(DartUIMessages.SelectionListenerWithASTManager_job_title) {
        @Override
        public IStatus run(IProgressMonitor monitor) {
          if (monitor == null) {
            monitor = new NullProgressMonitor();
          }
          synchronized (fJobLock) {
            return calculateASTandInform(input, selection, monitor);
          }
        }
      };
      fCurrentJob.setPriority(Job.DECORATE);
      fCurrentJob.setSystem(true);
      fCurrentJob.schedule();
    }

    public void fireSelectionChanged(final ITextSelection selection) {
      if (fCurrentJob != null) {
        fCurrentJob.cancel();
      }
    }

    public void install(ISelectionListenerWithAST listener) {
      if (isEmpty()) {
        fPart.getEditorSite().getPage().addPostSelectionListener(fPostSelectionListener);
        ISelectionProvider selectionProvider = fPart.getSelectionProvider();
        if (selectionProvider != null) {
          selectionProvider.addSelectionChangedListener(fSelectionListener);
        }
      }
      fAstListeners.add(listener);
    }

    public boolean isEmpty() {
      return fAstListeners.isEmpty();
    }

    public void uninstall(ISelectionListenerWithAST listener) {
      fAstListeners.remove(listener);
      if (isEmpty()) {
        fPart.getEditorSite().getPage().removePostSelectionListener(fPostSelectionListener);
        ISelectionProvider selectionProvider = fPart.getSelectionProvider();
        if (selectionProvider != null) {
          selectionProvider.removeSelectionChangedListener(fSelectionListener);
        }
      }
    }

    protected IStatus calculateASTandInform(DartElement input, ITextSelection selection,
        IProgressMonitor monitor) {
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      // create AST
      try {
        DartUnit astRoot = DartToolsPlugin.getDefault().getASTProvider().getAST(input,
            ASTProvider.WAIT_ACTIVE_ONLY, monitor);

        if (astRoot != null && !monitor.isCanceled()) {
          Object[] listeners;
          synchronized (PartListenerGroup.this) {
            listeners = fAstListeners.getListeners();
          }
          for (int i = 0; i < listeners.length; i++) {
            ((ISelectionListenerWithAST) listeners[i]).selectionChanged(fPart, selection, astRoot);
            if (monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            }
          }
          return Status.OK_STATUS;
        }
      } catch (OperationCanceledException e) {
        // thrown when canceling the AST creation
      }
      return Status.CANCEL_STATUS;
    }
  }

  private static SelectionListenerWithASTManager fgDefault;

  /**
   * @return Returns the default manager instance.
   */
  public static SelectionListenerWithASTManager getDefault() {
    if (fgDefault == null) {
      fgDefault = new SelectionListenerWithASTManager();
    }
    return fgDefault;
  }

  private Map<ITextEditor, PartListenerGroup> fListenerGroups;

  private SelectionListenerWithASTManager() {
    fListenerGroups = new HashMap<ITextEditor, PartListenerGroup>();
  }

  /**
   * Registers a selection listener for the given editor part.
   * 
   * @param part The editor part to listen to.
   * @param listener The listener to register.
   */
  public void addListener(ITextEditor part, ISelectionListenerWithAST listener) {
    synchronized (this) {
      PartListenerGroup partListener = fListenerGroups.get(part);
      if (partListener == null) {
        partListener = new PartListenerGroup(part);
        fListenerGroups.put(part, partListener);
      }
      partListener.install(listener);
    }
  }

  /**
   * Unregisters a selection listener.
   * 
   * @param part The editor part the listener was registered.
   * @param listener The listener to unregister.
   */
  public void removeListener(ITextEditor part, ISelectionListenerWithAST listener) {
    synchronized (this) {
      PartListenerGroup partListener = fListenerGroups.get(part);
      if (partListener != null) {
        partListener.uninstall(listener);
        if (partListener.isEmpty()) {
          fListenerGroups.remove(part);
        }
      }
    }
  }
}

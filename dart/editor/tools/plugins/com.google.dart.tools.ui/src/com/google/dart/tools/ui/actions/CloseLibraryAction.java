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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.actions.WorkbenchRunnableAdapter;
import com.google.dart.tools.ui.internal.handlers.NewFileCommandState;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.services.ISourceProviderService;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Close Library action. Removes a library from the Libraries view (but keeps the source code on
 * disk).
 */
public class CloseLibraryAction extends Action implements IWorkbenchAction, ISelectionListener,
    ISelectionChangedListener {

  public static final String ID = DartToolsPlugin.PLUGIN_ID + ".closeLibraryAction"; //$NON-NLS-1$

  IStructuredSelection selection;
  IWorkbenchWindow window;

  /**
   * Create an action that removes a library from the view.
   */
  public CloseLibraryAction(IWorkbenchWindow window) {
    setId(ID);
    setText(ActionMessages.CloseLibraryAction_label);
    setImageDescriptor(null);
    this.window = window;
    window.getSelectionService().addSelectionListener(this);
  }

  @Override
  public void dispose() {
    //do nothing
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run() {

    final List<DartLibrary> libraries = selection.toList();

    try {
      PlatformUI.getWorkbench().getProgressService().run(false, false,
          new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {

              monitor.beginTask(ActionMessages.CloseLibraryAction_jobTitle, 30);
              for (DartLibrary library : libraries) {
                library.setTopLevel(false);
              }
              List<DartLibrary> unreferencedLibraries = new ArrayList<DartLibrary>();

              try {
                unreferencedLibraries = DartModelManager.getInstance().getDartModel().getUnreferencedLibraries();
              } catch (DartModelException e) {
                ExceptionHandler.handle(e, window.getShell(),
                    ActionMessages.CloseLibraryAction_error_title,
                    ActionMessages.CloseLibraryAction_error_message);
              }

              for (DartLibrary library : unreferencedLibraries) {
                try {
                  library.delete(new SubProgressMonitor(monitor, 1));
                } catch (DartModelException e) {
                  ExceptionHandler.handle(e, window.getShell(),
                      ActionMessages.CloseLibraryAction_error_title,
                      ActionMessages.CloseLibraryAction_error_message);
                }
              }

              monitor.done();
            }
          })); // workspace lock
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(e, window.getShell(), ActionMessages.CloseLibraryAction_error_title,
          ActionMessages.CloseLibraryAction_error_message);
    } catch (InterruptedException e) {
      // canceled by user
    }

    ISourceProviderService service = (ISourceProviderService) window.getService(ISourceProviderService.class);

    NewFileCommandState newFileCommandStateProvider = (NewFileCommandState) service.getSourceProvider(NewFileCommandState.NEW_FILE_STATE);
    newFileCommandStateProvider.checkState();

  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) selection);
    } else {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    if (event.getSelection() instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) event.getSelection());
    } else {
      setEnabled(false);
    }
  }

  private void handleSelectionChanged(IStructuredSelection selection) {
    this.selection = selection;
    if (selection == null || selection.isEmpty()) {
      setEnabled(false);
    } else {
      for (Object object : selection.toList()) {
        if (!(object instanceof DartLibrary)) {
          setEnabled(false);
          return;
        }
      }
      setEnabled(true);
    }
  }

}

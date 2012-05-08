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
package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.preferences.DebugPreferencePage;
import com.google.dart.tools.debug.ui.internal.server.DartServerLaunchShortcut;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.dialogs.FilteredPreferenceDialog;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;

import java.io.File;

/**
 * Launch a Dart server application.
 * 
 * @deprecated
 */

@Deprecated
@SuppressWarnings("restriction")
public class RunInServerAction extends Action implements ISelectionListener,
    ISelectionChangedListener, IPartListener {
  private IWorkbenchWindow window;
  private IFile selectedFile;

  public RunInServerAction(IWorkbenchWindow window) {
    this.window = window;

    setText("Launch in Dart Server");
    setId(DartToolsPlugin.PLUGIN_ID + ".runServerAction");
    setDescription("Launch in Dart Server");
    setToolTipText("Launch in Dart Server");
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/run_server.png"));

    setEnabled(false);

    window.getSelectionService().addSelectionListener(this);
    window.getPartService().addPartListener(this);
  }

  @Override
  public void partActivated(IWorkbenchPart part) {
    if (part instanceof IEditorPart) {
      handleEditorActivated((IEditorPart) part);
    }
  }

  @Override
  public void partBroughtToTop(IWorkbenchPart part) {

  }

  @Override
  public void partClosed(IWorkbenchPart part) {

  }

  @Override
  public void partDeactivated(IWorkbenchPart part) {

  }

  @Override
  public void partOpened(IWorkbenchPart part) {

  }

  @Override
  public void run() {
    String vmExecPath = "";

    if (DartSdk.isInstalled()) {
      File vmExec = DartSdk.getInstance().getVmExecutable();
      if (vmExec != null) {
        vmExecPath = vmExec.getAbsolutePath().toString();
      }
    } else {
      vmExecPath = DartDebugCorePlugin.getPlugin().getDartVmExecutablePath();
      if (vmExecPath.length() == 0) {
        if (showPreferenceDialog() != Window.OK) {
          return;
        }
      }
    }

    DartServerLaunchShortcut launchShortcut = new DartServerLaunchShortcut();

    launchShortcut.launch(new StructuredSelection(selectedFile), ILaunchManager.RUN_MODE);
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) selection);
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    if (event.getSelection() instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) event.getSelection());
    }
  }

  private IFile findFile(IStructuredSelection selection) {
    Object sel = selection.getFirstElement();

    if (sel instanceof IFile) {
      return (IFile) sel;
    } else if (sel instanceof DartElement) {
      try {
        IResource resource = ((DartElement) sel).getCorrespondingResource();

        if (resource instanceof IFile) {
          return (IFile) resource;
        }
      } catch (DartModelException exception) {
        DartUtil.logError(exception);
      }
    }

    return null;
  }

  private void handleEditorActivated(IEditorPart editorPart) {
    if (editorPart.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) editorPart.getEditorInput();

      handleSelectionChanged(new StructuredSelection(input.getFile()));
    }
  }

  private void handleSelectionChanged(IStructuredSelection selection) {
    if (selection != null && !selection.isEmpty()) {
      selectedFile = findFile(selection);
    } else {
      selectedFile = null;
    }

    setEnabled(isValidSelectedFile());
  }

  private boolean isValidSelectedFile() {
    if (selectedFile == null) {
      return false;
    }

    if (!DartCore.isDartLikeFileName(selectedFile.getName())) {
      return false;
    }

    DartElement element = DartCore.create(selectedFile);

    if (element instanceof CompilationUnit) {
      CompilationUnit cu = (CompilationUnit) element;

      DartLibrary lib = cu.getLibrary();

      if (lib instanceof DartLibraryImpl) {
        DartLibraryImpl impl = (DartLibraryImpl) lib;

        return impl.hasMain() && impl.isServerApplication();
      }
    }

    return false;
  }

  /**
   * @return either Window.OK or Window.CANCEL
   */
  private int showPreferenceDialog() {
    FilteredPreferenceDialog dialog = WorkbenchPreferenceDialog.createDialogOn(
        window.getShell(),
        DebugPreferencePage.PAGE_ID);

    dialog.showOnly(new String[] {DebugPreferencePage.PAGE_ID});

    return dialog.open();
  }

}

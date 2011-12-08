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

import com.google.dart.compiler.backend.js.AbstractJsBackend;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.CompileOptimized;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.ImportedDartLibraryContainer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import java.io.File;

/**
 * An action to create an optimized javascript build of a Dart library.
 */
public class DeployOptimizedAction extends AbstractInstrumentedAction implements IWorkbenchAction,
    ISelectionListener, IPartListener {

  class DeployOptimizedJob extends Job {
    private IWorkbenchPage page;
    private File file;
    private DartLibrary library;

    public DeployOptimizedJob(IWorkbenchPage page, File file, DartLibrary library) {
      super(ActionMessages.DeployOptimizedAction_jobTitle);

      this.page = page;
      this.file = file;
      this.library = library;

      // Synchronize on the workspace root to catch any builds that are in progress.
      setRule(ResourcesPlugin.getWorkspace().getRoot());

      // Make sure we display a progress dialog if we do block.
      setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      return deployOptimizedLibrary(monitor, page, file, library);
    }
  }

  private IWorkbenchWindow window;

  private Object selectedObject;

  public DeployOptimizedAction(IWorkbenchWindow window) {
    this.window = window;

    setText(ActionMessages.DeployOptimizedAction_title);
    setId(DartToolsPlugin.PLUGIN_ID + ".deployOptimizedAction");
    setDescription(ActionMessages.DeployOptimizedAction_description);
    setToolTipText(ActionMessages.DeployOptimizedAction_tooltip);
    //setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_opt.png"));
    setEnabled(false);

    window.getPartService().addPartListener(this);
    window.getSelectionService().addSelectionListener(this);
  }

  @Override
  public void dispose() {

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
    EmitInstrumentationCommand();
    deployOptimized(window.getActivePage());
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) selection);
    }
  }

  private void deployOptimized(IWorkbenchPage page) {
    boolean isSaveNeeded = isSaveAllNeeded(page);

    if (isSaveNeeded) {
      if (!saveDirtyEditors(page)) {
        // The user cancelled the launch.
        return;
      }
    }

    final DartLibrary library = getCurrentLibrary();

    if (library == null) {
      MessageDialog.openError(window.getShell(),
          ActionMessages.DeployOptimizedAction_unableToLaunch,
          ActionMessages.DeployOptimizedAction_noneSelected);
    } else {
      try {
        // Get the output location
        FileDialog saveDialog = new FileDialog(window.getShell(), SWT.SAVE);
        IResource libraryResource = library.getCorrespondingResource();
        saveDialog.setFilterPath(libraryResource.getRawLocation().toFile().toString());
        saveDialog.setFileName(libraryResource.getName() + "." + AbstractJsBackend.EXTENSION_JS);

        String fileName = saveDialog.open();

        if (fileName != null) {
          DeployOptimizedJob job = new DeployOptimizedJob(page, new File(fileName), library);
          job.schedule(isSaveNeeded ? 100 : 0);
        }
      } catch (DartModelException exception) {
        DartToolsPlugin.log(exception);

        MessageDialog.openError(window.getShell(),
            ActionMessages.DeployOptimizedAction_unableToLaunch,
            NLS.bind(ActionMessages.DeployOptimizedAction_errorLaunching, exception.getMessage()));
      }
    }
  }

  private IStatus deployOptimizedLibrary(IProgressMonitor monitor, IWorkbenchPage page,
      File outputFile, DartLibrary library) {

    CompileOptimized dartCompile = new CompileOptimized(library, outputFile);
    return dartCompile.compileToJs(monitor);

  }

  private DartLibrary getCurrentLibrary() {
    IResource resource = null;
    DartElement element = null;

    if (selectedObject instanceof IResource) {
      resource = (IResource) selectedObject;
    }

    if (resource != null) {
      element = DartCore.create(resource);
    }

    if (selectedObject instanceof DartElement) {
      element = (DartElement) selectedObject;
    }

    if (selectedObject instanceof ImportedDartLibraryContainer) {
      element = ((ImportedDartLibraryContainer) selectedObject).getDartLibrary();
    }

    if (element == null) {
      return null;
    } else {
      // DartElement in a library
      DartLibrary library = element.getAncestor(DartLibrary.class);

      return library;
    }
  }

  private void handleEditorActivated(IEditorPart editorPart) {
    if (editorPart.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) editorPart.getEditorInput();

      handleSelectionChanged(new StructuredSelection(input.getFile()));
    }
  }

  private void handleSelectionChanged(IStructuredSelection selection) {
    if (selection != null && !selection.isEmpty()) {
      selectedObject = selection.getFirstElement();

      setEnabled(true);
    } else {
      selectedObject = null;

      setEnabled(false);
    }
  }

  private boolean isSaveAllNeeded(IWorkbenchPage page) {
    IEditorReference[] editors = page.getEditorReferences();
    for (int i = 0; i < editors.length; i++) {
      IEditorReference ed = editors[i];
      if (ed.isDirty()) {
        return true;
      }
    }
    return false;
  }

  private boolean saveDirtyEditors(IWorkbenchPage page) {
    return page.saveAllEditors(false);
  }

}

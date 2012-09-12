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

package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dartdoc.DartdocGenerator;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.ImportedDartLibraryContainer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * An action to create Dart documentation for some library.
 * 
 * @see GenerateJavascriptAction
 */
public class GenerateDartdocAction extends AbstractInstrumentedAction implements IWorkbenchAction,
    ISelectionListener, IPartListener {

  class DeployOptimizedJob extends Job {
    private DartLibrary library;

    public DeployOptimizedJob(IWorkbenchPage page, DartLibrary library) {
      super(ActionMessages.GenerateDartdocAction_jobTitle);

      this.library = library;

      // Synchronize on the workspace root to catch any builds that are in progress.
      setRule(ResourcesPlugin.getWorkspace().getRoot());

      // Make sure we display a progress dialog if we do block.
      setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        monitor.beginTask(
            ActionMessages.GenerateDartdocAction_Compiling + library.getElementName(),
            IProgressMonitor.UNKNOWN);

        DartdocGenerator.generateDartdoc(library, monitor, DartCore.getConsole());

        return Status.OK_STATUS;
      } catch (OperationCanceledException exception) {
        // The user cancelled.
        DartCore.getConsole().println("Generation cancelled.");

        return Status.CANCEL_STATUS;
      } catch (Exception exception) {
        DartCore.getConsole().println(
            NLS.bind(ActionMessages.GenerateDartdocAction_FailException, exception.toString()));

        return Status.CANCEL_STATUS;
      } finally {
        if (OPEN_BROWSER_AFTER_GENERATION) {
          try {
            openDocsInBrowser(DartdocGenerator.getDocsIndexPath(
                library.getCorrespondingResource().getLocation()).toOSString());
          } catch (DartModelException e) {
            e.printStackTrace();
          }
        }
        monitor.done();
      }
    }
  }

  private final static boolean OPEN_BROWSER_AFTER_GENERATION = false;

  private IWorkbenchWindow window;

  private Object selectedObject;

  public GenerateDartdocAction(IWorkbenchWindow window) {
    this.window = window;

    setText(ActionMessages.GenerateDartdocAction_title);
    setActionDefinitionId("com.google.dart.tools.ui.generateDartdoc");
    setDescription(ActionMessages.GenerateDartdocAction_description);
    setToolTipText(ActionMessages.GenerateDartdocAction_tooltip);
    //setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_opt.png"));
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
      MessageDialog.openError(
          window.getShell(),
          ActionMessages.GenerateDartdocAction_unableToLaunch,
          ActionMessages.GenerateDartdocAction_noneSelected);
    } else {
      DeployOptimizedJob job = new DeployOptimizedJob(page, library);
      job.schedule(isSaveNeeded ? 100 : 0);
    }
  }

  private DartLibrary getCurrentLibrary() {
    IResource resource = null;
    DartElement element = null;

    if (selectedObject == null) {
      IWorkbenchPage page = window.getActivePage();

      if (page != null) {
        IEditorPart part = page.getActiveEditor();

        if (part != null) {
          selectedObject = part.getEditorInput().getAdapter(IResource.class);
        }
      }
    }
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
    } else {
      selectedObject = null;
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

  private void openDocsInBrowser(String file) {
    IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
    try {
      IWebBrowser browser = support.getExternalBrowser();
      browser.openURL(new URL("file://" + file));
    } catch (MalformedURLException e) {
      DartToolsPlugin.log(e);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    }
  }

  private boolean saveDirtyEditors(IWorkbenchPage page) {
    return page.saveAllEditors(false);
  }

}

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
import com.google.dart.tools.core.internal.builder.DartBuilder;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.dartium.DartiumLaunchShortcut;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.ImportedDartLibraryContainer;
import com.google.dart.tools.ui.actions.AbstractInstrumentedAction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A menu for opening html files in the system browser.
 * 
 * @deprecated
 */
@Deprecated
public class RunInBrowserAction extends AbstractInstrumentedAction implements
    ISelectionChangedListener, ISelectionListener, IPartListener, IViewActionDelegate {

  class RunInBrowserJob extends UIJob {
    private IWorkbenchPage page;
    private IFile file;

    public RunInBrowserJob(IWorkbenchPage page, IFile file) {
      super(
          page.getWorkbenchWindow().getShell().getDisplay(),
          ActionMessages.OpenInBrowserAction_jobTitle);

      this.page = page;
      this.file = file;

      // Synchronize on the workspace root to catch any builds that are in progress.
      setRule(ResourcesPlugin.getWorkspace().getRoot());

      // Make sure we display a progress dialog if we do block.
      setUser(true);
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {
      launchBrowserForHtmlFile(page, file);

      return Status.OK_STATUS;
    }
  }

  /**
   * The id of this action.
   */
  public static final String ACTION_ID = DartDebugUIPlugin.PLUGIN_ID + ".runInBrowserAction"; //$NON-NLS-1$

  public static File getJsAppArtifactFile(IPath sourceLocation) {
    return DartBuilder.getJsAppArtifactFile(sourceLocation);
  }

  private IWorkbenchWindow window;

  private Object selectedObject;

  /**
   * Match both the input and id, so that different types of editor can be opened on the same input.
   */
  private static final int MATCH_BOTH = IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID;

  public RunInBrowserAction() {
    initialize();
  }

  public RunInBrowserAction(IWorkbenchWindow window) {
    this.window = window;

    initialize();

    window.getPartService().addPartListener(this);
    window.getSelectionService().addSelectionListener(this);
  }

  @Override
  public void init(IViewPart view) {
    view.getSite().getSelectionProvider().addSelectionChangedListener(this);
    window = view.getViewSite().getWorkbenchWindow();

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
    openInBrowser(window.getActivePage());
  }

  @Override
  public void run(IAction action) {
    run();

  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) selection);
    }

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

  void launchBrowserForHtmlFile(IWorkbenchPage page, IFile file) {
    DartElement element = DartCore.create(file);

    if (element == null) {
      MessageDialog.openError(
          window.getShell(),
          ActionMessages.OpenInBrowserAction_unableToLaunch,
          ActionMessages.OpenInBrowserAction_notInDartLib);
    } else if (!(element instanceof HTMLFile)) {
      MessageDialog.openError(
          window.getShell(),
          ActionMessages.OpenInBrowserAction_unableToLaunch,
          ActionMessages.OpenInBrowserAction_notAnHtmlFile);
    } else {
      // check that the js output file exists
      HTMLFile htmlFile = (HTMLFile) element;

      try {
        if (htmlFile.getReferencedLibraries().length > 0) {
          DartLibrary library = htmlFile.getReferencedLibraries()[0];
          File jsOutFile = getJsAppArtifactFile(library.getCorrespondingResource().getLocation());
          boolean useDefaultBrowser;
          if (jsOutFile.exists()) {
            try {
              IEclipsePreferences prefs = DartDebugCorePlugin.getPlugin().getPrefs();
              useDefaultBrowser = prefs.getBoolean(DartDebugCorePlugin.PREFS_DEFAULT_BROWSER, true);
              if (!useDefaultBrowser) {
                String browserName = prefs.get(DartDebugCorePlugin.PREFS_BROWSER_NAME, "");
                if (!browserName.isEmpty()) {
                  Program program = findProgram(browserName);
                  if (program != null) {
                    program.execute(file.getLocation().toOSString());

                  } else {
                    MessageDialog.openError(
                        window.getShell(),
                        ActionMessages.OpenInBrowserAction_unableToLaunch,
                        ActionMessages.OpenInBrowserAction_couldNotOpenFile);
                  }
                  return;
                }
              }

              if (DartSdk.isInstalled()) {
                // check if Dartium is available and launch as first option
                File dartiumFile = DartSdk.getInstance().getDartiumExecutable();
                if (dartiumFile != null) {
                  DartiumLaunchShortcut launchShortcut = new DartiumLaunchShortcut();
                  launchShortcut.launch(new StructuredSelection(file), ILaunchManager.RUN_MODE);
                  return;
                }
              }
              String editorId = IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID;
              page.openEditor(new FileEditorInput(file), editorId, true, MATCH_BOTH);
            } catch (PartInitException e) {
              MessageDialog.openError(
                  window.getShell(),
                  ActionMessages.OpenInBrowserAction_title,
                  ActionMessages.OpenInBrowserAction_couldNotOpenFile);
              DartDebugCorePlugin.logError(e);
            }
          } else {
            MessageDialog.openError(
                window.getShell(),
                ActionMessages.OpenInBrowserAction_unableToLaunch,
                NLS.bind(
                    ActionMessages.OpenInBrowserAction_noJSFile,
                    file.getName(),
                    library.getDisplayName()));
          }
        }
      } catch (DartModelException ex) {
        MessageDialog.openError(
            window.getShell(),
            ActionMessages.OpenInBrowserAction_title,
            ActionMessages.OpenInBrowserAction_couldNotOpenFile);
        DartDebugCorePlugin.logError(ex);
      }
    }
  }

  void openInBrowser(IWorkbenchPage page) {
    try {
      List<IFile> files = getFileResourcesForSelection();

      IFile file = null;

      if (files.size() == 0) {
        MessageDialog.openError(
            window.getShell(),
            ActionMessages.OpenInBrowserAction_noFileTitle,
            ActionMessages.OpenInBrowserAction_noFileMessage);
      } else if (files.size() == 1) {
        file = files.get(0);
      } else {
        file = chooseHtmlFile(files);
      }

      if (file != null) {
        boolean isSaveNeeded = isSaveAllNeeded(page);

        if (isSaveNeeded) {
          if (!saveDirtyEditors(page)) {
            // The user cancelled the launch.
            return;
          }
        }

        RunInBrowserJob job = new RunInBrowserJob(page, file);
        // If we saved any files, delay for a bit to allow the builder to fire off a build.
        // Once the builder starts, we will automatically wait for it to complete before launching.
        job.schedule(isSaveNeeded ? 100 : 0);

      }
    } catch (DartModelException e) {
      MessageDialog.openError(
          window.getShell(),
          ActionMessages.OpenInBrowserAction_title,
          ActionMessages.OpenInBrowserAction_couldNotOpenFile);
      DartDebugCorePlugin.logError(e);
    }
  }

  private IFile chooseHtmlFile(List<IFile> htmlFiles) {
    ListDialog dialog = new ListDialog(window.getShell());

    dialog.setTitle(ActionMessages.OpenInBrowserAction_selectFileTitle);
    dialog.setMessage(ActionMessages.OpenInBrowserAction_selectFileMessage);
    dialog.setLabelProvider(new WorkbenchLabelProvider());
    dialog.setContentProvider(new ArrayContentProvider());
    dialog.setInput(htmlFiles);

    dialog.open();

    Object[] result = dialog.getResult();

    if (result == null || result.length == 0) {
      return null;
    }

    return (IFile) result[0];
  }

  private Program findProgram(String name) {

    Program[] programs = Program.getPrograms();
    for (Program program : programs) {
      if (program.getName().equals(name)) {
        return program;
      }
    }

    return null;
  }

  private List<IFile> getFileResourcesForSelection() throws DartModelException {
    IResource resource = null;
    DartElement element = null;

    if (selectedObject == null) {
      return Collections.emptyList();
    }

    if (selectedObject instanceof IResource) {
      resource = (IResource) selectedObject;
    }

    if (resource != null) {
      // html file
      if (isHtmlFile(resource)) {
        return Collections.singletonList((IFile) resource);
      }

      // other resource
      element = DartCore.create(resource);
    }

    if (selectedObject instanceof DartElement) {
      element = (DartElement) selectedObject;
    }

    // HTMLFile
    if (element instanceof HTMLFile) {
      HTMLFile htmlFile = (HTMLFile) element;

      return Collections.singletonList((IFile) htmlFile.getCorrespondingResource());
    }

    if (selectedObject instanceof ImportedDartLibraryContainer) {
      element = ((ImportedDartLibraryContainer) selectedObject).getDartLibrary();
    }

    if (element == null) {
      return Collections.emptyList();
    } else {
      // DartElement in a library
      DartLibrary library = element.getAncestor(DartLibrary.class);

      if (library != null) {
        List<IFile> htmlFiles = getHtmlFilesFor(library);

        if (htmlFiles.size() > 0) {
          return htmlFiles;
        }
      }

      return Collections.emptyList();
    }
  }

  private List<IFile> getHtmlFilesFor(DartLibrary library) throws DartModelException {
    Set<IFile> files = new HashSet<IFile>();

    for (HTMLFile file : ((DartLibraryImpl) library).getChildrenOfType(HTMLFile.class)) {
      files.add((IFile) file.getUnderlyingResource());
    }

    return new ArrayList<IFile>(files);
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

    try {
      setEnabled(getFileResourcesForSelection().size() > 0);
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

  private void initialize() {
    setText(ActionMessages.OpenInBrowserAction_title);
    setId(ACTION_ID);
    setDescription(ActionMessages.OpenInBrowserAction_description);
    setToolTipText(ActionMessages.OpenInBrowserAction_toolTip);
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/run_client.png"));

    setEnabled(false);
  }

  private boolean isHtmlFile(IResource resource) {
    return resource instanceof IFile && resource.getName().endsWith(".html");
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

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
import com.google.dart.tools.core.internal.builder.ScanCallbackProvider;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.dialogs.DialogMessages;
import com.google.dart.tools.ui.internal.util.DirectoryVerification;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import java.io.File;
import java.io.IOException;

/**
 * Opens the "Open..." dialog.
 */
public class OpenExternalFolderDialogAction extends InstrumentedAction implements IWorkbenchAction {

  /**
   * A visitor that checks for the build directory and marks it as derived.
   */
  class BuildDirectoryFinder implements IResourceProxyVisitor {

    @Override
    public boolean visit(IResourceProxy proxy) throws CoreException {
      if (proxy.getType() == IResource.FOLDER) {
        if (proxy.getName().equals(DartCore.BUILD_DIRECTORY_NAME)) {
          IFolder folder = (IFolder) proxy.requestResource();
          if (DartCore.isBuildDirectory(folder)) {
            folder.setDerived(true, null);
            try {
              DartCore.addToIgnores(folder);
            } catch (IOException e) {

            }
          }
          return false;
        }
        return true;
      }
      return true;
    }
  }

  private static final String ACTION_ID = "com.google.dart.tools.ui.folder.open"; //$NON-NLS-1$

  private final IWorkbenchWindow window;

  public static final String DIALOGSTORE_LAST_DIR = DartUI.class.getPackage().getName()
      + ".last.dir"; //$NON-NLS-1$

  public OpenExternalFolderDialogAction(IWorkbenchWindow window) {
    this.window = window;

    setText(ActionMessages.OpenExistingFolderWizardAction_text);
    setDescription(ActionMessages.OpenExistingFolderWizardAction_description);
    setToolTipText(ActionMessages.OpenExistingFolderWizardAction_tooltip);
    setId(ACTION_ID);
  }

  @Override
  public void dispose() {

  }

  @Override
  public void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    IDialogSettings dialogSettings = DartToolsPlugin.getDefault().getDialogSettings();

    DirectoryDialog dialog = new DirectoryDialog(window.getShell(), SWT.SHEET);
    dialog.setText(DialogMessages.OpenFolderDialog_title);
    dialog.setMessage(DialogMessages.OpenFolderDialog_dialogMessage);
    dialog.setFilterPath(dialogSettings.get(OpenExternalFolderDialogAction.DIALOGSTORE_LAST_DIR));
    String directory = dialog.open();
    if (directory == null) {
      instrumentation.metric("OpenExternalFolderDialog", "Cancelled");
      return;
    }
    instrumentation.metric("OpenExternalFolderDialog", "OK");

    directory = TextProcessor.process(directory);
    if (directory.startsWith("~")) {
      String home = System.getProperty("user.home");
      directory = new File(new File(home), directory.substring(1)).toString();
    }
    instrumentation.data("OpenExternalFolder", directory);

    File directoryFile = new File(directory);
    if (!DirectoryVerification.validateOpenDirectoryLocation(window.getShell(), directoryFile)) {
      instrumentation.metric("DirectoryValidation", "Failed");
      return;
    }
    dialogSettings.put(OpenExternalFolderDialogAction.DIALOGSTORE_LAST_DIR, directory);

    CreateAndRevealProjectAction createAction = new CreateAndRevealProjectAction(window, directory);
    createAction.run();
    IProject project = createAction.getProject();

    // TODO: project can be null; this indicates that we didn't do any work when the user hit OK.
    // This should be communicated to the user.

    if (project != null) {
      instrumentation.metric("ProjectCreation", "Success");
      String projectName = project.getName();
      instrumentation.data("ProjectName", projectName);
      // show analysis progress dialog for open folder
      ScanCallbackProvider.setNewProjectName(projectName);
      try {
        project.accept(new BuildDirectoryFinder(), IResource.DEPTH_INFINITE);
      } catch (CoreException e) {
        DartCore.logError(e);
      }
    }
  }
}

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
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.dialogs.OpenFolderDialog;
import com.google.dart.tools.ui.internal.util.DirectoryVerification;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import java.io.File;

/**
 * Opens the "Open..." dialog.
 */
public class OpenExternalFolderDialogAction extends AbstractInstrumentedAction implements
    IWorkbenchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.folder.open"; //$NON-NLS-1$

  private final IWorkbenchWindow window;

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
  public void run() {

    OpenFolderDialog openFolderDialog = new OpenFolderDialog(window.getShell());
    if (openFolderDialog.open() == Window.OK) {
      String directory = openFolderDialog.getFolderLocation();

      if (directory == null) {
        return;
      }

      File directoryFile = new File(directory);

      if (DirectoryVerification.validateOpenDirectoryLocation(window.getShell(), directoryFile)) {

        IDialogSettings dialogSettings = DartToolsPlugin.getDefault().getDialogSettings();
        dialogSettings.put(OpenFolderDialog.DIALOGSTORE_LAST_DIR, directory);

        CreateAndRevealProjectAction createAction = new CreateAndRevealProjectAction(
            window,
            directory);
        createAction.run();

        if (openFolderDialog.isRunpub()) {
          IProject project = createAction.getProject();
          if (project != null && project.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {
            RunPubAction runPubAction = RunPubAction.createPubInstallAction(window);
            runPubAction.run(new StructuredSelection(project));
          }
        }
      }

    }
  }

}

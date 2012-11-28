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
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Action that runs pub commands on the selected project
 */
public class RunPubAction extends SelectionDispatchAction {

  public static RunPubAction createPubInstallAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.INSTALL_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Install"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.INSTALL_COMMAND));
    return action;
  }

  public static RunPubAction createPubUpdateAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.UPDATE_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Update"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.UPDATE_COMMAND));
    return action;
  }

  private String command;

  private RunPubAction(IWorkbenchWindow window, String command) {
    super(window);
    this.command = command;
  }

  @Override
  public void run(ISelection selection) {
    if (selection instanceof ITextSelection) {
      IWorkbenchPage page = DartToolsPlugin.getActivePage();
      if (page != null) {
        IEditorPart part = page.getActiveEditor();
        if (part != null) {
          IEditorInput editorInput = part.getEditorInput();
          DartProject dartProject = EditorUtility.getDartProject(editorInput);
          if (dartProject != null) {
            IProject project = dartProject.getProject();
            runPubJob(project);
          }
        }
      }

    }

  }

  @Override
  public void run(IStructuredSelection selection) {
    if (!selection.isEmpty() && selection.getFirstElement() instanceof IResource) {
      Object object = selection.getFirstElement();
      if (object instanceof IFile) {
        object = ((IFile) object).getParent();
      }
      runPubJob((IContainer) object);
    } else {
      MessageDialog.openError(
          getShell(),
          ActionMessages.RunPubAction_fail,
          ActionMessages.RunPubAction_fileNotFound);
    }
  }

  private void runPubJob(IContainer container) {
    if (container.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {
      RunPubJob runPubJob = new RunPubJob(container, command);
      runPubJob.schedule();
    } else {
      MessageDialog.openError(
          getShell(),
          ActionMessages.RunPubAction_fail,
          ActionMessages.RunPubAction_fileNotFound);
    }
  }

}

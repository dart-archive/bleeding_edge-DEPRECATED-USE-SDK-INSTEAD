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

package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.general.AdapterUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * An action to enable and disable running build.dart.
 */
public class EnableDartBuilderAction extends SelectionListenerAction {
  private final Shell shell;
  private IFile resource;

  protected EnableDartBuilderAction(Shell shell) {
    super(FilesViewMessages.EnableDartBuilderAction_dontAutoRunBuilder_label);
    this.shell = shell;
  }

  @Override
  public void run() {
    try {
      if (resource != null) {
        toggleState(resource);
        updateLabel();
      }
    } catch (CoreException e) {
      MessageDialog.openError(shell, "Error Performing Operation", e.getMessage()); //$NON-NLS-1$
    }
  }

  protected boolean shouldBeEnabled() {
    if (resource == null) {
      return false;
    }

    if (resource.getName().equals(DartCore.BUILD_DART_FILE_NAME)
        && resource.getParent() instanceof IProject) {
      return true;
    }

    return false;
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    resource = AdapterUtilities.getAdapter(selection.getFirstElement(), IFile.class);

    updateLabel();

    return resource != null;
  }

  void updateLabel() {
    if (resource != null) {
      boolean currentState = DartCore.getPlugin().getDisableDartBasedBuilder(resource.getProject());

      if (currentState) {
        setText(FilesViewMessages.EnableDartBuilderAction_autoRunBuilder_label);
        return;
      }
    }

    setText(FilesViewMessages.EnableDartBuilderAction_dontAutoRunBuilder_label);
  }

  private void toggleState(IResource resource) throws CoreException {
    IProject project = resource.getProject();

    boolean currentState = DartCore.getPlugin().getDisableDartBasedBuilder(project);

    DartCore.getPlugin().setDisableDartBasedBuilder(project, !currentState);
  }

}

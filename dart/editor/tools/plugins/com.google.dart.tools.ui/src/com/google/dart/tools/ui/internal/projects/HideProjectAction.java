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
package com.google.dart.tools.ui.internal.projects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.actions.CloseResourceAction;

/**
 * Standard action for hiding the currently selected project(s).
 */
public class HideProjectAction extends CloseResourceAction {

  /**
   * Create the action.
   */
  public HideProjectAction(IShellProvider shellProvider) {
    super(shellProvider, ProjectMessages.HideProjectAction_text);
    setToolTipText(ProjectMessages.HideProjectAction_tooltip);
  }

  @Override
  public void run() {

    super.run();

  }

  @Override
  protected String getOperationMessage() {
    return ProjectMessages.HideProjectAction_operation_msg;
  }

  @Override
  protected String getProblemsMessage() {
    return ProjectMessages.HideProjectAction_problems_msg;
  }

  @Override
  protected String getProblemsTitle() {
    return ProjectMessages.HideProjectAction_problems_title;
  }

  @Override
  protected void invokeOperation(IResource resource, IProgressMonitor monitor)
      throws CoreException {
    ((IProject) resource).delete(false, true, monitor);
  }

}

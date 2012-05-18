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
package com.google.dart.tools.ui.build;

import com.google.dart.tools.ui.internal.util.CleanLibrariesJob;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Clean the Dart libraries in the workspace and delete the index.
 */
public class CleanLibrariesAction extends Action implements ActionFactory.IWorkbenchAction {
  private static final String ACTION_ID = "buildClean"; //$NON-NLS-1$

  /**
   * Creates a new BuildCleanAction
   * 
   * @param window The window for parenting this action
   */
  public CleanLibrariesAction(IWorkbenchWindow window) {
    super(BuildMessages.CleanLibrariesAction_rebuildAll);

    setId(ACTION_ID);
  }

  @Override
  public void dispose() {

  }

  @Override
  public void run() {
    Job job = new CleanLibrariesJob();

    job.schedule();
  }

}

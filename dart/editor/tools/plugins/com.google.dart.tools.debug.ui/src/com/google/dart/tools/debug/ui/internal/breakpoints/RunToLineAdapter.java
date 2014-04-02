/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

// TODO: use the RunToLineHandler class

/**
 * An adapter to implement the run-to-line functionality.
 */
public class RunToLineAdapter implements IRunToLineTarget {

  public RunToLineAdapter() {

  }

  @Override
  public boolean canRunToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target) {
    // TODO(devoncarew): Implement this for command-line and Dartium debug targets.

    return false;
  }

  @Override
  public void runToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target)
      throws CoreException {
    // TODO(devoncarew): Implement.

  }
}

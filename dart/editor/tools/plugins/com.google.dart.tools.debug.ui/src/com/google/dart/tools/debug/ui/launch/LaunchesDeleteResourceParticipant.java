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

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle removing Dart launches.
 */
public class LaunchesDeleteResourceParticipant extends DeleteParticipant {

  private class TerminateLaunchesChange extends Change {

    private TerminateLaunchesChange() {

    }

    @Override
    public Object getModifiedElement() {
      return file;
    }

    @Override
    public String getName() {
      return "Terminate launches for '" + file.getFullPath() + "'";
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {

    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException {
      // Return an OK refactoring status.
      return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
      for (ILaunch launch : launches) {
        if (launch.isTerminated()) {
          continue;
        }

        launch.terminate();
      }

      return null;
    }
  }

  private static List<ILaunch> getLaunchesForFile(IFile file) {
    List<ILaunch> launches = new ArrayList<ILaunch>();

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    for (ILaunch launch : manager.getLaunches()) {
      if (launch.isTerminated()) {
        continue;
      }

      ILaunchConfiguration config = launch.getLaunchConfiguration();
      DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);

      IResource launchedResource = wrapper.getApplicationResource();

      if (launchedResource != null && launchedResource.equals(file)) {
        launches.add(launch);
      }
    }

    return launches;
  }

  private IFile file;

  private List<ILaunch> launches;

  public LaunchesDeleteResourceParticipant() {

  }

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new TerminateLaunchesChange();
  }

  @Override
  public String getName() {
    return "Remove Dart launches";
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IFile) {
      file = (IFile) element;

      launches = getLaunchesForFile(file);

      return launches.size() > 0;
    } else {
      return false;
    }
  }

}

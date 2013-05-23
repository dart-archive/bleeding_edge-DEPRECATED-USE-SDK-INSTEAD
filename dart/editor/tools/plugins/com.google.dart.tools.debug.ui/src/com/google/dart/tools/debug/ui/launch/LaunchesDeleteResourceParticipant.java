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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle deleting Dart launches.
 */
public class LaunchesDeleteResourceParticipant extends DeleteParticipant {

  private class DeleteLaunchesChange extends Change {

    private DeleteLaunchesChange() {

    }

    @Override
    public Object getModifiedElement() {
      return file;
    }

    @Override
    public String getName() {
      return "Delete launch configs for '" + file.getFullPath() + "'";
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {

    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException {
      return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
      for (ILaunchConfiguration config : launchConfigs) {
        config.delete();
      }

      return null;
    }
  }

  private static List<ILaunchConfiguration> getLaunchesForFile(IFile file) throws CoreException {
    List<ILaunchConfiguration> configs = new ArrayList<ILaunchConfiguration>();

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    for (ILaunchConfiguration config : manager.getLaunchConfigurations()) {
      DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);

      IResource launchedResource = wrapper.getApplicationResource();

      if (launchedResource != null && launchedResource.equals(file)) {
        configs.add(config);
      }
    }

    return configs;
  }

  private IFile file;

  private List<ILaunchConfiguration> launchConfigs;

  public LaunchesDeleteResourceParticipant() {

  }

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new DeleteLaunchesChange();
  }

  @Override
  public String getName() {
    return "Delete Dart launches";
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IFile) {
      file = (IFile) element;

      try {
        launchConfigs = getLaunchesForFile(file);
      } catch (CoreException e) {
        return false;
      }

      return launchConfigs.size() > 0;
    } else {
      return false;
    }
  }

}

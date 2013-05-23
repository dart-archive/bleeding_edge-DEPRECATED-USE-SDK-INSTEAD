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
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle renaming Dart launches.
 */
public class LaunchesRenameResourceParticipant extends RenameParticipant {

  private static class UpdateLaunchConfigurations extends NullChange {
    private final List<ILaunchConfiguration> configurations;
    private final String newName;
    private final String oldName;

    public UpdateLaunchConfigurations(List<ILaunchConfiguration> configurations, String newName) {
      super("Update launch configuration");

      this.configurations = configurations;
      this.newName = newName;
      this.oldName = new DartLaunchConfigWrapper(configurations.get(0)).getApplicationName();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
      for (ILaunchConfiguration config : configurations) {
        ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
        DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(wc);

        wrapper.setApplicationName(newName);
        wc.doSave();
      }

      // return undo
      return new UpdateLaunchConfigurations(configurations, oldName);
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

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    String newName = getArguments().getNewName();
    IFile newFile = file.getParent().getFile(new Path(newName));

    return new UpdateLaunchConfigurations(launchConfigs, newFile.getFullPath().toPortableString());
  }

  @Override
  public String getName() {
    return "Rename Dart launches";
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

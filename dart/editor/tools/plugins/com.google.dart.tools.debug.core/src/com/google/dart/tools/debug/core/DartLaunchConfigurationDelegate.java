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
package com.google.dart.tools.debug.core;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/**
 * Super class for all Dart launch configuration delegates
 */
public abstract class DartLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

  @Override
  public final boolean buildForLaunch(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return false;
  }

  public abstract void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException;

  @Override
  public final void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    InstrumentationBuilder instrumentation = Instrumentation.builder(this.getClass());
    try {

      instrumentation.metric("Mode", mode);

      doLaunch(configuration, mode, launch, monitor, instrumentation);

    } catch (CoreException e) {
      DebugUIHelper.getHelper().showError("Error Launching Application", e.getMessage());
    } finally {
      instrumentation.log();
    }

  }

  @Override
  protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode)
      throws CoreException {
    // indicate which project to save before launch
    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);
    IResource resource = launchConfig.getApplicationResource();
    if (resource != null) {
      return new IProject[] {resource.getProject()};
    }
    return null;
  }
}

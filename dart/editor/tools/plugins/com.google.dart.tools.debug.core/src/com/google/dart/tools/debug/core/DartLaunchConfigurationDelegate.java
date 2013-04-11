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
import com.google.dart.tools.core.pub.PubBuildParticipant;

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

  public static boolean LAUNCH_WAIT_FOR_BUILD = true;

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
  public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {

    // Check to see if the user wants to save any dirty editors before launch
    if (!super.preLaunchCheck(configuration, mode, monitor)) {
      return false;
    }

    if (!LAUNCH_WAIT_FOR_BUILD) {
      // Run pub install for this application if necessary
      DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);
      IResource res = launchConfig.getApplicationResource();
      if (res == null) {
        res = launchConfig.getProject();
      }
      if (res != null) {
        new PubBuildParticipant().runPubFor(res, monitor);
        // TODO (danrubel): run build.dart for this application if necessary
      }
    }

    return true;
  }

  @Override
  protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode)
      throws CoreException {

    if (!LAUNCH_WAIT_FOR_BUILD) {
      // Rely on the preLaunchCheck method above to run pub and build.dart before launch
      return new IProject[0];

    } else {
      // indicate which project to save before launch
      DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);
      IResource resource = launchConfig.getApplicationResource();
      if (resource != null) {
        return new IProject[] {resource.getProject()};
      }
      return null;
    }
  }
}

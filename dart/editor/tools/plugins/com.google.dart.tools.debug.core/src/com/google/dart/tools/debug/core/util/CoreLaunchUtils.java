/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.core.util;

import com.google.dart.tools.debug.core.source.DartSourceLookupDirector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;

/**
 * A set of utility methods for managing ILaunches.
 */
public class CoreLaunchUtils {

  /**
   * Adds the specified launch and notifies listeners. Has no effect if an identical launch is
   * already registered. This is a convenience method for a call to ILaunchManager.addLaunch().
   * 
   * @param launch the launch to add
   */
  public static void addLaunch(ILaunch launch) {
    DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
  }

  /**
   * Create a temporary launch with the given id and name. This launch is not yet registered with
   * the ILaunchManager.
   * 
   * @param id
   * @param name
   * @return
   * @throws CoreException
   */
  public static ILaunch createTemporaryLaunch(String id, String name) throws CoreException {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(id);
    ILaunchConfigurationWorkingCopy launchConfig = type.newInstance(
        null,
        manager.generateLaunchConfigurationName(name));

    // TODO(devoncarew): is this necessary?
    launchConfig.delete();

    DartSourceLookupDirector sourceLookupDirector = new DartSourceLookupDirector();
    sourceLookupDirector.initializeDefaults(launchConfig);

    return new Launch(launchConfig, ILaunchManager.DEBUG_MODE, sourceLookupDirector);
  }

  /**
   * Removes the specified launch and notifies listeners. Has no effect if an identical launch is
   * not already registered. This is a convenience method for a call to
   * ILaunchManager.removeLaunch().
   * 
   * @param launch the launch to remove
   */
  public static void removeLaunch(ILaunch launch) {
    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
  }

  private CoreLaunchUtils() {

  }

}

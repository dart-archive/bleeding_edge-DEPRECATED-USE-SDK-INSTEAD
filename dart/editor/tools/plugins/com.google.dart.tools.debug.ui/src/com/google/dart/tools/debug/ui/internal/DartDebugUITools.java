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
package com.google.dart.tools.debug.ui.internal;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants2;

/**
 * This class provides utilities for clients of the debug UI.
 */
public class DartDebugUITools {
  /**
   * Launches the given launch configuration in the specified mode in a background Job with progress
   * reported via the Job. Exceptions are reported in the Progress view.
   * 
   * @param configuration the configuration to launch
   * @param mode launch mode
   */
  public static void launch(ILaunchConfiguration config, String mode) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS && !DartLaunchConfigurationDelegate.LAUNCH_WAIT_FOR_BUILD) {
      // This builds only what's necessary for this launch before launching
      launchInBackground(config, mode);
    } else {
      // This waits for all build jobs to complete before launching
      DebugUITools.launch(config, mode);
    }
  }

  /**
   * Launches the given launch configuration in the specified mode in a background Job with progress
   * reported via the Job. Exceptions are reported in the Progress view.
   * 
   * @param configuration the configuration to launch
   * @param mode launch mode
   */
  private static void launchInBackground(final ILaunchConfiguration configuration, final String mode) {
    Job job = new Job("Running pub before launch") {
      @Override
      public IStatus run(final IProgressMonitor monitor) {
        monitor.beginTask("Launching" /* DebugUIMessages.DebugUITools_3 */, 100);
        try {
          DebugUITools.buildAndLaunch(configuration, mode, monitor);
        } catch (CoreException e) {
          final IStatus status = e.getStatus();
          IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);
          if (handler == null) {
            return status;
          }
          final ILaunchGroup group = DebugUITools.getLaunchGroup(configuration, mode);
          if (group == null) {
            return status;
          }
          final Display display = PlatformUI.getWorkbench().getDisplay();
          Runnable r = new Runnable() {
            @Override
            public void run() {
              DebugUITools.openLaunchConfigurationDialogOnGroup(
                  display.getActiveShell(),
                  new StructuredSelection(configuration),
                  group.getIdentifier(),
                  status);
            }
          };
          display.asyncExec(r);
        } finally {
          monitor.done();
        }
        return Status.OK_STATUS;
      }
    };
    job.setPriority(Job.INTERACTIVE);
    job.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
    job.schedule();
  }
}

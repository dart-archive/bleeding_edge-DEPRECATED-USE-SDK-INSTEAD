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
package com.google.dart.eclipse;

import com.google.dart.eclipse.ui.internal.jobs.ValidateSDKJob;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;

/**
 * The plugin activator for the com.google.dart.eclipse.ui plugin.
 */
public class DartEclipseUI extends Plugin {

  private static final int SDK_VALIDATION_JOB_INIT_DELAY = 1500;

  //The activated plugin
  private static DartEclipseUI PLUGIN;

  /**
   * The Dart Eclipse UI plugin id.
   */
  private static final String PLUGIN_ID = "com.google.dart.eclipse.ui";

  /**
   * Log the given message as an error to the Eclipse log.
   * 
   * @param message the message
   */
  public static void logError(String message) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param message the message
   * @param exception the exception
   */
  public static void logError(String message, Throwable exception) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param exception the exception to log
   */
  public static void logError(Throwable exception) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, exception.getMessage(), exception));
    }
  }

  /**
   * Log the given message as a warning to the Eclipse log.
   * 
   * @param message the message to log
   */
  public static void logWarning(String message) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception as a warning in the Eclipse log.
   * 
   * @param message the message
   * @param exception the exception
   */
  public static void logWarning(String message, Throwable exception) {
    if (PLUGIN != null) {
      PLUGIN.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, exception));
    }
  }

  private Job validateSDKJob;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    validateSDKJob = new ValidateSDKJob() {
      @Override
      protected IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
        //make doubly sure that the workbench is up and running 
        if (PlatformUI.isWorkbenchRunning()) {
          try {
            super.run(monitor);
          } finally {
            validateSDKJob = null;
          }
        } else {
          schedule(500);
        }

        return Status.OK_STATUS;
      }
    };

    validateSDKJob.schedule(SDK_VALIDATION_JOB_INIT_DELAY);

  }

  @Override
  public void stop(BundleContext context) throws Exception {

    if (validateSDKJob != null) {
      validateSDKJob.cancel();
    }

    super.stop(context);

    PLUGIN = null;
  }

}

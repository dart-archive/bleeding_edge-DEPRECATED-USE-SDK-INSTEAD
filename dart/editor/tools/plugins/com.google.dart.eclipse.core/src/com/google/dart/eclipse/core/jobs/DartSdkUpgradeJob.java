/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.eclipse.core.jobs;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

import java.io.File;

/**
 * Download and install the latest released version of the Dart SDK.
 */
public class DartSdkUpgradeJob extends Job {

  /**
   * Ensure that the execute bit is set on the given files.
   * 
   * @param files the files that should be executable
   */
  public static void ensureExecutable(File... files) {
    if (files != null) {
      for (File file : files) {
        if (file != null && !file.canExecute()) {
          if (!makeExecutable(file)) {
            DartCore.logError("Could not make " + file.getAbsolutePath() + " executable");
          }
        }
      }
    }
  }

  private static boolean makeExecutable(File file) {

    DartCore.logInformation("Setting execute bit for " + file.getAbsolutePath());

    // First try and set executable for all users.
    if (file.setExecutable(true, false)) {
      // success
      return true;
    }

    // Then try only for the current user.
    return file.setExecutable(true, true);
  }

  // TODO (danrubel): Remove this if not referenced from the plugin.xml
  public DartSdkUpgradeJob() {
    super("Downloading Dart SDK");

    setRule(ResourcesPlugin.getWorkspace().getRoot());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IStatus status = DartSdkManager.getManager().upgrade(monitor);

    if (!status.isOK()) {
      if (status.getException() != null) {
        DartCore.logError(status.getException());
      }
    }
    if (status.isOK() && DartSdkManager.getManager().hasSdk()) {
      ensureExecutable(new File(DartSdkManager.getManager().getSdk().getDirectory(), "bin").listFiles());
    }
    return status;
  }
}

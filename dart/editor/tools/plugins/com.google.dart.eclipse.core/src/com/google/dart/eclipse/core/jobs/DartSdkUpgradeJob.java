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

/**
 * Download and install the latest released version of the Dart SDK.
 */
public class DartSdkUpgradeJob extends Job {

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

    return status;
  }

}

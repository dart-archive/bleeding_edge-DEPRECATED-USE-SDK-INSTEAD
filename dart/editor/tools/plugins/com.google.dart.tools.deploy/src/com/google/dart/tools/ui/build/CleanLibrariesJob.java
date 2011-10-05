/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.build;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Clean all workspace projects and rebuild the index.
 */
public class CleanLibrariesJob extends Job {

  public CleanLibrariesJob() {
    super(BuildMessages.CleanLibrariesJob_cleanLibrariesProgress);

    setRule(ResourcesPlugin.getWorkspace().getRoot());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    } catch (CoreException ex) {
      return ex.getStatus();
    }

    return Status.OK_STATUS;
  }

}

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
package com.google.dart.tools.core.jobs;

import com.google.dart.engine.utilities.instrumentation.HealthUtils;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Clean all workspace projects and rebuild the index.
 * 
 * @coverage dart.tools.core
 */
public class CleanLibrariesJob extends Job {

  public CleanLibrariesJob() {
    super("Reanalyzing...");

    setRule(ResourcesPlugin.getWorkspace().getRoot());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {

    HealthUtils.ReportHealth("CleanLibrariesJob.run");

    try {

      SubMonitor subMonitor = SubMonitor.convert(monitor, "Reanalyzing...", 100);

      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root = workspace.getRoot();

      // Clear stale error messages from the console
      DartCore.getConsole().clear();

      // Refresh the workspace.
      root.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(50));

      for (IProject project : root.getProjects()) {
        if (project.isOpen()) {
          project.build(IncrementalProjectBuilder.CLEAN_BUILD, subMonitor.newChild(1));
        }
      }

      subMonitor.done();
    } catch (CoreException ex) {
      return ex.getStatus();
    }

    return Status.OK_STATUS;
  }
}

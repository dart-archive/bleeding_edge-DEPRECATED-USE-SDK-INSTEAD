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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

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
 */
public class CleanLibrariesJob extends Job {

  public CleanLibrariesJob() {
    super(Messages.CleanLibrariesJob_Title);

    setRule(ResourcesPlugin.getWorkspace().getRoot());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.CleanLibrariesJob_Title, 100);

      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root = workspace.getRoot();

      // Refresh the workspace.
      root.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(50));

      // Clean the projects, forcing a rebuild.
      if (DartCoreDebug.ANALYSIS_SERVER) {
        root.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        SystemLibraryManagerProvider.getDefaultAnalysisServer().reanalyze();
      } else {
        workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, subMonitor.newChild(50));
      }

      subMonitor.done();
    } catch (CoreException ex) {
      return ex.getStatus();
    }

    return Status.OK_STATUS;
  }
}

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
package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.LightweightModel;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Run pub build command for the project
 */
public class RunPubBuildHandler extends AbstractHandler {

  class PubBuildAndLaunchJob extends Job {

    public PubBuildAndLaunchJob() {
      super("Build and launch ...");

    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

      monitor.subTask("Running pub build ...");
      RunPubJob job = new RunPubJob(
          workingDir,
          RunPubJob.BUILD_COMMAND,
          false,
          resource.getParent());
      job.run(monitor);
      // now launch from build dir
      try {
        resource.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
      } catch (CoreException e) {
        DartCore.logError("Refreshing " + resource.getProject().getName(), e);
      }

      // find resource to launch in build folder and launch
      monitor.subTask("Launching...");
      final IResource launchResource = getLaunchableResource(resource);
      if (launchResource != null) {
        Display.getDefault().asyncExec(new Runnable() {

          @Override
          public void run() {
            ILaunchShortcut shortcut = LaunchUtils.getBrowserLaunchShortcut();
            ISelection selection = new StructuredSelection(launchResource);
            shortcut.launch(selection, ILaunchManager.RUN_MODE);
          }
        });
      } else {
        DartCore.getConsole().println(
            "Build and lunch .. Could not find file to launch. \n "
                + "Select file in \"build\" directory and run in browser");
      }
      return Status.OK_STATUS;
    }

    private IResource getLaunchableResource(IResource resource) {
      if (!(resource instanceof IFile)) {
        return null;
      }
      IContainer buildDir = (IContainer) workingDir.findMember(DartCore.BUILD_DIRECTORY_NAME);
      IPath path = resource.getFullPath();
      int index = path.segmentCount() - 1;
      while (index > 0 && !DartCore.pubDirectories.contains(path.segment(index))) {
        index--;
      }
      if (index > 0 && buildDir.exists()) {
        if (DartCore.isDartLikeFileName(resource.getName())) {
          LightweightModel model = LightweightModel.getModel();
          IFile htmlFile = model.getHtmlFileForLibrary((IFile) resource);
          if (htmlFile == null) {
            return null;
          }
          return buildDir.findMember(path.removeFirstSegments(index).removeLastSegments(1).toString()
              + "/" + htmlFile.getName());
        } else {
          return buildDir.findMember(path.removeFirstSegments(index));
        }
      }
      return null;
    }
  }

  private IContainer workingDir;

  private IResource resource;

  public RunPubBuildHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getActivePart(event).getSite().getSelectionProvider().getSelection();
    if (!selection.isEmpty()) {
      if (selection instanceof IStructuredSelection) {
        Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
        if (selectedObject instanceof IResource) {
          resource = (IResource) selectedObject;
          PubFolder folder = LightweightModel.getModel().getPubFolder((IResource) selectedObject);
          if (folder != null) {
            workingDir = folder.getResource();
            new PubBuildAndLaunchJob().schedule();
            return null;
          }
          DartCore.getConsole().println(
              "Error: Could not run pub build. Use Run Tools > Pub Build to build");
        }
      }
    }
    return null;
  }
}

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
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.pub.RunPubJob;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Run the build.dart with deploy option which compiles the Polymer app to JavaScript.
 */
public class DeployPolymerAppHandler extends AbstractHandler {

  public DeployPolymerAppHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getActivePart(event).getSite().getSelectionProvider().getSelection();
    if (!selection.isEmpty()) {
      if (selection instanceof IStructuredSelection) {
        Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
        if (selectedObject instanceof IResource) {
          PubFolder folder = DartCore.getProjectManager().getPubFolder((IResource) selectedObject);
          if (folder != null) {
            RunPubJob job = new RunPubJob(folder.getResource(), RunPubJob.BUILD_NOMINIFY_COMMAND);
            job.schedule(0);
            return null;
          }
          DartCore.getConsole().println(
              "Error: Could not run pub build. Use Run Tools > Pub Build to build Polymer app");
        }
      }
    }
    return null;
  }
}

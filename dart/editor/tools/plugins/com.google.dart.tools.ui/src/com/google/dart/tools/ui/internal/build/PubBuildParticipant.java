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
package com.google.dart.tools.ui.internal.build;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.builder.DartBuildParticipant;
import com.google.dart.tools.ui.actions.RunPubAction;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.util.Map;

/**
 * This build participant is called from the DartBuilder before the dart project is built. It will
 * run pub install if the pubspec file has changed.
 */
public class PubBuildParticipant implements DartBuildParticipant {

  @Override
  public void build(int kind, Map<String, String> args, IResourceDelta delta,
      final IProgressMonitor monitor) throws CoreException {

    if (!DartCore.isWindowsXp()) {
      // check if pubspec has changed, if so invoke pub install
      if (delta != null && delta.getKind() == IResourceDelta.CHANGED) {

        delta.accept(new IResourceDeltaVisitor() {
          @Override
          public boolean visit(IResourceDelta delta) {
            final IResource resource = delta.getResource();
            if (resource.getType() != IResource.FILE) {
              return true;
            }
            // TODO(keertip): optimize for just changes in dependencies
            if (resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
              if (PlatformUI.getWorkbench().getWorkbenchWindows().length > 0) {
                runPubAction(resource);
              }
              monitor.done();
            }
            return false;
          }
        });
      }
    }
  }

  @Override
  public void clean(IProject project, IProgressMonitor monitor) throws CoreException {
    // do nothing

  }

  protected void runPubAction(final IResource resource) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        RunPubAction runPubAction = RunPubAction.createPubInstallAction(window);
        runPubAction.run(new StructuredSelection(resource));
      }
    });
  }
}

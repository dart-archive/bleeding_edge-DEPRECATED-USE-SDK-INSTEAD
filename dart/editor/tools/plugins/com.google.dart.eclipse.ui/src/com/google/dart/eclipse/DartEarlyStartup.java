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

import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.model.DartSdkUpgradeJob;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Perspective;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;

import java.util.ArrayList;

/**
 * Check for and possibly install a Dart SDK.
 */
@SuppressWarnings("restriction")
public class DartEarlyStartup implements IStartup {

  public DartEarlyStartup() {

  }

  @Override
  public void earlyStartup() {

    // remove the debug action set toolbar items from Dart perspective
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow != null) {
          workbenchWindow.addPerspectiveListener(new PerspectiveAdapter() {

            @Override
            public void perspectiveActivated(IWorkbenchPage page,
                IPerspectiveDescriptor perspectiveDescriptor) {
              super.perspectiveActivated(page, perspectiveDescriptor);
              if (perspectiveDescriptor.getId().indexOf("com.google.dart.tools.ui.DartPerspective") > -1) {
                if (workbenchWindow.getActivePage() instanceof WorkbenchPage) {
                  WorkbenchPage worbenchPage = (WorkbenchPage) workbenchWindow.getActivePage();
                  // Get the perspective
                  Perspective perspective = worbenchPage.findPerspective(perspectiveDescriptor);
                  ArrayList<IActionSetDescriptor> toRemove = new ArrayList<IActionSetDescriptor>();
                  if (perspective != null) {
                    for (IActionSetDescriptor actionSetDescriptor : perspective.getAlwaysOnActionSets()) {
                      if (actionSetDescriptor.getId().indexOf("org.eclipse.debug.ui.debugActionSet") > -1) {
                        // Add the action set descriptor to the list of the action sets to remove
                        toRemove.add(actionSetDescriptor);
                      }
                    }
                    perspective.turnOffActionSets(toRemove.toArray(new IActionSetDescriptor[toRemove.size()]));
                  }
                }
              }
            }
          });
        }
      }
    });

    DartSdkManager manager = DartSdkManager.getManager();

    if (!manager.hasSdk()) {
      DartSdkUpgradeJob job = new DartSdkUpgradeJob();

      // TOOD(devoncarew): terminate this job on plugin shutdown
      job.schedule();
    }
  }

}

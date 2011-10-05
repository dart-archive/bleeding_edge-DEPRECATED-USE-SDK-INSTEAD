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
package com.google.dart.tools.deploy;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/*
 * This class controls all aspects of the application's execution.
 */
public class DartIDEApplication implements IApplication {
  @Override
  public Object start(IApplicationContext context) throws Exception {
    Display display = PlatformUI.createDisplay();
    // processor must be created before we start event loop
    DelayedEventsProcessor processor = new DelayedEventsProcessor(display);

    try {
      //
      // get application arguments 
      // String args[] = Platform.getApplicationArgs(); 
      // for (String arg : args) {
      //    if (arg.equals("-run")){ 
      //       System.out.println("Hello"); 
      //     } 
      // }

      int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor(
          processor));
      if (returnCode == PlatformUI.RETURN_RESTART) {
        return IApplication.EXIT_RESTART;
      } else {
        return IApplication.EXIT_OK;
      }
    } finally {
      display.dispose();
    }

  }

  @Override
  public void stop() {
    if (!PlatformUI.isWorkbenchRunning()) {
      return;
    }
    final IWorkbench workbench = PlatformUI.getWorkbench();
    final Display display = workbench.getDisplay();
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (!display.isDisposed()) {
          workbench.close();
        }
      }
    });
  }

}

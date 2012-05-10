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
package com.google.dart.tools.deploy;

import com.google.dart.tools.core.internal.perf.DartEditorCommandLineManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.util.ArrayList;

/**
 * This class controls all aspects of the application's execution.
 */
public class DartIDEApplication implements IApplication {
  @Override
  public Object start(IApplicationContext context) throws Exception {
    Display display = PlatformUI.createDisplay();
    // processor must be created before we start event loop
    DelayedEventsProcessor processor = new DelayedEventsProcessor(display);

    try {
      parseApplicationArgs();

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

  /**
   * Parse the command line arguments, and store them in {@link DartEditorCommandLineManager}.
   */
  private void parseApplicationArgs() {
    // get application arguments 
    String args[] = Platform.getApplicationArgs();
//    for (String arg : args) {
//      if (arg.equals("-hello")) {
//        System.out.println("Hello");
//      }
//    }
    ArrayList<File> fileSet = new ArrayList<File>(args.length);
    // if we find the PERF_FLAG at any point, set DartPerformance.MEASURE_PERFORMANCE to true
    // for all other arguments, add the argument as a java.io.File, if it is a file that exists,
    // and isn't in the set already
    for (String arg : args) {
      if (arg.equals(DartEditorCommandLineManager.PERF_FLAG)) {
        DartEditorCommandLineManager.MEASURE_PERFORMANCE = true;
      } else if (arg.length() > 0 && arg.charAt(0) != '-') {
        File file = new File(arg);
        if (fileSet.contains(file)) {
          continue;
        }
        if (file.exists()) {
          fileSet.add(file);
        } else {
          // else, make an attempt at constructing the file using the relative path to the Dart
          // Editor install directory
          File eclipseInstallFile = new File(Platform.getInstallLocation().getURL().getFile());
          file = new File(eclipseInstallFile, arg);
          if (fileSet.contains(file)) {
            continue;
          }
          if (file.exists()) {
            fileSet.add(file);
          } else {
            System.out.println("Input \"" + arg
                + "\" could not be parsed as a valid file, file inputs on the command line "
                + "need to be absolute paths for files or folders that exist, or relative to "
                + "the directory that the Dart Editor is installed.");
          }
        }
      }
    }
    DartEditorCommandLineManager.setFileSet(fileSet);
  }

}

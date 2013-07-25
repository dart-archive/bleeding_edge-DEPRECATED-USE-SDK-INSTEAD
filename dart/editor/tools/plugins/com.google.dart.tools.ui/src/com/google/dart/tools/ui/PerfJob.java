/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.ui;

import com.google.dart.tools.core.CmdLineOptions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.Method;

/**
 * A Job to run and capture performance statistics. This runs automatically when the application is
 * started with the --perf option.
 */
public class PerfJob extends Job {

  public PerfJob() {
    super("Running perf stats...");
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    CmdLineOptions options = CmdLineOptions.getOptions();

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {

    }

    if (options.getAutoExit()) {
      exit();
    }

    return Status.OK_STATUS;
  }

  private void exit() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbench workbench = PlatformUI.getWorkbench();

        try {
          // try and call workbench.close(int returnCode, final boolean force) boolean {
          Method m = workbench.getClass().getDeclaredMethod("close", int.class, boolean.class);
          m.setAccessible(true);
          m.invoke(workbench, 0, true);
        } catch (Exception e) {
          // else fall back on workbench.close()
          e.printStackTrace();
          workbench.close();
        }
      }
    });
  }

}

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

package com.google.dart.tools.debug.ui.internal.presentation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.model.elements.DebugElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.TreePath;

/**
 * This class exists to allow us to customize the display name of the current ILaunch.
 */
@SuppressWarnings("restriction")
public class DartLaunchElementLabelProvider extends DebugElementLabelProvider {

  public DartLaunchElementLabelProvider() {

  }

  @Override
  protected String getLabel(TreePath elementPath, IPresentationContext presentationContext,
      String columnId) throws CoreException {
    Object element = elementPath.getLastSegment();

    if (element instanceof ILaunch) {
      return getLaunchText((ILaunch) element);
    } else {
      return super.getLabel(elementPath, presentationContext, columnId);
    }
  }

  private String getLaunchText(ILaunch launch) {
    ILaunchConfiguration config = launch.getLaunchConfiguration();

    if (config == null) {
      return "unknown";
    }

    String name = config.getName();

    if (isTerminated(launch)) {
      name = "<" + name + ">";
    }

    IProcess[] processes = launch.getProcesses();

    if (processes.length > 0) {
      IProcess process = processes[0];

      try {
        int exitCode = process.getExitValue();

        name += " exit code=" + exitCode;
      } catch (DebugException ex) {
        // The process has not yet exited.

      }
    }

    return name;
  }

  private boolean isTerminated(ILaunch launch) {
    IProcess[] processes = launch.getProcesses();

    if (processes.length > 0) {
      IProcess process = processes[0];

      try {
        @SuppressWarnings("unused")
        int exitCode = process.getExitValue();

        return true;
      } catch (DebugException ex) {
        // The process has not yet exited.

        return false;
      }
    } else {
      return true;
    }
  }

}

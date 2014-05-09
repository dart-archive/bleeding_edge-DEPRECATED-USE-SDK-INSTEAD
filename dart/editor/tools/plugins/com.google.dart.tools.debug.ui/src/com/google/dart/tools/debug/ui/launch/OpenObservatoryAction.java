/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.swt.widgets.Event;

/**
 * Action to open a browser tab for the Observatory.
 */
public class OpenObservatoryAction extends InstrumentedAction {

  private int portNumber;

  public OpenObservatoryAction() {
    super("Open Observatory");
  }

  public void updateEnablement(ILaunch launch) {
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launch.getLaunchConfiguration());
    if (wrapper.getObservatoryPort() != -1) {
      portNumber = wrapper.getObservatoryPort();
      setEnabled(true);
    } else {
      setEnabled(false);
    }

  }

  @Override
  protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {

    try {
      LaunchUtils.openBrowser("http://localhost:" + portNumber);
    } catch (CoreException e) {
      DartCore.logInformation("Exception while opening browser tab", e);
    }

  }

}

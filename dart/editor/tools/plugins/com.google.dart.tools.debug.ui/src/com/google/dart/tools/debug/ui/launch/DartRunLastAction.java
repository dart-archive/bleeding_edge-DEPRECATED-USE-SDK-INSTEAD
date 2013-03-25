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

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.List;

/**
 * Launches the last application to have run.
 */
public class DartRunLastAction extends DartRunAbstractAction {

  public DartRunLastAction() {
    this(null, false);
  }

  public DartRunLastAction(IWorkbenchWindow window) {
    this(window, false);
  }

  public DartRunLastAction(IWorkbenchWindow window, boolean noMenu) {
    super(window, "Run", noMenu ? IAction.AS_PUSH_BUTTON : IAction.AS_DROP_DOWN_MENU);

    setActionDefinitionId("com.google.dart.tools.debug.ui.run.last");
    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/run_exc.png"));
  }

  public DartRunLastAction(IWorkbenchWindow window, String name, int flags) {
    super(window, name, flags);
  }

  @Override
  protected void doLaunch(UIInstrumentationBuilder instrumentation) {
    try {
      List<ILaunchConfiguration> launches = LaunchUtils.getAllLaunches();

      instrumentation.metric("launches-count", launches.size());

      if (launches.size() != 0) {
        ILaunchConfiguration launchConfig = LaunchUtils.chooseLatest(launches);

        if (launchConfig != null) {
          launch(launchConfig, instrumentation);
        } else {
          instrumentation.metric("Problem", "Launch config was null");
        }
      }
    } catch (Throwable exception) {
      // We need to defensively show all errors coming out of here - the user needs feedback as
      // to why their launch didn't work.
      instrumentation.metric("Problem-Exception", exception.getClass().toString());
      instrumentation.data("Problem-Exception", exception.toString());

      DartUtil.logError(exception);

      DebugErrorHandler.errorDialog(
          window.getShell(),
          "Error During Launch",
          "Internal error during launch - please report this using the feedback mechanism!",
          exception);
    }
  }

}

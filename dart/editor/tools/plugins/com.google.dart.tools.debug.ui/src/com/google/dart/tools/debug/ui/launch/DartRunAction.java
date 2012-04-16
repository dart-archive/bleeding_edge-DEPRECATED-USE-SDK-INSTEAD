/*
 * Copyright 2012 Dart project authors.
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

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.List;

/**
 * A toolbar action to enumerate a launch debug launch configurations.
 */
public class DartRunAction extends DartAbstractAction implements IViewActionDelegate {

  public DartRunAction() {
    this(null, false);
  }

  public DartRunAction(IWorkbenchWindow window) {
    this(window, false);
  }

  public DartRunAction(IWorkbenchWindow window, boolean noMenu) {
    super(window, "Run", noMenu ? IAction.AS_PUSH_BUTTON : IAction.AS_DROP_DOWN_MENU);

    setActionDefinitionId("com.google.dart.tools.debug.ui.run.selection");
    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/run_exc.gif"));
  }

  @Override
  public void init(IViewPart view) {
    window = view.getSite().getWorkbenchWindow();
  }

  @Override
  public void run() {
    try {
      IResource resource = LaunchUtils.getSelectedResource(window);

      if (resource == null) {
        List<ILaunchConfiguration> launches = LaunchUtils.getAllLaunches();

        if (launches.size() == 0) {
          MessageDialog.openInformation(getWindow().getShell(), "Unable to Run",
              "Unable to run the current selection. Please choose a file in a library with a main() function.");
        } else {
          chooseAndLaunch(launches);
        }

        return;
      } else {
        launchResource(resource);
      }
    } catch (Throwable exception) {
      // We need to defensively show all errors coming out of here - the user needs feedback as
      // to why their launch didn't work.
      DartUtil.logError(exception);

      DebugErrorHandler.errorDialog(window.getShell(), "Error During Launch",
          "Internal error during launch - please report this using the feedback mechanism!",
          exception);
    }
  }

  protected void launchResource(IResource resource) throws DartModelException {
    ILaunchConfiguration config = LaunchUtils.getLaunchFor(resource);

    if (config != null) {
      launch(config);
    } else {
      List<ILaunchShortcut> candidates = LaunchUtils.getApplicableLaunchShortcuts(resource);

      if (candidates.size() == 0) {
        MessageDialog.openInformation(getWindow().getShell(), "Unable to Run", "Unable to run "
            + resource.getName() + ". Please choose a file in a library with a main() function.");
      } else {
        ISelection sel = new StructuredSelection(resource);

        launch(candidates.get(0), sel);
      }
    }
  }

  private boolean chooseAndLaunch(List<ILaunchConfiguration> launches) {
    if (launches.size() == 0) {
      return false;
    } else if (launches.size() == 1) {
      launch(launches.get(0));

      return true;
    } else {
      ILaunchConfiguration config = LaunchUtils.chooseConfiguration(launches);

      if (config != null) {
        launch(config);

        return true;
      } else {
        return false;
      }
    }
  }

}

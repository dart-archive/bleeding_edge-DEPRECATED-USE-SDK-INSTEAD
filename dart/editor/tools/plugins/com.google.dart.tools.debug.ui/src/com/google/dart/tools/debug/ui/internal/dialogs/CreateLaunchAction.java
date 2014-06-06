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

package com.google.dart.tools.debug.ui.internal.dialogs;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * An action used to create a new launch configuration.
 */
class CreateLaunchAction extends InstrumentedAction implements IWorkbenchWindowActionDelegate {
  //private Menu menu;
  private ManageLaunchesDialog launchConfigurationDialog;
  private ILaunchConfigurationType configType;

  /**
   * Create a new CreateLaunchAction.
   */
  public CreateLaunchAction(ManageLaunchesDialog launchConfigurationDialog,
      ILaunchConfigurationType configType) {
    super("Create a new " + configType.getName());

    this.launchConfigurationDialog = launchConfigurationDialog;
    this.configType = configType;

    ImageDescriptor descriptor = DebugUITools.getDefaultImageDescriptor(configType);
    if (descriptor != null) {
      setImageDescriptor(new DecorationOverlayIcon(
          DartDebugUIPlugin.getImage(descriptor),
          DartDebugUIPlugin.getImageDescriptor("ovr16/new.png"),
          IDecoration.TOP_RIGHT));
    }
  }

  @Override
  public void dispose() {

  }

  @Override
  public void init(IWorkbenchWindow window) {

  }

  @Override
  public void run(IAction action) {
    run();
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {

  }

  protected void create(ILaunchConfigurationType configType) {
    try {
      ILaunchConfigurationWorkingCopy wc = configType.newInstance(
          null,
          DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName("New launch"));

      // TODO(devoncarew): init this launch config with a starting resource and name

      if (configType.getIdentifier().equals(DartDebugCorePlugin.MOBILE_LAUNCH_CONFIG_ID)) {
        DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(wc);
        wrapper.setUsePubServe(false);
      }
      wc.doSave();

      launchConfigurationDialog.selectLaunchConfiguration(wc.getName());
    } catch (CoreException exception) {
      DebugErrorHandler.errorDialog(
          launchConfigurationDialog.getShell(),
          "Error Created Launch",
          "Unable to create the selected launch: " + exception.toString(),
          exception);
    }
  }

  @Override
  protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    create(configType);

  }

}

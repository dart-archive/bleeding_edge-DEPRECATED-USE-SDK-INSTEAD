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

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationTreeContentProvider;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchViewerComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// TODO: see LaunchConfigurationsDialog

/**
 * A dialog to create, edit, and manage launch configurations.
 */
@SuppressWarnings("restriction")
public class ManageLaunchesDialog extends TitleAreaDialog {
  private List<ILaunchConfigurationType> launchTypes;

  public ManageLaunchesDialog(IWorkbenchWindow window) {
    super(window.getShell());

    setShellStyle(getShellStyle() | SWT.RESIZE);

    initLaunchInfo();
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText(Messages.ManageLaunchesDialog_createLaunch);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    createButton(parent, IDialogConstants.CLIENT_ID, Messages.ManageLaunchesDialog_launchRun, true);
    Button runButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
        false);
    runButton.setEnabled(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite contents = (Composite) super.createDialogArea(parent);

    setTitle(Messages.ManageLaunchesDialog_manageLaunches);
    setMessage("TODO: describe what to do here");
    setTitleImage(DartDebugUIPlugin.getImage("wiz/run_wiz.png")); //$NON-NLS-1$

    Composite composite = new Composite(contents, SWT.NONE);
    createDialogUI(composite);

    return contents;
  }

  void initLaunchInfo() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    launchTypes = new ArrayList<ILaunchConfigurationType>(
        Arrays.asList(manager.getLaunchConfigurationTypes()));

    Collections.sort(launchTypes, new Comparator<ILaunchConfigurationType>() {
      @Override
      public int compare(ILaunchConfigurationType launch0, ILaunchConfigurationType launch1) {
        return launch0.getName().compareToIgnoreCase(launch1.getName());
      }
    });
  }

  private void createDialogUI(Composite parent) {
    GridLayoutFactory.fillDefaults().numColumns(2).margins(12, 6).applyTo(parent);

    Label label = new Label(parent, SWT.NONE);
    label.setText("foo");

    // spacer
    new Label(parent, SWT.NONE);

    ListViewer treeViewer = new ListViewer(parent);
    treeViewer.setLabelProvider(new DecoratingLabelProvider(
        DebugUITools.newDebugModelPresentation(),
        PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
    treeViewer.setComparator(new WorkbenchViewerComparator());
    treeViewer.setContentProvider(new LaunchConfigurationTreeContentProvider(
        ILaunchManager.RUN_MODE, getShell()));
    //treeViewer.addFilter(new LaunchGroupFilter(ILaunchManager.RUN_MODE));
    treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
    GridDataFactory.swtDefaults().grab(false, true).hint(120, 200).align(SWT.FILL, SWT.FILL).applyTo(
        treeViewer.getControl());

    label = new Label(parent, SWT.NONE);
    label.setText("bar");
    GridDataFactory.swtDefaults().grab(true, true).hint(200, 200).align(SWT.FILL, SWT.FILL).applyTo(
        label);

    // TODO:

  }

}

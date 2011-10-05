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
package com.google.dart.tools.debug.ui.internal.client;

import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

public class MainDartJsLaunchConfigurationPanel extends Composite {
  private Text appPathField;
  private Label appPathLabel;
  private Button externalBrowserCheckbox;

  /**
   * Create the composite.
   * 
   * @param parent the parent component
   * @param style the style
   */
  public MainDartJsLaunchConfigurationPanel(Composite parent, int style,
      final MainDartJsLaunchConfigurationTab launchTab) {
    super(parent, style);
    setLayout(new GridLayout(3, false));

    appPathLabel = new Label(this, SWT.NONE);
    appPathLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    appPathLabel.setText("Resource:");

    appPathField = new Text(this, SWT.BORDER);
    appPathField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        launchTab.panelChanged();
      }
    });
    appPathField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    Button selectAppButton = new Button(this, SWT.NONE);
    selectAppButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        selectResource();
        launchTab.panelChanged();
      }
    });
    selectAppButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false, 1, 1));
    selectAppButton.setText("Select...");

    externalBrowserCheckbox = new Button(this, SWT.CHECK);
    externalBrowserCheckbox.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        launchTab.panelChanged();
      }
    });
    externalBrowserCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
    externalBrowserCheckbox.setText("Open in an external browser");
  }

  /**
   * Answer the resource path displayed in the UI
   */
  public String getResourcePath() {
    return appPathField.getText().trim();
  }

  /**
   * Answer true if the "open external browser" option is selected
   */
  public boolean isExternalBrowser() {
    return externalBrowserCheckbox.getSelection();
  }

  /**
   * Set the "open external browser" option
   */
  public void setExternalBrowser(boolean external) {
    externalBrowserCheckbox.setSelection(external);
  }

  /**
   * Set the resource path displayed in the UI
   */
  public void setResourcePath(IPath path) {
    setResourcePath(path != null ? path.toString() : "");
  }

  /**
   * Set the resource path displayed in the UI
   */
  public void setResourcePath(String path) {
    appPathField.setText(path != null ? path : "");
  }

  @Override
  protected void checkSubclass() {
    // Disable the check that prevents subclassing of SWT components
  }

  /**
   * Answer the selected launch type
   */
  protected int getLaunchType() {
    return ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT;
  }

  /**
   * Set the launch type displayed in the UI
   */
  protected void setLaunchType(int launchType) {
    switch (launchType) {
      case ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT:
        externalBrowserCheckbox.setEnabled(true);
        break;

      default:
        DartUtil.logError("Invalidate launch type specified: " + launchType);
        setLaunchType(ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT);
        break;
    }
  }

  /**
   * Show a dialog that lets the user select a Dart application or HTML file in the workspace
   */
  private void selectResource() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(getShell(),
        false, root, IResource.FILE);
    dialog.setTitle("Select a resource to launch");
    dialog.setInitialPattern("*.app", FilteredItemsSelectionDialog.FULL_SELECTION);
    IPath path = new Path(getResourcePath());
    if (workspace.validatePath(path.toString(), IResource.FILE).isOK()) {
      IFile file = root.getFile(path);
      if (file != null && file.exists()) {
        dialog.setInitialSelections(new Object[] {path});
      }
    }
    dialog.open();
    Object[] results = dialog.getResult();
    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      setResourcePath(((IFile) results[0]).getFullPath());
    }
  }
}

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
package com.google.dart.tools.debug.ui.internal.chrome;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.debug.core.ChromeBrowserConfig;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.preferences.DebugPreferencePage;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The main launch configuration UI for running Dart applications in Chrome.
 */
public class DartChromeMainTab extends AbstractLaunchConfigurationTab {

  private Text applicationHtmlText;
  private Combo browserCombo;
  private CLabel browserWarningLabel;

  private ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  /**
   * Create a new instance of DartServerMainTab.
   */
  public DartChromeMainTab() {

  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 1).applyTo(composite);

    // Project group
    Group group = new Group(composite, SWT.NONE);
    group.setText("Launch Target:");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("HTML file:");

    applicationHtmlText = new Text(group, SWT.BORDER | SWT.SINGLE);
    applicationHtmlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        applicationHtmlText);

    Button button = new Button(group, SWT.PUSH);
    button.setText("Browse...");
    PixelConverter converter = new PixelConverter(button);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleApplicationBrowseButton();
      }
    });

    // Runner group
    group = new Group(composite, SWT.NONE);
    group.setText("Browser:");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

    browserCombo = new Combo(group, SWT.READ_ONLY | SWT.DROP_DOWN);
    browserCombo.setItems(getConfiguredBrowsers());
    browserCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        validateBrowsers();
        notifyPanelChanged();
      }
    });
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        browserCombo);

    Link link = new Link(group, SWT.NONE);
    link.setText("<a href=\"settings\">settings...</a>");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(link);
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
            Display.getDefault().getActiveShell(), DebugPreferencePage.PAGE_ID, null, null);

        if (dialog != null) {
          String selectedBrowser = browserCombo.getText();

          dialog.open();

          restoreSelectedBrowser(selectedBrowser);

          validateBrowsers();
        }
      }
    });

    browserWarningLabel = new CLabel(group, SWT.NONE);
    browserWarningLabel.setText("");
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(browserWarningLabel);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    // check that the application name is not empty
    if (applicationHtmlText.getText().length() == 0) {
      return "A HTML file is required.";
    }

    return null;
  }

  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("chromium_16_server.png");
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    applicationHtmlText.setText(dartLauncher.getApplicationName());

    browserCombo.setText(dartLauncher.getBrowserConfig());

    if ("".equals(browserCombo.getText())) {
      if (browserCombo.getItemCount() > 0) {
        browserCombo.setText(browserCombo.getItem(0));
      }
    }

    validateBrowsers();
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    dartLauncher.setApplicationName(applicationHtmlText.getText());
    if (!applicationHtmlText.getText().trim().isEmpty()) {
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
          new Path(applicationHtmlText.getText().trim()));
      dartLauncher.setProjectName(file.getProject().getName());
    }
    dartLauncher.setBrowserConfig(browserCombo.getText());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    dartLauncher.setApplicationName("");

    List<ChromeBrowserConfig> browserConfigs = DartDebugCorePlugin.getPlugin().getConfiguredBrowsers();
    if (!browserConfigs.isEmpty()) {
      dartLauncher.setBrowserConfig(browserConfigs.get(0).getName());
    } else {
      dartLauncher.setBrowserConfig("");
    }
  }

  protected DartProject getDartProject() {
    String projectName = applicationHtmlText.getText().trim();
    if (projectName.length() < 1) {
      return null;
    }
    return DartCore.create(getWorkspaceRoot()).getDartProject(projectName);
  }

  protected void handleApplicationBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(getShell(), workspace.getRoot(), false, true);
    dialog.setTitle("Select a HTML page to launch");
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION);
    IPath path = new Path(applicationHtmlText.getText());
    if (workspace.validatePath(path.toString(), IResource.FILE).isOK()) {
      IFile file = workspace.getRoot().getFile(path);
      if (file != null && file.exists()) {
        dialog.setInitialSelections(new Object[] {path});
      }
    }

    dialog.open();

    Object[] results = dialog.getResult();
    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      String pathStr = ((IFile) results[0]).getFullPath().toPortableString();

      applicationHtmlText.setText(pathStr);
    }
  }

  private String[] getConfiguredBrowsers() {
    List<String> browsers = new ArrayList<String>();

    for (ChromeBrowserConfig browserConfig : DartDebugCorePlugin.getPlugin().getConfiguredBrowsers()) {
      browsers.add(browserConfig.getName());
    }

    return browsers.toArray(new String[browsers.size()]);
  }

  private IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  private void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }

  private void restoreSelectedBrowser(String browser) {
    browserCombo.setItems(getConfiguredBrowsers());

    browserCombo.setText(browser);

    if ("".equals(browserCombo.getText())) {
      if (browserCombo.getItemCount() > 0) {
        browserCombo.setText(browserCombo.getItem(0));
      }
    }
  }

  private void validateBrowsers() {
    if (DartDebugCorePlugin.getPlugin().getConfiguredBrowsers().size() == 0) {
      browserWarningLabel.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
      browserWarningLabel.setText("No Chrome browsers are configured.");
    } else {
      browserWarningLabel.setImage(null);
      browserWarningLabel.setText("");
    }
  }

}

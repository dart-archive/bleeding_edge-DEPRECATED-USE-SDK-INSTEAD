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
package com.google.dart.tools.debug.ui.internal.dartium;

import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * The main launch configuration UI for running Dart applications in Dartium.
 */
public class DartiumMainTab extends AbstractLaunchConfigurationTab {

  class ProjectSelectionDialog extends FilteredResourcesSelectionDialog {

    public ProjectSelectionDialog(Shell shell, IContainer container) {
      super(shell, false, container, IResource.PROJECT);

    }

    @Override
    protected ItemsFilter createFilter() {
      return new ResourceFilter() {
        @Override
        public boolean matchItem(Object item) {
          return item instanceof IProject;
        }
      };
    }
  }

  private Text htmlText;

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  private Button htmlButton;

  private Button htmlBrowseButton;

  private Button urlButton;

  private Text urlText;

  private Text projectText;

  private Button projectBrowseButton;

  private Button checkedModeButton;

  private Button enableDebuggingButton;

  protected Text argumentText;

  /**
   * Create a new instance of DartServerMainTab.
   */
  public DartiumMainTab() {

  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    // Project group
    Group group = new Group(composite, SWT.NONE);
    group.setText(DartiumLaunchMessages.DartiumMainTab_LaunchTarget);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    createHtmlField(group);

    Label filler = new Label(group, SWT.NONE);
    GridDataFactory.swtDefaults().span(3, 1).applyTo(filler);

    createUrlField(group);

    // Dartium settings group
    group = new Group(composite, SWT.NONE);
    group.setText("Dartium settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    checkedModeButton = new Button(group, SWT.CHECK);
    checkedModeButton.setText("Run in checked mode");
    GridDataFactory.swtDefaults().span(3, 1).applyTo(checkedModeButton);
    checkedModeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    enableDebuggingButton = new Button(group, SWT.CHECK);
    enableDebuggingButton.setText("Enable debugging");
    GridDataFactory.swtDefaults().span(3, 1).applyTo(enableDebuggingButton);
    enableDebuggingButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    // additional browser arguments
    Label argsLabel = new Label(group, SWT.NONE);
    argsLabel.setText("Arguments:");
    GridDataFactory.swtDefaults().hint(getLabelColumnWidth(), -1).applyTo(argsLabel);

    argumentText = new Text(group, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        argumentText);

    Label spacer = new Label(group, SWT.NONE);
    PixelConverter converter = new PixelConverter(spacer);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(spacer);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {
    if (performSdkCheck() != null) {
      return performSdkCheck();
    }

    if (htmlButton.getSelection() && htmlText.getText().length() == 0) {
      return DartiumLaunchMessages.DartiumMainTab_NoHtmlFile;
    }

    if (urlButton.getSelection()) {
      if (urlText.getText().length() == 0) {
        return DartiumLaunchMessages.DartiumMainTab_NoUrl;
      } else if (projectText.getText().length() == 0) {
        return DartiumLaunchMessages.DartiumMainTab_NoProject;
      }
    }

    return null;
  }

  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("chromium_16.png"); //$NON-NLS-1$
  }

  @Override
  public String getMessage() {
    return DartiumLaunchMessages.DartiumMainTab_Message;
  }

  @Override
  public String getName() {
    return DartiumLaunchMessages.DartiumMainTab_Name;
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    if (dartLauncher.getShouldLaunchFile()) {
      htmlButton.setSelection(true);
      htmlText.setText(dartLauncher.getApplicationName());
      urlText.setText(dartLauncher.getUrl());
      projectText.setText(dartLauncher.getProjectName());
      urlButton.setSelection(false);
      updateEnablements(true);
    } else {
      urlButton.setSelection(true);
      urlText.setText(dartLauncher.getUrl());
      projectText.setText(dartLauncher.getProjectName());
      htmlText.setText(dartLauncher.getApplicationName());
      htmlButton.setSelection(false);
      updateEnablements(false);
    }

    if (checkedModeButton != null) {
      checkedModeButton.setSelection(dartLauncher.getCheckedMode());
    }

    if (enableDebuggingButton != null) {
      enableDebuggingButton.setSelection(dartLauncher.getEnableDebugging());
    }

    argumentText.setText(dartLauncher.getArguments());
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;

//    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);
//
//    if (launchWrapper.getShouldLaunchFile()) {
//      IResource resource = launchWrapper.getApplicationResource();
//      if (resource == null) {
//        return false;
//      }
//      if (!resource.exists()) {
//        return false;
//      }
//      return (DartUtil.isWebPage(resource) || DartUtil.isDartLibrary(resource));
//    } else {
//      IResource project = ResourcesPlugin.getWorkspace().getRoot().findMember(
//          launchWrapper.getProjectName());
//      if (project == null) {
//        return false;
//      }
//      if (!project.exists()) {
//        return false;
//      }
//    }
//
//    return true;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    dartLauncher.setShouldLaunchFile(htmlButton.getSelection());
    dartLauncher.setApplicationName(htmlText.getText());
    dartLauncher.setUrl(urlText.getText().trim());
    dartLauncher.setProjectName(projectText.getText().trim());

    if (checkedModeButton != null) {
      dartLauncher.setCheckedMode(checkedModeButton.getSelection());
    }

    if (enableDebuggingButton != null) {
      dartLauncher.setEnableDebugging(enableDebuggingButton.getSelection());
    }

    dartLauncher.setArguments(argumentText.getText().trim());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    dartLauncher.setShouldLaunchFile(true);
    dartLauncher.setApplicationName(""); //$NON-NLS-1$
  }

  protected void createHtmlField(Composite composite) {
    htmlButton = new Button(composite, SWT.RADIO);
    htmlButton.setText(DartiumLaunchMessages.DartiumMainTab_HtmlFileLabel);
    htmlButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(true);
        notifyPanelChanged();
      }
    });

    htmlText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    htmlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(400, SWT.DEFAULT).grab(
        true,
        false).applyTo(htmlText);

    htmlBrowseButton = new Button(composite, SWT.PUSH);
    htmlBrowseButton.setText(DartiumLaunchMessages.DartiumMainTab_Browse);
    PixelConverter converter = new PixelConverter(htmlBrowseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        htmlBrowseButton);
    htmlBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleApplicationBrowseButton();
      }
    });
  }

  protected void createUrlField(Composite composite) {
    urlButton = new Button(composite, SWT.RADIO);
    urlButton.setText(DartiumLaunchMessages.DartiumMainTab_UrlLabel);
    urlButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(false);
        notifyPanelChanged();
      }
    });

    urlText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    urlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(urlText);

    // spacer
    new Label(composite, SWT.NONE);

    Label projectLabel = new Label(composite, SWT.NONE);
    projectLabel.setText(DartiumLaunchMessages.DartiumMainTab_ProjectLabel);
    GridDataFactory.swtDefaults().indent(20, 0).applyTo(projectLabel);

    projectText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    projectText.setEditable(false);
    projectText.setCursor(composite.getShell().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(projectText);

    projectBrowseButton = new Button(composite, SWT.PUSH);
    projectBrowseButton.setText(DartiumLaunchMessages.DartiumMainTab_Browse);
    PixelConverter converter = new PixelConverter(htmlBrowseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        projectBrowseButton);
    projectBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleProjectBrowseButton();
      }
    });
  }

  protected int getLabelColumnWidth() {
    htmlButton.pack();
    return htmlButton.getSize().x;
  }

  protected void handleApplicationBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(getShell(), workspace.getRoot(), false, true);
    dialog.setTitle(DartiumLaunchMessages.DartiumMainTab_SelectHtml);
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION); //$NON-NLS-1$
    IPath path = new Path(htmlText.getText());
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

      htmlText.setText(pathStr);
    }
  }

  protected void handleProjectBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    ProjectSelectionDialog dialog = new ProjectSelectionDialog(getShell(), workspace.getRoot());
    dialog.setTitle(DartiumLaunchMessages.DartiumMainTab_SelectProject);
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION); //$NON-NLS-1$

    try {
      DartProject[] dartProjects = DartModelManager.getInstance().getDartModel().getDartProjects();
      List<IProject> projects = new ArrayList<IProject>();
      for (DartProject dartProject : dartProjects) {
        projects.add(dartProject.getProject());
      }
      dialog.setInitialSelections(projects.toArray());
    } catch (DartModelException e) {
      DartDebugCorePlugin.logError(e);
    }

    dialog.open();

    Object[] results = dialog.getResult();
    if ((results != null) && (results.length > 0) && (results[0] instanceof IProject)) {
      String pathStr = ((IProject) results[0]).getFullPath().toPortableString();
      projectText.setText(pathStr);
      notifyPanelChanged();
    }
  }

  protected void notifyPanelChanged() {
    setDirty(true);

    updateLaunchConfigurationDialog();
  }

  protected String performSdkCheck() {
    if (!DartSdkManager.getManager().hasSdk()) {
      return "Dartium is not installed ("
          + DartSdkManager.getManager().getSdk().getDartiumWorkingDirectory() + ")";
    } else {
      return null;
    }
  }

  private void updateEnablements(boolean isFile) {
    if (isFile) {
      htmlText.setEnabled(true);
      htmlBrowseButton.setEnabled(true);
      urlText.setEnabled(false);
      projectText.setEnabled(false);
      projectBrowseButton.setEnabled(false);
    } else {
      htmlText.setEnabled(false);
      htmlBrowseButton.setEnabled(false);
      urlText.setEnabled(true);
      projectText.setEnabled(true);
      projectBrowseButton.setEnabled(true);
    }
  }

}

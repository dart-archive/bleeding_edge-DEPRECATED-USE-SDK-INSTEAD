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
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
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
          if (!(item instanceof IResource)) {
            return false;
          }
          IResource resource = (IResource) item;
          if (resource instanceof IProject) {
            return true;
          }
          return false;
        }
      };
    }

  }

  private Text htmlText;

  private ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  private Button htmlButton;

  private Button htmlBrowsebutton;

  private Button urlButton;

  private Text urlText;

  private Text projectText;

  private Button projectBrowseButton;

  /**
   * Create a new instance of DartServerMainTab.
   */
  public DartiumMainTab() {

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

    createHtmlField(group);

    Label filler = new Label(group, SWT.NONE);
    GridDataFactory.swtDefaults().span(3, 1).applyTo(filler);

    createUrlField(group);

    setControl(composite);
  }

  @Override
  public String getErrorMessage() {

    if (htmlButton.getSelection() && htmlText.getText().length() == 0) {
      return "HTML file not specified";
    }
    if (urlButton.getSelection()) {
      if (urlText.getText().length() == 0) {
        return "URL not specified";
      } else if (projectText.getText().length() == 0) {
        return "Project not specified";
      }
    }
    return null;
  }

  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("chromium_16_server.png");
  }

  @Override
  public String getMessage() {
    return "Create a configuration to launch Dart application";
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    if (dartLauncher.getShouldLaunchFile()) {
      htmlButton.setSelection(true);
      htmlText.setText(dartLauncher.getApplicationName());
      urlButton.setSelection(false);
      updateEnablements(true);
    } else {
      urlButton.setSelection(true);
      urlText.setText(dartLauncher.getApplicationName());
      projectText.setText(dartLauncher.getProjectName());
      htmlButton.setSelection(false);
      updateEnablements(false);
    }

  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    if (getErrorMessage() != null) {
      return false;
    }

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);

    if (launchWrapper.getShouldLaunchFile()) {
      IResource resource = launchWrapper.getApplicationResource();
      if (resource == null) {
        return false;
      }
      if (!resource.exists()) {
        return false;
      }
      return (DartUtil.isWebPage(resource) || DartUtil.isDartApp(resource));
    } else {

      IResource project = ResourcesPlugin.getWorkspace().getRoot().findMember(
          launchWrapper.getProjectName());
      if (project == null) {
        return false;
      }
      if (!project.exists()) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    if (htmlButton.getSelection()) {
      dartLauncher.setShouldLaunchFile(true);
      dartLauncher.setApplicationName(htmlText.getText());
      if (!htmlText.getText().trim().isEmpty()) {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(htmlText.getText().trim()));
        dartLauncher.setProjectName(file.getProject().getName());
      }

    } else {
      dartLauncher.setShouldLaunchFile(false);
      dartLauncher.setApplicationName(urlText.getText().trim());
      dartLauncher.setProjectName(projectText.getText().trim());
    }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    dartLauncher.setApplicationName("");
  }

  protected void createHtmlField(Composite composite) {
    htmlButton = new Button(composite, SWT.RADIO);
    htmlButton.setText("HTML file:");
    htmlButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(true);
        notifyPanelChanged();
      }
    });

    htmlText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    htmlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(400, SWT.DEFAULT).grab(true,
        false).applyTo(htmlText);

    htmlBrowsebutton = new Button(composite, SWT.PUSH);
    htmlBrowsebutton.setText("Browse...");
    PixelConverter converter = new PixelConverter(htmlBrowsebutton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        htmlBrowsebutton);
    htmlBrowsebutton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleApplicationBrowseButton();
      }
    });

  }

  protected void createUrlField(Composite composite) {
    urlButton = new Button(composite, SWT.RADIO);
    urlButton.setText("URL:");
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

    Label filler = new Label(composite, SWT.NONE);

    Label projectLabel = new Label(composite, SWT.NONE);
    projectLabel.setText("Project:");
    GridDataFactory.swtDefaults().indent(20, 0).applyTo(projectLabel);
    projectText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    projectText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(projectText);
    projectBrowseButton = new Button(composite, SWT.PUSH);
    projectBrowseButton.setText("Browse...");
    PixelConverter converter = new PixelConverter(htmlBrowsebutton);
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

  protected void handleApplicationBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(getShell(), workspace.getRoot(), false, true);
    dialog.setTitle("Select a HTML page to launch");
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION);
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
    dialog.setTitle("Select a project to launch");
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION);

    try {
      DartProject[] dartProjects = DartModelManager.getInstance().getDartModel().getDartProjects();
      List<IProject> projects = new ArrayList<IProject>();
      for (DartProject dartProject : dartProjects) {
        projects.add(dartProject.getProject());
      }
      dialog.setInitialSelections(projects.toArray());
    } catch (DartModelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    dialog.open();

    Object[] results = dialog.getResult();
    if ((results != null) && (results.length > 0) && (results[0] instanceof IProject)) {
      String pathStr = ((IProject) results[0]).getFullPath().toPortableString();

      projectText.setText(pathStr);
    }
  }

  protected void notifyPanelChanged() {

    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  private void updateEnablements(boolean isFile) {
    if (isFile) {
      htmlText.setEnabled(true);
      htmlBrowsebutton.setEnabled(true);
      urlText.setEnabled(false);
      projectText.setEnabled(false);
      projectBrowseButton.setEnabled(false);

    } else {
      htmlText.setEnabled(false);
      htmlBrowsebutton.setEnabled(false);
      urlText.setEnabled(true);
      projectText.setEnabled(true);
      projectBrowseButton.setEnabled(true);

    }
  }

}

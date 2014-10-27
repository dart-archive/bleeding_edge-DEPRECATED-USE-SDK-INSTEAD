/*
 * Copyright 2012 Google Inc.
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

package com.google.dart.tools.ui.internal.projects;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.AbstractSample;
import com.google.dart.tools.core.generator.DartIdentifierUtil;
import com.google.dart.tools.core.internal.util.StatusUtil;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.DirectoryVerification;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * New project creation page.
 */
public class NewApplicationCreationPage extends WizardPage {

  public static enum ProjectType {
    NONE,
    SERVER,
    WEB
  }

  static final String NEW_APPPLICATION_SETTINGS = "newApplicationWizard.settings"; //$NON-NLS-1$
  static final String PARENT_DIR = "parentDir"; //$NON-NLS-1$

  private Text projectNameField;
  private Text projectLocationField;
  private String defaultLocation;
  private TableViewer samplesViewer;

  /**
   * Creates a new project creation wizard page.
   */
  public NewApplicationCreationPage() {
    super("newApplication"); //$NON-NLS-1$
    setPageComplete(false);
    setTitle(ProjectMessages.OpenNewApplicationWizardAction_text);
    setDescription(ProjectMessages.OpenNewApplicationWizardAction_desc);
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/wizban/newprj_wiz.png")); //$NON-NLS-1$

    defaultLocation = getParentDirectory();
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    setControl(container);

    GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
    container.setLayoutData(gridData);
    container.setLayout(new GridLayout(1, false));

    Group nameGroup = new Group(container, SWT.NONE);
    nameGroup.setText("Name and location");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(nameGroup);
    GridLayoutFactory.fillDefaults().numColumns(3).margins(8, 8).applyTo(nameGroup);

    Label nameLabel = new Label(nameGroup, SWT.NONE);
    nameLabel.setText(ProjectMessages.NewApplicationWizardPage_project_name_label);

    projectNameField = new Text(nameGroup, SWT.BORDER);
    projectNameField.setText(""); //$NON-NLS-1$
    projectNameField.setToolTipText(ProjectMessages.NewApplicationWizardPage_project_name_tooltip);
    projectNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    projectNameField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        projectNameField.selectAll();
      }
    });
    projectNameField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateName();
      }
    });

    Label locationLabel = new Label(nameGroup, SWT.NONE);
    locationLabel.setText(ProjectMessages.NewApplicationWizardPage_directory_label);

    projectLocationField = new Text(nameGroup, SWT.BORDER);
    projectLocationField.setText(defaultLocation);
    projectLocationField.setToolTipText(ProjectMessages.NewApplicationWizardPage_directory_tooltip);
    projectLocationField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    projectLocationField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        projectLocationField.selectAll();
      }
    });
    projectLocationField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLocation();
      }
    });

    Button browseButton = new Button(nameGroup, SWT.NONE);
    browseButton.setText(ProjectMessages.NewApplicationWizardPage_browse_label);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowseButton(projectLocationField);
      }
    });
    PixelConverter converter = new PixelConverter(browseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(browseButton);

    projectNameField.setFocus();

    Group contentGroup = new Group(container, SWT.NONE);
    contentGroup.setText("Project templates");
    GridDataFactory.fillDefaults().span(3, 1).grab(true, false).indent(0, 10).applyTo(contentGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(contentGroup);

    samplesViewer = new TableViewer(contentGroup, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER
        | SWT.FULL_SELECTION);
    samplesViewer.setLabelProvider(new SamplesLabelProvider());
    samplesViewer.setContentProvider(new ArrayContentProvider());
    samplesViewer.setInput(new ArrayList<AbstractSample>());
    GridDataFactory.fillDefaults().hint(200, 90).grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(
        samplesViewer.getControl());
    samplesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateMessageAndEnablement();
      }
    });

    samplesViewer.getTable().setEnabled(true);

    updateMessageAndEnablement();

    setPageComplete(false);

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        populateSamplesList();
      }
    });
  }

  /**
   * Returns the current project location URI as entered by the user, or <code>null</code> if a
   * valid project location has not been entered.
   * 
   * @return the project location URI, or <code>null</code>
   */
  public URI getLocationURI() {
    String path = getLocationPath().toOSString();
    return URIUtil.toURI(path);
  }

  /**
   * Creates a project resource handle for the current project name field value. The project handle
   * is created relative to the workspace root.
   * <p>
   * This method does not create the project resource; this is the responsibility of
   * <code>IProject::create</code> invoked by the new project resource wizard.
   * </p>
   * 
   * @return the new project resource handle
   */
  public IProject getProjectHandle(String name) {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
  }

  /**
   * Returns the current project name as entered by the user.
   * 
   * @return the project name, its anticipated initial value, or <code>null</code> if no project
   *         name is known
   */
  public String getProjectName() {
    return getProjectNameFieldValue();
  }

  protected AbstractSample getCurrentSample() {

    IStructuredSelection selection = (IStructuredSelection) samplesViewer.getSelection();
    if (selection.isEmpty()) {
      return null;
    } else {
      return (AbstractSample) selection.getFirstElement();
    }

  }

  protected void handleBrowseButton(Text locationField) {
    DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
    String location = locationField.getText().trim();
    if (!new File(location).exists()) {
      if (new File(defaultLocation).exists()) {
        location = defaultLocation;
      } else {
        location = new Path(defaultLocation).removeLastSegments(1).toString();
      }
    }

    directoryDialog.setFilterPath(location);
    String directory = directoryDialog.open();
    if (directory != null) {
      locationField.setText(directory);
    }
  }

  protected void updateLocation() {
    update();
  }

  protected void updateName() {
    update();
  }

  private boolean doesProjectExist() {
    IPath path = getLocationPath();
    File file = path.toFile();

    return file.exists();
  }

  private AbstractSample getDefaultSample(List<AbstractSample> samples) {
    for (AbstractSample sample : samples) {
      if (sample.shouldBeDefault()) {
        return sample;
      }
    }

    return samples.get(0);
  }

  private IPath getLocationPath() {
    return new Path(projectLocationField.getText()).append(getProjectName());
  }

  private String getParentDirectory() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
        NEW_APPPLICATION_SETTINGS);
    String path = settings.get(PARENT_DIR);
    if (path != null) {
      return path;
    }

    return DartCore.getUserDefaultDartFolder();
  }

  /**
   * Returns the value of the project name field with leading and trailing spaces removed.
   * 
   * @return the project name in the field
   */
  private String getProjectNameFieldValue() {
    if (projectNameField == null) {
      return ""; //$NON-NLS-1$
    }

    return projectNameField.getText().trim();
  }

  private void populateSamplesList() {
    try {
      getContainer().run(true, false, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          monitor.beginTask("", IProgressMonitor.UNKNOWN);
          final List<AbstractSample> samples = AbstractSample.getAllSamples();
          Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
              samplesViewer.setInput(samples);
              samplesViewer.setSelection(new StructuredSelection(getDefaultSample(samples)));
            }
          });
          monitor.done();
        }
      });
    } catch (InvocationTargetException e) {
      DartToolsPlugin.log(e);
    } catch (InterruptedException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void update() {
    if (getProjectNameFieldValue().isEmpty()) {
      setMessage(ProjectMessages.OpenNewApplicationWizardAction_desc);
      setPageComplete(false);
      return;
    }

    IStatus status = validate();

    if (status.isOK()) {
      setPageComplete(true);
      setErrorMessage(null);
      setMessage(NLS.bind(ProjectMessages.NewApplicationWizardPage_create_new, getProjectName()));
    } else {
      setPageComplete(false);
      setErrorMessage(status.getMessage());
    }
  }

  private void updateMessageAndEnablement() {
    AbstractSample sample = getCurrentSample();
    setMessage(sample == null ? null : sample.getDescription());
  }

  private IStatus validate() {
    IStatus status = StatusUtil.getMoreSevere(Status.OK_STATUS, validateLocation());
    status = StatusUtil.getMoreSevere(status, validateName());
    return status;
  }

  private IStatus validateLocation() {
    String location = projectLocationField.getText();
    IPath locationPath = new Path(location);

    if (!locationPath.isValidPath(getProjectName())) {
      return new Status(
          IStatus.ERROR,
          DartToolsPlugin.PLUGIN_ID,
          ProjectMessages.NewProjectCreationPage_invalid_loc);
    }

    // TODO(scheglov) check after Pub issue fixed
    // https://code.google.com/p/dart/issues/detail?id=10439
    if (locationPath.isUNC()) {
      return new Status(
          IStatus.ERROR,
          DartToolsPlugin.PLUGIN_ID,
          ProjectMessages.NewProjectCreationPage_invalid_loc_unc);
    }

    if (doesProjectExist()) {
      return new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, NLS.bind(
          ProjectMessages.NewApplicationWizardPage_error_existing,
          getProjectName()));
    }

    IStatus status = DirectoryVerification.getOpenDirectoryLocationStatus(new File(location));

    if (!status.isOK()) {
      return status;
    }

    return Status.OK_STATUS;
  }

  private IStatus validateName() {
    AbstractSample sample = getCurrentSample();
    if (sample != null) {
      if (sample.isValidProjectName(projectNameField.getText())) {
        return DartIdentifierUtil.validateIdentifier(projectNameField.getText());
      } else {
        return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Invalid project name");
      }
    }

    return DartIdentifierUtil.validateIdentifier(projectNameField.getText());

  }

}

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
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.launchConfigurations.DeleteLaunchConfigurationAction;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationPresentationManager;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.WorkbenchPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * A dialog to create, edit, and manage launch configurations.
 */
@SuppressWarnings("restriction")
public class ManageLaunchesDialog extends TitleAreaDialog implements ILaunchConfigurationDialog,
    ILaunchConfigurationListener {

  /**
   * Asynchronously open the manage launches dialog with the given launch automatically selected.
   */
  public static void openAsync(final IWorkbenchWindow window, final ILaunchConfiguration config) {
    final Display display = Display.getDefault();
    display.asyncExec(new Runnable() {
      @Override
      public void run() {
        final ManageLaunchesDialog dialog = new ManageLaunchesDialog(window);
        dialog.selectLaunchOnOpen(config);
        dialog.open();
      }
    });
  }

  private IWorkbenchWindow window;

  private TableViewer launchesViewer;
  private Composite configUI;
  private ScrolledComposite launchConfigArea;
  private Text configNameText;

  private ILaunchConfiguration selectedConfig;
  private ILaunchConfigurationWorkingCopy workingCopy;
  private ILaunchConfigurationTabGroup currentTabGroup;
  private ILaunchConfigurationTab activeTab;

  private IAction deleteAction;

  private ILaunchConfiguration launchConfig;

  public ManageLaunchesDialog(IWorkbenchWindow window) {
    super(window.getShell());

    this.window = window;

    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  public boolean canLaunch() {
    if (workingCopy == null) {
      return false;
    }

    try {
      verifyName();
    } catch (CoreException e) {
      return false;
    }

    ILaunchConfigurationTab[] tabs = getTabs();

    if (tabs == null) {
      return false;
    }

    for (int i = 0; i < tabs.length; i++) {
      if (!tabs[i].isValid(workingCopy)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String generateName(String name) {
    if (name == null) {
      name = IInternalDebugCoreConstants.EMPTY_STRING;
    }

    return getLaunchManager().generateLaunchConfigurationName(name);
  }

  @Override
  public ILaunchConfigurationTab getActiveTab() {
    return activeTab;
  }

  @Override
  public String getMode() {
    return ILaunchManager.RUN_MODE;
  }

  @Override
  public ILaunchConfigurationTab[] getTabs() {
    return currentTabGroup.getTabs();
  }

  @Override
  public void launchConfigurationAdded(ILaunchConfiguration configuration) {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    if (selectedConfig != null && selectedConfig.equals(manager.getMovedFrom(configuration))) {
      // this config was re-named, update the dialog with the new config
      selectedConfig = configuration;
      selectLaunchConfiguration(selectedConfig.getName());
    }

    refreshLaunchesViewer();
  }

  @Override
  public void launchConfigurationChanged(ILaunchConfiguration configuration) {
    getShell().getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        Shell shell = getShell();

        if (shell != null && !shell.isDisposed()) {
          updateButtons();
          updateMessage();
        }
      }
    });
  }

  @Override
  public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
    try {
      getShell().setRedraw(false);

      if (configuration == selectedConfig) {
        closeConfig(true);
        refreshLaunchesViewer();
        selectFirstLaunchConfig();
      } else {
        refreshLaunchesViewer();
      }
    } finally {
      getShell().setRedraw(true);
    }
  }

  @Override
  public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException {
    throw new UnsupportedOperationException("run()");
  }

  public void selectLaunchConfiguration(String name) {
    saveConfig();

    ILaunchConfiguration config = getConfigurationNamed(name);

    if (config != null) {
      launchesViewer.setSelection(new StructuredSelection(config));
    }
  }

  /**
   * Select the specified launch configuration when the dialog opens.
   */
  public void selectLaunchOnOpen(ILaunchConfiguration config) {
    selectedConfig = config;
  }

  @Override
  public void setActiveTab(ILaunchConfigurationTab tab) {
    if (activeTab != null) {
      activeTab.deactivated(workingCopy);
      activeTab.getControl().dispose();
    }

    activeTab = tab;

    if (activeTab != null) {
      launchConfigArea.setRedraw(false);

      configNameText.setVisible(true);

      activeTab.createControl(launchConfigArea);
//      GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
//          activeTab.getControl());
      launchConfigArea.setContent(activeTab.getControl());
      activeTab.getControl().setSize(activeTab.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT));
      configUI.layout(true);

      activeTab.activated(workingCopy);

      launchConfigArea.setRedraw(true);
    } else {
      configNameText.setVisible(false);
    }

    updateButtons();
    updateMessage();
  }

  @Override
  public void setActiveTab(int index) {
    setActiveTab(getTabs()[index]);
  }

  @Override
  public void setName(String name) {
    configNameText.setText(name);
  }

  @Override
  public void updateButtons() {
    if (getButton(IDialogConstants.OK_ID) != null) {
      // Apply button
      getButton(IDialogConstants.CLIENT_ID).setEnabled(canEnableButton());
      // Run button
      getButton(IDialogConstants.OK_ID).setEnabled(canEnableButton());

      // Delete action
      getDeleteAction().setEnabled(selectedConfig != null);
    }
  }

  @Override
  public void updateMessage() {
    try {
      verifyName();
    } catch (CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
      return;
    }

    if (activeTab != null) {
      String errorMessage = activeTab.getErrorMessage();

      if (errorMessage != null) {
        setErrorMessage(errorMessage);
      } else {
        setMessage(activeTab.getMessage());
        setErrorMessage(null);
      }
    } else {
      setMessage(null);
      setErrorMessage(null);
    }
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText(Messages.ManageLaunchesDialog_createLaunch);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    // Close
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);

    // Apply
    createButton(parent, IDialogConstants.CLIENT_ID, "Apply", false);
    getButton(IDialogConstants.CLIENT_ID).addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleApplyButton();
      }
    });

    // Run
    createButton(parent, IDialogConstants.OK_ID, Messages.ManageLaunchesDialog_launchRun, true);

    updateButtons();
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite contents = (Composite) super.createDialogArea(parent);

    setTitle(Messages.ManageLaunchesDialog_manageLaunches);
    setTitleImage(DartDebugUIPlugin.getImage("wiz/run_wiz.png")); //$NON-NLS-1$

    Composite composite = new Composite(contents, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);
    createDialogUI(composite);

    DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(this);

    parent.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(
            ManageLaunchesDialog.this);
      }
    });

    return contents;
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    final String settingsName = getClass().getCanonicalName();

    IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
    IDialogSettings section = workbenchSettings.getSection(settingsName);

    if (DartDebugCorePlugin.getPlugin().getClearDialogSettings()) {
      section = null;
      DartDebugCorePlugin.getPlugin().setClearLaunchesDialogSettings(false);
    }

    if (section == null) {
      section = workbenchSettings.addNewSection(settingsName);
    }

    return section;
  }

  @Override
  protected int getDialogBoundsStrategy() {
    return DIALOG_PERSISTSIZE;
  }

  /**
   * Returns the current launch manager
   * 
   * @return the current launch manager
   */
  protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  protected void handleApplyButton() {
    saveConfig();
  }

  protected void handleSelectedConfigChanged() {
    closeConfig(false);

    ILaunchConfiguration sel = null;

    if (launchesViewer.getSelection() instanceof IStructuredSelection) {
      Object obj = ((IStructuredSelection) launchesViewer.getSelection()).getFirstElement();

      if (obj instanceof ILaunchConfiguration) {
        sel = (ILaunchConfiguration) obj;
      }
    }

    selectedConfig = sel;

    updateButtons();
    updateMessage();

    if (selectedConfig != null) {
      show(selectedConfig);
    }
  }

  @Override
  protected void okPressed() {
    saveConfig();

    boolean supportsDebug = false;

    try {
      supportsDebug = selectedConfig.supportsMode(ILaunchManager.DEBUG_MODE);
    } catch (CoreException e) {

    }

    if (supportsDebug) {
      LaunchUtils.launch(selectedConfig, ILaunchManager.DEBUG_MODE);
    } else {
      LaunchUtils.launch(selectedConfig, ILaunchManager.RUN_MODE);
    }

    super.okPressed();
  }

  private boolean canEnableButton() {
    return activeTab != null && activeTab.getErrorMessage() == null && canLaunch();
  }

  private void closeConfig(boolean terminate) {
    if (!terminate) {
      saveConfig();
    }

    setActiveTab(null);

    activeTab = null;
    selectedConfig = null;
    currentTabGroup = null;
    workingCopy = null;
  }

  private void createDialogUI(Composite parent) {
    GridLayoutFactory.fillDefaults().margins(12, 6).applyTo(parent);

    SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).hint(725, 415).applyTo(
        sashForm);

    Composite leftComposite = new Composite(sashForm, SWT.NONE);
    GridLayoutFactory.fillDefaults().applyTo(leftComposite);
    GridDataFactory.swtDefaults().grab(false, true).align(SWT.FILL, SWT.FILL).applyTo(leftComposite);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    ToolBar toolBar = toolBarManager.createControl(leftComposite);
    toolBar.setBackground(parent.getBackground());
    GridDataFactory.swtDefaults().grab(true, false).align(SWT.BEGINNING, SWT.FILL).applyTo(toolBar);

    launchesViewer = new TableViewer(leftComposite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
    launchesViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
        new LaunchConfigLabelProvider()));
    launchesViewer.setComparator(new ViewerComparator(String.CASE_INSENSITIVE_ORDER));
    launchesViewer.setContentProvider(new LaunchConfigContentProvider());
    launchesViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
    launchesViewer.getTable().setFocus();
    launchesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        handleSelectedConfigChanged();
      }
    });

    GridDataFactory.swtDefaults().grab(false, true).align(SWT.FILL, SWT.FILL).hint(50, 50).applyTo(
        launchesViewer.getControl());

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    for (final ILaunchConfigurationType configType : manager.getLaunchConfigurationTypes()) {

      CreateLaunchAction action = new CreateLaunchAction(this, configType);
      toolBarManager.add(action);
    }

    //toolBarManager.add(new Separator());
    toolBarManager.add(getDeleteAction());

    toolBarManager.update(true);

    configUI = new Composite(sashForm, SWT.NONE);
    GridLayoutFactory.fillDefaults().applyTo(configUI);
    GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(configUI);

    toolBar.pack();
    Label toolbarSpacer = new Label(configUI, SWT.NONE);
    GridDataFactory.swtDefaults().hint(SWT.NONE, toolBar.getSize().y).applyTo(toolbarSpacer);

    Composite nameComposite = new Composite(configUI, SWT.NONE);
    GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(nameComposite);
    GridLayoutFactory.swtDefaults().margins(6, 0).applyTo(nameComposite);

    configNameText = new Text(nameComposite, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(
        configNameText);
    configNameText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (workingCopy != null) {
          workingCopy.rename(configNameText.getText());
        }
      }
    });

    launchConfigArea = new ScrolledComposite(configUI, SWT.V_SCROLL);
    GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(
        launchConfigArea);
    launchConfigArea.setExpandVertical(false);
    launchConfigArea.setExpandHorizontal(true);

    configNameText.setVisible(false);

    sashForm.setWeights(new int[] {33, 67});

    selectLaunchConfigFromPage();
  }

  private ILaunchConfiguration getConfigurationNamed(String name) {
    try {
      for (ILaunchConfiguration config : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()) {
        if (name.equals(config.getName())) {
          return config;
        }
      }
    } catch (CoreException exception) {
      DartUtil.logError(exception);
    }

    return null;
  }

  private IAction getDeleteAction() {
    if (deleteAction == null) {
      deleteAction = new DeleteLaunchConfigurationAction(launchesViewer, getMode());
    }

    return deleteAction;
  }

  private void refreshLaunchesViewer() {
    launchesViewer.refresh();
  }

  private void refreshTable() {
    launchesViewer.refresh();
  }

  private void saveConfig() {
    if (currentTabGroup != null) {
      currentTabGroup.performApply(workingCopy);

      try {
        workingCopy.doSave();
      } catch (CoreException e) {
        DebugErrorHandler.errorDialog(
            getShell(),
            "Error Saving Launch",
            "Unable to save launch settings: " + e.toString(),
            e);
      }
    }

    updateButtons();
    updateMessage();
    refreshTable();
  }

  private void selectFirstLaunchConfig() {
    final ILaunchConfiguration launchConfig = (ILaunchConfiguration) launchesViewer.getElementAt(0);

    if (launchConfig != null && launchesViewer.getSelection().isEmpty()) {
      launchesViewer.setSelection(new StructuredSelection(launchConfig));
    }
  }

  private void selectLaunchConfigFromPage() {

    ILaunchConfiguration config = selectedConfig;
    if (config == null) {
      IResource resource = LaunchUtils.getSelectedResource(window);
      if (resource != null) {
        List<ILaunchConfiguration> configs = LaunchUtils.getExistingLaunchesFor(resource);
        if (!configs.isEmpty()) {
          config = configs.get(0);
        }
      }
    }

    if (config != null) {
      launchesViewer.setSelection(new StructuredSelection(config));
    } else {
      selectFirstLaunchConfig();
    }
  }

  private void show(ILaunchConfiguration config) {
    try {
      launchConfig = config;
      workingCopy = launchConfig.getWorkingCopy();
      configNameText.setText(workingCopy.getName());

      currentTabGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(
          workingCopy,
          getMode());
      currentTabGroup.createTabs(this, getMode());

      ILaunchConfigurationTab[] tabs = currentTabGroup.getTabs();

      for (int i = 0; i < tabs.length; i++) {
        tabs[i].setLaunchConfigurationDialog(this);
      }

      setActiveTab(0);
    } catch (CoreException ce) {
      DebugErrorHandler.errorDialog(
          getShell(),
          "Error Displaying Launch",
          "Unable to display launch settings: " + ce.toString(),
          ce);
    }
  }

  /**
   * Verify that the launch configuration name is valid.
   */
  private void verifyName() throws CoreException {
    if (configNameText.isVisible()) {
      ILaunchManager mgr = getLaunchManager();
      String currentName = configNameText.getText().trim();

      // If there is no name, complain
      if (currentName.length() < 1) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugUIPlugin.PLUGIN_ID,
            0,
            Messages.ManageLaunchesDialog_Name_required_for_launch_configuration,
            null));
      }
      try {
        mgr.isValidLaunchConfigurationName(currentName);
      } catch (IllegalArgumentException iae) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugUIPlugin.PLUGIN_ID,
            0,
            iae.getMessage(),
            null));
      }
      // Otherwise, if there's already a config with the same name, complain
      if (!launchConfig.getName().equals(currentName)) {
        if (mgr.isExistingLaunchConfigurationName(currentName)) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              DartDebugUIPlugin.PLUGIN_ID,
              0,
              Messages.ManageLaunchesDialog_Launch_configuration_already_exists_with_this_name,
              null));
        }
      }
    }
  }
}

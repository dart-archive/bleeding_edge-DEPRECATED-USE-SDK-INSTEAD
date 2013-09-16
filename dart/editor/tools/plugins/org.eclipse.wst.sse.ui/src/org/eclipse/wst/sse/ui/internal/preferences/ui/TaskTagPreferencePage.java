/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.tasks.TaskScanningScheduler;
import org.eclipse.wst.sse.core.internal.tasks.TaskTagPreferenceKeys;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.ui.internal.preferences.TabFolderLayout;
import org.osgi.service.prefs.BackingStoreException;

public class TaskTagPreferencePage extends PropertyPreferencePage {

  private static final boolean _debugPreferences = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.core/tasks/preferences")); //$NON-NLS-1$ //$NON-NLS-2$
  // Remember the last displayed tab for convenience
  private static final String TASK_TAG_LAST_TAB = "task-tag-last-tab"; //$NON-NLS-1$

  private int detectionRequested = 0;

  private Button fEnableCheckbox = null;

  private boolean fEnableTaskTags = true;

  private boolean fOriginalEnableTaskTags = true;

  private IPreferencesService fPreferencesService = null;

  private Button fRedetectButton;

  private SelectionListener fTabEnablementListener;

  private TabFolder fTabFolder;
  private IPreferenceTab[] fTabs = null;

  public TaskTagPreferencePage() {
    super();
    fPreferencesService = Platform.getPreferencesService();
  }

  protected void contributeButtons(Composite parent) {
    if (getElement() == null) {
      ((GridLayout) parent.getLayout()).numColumns += 2;
      fRedetectButton = new Button(parent, SWT.PUSH);
      fRedetectButton.setText(SSEUIMessages.TaskTagPreferencePage_32); //$NON-NLS-1$
      fRedetectButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END
          | GridData.VERTICAL_ALIGN_FILL));
      fRedetectButton.setEnabled(true);
      fRedetectButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          Job redetectJob = new Job(SSEUIMessages.TaskTagPreferenceTab_27) { //$NON-NLS-1$
            public Object getAdapter(Class adapter) {
              return null;
            }

            protected IStatus run(IProgressMonitor monitor) {
              TaskScanningScheduler.refresh();
              return Status.OK_STATUS;
            }
          };
          redetectJob.schedule();
        }
      });
      Label spacer = new Label(parent, SWT.NONE);
      spacer.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END
          | GridData.VERTICAL_ALIGN_CENTER));
    }
    super.contributeButtons(parent);
  }

  protected Control createCommonContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout compositeLayout = new GridLayout();
    compositeLayout.marginLeft = 0;
    compositeLayout.marginRight = 0;
    composite.setLayout(compositeLayout);

    IScopeContext[] preferenceScopes = createPreferenceScopes();
    fOriginalEnableTaskTags = fEnableTaskTags = fPreferencesService.getBoolean(
        getPreferenceNodeQualifier(), TaskTagPreferenceKeys.TASK_TAG_ENABLE, false,
        preferenceScopes);
    fEnableCheckbox = new Button(composite, SWT.CHECK);
    fEnableCheckbox.setSelection(fEnableTaskTags);
    fEnableCheckbox.setText(SSEUIMessages.TaskTagPreferenceTab_31); //$NON-NLS-1$
    fEnableCheckbox.setSelection(fEnableTaskTags);
    fEnableCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
        GridData.VERTICAL_ALIGN_END, false, false, 1, 1));

    fTabFolder = new TabFolder(composite, SWT.NONE);
    fTabFolder.setLayout(new TabFolderLayout());
    fTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

    TabItem taskItem = new TabItem(fTabFolder, SWT.NONE);
    MainTab mainTab = new MainTab(this, fPreferencesService, preferenceScopes);
    taskItem.setText(mainTab.getTitle());
    final Control taskTagsControl = mainTab.createContents(fTabFolder);
    taskItem.setControl(taskTagsControl);

    TabItem exclusionItem = new TabItem(fTabFolder, SWT.NONE);
    ExclusionsTab exclusionsTab = new ExclusionsTab(this, fPreferencesService, preferenceScopes);
    exclusionItem.setText(exclusionsTab.getTitle());
    final Control exclusionControl = exclusionsTab.createContents(fTabFolder);
    exclusionItem.setControl(exclusionControl);

    fTabs = new IPreferenceTab[] {mainTab, exclusionsTab};

    fTabEnablementListener = new SelectionAdapter() {
      ControlEnableState[] lastEnableStates = null;

      public void widgetSelected(SelectionEvent e) {
        fEnableTaskTags = fEnableCheckbox.getSelection();
        if (fEnableTaskTags) {
          if (lastEnableStates != null) {
            for (int i = 0; i < lastEnableStates.length; i++) {
              if (lastEnableStates[i] != null) {
                lastEnableStates[i].restore();
              }
            }
            lastEnableStates = null;
            fTabFolder.redraw();
          }
        } else if (lastEnableStates == null) {
          lastEnableStates = new ControlEnableState[fTabs.length + 1];
          lastEnableStates[0] = ControlEnableState.disable(taskTagsControl);
          lastEnableStates[1] = ControlEnableState.disable(exclusionControl);
          if (fRedetectButton != null) {
            lastEnableStates[2] = ControlEnableState.disable(fRedetectButton);
          }
        }
      }
    };
    fTabEnablementListener.widgetSelected(null);
    fEnableCheckbox.addSelectionListener(fTabEnablementListener);

    // restore last selected tab
    int activeTab = new DefaultScope().getNode(getPreferenceNodeQualifier()).getInt(
        TASK_TAG_LAST_TAB, 0);
    if (activeTab > 0) {
      fTabFolder.setSelection(activeTab);
    }

    SSEUIPlugin.getDefault().getWorkbench().getHelpSystem().setHelp(composite,
        IHelpContextIds.PREFWEBX_TASKTAGS_HELPID);
    return composite;
  }

  protected String getPreferenceNodeQualifier() {
    return TaskTagPreferenceKeys.TASK_TAG_NODE;
  }

  protected String getPreferencePageID() {
    return "org.eclipse.wst.sse.ui.preferences.tasktags";//$NON-NLS-1$

  }

  protected String getProjectSettingsKey() {
    return TaskTagPreferenceKeys.TASK_TAG_PER_PROJECT;
  }

  protected String getPropertyPageID() {
    return "org.eclipse.wst.sse.ui.project.properties.tasktags";//$NON-NLS-1$
  }

  public String getTitle() {
    return SSEUIMessages.TaskTagPreferenceTab_20; //$NON-NLS-1$
  }

  public void init(IWorkbench workbench) {
  }

  public void performApply() {
    super.performApply();
    save();

    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performApply();
    }

    promptForRedetectIfNecessary();
  }

  public void performDefaults() {
    super.performDefaults();
    IEclipsePreferences defaultPreferences = createPreferenceScopes()[1].getNode(getPreferenceNodeQualifier());
    fEnableTaskTags = defaultPreferences.getBoolean(TaskTagPreferenceKeys.TASK_TAG_ENABLE, false);
    fEnableCheckbox.setSelection(fEnableTaskTags);
    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performDefaults();
    }
    fTabEnablementListener.widgetSelected(null);
    if (_debugPreferences) {
      System.out.println("Loading defaults in " + getClass().getName()); //$NON-NLS-1$
    }
  }

  public boolean performOk() {
    boolean ok = super.performOk();
    save();

    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performOk();
    }

    promptForRedetectIfNecessary();

    // save last tab (use Default scope since it won't be persisted)
    new DefaultScope().getNode(getPreferenceNodeQualifier()).putInt(TASK_TAG_LAST_TAB,
        fTabFolder.getSelectionIndex());

    IScopeContext[] contexts = createPreferenceScopes();
    // remove project-specific information if it's not enabled
    if (getProject() != null && !isElementSettingsEnabled()) {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_PER_PROJECT + " (" + true + ") in scope " + contexts[0].getName() + ":" + contexts[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
      }
      contexts[0].getNode(getPreferenceNodeQualifier()).remove(
          TaskTagPreferenceKeys.TASK_TAG_PER_PROJECT);
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED + " (" + true + ") in scope " + contexts[0].getName() + ":" + contexts[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
      }
      contexts[0].getNode(getPreferenceNodeQualifier()).remove(
          TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED);
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_PRIORITIES + " (" + true + ") in scope " + contexts[0].getName() + ":" + contexts[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
      }
      contexts[0].getNode(getPreferenceNodeQualifier()).remove(
          TaskTagPreferenceKeys.TASK_TAG_PRIORITIES);
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_TAGS + " (" + true + ") in scope " + contexts[0].getName() + ":" + contexts[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
      }
      contexts[0].getNode(getPreferenceNodeQualifier()).remove(TaskTagPreferenceKeys.TASK_TAG_TAGS);
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_ENABLE + " (" + true + ") in scope " + contexts[0].getName() + ":" + contexts[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
      }
      contexts[0].getNode(getPreferenceNodeQualifier()).remove(
          TaskTagPreferenceKeys.TASK_TAG_ENABLE);
    }
    for (int i = 0; i < contexts.length; i++) {
      try {
        contexts[i].getNode(getPreferenceNodeQualifier()).flush();
      } catch (BackingStoreException e) {
        Logger.logException(
            "problem saving preference settings to scope " + contexts[i].getName(), e); //$NON-NLS-1$
      }
    }

    return ok;
  }

  private void promptForRedetectIfNecessary() {
    if (detectionRequested > 0) {
      MessageDialog dialog = new MessageDialog(
          PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          SSEUIMessages.TaskTagPreferenceTab_22,
          PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getImage(),
          SSEUIMessages.TaskTagPreferenceTab_23, MessageDialog.QUESTION, new String[] {
              SSEUIMessages.TaskTagPreferenceTab_24, SSEUIMessages.TaskTagPreferenceTab_25,
              SSEUIMessages.TaskTagPreferenceTab_26}, 2); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
      int button = dialog.open();
      if (button == 0) {
        Job redetectJob = new Job(SSEUIMessages.TaskTagPreferenceTab_27) { //$NON-NLS-1$
          public Object getAdapter(Class adapter) {
            return null;
          }

          protected IStatus run(IProgressMonitor monitor) {
            if (getProject() == null) {
              if (_debugPreferences) {
                System.out.println(getClass().getName() + ": rescanning all"); //$NON-NLS-1$
              }
              TaskScanningScheduler.refresh();
            } else {
              if (_debugPreferences) {
                System.out.println(getClass().getName() + ": rescanning " + getProject()); //$NON-NLS-1$
              }
              TaskScanningScheduler.refresh(getProject());
            }
            return Status.OK_STATUS;
          }
        };
        redetectJob.schedule(500);
      }
      detectionRequested = 0;
    }
  }

  void requestRedetection() {
    detectionRequested++;
  }

  private void save() {
    if (fEnableTaskTags != fOriginalEnableTaskTags) {
      requestRedetection();
    }
    fOriginalEnableTaskTags = fEnableTaskTags;

    IScopeContext[] preferenceScopes = createPreferenceScopes();
    IEclipsePreferences defaultPreferences = preferenceScopes[1].getNode(getPreferenceNodeQualifier());
    boolean defaultEnable = defaultPreferences.getBoolean(TaskTagPreferenceKeys.TASK_TAG_ENABLE,
        false);
    if (fEnableTaskTags == defaultEnable) {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_ENABLE + " from scope " + preferenceScopes[0].getName() + ":" + preferenceScopes[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
      preferenceScopes[0].getNode(getPreferenceNodeQualifier()).remove(
          TaskTagPreferenceKeys.TASK_TAG_ENABLE);
    } else {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " setting " + TaskTagPreferenceKeys.TASK_TAG_ENABLE + " \"" + fEnableTaskTags + "\" in scope " + preferenceScopes[0].getName() + ":" + preferenceScopes[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      preferenceScopes[0].getNode(getPreferenceNodeQualifier()).putBoolean(
          TaskTagPreferenceKeys.TASK_TAG_ENABLE, fEnableTaskTags);
    }
    if (getProject() != null && isElementSettingsEnabled()) {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " setting " + TaskTagPreferenceKeys.TASK_TAG_PER_PROJECT + " (" + true + ") in scope " + preferenceScopes[0].getName() + ":" + preferenceScopes[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
      }
      preferenceScopes[0].getNode(getPreferenceNodeQualifier()).putBoolean(
          TaskTagPreferenceKeys.TASK_TAG_PER_PROJECT, true);
    } else {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_PER_PROJECT + " from scope " + preferenceScopes[0].getName() + ":" + preferenceScopes[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      preferenceScopes[0].getNode(getPreferenceNodeQualifier()).remove(
          TaskTagPreferenceKeys.TASK_TAG_PER_PROJECT);
    }
    try {
      fPreferencesService.getRootNode().flush();
    } catch (BackingStoreException e) {
      // log it, there is no recovery
      Logger.logException(e);
    }
  }
}

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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.dialogs.StatusUtil;
import com.google.dart.tools.ui.internal.dialogs.fields.DialogField;
import com.google.dart.tools.ui.internal.dialogs.fields.IDialogFieldListener;
import com.google.dart.tools.ui.internal.dialogs.fields.LayoutUtil;
import com.google.dart.tools.ui.internal.dialogs.fields.SelectionButtonDialogField;
import com.google.dart.tools.ui.internal.preferences.PreferencesMessages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Base for project property and preference pages
 */
public abstract class PropertyAndPreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage, IWorkbenchPropertyPage {

  private Control fConfigurationBlockControl;
  private ControlEnableState fBlockEnableState;
  private Link fChangeWorkspaceSettings;
  private SelectionButtonDialogField fUseProjectSettings;
  private IStatus fBlockStatus;
  private Composite fParentComposite;

  private IProject fProject; // project or null
  private Map<String, Object> fData; // page data

  public static final String DATA_NO_LINK = "PropertyAndPreferencePage.nolink"; //$NON-NLS-1$

  public PropertyAndPreferencePage() {
    fBlockStatus = new StatusInfo();
    fBlockEnableState = null;
    fProject = null;
    fData = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void applyData(Object data) {
    if (data instanceof Map) {
      fData = (Map<String, Object>) data;
    }
    if (fChangeWorkspaceSettings != null) {
      if (!offerLink()) {
        fChangeWorkspaceSettings.dispose();
        fParentComposite.layout(true, true);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
   */
  @Override
  public IAdaptable getElement() {
    return fProject;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  @Override
  public void init(IWorkbench workbench) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPropertyPage#setElement(org.eclipse.core.runtime.IAdaptable)
   */
  @Override
  public void setElement(IAdaptable element) {
    fProject = (IProject) element.getAdapter(IResource.class);
  }

  /*
   * @see org.eclipse.jface.preference.IPreferencePage#createContents(Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setFont(parent.getFont());

    GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);

    fConfigurationBlockControl = createPreferenceContent(composite);
    fConfigurationBlockControl.setLayoutData(data);

    if (isProjectPreferencePage()) {
      boolean useProjectSettings = hasProjectSpecificOptions(getProject());
      enableProjectSpecificSettings(useProjectSettings);
    }

    Dialog.applyDialogFont(composite);
    return composite;
  }

  @Override
  protected Label createDescriptionLabel(Composite parent) {
    fParentComposite = parent;
    if (isProjectPreferencePage()) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setFont(parent.getFont());
      GridLayout layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 2;
      composite.setLayout(layout);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

      IDialogFieldListener listener = new IDialogFieldListener() {
        @Override
        public void dialogFieldChanged(DialogField field) {
          boolean enabled = ((SelectionButtonDialogField) field).isSelected();
          enableProjectSpecificSettings(enabled);

          if (enabled && getData() != null) {
            applyData(getData());
          }
        }
      };

      fUseProjectSettings = new SelectionButtonDialogField(SWT.CHECK);
      fUseProjectSettings.setDialogFieldListener(listener);
      fUseProjectSettings.setLabelText(PreferencesMessages.PropertyAndPreferencePage_useprojectsettings_label);
      fUseProjectSettings.doFillIntoGrid(composite, 1);
      LayoutUtil.setHorizontalGrabbing(fUseProjectSettings.getSelectionButton(null));

      if (offerLink()) {
        fChangeWorkspaceSettings = createLink(composite,
            PreferencesMessages.PropertyAndPreferencePage_useworkspacesettings_change);
        fChangeWorkspaceSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
      } else {
        LayoutUtil.setHorizontalSpan(fUseProjectSettings.getSelectionButton(null), 2);
      }

      Label horizontalLine = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
      horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
      horizontalLine.setFont(composite.getFont());
    } else if (supportsProjectSpecificOptions() && offerLink()) {
      fChangeWorkspaceSettings = createLink(parent,
          PreferencesMessages.PropertyAndPreferencePage_showprojectspecificsettings_label);
      fChangeWorkspaceSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
    }

    return super.createDescriptionLabel(parent);
  }

  protected abstract Control createPreferenceContent(Composite composite);

  protected void doStatusChanged() {
    if (!isProjectPreferencePage() || useProjectSettings()) {
      updateStatus(fBlockStatus);
    } else {
      updateStatus(new StatusInfo());
    }
  }

  protected void enablePreferenceContent(boolean enable) {
    if (enable) {
      if (fBlockEnableState != null) {
        fBlockEnableState.restore();
        fBlockEnableState = null;
      }
    } else {
      if (fBlockEnableState == null) {
        fBlockEnableState = ControlEnableState.disable(fConfigurationBlockControl);
      }
    }
  }

  protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
    fUseProjectSettings.setSelection(useProjectSpecificSettings);
    enablePreferenceContent(useProjectSpecificSettings);
    updateLinkVisibility();
    doStatusChanged();
  }

  protected Map<String, Object> getData() {
    return fData;
  }

  protected IStatus getPreferenceContentStatus() {
    return fBlockStatus;
  }

  protected abstract String getPreferencePageID();

  protected IProject getProject() {
    return fProject;
  }

  protected abstract String getPropertyPageID();

  protected abstract boolean hasProjectSpecificOptions(IProject project);

  protected boolean isProjectPreferencePage() {
    return fProject != null;
  }

  protected boolean offerLink() {
    return fData == null || !Boolean.TRUE.equals(fData.get(DATA_NO_LINK));
  }

  protected final void openProjectProperties(IProject project, Object data) {
    String id = getPropertyPageID();
    if (id != null) {
      PreferencesUtil.createPropertyDialogOn(getShell(), project, id, new String[] {id}, data).open();
    }
  }

  protected final void openWorkspacePreferences(Object data) {
    String id = getPreferencePageID();
    PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] {id}, data).open();
  }

  /*
   * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    if (useProjectSettings()) {
      enableProjectSpecificSettings(false);
    }
    super.performDefaults();
  }

  protected void setPreferenceContentStatus(IStatus status) {
    fBlockStatus = status;
    doStatusChanged();
  }

  protected boolean supportsProjectSpecificOptions() {
    return getPropertyPageID() != null;
  }

  protected boolean useProjectSettings() {
    return isProjectPreferencePage() && fUseProjectSettings != null
        && fUseProjectSettings.isSelected();
  }

  /**
   * Handle link activation.
   * 
   * @param link the link
   */
  final void doLinkActivated(Link link) {
    Map<String, Object> data = getData();
    if (data == null) {
      data = new HashMap<String, Object>();
    }
    data.put(DATA_NO_LINK, Boolean.TRUE);

    if (isProjectPreferencePage()) {
      openWorkspacePreferences(data);
    } else {
      HashSet<DartProject> projectsWithSpecifics = new HashSet<DartProject>();
      try {
        DartProject[] projects = DartCore.create(ResourcesPlugin.getWorkspace().getRoot()).getDartProjects();
        for (int i = 0; i < projects.length; i++) {
          DartProject curr = projects[i];
          if (hasProjectSpecificOptions(curr.getProject())) {
            projectsWithSpecifics.add(curr);
          }
        }
      } catch (DartModelException e) {
        // ignore
      }
      ProjectSelectionDialog dialog = new ProjectSelectionDialog(getShell(), projectsWithSpecifics);
      if (dialog.open() == Window.OK) {
        DartProject res = (DartProject) dialog.getFirstResult();
        openProjectProperties(res.getProject(), data);
      }
    }
  }

  private Link createLink(Composite composite, String text) {
    Link link = new Link(composite, SWT.NONE);
    link.setFont(composite.getFont());
    link.setText("<A>" + text + "</A>"); //$NON-NLS-1$//$NON-NLS-2$
    link.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        doLinkActivated((Link) e.widget);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        doLinkActivated((Link) e.widget);
      }
    });
    return link;
  }

  private void updateLinkVisibility() {
    if (fChangeWorkspaceSettings == null || fChangeWorkspaceSettings.isDisposed()) {
      return;
    }

    if (isProjectPreferencePage()) {
      fChangeWorkspaceSettings.setEnabled(!useProjectSettings());
    }
  }

  private void updateStatus(IStatus status) {
    setValid(!status.matches(IStatus.ERROR));
    StatusUtil.applyToStatusLine(this, status);
  }

}

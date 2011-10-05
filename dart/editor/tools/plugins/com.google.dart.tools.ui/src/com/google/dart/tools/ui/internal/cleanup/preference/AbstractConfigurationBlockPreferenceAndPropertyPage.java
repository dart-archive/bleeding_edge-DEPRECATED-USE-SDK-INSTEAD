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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Abstract preference and property page which is used to wrap a
 * {@link org.eclipse.jdt.internal.ui.preferences.IPreferenceAndPropertyConfigurationBlock}.
 * 
 * @since 3.3
 */
public abstract class AbstractConfigurationBlockPreferenceAndPropertyPage extends
    PropertyAndPreferencePage {

  private IPreferenceAndPropertyConfigurationBlock fConfigurationBlock;
  private PreferencesAccess fAccess;

  public AbstractConfigurationBlockPreferenceAndPropertyPage() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    fConfigurationBlock.dispose();
    super.dispose();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performDefaults() {
    fConfigurationBlock.performDefaults();
    super.performDefaults();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean performOk() {
    fConfigurationBlock.performOk();

    try {
      fAccess.applyChanges();
    } catch (BackingStoreException e) {
      DartToolsPlugin.log(e);
    }

    return true;
  }

  /**
   * Create a configuration block which does modify settings in <code>context</code>.
   * 
   * @param context the context to modify
   * @return the preference block, not null
   */
  protected abstract IPreferenceAndPropertyConfigurationBlock createConfigurationBlock(
      IScopeContext context);

  /**
   * {@inheritDoc}
   */
  @Override
  protected Control createPreferenceContent(Composite parent) {

    IPreferencePageContainer container = getContainer();
    IWorkingCopyManager manager;
    if (container instanceof IWorkbenchPreferenceContainer) {
      manager = ((IWorkbenchPreferenceContainer) container).getWorkingCopyManager();
    } else {
      manager = new WorkingCopyManager(); // non shared
    }
    fAccess = PreferencesAccess.getWorkingCopyPreferences(manager);
    IProject project = getProject();
    IScopeContext context;
    if (project != null) {
      context = fAccess.getProjectScope(project);
    } else {
      context = fAccess.getInstanceScope();
    }

    fConfigurationBlock = createConfigurationBlock(context);

    Control content = fConfigurationBlock.createControl(parent);

    fConfigurationBlock.initialize();

    Dialog.applyDialogFont(content);
    return content;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
    super.enableProjectSpecificSettings(useProjectSpecificSettings);
    if (useProjectSpecificSettings) {
      fConfigurationBlock.enableProjectSettings();
    } else {
      fConfigurationBlock.disableProjectSettings();
    }
  }

  protected abstract String getHelpId();
}

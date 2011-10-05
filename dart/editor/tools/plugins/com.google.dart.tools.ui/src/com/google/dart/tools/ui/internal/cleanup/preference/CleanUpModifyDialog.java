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
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.cleanup.ICleanUpConfigurationUI;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;
import com.google.dart.tools.ui.internal.cleanup.CleanUpRegistry.CleanUpTabPageDescriptor;
import com.google.dart.tools.ui.internal.cleanup.preference.ProfileManager.Profile;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.Map;

public class CleanUpModifyDialog extends ModifyDialog {

  /**
   * Constant array for boolean selection
   */
  static String[] FALSE_TRUE = {CleanUpOptions.FALSE, CleanUpOptions.TRUE};

  private Label fCountLabel;
  private ICleanUpConfigurationUI[] fPages;

  public CleanUpModifyDialog(Shell parentShell, Profile profile, ProfileManager profileManager,
      ProfileStore profileStore, boolean newProfile, String dialogPreferencesKey,
      String lastSavePathKey) {
    super(parentShell, profile, profileManager, profileStore, newProfile, dialogPreferencesKey,
        lastSavePathKey);
  }

  @Override
  public void updateStatus(IStatus status) {
    int count = 0;
    for (int i = 0; i < fPages.length; i++) {
      count += fPages[i].getSelectedCleanUpCount();
    }
    if (count == 0) {
      super.updateStatus(new Status(IStatus.ERROR, DartUI.ID_PLUGIN,
          CleanUpMessages.CleanUpModifyDialog_SelectOne_Error));
    } else {
      super.updateStatus(status);
    }
  }

  @Override
  public void valuesModified() {
    super.valuesModified();
    updateCountLabel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void addPages(final Map<String, String> values) {
    CleanUpTabPageDescriptor[] descriptors = DartToolsPlugin.getDefault().getCleanUpRegistry().getCleanUpTabPageDescriptors(
        CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);

    fPages = new ICleanUpConfigurationUI[descriptors.length];

    for (int i = 0; i < descriptors.length; i++) {
      String name = descriptors[i].getName();
      CleanUpTabPage page = descriptors[i].createTabPage();

      page.setOptionsKind(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
      page.setModifyListener(this);
      page.setWorkingValues(values);

      addTabPage(name, page);

      fPages[i] = page;
    }
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite control = (Composite) super.createDialogArea(parent);

    fCountLabel = new Label(control, SWT.NONE);
    fCountLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    fCountLabel.setFont(parent.getFont());
    updateCountLabel();

    return control;
  }

  /**
   * {@inheritDoc}
   * 
   * @since 3.5
   */
  @Override
  protected String getHelpContextId() {
    return IJavaHelpContextIds.CLEAN_UP_PREFERENCE_PAGE;
  }

  private void updateCountLabel() {
    int size = 0, count = 0;
    for (int i = 0; i < fPages.length; i++) {
      size += fPages[i].getCleanUpCount();
      count += fPages[i].getSelectedCleanUpCount();
    }

    fCountLabel.setText(Messages.format(CleanUpMessages.CleanUpModifyDialog_XofYSelected_Label,
        new Object[] {new Integer(count), new Integer(size)}));
  }
}

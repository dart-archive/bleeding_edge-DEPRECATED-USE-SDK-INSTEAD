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
import com.google.dart.tools.ui.internal.cleanup.preference.ProfileManager.CustomProfile;
import com.google.dart.tools.ui.internal.cleanup.preference.ProfileManager.Profile;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.dialogs.fields.DialogField;
import com.google.dart.tools.ui.internal.dialogs.fields.IDialogFieldListener;
import com.google.dart.tools.ui.internal.dialogs.fields.StringDialogField;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ModifyDialog extends StatusDialog implements
    IModifyDialogTabPage.IModificationListener {

  /**
   * The keys to retrieve the preferred area from the dialog settings.
   */
  private static final String DS_KEY_PREFERRED_WIDTH = "modify_dialog.preferred_width"; //$NON-NLS-1$
  private static final String DS_KEY_PREFERRED_HEIGHT = "modify_dialog.preferred_height"; //$NON-NLS-1$
  private static final String DS_KEY_PREFERRED_X = "modify_dialog.preferred_x"; //$NON-NLS-1$
  private static final String DS_KEY_PREFERRED_Y = "modify_dialog.preferred_y"; //$NON-NLS-1$

  /**
   * The key to store the number (beginning at 0) of the tab page which had the focus last time.
   */
  private static final String DS_KEY_LAST_FOCUS = "modify_dialog.last_focus"; //$NON-NLS-1$

  private static final int APPLAY_BUTTON_ID = IDialogConstants.CLIENT_ID;
  private static final int SAVE_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;

  private final String fKeyPreferredWidth;
  private final String fKeyPreferredHight;
  private final String fKeyPreferredX;
  private final String fKeyPreferredY;
  private final String fKeyLastFocus;
  private final String fLastSaveLoadPathKey;
  private final ProfileStore fProfileStore;
  private final boolean fNewProfile;
  private Profile fProfile;
  private final Map<String, String> fWorkingValues;
  private final List<IModifyDialogTabPage> fTabPages;
  private final IDialogSettings fDialogSettings;
  private TabFolder fTabFolder;
  private final ProfileManager fProfileManager;
  private Button fApplyButton;
  private Button fSaveButton;
  private StringDialogField fProfileNameField;

  public ModifyDialog(Shell parentShell, Profile profile, ProfileManager profileManager,
      ProfileStore profileStore, boolean newProfile, String dialogPreferencesKey,
      String lastSavePathKey) {
    super(parentShell);

    fProfileStore = profileStore;
    fLastSaveLoadPathKey = lastSavePathKey;

    fKeyPreferredWidth = DartUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_WIDTH;
    fKeyPreferredHight = DartUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_HEIGHT;
    fKeyPreferredX = DartUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_X;
    fKeyPreferredY = DartUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_Y;
    fKeyLastFocus = DartUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_LAST_FOCUS;

    fProfileManager = profileManager;
    fNewProfile = newProfile;

    fProfile = profile;
    setTitle(Messages.format(FormatterMessages.ModifyDialog_dialog_title, profile.getName()));
    fWorkingValues = new HashMap<String, String>(fProfile.getSettings());
    setStatusLineAboveButtons(false);
    fTabPages = new ArrayList<IModifyDialogTabPage>();
    fDialogSettings = DartToolsPlugin.getDefault().getDialogSettings();
  }

  @Override
  public boolean close() {
    final Rectangle shell = getShell().getBounds();

    fDialogSettings.put(fKeyPreferredWidth, shell.width);
    fDialogSettings.put(fKeyPreferredHight, shell.height);
    fDialogSettings.put(fKeyPreferredX, shell.x);
    fDialogSettings.put(fKeyPreferredY, shell.y);

    return super.close();
  }

  @Override
  public void create() {
    super.create();
    int lastFocusNr = 0;
    try {
      lastFocusNr = fDialogSettings.getInt(fKeyLastFocus);
      if (lastFocusNr < 0) {
        lastFocusNr = 0;
      }
      if (lastFocusNr > fTabPages.size() - 1) {
        lastFocusNr = fTabPages.size() - 1;
      }
    } catch (NumberFormatException x) {
      lastFocusNr = 0;
    }

    if (!fNewProfile) {
      fTabFolder.setSelection(lastFocusNr);
      ((IModifyDialogTabPage) fTabFolder.getSelection()[0].getData()).setInitialFocus();
    }
  }

  @Override
  public void updateStatus(IStatus status) {
    if (status == null) {
      doValidate();
    } else {
      super.updateStatus(status);
    }
  }

  @Override
  public void valuesModified() {
    doValidate();
  }

  protected abstract void addPages(Map<String, String> values);

  protected final void addTabPage(String title, IModifyDialogTabPage tabPage) {
    final TabItem tabItem = new TabItem(fTabFolder, SWT.NONE);
    applyDialogFont(tabItem.getControl());
    tabItem.setText(title);
    tabItem.setData(tabPage);
    tabItem.setControl(tabPage.createContents(fTabFolder));
    fTabPages.add(tabPage);
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == APPLAY_BUTTON_ID) {
      applyPressed();
      setTitle(Messages.format(FormatterMessages.ModifyDialog_dialog_title, fProfile.getName()));
    } else if (buttonId == SAVE_BUTTON_ID) {
      saveButtonPressed();
    } else {
      super.buttonPressed(buttonId);
    }
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    fApplyButton = createButton(parent, APPLAY_BUTTON_ID,
        FormatterMessages.ModifyDialog_apply_button, false);
    fApplyButton.setEnabled(false);

    GridLayout layout = (GridLayout) parent.getLayout();
    layout.numColumns++;
    layout.makeColumnsEqualWidth = false;
    Label label = new Label(parent, SWT.NONE);
    GridData data = new GridData();
    data.widthHint = layout.horizontalSpacing;
    label.setLayoutData(data);
    super.createButtonsForButtonBar(parent);
  }

  @Override
  protected Control createDialogArea(Composite parent) {

    final Composite composite = (Composite) super.createDialogArea(parent);

    Composite nameComposite = new Composite(composite, SWT.NONE);
    nameComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    nameComposite.setLayout(new GridLayout(3, false));

    fProfileNameField = new StringDialogField();
    fProfileNameField.setLabelText(FormatterMessages.ModifyDialog_ProfileName_Label);
    fProfileNameField.setText(fProfile.getName());
    fProfileNameField.getLabelControl(nameComposite).setLayoutData(
        new GridData(SWT.LEFT, SWT.CENTER, false, false));
    fProfileNameField.getTextControl(nameComposite).setLayoutData(
        new GridData(SWT.FILL, SWT.CENTER, true, false));
    fProfileNameField.setDialogFieldListener(new IDialogFieldListener() {
      @Override
      public void dialogFieldChanged(DialogField field) {
        doValidate();
      }
    });

    fSaveButton = createButton(nameComposite, SAVE_BUTTON_ID,
        FormatterMessages.ModifyDialog_Export_Button, false);

    fTabFolder = new TabFolder(composite, SWT.NONE);
    fTabFolder.setFont(composite.getFont());
    fTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    addPages(fWorkingValues);

    applyDialogFont(composite);

    fTabFolder.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        final TabItem tabItem = (TabItem) e.item;
        final IModifyDialogTabPage page = (IModifyDialogTabPage) tabItem.getData();
        //				page.fSashForm.setWeights();
        fDialogSettings.put(fKeyLastFocus, fTabPages.indexOf(page));
        page.makeVisible();
      }
    });

    doValidate();

    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, getHelpContextId());

    return composite;
  }

  /**
   * Returns the context ID for the Help system
   * 
   * @return the string used as ID for the Help context
   * @since 3.5
   */
  protected abstract String getHelpContextId();

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.window.Window#getInitialLocation(org.eclipse.swt.graphics.Point)
   */
  @Override
  protected Point getInitialLocation(Point initialSize) {
    try {
      return new Point(fDialogSettings.getInt(fKeyPreferredX),
          fDialogSettings.getInt(fKeyPreferredY));
    } catch (NumberFormatException ex) {
      return super.getInitialLocation(initialSize);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.window.Window#getInitialSize()
   */
  @Override
  protected Point getInitialSize() {
    Point initialSize = super.getInitialSize();
    try {
      int lastWidth = fDialogSettings.getInt(fKeyPreferredWidth);
      if (initialSize.x > lastWidth) {
        lastWidth = initialSize.x;
      }
      int lastHeight = fDialogSettings.getInt(fKeyPreferredHight);
      if (initialSize.y > lastHeight) {
        lastHeight = initialSize.y;
      }
      return new Point(lastWidth, lastHeight);
    } catch (NumberFormatException ex) {
    }
    return initialSize;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#isResizable()
   * 
   * @since 3.4
   */
  @Override
  protected boolean isResizable() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    applyPressed();
    super.okPressed();
  }

  @Override
  protected void updateButtonsEnableState(IStatus status) {
    super.updateButtonsEnableState(status);
    if (fApplyButton != null && !fApplyButton.isDisposed()) {
      fApplyButton.setEnabled(hasChanges() && !status.matches(IStatus.ERROR));
    }
    if (fSaveButton != null && !fSaveButton.isDisposed()) {
      fSaveButton.setEnabled(!validateProfileName().matches(IStatus.ERROR));
    }
  }

  private void applyPressed() {
    if (!fProfile.getName().equals(fProfileNameField.getText())) {
      fProfile = fProfile.rename(fProfileNameField.getText(), fProfileManager);
    }
    fProfile.setSettings(new HashMap<String, String>(fWorkingValues));
    fProfileManager.setSelected(fProfile);
    doValidate();
  }

  private void doValidate() {
    String name = fProfileNameField.getText().trim();
    if (name.equals(fProfile.getName())
        && fProfile.hasEqualSettings(fWorkingValues, fWorkingValues.keySet())) {
      updateStatus(StatusInfo.OK_STATUS);
      return;
    }

    IStatus status = validateProfileName();
    if (status.matches(IStatus.ERROR)) {
      updateStatus(status);
      return;
    }

    if (!name.equals(fProfile.getName()) && fProfileManager.containsName(name)) {
      updateStatus(new Status(IStatus.ERROR, DartUI.ID_PLUGIN,
          FormatterMessages.ModifyDialog_Duplicate_Status));
      return;
    }

    if (fProfile.isBuiltInProfile() || fProfile.isSharedProfile()) {
      updateStatus(new Status(IStatus.INFO, DartUI.ID_PLUGIN,
          FormatterMessages.ModifyDialog_NewCreated_Status));
      return;
    }

    updateStatus(StatusInfo.OK_STATUS);
  }

  private boolean hasChanges() {
    if (!fProfileNameField.getText().trim().equals(fProfile.getName())) {
      return true;
    }

    Iterator<Entry<String, String>> iter = fProfile.getSettings().entrySet().iterator();
    for (; iter.hasNext();) {
      Entry<String, String> curr = iter.next();
      if (!fWorkingValues.get(curr.getKey()).equals(curr.getValue())) {
        return true;
      }
    }
    return false;
  }

  private void saveButtonPressed() {
    Profile selected = new CustomProfile(fProfileNameField.getText(), new HashMap<String, String>(
        fWorkingValues), fProfile.getVersion(),
        fProfileManager.getProfileVersioner().getProfileKind());

    final FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
    dialog.setText(FormatterMessages.CodingStyleConfigurationBlock_save_profile_dialog_title);
    dialog.setFilterExtensions(new String[] {"*.xml"}); //$NON-NLS-1$

    final String lastPath = DartToolsPlugin.getDefault().getDialogSettings().get(
        fLastSaveLoadPathKey + ".savepath"); //$NON-NLS-1$
    if (lastPath != null) {
      dialog.setFilterPath(lastPath);
    }
    final String path = dialog.open();
    if (path == null) {
      return;
    }

    DartToolsPlugin.getDefault().getDialogSettings().put(
        fLastSaveLoadPathKey + ".savepath", dialog.getFilterPath()); //$NON-NLS-1$

    final File file = new File(path);
    if (file.exists()
        && !MessageDialog.openQuestion(getShell(),
            FormatterMessages.CodingStyleConfigurationBlock_save_profile_overwrite_title,
            Messages.format(
                FormatterMessages.CodingStyleConfigurationBlock_save_profile_overwrite_message,
//                BasicElementLabels.getPathLabel(file)))) {
                file.getAbsolutePath()))) {
      return;
    }
    String encoding = ProfileStore.ENCODING;
    final IContentType type = Platform.getContentTypeManager().getContentType(
        "com.google.dart.tools.core.runtime.xml"); //$NON-NLS-1$
    if (type != null) {
      encoding = type.getDefaultCharset();
    }
    final Collection<Profile> profiles = new ArrayList<Profile>();
    profiles.add(selected);
    try {
      fProfileStore.writeProfilesToFile(profiles, file, encoding);
    } catch (CoreException e) {
      final String title = FormatterMessages.CodingStyleConfigurationBlock_save_profile_error_title;
      final String message = FormatterMessages.CodingStyleConfigurationBlock_save_profile_error_message;
      ExceptionHandler.handle(e, getShell(), title, message);
    }
  }

  private IStatus validateProfileName() {
    final String name = fProfileNameField.getText().trim();

    if (fProfile.isBuiltInProfile()) {
      if (fProfile.getName().equals(name)) {
        return new Status(IStatus.ERROR, DartUI.ID_PLUGIN,
            FormatterMessages.ModifyDialog_BuiltIn_Status);
      }
    }

    if (fProfile.isSharedProfile()) {
      if (fProfile.getName().equals(name)) {
        return new Status(IStatus.ERROR, DartUI.ID_PLUGIN,
            FormatterMessages.ModifyDialog_Shared_Status);
      }
    }

    if (name.length() == 0) {
      return new Status(IStatus.ERROR, DartUI.ID_PLUGIN,
          FormatterMessages.ModifyDialog_EmptyName_Status);
    }

    return StatusInfo.OK_STATUS;
  }

}

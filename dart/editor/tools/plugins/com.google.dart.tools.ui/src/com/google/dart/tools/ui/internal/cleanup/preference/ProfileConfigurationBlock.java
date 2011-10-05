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
import com.google.dart.tools.ui.internal.preferences.PreferencesMessages;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public abstract class ProfileConfigurationBlock {

  class ButtonController implements Observer, SelectionListener {

    public ButtonController() {
      fProfileManager.addObserver(this);
      fNewButton.addSelectionListener(this);
      fEditButton.addSelectionListener(this);
      fDeleteButton.addSelectionListener(this);
      fLoadButton.addSelectionListener(this);
      fExportAllButton.addSelectionListener(this);
      update(fProfileManager, null);
    }

    @Override
    public void update(Observable o, Object arg) {
      Profile selected = ((ProfileManager) o).getSelected();
      final boolean notBuiltIn = !selected.isBuiltInProfile();
      fDeleteButton.setEnabled(notBuiltIn);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      final Button button = (Button) e.widget;
      if (button == fEditButton) {
        modifyButtonPressed();
      } else if (button == fDeleteButton) {
        deleteButtonPressed();
      } else if (button == fNewButton) {
        newButtonPressed();
      } else if (button == fLoadButton) {
        loadButtonPressed();
      } else if (button == fExportAllButton) {
        exportAllButtonPressed();
      }
    }

    private void deleteButtonPressed() {
      if (MessageDialog.openQuestion(fComposite.getShell(),
          FormatterMessages.CodingStyleConfigurationBlock_delete_confirmation_title,
          Messages.format(
              FormatterMessages.CodingStyleConfigurationBlock_delete_confirmation_question,
              fProfileManager.getSelected().getName()))) {
        fProfileManager.deleteSelected();
      }
    }

    /**
     * Exports all the profiles to a file.
     * 
     * @since 3.6
     */
    private void exportAllButtonPressed() {
      final FileDialog dialog = new FileDialog(fComposite.getShell(), SWT.SAVE);
      dialog.setText(FormatterMessages.CodingStyleConfigurationBlock_export_profiles_dialog_title);
      dialog.setFilterExtensions(new String[] {"*.xml"}); //$NON-NLS-1$
      final String lastPath = DartToolsPlugin.getDefault().getDialogSettings().get(
          fLastSaveLoadPathKey + ".loadpath"); //$NON-NLS-1$
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
          && !MessageDialog.openQuestion(
              fComposite.getShell(),
              FormatterMessages.CodingStyleConfigurationBlock_export_profiles_overwrite_title,
              Messages.format(
                  FormatterMessages.CodingStyleConfigurationBlock_export_profiles_overwrite_message,
                  file.getAbsolutePath()))) {
        return;
      }
      String encoding = ProfileStore.ENCODING;
      final IContentType type = Platform.getContentTypeManager().getContentType(
          "org.eclipse.core.runtime.xml"); //$NON-NLS-1$
      if (type != null) {
        encoding = type.getDefaultCharset();
      }
      final Collection<Profile> profiles = new ArrayList<Profile>();
      profiles.addAll(fProfileManager.getSortedProfiles());
      try {
        fProfileStore.writeProfilesToFile(profiles, file, encoding);
      } catch (CoreException e) {
        final String title = FormatterMessages.CodingStyleConfigurationBlock_export_profiles_error_title;
        final String message = FormatterMessages.CodingStyleConfigurationBlock_export_profiles_error_message;
        ExceptionHandler.handle(e, fComposite.getShell(), title, message);
      }

    }

    private void loadButtonPressed() {
      final FileDialog dialog = new FileDialog(fComposite.getShell(), SWT.OPEN);
      dialog.setText(FormatterMessages.CodingStyleConfigurationBlock_load_profile_dialog_title);
      dialog.setFilterExtensions(new String[] {"*.xml"}); //$NON-NLS-1$
      final String lastPath = DartToolsPlugin.getDefault().getDialogSettings().get(
          fLastSaveLoadPathKey + ".loadpath"); //$NON-NLS-1$
      if (lastPath != null) {
        dialog.setFilterPath(lastPath);
      }
      final String path = dialog.open();
      if (path == null) {
        return;
      }
      DartToolsPlugin.getDefault().getDialogSettings().put(
          fLastSaveLoadPathKey + ".loadpath", dialog.getFilterPath()); //$NON-NLS-1$

      final File file = new File(path);
      Collection<Profile> profiles = null;
      try {
        profiles = fProfileStore.readProfilesFromFile(file);
      } catch (CoreException e) {
        final String title = FormatterMessages.CodingStyleConfigurationBlock_load_profile_error_title;
        final String message = FormatterMessages.CodingStyleConfigurationBlock_load_profile_error_message;
        ExceptionHandler.handle(e, fComposite.getShell(), title, message);
      }
      if (profiles == null || profiles.isEmpty()) {
        return;
      }
      Iterator<Profile> iter = profiles.iterator();
      while (iter.hasNext()) {
        final CustomProfile profile = (CustomProfile) iter.next();

        if (!fProfileVersioner.getProfileKind().equals(profile.getKind())) {
          final String title = FormatterMessages.CodingStyleConfigurationBlock_load_profile_error_title;
          final String message = Messages.format(
              FormatterMessages.ProfileConfigurationBlock_load_profile_wrong_profile_message,
              new String[] {fProfileVersioner.getProfileKind(), profile.getKind()});
          MessageDialog.openError(fComposite.getShell(), title, message);
          return;
        }

        if (profile.getVersion() > fProfileVersioner.getCurrentVersion()) {
          final String title = FormatterMessages.CodingStyleConfigurationBlock_load_profile_error_too_new_title;
          final String message = FormatterMessages.CodingStyleConfigurationBlock_load_profile_error_too_new_message;
          MessageDialog.openWarning(fComposite.getShell(), title, message);
        }

        if (fProfileManager.containsName(profile.getName())) {
          final AlreadyExistsDialog aeDialog = new AlreadyExistsDialog(fComposite.getShell(),
              profile, fProfileManager);
          if (aeDialog.open() != Window.OK) {
            return;
          }
        }
        fProfileVersioner.update(profile);
        fProfileManager.addProfile(profile);
      }
    }

    private void modifyButtonPressed() {
      final StatusDialog modifyDialog = createModifyDialog(fComposite.getShell(),
          fProfileManager.getSelected(), fProfileManager, fProfileStore, false);
      modifyDialog.open();
    }

    private void newButtonPressed() {
      final CreateProfileDialog p = new CreateProfileDialog(fComposite.getShell(), fProfileManager,
          fProfileVersioner);
      if (p.open() != Window.OK) {
        return;
      }
      if (!p.openEditDialog()) {
        return;
      }
      final StatusDialog modifyDialog = createModifyDialog(fComposite.getShell(),
          p.getCreatedProfile(), fProfileManager, fProfileStore, true);
      modifyDialog.open();
    }
  }

  class ProfileComboController implements Observer, SelectionListener {

    private final List<Profile> fSortedProfiles;

    public ProfileComboController() {
      fSortedProfiles = fProfileManager.getSortedProfiles();
      fProfileCombo.addSelectionListener(this);
      fProfileManager.addObserver(this);
      updateProfiles();
      updateSelection();
    }

    @Override
    public void update(Observable o, Object arg) {
      if (arg == null) {
        return;
      }
      final int value = ((Integer) arg).intValue();
      switch (value) {
        case ProfileManager.PROFILE_CREATED_EVENT:
        case ProfileManager.PROFILE_DELETED_EVENT:
        case ProfileManager.PROFILE_RENAMED_EVENT:
          updateProfiles();
          updateSelection();
          break;
        case ProfileManager.SELECTION_CHANGED_EVENT:
          updateSelection();
          break;
      }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      final int index = fProfileCombo.getSelectionIndex();
      fProfileManager.setSelected(fSortedProfiles.get(index));
    }

    private void updateProfiles() {
      fProfileCombo.setItems(fProfileManager.getSortedDisplayNames());
    }

    private void updateSelection() {
      fProfileCombo.setText(fProfileManager.getSelected().getName());
    }
  }

  private class StoreUpdater implements Observer {

    public StoreUpdater() {
      fProfileManager.addObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
      try {
        fPreferenceListenerEnabled = false;
        final int value = ((Integer) arg).intValue();
        switch (value) {
          case ProfileManager.PROFILE_DELETED_EVENT:
          case ProfileManager.PROFILE_RENAMED_EVENT:
          case ProfileManager.PROFILE_CREATED_EVENT:
          case ProfileManager.SETTINGS_CHANGED_EVENT:
            try {
              fProfileStore.writeProfiles(fProfileManager.getSortedProfiles(), fInstanceScope); // update profile store
              fProfileManager.commitChanges(fCurrContext);
            } catch (CoreException x) {
              DartToolsPlugin.log(x);
            }
            break;
          case ProfileManager.SELECTION_CHANGED_EVENT:
            fProfileManager.commitChanges(fCurrContext);
            break;
        }
      } finally {
        fPreferenceListenerEnabled = true;
      }
    }
  }

  protected static Label createLabel(Composite composite, String text, int numColumns) {
    final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = numColumns;
    gd.widthHint = 0;

    final Label label = new Label(composite, SWT.WRAP);
    label.setFont(composite.getFont());
    label.setText(text);
    label.setLayoutData(gd);
    return label;
  }

  private static Button createButton(Composite composite, String text, final int style) {
    final Button button = new Button(composite, SWT.PUSH);
    button.setFont(composite.getFont());
    button.setText(text);

    final GridData gd = new GridData(style);
    gd.widthHint = SWTUtil.getButtonWidthHint(button);
    button.setLayoutData(gd);
    return button;
  }

  private static Combo createProfileCombo(Composite composite, int span, int widthHint) {
    final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = span;
    gd.widthHint = widthHint;

    final Combo combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
    combo.setFont(composite.getFont());
    SWTUtil.setDefaultVisibleItemCount(combo);
    combo.setLayoutData(gd);
    return combo;
  }

  /**
   * The GUI controls
   */
  private Composite fComposite;
  private Combo fProfileCombo;
  private Button fEditButton;
  private Button fDeleteButton;

  private Button fNewButton;
  private Button fLoadButton;
  private Button fExportAllButton;
  private PixelConverter fPixConv;
  /**
   * The ProfileManager, the model of this page.
   */
  private final ProfileManager fProfileManager;
  private final IScopeContext fCurrContext;
  private final IScopeContext fInstanceScope;
  private final ProfileStore fProfileStore;
  private final IProfileVersioner fProfileVersioner;
  private final String fLastSaveLoadPathKey;

  private IPreferenceChangeListener fPreferenceListener;

  private final PreferencesAccess fPreferenceAccess;

  private boolean fPreferenceListenerEnabled;

  public ProfileConfigurationBlock(IProject project, final PreferencesAccess access,
      String lastSaveLoadPathKey) {

    fPreferenceAccess = access;
    fLastSaveLoadPathKey = lastSaveLoadPathKey;

    fProfileVersioner = createProfileVersioner();
    fProfileStore = createProfileStore(fProfileVersioner);
    fInstanceScope = access.getInstanceScope();
    if (project != null) {
      fCurrContext = access.getProjectScope(project);
    } else {
      fCurrContext = fInstanceScope;
    }

    List<Profile> profiles = null;
    try {
      profiles = fProfileStore.readProfiles(fInstanceScope);
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
    if (profiles == null) {
      try {
        // bug 129427
        profiles = fProfileStore.readProfiles(PreferencesAccess.DEFAULT_SCOPE);
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      }
    }

    if (profiles == null) {
      profiles = new ArrayList<Profile>();
    }

    fProfileManager = createProfileManager(profiles, fCurrContext, access, fProfileVersioner);

    new StoreUpdater();

    fPreferenceListenerEnabled = true;
    fPreferenceListener = new IPreferenceChangeListener() {
      @Override
      public void preferenceChange(PreferenceChangeEvent event) {
        if (fPreferenceListenerEnabled) {
          preferenceChanged(event);
        }
      }
    };
    access.getInstanceScope().getNode(DartUI.ID_PLUGIN).addPreferenceChangeListener(
        fPreferenceListener);

  }

  /**
   * Create the contents
   * 
   * @param parent Parent composite
   * @return Created control
   */
  public Composite createContents(Composite parent) {

    final int numColumns = 5;

    fPixConv = new PixelConverter(parent);
    fComposite = createComposite(parent, numColumns);

    Label profileLabel = new Label(fComposite, SWT.NONE);
    profileLabel.setText(PreferencesMessages.CleanUpPreferencePage_Description);
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
    data.horizontalSpan = numColumns;
    profileLabel.setLayoutData(data);

    fProfileCombo = createProfileCombo(fComposite, 3, fPixConv.convertWidthInCharsToPixels(20));
    fEditButton = createButton(fComposite,
        FormatterMessages.CodingStyleConfigurationBlock_edit_button_desc,
        GridData.HORIZONTAL_ALIGN_BEGINNING);
    fDeleteButton = createButton(fComposite,
        FormatterMessages.CodingStyleConfigurationBlock_remove_button_desc,
        GridData.HORIZONTAL_ALIGN_BEGINNING);

    fNewButton = createButton(fComposite,
        FormatterMessages.CodingStyleConfigurationBlock_new_button_desc,
        GridData.HORIZONTAL_ALIGN_BEGINNING);
    fLoadButton = createButton(fComposite,
        FormatterMessages.CodingStyleConfigurationBlock_load_button_desc,
        GridData.HORIZONTAL_ALIGN_END);
    fExportAllButton = createButton(fComposite,
        FormatterMessages.CodingStyleConfigurationBlock_export_all_button_desc,
        GridData.HORIZONTAL_ALIGN_BEGINNING);
    createLabel(fComposite, "", 3); //$NON-NLS-1$

    configurePreview(fComposite, numColumns, fProfileManager);

    new ButtonController();
    new ProfileComboController();

    return fComposite;
  }

  public void dispose() {
    if (fPreferenceListener != null) {
      fPreferenceAccess.getInstanceScope().getNode(DartUI.ID_PLUGIN).removePreferenceChangeListener(
          fPreferenceListener);
      fPreferenceListener = null;
    }
  }

  public void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
    if (useProjectSpecificSettings) {
      fProfileManager.commitChanges(fCurrContext);
    } else {
      fProfileManager.clearAllSettings(fCurrContext);
    }
  }

  public final boolean hasProjectSpecificOptions(IProject project) {
    if (project != null) {
      return fProfileManager.hasProjectSpecificSettings(new ProjectScope(project));
    }
    return false;
  }

  public void performApply() {
    try {
      fCurrContext.getNode(DartUI.ID_PLUGIN).flush();
      fCurrContext.getNode(DartUI.ID_PLUGIN).flush();
      if (fCurrContext != fInstanceScope) {
        fInstanceScope.getNode(DartUI.ID_PLUGIN).flush();
        fInstanceScope.getNode(DartUI.ID_PLUGIN).flush();
      }
    } catch (BackingStoreException e) {
      DartToolsPlugin.log(e);
    }
  }

  public void performDefaults() {
    Profile profile = fProfileManager.getDefaultProfile();
    if (profile != null) {
      int defaultIndex = fProfileManager.getSortedProfiles().indexOf(profile);
      if (defaultIndex != -1) {
        fProfileManager.setSelected(profile);
      }
    }
  }

  public boolean performOk() {
    return true;
  }

  protected abstract void configurePreview(Composite composite, int numColumns,
      ProfileManager profileManager);

  protected abstract ModifyDialog createModifyDialog(Shell shell, Profile profile,
      ProfileManager profileManager, ProfileStore profileStore, boolean newProfile);

  protected abstract ProfileManager createProfileManager(List<Profile> profiles,
      IScopeContext context, PreferencesAccess access, IProfileVersioner profileVersioner);

  protected abstract ProfileStore createProfileStore(IProfileVersioner versioner);

  protected abstract IProfileVersioner createProfileVersioner();

  /**
   * Notifies that a preference has been changed.
   * 
   * @param event the preference change event
   */
  protected void preferenceChanged(PreferenceChangeEvent event) {

  }

  private Composite createComposite(Composite parent, int numColumns) {
    final Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());

    final GridLayout layout = new GridLayout(numColumns, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    return composite;
  }

}

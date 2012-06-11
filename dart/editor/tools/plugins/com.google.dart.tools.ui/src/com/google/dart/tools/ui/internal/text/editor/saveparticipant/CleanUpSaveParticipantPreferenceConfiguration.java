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
package com.google.dart.tools.ui.internal.text.editor.saveparticipant;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;
import com.google.dart.tools.ui.internal.cleanup.CleanUpPostSaveListener;
import com.google.dart.tools.ui.internal.cleanup.MapCleanUpOptions;
import com.google.dart.tools.ui.internal.cleanup.preference.BulletListBlock;
import com.google.dart.tools.ui.internal.cleanup.preference.CleanUpPreferenceUtil;
import com.google.dart.tools.ui.internal.cleanup.preference.PreferencesAccess;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Preference configuration UI for the clean up save participant.
 */
@SuppressWarnings("unused")
public class CleanUpSaveParticipantPreferenceConfiguration extends
    AbstractSaveParticipantPreferenceConfiguration {

  private static final int INDENT = 10;

  private IScopeContext fContext;
  private Map<String, String> fSettings;
  private BulletListBlock fSelectedActionsText;
//  private Button fFormatCodeButton;
//  private Button fFormatChangedOnlyButton;
//  private Button fOrganizeImportsButton;
  private Shell fShell;
//  private Link fFormatConfigLink;
//  private Link fOrganizeImportsConfigLink;
  private IPreferencePageContainer fContainer;
//  private Button fAdditionalActionButton;
  private Button fConfigureButton;
//  private Button fFormatAllButton;

  private Composite fCleanUpOptionsComposite;
  private ControlEnableState fControlEnableState;

  /**
   * {@inheritDoc}
   */
  @Override
  public void createConfigControl(final Composite parent, IPreferencePageContainer container) {
    fContainer = container;
    fShell = parent.getShell();

    fCleanUpOptionsComposite = new Composite(parent, SWT.NONE);
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    gridData.horizontalIndent = INDENT;
    fCleanUpOptionsComposite.setLayoutData(gridData);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    fCleanUpOptionsComposite.setLayout(gridLayout);

//    fFormatCodeButton = new Button(fCleanUpOptionsComposite, SWT.CHECK);
//    fFormatCodeButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_FormatSource_Checkbox);
//    fFormatCodeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
//    fFormatCodeButton.addSelectionListener(new SelectionAdapter() {
//      /**
//       * {@inheritDoc}
//       */
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        changeSettingsValue(CleanUpConstants.FORMAT_SOURCE_CODE, fFormatCodeButton.getSelection());
//      }
//    });

    Composite regionFormatingCombo = new Composite(fCleanUpOptionsComposite, SWT.NONE);
    gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
    gridData.horizontalIndent = 20;
    regionFormatingCombo.setLayoutData(gridData);
    gridLayout = new GridLayout(1, false);
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    regionFormatingCombo.setLayout(gridLayout);

//    fFormatAllButton = new Button(regionFormatingCombo, SWT.RADIO);
//    fFormatAllButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePAge_FormatAllLines_Radio);
//    gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
//    fFormatAllButton.setLayoutData(gridData);
//    fFormatAllButton.addSelectionListener(new SelectionAdapter() {
//      /**
//       * {@inheritDoc}
//       */
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        changeSettingsValue(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY,
//            !fFormatAllButton.getSelection());
//      }
//    });
//
//    fFormatChangedOnlyButton = new Button(regionFormatingCombo, SWT.RADIO);
//    fFormatChangedOnlyButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_FormatOnlyChangedRegions_Radio);
//    gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
//    fFormatChangedOnlyButton.setLayoutData(gridData);
//    fFormatChangedOnlyButton.addSelectionListener(new SelectionAdapter() {
//      /**
//       * {@inheritDoc}
//       */
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        changeSettingsValue(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY,
//            fFormatChangedOnlyButton.getSelection());
//      }
//    });
//
//    PixelConverter pixelConverter = new PixelConverter(parent);
//    int heightOneHalf = (int) Math.round(pixelConverter.convertHeightInCharsToPixels(1) * 1.5);
//
//    fFormatConfigLink = new Link(fCleanUpOptionsComposite, SWT.NONE);
//    fFormatConfigLink.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_ConfigureFormatter_Link);
//    GridData gridData2 = new GridData(SWT.LEFT, SWT.TOP, false, false);
//    gridData2.horizontalIndent = 20;
//    gridData2.minimumHeight = heightOneHalf;
//    fFormatConfigLink.setLayoutData(gridData2);
//
//    fOrganizeImportsButton = new Button(fCleanUpOptionsComposite, SWT.CHECK);
//    fOrganizeImportsButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_OrganizeImports_Checkbox);
//    fOrganizeImportsButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
//    fOrganizeImportsButton.addSelectionListener(new SelectionAdapter() {
//      /**
//       * {@inheritDoc}
//       */
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        changeSettingsValue(CleanUpConstants.ORGANIZE_IMPORTS,
//            fOrganizeImportsButton.getSelection());
//      }
//    });
//
//    fOrganizeImportsConfigLink = new Link(fCleanUpOptionsComposite, SWT.NONE);
//    fOrganizeImportsConfigLink.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_ConfigureImports_Link);
//    GridData gridData3 = new GridData(SWT.LEFT, SWT.TOP, false, false);
//    gridData3.horizontalIndent = 20;
//    gridData3.minimumHeight = heightOneHalf;
//    fOrganizeImportsConfigLink.setLayoutData(gridData3);
//
//    fAdditionalActionButton = new Button(fCleanUpOptionsComposite, SWT.CHECK);
//    fAdditionalActionButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_AdditionalActions_Checkbox);
//
    createAdvancedComposite(fCleanUpOptionsComposite);
//    fAdditionalActionButton.addSelectionListener(new SelectionAdapter() {
//      /**
//       * {@inheritDoc}
//       */
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        changeSettingsValue(CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS,
//            fAdditionalActionButton.getSelection());
//      }
//    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disableProjectSettings() {
    super.disableProjectSettings();

    IEclipsePreferences node = fContext.getNode(DartUI.ID_PLUGIN);

    Set<String> keys = DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
        CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS).getKeys();
    for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
      String key = iterator.next();
      node.remove(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + key);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void enableProjectSettings() {
    super.enableProjectSettings();

    CleanUpPreferenceUtil.saveSaveParticipantOptions(fContext, fSettings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(final IScopeContext context, IAdaptable element) {
    fContext = context;
    fSettings = CleanUpPreferenceUtil.loadSaveParticipantOptions(context);

    settingsChanged();

    DartProject javaProject = null;
    if (element != null) {
      IProject project = (IProject) element.getAdapter(IProject.class);
      if (project != null) {
        DartProject jProject = DartCore.create(project);
        if (jProject != null && jProject.exists()) {
          javaProject = jProject;
        }
      }
    }

//    configurePreferenceLink(fFormatConfigLink, javaProject, CodeFormatterPreferencePage.PREF_ID,
//        CodeFormatterPreferencePage.PROP_ID);

    super.initialize(context, element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performDefaults() {
    if (ProjectScope.SCOPE.equals(fContext.getName()) && !hasSettingsInScope(fContext)) {
      return;
    }

    enabled(true);

    if (ProjectScope.SCOPE.equals(fContext.getName())) {
      fSettings = CleanUpPreferenceUtil.loadSaveParticipantOptions(PreferencesAccess.INSTANCE_SCOPE);
    } else {
      fSettings = DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
          CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS).getMap();
    }
    settingsChanged();

    super.performDefaults();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performOk() {
    super.performOk();

    if (!ProjectScope.SCOPE.equals(fContext.getName()) || hasSettingsInScope(fContext)) {
      CleanUpPreferenceUtil.saveSaveParticipantOptions(fContext, fSettings);
    }
  }

  @Override
  protected void enableConfigControl(boolean isEnabled) {
    super.enableConfigControl(isEnabled);
    fSelectedActionsText.setEnabled(isEnabled);
    fConfigureButton.setEnabled(isEnabled);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void enabled(boolean isEnabled) {
    if (isEnabled) {
      if (fControlEnableState == null) {
        return;
      }

      fControlEnableState.restore();
      fControlEnableState = null;
    } else {
      if (fControlEnableState != null) {
        return;
      }

      fControlEnableState = ControlEnableState.disable(fCleanUpOptionsComposite);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getPostSaveListenerId() {
    return CleanUpPostSaveListener.POSTSAVELISTENER_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getPostSaveListenerName() {
    return SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_CleanUpActionsTopNodeName_Checkbox;
  }

  private void changeSettingsValue(String key, boolean enabled) {
    String value;
    if (enabled) {
      value = CleanUpOptions.TRUE;
    } else {
      value = CleanUpOptions.FALSE;
    }
    fSettings.put(key, value);
    settingsChanged();
  }

  private void configurePreferenceLink(Link link, final DartProject javaProject,
      final String preferenceId, final String propertyId) {
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fContainer instanceof IWorkbenchPreferenceContainer) {
          IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) fContainer;
          if (javaProject != null) {
            container.openPage(propertyId, null);
          } else {
            container.openPage(preferenceId, null);
          }
        } else {
          PreferencesUtil.createPreferenceDialogOn(fShell, preferenceId, null, null);
        }
      }
    });
  }

  private Composite createAdvancedComposite(final Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    gridData.horizontalIndent = INDENT;
    composite.setLayoutData(gridData);
    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);

    fSelectedActionsText = new BulletListBlock(composite, SWT.NONE);
    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    gridData.heightHint = new PixelConverter(composite).convertHeightInCharsToPixels(8);
    fSelectedActionsText.setLayoutData(gridData);

    fConfigureButton = new Button(composite, SWT.NONE);
    fConfigureButton.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_Configure_Button);
    fConfigureButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    fConfigureButton.addSelectionListener(new SelectionAdapter() {
      /**
       * {@inheritDoc}
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fConfigureButton.isEnabled()) {
          fSettings.put(CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, CleanUpOptions.TRUE);
        } else {
          fSettings.put(CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, CleanUpOptions.FALSE);
        }
        Hashtable<String, String> workingValues = new Hashtable<String, String>(fSettings);
        SaveActionSelectionDialog dialog = new SaveActionSelectionDialog(parent.getShell(),
            workingValues);
        if (dialog.open() == Window.OK) {
          fSettings = workingValues;
          settingsChanged();
        }
      }

    });

    return composite;
  }

  private String getSelectedCleanUpsText(CleanUpOptions options) {
    StringBuffer buf = new StringBuffer();

    final ICleanUp[] cleanUps = DartToolsPlugin.getDefault().getCleanUpRegistry().createCleanUps();
    for (int i = 0; i < cleanUps.length; i++) {
      cleanUps[i].setOptions(options);
      String[] descriptions = cleanUps[i].getStepDescriptions();
      if (descriptions != null) {
        for (int j = 0; j < descriptions.length; j++) {
          if (buf.length() > 0) {
            buf.append('\n');
          }
          buf.append(descriptions[j]);
        }
      }
    }
    String string = buf.toString();
    return string;
  }

  private void settingsChanged() {
    Map<String, String> settings = new HashMap<String, String>(fSettings);
    CleanUpOptions options = new MapCleanUpOptions(settings);

    fSelectedActionsText.setText(getSelectedCleanUpsText(options));
  }
}

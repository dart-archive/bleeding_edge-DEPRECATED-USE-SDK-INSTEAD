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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.utilities.io.Base16;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_catch_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_get_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_library_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_operators_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.style.Style_trailingSpace_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.style.Style_useBlocks_CleanUp;
import com.google.dart.tools.ui.internal.util.GridDataFactory;
import com.google.dart.tools.ui.internal.util.GridLayoutFactory;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import java.util.Map;
import java.util.Map.Entry;

public class CleanUpRefactoringWizard extends RefactoringWizard {

//  private static class CleanUpConfigurationPage extends UserInputWizardPage implements
//      IModifyDialogTabPage.IModificationListener {
//
//    private static final class ProfileTableAdapter implements IListAdapter<DartProject> {
//      private final ProjectProfileLableProvider fProvider;
//      private final Shell fShell;
//
//      private ProfileTableAdapter(ProjectProfileLableProvider provider, Shell shell) {
//        fProvider = provider;
//        fShell = shell;
//      }
//
//      public void customButtonPressed(ListDialogField<DartProject> field, int index) {
//        openPropertyDialog(field);
//      }
//
//      public void doubleClicked(ListDialogField<DartProject> field) {
//        openPropertyDialog(field);
//      }
//
//      public void selectionChanged(ListDialogField<DartProject> field) {
//        if (field.getSelectedElements().size() != 1) {
//          field.enableButton(0, false);
//        } else {
//          field.enableButton(0, true);
//        }
//      }
//
//      private void openPropertyDialog(ListDialogField<DartProject> field) {
//        DartProject project = field.getSelectedElements().get(0);
//        PreferencesUtil.createPropertyDialogOn(
//            fShell,
//            project,
//            CleanUpPreferencePage.PROP_ID,
//            null,
//            null).open();
//        List<?> selectedElements = field.getSelectedElements();
//        fProvider.reset();
//        field.refresh();
//        field.selectElements(new StructuredSelection(selectedElements));
//      }
//    }
//
//    private static final class WizardCleanUpSelectionDialog extends CleanUpSelectionDialog {
//
//      private static final String CLEAN_UP_SELECTION_PREFERENCE_KEY = "clean_up_selection_dialog"; //$NON-NLS-1$
//
//      private WizardCleanUpSelectionDialog(Shell parent, Map<String, String> settings) {
//        super(
//            parent,
//            settings,
//            MultiFixMessages.CleanUpRefactoringWizard_CustomCleanUpsDialog_title);
//      }
//
//      @Override
//      protected NamedCleanUpTabPage[] createTabPages(Map<String, String> workingValues) {
//        CleanUpTabPageDescriptor[] descriptors = DartToolsPlugin.getDefault().getCleanUpRegistry().getCleanUpTabPageDescriptors(
//            CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
//
//        NamedCleanUpTabPage[] result = new NamedCleanUpTabPage[descriptors.length];
//
//        for (int i = 0; i < descriptors.length; i++) {
//          String name = descriptors[i].getName();
//          CleanUpTabPage page = descriptors[i].createTabPage();
//
//          page.setOptionsKind(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
//          page.setModifyListener(this);
//          page.setWorkingValues(workingValues);
//
//          result[i] = new NamedCleanUpTabPage(name, page);
//        }
//
//        return result;
//      }
//
//      @Override
//      protected String getEmptySelectionMessage() {
//        return MultiFixMessages.CleanUpRefactoringWizard_EmptySelection_message;
//      }
//
//      @Override
//      protected String getPreferenceKeyPrefix() {
//        return CLEAN_UP_SELECTION_PREFERENCE_KEY;
//      }
//
//      @Override
//      protected String getSelectionCountMessage(int selectionCount, int size) {
//        return Messages.format(
//            MultiFixMessages.CleanUpRefactoringWizard_XofYCleanUpsSelected_message,
//            new Object[] {new Integer(selectionCount), new Integer(size)});
//      }
//    }
//
//    private static final String ENCODING = "UTF-8"; //$NON-NLS-1$
//
//    private final CleanUpRefactoring fCleanUpRefactoring;
//    private Map<String, String> fCustomSettings;
//    private SelectionButtonDialogField fUseCustomField;
//
//    private ControlEnableState fEnableState;
//
//    public CleanUpConfigurationPage(CleanUpRefactoring refactoring) {
//      super(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_title);
//      fCleanUpRefactoring = refactoring;
//      int cleanUpTargetsSize = fCleanUpRefactoring.getCleanUpTargetsSize();
//      DartProject[] projects = fCleanUpRefactoring.getProjects();
//      if (cleanUpTargetsSize == 1) {
//        setMessage(MultiFixMessages.CleanUpRefactoringWizard_CleaningUp11_Title);
//      } else if (projects.length == 1) {
//        setMessage(Messages.format(
//            MultiFixMessages.CleanUpRefactoringWizard_CleaningUpN1_Title,
//            new Integer(cleanUpTargetsSize)));
//      } else {
//        setMessage(Messages.format(
//            MultiFixMessages.CleanUpRefactoringWizard_CleaningUpNN_Title,
//            new Object[] {new Integer(cleanUpTargetsSize), new Integer(projects.length)}));
//      }
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void createControl(Composite parent) {
//      initializeDialogUnits(parent);
//
//      boolean isCustom = getDialogSettings().getBoolean(USE_CUSTOM_PROFILE_KEY);
//
//      final Composite composite = new Composite(parent, SWT.NONE);
//      composite.setLayout(new GridLayout(2, false));
//      composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//      composite.setFont(parent.getFont());
//
//      SelectionButtonDialogField useProfile = new SelectionButtonDialogField(SWT.RADIO);
//      useProfile.setLabelText(MultiFixMessages.CleanUpRefactoringWizard_use_configured_radio);
//      useProfile.setSelection(!isCustom);
//      useProfile.doFillIntoGrid(composite, 2);
//
//      ProjectProfileLableProvider tableLabelProvider = new ProjectProfileLableProvider();
//      IListAdapter<DartProject> listAdapter = new ProfileTableAdapter(
//          tableLabelProvider,
//          getShell());
//      String[] buttons = new String[] {MultiFixMessages.CleanUpRefactoringWizard_Configure_Button};
//      final ListDialogField<DartProject> settingsField = new ListDialogField<DartProject>(
//          listAdapter,
//          buttons,
//          tableLabelProvider) {
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        protected int getListStyle() {
//          return super.getListStyle() | SWT.SINGLE;
//        }
//      };
//
//      String[] headerNames = new String[] {
//          MultiFixMessages.CleanUpRefactoringWizard_Project_TableHeader,
//          MultiFixMessages.CleanUpRefactoringWizard_Profile_TableHeader};
//      ColumnLayoutData[] columns = new ColumnLayoutData[] {
//          new ColumnWeightData(2, true), new ColumnWeightData(1, true)};
//      settingsField.setTableColumns(new ListDialogField.ColumnsDescription(
//          columns,
//          headerNames,
//          true));
//      settingsField.setViewerComparator(new ViewerComparator());
//
//      settingsField.doFillIntoGrid(composite, 3);
//
//      Table table = settingsField.getTableViewer().getTable();
//      GridData data = (GridData) settingsField.getListControl(null).getLayoutData();
//      data.horizontalIndent = 15;
//      data.grabExcessVerticalSpace = false;
//      data.heightHint = SWTUtil.getTableHeightHint(
//          table,
//          Math.min(5, fCleanUpRefactoring.getProjects().length + 2));
//      data.grabExcessHorizontalSpace = true;
//      data.verticalAlignment = GridData.BEGINNING;
//
//      data = (GridData) settingsField.getButtonBox(null).getLayoutData();
//      data.grabExcessVerticalSpace = false;
//      data.verticalAlignment = GridData.BEGINNING;
//
//      data = (GridData) settingsField.getLabelControl(null).getLayoutData();
//      data.exclude = true;
//
//      settingsField.setElements(Arrays.asList(fCleanUpRefactoring.getProjects()));
//      settingsField.selectFirstElement();
//
//      fUseCustomField = new SelectionButtonDialogField(SWT.RADIO);
//      fUseCustomField.setLabelText(MultiFixMessages.CleanUpRefactoringWizard_use_custom_radio);
//      fUseCustomField.setSelection(isCustom);
//      fUseCustomField.doFillIntoGrid(composite, 2);
//
//      String settings = getDialogSettings().get(CUSTOM_PROFILE_KEY);
//      if (settings == null) {
//        fCustomSettings = DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
//            CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS).getMap();
//      } else {
//        try {
//          fCustomSettings = decodeSettings(settings);
//        } catch (CoreException e) {
//          DartToolsPlugin.log(e);
//          fCustomSettings = DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
//              CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS).getMap();
//        }
//      }
//
//      final BulletListBlock bulletListBlock = new BulletListBlock(composite, SWT.NONE);
//      GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
//      layoutData.horizontalIndent = 15;
//      layoutData.grabExcessVerticalSpace = true;
//      bulletListBlock.setLayoutData(layoutData);
//
//      final Button configure = new Button(composite, SWT.NONE);
//      configure.setText(MultiFixMessages.CleanUpRefactoringWizard_ConfigureCustomProfile_button);
//
//      data = new GridData(SWT.TOP, SWT.LEAD, false, false);
//      data.widthHint = SWTUtil.getButtonWidthHint(configure);
//      configure.setLayoutData(data);
//
//      showCustomSettings(bulletListBlock);
//      configure.addSelectionListener(new SelectionAdapter() {
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public void widgetSelected(SelectionEvent e) {
//          Hashtable<String, String> workingValues = new Hashtable<String, String>(fCustomSettings);
//          CleanUpSelectionDialog dialog = new WizardCleanUpSelectionDialog(
//              getShell(),
//              workingValues);
//          if (dialog.open() == Window.OK) {
//            fCustomSettings = workingValues;
//            showCustomSettings(bulletListBlock);
//          }
//        }
//      });
//
//      updateEnableState(isCustom, settingsField, configure, bulletListBlock);
//
//      fUseCustomField.setDialogFieldListener(new IDialogFieldListener() {
//        @Override
//        public void dialogFieldChanged(DialogField field) {
//          updateEnableState(fUseCustomField.isSelected(), settingsField, configure, bulletListBlock);
//        }
//      });
//
//      Link preferencePageLink = new Link(composite, SWT.WRAP);
//      preferencePageLink.setText(MultiFixMessages.CleanUpRefactoringWizard_HideWizard_Link);
//      preferencePageLink.setFont(parent.getFont());
//      GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
//      gridData.widthHint = convertWidthInCharsToPixels(50);
//      gridData.horizontalSpan = 2;
//      preferencePageLink.setLayoutData(gridData);
//      preferencePageLink.addSelectionListener(new SelectionAdapter() {
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public void widgetSelected(SelectionEvent e) {
//          PreferencesUtil.createPreferenceDialogOn(
//              composite.getShell(),
//              CleanUpPreferencePage.PREF_ID,
//              null,
//              null).open();
//        }
//      });
//
//      setControl(composite);
//
//      Dialog.applyDialogFont(composite);
//    }
//
//    public Map<String, String> decodeSettings(String settings) throws CoreException {
//      byte[] bytes;
//      try {
//        bytes = settings.getBytes(ENCODING);
//      } catch (UnsupportedEncodingException e) {
//        bytes = settings.getBytes();
//      }
//      InputStream is = new ByteArrayInputStream(bytes);
//      try {
//        List<Profile> res = ProfileStore.readProfilesFromStream(new InputSource(is));
//        if (res == null || res.size() == 0) {
//          return DartToolsPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(
//              CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS).getMap();
//        }
//
//        CustomProfile profile = (CustomProfile) res.get(0);
//        new CleanUpProfileVersioner().update(profile);
//        return profile.getSettings();
//      } finally {
//        try {
//          is.close();
//        } catch (IOException e) { /* ignore */
//        }
//      }
//    }
//
//    public String encodeSettings(Map<String, String> settings) throws CoreException {
//      ByteArrayOutputStream stream = new ByteArrayOutputStream(2000);
//      try {
//        CleanUpProfileVersioner versioner = new CleanUpProfileVersioner();
//        CustomProfile profile = new ProfileManager.CustomProfile(
//            "custom", settings, versioner.getCurrentVersion(), versioner.getProfileKind()); //$NON-NLS-1$
//        ArrayList<Profile> profiles = new ArrayList<Profile>();
//        profiles.add(profile);
//        ProfileStore.writeProfilesToStream(profiles, stream, ENCODING, versioner);
//        try {
//          return stream.toString(ENCODING);
//        } catch (UnsupportedEncodingException e) {
//          return stream.toString();
//        }
//      } finally {
//        try {
//          stream.close();
//        } catch (IOException e) { /* ignore */
//        }
//      }
//    }
//
//    @Override
//    public IWizardPage getNextPage() {
//      initializeRefactoring();
//      storeSettings();
//      return super.getNextPage();
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    public void updateStatus(IStatus status) {
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    public void valuesModified() {
//    }
//
//    @Override
//    protected boolean performFinish() {
//      initializeRefactoring();
//      storeSettings();
//      return super.performFinish();
//    }
//
//    private void initializeRefactoring() {
//      CleanUpRefactoring refactoring = (CleanUpRefactoring) getRefactoring();
//
//      CleanUpOptions options = null;
//      if (fUseCustomField.isSelected()) {
//        refactoring.setUseOptionsFromProfile(false);
//        options = new MapCleanUpOptions(fCustomSettings);
//      } else {
//        refactoring.setUseOptionsFromProfile(true);
//      }
//
//      refactoring.clearCleanUps();
//      ICleanUp[] cleanups = DartToolsPlugin.getDefault().getCleanUpRegistry().createCleanUps();
//      for (int i = 0; i < cleanups.length; i++) {
//        if (options != null) {
//          cleanups[i].setOptions(options);
//        }
//        refactoring.addCleanUp(cleanups[i]);
//      }
//    }
//
//    private void showCustomSettings(BulletListBlock bulletListBlock) {
//      StringBuffer buf = new StringBuffer();
//
//      final ICleanUp[] cleanUps = DartToolsPlugin.getDefault().getCleanUpRegistry().createCleanUps();
//      CleanUpOptions options = new MapCleanUpOptions(fCustomSettings);
//      for (int i = 0; i < cleanUps.length; i++) {
//        cleanUps[i].setOptions(options);
//        String[] descriptions = cleanUps[i].getStepDescriptions();
//        if (descriptions != null) {
//          for (int j = 0; j < descriptions.length; j++) {
//            if (buf.length() > 0) {
//              buf.append('\n');
//            }
//            buf.append(descriptions[j]);
//          }
//        }
//      }
//      bulletListBlock.setText(buf.toString());
//    }
//
//    private void storeSettings() {
//      getDialogSettings().put(USE_CUSTOM_PROFILE_KEY, fUseCustomField.isSelected());
//      try {
//        getDialogSettings().put(CUSTOM_PROFILE_KEY, encodeSettings(fCustomSettings));
//      } catch (CoreException e) {
//        DartToolsPlugin.log(e);
//      }
//    }
//
//    private void updateEnableState(boolean isCustom,
//        final ListDialogField<DartProject> settingsField, Button configureCustom,
//        BulletListBlock bulletListBlock) {
//      settingsField.getListControl(null).setEnabled(!isCustom);
//      if (isCustom) {
//        fEnableState = ControlEnableState.disable(settingsField.getButtonBox(null));
//      } else if (fEnableState != null) {
//        fEnableState.restore();
//        fEnableState = null;
//      }
//      bulletListBlock.setEnabled(isCustom);
//      configureCustom.setEnabled(isCustom);
//    }
//  }
//  private static class ProjectProfileLableProvider extends LabelProvider implements
//      ITableLabelProvider {
//
//    private Hashtable<String, Profile> fProfileIdsTable;
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public Image getColumnImage(Object element, int columnIndex) {
//      return null;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public String getColumnText(Object element, int columnIndex) {
//      if (columnIndex == 0) {
//        return ((DartProject) element).getProject().getName();
//      } else if (columnIndex == 1) {
//
//        if (fProfileIdsTable == null) {
//          fProfileIdsTable = loadProfiles();
//        }
//
//        IEclipsePreferences instancePreferences = InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN);
//
//        final String workbenchProfileId;
//        if (instancePreferences.get(CleanUpConstants.CLEANUP_PROFILE, null) != null) {
//          workbenchProfileId = instancePreferences.get(CleanUpConstants.CLEANUP_PROFILE, null);
//        } else {
//          workbenchProfileId = CleanUpConstants.DEFAULT_PROFILE;
//        }
//
//        return getProjectProfileName((DartProject) element, fProfileIdsTable, workbenchProfileId);
//      }
//      return null;
//    }
//
//    public void reset() {
//      fProfileIdsTable = null;
//    }
//
//    private String getProjectProfileName(final DartProject project,
//        Hashtable<String, Profile> profileIdsTable, String workbenchProfileId) {
//      ProjectScope projectScope = new ProjectScope(project.getProject());
//      IEclipsePreferences node = projectScope.getNode(JavaUI.ID_PLUGIN);
//      String id = node.get(CleanUpConstants.CLEANUP_PROFILE, null);
//      if (id == null) {
//        Profile profile = profileIdsTable.get(workbenchProfileId);
//        if (profile != null) {
//          return profile.getName();
//        } else {
//          return MultiFixMessages.CleanUpRefactoringWizard_unknownProfile_Name;
//        }
//      } else {
//        Profile profile = profileIdsTable.get(id);
//        if (profile != null) {
//          return profile.getName();
//        } else {
//          return Messages.format(
//              MultiFixMessages.CleanUpRefactoringWizard_UnmanagedProfileWithName_Name,
//              id.substring(ProfileManager.ID_PREFIX.length()));
//        }
//      }
//    }
//
//    private Hashtable<String, Profile> loadProfiles() {
//      List<Profile> list = CleanUpPreferenceUtil.loadProfiles(InstanceScope.INSTANCE);
//      Hashtable<String, Profile> profileIdsTable = new Hashtable<String, Profile>();
//      for (Iterator<Profile> iterator = list.iterator(); iterator.hasNext();) {
//        Profile profile = iterator.next();
//        profileIdsTable.put(profile.getID(), profile);
//      }
//
//      return profileIdsTable;
//    }
//  }
  private static class CleanUpConfigurationPage extends UserInputWizardPage {
    private static final Map<String, ICleanUp> CLEAN_UPS = Maps.newHashMap();
    private static final CleanUpSettings settings = new CleanUpSettings();

    private static final String ID_MIGRATE_SYNTAX_1M1_CATCH = "migrateSyntax-1M1-catch";
    private static final String ID_MIGRATE_SYNTAX_1M1_OPERS = "migrateSyntax-1M1-operators";
    private static final String ID_MIGRATE_SYNTAX_1M1_GET = "migrateSyntax-1M1-get";
    private static final String ID_MIGRATE_SYNTAX_1M1_LIBRARY = "migrateSyntax-1M1-library";
    private static final String ID_STYLE_TRAILING_WHITESPACE = "style-trailingWhitespace";
    private static final String ID_STYLE_USE_BLOCKS = "style-useBlocks";
    private static final String ID_STYLE_USE_BLOCKS_FLAG = "style-useBlocks-flag";

    static {
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_CATCH, new Migrate_1M1_catch_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_OPERS, new Migrate_1M1_operators_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_GET, new Migrate_1M1_get_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_LIBRARY, new Migrate_1M1_library_CleanUp());
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_CATCH, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_OPERS, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_GET, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_LIBRARY, false);
      // style
      CLEAN_UPS.put(ID_STYLE_TRAILING_WHITESPACE, new Style_trailingSpace_CleanUp());
      CLEAN_UPS.put(ID_STYLE_USE_BLOCKS, new Style_useBlocks_CleanUp());
      settings.setDefault(ID_STYLE_TRAILING_WHITESPACE, true);
      settings.setDefault(ID_STYLE_USE_BLOCKS, true);
      settings.setDefault(ID_STYLE_USE_BLOCKS_FLAG, "ALWAYS");
    }

    private final CleanUpRefactoring refactoring;

    public CleanUpConfigurationPage(CleanUpRefactoring refactoring) {
      super(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_title);
      this.refactoring = refactoring;
      int cleanUpTargetsSize = refactoring.getCleanUpTargetsSize();
      DartProject[] projects = refactoring.getProjects();
      if (cleanUpTargetsSize == 1) {
        setMessage(MultiFixMessages.CleanUpRefactoringWizard_CleaningUp11_Title);
      } else if (projects.length == 1) {
        setMessage(Messages.format(
            MultiFixMessages.CleanUpRefactoringWizard_CleaningUpN1_Title,
            new Integer(cleanUpTargetsSize)));
      } else {
        setMessage(Messages.format(
            MultiFixMessages.CleanUpRefactoringWizard_CleaningUpNN_Title,
            new Object[] {new Integer(cleanUpTargetsSize), new Integer(projects.length)}));
      }
    }

    @Override
    public void createControl(Composite parent) {
      restoreSettings();
      initializeDialogUnits(parent);
      Composite composite = new Composite(parent, SWT.NONE);
      GridLayoutFactory.create(composite);
      {
        TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
        GridDataFactory.create(tabFolder).grab().fill();
        // Migrate Syntax
        {
          TabItem syntaxItem = new TabItem(tabFolder, SWT.NONE);
          syntaxItem.setText("Migrate Syntax");
          {
            Composite syntaxComposite = new Composite(tabFolder, SWT.NONE);
            syntaxItem.setControl(syntaxComposite);
            GridLayoutFactory.create(syntaxComposite);
            createCheckButton(
                syntaxComposite,
                ID_MIGRATE_SYNTAX_1M1_CATCH,
                "Migrate 'catch' blocks");
            createCheckButton(
                syntaxComposite,
                ID_MIGRATE_SYNTAX_1M1_OPERS,
                "Migrate 'operator equals()' and 'operator negate()'");
            createCheckButton(syntaxComposite, ID_MIGRATE_SYNTAX_1M1_GET, "Migrate getters");
            new Label(syntaxComposite, SWT.NONE);
            new Label(syntaxComposite, SWT.NONE).setText("Work in progress... not fully implemented:");
            createCheckButton(
                syntaxComposite,
                ID_MIGRATE_SYNTAX_1M1_LIBRARY,
                "Migrate library/import/source");
          }
        }
        // Code Style
        {
          TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
          tabItem.setText("Code style");
          {
            Composite tabComposite = new Composite(tabFolder, SWT.NONE);
            tabItem.setControl(tabComposite);
            GridLayoutFactory.create(tabComposite);
            createCheckButton(
                tabComposite,
                ID_STYLE_TRAILING_WHITESPACE,
                "Remove trailing whitespaces");
            // use blocks
            createCheckButton(
                tabComposite,
                ID_STYLE_USE_BLOCKS,
                "User blocks in if/while/for statements");
            {
              Composite blocksComposite = new Composite(tabComposite, SWT.NONE);
              GridDataFactory.create(blocksComposite).indentHorizontalChars(3);
              GridLayoutFactory.create(blocksComposite);
              createRadioButtons(blocksComposite, ID_STYLE_USE_BLOCKS_FLAG, new String[] {
                  "Always", "Only if necessary"}, new String[] {
                  Style_useBlocks_CleanUp.ALWAYS, Style_useBlocks_CleanUp.WHEN_NECESSARY});
            }
          }
        }
      }
      setControl(composite);
      Dialog.applyDialogFont(composite);
    }

    @Override
    public IWizardPage getNextPage() {
      initializeRefactoring();
      storeSettings();
      return super.getNextPage();
    }

    @Override
    protected boolean performFinish() {
      initializeRefactoring();
      storeSettings();
      return super.performFinish();
    }

    private Button createCheckButton(Composite syntaxComposite, final String key, String text) {
      final Button button = new Button(syntaxComposite, SWT.CHECK);
      button.setText(text);
      button.setSelection(settings.getBoolean(key));
      button.addListener(SWT.Selection, new Listener() {
        @Override
        public void handleEvent(Event event) {
          settings.set(key, button.getSelection());
        }
      });
      return button;
    }

    private void createRadioButtons(Composite syntaxComposite, final String key, String[] titles,
        String[] values) {
      String currentValue = settings.get(key);
      for (int i = 0; i < titles.length; i++) {
        String text = titles[i];
        final String value = values[i];
        Button button = new Button(syntaxComposite, SWT.RADIO);
        button.setText(text);
        button.setSelection(Objects.equal(value, currentValue));
        button.addListener(SWT.Selection, new Listener() {
          @Override
          public void handleEvent(Event event) {
            settings.set(key, value);
          }
        });
      }
    }

    private void initializeRefactoring() {
      refactoring.clearCleanUps();
      for (Entry<String, ICleanUp> entry : CLEAN_UPS.entrySet()) {
        String id = entry.getKey();
        if (settings.getBoolean(id)) {
          ICleanUp cleanUp = entry.getValue();
          refactoring.addCleanUp(cleanUp);
        }
      }
      ((Style_useBlocks_CleanUp) CLEAN_UPS.get(ID_STYLE_USE_BLOCKS)).setFlag(settings.get(ID_STYLE_USE_BLOCKS_FLAG));
    }

    private void restoreSettings() {
      String str = getDialogSettings().get(CUSTOM_PROFILE_KEY);
      settings.decode(str);
    }

    private void storeSettings() {
      getDialogSettings().put(CUSTOM_PROFILE_KEY, settings.encode());
    }

  }

  private static class CleanUpSettings {
    private Map<String, String> map;
    private Map<String, String> defaultMap = Maps.newHashMap();

    public void decode(String settings) {
      map = Maps.newHashMap();
      if (settings != null) {
        try {
          map = Base16.decodeToObject(settings);
        } catch (Throwable e) {
          DartToolsPlugin.log(e);
        }
      }
    }

    public String encode() {
      try {
        return Base16.encodeObject(map);
      } catch (Throwable e) {
        DartToolsPlugin.log(e);
        return null;
      }
    }

    public String get(String key) {
      String str = map.get(key);
      if (str == null) {
        str = defaultMap.get(key);
      }
      return str;
    }

    public boolean getBoolean(String key) {
      String str = get(key);
      return "TRUE".equals(str);
    }

    public void set(String key, boolean value) {
      map.put(key, value ? "TRUE" : "FALSE");
    }

    public void set(String key, String value) {
      map.put(key, value);
    }

    public void setDefault(String key, boolean value) {
      defaultMap.put(key, value ? "TRUE" : "FALSE");
    }

    public void setDefault(String key, String value) {
      defaultMap.put(key, value);
    }
  }

//
//  private static final String USE_CUSTOM_PROFILE_KEY = "org.eclipse.jdt.ui.cleanup.use_dialog_profile"; //$NON-NLS-1$
//
  private static final String CUSTOM_PROFILE_KEY = "org.eclipse.jdt.ui.cleanup.custom_profile"; //$NON-NLS-1$

  public CleanUpRefactoringWizard(CleanUpRefactoring refactoring, int flags) {
    super(refactoring, flags);
    setDefaultPageTitle(MultiFixMessages.CleanUpRefactoringWizard_PageTitle);
    setWindowTitle(MultiFixMessages.CleanUpRefactoringWizard_WindowTitle);
    setDefaultPageImageDescriptor(DartPluginImages.DESC_WIZBAN_CLEAN_UP);
  }

  @Override
  protected void addUserInputPages() {
    addPage(new CleanUpConfigurationPage((CleanUpRefactoring) getRefactoring()));
  }

}

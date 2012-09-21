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

import com.google.common.collect.Maps;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.utilities.io.Base16;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_catch_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_get_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_identical_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_library_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_optionalNamed_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_parseNum_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_rawString_CleanUp;
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

  private static class CleanUpConfigurationPage extends UserInputWizardPage {
    private static final Map<String, ICleanUp> CLEAN_UPS = Maps.newHashMap();
    private static final CleanUpSettings settings = new CleanUpSettings();

    private static final String ID_MIGRATE_SYNTAX_1M1_CATCH = "migrateSyntax-1M1-catch";
    private static final String ID_MIGRATE_SYNTAX_1M1_GET = "migrateSyntax-1M1-get";
    private static final String ID_MIGRATE_SYNTAX_1M1_LIBRARY = "migrateSyntax-1M1-library";
    private static final String ID_MIGRATE_SYNTAX_1M1_OPTIONAL_NAMED = "migrateSyntax-1M1-optionalNamed-whereSure";
    private static final String ID_MIGRATE_SYNTAX_1M1_RAW_STRING = "migrateSyntax-1M1-rawString";
    private static final String ID_MIGRATE_SYNTAX_1M1_PARSE_NUM = "migrateSyntax-1M1-parseNum";
    private static final String ID_MIGRATE_SYNTAX_1M1_IDENTICAL = "migrateSyntax-1M1-identical";

    private static final String ID_STYLE_TRAILING_WHITESPACE = "style-trailingWhitespace";
    private static final String ID_STYLE_USE_BLOCKS = "style-useBlocks";
    private static final String ID_STYLE_USE_BLOCKS_FLAG = "style-useBlocks-flag";

    static {
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_CATCH, new Migrate_1M1_catch_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_GET, new Migrate_1M1_get_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_RAW_STRING, new Migrate_1M1_rawString_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_PARSE_NUM, new Migrate_1M1_parseNum_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_IDENTICAL, new Migrate_1M1_identical_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_LIBRARY, new Migrate_1M1_library_CleanUp());
      CLEAN_UPS.put(ID_MIGRATE_SYNTAX_1M1_OPTIONAL_NAMED, new Migrate_1M1_optionalNamed_CleanUp());
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_CATCH, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_GET, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_RAW_STRING, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_PARSE_NUM, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_IDENTICAL, true);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_LIBRARY, false);
      settings.setDefault(ID_MIGRATE_SYNTAX_1M1_OPTIONAL_NAMED, false);
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
            createCheckButton(syntaxComposite, ID_MIGRATE_SYNTAX_1M1_GET, "Migrate getters");
            createCheckButton(
                syntaxComposite,
                ID_MIGRATE_SYNTAX_1M1_RAW_STRING,
                "Migrate @'rawString' to r'rawString'");
            createCheckButton(
                syntaxComposite,
                ID_MIGRATE_SYNTAX_1M1_PARSE_NUM,
                "Migrate parseInt() and parseDouble() to int.parse() and double.parse()");
            createCheckButton(
                syntaxComposite,
                ID_MIGRATE_SYNTAX_1M1_IDENTICAL,
                "Replace === with == or identical(x,y). Replace !== with != or !identical(x,y).");
            createCheckButton(
                syntaxComposite,
                ID_MIGRATE_SYNTAX_1M1_OPTIONAL_NAMED,
                "Migrate [param = value] to {param: value} when only named arguments are used");
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
            createCheckButton(
                tabComposite,
                ID_STYLE_USE_BLOCKS,
                "Use blocks in if/while/for statements");
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

//    private void createRadioButtons(Composite syntaxComposite, final String key, String[] titles,
//        String[] values) {
//      String currentValue = settings.get(key);
//      for (int i = 0; i < titles.length; i++) {
//        String text = titles[i];
//        final String value = values[i];
//        Button button = new Button(syntaxComposite, SWT.RADIO);
//        button.setText(text);
//        button.setSelection(Objects.equal(value, currentValue));
//        button.addListener(SWT.Selection, new Listener() {
//          @Override
//          public void handleEvent(Event event) {
//            settings.set(key, value);
//          }
//        });
//      }
//    }

    private void initializeRefactoring() {
      refactoring.clearCleanUps();
      for (Entry<String, ICleanUp> entry : CLEAN_UPS.entrySet()) {
        String id = entry.getKey();
        if (settings.getBoolean(id)) {
          ICleanUp cleanUp = entry.getValue();
          refactoring.addCleanUp(cleanUp);
        }
      }
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

    public void setDefault(String key, boolean value) {
      defaultMap.put(key, value ? "TRUE" : "FALSE");
    }

    public void setDefault(String key, String value) {
      defaultMap.put(key, value);
    }
  }

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

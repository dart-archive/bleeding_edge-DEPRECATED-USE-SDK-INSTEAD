/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.preferences;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.internal.themes.ThemeElementHelper;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Page for setting general Dart plug-in preferences (the root of all Dart preferences).
 */
@SuppressWarnings("restriction")
public class DartBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String JAVA_BASE_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.DartBasePreferencePage"; //$NON-NLS-1$

  public static final String EDITOR_FONT_KEY = JFaceResources.TEXT_FONT; //"org.eclipse.jface.textfont";
  //private static final String EDITOR_FONT_KEY = "com.google.dart.tools.ui.editors.textfont";

  private static final int SZ_SMALL = 9;
  private static final int SZ_MEDIUM = 11;
  private static final int SZ_LARGE = 14;
  private static final int SZ_XL = 18;

  private Button lineNumbersCheck;

  private Button printMarginCheck;
  private Text printMarginText;
  private Button smallFontsButton;
  private Button mediumFontsButton;
  private Button largeFontsButton;
  private Button xlFontsButton;

  private FontData[] fontData;

  public DartBasePreferencePage() {
    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());

    setDescription(PreferencesMessages.DartBasePreferencePage_editor_preferences);

    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench workbench) {
    // do nothing
  }

  @Override
  public boolean performOk() {
    IPreferenceStore editorPreferences = EditorsPlugin.getDefault().getPreferenceStore();

    editorPreferences.setValue(
        AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER,
        lineNumbersCheck.getSelection());

    editorPreferences.setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN,
        printMarginCheck.getSelection());

    if (printMarginCheck.getSelection()) {
      editorPreferences.setValue(
          AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN,
          printMarginText.getText());
    }

    IPreferenceStore workbenchPrefStore = WorkbenchPlugin.getDefault().getPreferenceStore();

    IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
    ITheme theme = themeManager.getCurrentTheme();
    FontRegistry registry = theme.getFontRegistry();
    registry.put(EDITOR_FONT_KEY, fontData);

    String key = ThemeElementHelper.createPreferenceKey(theme, EDITOR_FONT_KEY);
    String fdString = PreferenceConverter.getStoredRepresentation(fontData);
    String storeString = workbenchPrefStore.getString(key);

    if (!fdString.equals(storeString)) {
      workbenchPrefStore.setValue(key, fdString);
    }
    /*
     * The following block of code adjusts fonts in dialogs. There are some issues, like much of the
     * non-framework section of the page not getting changed. For new, it is commented out.
     */
//    int height = fontData[0].getHeight();
//    scaleFontNamed(JFaceResources.HEADER_FONT, height + 3);
//    scaleFontNamed(JFaceResources.BANNER_FONT, height + 1);
//    scaleFontNamed(JFaceResources.DIALOG_FONT, height);
//    scaleFontNamed(JFaceResources.DEFAULT_FONT, height);

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    Group generalGroup = new Group(composite, SWT.NONE);
    generalGroup.setText(PreferencesMessages.DartBasePreferencePage_general);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        generalGroup);
    GridLayoutFactory.fillDefaults().numColumns(3).margins(8, 8).applyTo(generalGroup);

    // line numbers
    lineNumbersCheck = createCheckBox(generalGroup,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers_tooltip);
    GridDataFactory.fillDefaults().span(3, 1).applyTo(lineNumbersCheck);

    // print margin
    printMarginCheck = createCheckBox(generalGroup,
        PreferencesMessages.DartBasePreferencePage_show_print_margin,
        PreferencesMessages.DartBasePreferencePage_show_print_margin_tooltip);
    printMarginCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        printMarginText.setEnabled(printMarginCheck.getSelection());
      }
    });

    printMarginText = new Text(generalGroup, SWT.BORDER | SWT.SINGLE | SWT.RIGHT);
    printMarginText.setTextLimit(5);
    GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(printMarginText);

    Group fontGroup = new Group(composite, SWT.NONE);
    fontGroup.setText(PreferencesMessages.DartBasePreferencePage_font_group_label);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        fontGroup);
    GridLayoutFactory.fillDefaults().numColumns(5).margins(8, 8).applyTo(fontGroup);

    // font scaling
    Label fontLabel = new Label(fontGroup, SWT.NONE);
    fontLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, false, false));
    fontLabel.setText(PreferencesMessages.DartBasePreferencePage_font_scale_label);
    smallFontsButton = new Button(fontGroup, SWT.TOGGLE | SWT.FLAT | SWT.CENTER);
    smallFontsButton.setText(PreferencesMessages.DartBasePreferencePage_font_scale_indicator);
    smallFontsButton.setFont(getSmallFont());
    smallFontsButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, false, false));
    smallFontsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateFont(smallFontsButton.getFont());
      }
    });

    mediumFontsButton = new Button(fontGroup, SWT.TOGGLE | SWT.FLAT | SWT.CENTER);
    mediumFontsButton.setText(PreferencesMessages.DartBasePreferencePage_font_scale_indicator);
    mediumFontsButton.setFont(getMediumFont());
    mediumFontsButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, false, false));
    mediumFontsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateFont(mediumFontsButton.getFont());
      }
    });

    largeFontsButton = new Button(fontGroup, SWT.TOGGLE | SWT.FLAT | SWT.CENTER);
    largeFontsButton.setText(PreferencesMessages.DartBasePreferencePage_font_scale_indicator);
    largeFontsButton.setFont(getLargeFont());
    largeFontsButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, false, false));
    largeFontsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateFont(largeFontsButton.getFont());
      }
    });

    xlFontsButton = new Button(fontGroup, SWT.TOGGLE | SWT.FLAT | SWT.CENTER);
    xlFontsButton.setText(PreferencesMessages.DartBasePreferencePage_font_scale_indicator);
    xlFontsButton.setFont(getXlFont());
    xlFontsButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, false, false));
    xlFontsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateFont(xlFontsButton.getFont());
      }
    });

    initFromPrefs();

    return composite;
  }

  boolean isFontOfSize(Font font, int size) {
    return font.getFontData()[0].getHeight() == size;
  }

  private Button createCheckBox(Composite composite, String label, String tooltip) {
    final Button checkBox = new Button(composite, SWT.CHECK);

    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);

    return checkBox;
  }

  private Font getFont(int size) {
    FontData oldData = getFontData()[0];
    FontData data = new FontData(oldData.getName(), oldData.getHeight(), oldData.getStyle());
    data.height = size;
    Font font = smallFontsButton.getFont();
    font = new Font(font.getDevice(), data);
    return font;
  }

  private FontData[] getFontData() {
    if (fontData == null) {
      IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
      ITheme theme = themeManager.getCurrentTheme();
      fontData = theme.getFontRegistry().getFontData(EDITOR_FONT_KEY);
    }
    return fontData;
  }

  private Font getLargeFont() {
    return getFont(SZ_LARGE);
  }

  private Font getMediumFont() {
    return getFont(SZ_MEDIUM);
  }

  private Font getSmallFont() {
    return getFont(SZ_SMALL);
  }

  private Font getXlFont() {
    return getFont(SZ_XL);
  }

  private void initFromPrefs() {
    IPreferenceStore editorPreferences = EditorsPlugin.getDefault().getPreferenceStore();

    lineNumbersCheck.setSelection(editorPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER));

    printMarginCheck.setSelection(editorPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN));
    printMarginText.setText(editorPreferences.getString(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN));

    printMarginText.setEnabled(printMarginCheck.getSelection());

    getFontData();
    selectFontButton();
  }

  private void scaleFontNamed(String name, int size) {
    IPreferenceStore workbenchPrefStore = WorkbenchPlugin.getDefault().getPreferenceStore();
    IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
    ITheme theme = themeManager.getCurrentTheme();
    FontRegistry registry = theme.getFontRegistry();
    FontData[] fds = registry.getFontData(name);
    FontData data = new FontData(fds[0].getName(), size, fds[0].getStyle());
    registry.put(name, new FontData[] {data});
    String key = ThemeElementHelper.createPreferenceKey(theme, name);
    String fdString = PreferenceConverter.getStoredRepresentation(new FontData[] {data});
    String storeString = workbenchPrefStore.getString(key);
    if (!fdString.equals(storeString)) {
      workbenchPrefStore.setValue(key, fdString);
    }
  }

  private void selectFontButton() {
    smallFontsButton.setSelection(false);
    mediumFontsButton.setSelection(false);
    largeFontsButton.setSelection(false);
    xlFontsButton.setSelection(false);
    FontData data = getFontData()[0];
    int size = data.getHeight();
    if (isFontOfSize(smallFontsButton.getFont(), size)) {
      smallFontsButton.setSelection(true);
    } else if (isFontOfSize(mediumFontsButton.getFont(), size)) {
      mediumFontsButton.setSelection(true);
    } else if (isFontOfSize(largeFontsButton.getFont(), size)) {
      largeFontsButton.setSelection(true);
    } else if (isFontOfSize(xlFontsButton.getFont(), size)) {
      xlFontsButton.setSelection(true);
    }
  }

  private void updateFont(Font font) {
    fontData = font.getFontData();
    selectFontButton();
  }

}

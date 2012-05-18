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

package com.google.dart.tools.ui.internal.preferences;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.editor.DartDocumentSetupParticipant;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.themes.ThemeElementHelper;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Preference page for fonts in the editor
 */
public class FontPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private static final String SAMPLE_CODE = "  void run() {\n    write('Hello, world!');\n  }";

  public static final String
      FONT_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.FontPreferencePage"; //$NON-NLS-1$

//public static final String EDITOR_FONT_KEY = JFaceResources.TEXT_FONT; //"org.eclipse.jface.textfont";
  public static final String EDITOR_FONT_KEY = PreferenceConstants.EDITOR_TEXT_FONT;
  public static final String
      EDITOR_DEFAULT_FONT_KEY = "com.google.dart.tools.ui.editors.textfont.default";
  public static final String BASE_FONT_KEY = "com.google.dart.tools.ui.editors.basefont";
  public static final String
      BASE_DEFAULT_FONT_KEY = "com.google.dart.tools.ui.editors.basefont.default";

  private static final int SZ_SMALL = 10;
  private static final int SZ_MEDIUM = 11;
  private static final int SZ_LARGE = 14;
  private static final int SZ_XL = 18;

// Available font sizes
  private static final int[] FONT_SIZES = {
      8, 9, SZ_SMALL, SZ_MEDIUM, SZ_LARGE, SZ_XL, 24, 36, 48, 64, 72, 96, 144, 288};

  public static Map<String, FontData> getAllFontsByName() {
    // this returns nearly 150 named fonts, so is unsuitable for use directly in the UI
    FontData[] allFonts = Display.getCurrent().getFontList(null, true);
    Map<String, FontData> fonts = new HashMap<String, FontData>();
    for (FontData data : allFonts) {
      String name = data.getName();
      if (fonts.get(name) == null) {
        fonts.put(name, data);
      }
    }
    return fonts;
  }

  private Button smallFontsButton;
  private Button mediumFontsButton;
  private Button largeFontsButton;
  private Button xlFontsButton;
  private SourceViewer previewViewer;
  private Button selectFontButton;
  private Label codeFontLabel;
  private Button resetButton;

  private SourceViewerConfiguration sourceViewerConfiguration;
  private FontData[] fontData;
  private FontData[] baseData;

  public FontPreferencePage() {
    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());
    //   setDescription(PreferencesMessages.DartBasePreferencePage_text_font);
    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench arg0) {

  }

  @Override
  public boolean performOk() {

    persistFont(EDITOR_FONT_KEY, fontData);
    persistFont(BASE_FONT_KEY, baseData);
    /*
     * The following block of code adjusts fonts in dialogs. There are some issues, like much of the
     * non-framework section of the page not getting changed. For now, it is commented out.
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

    GridDataFactory.fillDefaults()
        .grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    Group fontGroup = new Group(composite, SWT.NONE);
    //   fontGroup.setText(PreferencesMessages.DartBasePreferencePage_font_group_label);
    GridDataFactory.fillDefaults()
        .grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(fontGroup);
    GridLayoutFactory.fillDefaults().numColumns(5).margins(8, 8).applyTo(fontGroup);

    // font scaling
    Label fontLabel = new Label(fontGroup, SWT.NONE);
    fontLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, false, false));
    fontLabel.setText(PreferencesMessages.DartBasePreferencePage_font_scale_label);
    smallFontsButton = new Button(fontGroup, SWT.TOGGLE | SWT.FLAT | SWT.CENTER);
    smallFontsButton.setFont(parent.getFont()); // bootstrapping
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

    Composite sep = new Composite(fontGroup, SWT.NONE);
    GridDataFactory.fillDefaults()
        .span(5, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(sep);
    GridLayoutFactory.fillDefaults().margins(50, 10).applyTo(sep);
    Label sepl = new Label(sep, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(sepl);

    // code font
    createPreviewer(fontGroup);

    Composite buttons = new Composite(fontGroup, SWT.NONE);
    GridDataFactory.fillDefaults().span(4, 1).align(SWT.BEGINNING, SWT.BEGINNING).applyTo(buttons);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(buttons);
    new Label(buttons, SWT.NONE);
    codeFontLabel = new Label(buttons, SWT.NONE);

    selectFontButton = new Button(buttons, SWT.PUSH);
    GridDataFactory.fillDefaults().grab(false, true).applyTo(selectFontButton);
    selectFontButton.setText(PreferencesMessages.DartBasePreferencePage_code_font_select_label);
    selectFontButton.addSelectionListener(new SelectionAdapter() {
        @Override
      public void widgetSelected(SelectionEvent e) {
        chooseCodeFont();
      }
    });

    resetButton = new Button(buttons, SWT.PUSH);
    GridDataFactory.fillDefaults().grab(false, true).applyTo(resetButton);
    resetButton.setText(PreferencesMessages.DartBasePreferencePage_reset_button_label);
    resetButton.addSelectionListener(new SelectionAdapter() {
        @Override
      public void widgetSelected(SelectionEvent e) {
        resetFonts();
      }
    });

    initFromPrefs();

    return composite;
  }

  boolean isFontOfSize(Font font, int size) {
    return font.getFontData()[0].getHeight() == size;
  }

  private void chooseCodeFont() {
    final FontDialog fontDialog = new FontDialog(getShell());
    fontDialog.setFontList(getFontData());
    final FontData data = fontDialog.open();
    if (data != null) {
      fontData = new FontData[] {data};
      updatePreviewFont(SWTUtil.getFont(getMediumFont().getDevice(), fontData));
    }
  }

  private void createPreviewer(Composite parent) {
    Composite previewComp = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = layout.marginWidth = 0;
    previewComp.setLayout(layout);
    previewComp.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label label = new Label(previewComp, SWT.NONE);
    label.setText("Code Editor");
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    previewViewer = new SourceViewer(previewComp, null, SWT.BORDER /* | SWT.V_SCROLL | SWT.H_SCROLL */);
    sourceViewerConfiguration = getSourceViewerConfiguration();

    if (sourceViewerConfiguration != null) {
      previewViewer.configure(sourceViewerConfiguration);
    }

    previewViewer.setEditable(false);
    previewViewer.setDocument(getDocument());

    Control control = previewViewer.getControl();
    GridData controlData = new GridData(GridData.FILL_BOTH);
    controlData.heightHint = 100;
    control.setLayoutData(controlData);
  }

  private int findFontIndex(int size) {
    // exact match?
    for (int i = 0; i < FONT_SIZES.length; i++) {
      if (FONT_SIZES[i] == size) {
        return i;
      }
    }
    // next largest?
    for (int i = 0; i < FONT_SIZES.length; i++) {
      if (FONT_SIZES[i] > size) {
        return i;
      }
    }
    // first
    return 0;
  }

  private int findScaledCodeFontSize(
      int oldCodeFontSize, int oldBaseFontSize, int newBaseFontSize) {
    int oldCodeIndex = findFontIndex(oldCodeFontSize);
    int oldBaseIndex = findFontIndex(oldBaseFontSize);
    int newBaseIndex = findFontIndex(newBaseFontSize);
    int delta = newBaseIndex - oldBaseIndex;
    int newCodeIndex = oldCodeIndex + delta;
    newCodeIndex = Math.max(Math.min(newCodeIndex, FONT_SIZES.length - 1), 0);
    return FONT_SIZES[newCodeIndex];
  }

  private FontData[] getBaseData() {
    if (baseData == null) {
      IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
      ITheme theme = themeManager.getCurrentTheme();
      baseData = theme.getFontRegistry().getFontData(BASE_FONT_KEY);
    }
    return baseData;
  }

  private IDocument getDocument() {
    IDocument document = new Document(SAMPLE_CODE);
    new DartDocumentSetupParticipant().setup(document);
    return document;
  }

  private Font getFont(int size) {
    FontData oldData = getBaseData()[0];
    FontData data = new FontData(oldData.getName(), oldData.getHeight(), oldData.getStyle());
    data.height = size;
    Font font = smallFontsButton.getFont(); // bootstrapped
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

  private SourceViewerConfiguration getSourceViewerConfiguration() {
    DartTextTools textTools = DartToolsPlugin.getDefault().getJavaTextTools();
    return new DartSourceViewerConfiguration(
        textTools.getColorManager(), getPreferenceStore(), null, DartPartitions.DART_PARTITIONING);
  }

  private Font getXlFont() {
    return getFont(SZ_XL);
  }

  private void initFromPrefs() {

    getFontData();
    getBaseData();
    updatePreviewFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
    selectFontButton();
    setCodeFontLabel();
  }

  private void persistFont(String fontKey, FontData[] fontData) {
    IPreferenceStore workbenchPrefStore = WorkbenchPlugin.getDefault().getPreferenceStore();

    IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
    ITheme theme = themeManager.getCurrentTheme();
    FontRegistry registry = theme.getFontRegistry();
    registry.put(fontKey, fontData);

    String key = ThemeElementHelper.createPreferenceKey(theme, fontKey);
    String fdString = PreferenceConverter.getStoredRepresentation(fontData);
    String storeString = workbenchPrefStore.getString(key);

    if (!fdString.equals(storeString)) {
      workbenchPrefStore.setValue(key, fdString);
    }
  }

  private void resetFonts() {
    IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
    ITheme theme = themeManager.getCurrentTheme();
    fontData = theme.getFontRegistry().getFontData(EDITOR_DEFAULT_FONT_KEY);
    baseData = theme.getFontRegistry().getFontData(BASE_DEFAULT_FONT_KEY);
    selectFontButton();
    updatePreviewFont(SWTUtil.getFont(getMediumFont().getDevice(), fontData));
  }

  @SuppressWarnings("unused")
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
    FontData data = getBaseData()[0];
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

  private void setCodeFontLabel() {
    StringBuilder label = new StringBuilder();
    FontData data = getFontData()[0];
    label.append(data.getName());
    label.append(" ");
    label.append(data.getHeight());
    codeFontLabel.setText(label.toString());
    codeFontLabel.getParent().getParent().layout(true, true);
  }

  private void updateFont(Font font) {
    int oldBaseFontSize = baseData[0].getHeight();
    int oldCodeFontSize = fontData[0].getHeight();
    FontData[] data = baseData = font.getFontData();
    int newBaseFontSize = data[0].getHeight();
    int newCodeFontSize = findScaledCodeFontSize(oldCodeFontSize, oldBaseFontSize, newBaseFontSize);
    baseData = data;
    fontData = SWTUtil.changeFontSize(fontData, newCodeFontSize);
    Font newFont = SWTUtil.getFont(font.getDevice(), fontData);
    selectFontButton();
    updatePreviewFont(newFont);
  }

  private void updatePreviewFont(Font font) {
    setCodeFontLabel();
    previewViewer.getTextWidget().setFont(font);
  }

}

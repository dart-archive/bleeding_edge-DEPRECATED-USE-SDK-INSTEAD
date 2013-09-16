/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;
import org.eclipse.wst.sse.ui.internal.preferences.TabFolderLayout;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy;
import org.eclipse.wst.sse.ui.internal.provisional.preferences.CommonEditorPreferenceNames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Gutted version of JavaEditorPreferencePage
 * 
 * @author pavery
 */
public class StructuredTextEditorPreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage {
  private ColorEditor fAppearanceColorEditor;
  private List fAppearanceColorList;

  private final String[][] fAppearanceColorListModel = new String[][] {
      {
          SSEUIMessages.StructuredTextEditorPreferencePage_2,
          EditorPreferenceNames.MATCHING_BRACKETS_COLOR},
      {
          SSEUIMessages.StructuredTextEditorPreferencePage_41,
          EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND},
      {
          SSEUIMessages.StructuredTextEditorPreferencePage_42,
          EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND},
      {
          SSEUIMessages.StructuredTextEditorPreferencePage_43,
          EditorPreferenceNames.CODEASSIST_PARAMETERS_BACKGROUND},
      {
          SSEUIMessages.StructuredTextEditorPreferencePage_44,
          EditorPreferenceNames.CODEASSIST_PARAMETERS_FOREGROUND}}; //$NON-NLS-1$
  private Map fCheckBoxes = new HashMap();
  private SelectionListener fCheckBoxListener = new SelectionListener() {
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
      Button button = (Button) e.widget;
      fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
    }
  };

  private Map fColorButtons = new HashMap();

  private ArrayList fNumberFields = new ArrayList();
  private OverlayPreferenceStore fOverlayStore;
  /** Button controlling default setting of the selected reference provider. */
  // TODO: private field never read locally
  Button fSetDefaultButton;
  private IPreferenceTab[] fTabs = null;
  private Map fTextFields = new HashMap();

  public StructuredTextEditorPreferencePage() {
    setDescription(SSEUIMessages.StructuredTextEditorPreferencePage_6); //$NON-NLS-1$
    setPreferenceStore(SSEUIPlugin.getDefault().getPreferenceStore());

    fOverlayStore = new OverlayPreferenceStore(getPreferenceStore(), createOverlayStoreKeys());
  }

  private Button addCheckBox(Composite parent, String label, String key, int indentation) {
    Button checkBox = new Button(parent, SWT.CHECK);
    checkBox.setText(label);

    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.horizontalIndent = indentation;
    gd.horizontalSpan = 2;
    checkBox.setLayoutData(gd);
    checkBox.addSelectionListener(fCheckBoxListener);

    fCheckBoxes.put(checkBox, key);

    return checkBox;
  }

  /**
   * Applies the status to the status line of a dialog page.
   */
  public void applyToStatusLine(DialogPage page, IStatus status) {
    String message = status.getMessage();
    switch (status.getSeverity()) {
      case IStatus.OK:
        page.setMessage(message, IMessageProvider.NONE);
        page.setErrorMessage(null);
        break;
      case IStatus.WARNING:
        page.setMessage(message, IMessageProvider.WARNING);
        page.setErrorMessage(null);
        break;
      case IStatus.INFO:
        page.setMessage(message, IMessageProvider.INFORMATION);
        page.setErrorMessage(null);
        break;
      default:
        if (message.length() == 0) {
          message = null;
        }
        page.setMessage(null);
        page.setErrorMessage(message);
        break;
    }
  }

  private Control createAppearancePage(Composite parent) {
    Composite appearanceComposite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    appearanceComposite.setLayout(layout);

    String label = SSEUIMessages.StructuredTextEditorPreferencePage_20; //$NON-NLS-1$
    addCheckBox(appearanceComposite, label, EditorPreferenceNames.MATCHING_BRACKETS, 0);

    label = SSEUIMessages.StructuredTextEditorPreferencePage_30; //$NON-NLS-1$
    addCheckBox(appearanceComposite, label,
        CommonEditorPreferenceNames.EVALUATE_TEMPORARY_PROBLEMS, 0);

    PreferenceLinkArea contentTypeArea = new PreferenceLinkArea(
        appearanceComposite,
        SWT.NONE,
        "ValidationPreferencePage", SSEUIMessages.StructuredTextEditorPreferencePage_40, (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$

    GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
    data.horizontalIndent = 20;
    contentTypeArea.getControl().setLayoutData(data);

    label = SSEUIMessages.StructuredTextEditorPreferencePage_39;
    addCheckBox(appearanceComposite, label, EditorPreferenceNames.SHOW_UNKNOWN_CONTENT_TYPE_MSG, 0);

    label = SSEUIMessages.StructuredTextEditorPreferencePage_3;
    addCheckBox(appearanceComposite, label, AbstractStructuredFoldingStrategy.FOLDING_ENABLED, 0);

    label = SSEUIMessages.StructuredTextEditorPreferencePage_1;
    addCheckBox(appearanceComposite, label, EditorPreferenceNames.SEMANTIC_HIGHLIGHTING, 0);

    Label l = new Label(appearanceComposite, SWT.LEFT);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    gd.heightHint = convertHeightInCharsToPixels(1) / 2;
    l.setLayoutData(gd);

    l = new Label(appearanceComposite, SWT.LEFT);
    l.setText(SSEUIMessages.StructuredTextEditorPreferencePage_23); //$NON-NLS-1$
    gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    l.setLayoutData(gd);

    Composite editorComposite = new Composite(appearanceComposite, SWT.NONE);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    editorComposite.setLayout(layout);
    gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
    gd.horizontalSpan = 2;
    editorComposite.setLayoutData(gd);

    fAppearanceColorList = new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
    gd.heightHint = convertHeightInCharsToPixels(7);
    fAppearanceColorList.setLayoutData(gd);

    Composite stylesComposite = new Composite(editorComposite, SWT.NONE);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    stylesComposite.setLayout(layout);
    stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

    l = new Label(stylesComposite, SWT.LEFT);
    // needs to be made final so label can be set in
    // foregroundcolorbutton's acc listener
    final String buttonLabel = SSEUIMessages.StructuredTextEditorPreferencePage_24; //$NON-NLS-1$ 
    l.setText(buttonLabel);
    gd = new GridData();
    gd.horizontalAlignment = GridData.BEGINNING;
    l.setLayoutData(gd);

    fAppearanceColorEditor = new ColorEditor(stylesComposite);
    Button foregroundColorButton = fAppearanceColorEditor.getButton();
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalAlignment = GridData.BEGINNING;
    foregroundColorButton.setLayoutData(gd);

    fAppearanceColorList.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        // do nothing
      }

      public void widgetSelected(SelectionEvent e) {
        handleAppearanceColorListSelection();
      }
    });
    foregroundColorButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        // do nothing
      }

      public void widgetSelected(SelectionEvent e) {
        int i = fAppearanceColorList.getSelectionIndex();
        String key = fAppearanceColorListModel[i][1];

        PreferenceConverter.setValue(fOverlayStore, key, fAppearanceColorEditor.getColorValue());
      }
    });

    // bug2541 - associate color label to button's label field
    foregroundColorButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
      public void getName(AccessibleEvent e) {
        if (e.childID == ACC.CHILDID_SELF)
          e.result = buttonLabel;
      }
    });

    PlatformUI.getWorkbench().getHelpSystem().setHelp(appearanceComposite,
        IHelpContextIds.PREFSTE_APPEARANCE_HELPID);
    return appearanceComposite;
  }

  /*
   * @see PreferencePage#createContents(Composite)
   */
  protected Control createContents(Composite parent) {
    // need to create tabs before loading/starting overlaystore in case
    // tabs also add values
    IPreferenceTab hoversTab = new TextHoverPreferenceTab(this, fOverlayStore);

    fOverlayStore.load();
    fOverlayStore.start();

    TabFolder folder = new TabFolder(parent, SWT.NONE);
    folder.setLayout(new TabFolderLayout());
    folder.setLayoutData(new GridData(GridData.FILL_BOTH));

    TabItem item = new TabItem(folder, SWT.NONE);
    item.setText(SSEUIMessages.StructuredTextEditorPreferencePage_0); //$NON-NLS-1$
    item.setControl(createAppearancePage(folder));

    item = new TabItem(folder, SWT.NONE);
    item.setText(hoversTab.getTitle());
    item.setControl(hoversTab.createContents(folder));

    fTabs = new IPreferenceTab[] {hoversTab};

    initialize();

    Dialog.applyDialogFont(folder);
    return folder;
  }

  /*
   * @see PreferencePage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    super.createControl(parent);
    // WorkbenchHelp.setHelp(getControl(),
    // IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE);
  }

  private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
    ArrayList overlayKeys = new ArrayList();

    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
        EditorPreferenceNames.MATCHING_BRACKETS_COLOR));
    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        EditorPreferenceNames.MATCHING_BRACKETS));

    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        CommonEditorPreferenceNames.EVALUATE_TEMPORARY_PROBLEMS));
    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        EditorPreferenceNames.SHOW_UNKNOWN_CONTENT_TYPE_MSG));

    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        AbstractStructuredFoldingStrategy.FOLDING_ENABLED));

    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        EditorPreferenceNames.SEMANTIC_HIGHLIGHTING));

    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
        EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND));
    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
        EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND));
    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
        EditorPreferenceNames.CODEASSIST_PARAMETERS_BACKGROUND));
    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
        EditorPreferenceNames.CODEASSIST_PARAMETERS_FOREGROUND));

    OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
    overlayKeys.toArray(keys);
    return keys;
  }

  /*
   * @see DialogPage#dispose()
   */
  public void dispose() {
    if (fOverlayStore != null) {
      fOverlayStore.stop();
      fOverlayStore = null;
    }

    super.dispose();
  }

  private void handleAppearanceColorListSelection() {
    int i = fAppearanceColorList.getSelectionIndex();
    String key = fAppearanceColorListModel[i][1];
    RGB rgb = PreferenceConverter.getColor(fOverlayStore, key);
    fAppearanceColorEditor.setColorValue(rgb);
  }

  /*
   * @see IWorkbenchPreferencePage#init()
   */
  public void init(IWorkbench workbench) {
    // nothing to do
  }

  private void initialize() {
    initializeFields();

    for (int i = 0; i < fAppearanceColorListModel.length; i++)
      fAppearanceColorList.add(fAppearanceColorListModel[i][0]);
    fAppearanceColorList.getDisplay().asyncExec(new Runnable() {
      public void run() {
        if (fAppearanceColorList != null && !fAppearanceColorList.isDisposed()) {
          fAppearanceColorList.select(0);
          handleAppearanceColorListSelection();
        }
      }
    });
  }

  private void initializeFields() {
    Iterator e = fColorButtons.keySet().iterator();
    while (e.hasNext()) {
      ColorEditor c = (ColorEditor) e.next();
      String key = (String) fColorButtons.get(c);
      RGB rgb = PreferenceConverter.getColor(fOverlayStore, key);
      c.setColorValue(rgb);
    }

    e = fCheckBoxes.keySet().iterator();
    while (e.hasNext()) {
      Button b = (Button) e.next();
      String key = (String) fCheckBoxes.get(b);
      b.setSelection(fOverlayStore.getBoolean(key));
    }

    e = fTextFields.keySet().iterator();
    while (e.hasNext()) {
      Text t = (Text) e.next();
      String key = (String) fTextFields.get(t);
      t.setText(fOverlayStore.getString(key));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#performApply()
   */
  protected void performApply() {
    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performApply();
    }
    super.performApply();
  }

  /*
   * @see PreferencePage#performDefaults()
   */
  protected void performDefaults() {
    fOverlayStore.loadDefaults();

    initializeFields();

    handleAppearanceColorListSelection();

    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performDefaults();
    }

    super.performDefaults();

    // there is currently no need for a viewer
    // fPreviewViewer.invalidateTextPresentation();
  }

  /*
   * @see PreferencePage#performOk()
   */
  public boolean performOk() {
    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performOk();
    }

    fOverlayStore.propagate();
    SSEUIPlugin.getDefault().savePluginPreferences();

    // tab width is also a model-side preference so need to set it
    // TODO need to handle tab width for formatter somehow
    // int tabWidth =
    // getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    // ModelPlugin.getDefault().getPluginPreferences().setValue(CommonModelPreferenceNames.TAB_WIDTH,
    // tabWidth);
    // ModelPlugin.getDefault().savePluginPreferences();

    return true;
  }

  void updateStatus(IStatus status) {
    if (!status.matches(IStatus.ERROR)) {
      for (int i = 0; i < fNumberFields.size(); i++) {
        Text text = (Text) fNumberFields.get(i);
        IStatus s = validatePositiveNumber(text.getText());
        status = s.getSeverity() > status.getSeverity() ? s : status;
      }
    }

    setValid(!status.matches(IStatus.ERROR));
    applyToStatusLine(this, status);
  }

  private IStatus validatePositiveNumber(String number) {
    StatusInfo status = new StatusInfo();
    if (number.length() == 0) {
      status.setError(SSEUIMessages.StructuredTextEditorPreferencePage_37);
    } else {
      try {
        int value = Integer.parseInt(number);
        if (value < 0)
          status.setError(number + SSEUIMessages.StructuredTextEditorPreferencePage_38);
      } catch (NumberFormatException e) {
        status.setError(number + SSEUIMessages.StructuredTextEditorPreferencePage_38);
      }
    }
    return status;
  }
}

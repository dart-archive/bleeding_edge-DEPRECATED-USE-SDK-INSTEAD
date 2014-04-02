/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.preferences;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.internal.ui.search.SearchManager;
import com.xored.glance.ui.sources.ColorManager;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @author Yuri Strot
 */
public class GlancePreferencePage extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage, IPreferenceConstants {

  private static class ColorEditor extends ColorFieldEditor {

    public ColorEditor(final Composite parent, final String text, final String prefKey) {
      this(parent, text, prefKey, ColorManager.getStore());
    }

    public ColorEditor(final Composite parent, final String text, final String prefKey,
        IPreferenceStore store) {
      super(prefKey, text, parent);
      super.setPreferenceStore(store);
    }

    @Override
    public void setPreferenceStore(final IPreferenceStore store) {
    }
  }

  private Button currentWindow;

  public GlancePreferencePage() {
    super(GRID);
  }

  @Override
  public void init(final IWorkbench workbench) {
  }

  @Override
  public boolean performOk() {
    final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      final boolean inWindow = SearchManager.getIntance().isInWindow(window);
      final boolean open = currentWindow.getSelection();
      if (open != inWindow) {
        SearchManager.getIntance().setStatusLine(window, open);
      }
    }
    return super.performOk();
  }

  /**
   * Adjust the layout of the field editors so that they are properly aligned.
   */
  @Override
  protected void adjustGridLayout() {
    super.adjustGridLayout();
    ((GridLayout) getFieldEditorParent().getLayout()).numColumns = 1;
  }

  @Override
  protected void createFieldEditors() {
    createSearchSettings(getFieldEditorParent());
    createColorSettings(getFieldEditorParent());
    createPanelSettings(getFieldEditorParent());
  }

  @Override
  protected IPreferenceStore doGetPreferenceStore() {
    return GlancePlugin.getDefault().getPreferenceStore();
  }

  @Override
  protected void performDefaults() {
    super.performDefaults();
    initCurrentWindow();
  }

  private Group createColorSettings(final Composite parent) {
    final Group group = new Group(parent, SWT.NONE);
    group.setText("Colors");
    group.setLayout(new GridLayout(1, false));
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Composite composite = new Composite(group, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    addField(new ColorEditor(composite, "Highlight:", COLOR_HIGHLIGHT));
    addField(new ColorEditor(composite, "Selection:", COLOR_SELECTION));

    return group;
  }

  private void createCurrentWindowOption(final Composite composite) {
    currentWindow = new Button(composite, SWT.CHECK);
    currentWindow.setText("Show in the current window");
    initCurrentWindow();
  }

  private Group createPanelSettings(final Composite parent) {
    final Group group = new Group(parent, SWT.NONE);
    group.setText("Panel");
    group.setLayout(new GridLayout(1, false));
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Composite composite = new Composite(group, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createCurrentWindowOption(composite);
    addField(new BooleanFieldEditor(PANEL_STARTUP, "Show at startup", composite));
    addField(new BooleanFieldEditor(
        PANEL_STATUS_LINE,
        "Show panel in status line when possible",
        composite));
    addField(new BooleanFieldEditor(PANEL_DIRECTIONS, "Show direction buttons", composite));
    addField(new BooleanFieldEditor(PANEL_CLOSE, "Show close button", composite));
    addField(new BooleanFieldEditor(PANEL_AUTO_INDEXING, "Enable auto indexing", composite));
    addField(new BooleanFieldEditor(SEARCH_INCREMENTAL, "Enable incremental search", composite));
    final IntegerFieldEditor maxIndexingDepthEditor = new IntegerFieldEditor(
        PANEL_MAX_INDEXING_DEPTH,
        "Max indexing depth for trees:",
        composite);
    maxIndexingDepthEditor.setValidRange(1, Integer.MAX_VALUE);
    addField(maxIndexingDepthEditor);
    addField(new IntegerFieldEditor(PANEL_TEXT_SIZE, "Default box width in chars:", composite));

    return group;
  }

  private Group createSearchSettings(final Composite parent) {
    final Group group = new Group(parent, SWT.NONE);
    group.setText("Search");
    group.setLayout(new GridLayout(1, false));
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Composite composite = new Composite(group, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    addField(new BooleanFieldEditor(SEARCH_CASE_SENSITIVE, LABEL_CASE_SENSITIVE, composite));
    addField(new BooleanFieldEditor(SEARCH_CAMEL_CASE, LABEL_CAMEL_CASE, composite));
    addField(new BooleanFieldEditor(SEARCH_WORD_PREFIX, LABEL_WORD_PREFIX, composite));
    addField(new BooleanFieldEditor(SEARCH_REGEXP, LABEL_REGEXP, composite));

    return group;
  }

  private void initCurrentWindow() {
    final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      currentWindow.setEnabled(false);
    } else {
      final boolean inWindow = SearchManager.getIntance().isInWindow(window);
      currentWindow.setSelection(inWindow);
    }
  }

}

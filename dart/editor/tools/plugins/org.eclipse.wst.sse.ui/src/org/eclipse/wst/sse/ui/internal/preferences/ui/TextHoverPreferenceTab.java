/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;
import org.eclipse.wst.sse.ui.internal.taginfo.TextHoverManager;
import org.eclipse.wst.sse.ui.internal.taginfo.TextHoverManager.TextHoverDescriptor;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Preference tab for Structured text editor hover help preferences
 * 
 * @author amywu
 */
public class TextHoverPreferenceTab extends AbstractPreferenceTab {

  private class InternalTableLabelProvider extends LabelProvider implements ITableLabelProvider {
    public InternalTableLabelProvider() {
      super();
    }

    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    public String getColumnText(Object element, int columnIndex) {
      switch (columnIndex) {
        case 0: // text hover label
          return ((TextHoverManager.TextHoverDescriptor) element).getLabel();

        case 1: // text hover state mask
          return ((TextHoverManager.TextHoverDescriptor) element).getModifierString();

        default:
          break;
      }

      return null;
    }
  }

  private static final String DELIMITER = SSEUIMessages.TextHoverPreferenceTab_delimiter; //$NON-NLS-1$
  private Text fDescription;
  private Table fHoverTable;
  private TableViewer fHoverTableViewer;
  private TableColumn fModifierColumn;
  // for this preference page
  private Text fModifierEditor;
  private TableColumn fNameColumn;

  private TextHoverDescriptor[] fTextHovers; // current list of text hovers

  public TextHoverPreferenceTab(PreferencePage mainPreferencePage, OverlayPreferenceStore store) {
    Assert.isNotNull(mainPreferencePage);
    Assert.isNotNull(store);
    setMainPreferencePage(mainPreferencePage);
    setOverlayStore(store);
    getOverlayStore().addKeys(createOverlayStoreKeys());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.preferences.ui.IPreferenceTab#createContents(org.eclipse.swt.widgets
   * .Composite)
   */
  public Control createContents(Composite tabFolder) {
    Composite hoverComposite = new Composite(tabFolder, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    hoverComposite.setLayout(layout);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    hoverComposite.setLayoutData(gd);

    // commented out until these preferences are actually handled in some
    // way
    //		String rollOverLabel=
    // ResourceHandler.getString("TextHoverPreferenceTab.annotationRollover");
    // //$NON-NLS-1$
    //		addCheckBox(hoverComposite, rollOverLabel,
    // CommonEditorPreferenceNames.EDITOR_ANNOTATION_ROLL_OVER, 0);
    //
    //		// Affordance checkbox
    //		String showAffordanceLabel =
    // ResourceHandler.getString("TextHoverPreferenceTab.showAffordance");
    // //$NON-NLS-1$
    //		addCheckBox(hoverComposite, showAffordanceLabel,
    // CommonEditorPreferenceNames.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE, 0);

    Label label = new Label(hoverComposite, SWT.NONE);
    label.setText(SSEUIMessages.TextHoverPreferenceTab_hoverPreferences); //$NON-NLS-1$
    gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
    gd.horizontalAlignment = GridData.BEGINNING;
    label.setLayoutData(gd);

    fHoverTableViewer = CheckboxTableViewer.newCheckList(hoverComposite, SWT.H_SCROLL
        | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
    // Hover table
    fHoverTable = fHoverTableViewer.getTable();
    fHoverTable.setHeaderVisible(true);
    fHoverTable.setLinesVisible(true);

    gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=104507
    GC gc = new GC(fHoverTable);
    gc.setFont(fHoverTable.getFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    gc.dispose();
    int heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, 7);
    gd.heightHint = heightHint;

    fHoverTable.setLayoutData(gd);

    TableLayout tableLayout = new TableLayout();
    tableLayout.addColumnData(new ColumnWeightData(1, 140, true));
    tableLayout.addColumnData(new ColumnWeightData(1, 140, true));
    fHoverTable.setLayout(tableLayout);

    fHoverTable.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        handleHoverListSelection();
      }
    });

    fNameColumn = new TableColumn(fHoverTable, SWT.NONE);
    fNameColumn.setText(SSEUIMessages.TextHoverPreferenceTab_nameColumnTitle); //$NON-NLS-1$
    fNameColumn.setResizable(true);

    fModifierColumn = new TableColumn(fHoverTable, SWT.NONE);
    fModifierColumn.setText(SSEUIMessages.TextHoverPreferenceTab_modifierColumnTitle); //$NON-NLS-1$
    fModifierColumn.setResizable(true);

    fHoverTableViewer.setUseHashlookup(true);
    fHoverTableViewer.setContentProvider(new ArrayContentProvider());
    fHoverTableViewer.setLabelProvider(new InternalTableLabelProvider());
    ((CheckboxTableViewer) fHoverTableViewer).addCheckStateListener(new ICheckStateListener() {
      /*
       * @see
       * org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers
       * .CheckStateChangedEvent)
       */
      public void checkStateChanged(CheckStateChangedEvent event) {
        String id = ((TextHoverDescriptor) event.getElement()).getId();
        if (id == null)
          return;

        TextHoverManager.TextHoverDescriptor[] descriptors = getTextHoverManager().getTextHovers();
        TextHoverManager.TextHoverDescriptor hoverConfig = null;
        int i = 0, length = fTextHovers.length;
        while (i < length) {
          if (id.equals(descriptors[i].getId())) {
            hoverConfig = fTextHovers[i];
            hoverConfig.setEnabled(event.getChecked());
            fModifierEditor.setEnabled(event.getChecked());
            fHoverTableViewer.setSelection(new StructuredSelection(descriptors[i]));
          }
          i++;
        }

        handleHoverListSelection();
        updateStatus(hoverConfig);
      }
    });

    // Text field for modifier string
    label = new Label(hoverComposite, SWT.LEFT);
    label.setText(SSEUIMessages.TextHoverPreferenceTab_keyModifier); //$NON-NLS-1$
    fModifierEditor = new Text(hoverComposite, SWT.BORDER);
    gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    fModifierEditor.setLayoutData(gd);

    fModifierEditor.addKeyListener(new KeyListener() {
      private boolean isModifierCandidate;

      public void keyPressed(KeyEvent e) {
        isModifierCandidate = e.keyCode > 0 && e.character == 0 && e.stateMask == 0;
      }

      public void keyReleased(KeyEvent e) {
        if (isModifierCandidate && e.stateMask > 0 && e.stateMask == e.stateMask
            && e.character == 0) {// &&
          // e.time
          // -time
          // <
          // 1000)
          // {
          String text = fModifierEditor.getText();
          Point selection = fModifierEditor.getSelection();
          int i = selection.x - 1;
          while (i > -1 && Character.isWhitespace(text.charAt(i))) {
            i--;
          }
          boolean needsPrefixDelimiter = i > -1
              && !String.valueOf(text.charAt(i)).equals(DELIMITER);

          i = selection.y;
          while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
          }
          boolean needsPostfixDelimiter = i < text.length()
              && !String.valueOf(text.charAt(i)).equals(DELIMITER);

          String insertString;

          if (needsPrefixDelimiter && needsPostfixDelimiter)
            insertString = NLS.bind(
                SSEUIMessages.TextHoverPreferenceTab_insertDelimiterAndModifierAndDelimiter,
                new String[] {Action.findModifierString(e.stateMask)});
          else if (needsPrefixDelimiter)
            insertString = NLS.bind(
                SSEUIMessages.TextHoverPreferenceTab_insertDelimiterAndModifier,
                new String[] {Action.findModifierString(e.stateMask)});
          else if (needsPostfixDelimiter)
            insertString = NLS.bind(
                SSEUIMessages.TextHoverPreferenceTab_insertModifierAndDelimiter,
                new String[] {Action.findModifierString(e.stateMask)});
          else
            insertString = Action.findModifierString(e.stateMask);

          if (insertString != null)
            fModifierEditor.insert(insertString);
        }
      }
    });

    fModifierEditor.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        handleModifierModified();
      }
    });

    // Description
    Label descriptionLabel = new Label(hoverComposite, SWT.LEFT);
    descriptionLabel.setText(SSEUIMessages.TextHoverPreferenceTab_description); //$NON-NLS-1$
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    gd.horizontalSpan = 2;
    descriptionLabel.setLayoutData(gd);
    fDescription = new Text(hoverComposite, SWT.LEFT | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY
        | SWT.BORDER);
    gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    gd.horizontalSpan = 2;
    fDescription.setLayoutData(gd);

    initialize();

    Dialog.applyDialogFont(hoverComposite);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(hoverComposite,
        IHelpContextIds.PREFSTE_HOVERS_HELPID);
    return hoverComposite;
  }

  private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
    ArrayList overlayKeys = new ArrayList();

    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
        EditorPreferenceNames.EDITOR_TEXT_HOVER_MODIFIERS));

    OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
    overlayKeys.toArray(keys);
    return keys;
  }

  private String generateTextHoverString() {
    StringBuffer buf = new StringBuffer();

    for (int i = 0; i < fTextHovers.length; i++) {
      buf.append(fTextHovers[i].getId());
      buf.append(TextHoverManager.HOVER_ATTRIBUTE_SEPARATOR);
      buf.append(Boolean.toString(fTextHovers[i].isEnabled()));
      buf.append(TextHoverManager.HOVER_ATTRIBUTE_SEPARATOR);
      String modifier = fTextHovers[i].getModifierString();
      if (modifier == null || modifier.length() == 0)
        modifier = TextHoverManager.NO_MODIFIER;
      buf.append(modifier);
      buf.append(TextHoverManager.HOVER_SEPARATOR);
    }
    return buf.toString();
  }

  private TextHoverManager getTextHoverManager() {
    return SSEUIPlugin.getDefault().getTextHoverManager();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.IPreferenceTab#getTitle()
   */
  public String getTitle() {
    return SSEUIMessages.TextHoverPreferenceTab_title; //$NON-NLS-1$
  }

  void handleHoverListSelection() {
    int i = fHoverTable.getSelectionIndex();

    if (i == -1) {
      if (fHoverTable.getSelectionCount() == 0)
        fModifierEditor.setEnabled(false);
      return;
    }

    boolean enabled = fTextHovers[i].isEnabled();
    fModifierEditor.setEnabled(enabled);
    fModifierEditor.setText(fTextHovers[i].getModifierString());
    String description = fTextHovers[i].getDescription();
    if (description == null)
      description = ""; //$NON-NLS-1$
    fDescription.setText(description);
  }

  private void handleModifierModified() {
    int i = fHoverTable.getSelectionIndex();
    if (i == -1)
      return;

    String modifiers = fModifierEditor.getText();
    fTextHovers[i].setModifierString(modifiers);

    // update table
    fHoverTableViewer.refresh(fTextHovers[i]);

    updateStatus(fTextHovers[i]);
  }

  private void initialize() {
    restoreFromOverlay();
    fHoverTableViewer.setInput(fTextHovers);

    initializeFields();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.AbstractPreferenceTab#initializeFields()
   */
  protected void initializeFields() {
    super.initializeFields();

    fModifierEditor.setEnabled(false);
    // initialize checkboxes in hover table
    for (int i = 0; i < fTextHovers.length; i++)
      fHoverTable.getItem(i).setChecked(fTextHovers[i].isEnabled());
    fHoverTableViewer.refresh();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.IPreferenceTab#performApply()
   */
  public void performApply() {
    performOk();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.IPreferenceTab#performDefaults()
   */
  public void performDefaults() {
    initialize();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.IPreferenceTab#performOk()
   */
  public void performOk() {
    String textHoverString = generateTextHoverString();
    getOverlayStore().setValue(EditorPreferenceNames.EDITOR_TEXT_HOVER_MODIFIERS, textHoverString);
    getTextHoverManager().resetTextHovers(); // notify text hover manager
    // it should reset to get
    // latest preferences
  }

  /**
   * Populates fTextHovers with text hover description from the overlay store (which is the
   * preferences)
   */
  private void restoreFromOverlay() {
    String descriptorsString = getOverlayStore().getString(
        EditorPreferenceNames.EDITOR_TEXT_HOVER_MODIFIERS);
    fTextHovers = getTextHoverManager().generateTextHoverDescriptors(descriptorsString);
  }

  void updateStatus(TextHoverManager.TextHoverDescriptor hoverConfig) {
    IStatus status = new StatusInfo();

    if (hoverConfig != null && hoverConfig.isEnabled()
        && EditorUtility.computeStateMask(hoverConfig.getModifierString()) == -1)
      status = new StatusInfo(IStatus.ERROR, NLS.bind(
          SSEUIMessages.TextHoverPreferenceTab_modifierIsNotValid,
          new String[] {hoverConfig.getModifierString()}));

    int i = 0;
    HashMap stateMasks = new HashMap(fTextHovers.length);
    while (status.isOK() && i < fTextHovers.length) {
      if (fTextHovers[i].isEnabled()) {
        String label = fTextHovers[i].getLabel();
        Integer stateMask = new Integer(
            EditorUtility.computeStateMask(fTextHovers[i].getModifierString()));
        if (stateMask.intValue() == -1)
          status = new StatusInfo(IStatus.ERROR, NLS.bind(
              SSEUIMessages.TextHoverPreferenceTab_modifierIsNotValidForHover, new String[] {
                  fTextHovers[i].getModifierString(), label}));
        else if (stateMasks.containsKey(stateMask))
          status = new StatusInfo(IStatus.ERROR, NLS.bind(
              SSEUIMessages.TextHoverPreferenceTab_duplicateModifier, new String[] {
                  label, (String) stateMasks.get(stateMask)}));
        else
          stateMasks.put(stateMask, label);
      }
      i++;
    }

    updateStatus(status);
  }
}

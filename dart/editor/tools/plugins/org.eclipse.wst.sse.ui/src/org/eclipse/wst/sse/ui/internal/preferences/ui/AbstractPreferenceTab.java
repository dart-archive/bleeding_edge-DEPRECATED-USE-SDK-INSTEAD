/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Generic preference tab page that contains helpful methods
 * 
 * @author amywu
 */
abstract public class AbstractPreferenceTab implements IPreferenceTab {

  Map fCheckBoxes = new HashMap();
  private SelectionListener fCheckBoxListener = new SelectionListener() {
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
      Button button = (Button) e.widget;
      fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
    }
  };
  private PreferencePage fMainPreferencePage;
  private ModifyListener fNumberFieldListener = new ModifyListener() {
    public void modifyText(ModifyEvent e) {
      numberFieldChanged((Text) e.widget);
    }
  };
  private ArrayList fNumberFields = new ArrayList();
  OverlayPreferenceStore fOverlayStore;
  private ModifyListener fTextFieldListener = new ModifyListener() {
    public void modifyText(ModifyEvent e) {
      Text text = (Text) e.widget;
      fOverlayStore.setValue((String) fTextFields.get(text), text.getText());
    }
  };
  Map fTextFields = new HashMap();

  protected Button addCheckBox(Composite parent, String label, String key, int indentation) {
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
   * Returns an array of size 2: - first element is of type <code>Label</code>- second element is of
   * type <code>Text</code> Use <code>getLabelControl</code> and <code>getTextControl</code> to get
   * the 2 controls.
   */
  private Control[] addLabelledTextField(Composite composite, String label, String key,
      int textLimit, int indentation, boolean isNumber) {
    Label labelControl = new Label(composite, SWT.NONE);
    labelControl.setText(label);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.horizontalIndent = indentation;
    labelControl.setLayoutData(gd);

    Text textControl = new Text(composite, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.widthHint = convertWidthInCharsToPixels(textControl, textLimit + 1);
    textControl.setLayoutData(gd);
    textControl.setTextLimit(textLimit);
    fTextFields.put(textControl, key);
    if (isNumber) {
      fNumberFields.add(textControl);
      textControl.addModifyListener(fNumberFieldListener);
    } else {
      textControl.addModifyListener(fTextFieldListener);
    }

    return new Control[] {labelControl, textControl};
  }

  protected Text addTextField(Composite composite, String label, String key, int textLimit,
      int indentation, boolean isNumber) {
    return getTextControl(addLabelledTextField(composite, label, key, textLimit, indentation,
        isNumber));
  }

  /**
   * Applies the status to the status line of a dialog page.
   */
  private void applyToStatusLine(IStatus status) {
    String message = status.getMessage();
    switch (status.getSeverity()) {
      case IStatus.OK:
        fMainPreferencePage.setMessage(message, IMessageProvider.NONE);
        fMainPreferencePage.setErrorMessage(null);
        break;
      case IStatus.WARNING:
        fMainPreferencePage.setMessage(message, IMessageProvider.WARNING);
        fMainPreferencePage.setErrorMessage(null);
        break;
      case IStatus.INFO:
        fMainPreferencePage.setMessage(message, IMessageProvider.INFORMATION);
        fMainPreferencePage.setErrorMessage(null);
        break;
      default:
        if (message.length() == 0) {
          message = null;
        }
        fMainPreferencePage.setMessage(null);
        fMainPreferencePage.setErrorMessage(message);
        break;
    }
  }

  /**
   * Returns the number of pixels corresponding to the width of the given number of characters. This
   * method was copied from org.eclipse.jface.dialogs.DialogPage
   * <p>
   * 
   * @param a control in the page
   * @param chars the number of characters
   * @return the number of pixels
   */
  private int convertWidthInCharsToPixels(Control testControl, int chars) {
    // Compute and store a font metric
    GC gc = new GC(testControl);
    gc.setFont(JFaceResources.getDialogFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    gc.dispose();

    // test for failure to initialize for backward compatibility
    if (fontMetrics == null)
      return 0;
    return Dialog.convertWidthInCharsToPixels(fontMetrics, chars);
  }

  /**
   * @return Returns the fMainPreferencePage.
   */
  protected PreferencePage getMainPreferencePage() {
    return fMainPreferencePage;
  }

  /**
   * @return Returns the fOverlayStore.
   */
  protected OverlayPreferenceStore getOverlayStore() {
    return fOverlayStore;
  }

  private Text getTextControl(Control[] labelledTextField) {
    return (Text) labelledTextField[1];
  }

  protected void initializeFields() {
    Iterator e = fCheckBoxes.keySet().iterator();
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

  void numberFieldChanged(Text textControl) {
    String number = textControl.getText();
    IStatus status = validatePositiveNumber(number);
    if (!status.matches(IStatus.ERROR))
      fOverlayStore.setValue((String) fTextFields.get(textControl), number);
    updateStatus(status);
  }

  /**
   * @param mainPreferencePage The fMainPreferencePage to set.
   */
  protected void setMainPreferencePage(PreferencePage mainPreferencePage) {
    fMainPreferencePage = mainPreferencePage;
  }

  /**
   * @param overlayStore The fOverlayStore to set.
   */
  protected void setOverlayStore(OverlayPreferenceStore overlayStore) {
    fOverlayStore = overlayStore;
  }

  /**
   * Update status of main preference page
   * 
   * @param status
   */
  protected void updateStatus(IStatus status) {
    if (!status.matches(IStatus.ERROR)) {
      for (int i = 0; i < fNumberFields.size(); i++) {
        Text text = (Text) fNumberFields.get(i);
        IStatus s = validatePositiveNumber(text.getText());
        status = s.getSeverity() > status.getSeverity() ? s : status;
      }
    }

    fMainPreferencePage.setValid(!status.matches(IStatus.ERROR));
    applyToStatusLine(status);
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

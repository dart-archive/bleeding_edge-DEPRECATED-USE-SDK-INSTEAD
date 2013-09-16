/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

/**
 * <p>
 * This is a <code>MessageDialog</code> which allows the user to choose that the dialog isn't shown
 * again the next time.
 * </p>
 * 
 * @base org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog
 */
public class OptionalMessageDialog extends MessageDialog {

  // String constants for widgets
  private static final String CHECKBOX_TEXT = SSEUIMessages.OptionalMessageDialog_dontShowAgain;

  // Dialog store id constants
  private static final String STORE_ID = "ContentAssist_OptionalMessageDialog.hide."; //$NON-NLS-1$

  public static final int NOT_SHOWN = IDialogConstants.CLIENT_ID + 1;

  private final String fId;
  private final String fCheckBoxText;

  private Button fHideDialogCheckBox;

  /**
   * Opens the dialog but only if the user hasn't chosen to hide it.
   * 
   * @return the index of the pressed button or {@link SWT#DEFAULT} if the dialog got dismissed
   *         without pressing a button (e.g. via Esc) or {{@link #NOT_SHOWN} if the dialog was not
   *         shown
   */
  public static int open(String id, Shell parent, String title, Image titleImage, String message,
      int dialogType, String[] buttonLabels, int defaultButtonIndex) {
    return open(id, parent, title, titleImage, message, dialogType, buttonLabels,
        defaultButtonIndex, CHECKBOX_TEXT);
  }

  /**
   * Opens the dialog but only if the user hasn't chosen to hide it.
   * 
   * @return the index of the pressed button or {@link SWT#DEFAULT} if the dialog got dismissed
   *         without pressing a button (e.g. via Esc) or {{@link #NOT_SHOWN} if the dialog was not
   *         shown
   */
  public static int open(String id, Shell parent, String title, Image titleImage, String message,
      int dialogType, String[] buttonLabels, int defaultButtonIndex, String checkboxText) {
    if (!isDialogEnabled(id))
      return OptionalMessageDialog.NOT_SHOWN;

    MessageDialog dialog = new OptionalMessageDialog(id, parent, title, titleImage, message,
        dialogType, buttonLabels, defaultButtonIndex, checkboxText);
    return dialog.open();
  }

  protected OptionalMessageDialog(String id, Shell parent, String title, Image titleImage,
      String message, int dialogType, String[] buttonLabels, int defaultButtonIndex) {
    this(id, parent, title, titleImage, message, dialogType, buttonLabels, defaultButtonIndex,
        CHECKBOX_TEXT);
  }

  protected OptionalMessageDialog(String id, Shell parent, String title, Image titleImage,
      String message, int dialogType, String[] buttonLabels, int defaultButtonIndex,
      String checkBoxText) {
    super(parent, title, titleImage, message, dialogType, buttonLabels, defaultButtonIndex);
    fId = id;
    fCheckBoxText = checkBoxText;
  }

  protected Control createCustomArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    fHideDialogCheckBox = new Button(composite, SWT.CHECK | SWT.LEFT);
    fHideDialogCheckBox.setText(fCheckBoxText);
    fHideDialogCheckBox.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        setDialogEnabled(fId, !((Button) e.widget).getSelection());
      }
    });
    applyDialogFont(fHideDialogCheckBox);
    return fHideDialogCheckBox;
  }

  //--------------- Configuration handling --------------

  /**
   * Returns this dialog
   * 
   * @return the settings to be used
   */
  private static IDialogSettings getDialogSettings() {
    IDialogSettings settings = SSEUIPlugin.getDefault().getDialogSettings();
    settings = settings.getSection(STORE_ID);
    if (settings == null)
      settings = SSEUIPlugin.getDefault().getDialogSettings().addNewSection(STORE_ID);
    return settings;
  }

  /**
   * Answers whether the optional dialog is enabled and should be shown.
   */
  public static boolean isDialogEnabled(String key) {
    IDialogSettings settings = getDialogSettings();
    return !settings.getBoolean(key);
  }

  /**
   * Sets whether the optional dialog is enabled and should be shown.
   */
  public static void setDialogEnabled(String key, boolean isEnabled) {
    IDialogSettings settings = getDialogSettings();
    settings.put(key, !isEnabled);
  }

  /**
   * Clears all remembered information about hidden dialogs
   */
  public static void clearAllRememberedStates() {
    IDialogSettings settings = SSEUIPlugin.getDefault().getDialogSettings();
    settings.addNewSection(STORE_ID);
  }
}
